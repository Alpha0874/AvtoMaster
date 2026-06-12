package com.avtoforward.automaster;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Загружаем сохранённую тему
            SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
            int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            AppCompatDelegate.setDefaultNightMode(themeMode);

            PocketBaseClient.init(this);
            Log.d(TAG, "Initialization successful");
        } catch (Exception e) {
            Log.e(TAG, "Initialization failed", e);
        }
    }
}