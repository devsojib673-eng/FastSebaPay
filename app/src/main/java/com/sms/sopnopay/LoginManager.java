package com.sms.sopnopay;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginManager {
    private static final String PREF_NAME = "LoginPrefs";
    private static final String USER_EMAIL = "";
    private static final String UID = "";

    public static void saveLoginInfo(Context context, String device_key, String uid) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(USER_EMAIL, device_key);
        editor.putString(UID, uid);
        editor.apply();
    }

    public static String getUserEmail(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(USER_EMAIL, "");
    }

    public static String getSavedUid(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(UID, "");
    }
}
