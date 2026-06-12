package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    private TextView textCompletedCount, textTotalEarnings, textAverageRating;
    private MaterialCardView cardCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Статистика");

        textCompletedCount = findViewById(R.id.textCompletedCount);
        textTotalEarnings = findViewById(R.id.textTotalEarnings);
        textAverageRating = findViewById(R.id.textAverageRating);
        cardCompleted = findViewById(R.id.cardCompleted);

        cardCompleted.setOnClickListener(v -> {
            Intent intent = new Intent(StatisticsActivity.this, CompletedOrdersActivity.class);
            startActivity(intent);
        });

        loadStatistics();
    }

    private void loadStatistics() {
        String masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(() -> {
            List<com.avtoforward.automaster.Order> completedOrders = PocketBaseClient.getCompletedOrders(masterId);
            int count = completedOrders.size();
            int total = 0;
            for (com.avtoforward.automaster.Order order : completedOrders) {
                total += order.getPrice();
            }
            double averageRating = 5.0; // заглушка
            final int finalCount = count;
            final int finalTotal = total;
            runOnUiThread(() -> {
                textCompletedCount.setText(String.valueOf(finalCount));
                textTotalEarnings.setText(finalTotal + " ₽");
                textAverageRating.setText(String.format("%.1f ★", averageRating));
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}