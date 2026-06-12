package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.JsonObject;

public class ForumActivity extends AppCompatActivity {

    private static final String TAG = "ForumActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Форум");
        }

        // Слушатель для смены заголовка при переходе между фрагментами
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
            if (current instanceof ForumKnowledgeFragment) {
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Форум");
            } else if (current instanceof ForumTopicsFragment) {
                // Заголовок установится в самом фрагменте, но можно оставить
            } else if (current instanceof ForumChatFragment) {
                // Заголовок установится в фрагменте
            } else {
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Форум");
            }
        });

        if (!PocketBaseClient.isLoggedIn()) {
            Log.w(TAG, "Токен отсутствует → переход на LoginActivity");
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.putExtra("role", "Мастер");
            startActivity(loginIntent);
            finish();
            return;
        }

        String userId = PocketBaseClient.getCurrentUserId();
        if (userId != null) {
            new Thread(() -> {
                String verificationStatus = PocketBaseClient.getVerificationStatus(userId);
                runOnUiThread(() -> {
                    if (!"verified".equals(verificationStatus)) {
                        Toast.makeText(this, "Доступ к форуму после подтверждения личности. Загрузите фото паспорта в профиле.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        if (savedInstanceState == null) {
                            replaceFragmentSafely(new ForumKnowledgeFragment(), false);
                        }
                    }
                });
            }).start();
        } else {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PocketBaseClient.isLoggedIn()) {
            new Thread(PocketBaseClient::updateLastForumActivity).start();
        }
    }

    public void openTopicsForSubcategory(String subcategoryId, String subcategoryName) {
        replaceFragmentSafely(new ForumTopicsFragment(subcategoryId, subcategoryName), true);
    }

    public void openChat(String topicId, String topicTitle) {
        try {
            replaceFragmentSafely(new ForumChatFragment(topicId, topicTitle), true);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при открытии чата: " + e.getMessage(), e);
            Toast.makeText(this, "Не удалось открыть чат. Попробуйте позже.", Toast.LENGTH_LONG).show();
        }
    }

    public void openChat(String topicId) {
        new Thread(() -> {
            String title = topicId;
            JsonObject topic = PocketBaseClient.getTopicById(topicId);
            if (topic != null && topic.has("title")) {
                title = topic.get("title").getAsString();
            }
            String finalTitle = title;
            runOnUiThread(() -> openChat(topicId, finalTitle));
        }).start();
    }

    private void replaceFragmentSafely(Fragment fragment, boolean addToBackStack) {
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment);
            if (addToBackStack) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Ошибка транзакции фрагмента", e);
            Toast.makeText(this, "Ошибка интерфейса", Toast.LENGTH_SHORT).show();
        }
    }
}