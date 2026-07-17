package com.avtoforward.automaster.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.avtoforward.automaster.adapters.ChatAdapter;
import com.avtoforward.automaster.models.ChatMessage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;

public class ForumChatFragment extends Fragment implements ChatAdapter.OnMessageActionListener {

    private static final String TAG = "ForumChat";
    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int PERMISSION_REQUEST = 102;

    private String topicId;
    private String topicTitle;
    private ListView listView;
    private EditText editMessage;
    private Button buttonSend;
    private Button buttonAttach;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private Handler autoRefreshHandler = new Handler();
    private Runnable autoRefreshRunnable;
    private String currentUserId;
    private String currentNickname;

    public ForumChatFragment(String topicId, String topicTitle) {
        this.topicId = topicId;
        this.topicTitle = topicTitle;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum_chat, container, false);

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(topicTitle);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        listView = view.findViewById(R.id.listChatMessages);
        editMessage = view.findViewById(R.id.editChatMessage);
        buttonSend = view.findViewById(R.id.buttonSendMessage);
        buttonAttach = view.findViewById(R.id.buttonAttach);

        currentUserId = PocketBaseClient.getCurrentUserId();
        Log.d(TAG, "currentUserId: " + currentUserId);

        loadUserInfo();

        adapter = new ChatAdapter(requireContext(), messages, currentUserId, this);
        listView.setAdapter(adapter);

        loadMessages();

        buttonSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                if (currentNickname == null || currentNickname.isEmpty()) {
                    Toast.makeText(getContext(), "Заполните никнейм в профиле (поле 'Никнейм')", Toast.LENGTH_LONG).show();
                    return;
                }
                sendMessage(text, null);
            } else {
                Toast.makeText(getContext(), "Введите сообщение", Toast.LENGTH_SHORT).show();
            }
        });

        buttonAttach.setOnClickListener(v -> showAttachmentDialog());

        autoRefreshRunnable = () -> {
            loadMessages();
            autoRefreshHandler.postDelayed(autoRefreshRunnable, 5000);
        };
        autoRefreshHandler.postDelayed(autoRefreshRunnable, 5000);

        return view;
    }

    private void loadUserInfo() {
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(currentUserId);
            if (user != null) {
                if (user.has("nickname") && !user.get("nickname").isJsonNull()) {
                    currentNickname = user.get("nickname").getAsString();
                } else {
                    currentNickname = "";
                }
                Log.d(TAG, "currentNickname: " + currentNickname);
            }
        }).start();
    }

    private void showAttachmentDialog() {
        String[] options = {"Выбрать из галереи", "Сделать фото"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Добавить фото")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkPermissionAndPickImage();
                    } else {
                        checkPermissionAndOpenCamera();
                    }
                })
                .show();
    }

    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = READ_MEDIA_IMAGES;
        } else {
            permission = READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{permission}, PERMISSION_REQUEST);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        }
    }

    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{CAMERA}, PERMISSION_REQUEST);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_CAMERA);
            } else {
                Toast.makeText(getContext(), "Камера не найдена", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        } else {
            Toast.makeText(getContext(), "Нет доступа к галерее", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_OK && data != null) {
            Uri imageUri = null;
            if (requestCode == REQUEST_IMAGE_PICK) {
                imageUri = data.getData();
            } else if (requestCode == REQUEST_CAMERA) {
                Toast.makeText(getContext(), "Фото с камеры пока не поддерживается", Toast.LENGTH_SHORT).show();
                return;
            }
            if (imageUri != null) {
                if (currentNickname == null || currentNickname.isEmpty()) {
                    Toast.makeText(getContext(), "Заполните никнейм в профиле", Toast.LENGTH_LONG).show();
                    return;
                }
                sendMessage("", imageUri);
            }
        }
    }

    private void sendMessage(String text, Uri imageUri) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        if (topicId == null || topicId.isEmpty()) {
            Toast.makeText(getContext(), "Ошибка: тема не выбрана", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "topicId is null or empty");
            return;
        }

        editMessage.setText("");
        buttonSend.setEnabled(false);
        buttonAttach.setEnabled(false);

        if (imageUri == null) {
            new Thread(() -> {
                PocketBaseClient.sendForumMessage(topicId, currentUserId, text, () -> {
                    requireActivity().runOnUiThread(() -> {
                        loadMessages();
                        buttonSend.setEnabled(true);
                        buttonAttach.setEnabled(true);
                    });
                });
            }).start();
        } else {
            List<Uri> images = new ArrayList<>();
            images.add(imageUri);
            new Thread(() -> {
                PocketBaseClient.sendForumMessage(topicId, currentUserId, text, images, () -> {
                    requireActivity().runOnUiThread(() -> {
                        loadMessages();
                        buttonSend.setEnabled(true);
                        buttonAttach.setEnabled(true);
                    });
                });
            }).start();
        }
    }

    private void loadMessages() {
        new Thread(() -> {
            try {
                JsonObject result = PocketBaseClient.getForumMessages(topicId);
                Log.d(TAG, "loadMessages result: " + (result != null ? result.toString() : "null"));
                if (result != null && result.has("items")) {
                    JsonArray items = result.getAsJsonArray("items");
                    Log.d(TAG, "items count: " + items.size());
                    List<ChatMessage> newMessages = new ArrayList<>();
                    SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US);
                    SimpleDateFormat sdfOutput = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

                    for (int i = 0; i < items.size(); i++) {
                        try {
                            JsonObject item = items.get(i).getAsJsonObject();
                            String id = item.get("id").getAsString();
                            String messageText = item.has("message_text") ? item.get("message_text").getAsString() : "";
                            String created = item.get("created").getAsString();

                            // Парсим время
                            String formattedTime = "";
                            try {
                                Date date = sdfInput.parse(created);
                                if (date != null) {
                                    formattedTime = sdfOutput.format(date);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing date: " + e.getMessage());
                            }

                            String authorId = "";
                            String authorName = "Пользователь";
                            String city = ""; // город автора
                            if (item.has("expand") && item.getAsJsonObject("expand").has("author")) {
                                JsonObject author = item.getAsJsonObject("expand").getAsJsonObject("author");
                                authorId = author.get("id").getAsString();
                                // Получаем никнейм
                                if (author.has("nickname") && !author.get("nickname").isJsonNull()) {
                                    String nickname = author.get("nickname").getAsString();
                                    authorName = nickname.isEmpty() ? author.get("email").getAsString() : nickname;
                                } else if (author.has("full_name") && !author.get("full_name").isJsonNull()) {
                                    String fullName = author.get("full_name").getAsString();
                                    authorName = fullName.isEmpty() ? author.get("email").getAsString() : fullName;
                                } else {
                                    authorName = author.get("email").getAsString();
                                }
                                // Получаем город
                                if (author.has("city") && !author.get("city").isJsonNull()) {
                                    city = author.get("city").getAsString();
                                }
                            }

                            // Добавляем город к времени, если он есть
                            if (!city.isEmpty()) {
                                formattedTime = formattedTime + " (" + city + ")";
                            }

                            List<String> imageUrls = new ArrayList<>();
                            if (item.has("attachments") && !item.get("attachments").isJsonNull()) {
                                if (item.get("attachments").isJsonArray()) {
                                    JsonArray arr = item.get("attachments").getAsJsonArray();
                                    for (int j = 0; j < arr.size(); j++) {
                                        imageUrls.add(arr.get(j).getAsString());
                                    }
                                } else if (item.get("attachments").isJsonPrimitive()) {
                                    String single = item.get("attachments").getAsString();
                                    if (!single.isEmpty()) imageUrls.add(single);
                                }
                            }

                            // Создаём ChatMessage с временем (уже с городом)
                            ChatMessage chatMessage = new ChatMessage(id, authorId, authorName, messageText, imageUrls, formattedTime);
                            newMessages.add(chatMessage);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing message: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    requireActivity().runOnUiThread(() -> {
                        messages.clear();
                        messages.addAll(newMessages);
                        adapter.notifyDataSetChanged();
                        if (messages.size() > 0) {
                            listView.setSelection(messages.size() - 1);
                        }
                        markTopicAsRead();
                    });
                } else {
                    Log.e(TAG, "loadMessages: result is null or no items");
                }
            } catch (Exception e) {
                Log.e(TAG, "loadMessages error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void markTopicAsRead() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("forum_last_read", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("topic_" + topicId, System.currentTimeMillis());
        editor.apply();
    }

    @Override
    public void onEditMessage(ChatMessage message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать сообщение");
        EditText input = new EditText(requireContext());
        input.setText(message.getMessageText());
        builder.setView(input);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty()) {
                new Thread(() -> {
                    boolean success = PocketBaseClient.updateMessage(message.getId(), newText);
                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            loadMessages();
                            Toast.makeText(getContext(), "Сообщение обновлено", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Ошибка редактирования", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    @Override
    public void onDeleteMessage(ChatMessage message) {
        Toast.makeText(getContext(), "Удаление: " + message.getId(), Toast.LENGTH_SHORT).show();

        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить сообщение")
                .setMessage("Вы уверены?")
                .setPositiveButton("Да", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = PocketBaseClient.deleteMessage(message.getId());
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                loadMessages();
                                Toast.makeText(getContext(), "Сообщение удалено", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Ошибка удаления (код ошибки см. в логах)", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMessages();
        markTopicAsRead();
        loadUserInfo();
        if (autoRefreshRunnable != null) {
            autoRefreshHandler.postDelayed(autoRefreshRunnable, 5000);
        }
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(topicTitle);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }
}