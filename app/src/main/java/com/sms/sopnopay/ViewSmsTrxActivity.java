package com.sms.sopnopay;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ViewSmsTrxActivity extends AppCompatActivity {

    private RecyclerView rvSmsList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private EditText etSearchTrx, etDateFilter;
    private ImageButton btnClearDate, backButton;
    private AppCompatButton btnAll, btnSuccess, btnWaiting;

    private SmsListAdapter adapter;
    private List<HashMap<String, String>> allSmsList = new ArrayList<>();
    private List<HashMap<String, String>> filteredList = new ArrayList<>();
    private String currentFilter = "all";
    private String currentDateFilter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_sms_trx);

        initViews();
        setupRecyclerView();
        setupFilters();
        loadSmsData();
    }

    private void initViews() {
        rvSmsList = findViewById(R.id.rvSmsList);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearchTrx = findViewById(R.id.etSearchTrx);
        etDateFilter = findViewById(R.id.etDateFilter);
        btnClearDate = findViewById(R.id.btnClearDate);
        backButton = findViewById(R.id.backButton);
        btnAll = findViewById(R.id.btnAll);
        btnSuccess = findViewById(R.id.btnSuccess);
        btnWaiting = findViewById(R.id.btnWaiting);

        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new SmsListAdapter(filteredList);
        rvSmsList.setLayoutManager(new LinearLayoutManager(this));
        rvSmsList.setAdapter(adapter);
    }

    private void setupFilters() {
        btnAll.setSelected(true);

        View.OnClickListener filterClickListener = v -> {
            btnAll.setSelected(false);
            btnSuccess.setSelected(false);
            btnWaiting.setSelected(false);
            v.setSelected(true);

            if (v.getId() == R.id.btnAll) currentFilter = "all";
            else if (v.getId() == R.id.btnSuccess) currentFilter = "completed";
            else if (v.getId() == R.id.btnWaiting) currentFilter = "pending";
            applyFilters();
        };

        btnAll.setOnClickListener(filterClickListener);
        btnSuccess.setOnClickListener(filterClickListener);
        btnWaiting.setOnClickListener(filterClickListener);

        etSearchTrx.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        etDateFilter.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                currentDateFilter = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etDateFilter.setText(currentDateFilter);
                applyFilters();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnClearDate.setOnClickListener(v -> {
            currentDateFilter = "";
            etDateFilter.setText("");
            applyFilters();
        });
    }

    private void loadSmsData() {
        progressBar.setVisibility(View.VISIBLE);
        sqlite dbHelper = new sqlite(this);
        allSmsList.clear();

        ArrayList<HashMap<String, String>> smsData = dbHelper.getAllSms();
        for (HashMap<String, String> sms : smsData) {
            HashMap<String, String> item = new HashMap<>();
            item.put("title", sms.get("title") != null ? sms.get("title") : "");
            item.put("body", sms.get("body") != null ? sms.get("body") : "");
            item.put("status", "completed");
            item.put("id", sms.get("id") != null ? sms.get("id") : "");
            allSmsList.add(item);
        }

        progressBar.setVisibility(View.GONE);
        applyFilters();
    }

    private void applyFilters() {
        filteredList.clear();
        String query = etSearchTrx.getText().toString().toLowerCase();

        for (HashMap<String, String> item : allSmsList) {
            String title = item.get("title") != null ? item.get("title").toLowerCase() : "";
            String body = item.get("body") != null ? item.get("body").toLowerCase() : "";
            String status = item.get("status") != null ? item.get("status").toLowerCase() : "";
            String id = item.get("id") != null ? item.get("id").toLowerCase() : "";

            boolean matchesFilter = currentFilter.equals("all") || status.equals(currentFilter);
            boolean matchesSearch = query.isEmpty() || title.contains(query) || body.contains(query) || id.contains(query);

            if (matchesFilter && matchesSearch) {
                filteredList.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        rvSmsList.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    class SmsListAdapter extends RecyclerView.Adapter<SmsListAdapter.ViewHolder> {
        private final List<HashMap<String, String>> data;

        SmsListAdapter(List<HashMap<String, String>> data) {
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
            HashMap<String, String> item = data.get(position);
            holder.tvMessage.setText(item.get("title") != null ? item.get("title") : "SMS");
            holder.tvUpdatedAt.setText(item.get("body") != null ? item.get("body") : "");
            holder.tvAmount.setText("");
            holder.tvMethodId.setText("ID: " + (item.get("id") != null ? item.get("id") : ""));

            String status = item.get("status") != null ? item.get("status") : "pending";
            holder.tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

            if ("completed".equals(status)) {
                holder.tvStatus.setTextColor(0xFF2E7D32);
                holder.tvStatus.setBackgroundColor(0xFFE8F5E9);
            } else {
                holder.tvStatus.setTextColor(0xFFE65100);
                holder.tvStatus.setBackgroundColor(0xFFFFF3E0);
            }
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage, tvUpdatedAt, tvAmount, tvStatus, tvMethodId;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvUpdatedAt = itemView.findViewById(R.id.tvUpdatedAt);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvMethodId = itemView.findViewById(R.id.tvMethodId);
            }
        }
    }
}
