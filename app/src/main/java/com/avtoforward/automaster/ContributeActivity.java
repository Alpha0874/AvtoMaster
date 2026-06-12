package com.avtoforward.automaster;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ContributeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contribute);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Участвовать в развитии");

        Button buttonDonate = findViewById(R.id.buttonDonate);
        Button buttonFeedback = findViewById(R.id.buttonFeedback);

        buttonDonate.setOnClickListener(v -> {
            // Ссылка на донат (замените на реальную)
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.donationalerts.com/r/automaster"));
            startActivity(intent);
        });

        buttonFeedback.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@automaster.ru"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Предложение по улучшению");
            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}