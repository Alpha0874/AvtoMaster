package com.avtoforward.automaster;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class AdminMasterDetailActivity extends AppCompatActivity {

    private String userId;
    private TextView textEmail, textVerificationStatus, textRole, textMasterStatus;
    private EditText editFullName, editNickname, editPhone, editCity;
    private Button buttonVerify, buttonReject, buttonSave, buttonDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_master_detail);

        userId = getIntent().getStringExtra("user_id");
        if (userId == null) {
            Toast.makeText(this, "Ошибка: ID пользователя не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Профиль мастера");
        }

        textEmail = findViewById(R.id.textEmail);
        textVerificationStatus = findViewById(R.id.textVerificationStatus);
        textRole = findViewById(R.id.textRole);
        textMasterStatus = findViewById(R.id.textMasterStatus);
        editFullName = findViewById(R.id.editFullName);
        editNickname = findViewById(R.id.editNickname);
        editPhone = findViewById(R.id.editPhone);
        editCity = findViewById(R.id.editCity);
        buttonVerify = findViewById(R.id.buttonVerify);
        buttonReject = findViewById(R.id.buttonReject);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);

        loadUserData();

        buttonVerify.setOnClickListener(v -> updateVerificationStatus("verified"));
        buttonReject.setOnClickListener(v -> updateVerificationStatus("rejected"));
        buttonSave.setOnClickListener(v -> saveUserData());
        buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void loadUserData() {
        new Thread(() -> {
            JsonObject user = PocketBaseClient.getUserInfo(userId);
            if (user != null) {
                runOnUiThread(() -> {
                    String email = user.has("email") ? user.get("email").getAsString() : "не указан";
                    textEmail.setText(email);

                    String fullName = getStringValue(user, "full_name");
                    editFullName.setText(fullName);

                    String nickname = getStringValue(user, "nickname");
                    editNickname.setText(nickname);

                    String phone = getStringValue(user, "phone");
                    editPhone.setText(phone);

                    String city = getStringValue(user, "city");
                    editCity.setText(city);

                    String verification = getStringValue(user, "verification_status");
                    if ("verified".equals(verification)) {
                        textVerificationStatus.setText("Подтверждён ✓");
                        textVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.switch_thumb_on));
                    } else if ("pending".equals(verification)) {
                        textVerificationStatus.setText("На проверке...");
                        textVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    } else {
                        textVerificationStatus.setText("Не проверен");
                        textVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                    }

                    String role = getStringValue(user, "role");
                    textRole.setText(role);

                    String masterStatus = getStringValue(user, "master_status");
                    textMasterStatus.setText(masterStatus.isEmpty() ? "Не указан" : masterStatus);
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String getStringValue(JsonObject user, String key) {
        if (user.has(key) && !user.get(key).isJsonNull()) {
            return user.get(key).getAsString();
        }
        return "";
    }

    private void updateVerificationStatus(String status) {
        new Thread(() -> {
            boolean success = PocketBaseClient.updateVerificationStatus(userId, status);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Статус обновлён: " + status, Toast.LENGTH_SHORT).show();
                    loadUserData();
                } else {
                    Toast.makeText(this, "Ошибка обновления статуса", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void saveUserData() {
        String fullName = editFullName.getText().toString().trim();
        String nickname = editNickname.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String city = editCity.getText().toString().trim();

        Map<String, Object> data = new HashMap<>();
        data.put("full_name", fullName);
        data.put("nickname", nickname);
        data.put("phone", phone);
        data.put("city", city);

        new Thread(() -> {
            boolean success = PocketBaseClient.updateUser(userId, data);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
                    loadUserData();
                } else {
                    Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить мастера?")
                .setMessage("Все данные мастера будут удалены без возможности восстановления.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = PocketBaseClient.deleteUser(userId);
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Мастер удалён", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}