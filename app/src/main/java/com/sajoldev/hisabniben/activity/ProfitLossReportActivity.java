package com.sajoldev.hisabniben.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.ReportCalculationManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class ProfitLossReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvNetProfitAmount, tvNetProfitMargin, tvPlRevenue, tvPlCogs, tvPlGrossProfit, tvPlExpenses, tvPlNetProfit;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_profit_loss_report);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupListeners();
        loadProfitLossData();
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
        tvNetProfitAmount = findViewById(R.id.tvNetProfitAmount);
        tvNetProfitMargin = findViewById(R.id.tvNetProfitMargin);
        tvPlRevenue = findViewById(R.id.tvPlRevenue);
        tvPlCogs = findViewById(R.id.tvPlCogs);
        tvPlGrossProfit = findViewById(R.id.tvPlGrossProfit);
        tvPlExpenses = findViewById(R.id.tvPlExpenses);
        tvPlNetProfit = findViewById(R.id.tvPlNetProfit);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("লাভ-ক্ষতি রিপোর্ট (Profit & Loss)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadProfitLossData);
    }

    private void loadProfitLossData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("sales")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(salesSnap -> {
                List<Sale> sales = new ArrayList<>();
                for (DocumentSnapshot doc : salesSnap.getDocuments()) {
                    Sale sale = doc.toObject(Sale.class);
                    if (sale != null) sales.add(sale);
                }

                FirestoreManager.getInstance().getExpensesByUser(userId, new FirestoreManager.FirestoreListCallback<Expense>() {
                    @Override
                    public void onSuccess(List<Expense> expenses) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        ReportCalculationManager.ProfitLossSummary pl = ReportCalculationManager.calculateProfitLossSummary(sales, expenses, 0, 0);

                        tvNetProfitAmount.setText(UnitConverterHelper.formatCurrency(pl.netProfit));
                        tvNetProfitMargin.setText("নিট প্রফিট মার্জিন: " + String.format("%.1f", pl.netProfitMargin) + "%");
                        tvPlRevenue.setText(UnitConverterHelper.formatCurrency(pl.salesRevenue));
                        tvPlCogs.setText(UnitConverterHelper.formatCurrency(pl.cogs));
                        tvPlGrossProfit.setText(UnitConverterHelper.formatCurrency(pl.grossProfit));
                        tvPlExpenses.setText(UnitConverterHelper.formatCurrency(pl.expenses));
                        tvPlNetProfit.setText(UnitConverterHelper.formatCurrency(pl.netProfit));
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ProfitLossReportActivity.this, "লাভ-ক্ষতি লোড করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
