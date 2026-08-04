package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditMasterProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileEdit";

    private EditText editFullName, editNickname, editServiceName, editPhone, editCity;
    private EditText editExperienceYears;
    private RadioGroup radioGroupCorporate;
    private Button buttonSave;
    private ImageView imageAvatar;
    private Button buttonChangeAvatar;
    private String userId;
    private Uri selectedAvatarUri = null;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    // Трансформация для Picasso – круглый аватар
    private final Transformation circleTransform = new Transformation() {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);
            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_master_profile);

        userId = PocketBaseClient.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editFullName = findViewById(R.id.editFullName);
        editNickname = findViewById(R.id.editNickname);
        editServiceName = findViewById(R.id.editServiceName);
        editPhone = findViewById(R.id.editPhone);
        editCity = findViewById(R.id.editCity);
        editExperienceYears = findViewById(R.id.editExperienceYears);
        radioGroupCorporate = findViewById(R.id.radioGroupCorporate);
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
                            imageAvatar.setImageURI(uri);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && selectedAvatarUri != null) {
                        imageAvatar.setImageURI(selectedAvatarUri);
                    }
                });

        buttonChangeAvatar.setOnClickListener(v -> showImageSourceDialog());
        buttonSave.setOnClickListener(v -> saveProfile());

        Button buttonLogout = findViewById(R.id.buttonLogout);
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> {
                PocketBaseClient.logout();
                Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        Button buttonAbout = findViewById(R.id.buttonAbout);
        if (buttonAbout != null) {
            buttonAbout.setOnClickListener(v -> {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            });
        }

        loadProfile();
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
            try {
                JsonObject user = PocketBaseClient.getUserInfo(userId);
                if (user != null) {
                    runOnUiThread(() -> {
                        String role = user.has("role") && !user.get("role").isJsonNull()
                                ? user.get("role").getAsString()
                                : "user";
                        boolean isMaster = "master".equals(role) || "admin".equals(role);

                        editFullName.setText(getStringValue(user, "full_name"));
                        editNickname.setText(getStringValue(user, "nickname"));
                        editServiceName.setText(getStringValue(user, "service_name"));
                        editPhone.setText(getStringValue(user, "phone"));
                        editCity.setText(getStringValue(user, "city"));
                        editExperienceYears.setText(getStringValue(user, "master_status"));

                        if (user.has("avatar") && !user.get("avatar").isJsonNull()) {
                            String avatarUrl = PocketBaseClient.getBaseUrl() + "/api/files/users/" + userId + "/" + user.get("avatar").getAsString();
                            Picasso.get()
                                    .load(avatarUrl)
                                    .transform(circleTransform)
                                    .placeholder(android.R.drawable.ic_menu_edit)
                                    .error(android.R.drawable.ic_menu_edit)
                                    .into(imageAvatar);
                        } else {
                            imageAvatar.setImageResource(android.R.drawable.ic_menu_edit);
                        }

                        if (isMaster) {
                            String corp = getStringValue(user, "corporate_ready");
                            if ("yes".equals(corp)) {
                                radioGroupCorporate.check(R.id.radioCorporateYes);
                            } else {
                                radioGroupCorporate.check(R.id.radioCorporateNo);
                            }
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки профиля", e);
                runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show());
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
        String serviceName = editServiceName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String city = editCity.getText().toString().trim();
        String experienceYears = editExperienceYears.getText().toString().trim();

        if (fullName.isEmpty() || nickname.isEmpty() || phone.isEmpty() || city.isEmpty() || experienceYears.isEmpty()) {
            Toast.makeText(this, "Заполните все поля, отмеченные *", Toast.LENGTH_LONG).show();
            return;
        }

        // Получаем роль в фоновом потоке
        new Thread(() -> {
            String role = PocketBaseClient.getUserRole();
            runOnUiThread(() -> {
                buttonSave.setEnabled(false);
                buttonSave.setText("Сохранение...");

                Map<String, Object> data = new HashMap<>();
                data.put("full_name", fullName);
                data.put("nickname", nickname);
                data.put("service_name", serviceName);
                data.put("phone", phone);
                data.put("city", city);
                data.put("master_status", experienceYears);

                if ("master".equals(role) || "admin".equals(role)) {
                    int corpCheckedId = radioGroupCorporate.getCheckedRadioButtonId();
                    String corporateReady = (corpCheckedId == R.id.radioCorporateYes) ? "yes" : "no";
                    data.put("corporate_ready", corporateReady);
                }

                if (selectedAvatarUri != null) {
                    uploadAvatarThenSave(data);
                } else {
                    updateUserData(data);
                }
            });
        }).start();
    }

    private void uploadAvatarThenSave(Map<String, Object> userData) {
        new Thread(() -> {
            try {
                File tempFile = copyUriToTempFile(selectedAvatarUri);
                if (tempFile == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Ошибка копирования аватара", Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                        buttonSave.setText("Сохранить");
                    });
                    return;
                }
                boolean success = PocketBaseClient.uploadAvatar(userId, tempFile.getAbsolutePath());
                if (success) {
                    updateUserData(userData);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Ошибка загрузки аватара", Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                        buttonSave.setText("Сохранить");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки аватара", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonSave.setEnabled(true);
                    buttonSave.setText("Сохранить");
                });
            }
        }).start();
    }

    private void updateUserData(Map<String, Object> data) {
        new Thread(() -> {
            try {
                boolean success = PocketBaseClient.updateUser(userId, data);
                runOnUiThread(() -> {
                    buttonSave.setEnabled(true);
                    buttonSave.setText("Сохранить");
                    if (success) {
                        Toast.makeText(this, "Профиль сохранён", Toast.LENGTH_SHORT).show();
                        loadProfile();
                        selectedAvatarUri = null;
                    } else {
                        Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения", e);
                runOnUiThread(() -> {
                    buttonSave.setEnabled(true);
                    buttonSave.setText("Сохранить");
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
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
            Log.e(TAG, "Ошибка копирования файла", e);
            return null;
        }
    }
}