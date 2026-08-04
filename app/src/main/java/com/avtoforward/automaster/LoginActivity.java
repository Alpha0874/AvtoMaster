package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.avtoforward.automaster.utils.SessionManager;
import com.google.gson.JsonObject;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button buttonLogin, buttonToRegister;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            redirectToMain();
            finish();
            return;
        }

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
                    sessionManager.createLoginSession(email, role);
                    Toast.makeText(LoginActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                    Intent serviceIntent = new Intent(LoginActivity.this, ForegroundNotificationService.class);
                    startService(serviceIntent);
                    redirectToMain();
                    finish();
                });
            } else {
                // Проверяем причину через получение данных пользователя
                String userId = PocketBaseClient.getCurrentUserId();
                if (userId != null) {
                    JsonObject user = PocketBaseClient.getUserInfo(userId);
                    if (user != null) {
                        boolean banned = user.has("banned") && user.get("banned").getAsBoolean();
                        boolean verified = user.has("verified") && user.get("verified").getAsBoolean();
                        if (banned) {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Ваш аккаунт заблокирован. Обратитесь к администратору.", Toast.LENGTH_LONG).show());
                            return;
                        } else if (!verified) {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Аккаунт не подтверждён администратором. Дождитесь проверки.", Toast.LENGTH_LONG).show());
                            return;
                        }
                    }
                }
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Ошибка входа. Проверьте email и пароль", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void redirectToMain() {
        String role = sessionManager.getUserRole();
        if ("admin".equals(role)) {
            startActivity(new Intent(LoginActivity.this, AdminActivity.class));
        } else if ("master".equals(role)) {
            startActivity(new Intent(LoginActivity.this, MasterActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}