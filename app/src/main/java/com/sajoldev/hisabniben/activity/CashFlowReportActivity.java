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
import com.sajoldev.hisabniben.adapter.WalletTransactionAdapter;
import com.sajoldev.hisabniben.model.WalletTransaction;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class CashFlowReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvNetCashFlow, tvMoneyIn, tvMoneyOut;
    private MaterialButton btnOpenWalletHistory;
    private RecyclerView rvCashFlowReport;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private WalletTransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_cash_flow_report);

        sessionManager = SessionManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadCashFlowData();
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
        tvNetCashFlow = findViewById(R.id.tvNetCashFlow);
        tvMoneyIn = findViewById(R.id.tvMoneyIn);
        tvMoneyOut = findViewById(R.id.tvMoneyOut);
        btnOpenWalletHistory = findViewById(R.id.btnOpenWalletHistory);
        rvCashFlowReport = findViewById(R.id.rvCashFlowReport);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ক্যাশ ফ্লো রিপোর্ট (Cash Flow)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new WalletTransactionAdapter(this);
        rvCashFlowReport.setLayoutManager(new LinearLayoutManager(this));
        rvCashFlowReport.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCashFlowData);
        btnOpenWalletHistory.setOnClickListener(v -> startActivity(new Intent(this, WalletHistoryActivity.class)));
    }

    private void loadCashFlowData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        firestoreManager.getWalletTransactions(userId, new FirestoreManager.FirestoreListCallback<WalletTransaction>() {
            @Override
            public void onSuccess(List<WalletTransaction> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                List<WalletTransaction> list = result != null ? result : new ArrayList<>();
                double moneyIn = 0;
                double moneyOut = 0;

                for (WalletTransaction wt : list) {
                    if (!WalletTransaction.STATUS_REVERSED.equals(wt.getStatus())) {
                        if (WalletTransaction.DIRECTION_IN.equals(wt.getDirection())) {
                            moneyIn += wt.getAmount();
                        } else if (WalletTransaction.DIRECTION_OUT.equals(wt.getDirection())) {
                            moneyOut += wt.getAmount();
                        }
                    }
                }

                double netFlow = moneyIn - moneyOut;

                tvMoneyIn.setText("+" + UnitConverterHelper.formatCurrency(moneyIn));
                tvMoneyOut.setText("-" + UnitConverterHelper.formatCurrency(moneyOut));

                if (netFlow >= 0) {
                    tvNetCashFlow.setText("+" + UnitConverterHelper.formatCurrency(netFlow));
                } else {
                    tvNetCashFlow.setText("-" + UnitConverterHelper.formatCurrency(Math.abs(netFlow)));
                }

                int limit = Math.min(10, list.size());
                adapter.setTransactions(list.subList(0, limit));
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CashFlowReportActivity.this, "ক্যাশ ফ্লো লোড করতে ব্যর্থ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
