package com.avtoforward.automaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class ForegroundNotificationService extends Service {

    private static final String TAG = "ForegroundService";
    private static final String CHANNEL_ID = "avtomaster_foreground";
    private static final int NOTIFICATION_ID = 1001;
    private static final long POLL_INTERVAL = 3000; // 3 секунды
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_LAST_CHECK_TIME = "last_check_time";

    private Handler handler = new Handler();
    private Runnable pollRunnable;
    private Set<String> shownOrderIds = new HashSet<>();
    private Set<String> shownMessageIds = new HashSet<>();
    private SharedPreferences prefs;
    private SimpleDateFormat dateFormat;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "✅ Foreground service CREATED");
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Формат даты, который приходит от PocketBase: "2026-06-25 08:35:54.125Z"
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getServiceNotification());
        loadShownIds();
        startPolling();
        Log.d(TAG, "✅ Service started, polling every " + POLL_INTERVAL + " ms");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "❌ Service destroyed");
        if (handler != null && pollRunnable != null) {
            handler.removeCallbacks(pollRunnable);
        }
        saveShownIds();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (PocketBaseClient.isLoggedIn()) {
                    Log.d(TAG, "🔄 Polling... (user logged in)");
                    new Thread(() -> checkNewOrders()).start();
                    new Thread(() -> checkNewForumMessages()).start();
                } else {
                    Log.d(TAG, "⏳ User not logged in, skipping poll");
                }
                handler.postDelayed(this, POLL_INTERVAL);
            }
        };
        handler.post(pollRunnable);
    }

    // === Проверка новых заказов ===

    private void checkNewOrders() {
        try {
            Log.d(TAG, "📦 Checking new orders...");
            List<Order> newOrders = PocketBaseClient.getNewOrders();
            Log.d(TAG, "📦 Found " + newOrders.size() + " new orders");
            for (Order order : newOrders) {
                String orderId = order.getId();
                if (!shownOrderIds.contains(orderId)) {
                    shownOrderIds.add(orderId);
                    Log.d(TAG, "🔔 New order detected: " + orderId);
                    String title = "🔧 Новый заказ!";
                    String body = "Заказ #" + orderId.substring(0, 6) + " | " + order.getService();
                    runOnUiThread(() -> showNotification(title, body, "order", orderId));
                } else {
                    Log.d(TAG, "⏩ Order " + orderId + " already shown, skipping");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking orders", e);
        }
    }

    // === Проверка новых сообщений форума (исправленный парсинг даты) ===

    private void checkNewForumMessages() {
        try {
            Log.d(TAG, "💬 Checking new forum messages...");
            long lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis() - 60000);
            JsonObject result = PocketBaseClient.getNewMessagesSince(lastCheck);
            if (result == null) {
                Log.d(TAG, "💬 getNewMessagesSince returned null");
                return;
            }
            if (!result.has("items")) {
                Log.d(TAG, "💬 No 'items' in response");
                return;
            }
            JsonArray items = result.getAsJsonArray("items");
            Log.d(TAG, "💬 Found " + items.size() + " new messages");
            long newestTime = lastCheck;
            for (int i = 0; i < items.size(); i++) {
                JsonObject msg = items.get(i).getAsJsonObject();
                String msgId = msg.get("id").getAsString();
                String topicId = msg.get("topic_id").getAsString();
                String authorId = msg.has("author") ? msg.get("author").getAsString() : "";

                // Пропускаем свои сообщения
                if (authorId.equals(PocketBaseClient.getCurrentUserId())) {
                    Log.d(TAG, "⏩ Skipping own message: " + msgId);
                    continue;
                }

                if (!shownMessageIds.contains(msgId)) {
                    shownMessageIds.add(msgId);
                    Log.d(TAG, "🔔 New forum message detected: " + msgId);
                    String title = "💬 Новое сообщение в форуме";
                    String body = "Кто-то написал в форуме";
                    showNotification(title, body, "forum", topicId);
                } else {
                    Log.d(TAG, "⏩ Message " + msgId + " already shown");
                }

                // Обновляем время последнего проверенного сообщения
                try {
                    String createdStr = msg.get("created").getAsString();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    long createdAt = sdf.parse(createdStr).getTime();
                    if (createdAt > newestTime) {
                        newestTime = createdAt;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing date", e);
                }
            }
            if (newestTime > lastCheck) {
                prefs.edit().putLong(KEY_LAST_CHECK_TIME, newestTime).apply();
                Log.d(TAG, "💬 Updated last check time to " + newestTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking forum messages", e);
        }
    }

    private String getMessagePreview(JsonObject msg) {
        String text = msg.has("message_text") ? msg.get("message_text").getAsString() : "";
        if (text.length() > 30) {
            return text.substring(0, 30) + "...";
        }
        return text;
    }

    // === Вспомогательные методы ===

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Сервис АвтоТехПомощь",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Обеспечивает работу уведомлений в фоне");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            Log.d(TAG, "✅ Notification channel created");
        }
    }

    private Notification getServiceNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("АвтоТехПомощь")
                .setContentText("Приложение работает в фоне")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void showNotification(String title, String body, String type, String id) {
        Log.d(TAG, "📢 Showing notification: " + title + " - " + body);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "avtomaster_channel",
                    "Уведомления приложения",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        Intent intent;
        if ("order".equals(type)) {
            intent = new Intent(this, ActiveOrdersActivity.class);
            intent.putExtra("order_id", id);
        } else if ("forum".equals(type)) {
            intent = new Intent(this, ForumActivity.class);
            intent.putExtra("topic_id", id);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "avtomaster_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
        Log.d(TAG, "✅ Notification shown");
    }

    private void runOnUiThread(Runnable action) {
        if (action != null) {
            new Handler(getMainLooper()).post(action);
        }
    }

    private void loadShownIds() {
        String ids = prefs.getString("shown_order_ids", "");
        if (!ids.isEmpty()) {
            for (String id : ids.split(",")) {
                shownOrderIds.add(id);
            }
        }
        ids = prefs.getString("shown_message_ids", "");
        if (!ids.isEmpty()) {
            for (String id : ids.split(",")) {
                shownMessageIds.add(id);
            }
        }
        Log.d(TAG, "Loaded shown IDs: orders=" + shownOrderIds.size() + ", messages=" + shownMessageIds.size());
    }

    private void saveShownIds() {
        String orderIds = String.join(",", shownOrderIds);
        String messageIds = String.join(",", shownMessageIds);
        prefs.edit()
                .putString("shown_order_ids", orderIds)
                .putString("shown_message_ids", messageIds)
                .apply();
        Log.d(TAG, "Saved shown IDs");
    }
}