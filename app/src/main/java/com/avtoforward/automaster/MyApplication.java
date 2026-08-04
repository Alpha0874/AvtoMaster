package com.avtoforward.automaster;

import android.app.Application;
import android.util.Log;

import com.avtoforward.automaster.utils.TimeTracker;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Инициализация PocketBase
            PocketBaseClient.init(this);
            Log.d(TAG, "PocketBaseClient initialized");

            // Инициализация трекера времени (сбор статистики использования)
            TimeTracker.getInstance(this);
            Log.d(TAG, "TimeTracker initialized");
        } catch (Exception e) {
            Log.e(TAG, "Initialization failed", e);
        }
    }
}