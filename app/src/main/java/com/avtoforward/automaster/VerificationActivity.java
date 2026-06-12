package com.avtoforward.automaster;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class VerificationActivity extends AppCompatActivity {

    private Button buttonUploadPhoto, buttonContinue;
    private TextView textStatus;
    private Uri selectedPhotoUri; // Ссылка на выбранное фото (в реальном приложении загружаем на сервер)
    private boolean photoChosen = false;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedPhotoUri = result.getData().getData();
                    photoChosen = true;
                    textStatus.setText("Фото выбрано");
                    buttonContinue.setEnabled(true);
                    buttonContinue.setAlpha(1.0f);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        buttonUploadPhoto = findViewById(R.id.buttonUploadPhoto);
        buttonContinue = findViewById(R.id.buttonContinue);
        textStatus = findViewById(R.id.textStatus);

        buttonUploadPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        buttonContinue.setOnClickListener(v -> {
            if (!photoChosen) {
                Toast.makeText(this, "Сначала выберите фото", Toast.LENGTH_SHORT).show();
                return;
            }

            // Сохраняем статус "pending" в профиль пользователя
            String userId = PocketBaseClient.getCurrentUserId();
            Map<String, Object> data = new HashMap<>();
            data.put("verification_status", "pending");

            new Thread(() -> {
                boolean updated = PocketBaseClient.updateUser(userId, data);
                runOnUiThread(() -> {
                    if (updated) {
                        Toast.makeText(VerificationActivity.this, "Фото отправлено на проверку", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(VerificationActivity.this, MasterActivity.class));
                        finish();
                    } else {
                        Toast.makeText(VerificationActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
    }
}