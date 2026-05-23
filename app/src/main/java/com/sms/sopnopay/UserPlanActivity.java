package com.sms.sopnopay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UserPlanActivity extends AppCompatActivity {

    private TextView tvPlanId, tvPrice, tvBrand, tvDevice, tvTransaction, tvKey, tvExpire, tvNoPlan;
    private ProgressBar progressBar;
    private CardView planContactSupport;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_plan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        fetchPlanData();
    }

    private void initViews() {
        tvPlanId = findViewById(R.id.tvPlanId);
        tvPrice = findViewById(R.id.tvPrice);
        tvBrand = findViewById(R.id.tvBrand);
        tvDevice = findViewById(R.id.tvDevice);
        tvTransaction = findViewById(R.id.tvTransaction);
        tvKey = findViewById(R.id.tvKey);
        tvExpire = findViewById(R.id.tvExpire);
        tvNoPlan = findViewById(R.id.tvNoPlan);
        progressBar = findViewById(R.id.progressBar);
        planContactSupport = findViewById(R.id.planContactSupport);
        queue = Volley.newRequestQueue(this);

        planContactSupport.setOnClickListener(v -> {
            String phone = getString(R.string.whatsapp_number);
            String url = "https://wa.me/" + phone.replace("+", "");
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPlanData() {
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        String email = prefs.getString("user_email", "");
        String deviceKey = prefs.getString("device_key", "");

        String url = getString(R.string.base_url) + "/app-api/v1/user_plan.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("success", false) || json.has("plan_id") || json.has("data")) {
                            JSONObject data = json.has("data") ? json.getJSONObject("data") : json;

                            tvPlanId.setText("Plan ID: " + data.optString("plan_id", data.optString("id", "--")));
                            tvPrice.setText("Price: " + data.optString("price", data.optString("amount", "--")) + " TK");
                            tvBrand.setText("Brand: " + data.optString("brand", data.optString("brand_name", "--")));
                            tvDevice.setText("Device: " + data.optString("device", data.optString("device_count", "--")));
                            tvTransaction.setText("Transaction: " + data.optString("transaction", data.optString("trx_count", "--")));
                            tvKey.setText("Key: " + data.optString("key", data.optString("device_key", "--")));
                            tvExpire.setText("Expire: " + data.optString("expire", data.optString("expiry_date", "--")));
                            tvNoPlan.setVisibility(View.GONE);
                        } else {
                            showNoPlan();
                        }
                    } catch (Exception e) {
                        showNoPlan();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    showLocalPlanData();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_email", email);
                params.put("device_key", deviceKey);
                return params;
            }
        };

        queue.add(request);
    }

    private void showLocalPlanData() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        String email = prefs.getString("user_email", "");
        String deviceKey = prefs.getString("device_key", "");

        sqlite dbHelper = new sqlite(this);
        int totalTxn = dbHelper.getTransactionCount();

        tvPlanId.setText("Plan ID: Local");
        tvPrice.setText("Price: --");
        tvBrand.setText("Brand: FastSebaPay");
        tvDevice.setText("Device: 1");
        tvTransaction.setText("Transaction: " + totalTxn);
        tvKey.setText("Key: " + deviceKey);
        tvExpire.setText("Expire: --");
        tvNoPlan.setVisibility(View.GONE);
    }

    private void showNoPlan() {
        tvPlanId.setText("Plan ID: --");
        tvPrice.setText("Price: --");
        tvBrand.setText("Brand: --");
        tvDevice.setText("Device: --");
        tvTransaction.setText("Transaction: --");
        tvKey.setText("Key: --");
        tvExpire.setText("Expire: --");
        tvNoPlan.setVisibility(View.VISIBLE);
    }
}
