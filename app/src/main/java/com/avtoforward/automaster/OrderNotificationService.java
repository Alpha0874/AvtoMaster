package com.avtoforward.automaster;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class OrderNotificationService extends Service {
    private static final int NOTIF_ID = 1;
    private Handler handler = new Handler();
    private Runnable checkRunnable;
    private int lastOrderCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        checkRunnable = () -> {
            checkNewOrders();
            handler.postDelayed(checkRunnable, 30000);
        };
        handler.post(checkRunnable);
    }

    private void checkNewOrders() {
        String masterId = PocketBaseClient.getCurrentUserId();
        if (masterId == null) return;
        if (!PocketBaseClient.isAcceptingOrders(masterId)) {
            return; // не проверяем и не показываем уведомления
        }
        if (!PocketBaseClient.isLoggedIn()) return;
        new Thread(() -> {
            JsonObject result = PocketBaseClient.getNewOrdersForMaster();
            if (result != null && result.has("items")) {
                JsonArray items = result.getAsJsonArray("items");
                int currentCount = items.size();
                if (currentCount > lastOrderCount) {
                    showNotification(currentCount - lastOrderCount);
                }
                lastOrderCount = currentCount;
            }
        }).start();
    }

    private void showNotification(int newCount) {
        Intent intent = new Intent(this, MasterActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "orders_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Новый заказ!")
                .setContentText("Поступило " + newCount + " новых заказов")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIF_ID, builder.build());
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("orders_channel", "Заказы", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
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