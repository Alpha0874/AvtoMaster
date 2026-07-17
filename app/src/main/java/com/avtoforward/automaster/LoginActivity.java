package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button buttonLogin, buttonToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonToRegister = findViewById(R.id.buttonToRegister);

        buttonLogin.setOnClickListener(v -> loginUser());
        buttonToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            boolean success = PocketBaseClient.login(email, password);
            if (success) {
                String role = PocketBaseClient.getUserRole();
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                    if ("admin".equals(role)) {
                        startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                    } else if ("master".equals(role)) {
                        startActivity(new Intent(LoginActivity.this, MasterActivity.class));
                    } else {
                        // клиент → MainActivity (старый дизайн с услугами)
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                    finish();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Ошибка входа. Проверьте email и пароль", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}