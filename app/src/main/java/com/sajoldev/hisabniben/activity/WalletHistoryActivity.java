package com.sajoldev.hisabniben.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.WalletTransactionAdapter;
import com.sajoldev.hisabniben.dialog.WalletTransactionDetailsBottomSheet;
import com.sajoldev.hisabniben.model.WalletTransaction;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class WalletHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "account_id";

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearchHistory;
    private ChipGroup cgDirectionFilter, cgDateFilter;
    private RecyclerView rvWalletHistory;
    private TextView tvEmptyHistory;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private WalletTransactionAdapter adapter;

    private List<WalletTransaction> allTransactions = new ArrayList<>();
    private List<WalletTransaction> filteredTransactions = new ArrayList<>();

    private String currentSearchQuery = "";
    private int checkedDirectionId = R.id.chipDirAll;
    private int checkedDateId = R.id.chipDateAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_wallet_history);

        sessionManager = SessionManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadHistoryData();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            int topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(view.getPaddingLeft(), topInsets, view.getPaddingRight(), view.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        etSearchHistory = findViewById(R.id.etSearchHistory);
        cgDirectionFilter = findViewById(R.id.cgDirectionFilter);
        cgDateFilter = findViewById(R.id.cgDateFilter);
        rvWalletHistory = findViewById(R.id.rvWalletHistory);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("টাকা-পয়সার ইতিহাস (Wallet History)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new WalletTransactionAdapter(this);
        adapter.setOnTransactionClickListener(transaction -> {
            WalletTransactionDetailsBottomSheet sheet = WalletTransactionDetailsBottomSheet.newInstance(transaction);
            sheet.show(getSupportFragmentManager(), "TransactionDetails");
        });
        rvWalletHistory.setLayoutManager(new LinearLayoutManager(this));
        rvWalletHistory.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadHistoryData);

        etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        cgDirectionFilter.setOnCheckedChangeListener((group, checkedId) -> {
            checkedDirectionId = checkedId;
            applyFilters();
        });

        cgDateFilter.setOnCheckedChangeListener((group, checkedId) -> {
            checkedDateId = checkedId;
            applyFilters();
        });
    }

    private void loadHistoryData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        firestoreManager.getWalletTransactions(userId, new FirestoreManager.FirestoreListCallback<WalletTransaction>() {
            @Override
            public void onSuccess(List<WalletTransaction> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                allTransactions = result != null ? result : new ArrayList<>();
                applyFilters();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(WalletHistoryActivity.this, "ইতিহাস লোড করতে ব্যর্থ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredTransactions.clear();

        long startTime = 0;

        if (checkedDateId == R.id.chipDateToday) {
            startTime = getTodayStartTimestamp();
        } else if (checkedDateId == R.id.chipDate7Days) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
            cal.add(Calendar.DAY_OF_YEAR, -6);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            startTime = cal.getTimeInMillis();
        } else if (checkedDateId == R.id.chipDateMonth) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            startTime = cal.getTimeInMillis();
        }

        for (WalletTransaction t : allTransactions) {
            long tDate = t.getTransactionDate() > 0 ? t.getTransactionDate() : t.getCreatedAt();
            if (startTime > 0 && tDate < startTime) continue;

            if (checkedDirectionId == R.id.chipDirIn && !WalletTransaction.DIRECTION_IN.equals(t.getDirection())) {
                continue;
            }
            if (checkedDirectionId == R.id.chipDirOut && !WalletTransaction.DIRECTION_OUT.equals(t.getDirection())) {
                continue;
            }

            if (!currentSearchQuery.isEmpty()) {
                boolean matchCat = t.getCategory() != null && t.getCategory().toLowerCase().contains(currentSearchQuery);
                boolean matchAcc = t.getAccountName() != null && t.getAccountName().toLowerCase().contains(currentSearchQuery);
                boolean matchCust = t.getCustomerName() != null && t.getCustomerName().toLowerCase().contains(currentSearchQuery);
                boolean matchSupp = t.getSupplierName() != null && t.getSupplierName().toLowerCase().contains(currentSearchQuery);
                boolean matchDesc = t.getDescription() != null && t.getDescription().toLowerCase().contains(currentSearchQuery);
                boolean matchRef = t.getReference() != null && t.getReference().toLowerCase().contains(currentSearchQuery);

                if (!matchCat && !matchAcc && !matchCust && !matchSupp && !matchDesc && !matchRef) {
                    continue;
                }
            }

            filteredTransactions.add(t);
        }

        adapter.setTransactions(filteredTransactions);

        if (filteredTransactions.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvWalletHistory.setVisibility(View.GONE);
        } else {
            tvEmptyHistory.setVisibility(View.GONE);
            rvWalletHistory.setVisibility(View.VISIBLE);
        }
    }

    private long getTodayStartTimestamp() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
