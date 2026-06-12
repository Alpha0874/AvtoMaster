package com.avtoforward.automaster;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ForumNotificationService extends Service {
    private static final String CHANNEL_ID = "forum_notifications";
    private static final int NOTIFICATION_ID = 1001;
    private final Handler handler = new Handler();
    private Runnable runnable;
    private long lastCheckTime = System.currentTimeMillis() - 60000;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        runnable = () -> {
            checkNewMessages();
            handler.postDelayed(runnable, 15000);
        };
        handler.post(runnable);
    }

    private void checkNewMessages() {
        if (!PocketBaseClient.isLoggedIn()) return;

        new Thread(() -> {
            try {
                JsonObject result = PocketBaseClient.getNewMessagesSince(lastCheckTime);
                if (result != null && result.has("items")) {
                    JsonArray items = result.getAsJsonArray("items");
                    if (!items.isEmpty()) {
                        JsonObject lastMsg = items.get(items.size() - 1).getAsJsonObject();
                        if (lastMsg.has("created") && !lastMsg.get("created").isJsonNull()) {
                            String createdStr = lastMsg.get("created").getAsString();
                            long newTime = parseIsoToMillis(createdStr);
                            if (newTime > lastCheckTime) lastCheckTime = newTime;
                        }
                        showNotification(items.size());
                    }
                }
            } catch (Exception e) {
                Log.e("ForumNotify", "checkNewMessages error", e);
            }
        }).start();
    }

    private long parseIsoToMillis(String isoDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(isoDate);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    private void showNotification(int count) {
        // Проверка разрешения для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("ForumNotify", "POST_NOTIFICATIONS permission not granted");
                return;
            }
        }

        Intent intent = new Intent(this, ForumActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Новые сообщения в форуме")
                .setContentText("Пришло " + count + " новых сообщений")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Уведомления форума",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}