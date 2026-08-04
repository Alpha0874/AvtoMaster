package com.avtoforward.automaster;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });

        // Проверяем только авторизацию
        if (!PocketBaseClient.isLoggedIn()) {
            Log.w(TAG, "Пользователь не авторизован");
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.putExtra("role", "Мастер");
            startActivity(loginIntent);
            finish();
            return;
        }

        // Открываем ForumKnowledgeFragment (как было раньше)
        if (savedInstanceState == null) {
            Fragment fragment = new ForumKnowledgeFragment();
            replaceFragmentSafely(fragment, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Обрабатывается через OnBackPressedDispatcher
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

    // Метод для открытия чата (используется из других фрагментов)
    public void openChat(String topicId, String topicTitle) {
        try {
            com.avtoforward.automaster.fragments.ForumChatFragment fragment =
                    new com.avtoforward.automaster.fragments.ForumChatFragment(topicId, topicTitle);
            replaceFragmentSafely(fragment, true);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при открытии чата: " + e.getMessage(), e);
            Toast.makeText(this, "Не удалось открыть чат. Попробуйте позже.", Toast.LENGTH_LONG).show();
        }
    }

    // Метод для открытия чата по ID (с подгрузкой названия)
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