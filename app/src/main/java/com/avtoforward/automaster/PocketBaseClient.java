package com.avtoforward.automaster;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.avtoforward.automaster.utils.SessionManager;
import com.avtoforward.automaster.utils.TimeTracker;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PocketBaseClient {
    private static final String BASE_URL = "http://195.133.52.115:8090";
    private static final String PREFS_NAME = "pocketbase_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .hostnameVerifier((hostname, session) -> true)
            .build();
    private static final Gson gson = new Gson();

    private static String authToken;
    private static String currentUserId;
    private static SharedPreferences prefs;
    private static Context appContext;

    private static String getCurrentIsoTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public static void init(Context context) {
        try {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            authToken = prefs.getString(KEY_TOKEN, null);
            currentUserId = prefs.getString(KEY_USER_ID, null);
            Log.d("PocketBase", "init: token=" + (authToken != null ? "present" : "null") + ", userId=" + currentUserId);
            appContext = context.getApplicationContext();
        } catch (Exception e) {
            Log.e("PocketBase", "init error", e);
        }
    }

    public static boolean isLoggedIn() {
        return authToken != null && !authToken.isEmpty();
    }

    private static void saveCredentials() {
        if (prefs != null && authToken != null) {
            prefs.edit()
                    .putString(KEY_TOKEN, authToken)
                    .putString(KEY_USER_ID, currentUserId)
                    .apply();
            Log.d("PocketBase", "Credentials saved: userId=" + currentUserId);
        }
    }

    public static void logout() {
        Log.d("PocketBase", "logout called");
        authToken = null;
        currentUserId = null;
        if (prefs != null) {
            prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_ID).apply();
            Log.d("PocketBase", "SharedPreferences cleared");
        }
        if (appContext != null) {
            try {
                TimeTracker.getInstance(appContext).onAppExit();
                Log.d("PocketBase", "TimeTracker data sent");
            } catch (Exception e) {
                Log.e("PocketBase", "Error sending TimeTracker data", e);
            }
            try {
                SessionManager sessionManager = new SessionManager(appContext);
                sessionManager.logout();
                Log.d("PocketBase", "SessionManager cleared");
            } catch (Exception e) {
                Log.e("PocketBase", "Error clearing SessionManager", e);
            }
            try {
                Intent intent = new Intent(appContext, ForegroundNotificationService.class);
                appContext.stopService(intent);
                Log.d("PocketBase", "Notification service stopped");
            } catch (Exception e) {
                Log.e("PocketBase", "Error stopping service", e);
            }
            try {
                Intent roleIntent = new Intent(appContext, RoleSelectionActivity.class);
                roleIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                appContext.startActivity(roleIntent);
                Log.d("PocketBase", "Redirected to RoleSelectionActivity");
            } catch (Exception e) {
                Log.e("PocketBase", "Error starting RoleSelectionActivity", e);
            }
        }
    }

    // ==================== РЕГИСТРАЦИЯ (УПРОЩЁННАЯ) ====================

    public static boolean register(String email, String password, String passwordConfirm, String nickname, String role) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("passwordConfirm", passwordConfirm);
        if (nickname == null || nickname.isEmpty()) {
            nickname = email.split("@")[0];
        }
        body.addProperty("nickname", nickname);
        body.addProperty("role", role != null ? role : "user");
        // Убираем отправку verified и banned, чтобы избежать ошибки типа
        // Если они обязательны, в админке можно установить значения по умолчанию.

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/collections/users/records")
                .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "null";
            Log.d("PocketBase", "Register response code: " + response.code() + ", body: " + respBody);
            if (response.isSuccessful()) {
                return login(email, password);
            } else {
                Log.e("PocketBase", "Register failed: " + respBody);
                return false;
            }
        } catch (IOException e) {
            Log.e("PocketBase", "Register error", e);
            return false;
        }
    }

    // ==================== ЛОГИН (С ПРОВЕРКОЙ ТОЛЬКО BANNED) ====================

    public static boolean login(String email, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("identity", email);
        body.addProperty("password", password);

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/collections/users/auth-with-password")
                .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();
                JsonObject respObj = gson.fromJson(json, JsonObject.class);
                JsonObject record = respObj.getAsJsonObject("record");

                // Проверяем только бан
                boolean banned = getBooleanSafe(record, "banned");
                if (banned) {
                    Log.w("PocketBase", "User is banned: " + email);
                    return false;
                }

                authToken = respObj.get("token").getAsString();
                currentUserId = record.get("id").getAsString();
                saveCredentials();
                Log.d("PocketBase", "Login success, userId=" + currentUserId);
                return true;
            } else {
                String respBody = response.body() != null ? response.body().string() : "null";
                Log.e("PocketBase", "Login failed: " + respBody);
                return false;
            }
        } catch (IOException e) {
            Log.e("PocketBase", "Login error", e);
            return false;
        }
    }

    // ==================== НОВЫЕ МЕТОДЫ ДЛЯ АДМИНА ====================

    public static boolean verifyUser(String userId) {
        // Заглушка, так как верификация отключена
        Log.d("PocketBase", "verifyUser called but skipped (verified is always true)");
        return true;
    }

    public static boolean banUser(String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("banned", "true");
        return updateUser(userId, data);
    }

    public static boolean unbanUser(String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("banned", "false");
        return updateUser(userId, data);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ ЧТЕНИЯ BOOLEAN ====================

    private static boolean getBooleanSafe(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return false;
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            try {
                String val = obj.get(key).getAsString();
                return "true".equalsIgnoreCase(val);
            } catch (Exception ex) {
                return false;
            }
        }
    }

    // ==================== ОСТАЛЬНЫЕ МЕТОДЫ (БЕЗ ИЗМЕНЕНИЙ) ====================

    public static String getCurrentUserId() {
        return currentUserId;
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static JsonObject getUserInfo(String userId) {
        if (!isLoggedIn()) return null;
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/collections/users/records/" + userId)
                .header("Authorization", authToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return gson.fromJson(response.body().string(), JsonObject.class);
            }
        } catch (IOException e) {
            Log.e("PocketBase", "Get user info error", e);
        }
        return null;
    }

    public static boolean updateUser(String userId, Map<String, Object> data) {
        if (!isLoggedIn()) return false;
        String jsonStr = gson.toJson(data);
        Log.d("PocketBase", "updateUser: userId=" + userId + ", data=" + jsonStr);
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/collections/users/records/" + userId)
                .header("Authorization", authToken)
                .patch(RequestBody.create(jsonStr, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "null";
            Log.d("PocketBase", "updateUser response code: " + response.code() + ", body: " + respBody);
            return response.isSuccessful();
        } catch (IOException e) {
            Log.e("PocketBase", "Update user error", e);
        }
        return false;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static OkHttpClient getClient() {
        return client;
    }

    public static Gson getGson() {
        return gson;
    }

    // ==================== getUsersByRole ====================
    public static JsonObject getUsersByRole(String role) {
        if (!isLoggedIn()) return null;
        try {
            String filter = "role='" + role + "'";
            String url = BASE_URL + "/api/collections/users/records?filter="
                    + java.net.URLEncoder.encode(filter, "UTF-8")
                    + "&perPage=100";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getUsersByRole error", e);
        }
        return null;
    }

    // ==================== ФОРУМ – СООБЩЕНИЯ ====================
    public static JsonObject getForumMessagesPage(String topicId, int page, int perPage) {
        if (!isLoggedIn()) return null;
        long cacheBuster = System.currentTimeMillis();
        String url = BASE_URL + "/api/collections/forum_messages/records?filter=(topic_id='" + topicId + "')&sort=created&expand=author&_=" + cacheBuster + "&page=" + page + "&perPage=" + perPage;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", authToken)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return gson.fromJson(response.body().string(), JsonObject.class);
            } else {
                Log.e("PocketBase", "getForumMessagesPage failed: " + response.code());
            }
        } catch (IOException e) {
            Log.e("PocketBase", "getForumMessagesPage error", e);
        }
        return null;
    }

    public static JsonObject getForumMessages(String topicId) {
        if (!isLoggedIn()) {
            Log.e("POCKETBASE", "getForumMessages: not logged in");
            return null;
        }
        try {
            String url = BASE_URL + "/api/collections/forum_messages/records?filter=(topic_id='" + topicId + "')&sort=created&expand=author&perPage=100";
            Log.d("POCKETBASE", "getForumMessages URL: " + url);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("POCKETBASE", "getForumMessages response: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    return gson.fromJson(responseBody, JsonObject.class);
                } else {
                    Log.e("POCKETBASE", "getForumMessages failed: " + response.code());
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e("POCKETBASE", "getForumMessages error", e);
            return null;
        }
    }

    public static void sendForumMessage(String topicId, String authorId, String messageText, Runnable onSuccess) {
        if (!isLoggedIn()) {
            Log.e("POCKETBASE", "Not logged in");
            return;
        }
        if (topicId == null || topicId.isEmpty()) {
            Log.e("POCKETBASE", "topicId is null or empty");
            return;
        }
        if (authorId == null || authorId.isEmpty()) {
            Log.e("POCKETBASE", "authorId is null or empty");
            return;
        }
        new Thread(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("author", authorId);
                body.addProperty("message_text", messageText);
                body.addProperty("topic_id", topicId);
                String jsonBody = gson.toJson(body);
                Log.d("POCKETBASE", "Sending JSON: " + jsonBody);
                Request request = new Request.Builder()
                        .url(BASE_URL + "/api/collections/forum_messages/records")
                        .header("Authorization", authToken)
                        .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("POCKETBASE", "Response code: " + response.code() + ", body: " + responseBody);
                    if (response.isSuccessful()) {
                        updateTopicLastMessage(topicId);
                        if (onSuccess != null) {
                            new Handler(Looper.getMainLooper()).post(onSuccess);
                        }
                    } else {
                        Log.e("POCKETBASE", "Failed to send message: " + response.code() + " - " + responseBody);
                    }
                }
            } catch (Exception e) {
                Log.e("POCKETBASE", "sendForumMessage error", e);
            }
        }).start();
    }

    public static void sendForumMessage(String topicId, String authorId, String messageText, List<Uri> imageUris, Runnable onSuccess) {
        if (!isLoggedIn()) return;
        sendMessageWithRetryMultipart(topicId, authorId, messageText, imageUris, onSuccess, 1);
    }

    private static void sendMessageWithRetryMultipart(String topicId, String authorId, String messageText, List<Uri> imageUris, Runnable onSuccess, int attempt) {
        new Thread(() -> {
            try {
                MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("author", authorId)
                        .addFormDataPart("message_text", messageText == null ? "" : messageText)
                        .addFormDataPart("topic_id", topicId);
                if (appContext != null) {
                    ContentResolver resolver = appContext.getContentResolver();
                    for (Uri uri : imageUris) {
                        String mimeType = resolver.getType(uri);
                        if (mimeType == null) mimeType = "image/jpeg";
                        File tempFile = copyUriToTempFile(uri);
                        if (tempFile != null) {
                            bodyBuilder.addFormDataPart("attachments", tempFile.getName(),
                                    RequestBody.create(MediaType.parse(mimeType), tempFile));
                        }
                    }
                }
                Request request = new Request.Builder()
                        .url(BASE_URL + "/api/collections/forum_messages/records")
                        .header("Authorization", authToken)
                        .post(bodyBuilder.build())
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("POCKETBASE_MULTIPART", "Response code: " + response.code() + ", body: " + responseBody);
                    if (response.isSuccessful()) {
                        updateTopicLastMessage(topicId);
                        if (onSuccess != null) {
                            new Handler(Looper.getMainLooper()).post(onSuccess);
                        }
                    } else if (response.code() == 401 && attempt <= 1) {
                        if (refreshToken()) {
                            sendMessageWithRetryMultipart(topicId, authorId, messageText, imageUris, onSuccess, attempt + 1);
                        }
                    } else {
                        Log.e("POCKETBASE_MULTIPART", "Send message with photos failed: " + response.code() + " - " + responseBody);
                    }
                }
            } catch (Exception e) {
                Log.e("POCKETBASE_MULTIPART", "Send message with photos error", e);
            }
        }).start();
    }

    public static void sendForumMessageWithPaths(String topicId, String authorId, String messageText, List<String> filePaths, Runnable onSuccess) {
        if (!isLoggedIn()) return;
        new Thread(() -> {
            try {
                MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("author", authorId)
                        .addFormDataPart("message_text", messageText)
                        .addFormDataPart("topic_id", topicId);
                for (String path : filePaths) {
                    File file = new File(path);
                    if (file.exists()) {
                        String mimeType = getMimeTypeFromPath(path);
                        bodyBuilder.addFormDataPart("attachments", file.getName(),
                                RequestBody.create(MediaType.parse(mimeType != null ? mimeType : "image/jpeg"), file));
                    } else {
                        try {
                            Uri uri = Uri.parse(path);
                            File tempFile = copyUriToTempFile(uri);
                            if (tempFile != null) {
                                bodyBuilder.addFormDataPart("attachments", tempFile.getName(),
                                        RequestBody.create(MediaType.parse("image/jpeg"), tempFile));
                            }
                        } catch (Exception e) {
                            Log.e("PocketBase", "Error processing URI", e);
                        }
                    }
                }
                Request request = new Request.Builder()
                        .url(BASE_URL + "/api/collections/forum_messages/records")
                        .header("Authorization", authToken)
                        .post(bodyBuilder.build())
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        updateTopicLastMessage(topicId);
                        if (onSuccess != null) {
                            new Handler(Looper.getMainLooper()).post(onSuccess);
                        }
                    } else {
                        Log.e("PocketBase", "sendForumMessageWithPaths failed: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e("PocketBase", "sendForumMessageWithPaths error", e);
            }
        }).start();
    }

    private static String getMimeTypeFromPath(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension == null) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0) extension = path.substring(lastDot + 1);
        }
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return null;
    }

    public static File copyUriToTempFile(Uri uri) {
        if (appContext == null) return null;
        try {
            String timeStamp = String.valueOf(System.currentTimeMillis());
            File tempFile = new File(appContext.getCacheDir(), "temp_" + timeStamp + ".jpg");
            try (InputStream is = appContext.getContentResolver().openInputStream(uri);
                 FileOutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } catch (Exception e) {
            Log.e("PocketBase", "copyUriToTempFile error", e);
            return null;
        }
    }

    // ==================== ТЕМЫ ФОРУМА ====================
    public static JsonObject getTopicsBySubcategory(String subcategoryId) {
        if (!isLoggedIn()) {
            Log.e("POCKETBASE", "getTopicsBySubcategory: not logged in");
            return null;
        }
        try {
            String filter = "category='" + subcategoryId + "'";
            String encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8");
            String url = BASE_URL + "/api/collections/forum_topics/records?filter=" + encodedFilter + "&sort=-last_message_at";
            Log.d("POCKETBASE", "getTopicsBySubcategory URL: " + url);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("POCKETBASE", "getTopicsBySubcategory response: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    return gson.fromJson(responseBody, JsonObject.class);
                } else {
                    Log.e("POCKETBASE", "getTopicsBySubcategory failed: " + response.code());
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e("POCKETBASE", "getTopicsBySubcategory error", e);
            return null;
        }
    }

    public static boolean createTopic(String title, String subcategoryId, String createdBy) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("title", title);
            body.addProperty("category", subcategoryId);
            body.addProperty("created_by", createdBy);
            body.addProperty("last_message_at", getCurrentIsoTime());
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_topics/records")
                    .header("Authorization", authToken)
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "createTopic error", e);
        }
        return false;
    }

    public static boolean renameTopic(String topicId, String newTitle) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("title", newTitle);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_topics/records/" + topicId)
                    .header("Authorization", authToken)
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "renameTopic error", e);
        }
        return false;
    }

    public static boolean deleteTopic(String topicId) {
        if (!isLoggedIn()) return false;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_topics/records/" + topicId)
                    .header("Authorization", authToken)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "deleteTopic error", e);
        }
        return false;
    }

    public static JsonObject getTopicById(String topicId) {
        if (!isLoggedIn()) return null;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_topics/records/" + topicId)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getTopicById error", e);
        }
        return null;
    }

    public static void updateTopicLastMessage(String topicId) {
        if (!isLoggedIn()) return;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("last_message_at", getCurrentIsoTime());
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_topics/records/" + topicId)
                    .header("Authorization", authToken)
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e("PocketBase", "updateTopicLastMessage failed: " + response.code());
                }
            }
        } catch (IOException e) {
            Log.e("PocketBase", "updateTopicLastMessage error", e);
        }
    }

    // ==================== КАТЕГОРИИ И ПОДКАТЕГОРИИ ====================
    public static JsonObject getForumCategories(String type) {
        if (!isLoggedIn()) return null;
        try {
            String filter = "type='" + type + "'";
            String encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8");
            String url = BASE_URL + "/api/collections/forum_categories/records?filter=" + encodedFilter + "&sort=order";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getForumCategories error", e);
        }
        return null;
    }

    public static JsonObject getSubcategories(String categoryId) {
        if (!isLoggedIn()) return null;
        try {
            String filter = "parent_category='" + categoryId + "'";
            String encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8");
            String url = BASE_URL + "/api/collections/forum_subcategories/records?filter=" + encodedFilter + "&sort=order";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getSubcategories error", e);
        }
        return null;
    }

    // ==================== РЕДАКТИРОВАНИЕ СООБЩЕНИЙ ====================
    public static boolean updateMessage(String messageId, String newText) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("message_text", newText);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_messages/records/" + messageId)
                    .header("Authorization", authToken)
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "updateMessage error", e);
        }
        return false;
    }

    public static boolean deleteMessage(String messageId) {
        if (!isLoggedIn()) {
            Log.e("DELETE_DEBUG", "deleteMessage: not logged in");
            return false;
        }
        try {
            Log.d("DELETE_DEBUG", "deleteMessage: trying to delete messageId=" + messageId);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_messages/records/" + messageId)
                    .header("Authorization", authToken)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String respBody = response.body() != null ? response.body().string() : "";
                Log.d("DELETE_DEBUG", "deleteMessage response code=" + response.code() + ", body=" + respBody);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("DELETE_DEBUG", "deleteMessage error", e);
        }
        return false;
    }

    public static int getUnreadCountForTopic(String topicId, long lastReadTime) {
        if (!isLoggedIn()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String filter = "topic_id='" + topicId + "' && created > '" + sdf.format(new Date(lastReadTime)) + "'";
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/forum_messages/records?filter=" + java.net.URLEncoder.encode(filter, "UTF-8") + "&perPage=1")
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    return result.get("totalItems").getAsInt();
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getUnreadCountForTopic error", e);
        }
        return 0;
    }

    // ==================== НОВЫЕ СООБЩЕНИЯ (ВРЕМЕННО БЕЗ ФИЛЬТРА) ====================
    public static JsonObject getNewMessagesSince(long lastTimestamp) {
        if (!isLoggedIn()) {
            Log.e("PocketBase", "getNewMessagesSince: not logged in");
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String lastTimeStr = sdf.format(new Date(lastTimestamp));
            String filter = "created > '" + lastTimeStr + "'";
            String encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8");
            String url = BASE_URL + "/api/collections/forum_messages/records?filter=" + encodedFilter + "&sort=created&expand=author&perPage=100";
            Log.d("PocketBase", "getNewMessagesSince URL: " + url);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("PocketBase", "getNewMessagesSince response code: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    return gson.fromJson(responseBody, JsonObject.class);
                } else {
                    Log.e("PocketBase", "getNewMessagesSince failed with code " + response.code() + ": " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getNewMessagesSince error", e);
            return null;
        }
    }

    // ==================== ДОКУМЕНТАЦИЯ ====================
    public static JsonObject getRecords(String collectionName) {
        if (!isLoggedIn()) return null;
        try {
            String url = BASE_URL + "/api/collections/" + collectionName + "/records?perPage=100";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                } else {
                    Log.e("PocketBase", "getRecords failed for " + collectionName + ": " + response.code());
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getRecords error", e);
        }
        return null;
    }

    public static JsonObject getModelsByFilter(String filter) {
        if (!isLoggedIn()) return null;
        try {
            String encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8");
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/car_models/records?filter=" + encodedFilter)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getModelsByFilter error", e);
        }
        return null;
    }

    public static JsonObject getDocumentsByModel(String modelId) {
        if (!isLoggedIn()) return null;
        try {
            String encodedId = java.net.URLEncoder.encode(modelId, "UTF-8");
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/documents/records?filter=model_id='" + encodedId + "'&sort=-created_at")
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getDocumentsByModel error", e);
        }
        return null;
    }

    public static boolean uploadDocument(String title, String description, String modelId, String filePath) {
        if (!isLoggedIn()) return false;
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e("PocketBase", "uploadDocument: file not exists");
            return false;
        }
        try {
            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", title)
                    .addFormDataPart("description", description == null ? "" : description)
                    .addFormDataPart("model_id", modelId)
                    .addFormDataPart("uploaded_by", currentUserId)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("application/pdf"), file));
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/documents/records")
                    .header("Authorization", authToken)
                    .post(bodyBuilder.build())
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d("PocketBase", "uploadDocument success");
                    return true;
                } else {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.e("PocketBase", "uploadDocument failed: " + response.code() + " " + responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "uploadDocument exception", e);
        }
        return false;
    }

    public static boolean updateDocument(String docId, String newTitle, String newDescription) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("title", newTitle);
            if (newDescription != null) body.addProperty("description", newDescription);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/documents/records/" + docId)
                    .header("Authorization", authToken)
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "updateDocument error", e);
        }
        return false;
    }

    public static boolean deleteDocument(String docId) {
        if (!isLoggedIn()) return false;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/documents/records/" + docId)
                    .header("Authorization", authToken)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "deleteDocument error", e);
        }
        return false;
    }

    // ==================== АВАТАР ====================
    public static boolean uploadAvatar(String userId, String filePath) {
        if (!isLoggedIn()) return false;
        File file = new File(filePath);
        if (!file.exists()) return false;
        try {
            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("avatar", file.getName(),
                            RequestBody.create(MediaType.parse("image/jpeg"), file));
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/users/records/" + userId)
                    .header("Authorization", authToken)
                    .patch(bodyBuilder.build())
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "uploadAvatar error", e);
        }
        return false;
    }

    // ==================== ЗАКАЗЫ ====================
    public static boolean createOrder(com.avtoforward.automaster.Order order) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("service", order.getService());
            body.addProperty("address", order.getAddress());
            body.addProperty("description", order.getDescription());
            body.addProperty("comment", order.getComment());
            body.addProperty("vehicle_type", order.getVehicleType());
            body.addProperty("vehicle_brand", order.getVehicleBrand());
            body.addProperty("vehicle_model", order.getVehicleModel());
            body.addProperty("vehicle_year", order.getVehicleYear());
            body.addProperty("price", order.getPrice());
            body.addProperty("status", order.getStatus());
            body.addProperty("user_id", order.getUserId());
            body.addProperty("is_premium", order.isPremiumOrder());
            body.addProperty("assigned_to", order.getAssignedTo() != null ? order.getAssignedTo() : "");
            body.addProperty("created_at", order.getCreatedAt());

            if (order.getClientName() != null) body.addProperty("client_name", order.getClientName());
            if (order.getClientPhone() != null) body.addProperty("client_phone", order.getClientPhone());
            if (order.getCity() != null) body.addProperty("city", order.getCity());
            body.addProperty("distance_mkad_km", order.getDistanceMkadKm());
            if (order.getMasterType() != null) body.addProperty("master_type", order.getMasterType());
            if (order.getPaymentMethod() != null) body.addProperty("payment_method", order.getPaymentMethod());
            body.addProperty("is_price_by_agreement", order.isPriceByAgreement());
            body.addProperty("final_price", order.getFinalPrice());

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records")
                    .header("Authorization", authToken)
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "Create order error", e);
        }
        return false;
    }

    public static List<Order> getNewOrders() {
        List<Order> orders = new ArrayList<>();
        if (!isLoggedIn()) {
            Log.e("PocketBase", "getNewOrders: not logged in");
            return orders;
        }
        try {
            String filter = "status='new' && (assigned_to='' || assigned_to=null)";
            String encodedFilter = java.net.URLEncoder.encode(filter, "UTF-8");
            String url = BASE_URL + "/api/collections/orders/records?filter=" + encodedFilter + "&sort=-created_at";
            Log.d("PocketBase", "getNewOrders URL: " + url);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("PocketBase", "getNewOrders response code: " + response.code() + ", body: " + responseBody);
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    for (int i = 0; i < items.size(); i++) {
                        Order order = parseOrder(items.get(i).getAsJsonObject());
                        if (order != null) orders.add(order);
                    }
                } else {
                    Log.e("PocketBase", "getNewOrders failed: " + response.code());
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "Get new orders error", e);
        }
        Log.d("PocketBase", "getNewOrders result: " + orders.size() + " orders found");
        return orders;
    }

    public static List<com.avtoforward.automaster.Order> getMyOrders(String masterId) {
        List<com.avtoforward.automaster.Order> orders = new ArrayList<>();
        if (!isLoggedIn()) return orders;
        try {
            String filter = "(status='accepted' || status='completed') && assigned_to='" + masterId + "'";
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records?filter=" + java.net.URLEncoder.encode(filter, "UTF-8") + "&sort=-created_at")
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    for (int i = 0; i < items.size(); i++) {
                        com.avtoforward.automaster.Order order = parseOrder(items.get(i).getAsJsonObject());
                        if (order != null) orders.add(order);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "Get my orders error", e);
        }
        return orders;
    }

    public static List<com.avtoforward.automaster.Order> getUserOrders(String userId) {
        List<com.avtoforward.automaster.Order> orders = new ArrayList<>();
        if (!isLoggedIn()) return orders;
        try {
            String filter = "user_id='" + userId + "'";
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records?filter=" + java.net.URLEncoder.encode(filter, "UTF-8") + "&sort=-created_at")
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    for (int i = 0; i < items.size(); i++) {
                        com.avtoforward.automaster.Order order = parseOrder(items.get(i).getAsJsonObject());
                        if (order != null) orders.add(order);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getUserOrders error", e);
        }
        return orders;
    }

    public static boolean acceptOrder(String orderId, String masterId) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("status", "accepted");
            body.addProperty("assigned_to", masterId);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records/" + orderId)
                    .header("Authorization", authToken)
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "Accept order error", e);
        }
        return false;
    }

    public static boolean completeOrder(String orderId) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("status", "completed");
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records/" + orderId)
                    .header("Authorization", authToken)
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "Complete order error", e);
        }
        return false;
    }

    private static com.avtoforward.automaster.Order parseOrder(JsonObject item) {
        try {
            String id = item.get("id").getAsString();
            String service = item.has("service") ? item.get("service").getAsString() : "";
            String address = item.has("address") ? item.get("address").getAsString() : "";
            String description = item.has("description") ? item.get("description").getAsString() : "";
            String comment = item.has("comment") ? item.get("comment").getAsString() : "";
            String vehicleType = item.has("vehicle_type") ? item.get("vehicle_type").getAsString() : "";
            String vehicleBrand = item.has("vehicle_brand") ? item.get("vehicle_brand").getAsString() : "";
            String vehicleModel = item.has("vehicle_model") ? item.get("vehicle_model").getAsString() : "";
            String vehicleYear = item.has("vehicle_year") ? item.get("vehicle_year").getAsString() : "";
            int price = item.has("price") ? item.get("price").getAsInt() : 0;
            String status = item.has("status") ? item.get("status").getAsString() : "new";
            String userId = item.has("user_id") ? item.get("user_id").getAsString() : "";
            boolean isPremium = item.has("is_premium") && item.get("is_premium").getAsBoolean();
            String assignedTo = item.has("assigned_to") && !item.get("assigned_to").isJsonNull() ? item.get("assigned_to").getAsString() : null;
            long createdAt = item.has("created_at") ? item.get("created_at").getAsLong() : 0;
            long orderNumber = item.has("order_number") && !item.get("order_number").isJsonNull()
                    ? item.get("order_number").getAsLong()
                    : 0;

            String clientName = item.has("client_name") && !item.get("client_name").isJsonNull() ? item.get("client_name").getAsString() : "";
            String clientPhone = item.has("client_phone") && !item.get("client_phone").isJsonNull() ? item.get("client_phone").getAsString() : "";
            String city = item.has("city") && !item.get("city").isJsonNull() ? item.get("city").getAsString() : "";
            int distanceMkadKm = item.has("distance_mkad_km") && !item.get("distance_mkad_km").isJsonNull() ? item.get("distance_mkad_km").getAsInt() : 0;
            String masterType = item.has("master_type") && !item.get("master_type").isJsonNull() ? item.get("master_type").getAsString() : "";
            String paymentMethod = item.has("payment_method") && !item.get("payment_method").isJsonNull() ? item.get("payment_method").getAsString() : "";
            boolean isPriceByAgreement = item.has("is_price_by_agreement") && item.get("is_price_by_agreement").getAsBoolean();
            int finalPrice = item.has("final_price") && !item.get("final_price").isJsonNull() ? item.get("final_price").getAsInt() : 0;

            return new com.avtoforward.automaster.Order(id, service, address, description, comment,
                    vehicleType, vehicleBrand, vehicleModel, vehicleYear,
                    price, status, userId, isPremium, assignedTo, createdAt,
                    clientName, clientPhone, city, distanceMkadKm,
                    masterType, paymentMethod, isPriceByAgreement, finalPrice);
        } catch (Exception e) {
            Log.e("PocketBase", "Parse order error", e);
            return null;
        }
    }

    // ==================== УПРАВЛЕНИЕ ПРИЁМОМ ЗАКАЗОВ ====================
    public static boolean isAcceptingOrders(String userId) {
        JsonObject user = getUserInfo(userId);
        if (user != null && user.has("accepting_orders") && !user.get("accepting_orders").isJsonNull()) {
            return "yes".equals(user.get("accepting_orders").getAsString());
        }
        return true;
    }

    public static boolean setAcceptingOrders(String userId, boolean accepting) {
        Map<String, Object> data = new HashMap<>();
        data.put("accepting_orders", accepting ? "yes" : "no");
        return updateUser(userId, data);
    }

    // ==================== СТАТИСТИКА ====================
    public static List<com.avtoforward.automaster.Order> getCompletedOrders(String masterId) {
        List<com.avtoforward.automaster.Order> orders = new ArrayList<>();
        if (!isLoggedIn()) return orders;
        try {
            String filter = "status='completed' && assigned_to='" + masterId + "'";
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records?filter=" + java.net.URLEncoder.encode(filter, "UTF-8") + "&sort=-created_at")
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    for (int i = 0; i < items.size(); i++) {
                        com.avtoforward.automaster.Order order = parseOrder(items.get(i).getAsJsonObject());
                        if (order != null) orders.add(order);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getCompletedOrders error", e);
        }
        return orders;
    }

    // ==================== СТАРЫЕ МЕТОДЫ (устарели) ====================
    @Deprecated
    public static String getVerificationStatus(String userId) {
        JsonObject user = getUserInfo(userId);
        if (user != null && user.has("verification_status") && !user.get("verification_status").isJsonNull()) {
            return user.get("verification_status").getAsString();
        }
        return "pending";
    }

    @Deprecated
    public static boolean uploadPassportPhoto(String userId, String filePath) {
        if (!isLoggedIn()) return false;
        File file = new File(filePath);
        if (!file.exists()) return false;
        try {
            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("passport_photo", file.getName(),
                            RequestBody.create(MediaType.parse("image/jpeg"), file));
            bodyBuilder.addFormDataPart("verification_status", "pending");
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/users/records/" + userId)
                    .header("Authorization", authToken)
                    .patch(bodyBuilder.build())
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "uploadPassportPhoto error", e);
        }
        return false;
    }

    @Deprecated
    public static JsonObject getPendingVerifications() {
        if (!isLoggedIn()) return null;
        try {
            String filter = "role='master' && verification_status='pending'";
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/users/records?filter=" + java.net.URLEncoder.encode(filter, "UTF-8") + "&perPage=100")
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getPendingVerifications error", e);
        }
        return null;
    }

    @Deprecated
    public static boolean updateVerificationStatus(String userId, String status) {
        if (!isLoggedIn()) return false;
        Map<String, Object> data = new HashMap<>();
        data.put("verification_status", status);
        return updateUser(userId, data);
    }

    // ==================== АДМИНИСТРИРОВАНИЕ ====================
    public static String getUserRole() {
        if (currentUserId == null) return null;
        JsonObject user = getUserInfo(currentUserId);
        if (user != null && user.has("role") && !user.get("role").isJsonNull()) {
            return user.get("role").getAsString();
        }
        return "master";
    }

    public static JsonObject getAllUsers() {
        if (!isLoggedIn()) return null;
        try {
            String url = BASE_URL + "/api/collections/users/records?perPage=100&sort=-created";
            Log.d("PocketBase", "getAllUsers URL: " + url);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("PocketBase", "getAllUsers response: " + responseBody);
                if (response.isSuccessful()) {
                    return gson.fromJson(responseBody, JsonObject.class);
                } else {
                    Log.e("PocketBase", "getAllUsers failed: " + response.code() + " - " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getAllUsers error", e);
        }
        return null;
    }

    public static JsonObject getAllOrders() {
        if (!isLoggedIn()) return null;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records?sort=-created_at&perPage=100")
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getAllOrders error", e);
        }
        return null;
    }

    public static boolean updateOrderStatus(String orderId, String newStatus, String assignedTo) {
        if (!isLoggedIn()) return false;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("status", newStatus);
            if (assignedTo != null) body.addProperty("assigned_to", assignedTo);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/orders/records/" + orderId)
                    .header("Authorization", authToken)
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "updateOrderStatus error", e);
        }
        return false;
    }

    public static boolean deleteUser(String userId) {
        if (!isLoggedIn()) return false;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/users/records/" + userId)
                    .header("Authorization", authToken)
                    .delete()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "deleteUser error", e);
        }
        return false;
    }

    public static JsonObject getStats() {
        if (!isLoggedIn()) return null;
        try {
            String filterAllMasters = "role='master'";
            int totalMasters = getTotalCount("users", filterAllMasters);
            long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String fiveMinutesAgoStr = sdf.format(new Date(fiveMinutesAgo));
            String filterOnlineMasters = "role='master' && last_online > '" + fiveMinutesAgoStr + "'";
            int onlineMasters = getTotalCount("users", filterOnlineMasters);
            long tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000;
            String tenMinutesAgoStr = sdf.format(new Date(tenMinutesAgo));
            String filterForumMasters = "role='master' && last_forum_activity > '" + tenMinutesAgoStr + "'";
            int mastersOnForum = getTotalCount("users", filterForumMasters);
            String filterAllClients = "role='user'";
            int totalClients = getTotalCount("users", filterAllClients);
            String filterOnlineClients = "role='user' && last_online > '" + fiveMinutesAgoStr + "'";
            int onlineClients = getTotalCount("users", filterOnlineClients);
            String filterNewOrders = "status='new'";
            int newOrders = getTotalCount("orders", filterNewOrders);
            String filterCompletedOrders = "status='completed'";
            int completedOrders = getTotalCount("orders", filterCompletedOrders);
            JsonObject stats = new JsonObject();
            stats.addProperty("total_masters", totalMasters);
            stats.addProperty("online_masters", onlineMasters);
            stats.addProperty("masters_on_forum", mastersOnForum);
            stats.addProperty("total_clients", totalClients);
            stats.addProperty("online_clients", onlineClients);
            stats.addProperty("new_orders", newOrders);
            stats.addProperty("completed_orders", completedOrders);
            return stats;
        } catch (Exception e) {
            Log.e("PocketBase", "getStats error", e);
        }
        return null;
    }

    private static int getTotalCount(String collection, String filter) {
        try {
            String url = BASE_URL + "/api/collections/" + collection + "/records?perPage=1&filter=" + java.net.URLEncoder.encode(filter, "UTF-8");
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    return result.get("totalItems").getAsInt();
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getTotalCount error", e);
        }
        return 0;
    }

    private static boolean refreshToken() {
        if (!isLoggedIn()) return false;
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/collections/users/refresh")
                .post(RequestBody.create("", MediaType.parse("application/json")))
                .header("Authorization", authToken)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String json = response.body().string();
                JsonObject respObj = gson.fromJson(json, JsonObject.class);
                authToken = respObj.get("token").getAsString();
                currentUserId = respObj.getAsJsonObject("record").get("id").getAsString();
                saveCredentials();
                Log.d("PocketBase", "Token refreshed successfully");
                return true;
            } else {
                Log.e("PocketBase", "Refresh failed: " + response.code());
                logout();
            }
        } catch (IOException e) {
            Log.e("PocketBase", "Refresh error", e);
        }
        return false;
    }

    public static void updateLastOnline() {
        if (!isLoggedIn()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("last_online", getCurrentIsoTime());
        updateUser(currentUserId, data);
    }

    public static void updateLastForumActivity() {
        if (!isLoggedIn()) return;
        Map<String, Object> data = new HashMap<>();
        data.put("last_forum_activity", getCurrentIsoTime());
        updateUser(currentUserId, data);
    }

    // ==================== ТАРИФЫ ====================
    public static JsonObject getTariff(String city, String vehicleType) {
        if (!isLoggedIn()) {
            Log.e("PocketBase", "getTariff: not logged in");
            return null;
        }
        try {
            String url = BASE_URL + "/api/collections/tariffs/records?perPage=100";
            Log.d("PocketBase", "getTariff URL: " + url);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        String c = item.has("city") ? item.get("city").getAsString() : "";
                        String vt = item.has("vehicle_type") ? item.get("vehicle_type").getAsString() : "";
                        int price = item.has("base_price") ? item.get("base_price").getAsInt() : 0;
                        Log.d("PocketBase", "Тариф: город=" + c + ", тип=" + vt + ", цена=" + price);
                    }
                    String cityTrim = city.trim();
                    String vehicleTrim = vehicleType.trim();
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        String c = item.has("city") ? item.get("city").getAsString().trim() : "";
                        String vt = item.has("vehicle_type") ? item.get("vehicle_type").getAsString().trim() : "";
                        if (c.equalsIgnoreCase(cityTrim) && vt.equalsIgnoreCase(vehicleTrim)) {
                            Log.d("PocketBase", "Найден тариф: " + c + ", " + vt + " = " + item.get("base_price").getAsInt());
                            return item;
                        }
                    }
                    Log.e("PocketBase", "Тариф НЕ найден для города: " + city + ", типа ТС: " + vehicleType);
                } else {
                    Log.e("PocketBase", "getTariff failed, code: " + response.code());
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getTariff error", e);
        }
        return null;
    }

    // ==================== СТАТИСТИКА ИСПОЛЬЗОВАНИЯ ====================
    public static boolean createAppStat(String userId, String role, String date, long activeSeconds, long backgroundSeconds) {
        if (!isLoggedIn()) {
            Log.e("PocketBase", "createAppStat: not logged in");
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("user_id", userId);
            body.addProperty("role", role);
            body.addProperty("date", date);
            body.addProperty("active_seconds", activeSeconds);
            body.addProperty("background_seconds", backgroundSeconds);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/collections/app_stats/records")
                    .header("Authorization", authToken)
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("PocketBase", "createAppStat response code: " + response.code() + ", body: " + responseBody);
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e("PocketBase", "createAppStat error", e);
            return false;
        }
    }

    // ==================== СТАТИСТИКА МАСТЕРА ДЛЯ АДМИНКИ ====================
    public static int getOrderCountByStatus(String masterId, String status) {
        if (!isLoggedIn()) return 0;
        try {
            String filter = "assigned_to='" + masterId + "' && status='" + status + "'";
            String url = BASE_URL + "/api/collections/orders/records?perPage=1&filter=" + java.net.URLEncoder.encode(filter, "UTF-8");
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    return result.get("totalItems").getAsInt();
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getOrderCountByStatus error", e);
        }
        return 0;
    }

    public static long getTotalAppTime(String userId) {
        if (!isLoggedIn()) return 0;
        try {
            String filter = "user_id='" + userId + "'";
            String url = BASE_URL + "/api/collections/app_stats/records?filter=" + java.net.URLEncoder.encode(filter, "UTF-8") + "&perPage=1000";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    long total = 0;
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        long active = item.has("active_seconds") ? item.get("active_seconds").getAsLong() : 0;
                        long background = item.has("background_seconds") ? item.get("background_seconds").getAsLong() : 0;
                        total += active + background;
                    }
                    return total;
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getTotalAppTime error", e);
        }
        return 0;
    }

    public static int getMasterNumber(String masterId) {
        if (!isLoggedIn()) return 0;
        try {
            String filter = "role='master'";
            String url = BASE_URL + "/api/collections/users/records?filter=" + java.net.URLEncoder.encode(filter, "UTF-8") + "&sort=created&perPage=1000";
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authToken)
                    .get()
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);
                    JsonArray items = result.getAsJsonArray("items");
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject user = items.get(i).getAsJsonObject();
                        String id = user.get("id").getAsString();
                        if (id.equals(masterId)) {
                            return i + 1;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("PocketBase", "getMasterNumber error", e);
        }
        return 0;
    }
}