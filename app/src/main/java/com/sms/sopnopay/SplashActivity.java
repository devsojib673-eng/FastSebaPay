package com.sms.sopnopay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);


        SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        String email = preferences.getString("user_email", "");

        if (email.length() > 1) {

            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            Animatoo.animateSwipeLeft(SplashActivity.this);

        } else {

            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            Animatoo.animateSwipeLeft(SplashActivity.this);


        }


    }
}
