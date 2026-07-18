package com.avtoforward.automaster.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Сохранить сессию после входа
    public void createLoginSession(String email, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.commit();
    }

    // Проверить, залогинен ли пользователь
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Получить роль пользователя
    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "user");
    }

    // Получить email пользователя
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    // Очистить сессию (выход)
    public void logout() {
        editor.clear();
        editor.commit();
    }
}