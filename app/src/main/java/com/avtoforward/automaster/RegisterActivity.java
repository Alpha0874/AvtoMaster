package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editPasswordConfirm, editNickname;
    private Button buttonRegister, buttonToLogin;
    private String selectedRole = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getIntent() != null) {
            selectedRole = getIntent().getStringExtra("selected_role");
            if (selectedRole == null || selectedRole.isEmpty()) {
                selectedRole = "user";
            }
        }

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editPasswordConfirm = findViewById(R.id.editPasswordConfirm);
        editNickname = findViewById(R.id.editNickname);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonToLogin = findViewById(R.id.buttonToLogin);

        buttonRegister.setOnClickListener(v -> registerUser());
        buttonToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String passwordConfirm = editPasswordConfirm.getText().toString().trim();
        String nickname = editNickname.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            boolean success = PocketBaseClient.register(email, password, passwordConfirm, nickname, selectedRole);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(RegisterActivity.this, "Регистрация успешна! Дождитесь подтверждения администратором.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Ошибка регистрации. Возможно, email уже используется.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}