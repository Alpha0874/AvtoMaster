package com.avtoforward.automaster.utils;

import android.os.Handler;
import android.os.Looper;

import com.avtoforward.automaster.PocketBaseClient;

public class OnlineStatusUpdater {
    private static OnlineStatusUpdater instance;
    private Handler handler;
    private Runnable updateRunnable;
    private boolean isRunning = false;

    private OnlineStatusUpdater() {
        handler = new Handler(Looper.getMainLooper());
    }

    public static synchronized OnlineStatusUpdater getInstance() {
        if (instance == null) {
            instance = new OnlineStatusUpdater();
        }
        return instance;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (PocketBaseClient.isLoggedIn()) {
                    new Thread(() -> {
                        PocketBaseClient.updateLastOnline();
                    }).start();
                }
                if (isRunning) {
                    handler.postDelayed(this, 30000); // каждые 30 секунд
                }
            }
        };
        handler.post(updateRunnable);
    }

    public void stop() {
        isRunning = false;
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}