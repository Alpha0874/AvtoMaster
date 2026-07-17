package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClientProfileEditActivity extends AppCompatActivity {

    private EditText editFullName, editNickname, editPhone, editCity;
    private Button buttonSave;
    private ImageView imageAvatar;
    private Button buttonChangeAvatar;
    private String userId;
    private Uri selectedAvatarUri = null;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_profile_edit);

        userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editFullName = findViewById(R.id.editFullName);
        editNickname = findViewById(R.id.editNickname);
        editPhone = findViewById(R.id.editPhone);
        editCity = findViewById(R.id.editCity);
        buttonSave = findViewById(R.id.buttonSave);
        imageAvatar = findViewById(R.id.imageAvatar);
        buttonChangeAvatar = findViewById(R.id.buttonChangeAvatar);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedAvatarUri = uri;
                            // Загружаем с учётом ориентации
                            loadImageWithOrientation(uri);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && selectedAvatarUri != null) {
                        loadImageWithOrientation(selectedAvatarUri);
                    }
                });

        buttonChangeAvatar.setOnClickListener(v -> showImageSourceDialog());
        buttonSave.setOnClickListener(v -> saveProfile());

        // Кнопка выхода
        Button buttonLogout = findViewById(R.id.buttonLogout);
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> {
                PocketBaseClient.logout();
                Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // Кнопка "Информация о приложении"
        Button buttonAbout = findViewById(R.id.buttonAbout);
        if (buttonAbout != null) {
            buttonAbout.setOnClickListener(v -> {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            });
        }

        loadProfile();
    }

    // ========== НОВЫЙ МЕТОД ДЛЯ ЗАГРУЗКИ ИЗОБРАЖЕНИЯ С УЧЁТОМ ПОВОРОТА ==========
    private void loadImageWithOrientation(Uri uri) {
        try {
            // Определяем ориентацию
            int rotation = 0;
            if (uri.getScheme().equals("content")) {
                // Для content Uri используем ExifInterface через InputStream
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    ExifInterface exif = new ExifInterface(inputStream);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotation = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotation = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotation = 270;
                            break;
                    }
                    inputStream.close();
                }
            } else {
                // Для file Uri (камера) используем путь
                String filePath = uri.getPath();
                if (filePath != null) {
                    ExifInterface exif = new ExifInterface(filePath);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotation = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotation = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotation = 270;
                            break;
                    }
                }
            }

            // Загружаем через Picasso с учётом поворота
            if (rotation != 0) {
                Picasso.get()
                        .load(uri)
                        .rotate(rotation)
                        .centerCrop()
                        .resize(200, 200)
                        .into(imageAvatar);
            } else {
                Picasso.get()
                        .load(uri)
                        .centerCrop()
                        .resize(200, 200)
                        .into(imageAvatar);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // fallback
            imageAvatar.setImageURI(uri);
        }
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите источник")
                .setItems(new String[]{"Галерея", "Камера"}, (dialog, which) -> {
                    if (which == 0) openGallery();
                    else openCamera();
                });
        builder.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void openCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
            return;
        }
        try {
            File photoFile = new File(getCacheDir(), "temp_avatar.jpg");
            selectedAvatarUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedAvatarUri);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка запуска камеры", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfile() {
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user != null) {
                runOnUiThread(() -> {
                    String fullName = getStringValue(user, "full_name");
                    editFullName.setText(fullName);
                    String nickname = getStringValue(user, "nickname");
                    editNickname.setText(nickname);
                    String phone = getStringValue(user, "phone");
                    editPhone.setText(phone);
                    String city = getStringValue(user, "city");
                    editCity.setText(city);

                    if (user.has("avatar") && !user.get("avatar").isJsonNull()) {
                        String avatarUrl = PocketBaseClient.getBaseUrl() + "/api/files/users/" + userId + "/" + user.get("avatar").getAsString();
                        Picasso.get().load(avatarUrl).into(imageAvatar);
                    } else {
                        imageAvatar.setImageResource(R.drawable.ic_role_master);
                    }
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String getStringValue(JsonObject user, String key) {
        if (user.has(key) && !user.get(key).isJsonNull()) {
            return user.get(key).getAsString();
        }
        return "";
    }

    private void saveProfile() {
        String fullName = editFullName.getText().toString().trim();
        String nickname = editNickname.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String city = editCity.getText().toString().trim();

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", fullName);
        data.put("nickname", nickname);
        data.put("phone", phone);
        data.put("city", city);

        if (selectedAvatarUri != null) {
            uploadAvatarThenSave(data);
        } else {
            updateUserData(data);
        }
    }

    private void uploadAvatarThenSave(Map<String, Object> userData) {
        new Thread(() -> {
            try {
                File tempFile = copyUriToTempFile(selectedAvatarUri);
                if (tempFile == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка копирования аватара", Toast.LENGTH_SHORT).show());
                    return;
                }
                boolean success = PocketBaseClient.uploadAvatar(userId, tempFile.getAbsolutePath());
                if (success) {
                    updateUserData(userData);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки аватара", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateUserData(Map<String, Object> data) {
        new Thread(() -> {
            boolean success = PocketBaseClient.updateUser(userId, data);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Профиль сохранён", Toast.LENGTH_SHORT).show();
                    loadProfile();
                } else {
                    Toast.makeText(this, "Ошибка сохранения. Проверьте интернет и права.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private File copyUriToTempFile(Uri uri) {
        try {
            File tempFile = new File(getCacheDir(), "temp_" + System.currentTimeMillis() + ".jpg");
            InputStream is = getContentResolver().openInputStream(uri);
            FileOutputStream os = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            is.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}