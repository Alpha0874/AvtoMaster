package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private EditText editEmail, editPassword, editPasswordConfirm, editNickname;
    private RadioGroup radioGroupRole;
    private Button buttonRegister, buttonToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editPasswordConfirm = findViewById(R.id.editConfirmPassword);
        editNickname = findViewById(R.id.editNickname); // новое поле
        radioGroupRole = findViewById(R.id.radioGroupRole);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonToLogin = findViewById(R.id.buttonToLogin);

        RadioButton radioClient = findViewById(R.id.radioClient);
        if (radioClient != null) {
            radioClient.setText("Заказчик");
        }

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

        if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || nickname.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();
        String role = "user";
        if (selectedRoleId == R.id.radioMaster) {
            role = "master";
        } else if (selectedRoleId == R.id.radioClient) {
            role = "user";
        }

        final String finalRole = role;

        buttonRegister.setEnabled(false);
        buttonRegister.setText("Регистрация...");

        new Thread(() -> {
            try {
                // Регистрация с никнеймом (передаём nickname)
                boolean registerSuccess = PocketBaseClient.register(email, password, passwordConfirm, email.split("@")[0]);
                if (!registerSuccess) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this,
                                "Ошибка регистрации. Проверьте email (уникальный) и пароль (минимум 8 символов).",
                                Toast.LENGTH_LONG).show();
                        buttonRegister.setEnabled(true);
                        buttonRegister.setText("Зарегистрироваться");
                    });
                    return;
                }

                String userId = PocketBaseClient.getCurrentUserId();
                if (userId == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this,
                                "Ошибка авторизации после регистрации", Toast.LENGTH_SHORT).show();
                        buttonRegister.setEnabled(true);
                        buttonRegister.setText("Зарегистрироваться");
                    });
                    return;
                }

                // Обновляем роль
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("role", finalRole);
                boolean updateSuccess = PocketBaseClient.updateUser(userId, data);

                runOnUiThread(() -> {
                    if (updateSuccess) {
                        Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                        if ("admin".equals(finalRole)) {
                            startActivity(new Intent(RegisterActivity.this, AdminActivity.class));
                        } else if ("master".equals(finalRole)) {
                            startActivity(new Intent(RegisterActivity.this, MasterActivity.class));
                        } else {
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Ошибка сохранения роли. Обратитесь в поддержку.", Toast.LENGTH_LONG).show();
                        buttonRegister.setEnabled(true);
                        buttonRegister.setText("Зарегистрироваться");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Ошибка регистрации", e);
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this,
                            "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    buttonRegister.setEnabled(true);
                    buttonRegister.setText("Зарегистрироваться");
                });
            }
        }).start();
    }
}