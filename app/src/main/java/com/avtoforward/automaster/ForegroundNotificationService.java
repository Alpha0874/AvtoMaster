package com.avtoforward.automaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.avtoforward.automaster.network.WebSocketManager;
import com.google.gson.JsonObject;

public class ForegroundNotificationService extends Service implements WebSocketManager.MessageListener {

    private static final String CHANNEL_ID = "ForumNotificationChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "FgNotificationService";
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mainHandler = new Handler(Looper.getMainLooper());
        startForeground(NOTIFICATION_ID, getNotification("Сервис уведомлений запущен", ""));
        WebSocketManager.getInstance().setMessageListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (PocketBaseClient.isLoggedIn()) {
            WebSocketManager.getInstance().connect(PocketBaseClient.getAuthToken());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WebSocketManager.getInstance().disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Уведомления форума",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Получение уведомлений о новых сообщениях в форуме");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String title, String content) {
        Intent intent = new Intent(this, ForumActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onNewMessage(JsonObject message) {
        String authorId = message.has("author") ? message.get("author").getAsString() : "";
        String currentUserId = PocketBaseClient.getCurrentUserId();
        if (authorId.equals(currentUserId)) return;

        String messageText = message.has("message_text") ? message.get("message_text").getAsString() : "";
        String topicId = message.has("topic_id") ? message.get("topic_id").getAsString() : "";

        new Thread(() -> {
            String topicTitle = "Новое сообщение";
            JsonObject topic = PocketBaseClient.getTopicById(topicId);
            if (topic != null && topic.has("title")) {
                topicTitle = topic.get("title").getAsString();
            }
            final String finalTitle = topicTitle;
            mainHandler.post(() -> showNotification(finalTitle, messageText));
        }).start();
    }

    private void showNotification(String title, String content) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = getNotification(title, content);
        manager.notify((int) System.currentTimeMillis(), notification);
    }
}