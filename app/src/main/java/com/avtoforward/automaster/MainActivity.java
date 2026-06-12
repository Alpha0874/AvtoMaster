package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonProfile = findViewById(R.id.buttonProfile);
        buttonProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
        });

        Button buttonChangeRole = findViewById(R.id.buttonChangeRole);
        buttonChangeRole.setOnClickListener(v -> {
            PocketBaseClient.logout();
            Intent intent = new Intent(MainActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.cardElectrician).setOnClickListener(v -> openOrderCreation("Автоэлектрик"));
        findViewById(R.id.cardMechanic).setOnClickListener(v -> openOrderCreation("Автомеханик"));
        findViewById(R.id.cardHydraulic).setOnClickListener(v -> openOrderCreation("Гидравлик"));
        findViewById(R.id.cardTireService).setOnClickListener(v -> openOrderCreation("Шиномонтаж"));
        findViewById(R.id.cardJumpStart).setOnClickListener(v -> openOrderCreation("Прикур-авто"));
        findViewById(R.id.cardTowTruck).setOnClickListener(v -> openOrderCreation("Эвакуатор"));
        findViewById(R.id.cardOther).setOnClickListener(v -> openOrderCreation("Другое"));
    }

    private void openOrderCreation(String service) {
        Intent intent = new Intent(MainActivity.this, OrderCreationActivity.class);
        intent.putExtra("selected_service", service);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PocketBaseClient.isLoggedIn()) {
            new Thread(PocketBaseClient::updateLastOnline).start();
        }
    }
}