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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ForegroundNotificationService extends Service {

    private static final String TAG = "ForegroundNotif";
    private static final int NOTIF_ID = 1001;
    private static final String CHANNEL_ID = "avtomaster_channel";

    private Handler handler;
    private Runnable checkRunnable;
    private int lastOrderCount = 0;
    private int lastMessageCount = 0;
    private String masterId;
    private boolean isMaster = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Показываем уведомление о том, что сервис работает
        startForeground(NOTIF_ID, createForegroundNotification());

        // Получаем ID текущего пользователя
        masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) {
            Log.e(TAG, "Пользователь не авторизован, сервис остановлен");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Определяем, мастер ли пользователь
        String role = PocketBaseClient.getUserRole();
        isMaster = "master".equals(role);

        // Запускаем периодическую проверку
        startChecking();

        return START_STICKY;
    }

    private void startChecking() {
        if (checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }

        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkNewItems();
                handler.postDelayed(this, 30000); // проверка каждые 30 секунд
            }
        };
        handler.post(checkRunnable);
    }

    private void checkNewItems() {
        if (!PocketBaseClient.isLoggedIn()) {
            return;
        }

        new Thread(() -> {
            try {
                // Если пользователь — мастер, проверяем новые заказы
                if (isMaster && PocketBaseClient.isAcceptingOrders(masterId)) {
                    checkNewOrders();
                }

                // Проверяем новые сообщения в форуме (для всех пользователей)
                checkNewMessages();

            } catch (Exception e) {
                Log.e(TAG, "Ошибка проверки", e);
            }
        }).start();
    }

    private void checkNewOrders() {
        try {
            List<Order> orders = PocketBaseClient.getNewOrders();
            int currentCount = orders != null ? orders.size() : 0;

            if (currentCount > lastOrderCount) {
                int newCount = currentCount - lastOrderCount;
                showNotification(
                        "Новый заказ!",
                        "Поступило " + newCount + " новых заказов",
                        new Intent(this, ActiveOrdersActivity.class)
                );
            }
            lastOrderCount = currentCount;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки заказов", e);
        }
    }

    private void checkNewMessages() {
        try {
            // Получаем все темы, где пользователь участвовал
            // Упрощённо: проверяем все сообщения за последние 30 секунд
            long lastCheckTime = System.currentTimeMillis() - 30000;

            JsonObject result = PocketBaseClient.getNewMessagesSince(lastCheckTime);
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                int currentCount = items.size();

                if (currentCount > lastMessageCount) {
                    int newCount = currentCount - lastMessageCount;
                    // Показываем уведомление только если это не сообщение от текущего пользователя
                    // (упрощённо: проверяем, что автор не текущий)
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject msg = items.get(i).getAsJsonObject();
                        String authorId = msg.get("author").getAsString();
                        if (!authorId.equals(masterId)) {
                            showNotification(
                                    "Новое сообщение в форуме",
                                    "Появилось новое сообщение",
                                    new Intent(this, ForumActivity.class)
                            );
                            break;
                        }
                    }
                }
                lastMessageCount = currentCount;
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки сообщений", e);
        }
    }

    private void showNotification(String title, String text, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private Notification createForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("АвтоТехПомощь")
                .setContentText("Приложение работает в фоне")
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Уведомления АвтоТехПомощь",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        Log.d(TAG, "Сервис остановлен");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}