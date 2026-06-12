package com.avtoforward.automaster;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Техподдержка");

        TextView textInfo = findViewById(R.id.textSupportInfo);
        textInfo.setText("По всем вопросам обращайтесь:\n\nEmail: support@automaster.ru\nТелефон: +7 (999) 123-45-67\n\nРежим работы: Пн-Пт 10:00-19:00");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}