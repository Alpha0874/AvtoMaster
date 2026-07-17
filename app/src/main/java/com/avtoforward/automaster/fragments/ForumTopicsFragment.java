package com.avtoforward.automaster.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.avtoforward.automaster.ForumActivity;
import com.avtoforward.automaster.PocketBaseClient;
import com.avtoforward.automaster.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForumTopicsFragment extends Fragment {

    private final String subcategoryId;
    private final String subcategoryName;
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TopicAdapter adapter;
    private List<TopicItem> topics = new ArrayList<>();
    private SharedPreferences lastReadPrefs;
    private String currentUserId;

    private static class TopicItem {
        String id;
        String title;
        int unreadCount;
        String createdBy;       // ID автора
        String createdDate;     // Дата создания в формате ISO для проверки

        TopicItem(String id, String title, String createdBy, String createdDate) {
            this.id = id;
            this.title = title;
            this.unreadCount = 0;
            this.createdBy = createdBy;
            this.createdDate = createdDate;
        }
    }

    private class TopicAdapter extends ArrayAdapter<TopicItem> {
        TopicAdapter(Context context, List<TopicItem> items) {
            super(context, android.R.layout.simple_list_item_2, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);
            TopicItem item = getItem(position);
            text1.setText(item.title);
            if (item.unreadCount > 0) {
                text2.setText(String.valueOf(item.unreadCount));
                text2.setVisibility(View.VISIBLE);
            } else {
                text2.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    public ForumTopicsFragment(String subcategoryId, String subcategoryName) {
        this.subcategoryId = subcategoryId;
        this.subcategoryName = subcategoryName;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum_topics, container, false);

        listView = view.findViewById(R.id.listTopics);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        Button buttonCreateTopic = view.findViewById(R.id.buttonCreateTopic);
        TextView title = view.findViewById(R.id.textCategoryTitle);
        title.setText("Темы: " + subcategoryName);

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Темы: " + subcategoryName);
        }

        currentUserId = PocketBaseClient.getCurrentUserId();
        lastReadPrefs = requireContext().getSharedPreferences("forum_last_read", Context.MODE_PRIVATE);

        adapter = new TopicAdapter(requireContext(), topics);
        listView.setAdapter(adapter);

        loadTopics();

        swipeRefreshLayout.setOnRefreshListener(() -> loadTopics());

        buttonCreateTopic.setOnClickListener(v -> showCreateTopicDialog());

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            TopicItem item = adapter.getItem(position);
            if (item != null && getActivity() instanceof ForumActivity) {
                ((ForumActivity) getActivity()).openChat(item.id, item.title);
            }
        });

        // Долгое нажатие — показываем меню (редактировать/удалить)
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            TopicItem item = adapter.getItem(position);
            if (item == null) return false;

            // Все проверки делаем в отдельном потоке, чтобы не блокировать UI
            new Thread(() -> {
                String userRole = PocketBaseClient.getUserRole();
                boolean isAdmin = "admin".equals(userRole);
                boolean isAuthor = currentUserId != null && currentUserId.equals(item.createdBy);

                // Проверяем, можно ли удалить (автор и прошло < 15 минут, или админ)
                boolean canDelete = false;
                if (isAdmin) {
                    canDelete = true;
                } else if (isAuthor) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US);
                        Date createdDate = sdf.parse(item.createdDate);
                        if (createdDate != null) {
                            long now = System.currentTimeMillis();
                            long diffMs = now - createdDate.getTime();
                            long diffMin = diffMs / (60 * 1000);
                            canDelete = (diffMin < 15);
                        }
                    } catch (Exception e) {
                        Log.e("ForumTopics", "Error parsing date", e);
                    }
                }

                final boolean finalCanDelete = canDelete;
                final boolean finalIsAuthor = isAuthor;

                requireActivity().runOnUiThread(() -> {
                    // Формируем меню
                    List<String> actions = new ArrayList<>();
                    if (finalIsAuthor) {
                        actions.add("Редактировать");
                    }
                    if (finalCanDelete) {
                        actions.add("Удалить");
                    }
                    if (actions.isEmpty()) {
                        Toast.makeText(getContext(), "Нет доступных действий", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] items = actions.toArray(new String[0]);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Действие с темой")
                            .setItems(items, (dialog, which) -> {
                                String selected = items[which];
                                if ("Редактировать".equals(selected)) {
                                    showRenameDialog(item.id, item.title);
                                } else if ("Удалить".equals(selected)) {
                                    confirmDelete(item.id);
                                }
                            })
                            .show();
                });
            }).start();

            return true;
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Задержка для обновления после возврата
        new Handler().postDelayed(() -> {
            Log.d("ForumTopics", "Reloading topics after returning");
            loadTopics();
        }, 300);
    }

    private void loadTopics() {
        new Thread(() -> {
            JsonObject result = PocketBaseClient.getTopicsBySubcategory(subcategoryId);
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                List<TopicItem> newTopics = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    JsonObject obj = items.get(i).getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    String title = obj.get("title").getAsString();
                    String createdBy = obj.has("created_by") && !obj.get("created_by").isJsonNull()
                            ? obj.get("created_by").getAsString() : "";
                    String createdDate = obj.has("created") && !obj.get("created").isJsonNull()
                            ? obj.get("created").getAsString() : "";
                    newTopics.add(new TopicItem(id, title, createdBy, createdDate));
                }
                requireActivity().runOnUiThread(() -> {
                    topics.clear();
                    topics.addAll(newTopics);
                    adapter.notifyDataSetChanged();
                    loadUnreadCounts();
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "Ошибка загрузки тем", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadUnreadCounts() {
        if (currentUserId == null) return;
        for (TopicItem topic : topics) {
            new Thread(() -> {
                long lastRead = lastReadPrefs.getLong("topic_" + topic.id, 0);
                int unread = PocketBaseClient.getUnreadCountForTopic(topic.id, lastRead);
                requireActivity().runOnUiThread(() -> {
                    topic.unreadCount = unread;
                    adapter.notifyDataSetChanged();
                });
            }).start();
        }
        requireActivity().runOnUiThread(() -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void showCreateTopicDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Новая тема");
        EditText input = new EditText(requireContext());
        input.setHint("Введите название темы");
        builder.setView(input);
        builder.setPositiveButton("Создать", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (!title.isEmpty()) {
                createNewTopic(title);
            } else {
                Toast.makeText(getContext(), "Введите название", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void createNewTopic(String title) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            boolean success = PocketBaseClient.createTopic(title, subcategoryId, currentUserId);
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Тема создана", Toast.LENGTH_SHORT).show();
                    loadTopics();
                } else {
                    Toast.makeText(getContext(), "Ошибка создания темы", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showRenameDialog(String topicId, String oldTitle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Переименовать тему");
        EditText input = new EditText(requireContext());
        input.setText(oldTitle);
        input.setSelection(oldTitle.length());
        builder.setView(input);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                new Thread(() -> {
                    boolean success = PocketBaseClient.renameTopic(topicId, newTitle);
                    requireActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "Тема переименована", Toast.LENGTH_SHORT).show();
                            loadTopics();
                        } else {
                            Toast.makeText(getContext(), "Ошибка переименования", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void confirmDelete(String topicId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить тему?")
                .setMessage("Все сообщения в теме будут удалены безвозвратно.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = PocketBaseClient.deleteTopic(topicId);
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(getContext(), "Тема удалена", Toast.LENGTH_SHORT).show();
                                loadTopics();
                            } else {
                                Toast.makeText(getContext(), "Ошибка удаления (возможно, прошло более 15 минут)", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}