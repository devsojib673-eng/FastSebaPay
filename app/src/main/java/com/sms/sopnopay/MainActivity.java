package com.sms.sopnopay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    private static final int LOCATION_PERMISSION_CODE = 103;
    private static final int REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATIONS = 1001;

    private RequestQueue queue;
    private NetworkChangeReceiver networkChangeReceiver;
    private TextView runTxt, status;
    private LottieAnimationView lottie;
    ImageView nowifi;
    private ListView listView;
    private ProgressBar progressBar;


    private final ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private final sqlite dbHelper = new sqlite(this);


    private final Handler mHandler = new Handler();
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnectedToInternet() && !arrayList.isEmpty()) {
                // Perform your network operations here
            }
            mHandler.postDelayed(this, 10000); // Check every 10 seconds
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeNetworkChangeReceiver();
        checkAndRequestPermissions();
        initializeListView();
        initializeVolleyQueue();

        saveSmsToDatabase();

        // Ensure continuous background operation
        startForegroundService();
    }

    private void initializeViews() {
        runTxt = findViewById(R.id.runTxt);
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressbar);
        lottie = findViewById(R.id.lottie);
        status = findViewById(R.id.status);
        nowifi = findViewById(R.id.nowifi);
    }

    private void initializeNetworkChangeReceiver() {
        networkChangeReceiver = new NetworkChangeReceiver(this);
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    private void initializeVolleyQueue() {
        queue = Volley.newRequestQueue(this);
    }

    private void checkAndRequestPermissions() {
        if (checkSmsPermission() && checkLocationPermissions()) {
            if (checkNotificationPermission()) {
                startForegroundService();
            } else {
                requestNotificationPermission();
            }
        } else {
            requestSmsAndLocationPermissions();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkBatteryOptimization();
        }

        startBroadcastService();
    }

    private void initializeListView() {
        MyAdapter myAdapter = new MyAdapter();
        listView.setAdapter(myAdapter);
    }



    private boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void saveSmsToDatabase() {
        ArrayList<HashMap<String, String>> dbData = dbHelper.getAllSms();
        arrayList.clear();
        arrayList.addAll(dbData);
        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
    }

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return arrayList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
            View myView = layoutInflater.inflate(R.layout.message, parent, false);

            HashMap<String, String> hashMap = arrayList.get(position);
            String body = hashMap.get("title");
            String title = hashMap.get("body");

            TextView bodyx = myView.findViewById(R.id.body);
            TextView titlex = myView.findViewById(R.id.title);

            titlex.setText(body);
            bodyx.setText(title);

            return myView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkChangeReceiver);
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, MyBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d("MainActivity", "ForegroundService started");
    }

    private void startBroadcastService() {
        Intent serviceIntent = new Intent(this, BootReceiver.class);
        startService(serviceIntent);
        Log.d("MainActivity", "BroadcastService started");
    }

    public void updateNetworkStatus(boolean isConnected) {
        if (isConnected) {
            status.setText("Active Now!");
            lottie.setVisibility(View.VISIBLE);
            nowifi.setVisibility(View.GONE);
        } else {
            status.setText("No Internet Connection!");
            nowifi.setVisibility(View.VISIBLE);
            lottie.setVisibility(View.INVISIBLE);
            //ok

        }
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Confirmation")
                .setIcon(R.drawable.baseline_exit_to_app_24)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes, Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();  // Close all activities
                        System.exit(0);    // Exit the application
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }



    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkLocationPermissions() {
        boolean coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return coarseLocationPermission && fineLocationPermission;
    }

    private void requestSmsAndLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {Manifest.permission.RECEIVE_SMS, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, SMS_PERMISSION_CODE);
        }
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
    }

    private boolean checkNotificationPermission() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        return notificationManagerCompat.areNotificationsEnabled();
    }

    private void requestNotificationPermission() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivityForResult(intent, NOTIFICATION_PERMISSION_CODE);
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATIONS) {
            checkBatteryOptimization();
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (checkNotificationPermission()) {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. Some features may not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!checkLocationPermissions()) {
                    requestLocationPermissions();
                } else if (!checkNotificationPermission()) {
                    requestNotificationPermission();
                }
            } else {
                Toast.makeText(this, "SMS permission denied. The app may not work correctly.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!checkNotificationPermission()) {
                    requestNotificationPermission();
                }
            } else {
                Toast.makeText(this, "Location permission denied. The app may not work correctly.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
