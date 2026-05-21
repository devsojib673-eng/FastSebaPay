package com.sms.sopnopay;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class MyBackgroundService extends Service {

    private static final int NOTIFICATION_ID = 1000;
    private static final String CHANNEL_ID = "MyBackgroundServiceChannel";
    private static final String ACTION_MAIN_ACTIVITY = "com.sms.sohojpay.ACTION_MAIN_ACTIVITY";

    private ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private sqlite dbHelper;
    private RequestQueue requestQueue;

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean isConnected = isNetworkConnected();
                if (isConnected) {
                    updateBackgroundNotification("Background is running", "Background Process");
                    saveSmsFromDatabase();
                    uploadDataToServer(context);
                } else {
                    updateBackgroundNotification("Please check your network connection.", "Background Process");
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new sqlite(this);
        requestQueue = Volley.newRequestQueue(this);

        registerConnectivityReceiver();
        createNotificationChannel();

        startForeground(NOTIFICATION_ID, createBackgroundNotification("Background is running", "Background Process"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_MAIN_ACTIVITY)) {
            // Handle the action from MainActivity if needed
        }
        // Ensure the service restarts if it gets terminated
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterConnectivityReceiver();
        restartService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerConnectivityReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);
    }

    private void unregisterConnectivityReceiver() {
        unregisterReceiver(connectivityReceiver);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MyBackgroundService Channel";
            String description = "Channel for MyBackgroundService";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateBackgroundNotification(String contentText, String contentTitle) {
        Notification notification = createBackgroundNotification(contentText, contentTitle);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Handle permission check and request if necessary
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification createBackgroundNotification(String contentText, String contentTitle) {
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.setAction(ACTION_MAIN_ACTIVITY);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.selfpayfev)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void saveSmsFromDatabase() {
        ArrayList<HashMap<String, String>> dbData = dbHelper.getAllSms();
        arrayList.clear();
        arrayList.addAll(dbData);
    }

    private void uploadDataToServer(Context context) {
        if (!arrayList.isEmpty()) {
            HashMap<String, String> data = arrayList.get(0);
            String id = data.get("id");
            String title = data.get(sqlite.COLUMN_TITLE);
            String body = data.get(sqlite.COLUMN_BODY);

            SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
            String user_email = preferences.getString("user_email", "");
            String device_key = preferences.getString("device_key", "");
            String device_ip = preferences.getString("device_ip", "");

            String url = "https://selfnumberpay.mcmmadaripur.com/api/add-data";

            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            int status = jsonResponse.getInt("status");

                            if (status == 1) {
                                deleteFromDatabase(id);
                                uploadDataToServer(context); // Call recursively
                            } else if (status == 0 ){


                                // Handle failure response
                            } else {

                                deleteFromDatabase(id);
                                uploadDataToServer(context); // Call recursively


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();


                        }
                    },
                    error -> Log.e("MyBackgroundService", "Server error: " + error.getMessage())
            ) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_email", user_email);
                    params.put("device_key", device_key);
                    params.put("device_ip", device_ip);
                    params.put("address", title);
                    params.put("message", body);
                    return params;
                }

                @Override
                public String getBodyContentType() {
                    return "application/x-www-form-urlencoded; charset=UTF-8";
                }
            };

            requestQueue.add(postRequest);
        }
    }

    private void deleteFromDatabase(String id) {
        dbHelper.deleteSms(Long.parseLong(id));
        saveSmsFromDatabase();
    }

    private void restartService() {
        Intent restartServiceIntent = new Intent(this, MyBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent);
        } else {
            startService(restartServiceIntent);
        }
    }
}
