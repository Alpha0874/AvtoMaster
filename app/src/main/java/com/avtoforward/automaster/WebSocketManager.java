package com.avtoforward.automaster.network;

import android.util.Log;

import com.avtoforward.automaster.PocketBaseClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    private static WebSocketManager instance;
    private WebSocket webSocket;
    private OkHttpClient client;
    private boolean isConnected = false;
    private MessageListener messageListener;

    private WebSocketManager() {
        client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    public void connect(String authToken) {
        if (isConnected) return;
        String wsUrl = PocketBaseClient.getBaseUrl().replace("http", "ws") + "/api/realtime";
        Request request = new Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", authToken)
                .build();
        webSocket = client.newWebSocket(request, new PocketBaseWebSocketListener());
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing");
            webSocket = null;
            isConnected = false;
        }
    }

    private void subscribeToForumMessages() {
        if (!isConnected) return;
        JsonObject sub = new JsonObject();
        sub.addProperty("type", "subscribe");
        JsonObject body = new JsonObject();
        body.addProperty("collection", "forum_messages");
        sub.add("body", body);
        webSocket.send(new Gson().toJson(sub));
        Log.d(TAG, "Subscribed to forum_messages");
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public interface MessageListener {
        void onNewMessage(JsonObject message);
    }

    private class PocketBaseWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, okhttp3.Response response) {
            super.onOpen(webSocket, response);
            isConnected = true;
            Log.d(TAG, "WebSocket opened");
            subscribeToForumMessages();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.d(TAG, "Received: " + text);
            try {
                JsonObject json = new Gson().fromJson(text, JsonObject.class);
                if (json.has("type") && "record".equals(json.get("type").getAsString())) {
                    JsonObject record = json.get("record").getAsJsonObject();
                    if (record.has("collection") && "forum_messages".equals(record.get("collection").getAsString())) {
                        if (messageListener != null) {
                            messageListener.onNewMessage(record);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing message", e);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {}

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            isConnected = false;
            Log.d(TAG, "WebSocket closing");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
            super.onFailure(webSocket, t, response);
            isConnected = false;
            Log.e(TAG, "WebSocket failure: ", t);
            // Попробуем переподключиться через 5 секунд
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (PocketBaseClient.isLoggedIn()) {
                    connect(PocketBaseClient.getAuthToken());
                }
            }, 5000);
        }
    }
}