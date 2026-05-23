package com.sms.sopnopay;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddTnxActivity extends AppCompatActivity {

    private Spinner addressSpinner;
    private EditText messageContent;
    private Button btnSendTransaction;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_transaction);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        initViews();
        setupSpinner();
    }

    private void initViews() {
        addressSpinner = findViewById(R.id.addressSpinner);
        messageContent = findViewById(R.id.messageContent);
        btnSendTransaction = findViewById(R.id.btnSendTransaction);
        queue = Volley.newRequestQueue(this);

        btnSendTransaction.setOnClickListener(v -> sendTransaction());
    }

    private void setupSpinner() {
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

        SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        String email = prefs.getString("user_email", "");
        String deviceKey = prefs.getString("device_key", "");
        String deviceIp = prefs.getString("device_ip", "");

        String url = getString(R.string.api_add_data);

        sqlite dbHelper = new sqlite(this);
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
                    } catch (Exception e) {
                        Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error. Transaction saved as pending.", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_email", email);
                params.put("device_key", deviceKey);
                params.put("device_ip", deviceIp);
                params.put("address", address);
                params.put("message", message);
                return params;
            }
        };

        queue.add(postRequest);
    }
}
