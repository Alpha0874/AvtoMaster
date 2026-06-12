package com.avtoforward.automaster;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PremiumActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        TextView textStatus = findViewById(R.id.textPremiumStatus);
        Button buttonBuy = findViewById(R.id.buttonBuyPremium);

        SharedPreferences prefs = getSharedPreferences("AutoMasterPrefs", MODE_PRIVATE);
        boolean isPremium = prefs.getBoolean("is_premium", false);
        textStatus.setText(isPremium ? "Статус: Премиум активен" : "Статус: Бесплатный");

        buttonBuy.setText(isPremium ? "Продлить" : "Купить Премиум (заглушка)");
        buttonBuy.setOnClickListener(v -> {
            // Эмуляция покупки
            prefs.edit().putBoolean("is_premium", true).apply();
            textStatus.setText("Статус: Премиум активен");
            buttonBuy.setText("Продлить");
            Toast.makeText(this, "Премиум активирован!", Toast.LENGTH_SHORT).show();
            // Здесь позже будет интеграция с Google Play Billing
        });
    }
}