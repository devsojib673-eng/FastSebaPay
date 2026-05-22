package com.sms.sopnopay;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvUserName, tvDeviceKey;
    private ImageButton btnCopyKey;
    private CardView cardPlan, cardHistory, cardSms, cardProfile, cardSupport, cardSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvDeviceKey = findViewById(R.id.tvDeviceKey);
        btnCopyKey = findViewById(R.id.btnCopyKey);
        cardPlan = findViewById(R.id.cardPlan);
        cardHistory = findViewById(R.id.cardHistory);
        cardSms = findViewById(R.id.cardSms);
        cardProfile = findViewById(R.id.cardProfile);
        cardSupport = findViewById(R.id.cardSupport);
        cardSettings = findViewById(R.id.cardSettings);
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        String email = prefs.getString("user_email", "User");
        String key = prefs.getString("device_key", "No Key");

        tvUserName.setText(email);
        tvDeviceKey.setText(key);
    }

    private void setupClickListeners() {
        btnCopyKey.setOnClickListener(v -> {
            String key = tvDeviceKey.getText().toString();
            if (!key.isEmpty() && !key.equals("No Key")) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Device Key", key);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Device Key copied!", Toast.LENGTH_SHORT).show();
            }
        });

        cardSupport.setOnClickListener(v -> {
            String phone = getString(R.string.whatsapp_number);
            String url = "https://wa.me/" + phone.replace("+", "");
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });

        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTnxActivity.class);
            startActivity(intent);
        });

        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserHistoryActivity.class);
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> showDeveloperInfo());

        cardSms.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewSmsTrxActivity.class);
            startActivity(intent);
        });

        cardPlan.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserPlanActivity.class);
            startActivity(intent);
        });
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
}
