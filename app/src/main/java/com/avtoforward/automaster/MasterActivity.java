package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.fragments.MenuFragment;

public class MasterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new MenuFragment())
                    .commit();
        }

        // ✅ ЗАПУСКАЕМ СЕРВИС УВЕДОМЛЕНИЙ
        Intent serviceIntent = new Intent(this, ForegroundNotificationService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PocketBaseClient.isLoggedIn()) {
            new Thread(PocketBaseClient::updateLastOnline).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Останавливаем сервис уведомлений при уничтожении активности
        Intent serviceIntent = new Intent(this, ForegroundNotificationService.class);
        stopService(serviceIntent);
    }
}