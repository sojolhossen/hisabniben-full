package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.WalletAccountAdapter;
import com.sajoldev.hisabniben.adapter.WalletTransactionAdapter;
import com.sajoldev.hisabniben.dialog.AddTransactionDialog;
import com.sajoldev.hisabniben.dialog.WalletTransactionDetailsBottomSheet;
import com.sajoldev.hisabniben.dialog.WalletTransferDialog;
import com.sajoldev.hisabniben.model.WalletAccount;
import com.sajoldev.hisabniben.model.WalletTransaction;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class WalletDashboardActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalBusinessMoney, tvTodayIn, tvTodayOut, tvTodayNet, tvViewAllHistory;
    private RecyclerView rvWalletAccounts, rvRecentWalletTransactions;
    private MaterialButton btnQuickCashIn, btnQuickCashOut, btnQuickTransfer, btnQuickHistory, btnManageAccounts;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;

    private WalletAccountAdapter accountAdapter;
    private WalletTransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_wallet_dashboard);

        sessionManager = SessionManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerViews();
        setupListeners();
        loadWalletDashboardData();
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

        tvTotalBusinessMoney = findViewById(R.id.tvTotalBusinessMoney);
        tvTodayIn = findViewById(R.id.tvTodayIn);
        tvTodayOut = findViewById(R.id.tvTodayOut);
        tvTodayNet = findViewById(R.id.tvTodayNet);
        tvViewAllHistory = findViewById(R.id.tvViewAllHistory);

        rvWalletAccounts = findViewById(R.id.rvWalletAccounts);
        rvRecentWalletTransactions = findViewById(R.id.rvRecentWalletTransactions);

        btnQuickCashIn = findViewById(R.id.btnQuickCashIn);
        btnQuickCashOut = findViewById(R.id.btnQuickCashOut);
        btnQuickTransfer = findViewById(R.id.btnQuickTransfer);
        btnQuickHistory = findViewById(R.id.btnQuickHistory);
        btnManageAccounts = findViewById(R.id.btnManageAccounts);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ব্যবসা ক্যাশ ও ওয়ালেট (Business Wallet)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerViews() {
        accountAdapter = new WalletAccountAdapter(this);
        accountAdapter.setOnAccountClickListener(account -> {
            Intent intent = new Intent(this, WalletHistoryActivity.class);
            intent.putExtra(WalletHistoryActivity.EXTRA_ACCOUNT_ID, account.getAccountId());
            startActivity(intent);
        });
        rvWalletAccounts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvWalletAccounts.setAdapter(accountAdapter);

        transactionAdapter = new WalletTransactionAdapter(this);
        transactionAdapter.setOnTransactionClickListener(transaction -> {
            WalletTransactionDetailsBottomSheet sheet = WalletTransactionDetailsBottomSheet.newInstance(transaction);
            sheet.show(getSupportFragmentManager(), "TransactionDetails");
        });
        rvRecentWalletTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvRecentWalletTransactions.setAdapter(transactionAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadWalletDashboardData);

        btnQuickCashIn.setOnClickListener(v -> {
            AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_RECEIVE);
            dialog.setOnTransactionSavedListener(this::loadWalletDashboardData);
            dialog.show(getSupportFragmentManager(), "CashIn");
        });

        btnQuickCashOut.setOnClickListener(v -> {
            AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_EXPENSE);
            dialog.setOnTransactionSavedListener(this::loadWalletDashboardData);
            dialog.show(getSupportFragmentManager(), "CashOut");
        });

        btnQuickTransfer.setOnClickListener(v -> {
            WalletTransferDialog dialog = new WalletTransferDialog();
            dialog.setOnTransferSavedListener(this::loadWalletDashboardData);
            dialog.show(getSupportFragmentManager(), "Transfer");
        });

        btnQuickHistory.setOnClickListener(v -> startActivity(new Intent(this, WalletHistoryActivity.class)));
        tvViewAllHistory.setOnClickListener(v -> startActivity(new Intent(this, WalletHistoryActivity.class)));
        btnManageAccounts.setOnClickListener(v -> startActivity(new Intent(this, WalletAccountsActivity.class)));
    }

    private void loadWalletDashboardData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        firestoreManager.ensureDefaultWalletAccounts(userId, new FirestoreManager.FirestoreListCallback<WalletAccount>() {
            @Override
            public void onSuccess(List<WalletAccount> accounts) {
                double totalMoney = 0;
                for (WalletAccount acc : accounts) {
                    if (acc.isActive()) totalMoney += acc.getCurrentBalance();
                }
                tvTotalBusinessMoney.setText(UnitConverterHelper.formatCurrency(totalMoney));
                accountAdapter.setAccounts(accounts);

                loadTransactionsData(userId);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(WalletDashboardActivity.this, "ওয়ালেট লোড করতে ব্যর্থ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTransactionsData(String userId) {
        firestoreManager.getWalletTransactions(userId, new FirestoreManager.FirestoreListCallback<WalletTransaction>() {
            @Override
            public void onSuccess(List<WalletTransaction> transactions) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                long todayStart = getTodayStartTimestamp();
                double todayIn = 0;
                double todayOut = 0;

                for (WalletTransaction t : transactions) {
                    if (!WalletTransaction.STATUS_REVERSED.equals(t.getStatus())) {
                        long date = t.getTransactionDate() > 0 ? t.getTransactionDate() : t.getCreatedAt();
                        if (date >= todayStart) {
                            if (WalletTransaction.DIRECTION_IN.equals(t.getDirection())) {
                                todayIn += t.getAmount();
                            } else if (WalletTransaction.DIRECTION_OUT.equals(t.getDirection())) {
                                todayOut += t.getAmount();
                            }
                        }
                    }
                }

                double todayNet = todayIn - todayOut;

                tvTodayIn.setText("+" + UnitConverterHelper.formatCurrency(todayIn));
                tvTodayOut.setText("-" + UnitConverterHelper.formatCurrency(todayOut));

                if (todayNet >= 0) {
                    tvTodayNet.setText("+" + UnitConverterHelper.formatCurrency(todayNet));
                    tvTodayNet.setTextColor(getResources().getColor(R.color.brand_green));
                } else {
                    tvTodayNet.setText("-" + UnitConverterHelper.formatCurrency(Math.abs(todayNet)));
                    tvTodayNet.setTextColor(getResources().getColor(R.color.error));
                }

                int limit = Math.min(5, transactions.size());
                transactionAdapter.setTransactions(transactions.subList(0, limit));
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }
        });
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
