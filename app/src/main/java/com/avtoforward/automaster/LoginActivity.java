package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.avtoforward.automaster.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button buttonLogin, buttonToRegister;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Если уже залогинены — сразу переходим в главное меню
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

                // Сохраняем сессию
                sessionManager.createLoginSession(email, role);

                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();

                    // Запускаем сервис уведомлений
                    Intent serviceIntent = new Intent(LoginActivity.this, ForegroundNotificationService.class);
                    startService(serviceIntent);

                    redirectToMain();
                    finish();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Ошибка входа. Проверьте email и пароль", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Метод для перенаправления в зависимости от роли
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

    // ✅ Обработка нажатия кнопки "Назад" – возврат на экран выбора роли
    @Override
    public void onBackPressed() {
        // Переходим на экран выбора роли (очищая стек, чтобы нельзя было вернуться назад в LoginActivity)
        Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}