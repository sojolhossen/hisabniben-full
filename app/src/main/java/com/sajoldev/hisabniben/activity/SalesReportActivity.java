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
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.UnifiedHistoryItem;
import com.sajoldev.hisabniben.util.ReportCalculationManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class SalesReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalSales, tvSalesPaid, tvSalesDue, tvSalesInvoices, tvEmptySales;
    private TextInputEditText etSearchSales;
    private RecyclerView rvSalesReport;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private UnifiedHistoryAdapter adapter;

    private List<Sale> allSales = new ArrayList<>();
    private List<UnifiedHistoryItem> filteredItems = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_sales_report);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadSalesReportData();
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
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvSalesPaid = findViewById(R.id.tvSalesPaid);
        tvSalesDue = findViewById(R.id.tvSalesDue);
        tvSalesInvoices = findViewById(R.id.tvSalesInvoices);
        tvEmptySales = findViewById(R.id.tvEmptySales);
        etSearchSales = findViewById(R.id.etSearchSales);
        rvSalesReport = findViewById(R.id.rvSalesReport);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("বিক্রি রিপোর্ট (Sales Report)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new UnifiedHistoryAdapter(new ArrayList<>(), null);
        rvSalesReport.setLayoutManager(new LinearLayoutManager(this));
        rvSalesReport.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadSalesReportData);

        etSearchSales.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applySearchFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSalesReportData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("sales")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(snap -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                allSales.clear();
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Sale sale = doc.toObject(Sale.class);
                    if (sale != null && !Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                        if (sale.getId() == null) sale.setId(doc.getId());
                        allSales.add(sale);
                    }
                }
                allSales.sort((s1, s2) -> Long.compare(s2.getCreatedAt(), s1.getCreatedAt()));

                // Calculate summary
                ReportCalculationManager.SalesSummary summary = ReportCalculationManager.calculateSalesSummary(allSales, 0, 0);

                tvTotalSales.setText(UnitConverterHelper.formatCurrency(summary.totalSales));
                tvSalesPaid.setText(UnitConverterHelper.formatCurrency(summary.totalPaid));
                tvSalesDue.setText(UnitConverterHelper.formatCurrency(summary.totalDue));
                tvSalesInvoices.setText(summary.invoiceCount + " টি");

                applySearchFilter();
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(SalesReportActivity.this, "সেলস লোড করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void applySearchFilter() {
        filteredItems.clear();
        for (Sale sale : allSales) {
            boolean matchMemo = sale.getInvoiceNo() != null && sale.getInvoiceNo().toLowerCase().contains(currentQuery);
            boolean matchCustomer = sale.getCustomerName() != null && sale.getCustomerName().toLowerCase().contains(currentQuery);
            boolean matchPhone = sale.getCustomerPhone() != null && sale.getCustomerPhone().contains(currentQuery);

            if (currentQuery.isEmpty() || matchMemo || matchCustomer || matchPhone) {
                String title = sale.getCustomerName() != null ? sale.getCustomerName() : "ক্যাশ কাস্টমার";
                String sub = "মেমো #" + (sale.getInvoiceNo() != null ? sale.getInvoiceNo() : sale.getId());
                long date = sale.getSaleDate() > 0 ? sale.getSaleDate() : sale.getCreatedAt();

                UnifiedHistoryItem item = new UnifiedHistoryItem(
                    sale.getId(),
                    UnifiedHistoryItem.TYPE_SALE,
                    title,
                    sub,
                    sale.getGrandTotal(),
                    sale.getDueAmount(),
                    sale.getPaidAmount(),
                    date,
                    sale.getPaymentMethod(),
                    sale.getSaleStatus(),
                    sale
                );
                filteredItems.add(item);
            }
        }

        adapter.updateData(filteredItems);

        if (filteredItems.isEmpty()) {
            tvEmptySales.setVisibility(View.VISIBLE);
            rvSalesReport.setVisibility(View.GONE);
        } else {
            tvEmptySales.setVisibility(View.GONE);
            rvSalesReport.setVisibility(View.VISIBLE);
        }
    }
}
