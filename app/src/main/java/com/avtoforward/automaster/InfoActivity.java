package com.avtoforward.automaster;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("О приложении");

        TextView textInfo = findViewById(R.id.textAppInfo);
        String version = "1.0"; // временно, пока BuildConfig не работает
        // Если хотите использовать BuildConfig, добавьте импорт и убедитесь, что build.gradle содержит versionName
        // String version = BuildConfig.VERSION_NAME;
        textInfo.setText("АвтоМастер\nВерсия: " + version + "\n\nПриложение для вызова автоэлектрика, автомеханика и гидравлика.\n\nРазработчик: ООО «АвтоФорвард»\n© 2026");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}