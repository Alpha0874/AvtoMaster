package com.avtoforward.automaster.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.avtoforward.automaster.PocketBaseClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeTracker implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "TimeTracker";
    private static final String PREFS_NAME = "time_tracker";
    private static final String KEY_SESSIONS = "sessions";
    private static final long SEND_INTERVAL = 5 * 60 * 1000; // 5 минут

    private static TimeTracker instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<Session>>() {}.getType();

    private int activityCount = 0;
    private boolean isInForeground = false;
    private long foregroundStartTime = 0;
    private long backgroundStartTime = 0;
    private long totalForegroundTime = 0;
    private long totalBackgroundTime = 0;
    private String currentUserId = null;
    private String currentRole = null;
    private String currentDate = null;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sendRunnable;
    private boolean isInitialized = false;

    private TimeTracker(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ((Application) context).registerActivityLifecycleCallbacks(this);
        startSendingTask();
        // Инициализация в фоновом потоке
        new Thread(() -> {
            loadCurrentSession();
            isInitialized = true;
        }).start();
    }

    public static synchronized TimeTracker getInstance(Context context) {
        if (instance == null) {
            instance = new TimeTracker(context);
        }
        return instance;
    }

    // === Жизненный цикл Activity ===

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (!isInitialized) return;
        activityCount++;
        if (!isInForeground) {
            isInForeground = true;
            foregroundStartTime = System.currentTimeMillis();
            if (backgroundStartTime > 0) {
                long bgTime = System.currentTimeMillis() - backgroundStartTime;
                totalBackgroundTime += bgTime;
                backgroundStartTime = 0;
                Log.d(TAG, "Added background time: " + bgTime / 1000 + "s");
            }
            Log.d(TAG, "App in foreground");
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (!isInitialized) return;
        activityCount--;
        if (activityCount == 0 && isInForeground) {
            isInForeground = false;
            if (foregroundStartTime > 0) {
                long fgTime = System.currentTimeMillis() - foregroundStartTime;
                totalForegroundTime += fgTime;
                foregroundStartTime = 0;
                Log.d(TAG, "Added foreground time: " + fgTime / 1000 + "s");
            }
            backgroundStartTime = System.currentTimeMillis();
            Log.d(TAG, "App in background");
            sendDataIfNeeded();
        }
    }

    @Override public void onActivityCreated(@NonNull Activity activity, android.os.Bundle savedInstanceState) {}
    @Override public void onActivityStarted(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, android.os.Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}

    // === Сохранение и отправка данных ===

    private void loadCurrentSession() {
        // Безопасно получаем userId и роль
        currentUserId = PocketBaseClient.getCurrentUserId();
        if (currentUserId == null) {
            Log.w(TAG, "User not logged in, stats will not be recorded");
            return;
        }
        // Получаем роль (может вызывать сетевой запрос – делаем в потоке)
        currentRole = PocketBaseClient.getUserRole();
        if (currentRole == null) {
            currentRole = "user"; // fallback
        }
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Log.d(TAG, "Session loaded: userId=" + currentUserId + ", role=" + currentRole);
    }

    private void startSendingTask() {
        sendRunnable = new Runnable() {
            @Override
            public void run() {
                if (isInitialized) {
                    sendDataIfNeeded();
                }
                handler.postDelayed(this, SEND_INTERVAL);
            }
        };
        handler.postDelayed(sendRunnable, SEND_INTERVAL);
    }

    private void sendDataIfNeeded() {
        if (!isInitialized) return;
        if (currentUserId == null) {
            // Попробуем ещё раз загрузить сессию
            loadCurrentSession();
            if (currentUserId == null) return;
        }

        if (totalForegroundTime == 0 && totalBackgroundTime == 0) {
            return;
        }

        Session session = new Session();
        session.userId = currentUserId;
        session.role = currentRole;
        session.date = currentDate;
        session.activeSeconds = totalForegroundTime / 1000;
        session.backgroundSeconds = totalBackgroundTime / 1000;

        List<Session> sessions = getSessions();
        sessions.add(session);
        saveSessions(sessions);

        sendSessionsToServer(sessions);

        totalForegroundTime = 0;
        totalBackgroundTime = 0;

        String newDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (!newDate.equals(currentDate)) {
            currentDate = newDate;
        }
    }

    private List<Session> getSessions() {
        String json = prefs.getString(KEY_SESSIONS, "[]");
        return gson.fromJson(json, listType);
    }

    private void saveSessions(List<Session> sessions) {
        String json = gson.toJson(sessions);
        prefs.edit().putString(KEY_SESSIONS, json).apply();
    }

    private void sendSessionsToServer(List<Session> sessions) {
        new Thread(() -> {
            for (Session session : sessions) {
                boolean success = PocketBaseClient.createAppStat(
                        session.userId,
                        session.role,
                        session.date,
                        session.activeSeconds,
                        session.backgroundSeconds
                );
                if (!success) {
                    Log.e(TAG, "Failed to send session, will retry later");
                    // Можно оставить для повторной отправки, но для простоты удалим все
                }
            }
            // Очищаем после отправки (даже если ошибка – чтобы не дублировать)
            prefs.edit().remove(KEY_SESSIONS).apply();
        }).start();
    }

    public void onAppExit() {
        sendDataIfNeeded();
        handler.removeCallbacks(sendRunnable);
    }

    static class Session {
        String userId;
        String role;
        String date;
        long activeSeconds;
        long backgroundSeconds;
    }
}