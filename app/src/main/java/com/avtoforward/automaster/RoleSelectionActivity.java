package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class RoleSelectionActivity extends AppCompatActivity {

    private MaterialCardView cardCorporate, cardUser, cardMaster, cardTowTruck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        cardCorporate = findViewById(R.id.cardCorporate);
        cardUser = findViewById(R.id.cardUser);
        cardMaster = findViewById(R.id.cardMaster);
        cardTowTruck = findViewById(R.id.cardTowTruck);

        cardCorporate.setOnClickListener(v -> selectRole("corporate"));
        cardUser.setOnClickListener(v -> selectRole("user"));
        cardMaster.setOnClickListener(v -> selectRole("master"));
        cardTowTruck.setOnClickListener(v -> selectRole("tow_truck"));
    }

    private void selectRole(String role) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putString("selected_role", role)
                .apply();

        Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
        intent.putExtra("selected_role", role);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Проверяем, залогинен ли пользователь (в фоновом потоке)
        new Thread(() -> {
            if (PocketBaseClient.isLoggedIn()) {
                String role = PocketBaseClient.getUserRole();
                runOnUiThread(() -> redirectByRole(role));
            }
        }).start();
    }

    private void redirectByRole(String role) {
        if ("admin".equals(role)) {
            startActivity(new Intent(this, AdminActivity.class));
        } else if ("master".equals(role)) {
            startActivity(new Intent(this, MasterActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}