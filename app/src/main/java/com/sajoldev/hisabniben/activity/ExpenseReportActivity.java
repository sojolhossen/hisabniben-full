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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.UnifiedHistoryAdapter;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.UnifiedHistoryItem;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class ExpenseReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalExpense, tvExpenseCount, tvEmptyExpense;
    private RecyclerView rvExpenseReport;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private UnifiedHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_expense_report);

        sessionManager = SessionManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadExpenseReportData();
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
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvExpenseCount = findViewById(R.id.tvExpenseCount);
        tvEmptyExpense = findViewById(R.id.tvEmptyExpense);
        rvExpenseReport = findViewById(R.id.rvExpenseReport);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ব্যবসার খরচ রিপোর্ট (Expense Report)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new UnifiedHistoryAdapter(new ArrayList<>(), null);
        rvExpenseReport.setLayoutManager(new LinearLayoutManager(this));
        rvExpenseReport.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadExpenseReportData);
    }

    private void loadExpenseReportData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        firestoreManager.getExpensesByUser(userId, new FirestoreManager.FirestoreListCallback<Expense>() {
            @Override
            public void onSuccess(List<Expense> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                List<Expense> list = result != null ? result : new ArrayList<>();
                double total = 0;
                List<UnifiedHistoryItem> items = new ArrayList<>();

                for (Expense e : list) {
                    total += e.getAmount();

                    String title = e.getCategory() != null ? e.getCategory() : "ব্যবসার খরচ";
                    String sub = e.getDescription() != null && !e.getDescription().isEmpty() ? e.getDescription() : "নোট নেই";
                    long date = e.getDate() > 0 ? e.getDate() : e.getCreatedAt();

                    UnifiedHistoryItem item = new UnifiedHistoryItem(
                        e.getId(),
                        UnifiedHistoryItem.TYPE_EXPENSE,
                        title,
                        sub,
                        e.getAmount(),
                        0,
                        e.getAmount(),
                        date,
                        e.getPaymentMethod() != null ? e.getPaymentMethod() : "Cash",
                        "PAID",
                        e
                    );
                    items.add(item);
                }

                tvTotalExpense.setText(UnitConverterHelper.formatCurrency(total));
                tvExpenseCount.setText(list.size() + " টি খরচ রেকর্ড করা হয়েছে");

                adapter.updateData(items);

                if (items.isEmpty()) {
                    tvEmptyExpense.setVisibility(View.VISIBLE);
                    rvExpenseReport.setVisibility(View.GONE);
                } else {
                    tvEmptyExpense.setVisibility(View.GONE);
                    rvExpenseReport.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ExpenseReportActivity.this, "খরচ লোড করতে ব্যর্থ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
