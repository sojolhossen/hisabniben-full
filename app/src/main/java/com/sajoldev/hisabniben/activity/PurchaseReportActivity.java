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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.UnifiedHistoryAdapter;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.UnifiedHistoryItem;
import com.sajoldev.hisabniben.util.ReportCalculationManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class PurchaseReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalPurchase, tvPurchasePaid, tvPurchasePayable, tvPurchaseCount, tvEmptyPurchase;
    private TextInputEditText etSearchPurchase;
    private RecyclerView rvPurchaseReport;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private UnifiedHistoryAdapter adapter;

    private List<Purchase> allPurchases = new ArrayList<>();
    private List<UnifiedHistoryItem> filteredItems = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_purchase_report);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadPurchaseReportData();
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
        tvTotalPurchase = findViewById(R.id.tvTotalPurchase);
        tvPurchasePaid = findViewById(R.id.tvPurchasePaid);
        tvPurchasePayable = findViewById(R.id.tvPurchasePayable);
        tvPurchaseCount = findViewById(R.id.tvPurchaseCount);
        tvEmptyPurchase = findViewById(R.id.tvEmptyPurchase);
        etSearchPurchase = findViewById(R.id.etSearchPurchase);
        rvPurchaseReport = findViewById(R.id.rvPurchaseReport);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("চাল ক্রয় রিপোর্ট (Purchase Report)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new UnifiedHistoryAdapter(new ArrayList<>(), null);
        rvPurchaseReport.setLayoutManager(new LinearLayoutManager(this));
        rvPurchaseReport.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadPurchaseReportData);

        etSearchPurchase.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applySearchFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadPurchaseReportData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("purchases")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(snap -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                allPurchases.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Purchase purchase = doc.toObject(Purchase.class);
                    if (purchase != null) {
                        if (purchase.getId() == null) purchase.setId(doc.getId());
                        allPurchases.add(purchase);
                    }
                }
                allPurchases.sort((p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));

                // Calculate summary
                ReportCalculationManager.PurchaseSummary summary = ReportCalculationManager.calculatePurchaseSummary(allPurchases, 0, 0);

                tvTotalPurchase.setText(UnitConverterHelper.formatCurrency(summary.totalPurchase));
                tvPurchasePaid.setText(UnitConverterHelper.formatCurrency(summary.totalPaid));
                tvPurchasePayable.setText(UnitConverterHelper.formatCurrency(summary.totalPayable));
                tvPurchaseCount.setText(summary.purchaseCount + " টি");

                applySearchFilter();
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PurchaseReportActivity.this, "ক্রয় লোড করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void applySearchFilter() {
        filteredItems.clear();
        for (Purchase p : allPurchases) {
            boolean matchInvoice = p.getInvoiceNo() != null && p.getInvoiceNo().toLowerCase().contains(currentQuery);
            boolean matchSupplier = p.getSupplierName() != null && p.getSupplierName().toLowerCase().contains(currentQuery);

            if (currentQuery.isEmpty() || matchInvoice || matchSupplier) {
                String title = p.getSupplierName() != null ? p.getSupplierName() : "চাল মহাজন";
                String sub = "চালান #" + (p.getInvoiceNo() != null ? p.getInvoiceNo() : p.getId());
                long date = p.getPurchaseDate() > 0 ? p.getPurchaseDate() : p.getCreatedAt();

                UnifiedHistoryItem item = new UnifiedHistoryItem(
                    p.getId(),
                    UnifiedHistoryItem.TYPE_PURCHASE,
                    title,
                    sub,
                    p.getGrandTotal(),
                    p.getDueAmount(),
                    p.getPaidAmount(),
                    date,
                    p.getPaymentMethod(),
                    p.getPurchaseStatus(),
                    p
                );
                filteredItems.add(item);
            }
        }

        adapter.updateData(filteredItems);

        if (filteredItems.isEmpty()) {
            tvEmptyPurchase.setVisibility(View.VISIBLE);
            rvPurchaseReport.setVisibility(View.GONE);
        } else {
            tvEmptyPurchase.setVisibility(View.GONE);
            rvPurchaseReport.setVisibility(View.VISIBLE);
        }
    }
}
