package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditMasterProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileEdit";

    private EditText editFullName, editNickname, editServiceName, editPhone, editCity;
    private RadioGroup radioGroupCorporate;
    private Spinner spinnerStatus;
    private RadioGroup radioGroupEquipment;
    private Button buttonSave;
    private ImageView imageAvatar;
    private Button buttonChangeAvatar;
    private TextView textVerificationStatus;
    private Button buttonUploadPassport;
    private String userId;
    private Uri selectedAvatarUri = null;
    private File passportPhotoFile = null;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> cameraPassportLauncher;

    // Для управления видимостью блоков мастера
    private View groupCorporate, groupStatus, groupEquipment, groupVerification;

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
        radioGroupCorporate = findViewById(R.id.radioGroupCorporate);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        radioGroupEquipment = findViewById(R.id.radioGroupEquipment);
        buttonSave = findViewById(R.id.buttonSave);
        imageAvatar = findViewById(R.id.imageAvatar);
        buttonChangeAvatar = findViewById(R.id.buttonChangeAvatar);
        textVerificationStatus = findViewById(R.id.textVerificationStatus);
        buttonUploadPassport = findViewById(R.id.buttonUploadPassport);

        // Находим группы для скрытия (оборачиваем в View, чтобы скрыть целиком)
        groupCorporate = findViewById(R.id.groupCorporate);
        groupStatus = findViewById(R.id.groupStatus);
        groupEquipment = findViewById(R.id.groupEquipment);
        groupVerification = findViewById(R.id.groupVerification);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.master_statuses, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

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

        cameraPassportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && passportPhotoFile != null && passportPhotoFile.exists()) {
                        uploadPassportPhoto(Uri.fromFile(passportPhotoFile));
                    } else {
                        Toast.makeText(this, "Фото не получено", Toast.LENGTH_SHORT).show();
                    }
                });

        buttonChangeAvatar.setOnClickListener(v -> showImageSourceDialog());
        buttonSave.setOnClickListener(v -> saveProfile());
        buttonUploadPassport.setOnClickListener(v -> openCameraForPassport());

        // Кнопка выхода - ИСПРАВЛЕННАЯ
        Button buttonLogout = findViewById(R.id.buttonLogout);
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> {
                // Вызываем единый метод выхода, который очистит всё и перенаправит
                PocketBaseClient.logout();
                // Закрываем эту Activity, потому что logout() уже перезапускает приложение
                finish();
            });
        }

        // Кнопка "Информация о приложении"
        Button buttonAbout = findViewById(R.id.buttonAbout);
        if (buttonAbout != null) {
            buttonAbout.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.avtoforward.automaster.AboutActivity.class);
                startActivity(intent);
            });
        }

        loadProfile();
    }

    // ========== ОСТАЛЬНЫЕ МЕТОДЫ (без изменений) ==========

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

    private void openCameraForPassport() {
        new AlertDialog.Builder(this)
                .setTitle("Фото для верификации")
                .setMessage("Сделайте селфи с разворотом паспорта.\n\n"
                        + "В кадре должны быть:\n"
                        + "• Ваше лицо\n"
                        + "• Разворот паспорта с фото и данными\n\n"
                        + "Фото будет проверено администратором.")
                .setPositiveButton("Понятно, сделать фото", (dialog, which) -> {
                    if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 102);
                        return;
                    }
                    try {
                        passportPhotoFile = new File(getCacheDir(), "passport_photo_" + System.currentTimeMillis() + ".jpg");
                        Uri photoUri = FileProvider.getUriForFile(this,
                                getPackageName() + ".fileprovider", passportPhotoFile);
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        cameraPassportLauncher.launch(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка запуска камеры", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadProfile() {
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user != null) {
                runOnUiThread(() -> {
                    // Определяем роль
                    String role = user.has("role") && !user.get("role").isJsonNull()
                            ? user.get("role").getAsString()
                            : "user";
                    boolean isMaster = "master".equals(role) || "admin".equals(role);

                    // Скрываем блоки мастера, если пользователь — клиент
                    if (groupCorporate != null) {
                        groupCorporate.setVisibility(isMaster ? View.VISIBLE : View.GONE);
                    }
                    if (groupStatus != null) {
                        groupStatus.setVisibility(isMaster ? View.VISIBLE : View.GONE);
                    }
                    if (groupEquipment != null) {
                        groupEquipment.setVisibility(isMaster ? View.VISIBLE : View.GONE);
                    }
                    if (groupVerification != null) {
                        groupVerification.setVisibility(isMaster ? View.VISIBLE : View.GONE);
                    }

                    // ФИО
                    String fullName = getStringValue(user, "full_name");
                    editFullName.setText(fullName);
                    // Никнейм
                    String nickname = getStringValue(user, "nickname");
                    editNickname.setText(nickname);
                    // Название сервиса
                    String serviceName = getStringValue(user, "service_name");
                    editServiceName.setText(serviceName);
                    // Телефон
                    String phone = getStringValue(user, "phone");
                    editPhone.setText(phone);
                    // Город
                    String city = getStringValue(user, "city");
                    editCity.setText(city);

                    // Аватар
                    if (user.has("avatar") && !user.get("avatar").isJsonNull()) {
                        String avatarUrl = PocketBaseClient.getBaseUrl() + "/api/files/users/" + userId + "/" + user.get("avatar").getAsString();
                        Picasso.get().load(avatarUrl).into(imageAvatar);
                    } else {
                        imageAvatar.setImageResource(R.drawable.ic_role_master);
                    }

                    // Статус верификации (показываем только для мастеров)
                    if (isMaster) {
                        String verificationStatus = getStringValue(user, "verification_status");
                        if ("verified".equals(verificationStatus)) {
                            textVerificationStatus.setText("Статус: подтверждён ✓");
                            textVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.switch_thumb_on));
                        } else if ("pending".equals(verificationStatus)) {
                            textVerificationStatus.setText("Статус: на проверке...");
                            textVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                        } else {
                            textVerificationStatus.setText("Статус: не проверен");
                            textVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                        }
                    }

                    // Corporate ready (только для мастеров)
                    if (isMaster) {
                        String corp = getStringValue(user, "corporate_ready");
                        if ("yes".equals(corp)) {
                            radioGroupCorporate.check(R.id.radioCorporateYes);
                        } else {
                            radioGroupCorporate.check(R.id.radioCorporateNo);
                        }
                    }

                    // Master status (только для мастеров)
                    if (isMaster) {
                        String masterStatus = getStringValue(user, "master_status");
                        if (!masterStatus.isEmpty()) {
                            int pos = ((ArrayAdapter) spinnerStatus.getAdapter()).getPosition(masterStatus);
                            if (pos >= 0) spinnerStatus.setSelection(pos);
                        } else {
                            spinnerStatus.setSelection(0);
                        }
                        Log.d(TAG, "Загружен статус: " + masterStatus);
                    }

                    // Наличие сканера (только для мастеров)
                    if (isMaster) {
                        String scanner = getStringValue(user, "has_scanner");
                        if ("yes".equals(scanner)) {
                            radioGroupEquipment.check(R.id.radioEquipmentYes);
                        } else {
                            radioGroupEquipment.check(R.id.radioEquipmentNo);
                        }
                    }
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Вспомогательный метод: возвращает строку, если поле существует и не null, иначе пустую строку
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

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", fullName);
        data.put("nickname", nickname);
        data.put("service_name", serviceName);
        data.put("phone", phone);
        data.put("city", city);

        // Для мастеров сохраняем дополнительные поля
        String role = PocketBaseClient.getUserRole();
        if ("master".equals(role) || "admin".equals(role)) {
            int corpCheckedId = radioGroupCorporate.getCheckedRadioButtonId();
            String corporateReady = (corpCheckedId == R.id.radioCorporateYes) ? "yes" : "no";
            data.put("corporate_ready", corporateReady);

            String masterStatus = spinnerStatus.getSelectedItem().toString();
            data.put("master_status", masterStatus);

            int equipCheckedId = radioGroupEquipment.getCheckedRadioButtonId();
            String hasScanner = (equipCheckedId == R.id.radioEquipmentYes) ? "yes" : "no";
            data.put("has_scanner", hasScanner);
        }

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

    private void uploadPassportPhoto(Uri uri) {
        new Thread(() -> {
            try {
                File tempFile = copyUriToTempFile(uri);
                if (tempFile == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка копирования фото", Toast.LENGTH_SHORT).show());
                    return;
                }
                boolean success = PocketBaseClient.uploadPassportPhoto(userId, tempFile.getAbsolutePath());
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "Фото паспорта загружено. Статус: на проверке.", Toast.LENGTH_LONG).show();
                        loadProfile();
                    } else {
                        Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
            e.printStackTrace();
            return null;
        }
    }
}