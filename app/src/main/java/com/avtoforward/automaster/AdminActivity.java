package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.fragments.AdminCreateOrderFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle("Администрирование");

        bottomNavigation = findViewById(R.id.bottomNavigationAdmin);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_verification) {
                selectedFragment = new AdminVerificationFragment();
            } else if (itemId == R.id.nav_masters) {
                selectedFragment = new AdminMastersFragment();
            } else if (itemId == R.id.nav_orders) {
                selectedFragment = new AdminOrdersFragment();
            } else if (itemId == R.id.nav_create_order) {
                selectedFragment = new AdminCreateOrderFragment();
            } else if (itemId == R.id.nav_stats) {
                selectedFragment = new AdminStatsFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.adminContainer, selectedFragment)
                        .commit();
            }
            return true;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_verification);

        // Проверяем роль в фоновом потоке
        new Thread(() -> {
            if (!PocketBaseClient.isLoggedIn()) {
                runOnUiThread(this::goToLogin);
                return;
            }
            String role = PocketBaseClient.getUserRole();
            if (!"admin".equals(role)) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Доступ только для администратора", Toast.LENGTH_SHORT).show();
                    goToLogin();
                });
            }
        }).start();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            goToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Просто вызываем goToLogin()
        goToLogin();
    }
}