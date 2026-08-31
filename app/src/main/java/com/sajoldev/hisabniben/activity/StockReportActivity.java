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
import com.sajoldev.hisabniben.adapter.RiceProductAdapter;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.util.ReportCalculationManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class StockReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalStockValuation, tvTotalStockBags, tvTotalStockKg, tvProductCount, tvEmptyStock;
    private TextInputEditText etSearchProduct;
    private RecyclerView rvStockReport;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private RiceProductAdapter adapter;

    private List<RiceProduct> allProducts = new ArrayList<>();
    private List<RiceProduct> filteredProducts = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_stock_report);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadStockReportData();
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
        tvTotalStockValuation = findViewById(R.id.tvTotalStockValuation);
        tvTotalStockBags = findViewById(R.id.tvTotalStockBags);
        tvTotalStockKg = findViewById(R.id.tvTotalStockKg);
        tvProductCount = findViewById(R.id.tvProductCount);
        tvEmptyStock = findViewById(R.id.tvEmptyStock);
        etSearchProduct = findViewById(R.id.etSearchProduct);
        rvStockReport = findViewById(R.id.rvStockReport);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("স্টক ও মূল্য রিপোর্ট (Stock & Valuation)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new RiceProductAdapter(this, new ArrayList<>(), null);
        rvStockReport.setLayoutManager(new LinearLayoutManager(this));
        rvStockReport.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadStockReportData);

        etSearchProduct.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applySearchFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadStockReportData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("riceProducts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(snap -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                allProducts.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    RiceProduct p = doc.toObject(RiceProduct.class);
                    if (p != null) {
                        if (p.getId() == null) p.setId(doc.getId());
                        allProducts.add(p);
                    }
                }
                allProducts.sort((p1, p2) -> Double.compare(p2.getCurrentStockKg(), p1.getCurrentStockKg()));

                ReportCalculationManager.StockSummary summary = ReportCalculationManager.calculateStockSummary(allProducts);

                tvTotalStockValuation.setText(UnitConverterHelper.formatCurrency(summary.totalValuation));
                tvTotalStockBags.setText((int)summary.totalBags + " বস্তা");
                tvTotalStockKg.setText(UnitConverterHelper.formatKg(summary.totalKg));
                tvProductCount.setText(summary.productCount + " টি");

                applySearchFilter();
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(StockReportActivity.this, "স্টক লোড করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void applySearchFilter() {
        filteredProducts.clear();
        for (RiceProduct p : allProducts) {
            if (currentQuery.isEmpty()) {
                filteredProducts.add(p);
            } else {
                boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(currentQuery);
                boolean matchBrand = p.getBrand() != null && p.getBrand().toLowerCase().contains(currentQuery);

                if (matchName || matchBrand) {
                    filteredProducts.add(p);
                }
            }
        }

        adapter.updateData(filteredProducts);

        if (filteredProducts.isEmpty()) {
            tvEmptyStock.setVisibility(View.VISIBLE);
            rvStockReport.setVisibility(View.GONE);
        } else {
            tvEmptyStock.setVisibility(View.GONE);
            rvStockReport.setVisibility(View.VISIBLE);
        }
    }
}
