package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.fragments.AdminStatsFragment;
import com.avtoforward.automaster.fragments.AdminUsersFragment;
import com.avtoforward.automaster.utils.SessionManager;

public class AdminActivity extends AppCompatActivity {

    private Button btnStats, btnUsers, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        if (!PocketBaseClient.isLoggedIn()) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnStats = findViewById(R.id.btnStats);
        btnUsers = findViewById(R.id.btnVerification);
        btnLogout = findViewById(R.id.btnLogout);

        // Меняем текст кнопки пользователей
        btnUsers.setText("Пользователи");

        if (savedInstanceState == null) {
            showStatsFragment();
        }

        btnStats.setOnClickListener(v -> showStatsFragment());
        btnUsers.setOnClickListener(v -> showUsersFragment());

        btnLogout.setOnClickListener(v -> {
            // Очищаем сессию и выходим
            SessionManager sessionManager = new SessionManager(this);
            sessionManager.logout();
            PocketBaseClient.logout();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showStatsFragment() {
        loadFragment(new AdminStatsFragment());
        btnStats.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark));
        btnUsers.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
    }

    private void showUsersFragment() {
        loadFragment(new AdminUsersFragment());
        btnUsers.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark));
        btnStats.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.adminContainer, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // При нажатии "Назад" выходим из админ-панели и переходим на главный экран
        // в зависимости от роли пользователя
        String role = PocketBaseClient.getUserRole();
        Intent intent;
        if ("admin".equals(role)) {
            // Если админ, можно перейти на MasterActivity (или остаться, но лучше перейти в главное меню)
            intent = new Intent(this, MasterActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}