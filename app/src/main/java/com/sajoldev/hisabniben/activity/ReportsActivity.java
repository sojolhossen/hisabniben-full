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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.model.WalletTransaction;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.ReportCalculationManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.SubscriptionGuard;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class ReportsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroupReportPeriod;
    private TextView tvSummaryPeriodTitle, tvSumSales, tvSumPurchase, tvSumExpenses, tvSumNetProfit;
    private TextView tvReportCustomerDueVal, tvReportSupplierPayableVal, tvReportStockValuationVal, tvReportCashFlowVal;
    private MaterialCardView btnSalesReport, btnPurchaseReport, btnCustomerDueReport, btnSupplierPayableReport;
    private MaterialCardView btnStockReport, btnCashFlowReport, btnProfitLossReport, btnExpenseReport;
    private MaterialButton btnGeneratePdfReport, btnShareSummary;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private FirestoreManager firestoreManager;

    private long selectedStartDate = 0;
    private long selectedEndDate = 0;
    private String currentPeriodName = "এই মাসের সারাংশ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_reports);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();
        firestoreManager = FirestoreManager.getInstance();

        initViews();
        setupWindowInsets();
        setupPeriodSelector();
        setupClickListeners();
        setPeriodMonth();
        loadReportsCenterData();
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
        chipGroupReportPeriod = findViewById(R.id.chipGroupReportPeriod);

        tvSummaryPeriodTitle = findViewById(R.id.tvSummaryPeriodTitle);
        tvSumSales = findViewById(R.id.tvSumSales);
        tvSumPurchase = findViewById(R.id.tvSumPurchase);
        tvSumExpenses = findViewById(R.id.tvSumExpenses);
        tvSumNetProfit = findViewById(R.id.tvSumNetProfit);

        tvReportCustomerDueVal = findViewById(R.id.tvReportCustomerDueVal);
        tvReportSupplierPayableVal = findViewById(R.id.tvReportSupplierPayableVal);
        tvReportStockValuationVal = findViewById(R.id.tvReportStockValuationVal);
        tvReportCashFlowVal = findViewById(R.id.tvReportCashFlowVal);

        btnSalesReport = findViewById(R.id.btnSalesReport);
        btnPurchaseReport = findViewById(R.id.btnPurchaseReport);
        btnCustomerDueReport = findViewById(R.id.btnCustomerDueReport);
        btnSupplierPayableReport = findViewById(R.id.btnSupplierPayableReport);
        btnStockReport = findViewById(R.id.btnStockReport);
        btnCashFlowReport = findViewById(R.id.btnCashFlowReport);
        btnProfitLossReport = findViewById(R.id.btnProfitLossReport);
        btnExpenseReport = findViewById(R.id.btnExpenseReport);

        btnGeneratePdfReport = findViewById(R.id.btnGeneratePdfReport);
        btnShareSummary = findViewById(R.id.btnShareSummary);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ব্যবসার রিপোর্ট (Reports Center)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupPeriodSelector() {
        chipGroupReportPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRepToday) {
                setPeriodToday();
            } else if (checkedId == R.id.chipRep7Days) {
                setPeriod7Days();
            } else if (checkedId == R.id.chipRepMonth) {
                setPeriodMonth();
            } else if (checkedId == R.id.chipRepLastMonth) {
                setPeriodLastMonth();
            } else if (checkedId == R.id.chipRepYear) {
                setPeriodYear();
            }
            loadReportsCenterData();
        });
    }

    private void setPeriodToday() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedStartDate = cal.getTimeInMillis();
        selectedEndDate = System.currentTimeMillis();
        currentPeriodName = "আজকের সারাংশ";
        tvSummaryPeriodTitle.setText(currentPeriodName);
    }

    private void setPeriod7Days() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.add(Calendar.DAY_OF_YEAR, -6);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedStartDate = cal.getTimeInMillis();
        selectedEndDate = System.currentTimeMillis();
        currentPeriodName = "গত ৭ দিনের সারাংশ";
        tvSummaryPeriodTitle.setText(currentPeriodName);
    }

    private void setPeriodMonth() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedStartDate = cal.getTimeInMillis();
        selectedEndDate = System.currentTimeMillis();
        currentPeriodName = "এই মাসের সারাংশ";
        tvSummaryPeriodTitle.setText(currentPeriodName);
    }

    private void setPeriodLastMonth() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedStartDate = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        selectedEndDate = cal.getTimeInMillis();

        currentPeriodName = "গত মাসের সারাংশ";
        tvSummaryPeriodTitle.setText(currentPeriodName);
    }

    private void setPeriodYear() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedStartDate = cal.getTimeInMillis();
        selectedEndDate = System.currentTimeMillis();
        currentPeriodName = "এই বছরের সারাংশ";
        tvSummaryPeriodTitle.setText(currentPeriodName);
    }

    private void setupClickListeners() {
        swipeRefresh.setOnRefreshListener(this::loadReportsCenterData);

        btnSalesReport.setOnClickListener(v -> openReportScreen(SalesReportActivity.class));
        btnPurchaseReport.setOnClickListener(v -> openReportScreen(PurchaseReportActivity.class));
        btnCustomerDueReport.setOnClickListener(v -> openReportScreen(CustomerDueReportActivity.class));
        btnSupplierPayableReport.setOnClickListener(v -> openReportScreen(SupplierPayableReportActivity.class));
        btnStockReport.setOnClickListener(v -> openReportScreen(StockReportActivity.class));
        btnCashFlowReport.setOnClickListener(v -> openReportScreen(CashFlowReportActivity.class));
        btnProfitLossReport.setOnClickListener(v -> openReportScreen(ProfitLossReportActivity.class));
        btnExpenseReport.setOnClickListener(v -> openReportScreen(ExpenseReportActivity.class));

        btnGeneratePdfReport.setOnClickListener(v -> SubscriptionGuard.checkAccess(this, () -> {
            Intent intent = new Intent(this, ReportPreviewActivity.class);
            intent.putExtra(ReportPreviewActivity.EXTRA_REPORT_TYPE, ReportPreviewActivity.TYPE_SALES);
            startActivity(intent);
        }));

        btnShareSummary.setOnClickListener(v -> shareSummaryText());
    }

    private void openReportScreen(Class<?> cls) {
        SubscriptionGuard.checkAccess(this, () -> startActivity(new Intent(this, cls)));
    }

    private void loadReportsCenterData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        // Fetch Sales
        db.collection("sales").whereEqualTo("userId", userId).get().addOnSuccessListener(salesSnap -> {
            List<Sale> sales = new ArrayList<>();
            for (DocumentSnapshot doc : salesSnap.getDocuments()) {
                Sale s = doc.toObject(Sale.class);
                if (s != null) sales.add(s);
            }

            // Fetch Purchases
            db.collection("purchases").whereEqualTo("userId", userId).get().addOnSuccessListener(purSnap -> {
                List<Purchase> purchases = new ArrayList<>();
                for (DocumentSnapshot doc : purSnap.getDocuments()) {
                    Purchase p = doc.toObject(Purchase.class);
                    if (p != null) purchases.add(p);
                }

                // Fetch Customers
                db.collection("customers").whereEqualTo("userId", userId).get().addOnSuccessListener(custSnap -> {
                    List<Customer> customers = custSnap.toObjects(Customer.class);

                    // Fetch Suppliers
                    db.collection("suppliers").whereEqualTo("userId", userId).get().addOnSuccessListener(suppSnap -> {
                        List<Supplier> suppliers = suppSnap.toObjects(Supplier.class);

                        // Fetch Products
                        db.collection("riceProducts").whereEqualTo("userId", userId).get().addOnSuccessListener(prodSnap -> {
                            List<RiceProduct> products = prodSnap.toObjects(RiceProduct.class);

                            // Fetch Expenses
                            firestoreManager.getExpensesByUser(userId, new FirestoreManager.FirestoreListCallback<Expense>() {
                                @Override
                                public void onSuccess(List<Expense> expenses) {
                                    // Fetch Wallet Transactions
                                    firestoreManager.getWalletTransactions(userId, new FirestoreManager.FirestoreListCallback<WalletTransaction>() {
                                        @Override
                                        public void onSuccess(List<WalletTransaction> walletTransactions) {
                                            progressBar.setVisibility(View.GONE);
                                            swipeRefresh.setRefreshing(false);

                                            ReportCalculationManager.OverallSummary summary =
                                                ReportCalculationManager.calculateOverallSummary(
                                                    sales, purchases, expenses, customers, suppliers, products, walletTransactions, selectedStartDate, selectedEndDate
                                                );

                                            tvSumSales.setText(UnitConverterHelper.formatCurrency(summary.totalSales));
                                            tvSumPurchase.setText(UnitConverterHelper.formatCurrency(summary.totalPurchases));
                                            tvSumExpenses.setText(UnitConverterHelper.formatCurrency(summary.totalExpenses));
                                            tvSumNetProfit.setText(UnitConverterHelper.formatCurrency(summary.netProfit));

                                            tvReportCustomerDueVal.setText(UnitConverterHelper.formatCurrency(summary.totalCustomerDue));
                                            tvReportSupplierPayableVal.setText(UnitConverterHelper.formatCurrency(summary.totalSupplierPayable));

                                            ReportCalculationManager.StockSummary stockSum = ReportCalculationManager.calculateStockSummary(products);
                                            tvReportStockValuationVal.setText((int)stockSum.totalBags + " বস্তা | " + UnitConverterHelper.formatKg(stockSum.totalKg) + " (মূল্য: " + UnitConverterHelper.formatCurrency(stockSum.totalValuation) + ")");

                                            tvReportCashFlowVal.setText("জমা: " + UnitConverterHelper.formatCurrency(summary.totalMoneyIn) + " | খরচ: " + UnitConverterHelper.formatCurrency(summary.totalMoneyOut));
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            progressBar.setVisibility(View.GONE);
                                            swipeRefresh.setRefreshing(false);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    swipeRefresh.setRefreshing(false);
                                }
                            });
                        });
                    });
                });
            });
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            Toast.makeText(ReportsActivity.this, "রিপোর্ট লোড করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void shareSummaryText() {
        String storeName = sessionManager.getStoreName() != null ? sessionManager.getStoreName() : "HisabNiben Rice Store";
        StringBuilder sb = new StringBuilder();
        sb.append("📊 ").append(storeName).append(" - ").append(currentPeriodName).append("\n");
        sb.append("-----------------------------\n");
        sb.append("🛒 মোট চাল বিক্রি: ").append(tvSumSales.getText().toString()).append("\n");
        sb.append("🌾 মোট চাল ক্রয়: ").append(tvSumPurchase.getText().toString()).append("\n");
        sb.append("💸 মোট খরচ: ").append(tvSumExpenses.getText().toString()).append("\n");
        sb.append("📈 নিট লাভ: ").append(tvSumNetProfit.getText().toString()).append("\n");
        sb.append("-----------------------------\n");
        sb.append("👥 কাস্টমার বাকি: ").append(tvReportCustomerDueVal.getText().toString()).append("\n");
        sb.append("🏬 মহাজন পাওনা: ").append(tvReportSupplierPayableVal.getText().toString()).append("\n");
        sb.append("-----------------------------\n");
        sb.append("Generated by HisabNiben App");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, storeName + " Report Summary");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "রিপোর্ট সারাংশ শেয়ার করুন"));
    }
}
