package com.avtoforward.automaster;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class UserManager {
    private static final String PREFS = "user_prefs";
    private static final String KEY_USER_ID = "user_id";

    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_USER_ID, userId).apply();
        }
        return userId;
    }
}