package com.sms.sopnopay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    private static final int LOCATION_PERMISSION_CODE = 103;
    private static final int REQUEST_CODE_IGNORE_BATTERY_OPTIMIZATIONS = 1001;

    private RequestQueue queue;
    private NetworkChangeReceiver networkChangeReceiver;
    private TextView status;
    private LottieAnimationView lottie;
    ImageView nowifi;
    private ListView listView;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNav;
    private Toolbar toolbar;
    private TextView marqueeNotice;

    private final ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private sqlite dbHelper;

    private Handler mHandler;
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnectedToInternet() && !arrayList.isEmpty()) {
                // Perform network operations
            }
            mHandler.postDelayed(this, 10000);
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new sqlite(this);
        mHandler = new Handler(getMainLooper());

        initializeViews();
        setupToolbar();
        setupBottomNavigation();
        fetchNotice();

        marqueeNotice.setSelected(true);

        initializeNetworkChangeReceiver();
        checkAndRequestPermissions();
        initializeListView();
        initializeVolleyQueue();
        saveSmsToDatabase();
        startForegroundService();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNav = findViewById(R.id.bottom_nav);
        marqueeNotice = findViewById(R.id.marqueeNotice);
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressbar);
        lottie = findViewById(R.id.lottie);
        status = findViewById(R.id.status);
        nowifi = findViewById(R.id.nowifi);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_home) {
            // Already on home
        } else if (id == R.id.menu_add_tnx) {
            startActivity(new Intent(this, AddTnxActivity.class));
        } else if (id == R.id.menu_view_sms) {
            startActivity(new Intent(this, ViewSmsTrxActivity.class));
        } else if (id == R.id.menu_notification) {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_developer) {
            showDeveloperInfo();
        } else if (id == R.id.menu_logout) {
            performLogout();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already on home
            } else if (id == R.id.nav_add_trx) {
                startActivity(new Intent(this, AddTnxActivity.class));
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
            } else if (id == R.id.nav_notification) {
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_tutorial) {
                String url = getString(R.string.base_url);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
            return true;
        });
    }

    private void fetchNotice() {
        String url = getString(R.string.api_notice_url);
        RequestQueue rq = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String notice = json.optString("notice", json.optString("message", ""));
                        if (!notice.isEmpty()) {
                            marqueeNotice.setText(notice);
                        }
                    } catch (Exception e) {
                        // Keep default notice
                    }
                },
                error -> {
                    // Keep default notice
                });

        rq.add(request);
    }

    private void showDeveloperInfo() {
        new AlertDialog.Builder(this)
                .setTitle("Developer Info")
                .setMessage("FastSebaPay\n\nDeveloped by FastSebaPay Team\n\nWebsite: fastsebapay.top\nEmail: " + getString(R.string.admin_email) + "\nWhatsApp: " + getString(R.string.whatsapp_number) + "\n\nVersion: 1.0")
                .setPositiveButton("Visit Website", (dialog, which) -> {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.base_url))));
                })
                .setNeutralButton("WhatsApp", (dialog, which) -> {
                    String phone = getString(R.string.whatsapp_number);
                    String url = "https://wa.me/" + phone.replace("+", "");
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void performLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // SMS Adapter
    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() { return arrayList.size(); }

        @Override
        public Object getItem(int position) { return null; }

        @Override
        public long getItemId(int position) { return position; }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkChangeReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, MyBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void startBroadcastService() {
        Intent serviceIntent = new Intent(this, BootReceiver.class);
        startService(serviceIntent);
    }

    public void updateNetworkStatus(boolean isConnected) {
        if (isConnected) {
            status.setText("Active Now!");
            status.setVisibility(View.GONE);
            nowifi.setVisibility(View.GONE);
        } else {
            status.setText("No Internet Connection!");
            status.setVisibility(View.VISIBLE);
            nowifi.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Confirmation")
                .setIcon(R.drawable.baseline_exit_to_app_24)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes, Exit", (dialog, which) -> {
                    finishAffinity();
                    System.exit(0);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
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
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
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
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!checkNotificationPermission()) {
                    requestNotificationPermission();
                }
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
