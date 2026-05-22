package com.sms.sopnopay;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserHistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SearchView searchView;
    private ChipGroup statusChipGroup;
    private Chip chipAll, chipSuccess, chipPending, chipCancel, chipDateFilter;
    private Button btnDatePicker, btnFetch;
    private ImageButton btnBack;

    private RequestQueue queue;
    private TransactionAdapter adapter;
    private List<HashMap<String, String>> allTransactions = new ArrayList<>();
    private List<HashMap<String, String>> filteredTransactions = new ArrayList<>();
    private String currentStatusFilter = "all";
    private String currentDateFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_history);

        initViews();
        setupRecyclerView();
        setupChipFilters();
        setupSearch();
        setupDatePicker();
        fetchTransactions();

        btnBack.setOnClickListener(v -> finish());
        btnFetch.setOnClickListener(v -> fetchTransactions());
    }

    private void initViews() {
        rvTransactions = findViewById(R.id.rvTransactions);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        searchView = findViewById(R.id.searchView);
        statusChipGroup = findViewById(R.id.statusChipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipSuccess = findViewById(R.id.chipSuccess);
        chipPending = findViewById(R.id.chipPending);
        chipCancel = findViewById(R.id.chipCancel);
        chipDateFilter = findViewById(R.id.chipDateFilter);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        btnFetch = findViewById(R.id.btnFetch);
        btnBack = findViewById(R.id.btnBack);
        queue = Volley.newRequestQueue(this);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(filteredTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void setupChipFilters() {
        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) currentStatusFilter = "all";
            else if (checkedId == R.id.chipSuccess) currentStatusFilter = "success";
            else if (checkedId == R.id.chipPending) currentStatusFilter = "pending";
            else if (checkedId == R.id.chipCancel) currentStatusFilter = "cancel";
            applyFilters();
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilters();
                return true;
            }
        });
    }

    private void setupDatePicker() {
        btnDatePicker.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                currentDateFilter = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                chipDateFilter.setText(currentDateFilter);
                chipDateFilter.setVisibility(View.VISIBLE);
                applyFilters();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        chipDateFilter.setOnCloseIconClickListener(v -> {
            currentDateFilter = "";
            chipDateFilter.setVisibility(View.GONE);
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredTransactions.clear();
        String query = searchView.getQuery() != null ? searchView.getQuery().toString().toLowerCase() : "";

        for (HashMap<String, String> txn : allTransactions) {
            String status = txn.get("status") != null ? txn.get("status").toLowerCase() : "";
            String id = txn.get("id") != null ? txn.get("id").toLowerCase() : "";
            String message = txn.get("message") != null ? txn.get("message").toLowerCase() : "";
            String date = txn.get("date") != null ? txn.get("date") : "";

            boolean matchesStatus = currentStatusFilter.equals("all") || status.contains(currentStatusFilter);
            boolean matchesSearch = query.isEmpty() || id.contains(query) || message.contains(query);
            boolean matchesDate = currentDateFilter.isEmpty() || date.startsWith(currentDateFilter);

            if (matchesStatus && matchesSearch && matchesDate) {
                filteredTransactions.add(txn);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredTransactions.isEmpty() ? View.VISIBLE : View.GONE);
        rvTransactions.setVisibility(filteredTransactions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void fetchTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        String email = prefs.getString("user_email", "");
        String deviceKey = prefs.getString("device_key", "");

        String url = getString(R.string.api_history_url);

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.has("data")) {
                            JSONArray dataArray = jsonResponse.getJSONArray("data");
                            allTransactions.clear();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject item = dataArray.getJSONObject(i);
                                HashMap<String, String> txn = new HashMap<>();
                                txn.put("id", item.optString("id", ""));
                                txn.put("message", item.optString("message", item.optString("address", "")));
                                txn.put("amount", item.optString("amount", ""));
                                txn.put("status", item.optString("status", "pending"));
                                txn.put("date", item.optString("created_at", item.optString("date", "")));
                                txn.put("method_id", item.optString("method_id", item.optString("trx_id", "")));
                                allTransactions.add(txn);
                            }
                        }
                        applyFilters();
                    } catch (Exception e) {
                        tvEmpty.setText("Error loading data");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    loadLocalTransactions();
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

    private void loadLocalTransactions() {
        sqlite dbHelper = new sqlite(this);
        allTransactions.clear();
        ArrayList<HashMap<String, String>> localTxns = dbHelper.getAllTransactions();
        allTransactions.addAll(localTxns);
        applyFilters();
    }

    // RecyclerView Adapter
    class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private final List<HashMap<String, String>> data;

        TransactionAdapter(List<HashMap<String, String>> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> txn = data.get(position);

            holder.tvMessage.setText(txn.get("message") != null ? txn.get("message") : "Transaction");
            holder.tvUpdatedAt.setText(txn.get("date") != null ? txn.get("date") : "");
            holder.tvAmount.setText(txn.get("amount") != null && !txn.get("amount").isEmpty() ? txn.get("amount") : "");

            String methodId = txn.get("method_id");
            if (methodId != null && !methodId.isEmpty()) {
                holder.tvMethodId.setText("TRX: " + methodId);
            } else {
                holder.tvMethodId.setText("ID: " + (txn.get("id") != null ? txn.get("id") : ""));
            }

            String status = txn.get("status") != null ? txn.get("status").toLowerCase() : "pending";
            holder.tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

            switch (status) {
                case "completed":
                case "success":
                    holder.tvStatus.setTextColor(0xFF2E7D32);
                    holder.tvStatus.setBackgroundColor(0xFFE8F5E9);
                    holder.tvAmount.setTextColor(0xFF2E7D32);
                    break;
                case "pending":
                    holder.tvStatus.setTextColor(0xFFE65100);
                    holder.tvStatus.setBackgroundColor(0xFFFFF3E0);
                    holder.tvAmount.setTextColor(0xFFE65100);
                    break;
                case "cancel":
                case "cancelled":
                case "failed":
                    holder.tvStatus.setTextColor(0xFFD32F2F);
                    holder.tvStatus.setBackgroundColor(0xFFFFEBEE);
                    holder.tvAmount.setTextColor(0xFFD32F2F);
                    break;
                default:
                    holder.tvStatus.setTextColor(0xFF616161);
                    holder.tvStatus.setBackgroundColor(0xFFF5F5F5);
                    break;
            }

            holder.btnCopyTrx.setOnClickListener(v -> {
                String trxId = holder.tvMethodId.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("TRX ID", trxId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(UserHistoryActivity.this, "Copied: " + trxId, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage, tvUpdatedAt, tvAmount, tvStatus, tvMethodId;
            View btnCopyTrx;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvUpdatedAt = itemView.findViewById(R.id.tvUpdatedAt);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvMethodId = itemView.findViewById(R.id.tvMethodId);
                btnCopyTrx = itemView.findViewById(R.id.btnCopyTrx);
            }
        }
    }
}
