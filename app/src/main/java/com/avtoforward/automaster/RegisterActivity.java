package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editConfirmPassword;
    private RadioGroup radioGroupRole;
    private RadioButton radioClient, radioMaster;
    private Button buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        radioClient = findViewById(R.id.radioClient);
        radioMaster = findViewById(R.id.radioMaster);
        buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirm = editConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = radioGroupRole.getCheckedRadioButtonId();
        String role;
        if (selectedId == R.id.radioClient) {
            role = "user";
        } else if (selectedId == R.id.radioMaster) {
            role = "master";
        } else {
            Toast.makeText(this, "Выберите роль", Toast.LENGTH_SHORT).show();
            return;
        }

        // Регистрация через PocketBaseClient
        new Thread(() -> {
            boolean success = PocketBaseClient.register(email, password, confirm);
            if (success) {
                // После успешной регистрации нужно обновить роль пользователя
                String userId = PocketBaseClient.getCurrentUserId();
                if (userId != null) {
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("role", role);
                    PocketBaseClient.updateUser(userId, data);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                    // Перенаправляем в зависимости от роли
                    if ("master".equals(role)) {
                        startActivity(new Intent(this, MasterActivity.class));
                    } else {
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка регистрации. Возможно, email уже существует", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}