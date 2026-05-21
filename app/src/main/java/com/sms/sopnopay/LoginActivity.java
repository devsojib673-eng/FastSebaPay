package com.sms.sopnopay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.blogspot.atifsoftwares.animatoolib.Animatoo;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText userEmail, deviceKey;
    private CheckBox rememberMeCheckbox;
    private RequestQueue requestQueue;
    private LottieAnimationView lottie;


    public static String EMAIL = "";
    public static String DEVICEKEY = "";
    public static String STATUS = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Lock orientation to portrait programmatically
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize UI components
        userEmail = findViewById(R.id.userEmail);
        deviceKey = findViewById(R.id.device_key);
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox);
        lottie = findViewById(R.id.lottie);
        requestQueue = Volley.newRequestQueue(this);


        if (STATUS.contains("1")){

            String intentemail = getIntent().getStringExtra("EMAIL");
            String intentdevicekey = getIntent().getStringExtra("DEVICEKEY");


            EMAIL=intentemail;
            DEVICEKEY=intentdevicekey;

            if (EMAIL.length()>5){



                userEmail.setText(EMAIL);
                deviceKey.setText(DEVICEKEY);


                handleLogin();


            };

        } else {





        }




        findViewById(R.id.loginButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-lock orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void handleLogin() {
        String url = "https://selfnumberpay.mcmmadaripur.com/api/device-connect";
        String username = userEmail.getText().toString().trim();
        String password = deviceKey.getText().toString().trim();
        String deviceIp = getAndroidId(this);

        if (username.isEmpty()) {
            userEmail.setError("Empty Field");
            return;
        }

        if (password.isEmpty()) {
            deviceKey.setError("Empty Field");
            return;
        }

        lottie.setVisibility(View.VISIBLE);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        lottie.setVisibility(View.GONE);
                        handleVerificationResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        lottie.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Api not live", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("user_email", username);
                params.put("device_key", password);
                params.put("device_ip", deviceIp);
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        requestQueue.add(postRequest);
    }


    private void handleVerificationResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            int status = jsonResponse.getInt("status");


            Log.e("response", response);

            if (status == 1) {
                Toast.makeText(this, "Verification Success", Toast.LENGTH_SHORT).show();

                SharedPreferences preferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                String username = userEmail.getText().toString();
                String password = deviceKey.getText().toString();
                String deviceIp = getAndroidId(this);

                editor.putString("user_email", username);
                editor.putString("device_key", password);
                editor.putString("device_ip", deviceIp);
                editor.apply();



                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
                Animatoo.animateSwipeRight(LoginActivity.this);

                // Perform additional actions on success, if needed
            } else {
                if (status == 3){

                    Toast.makeText(this, "Already Login", Toast.LENGTH_SHORT).show();
                } else {


                    Toast.makeText(this, "Verification Failed", Toast.LENGTH_SHORT).show();


                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing JSON", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }



    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Exit")
                .setMessage("এপ থেকে বের হতে চান?")
                .setPositiveButton("হ্যা", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton("না", null)
                .show();
    }
}
