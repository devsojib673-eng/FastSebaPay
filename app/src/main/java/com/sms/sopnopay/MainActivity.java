package com.sms.sopnopay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
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

    // UI elements
    private BottomNavigationView bottomNavigation;
    private Toolbar toolbar;
    private TextView marqueeNotice;

    // Dashboard elements
    private TextView userEmailText, deviceKeyText;
    private ImageView copyDeviceKey;

    // Content views
    private View dashboardView, addTransactionView, viewSmsView, historyView, notificationView, myPlanView;

    // Add Transaction elements
    private Spinner addressSpinner;
    private EditText messageContent;
    private Button btnSendTransaction;

    // History elements
    private ListView historyListView;
    private Button btnAll, btnPending, btnCompleted;

    // View SMS elements
    private ListView smsListView;

    // My Plan elements
    private TextView planName, planStatus, totalTransactions, completedTransactions;
    private CardView planContactSupport;

    private final ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private final sqlite dbHelper = new sqlite(this);

    private final Handler mHandler = new Handler();
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

        initializeViews();
        setupToolbar();
        setupBottomNavigation();
        setupAddTransaction();
        setupHistory();
        setupMyPlan();

        // Start marquee
        marqueeNotice.setSelected(true);

        initializeNetworkChangeReceiver();
        checkAndRequestPermissions();
        initializeListView();
        initializeVolleyQueue();

        loadUserInfo();
        saveSmsToDatabase();

        startForegroundService();
    }

    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        toolbar = findViewById(R.id.toolbar);
        marqueeNotice = findViewById(R.id.marqueeNotice);

        // Dashboard
        dashboardView = findViewById(R.id.dashboard_view);
        userEmailText = findViewById(R.id.userEmailText);
        deviceKeyText = findViewById(R.id.deviceKeyText);
        copyDeviceKey = findViewById(R.id.copyDeviceKey);

        // Content sections
        addTransactionView = findViewById(R.id.add_transaction_view);
        viewSmsView = findViewById(R.id.view_sms_view);
        historyView = findViewById(R.id.history_view);
        notificationView = findViewById(R.id.notification_view);
        myPlanView = findViewById(R.id.my_plan_view);

        // Old views
        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressbar);
        lottie = findViewById(R.id.lottie);
        status = findViewById(R.id.status);
        nowifi = findViewById(R.id.nowifi);

        // Copy device key
        copyDeviceKey.setOnClickListener(v -> {
            String key = deviceKeyText.getText().toString();
            if (!key.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Device Key", key);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Device Key copied!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("FastSebaPay");
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
            showSection("home");
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else if (id == R.id.menu_add_tnx) {
            showSection("addTransaction");
            bottomNavigation.setSelectedItemId(R.id.nav_add_trx);
        } else if (id == R.id.menu_view_sms) {
            showSection("sms");
            loadSmsData();
            bottomNavigation.setSelectedItemId(R.id.nav_stored_data);
        } else if (id == R.id.menu_notification) {
            showSection("notifications");
        } else if (id == R.id.menu_developer) {
            showDeveloperInfo();
        } else if (id == R.id.menu_logout) {
            performLogout();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showSection("home");
            } else if (id == R.id.nav_add_trx) {
                showSection("addTransaction");
            } else if (id == R.id.nav_trx_history) {
                showSection("history");
                loadTransactionHistory(null);
            } else if (id == R.id.nav_stored_data) {
                showSection("sms");
                loadSmsData();
            } else if (id == R.id.nav_my_plan) {
                showSection("myPlan");
                loadMyPlanData();
            }
            return true;
        });
    }

    private void setupAddTransaction() {
        addressSpinner = findViewById(R.id.addressSpinner);
        messageContent = findViewById(R.id.messageContent);
        btnSendTransaction = findViewById(R.id.btnSendTransaction);

        ArrayList<String> addresses = new ArrayList<>();
        addresses.add("-- Select Address --");
        addresses.add("bKash");
        addresses.add("Nagad");
        addresses.add("Rocket");
        addresses.add("Upay");
        addresses.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, addresses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addressSpinner.setAdapter(adapter);

        btnSendTransaction.setOnClickListener(v -> sendTransaction());
    }

    private void setupHistory() {
        historyListView = findViewById(R.id.historyListView);
        btnAll = findViewById(R.id.btnAll);
        btnPending = findViewById(R.id.btnPending);
        btnCompleted = findViewById(R.id.btnCompleted);

        btnAll.setOnClickListener(v -> loadTransactionHistory(null));
        btnPending.setOnClickListener(v -> loadTransactionHistory("pending"));
        btnCompleted.setOnClickListener(v -> loadTransactionHistory("completed"));
    }

    private void setupMyPlan() {
        planName = findViewById(R.id.planName);
        planStatus = findViewById(R.id.planStatus);
        totalTransactions = findViewById(R.id.totalTransactions);
        completedTransactions = findViewById(R.id.completedTransactions);
        planContactSupport = findViewById(R.id.planContactSupport);

        planContactSupport.setOnClickListener(v -> openWhatsAppContact());
    }

    private void showSection(String section) {
        dashboardView.setVisibility(View.GONE);
        addTransactionView.setVisibility(View.GONE);
        viewSmsView.setVisibility(View.GONE);
        historyView.setVisibility(View.GONE);
        notificationView.setVisibility(View.GONE);
        myPlanView.setVisibility(View.GONE);

        switch (section) {
            case "home":
                dashboardView.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("FastSebaPay");
                break;
            case "addTransaction":
                addTransactionView.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Add Transaction");
                break;
            case "sms":
                viewSmsView.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Stored Data");
                break;
            case "history":
                historyView.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Transaction History");
                break;
            case "notifications":
                notificationView.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Notifications");
                break;
            case "myPlan":
                myPlanView.setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("My Plan");
                break;
        }
    }

    private void loadUserInfo() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        String email = preferences.getString("user_email", "");
        String key = preferences.getString("device_key", "");

        userEmailText.setText(email);
        deviceKeyText.setText(key);
    }

    private void loadMyPlanData() {
        int total = dbHelper.getTransactionCount();
        ArrayList<HashMap<String, String>> completedList = dbHelper.getTransactionsByStatus("completed");
        int completed = completedList.size();

        totalTransactions.setText(String.valueOf(total));
        completedTransactions.setText(String.valueOf(completed));
        planName.setText("FastSebaPay Basic");
        planStatus.setText("Active");
    }

    private void sendTransaction() {
        String address = addressSpinner.getSelectedItem().toString();
        String message = messageContent.getText().toString().trim();

        if (address.equals("-- Select Address --")) {
            Toast.makeText(this, "Please select an address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter message content", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        String user_email = preferences.getString("user_email", "");
        String device_key = preferences.getString("device_key", "");
        String device_ip = preferences.getString("device_ip", "");

        String url = getString(R.string.api_add_data);

        long txnId = dbHelper.saveTransaction(address, message, "pending");

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        int statusVal = jsonResponse.getInt("status");
                        if (statusVal == 1) {
                            dbHelper.updateTransactionStatus(txnId, "completed");
                            Toast.makeText(this, "Transaction sent successfully!", Toast.LENGTH_SHORT).show();
                            messageContent.setText("");
                            addressSpinner.setSelection(0);
                        } else {
                            Toast.makeText(this, "Transaction failed. Saved as pending.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error. Transaction saved as pending.", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("user_email", user_email);
                params.put("device_key", device_key);
                params.put("device_ip", device_ip);
                params.put("address", address);
                params.put("message", message);
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        queue.add(postRequest);
    }

    private void loadTransactionHistory(String statusFilter) {
        ArrayList<HashMap<String, String>> transactions;
        if (statusFilter == null) {
            transactions = dbHelper.getAllTransactions();
        } else {
            transactions = dbHelper.getTransactionsByStatus(statusFilter);
        }

        TransactionAdapter adapter = new TransactionAdapter(transactions);
        historyListView.setAdapter(adapter);

        TextView emptyText = findViewById(R.id.emptyHistoryText);
        if (transactions.isEmpty()) {
            historyListView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            historyListView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    private void loadSmsData() {
        smsListView = findViewById(R.id.smsListView);
        ArrayList<HashMap<String, String>> smsList = dbHelper.getAllSms();

        SmsAdapter adapter = new SmsAdapter(smsList);
        smsListView.setAdapter(adapter);

        TextView emptyText = findViewById(R.id.emptySmsText);
        if (smsList.isEmpty()) {
            smsListView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            smsListView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    private void openWhatsAppContact() {
        String phone = getString(R.string.whatsapp_number);
        String url = "https://wa.me/" + phone.replace("+", "");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeveloperInfo() {
        new AlertDialog.Builder(this)
                .setTitle("Developer Info")
                .setMessage("FastSebaPay\n\nDeveloped by FastSebaPay Team\n\nWebsite: fastsebapay.top\nEmail: " + getString(R.string.admin_email) + "\nWhatsApp: " + getString(R.string.whatsapp_number) + "\n\nVersion: 1.0")
                .setPositiveButton("Visit Website", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.base_url)));
                    startActivity(intent);
                })
                .setNeutralButton("WhatsApp", (dialog, which) -> openWhatsAppContact())
                .setNegativeButton("Close", null)
                .show();
    }

    private void performLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences preferences = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.clear();
                    editor.apply();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Adapters
    private class TransactionAdapter extends BaseAdapter {
        private final ArrayList<HashMap<String, String>> data;

        TransactionAdapter(ArrayList<HashMap<String, String>> data) {
            this.data = data;
        }

        @Override
        public int getCount() { return data.size(); }

        @Override
        public Object getItem(int position) { return data.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View view = inflater.inflate(R.layout.item_transaction, parent, false);

            HashMap<String, String> txn = data.get(position);

            TextView txnAddress = view.findViewById(R.id.txnAddress);
            TextView txnMessage = view.findViewById(R.id.txnMessage);
            TextView txnDate = view.findViewById(R.id.txnDate);
            TextView txnStatus = view.findViewById(R.id.txnStatus);

            txnAddress.setText(txn.get("address"));
            txnMessage.setText(txn.get("message"));
            txnDate.setText(txn.get("date"));

            String statusText = txn.get("status");
            txnStatus.setText(statusText != null ? statusText.toUpperCase() : "PENDING");

            if ("completed".equals(statusText)) {
                txnStatus.setBackgroundColor(getResources().getColor(R.color.completed_color));
            } else if ("pending".equals(statusText)) {
                txnStatus.setBackgroundColor(getResources().getColor(R.color.pending_color));
            } else {
                txnStatus.setBackgroundColor(getResources().getColor(R.color.failed_color));
            }

            return view;
        }
    }

    private class SmsAdapter extends BaseAdapter {
        private final ArrayList<HashMap<String, String>> data;

        SmsAdapter(ArrayList<HashMap<String, String>> data) {
            this.data = data;
        }

        @Override
        public int getCount() { return data.size(); }

        @Override
        public Object getItem(int position) { return data.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View view = inflater.inflate(R.layout.message, parent, false);

            HashMap<String, String> sms = data.get(position);
            TextView bodyx = view.findViewById(R.id.body);
            TextView titlex = view.findViewById(R.id.title);

            titlex.setText(sms.get("title"));
            bodyx.setText(sms.get("body"));

            return view;
        }
    }

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
        if (dashboardView.getVisibility() != View.VISIBLE) {
            showSection("home");
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            return;
        }

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
