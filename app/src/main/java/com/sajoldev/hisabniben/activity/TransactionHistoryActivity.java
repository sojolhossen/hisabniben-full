package com.sajoldev.hisabniben.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.UnifiedHistoryAdapter;
import com.sajoldev.hisabniben.dialog.AddTransactionDialog;
import com.sajoldev.hisabniben.dialog.UnifiedDetailBottomSheet;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.PurchaseItem;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.SaleItem;
import com.sajoldev.hisabniben.model.StockMovement;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.model.UnifiedHistoryItem;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TransactionHistoryActivity extends AppCompatActivity implements UnifiedHistoryAdapter.OnHistoryItemClickListener {

    public static final String EXTRA_FILTER_TYPE = "filter_type";

    private View btnBack, btnRefresh;
    private TextView tvDateSubtitle;
    private TextView tvKpiSales, tvKpiPurchases, tvKpiReceive, tvKpiExpense;
    private TextInputEditText etSearchHistory;
    private ChipGroup cgCategoryFilter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private ExtendedFloatingActionButton fabAddTransaction;

    private SessionManager sessionManager;
    private FirebaseFirestore db;

    private List<UnifiedHistoryItem> allHistoryItems = new ArrayList<>();
    private List<UnifiedHistoryItem> filteredHistoryItems = new ArrayList<>();
    private UnifiedHistoryAdapter adapter;
    private String currentCategoryFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        updateDateSubtitle();

        if (getIntent() != null && getIntent().hasExtra(EXTRA_FILTER_TYPE)) {
            String initFilter = getIntent().getStringExtra(EXTRA_FILTER_TYPE);
            applyInitialCategoryFilter(initFilter);
        }

        loadAllHistoryData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        tvDateSubtitle = findViewById(R.id.tvDateSubtitle);

        tvKpiSales = findViewById(R.id.tvKpiSales);
        tvKpiPurchases = findViewById(R.id.tvKpiPurchases);
        tvKpiReceive = findViewById(R.id.tvKpiReceive);
        tvKpiExpense = findViewById(R.id.tvKpiExpense);

        etSearchHistory = findViewById(R.id.etSearchHistory);
        cgCategoryFilter = findViewById(R.id.cgCategoryFilter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvHistory = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBar);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabAddTransaction = findViewById(R.id.fabAddTransaction);
    }

    private void setupRecyclerView() {
        adapter = new UnifiedHistoryAdapter(filteredHistoryItems, this);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        swipeRefresh.setOnRefreshListener(this::loadAllHistoryData);
        btnRefresh.setOnClickListener(v -> loadAllHistoryData());

        cgCategoryFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterSales) currentCategoryFilter = UnifiedHistoryItem.TYPE_SALE;
            else if (checkedId == R.id.chipFilterPurchases) currentCategoryFilter = UnifiedHistoryItem.TYPE_PURCHASE;
            else if (checkedId == R.id.chipFilterReceive) currentCategoryFilter = UnifiedHistoryItem.TYPE_MONEY_RECEIVE;
            else if (checkedId == R.id.chipFilterExpenses) currentCategoryFilter = UnifiedHistoryItem.TYPE_EXPENSE;
            else if (checkedId == R.id.chipFilterStock) currentCategoryFilter = UnifiedHistoryItem.TYPE_STOCK_MOVEMENT;
            else currentCategoryFilter = "ALL";

            applyFilters();
        });

        etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        fabAddTransaction.setOnClickListener(v -> {
            AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_RECEIVE);
            dialog.setOnTransactionSavedListener(this::loadAllHistoryData);
            dialog.show(getSupportFragmentManager(), "AddTransactionFromHistory");
        });
    }

    private void applyInitialCategoryFilter(String filter) {
        if ("SALE".equalsIgnoreCase(filter)) cgCategoryFilter.check(R.id.chipFilterSales);
        else if ("PURCHASE".equalsIgnoreCase(filter)) cgCategoryFilter.check(R.id.chipFilterPurchases);
        else if ("EXPENSE".equalsIgnoreCase(filter)) cgCategoryFilter.check(R.id.chipFilterExpenses);
        else if ("RECEIVE".equalsIgnoreCase(filter)) cgCategoryFilter.check(R.id.chipFilterReceive);
        else if ("STOCK".equalsIgnoreCase(filter)) cgCategoryFilter.check(R.id.chipFilterStock);
    }

    private void updateDateSubtitle() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        tvDateSubtitle.setText("আজ · " + sdf.format(new Date()));
    }

    private void loadAllHistoryData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        allHistoryItems.clear();

        long todayStart = getTodayStartTimestamp();

        // 1. Fetch Sales
        db.collection("sales")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(salesSnap -> {
                double totalSalesToday = 0;
                for (QueryDocumentSnapshot doc : salesSnap) {
                    Sale sale = doc.toObject(Sale.class);
                    if (sale != null && !Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                        long sDate = sale.getSaleDate() > 0 ? sale.getSaleDate() : sale.getCreatedAt();
                        if (sDate >= todayStart) totalSalesToday += sale.getGrandTotal();

                        StringBuilder itemsDesc = new StringBuilder();
                        if (sale.getItems() != null) {
                            for (SaleItem item : sale.getItems()) {
                                if (itemsDesc.length() > 0) itemsDesc.append(", ");
                                itemsDesc.append(item.getProductNameSnapshot())
                                         .append(" · ")
                                         .append((int) item.getBagQuantity()).append(" বস্তা (")
                                         .append(UnitConverterHelper.formatKg(item.getTotalKg())).append(")");
                            }
                        }

                        allHistoryItems.add(new UnifiedHistoryItem(
                            sale.getId(),
                            UnifiedHistoryItem.TYPE_SALE,
                            sale.getCustomerName() != null ? sale.getCustomerName() : "ক্যাশ কাস্টমার",
                            itemsDesc.toString(),
                            sale.getGrandTotal(),
                            sale.getDueAmount(),
                            sale.getPaidAmount(),
                            sDate,
                            sale.getPaymentMethod(),
                            sale.getPaymentStatus(),
                            sale
                        ));
                    }
                }
                final double kpiSales = totalSalesToday;
                tvKpiSales.setText(UnitConverterHelper.formatCurrency(kpiSales));

                // 2. Fetch Purchases
                db.collection("purchases")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(purchasesSnap -> {
                        double totalPurchasesToday = 0;
                        for (QueryDocumentSnapshot doc : purchasesSnap) {
                            Purchase p = doc.toObject(Purchase.class);
                            if (p != null && !Purchase.PURCHASE_STATUS_CANCELLED.equals(p.getPurchaseStatus())) {
                                long pDate = p.getPurchaseDate() > 0 ? p.getPurchaseDate() : p.getCreatedAt();
                                if (pDate >= todayStart) totalPurchasesToday += p.getGrandTotal();

                                StringBuilder itemsDesc = new StringBuilder();
                                if (p.getItems() != null) {
                                    for (PurchaseItem item : p.getItems()) {
                                        if (itemsDesc.length() > 0) itemsDesc.append(", ");
                                        itemsDesc.append(item.getProductName())
                                                 .append(" · ")
                                                 .append((int) item.getBagQuantity()).append(" বস্তা (")
                                                 .append(UnitConverterHelper.formatKg(item.getTotalKg())).append(")");
                                    }
                                }

                                allHistoryItems.add(new UnifiedHistoryItem(
                                    p.getId(),
                                    UnifiedHistoryItem.TYPE_PURCHASE,
                                    p.getSupplierName() != null ? p.getSupplierName() : "সাপ্লায়ার",
                                    itemsDesc.toString(),
                                    p.getGrandTotal(),
                                    p.getDueAmount(),
                                    p.getPaidAmount(),
                                    pDate,
                                    "Cash",
                                    p.getPaymentStatus(),
                                    p
                                ));
                            }
                        }
                        final double kpiPurchases = totalPurchasesToday;
                        tvKpiPurchases.setText(UnitConverterHelper.formatCurrency(kpiPurchases));

                        // 3. Fetch Money Transactions (Receive)
                        db.collection("transactions")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(txSnap -> {
                                double totalReceiveToday = 0;
                                for (QueryDocumentSnapshot doc : txSnap) {
                                    Transaction tx = doc.toObject(Transaction.class);
                                    if (tx != null) {
                                        long tDate = tx.getDate() > 0 ? tx.getDate() : tx.getCreatedAt();
                                        if (tDate >= todayStart && (Transaction.TYPE_PAYMENT.equals(tx.getType()) || Transaction.TYPE_CUSTOMER_PAYMENT.equals(tx.getType()))) {
                                            totalReceiveToday += tx.getAmount();
                                        }

                                        String title = tx.getCustomerName() != null ? tx.getCustomerName() : (tx.getSupplierName() != null ? tx.getSupplierName() : "লেনদেন");
                                        String subtitle = tx.getNote() != null && !tx.getNote().isEmpty() ? tx.getNote() : "টাকা জমা (" + tx.getPaymentMethod() + ")";

                                        allHistoryItems.add(new UnifiedHistoryItem(
                                            tx.getId(),
                                            UnifiedHistoryItem.TYPE_MONEY_RECEIVE,
                                            title,
                                            subtitle,
                                            tx.getAmount(),
                                            tx.getNewBaki(),
                                            tx.getAmount(),
                                            tDate,
                                            tx.getPaymentMethod(),
                                            "PAID",
                                            tx
                                        ));
                                    }
                                }
                                final double kpiReceive = totalReceiveToday;
                                tvKpiReceive.setText(UnitConverterHelper.formatCurrency(kpiReceive));

                                // 4. Fetch Expenses
                                db.collection("expenses")
                                    .whereEqualTo("userId", userId)
                                    .get()
                                    .addOnSuccessListener(expSnap -> {
                                        double totalExpenseToday = 0;
                                        for (QueryDocumentSnapshot doc : expSnap) {
                                            Expense exp = doc.toObject(Expense.class);
                                            if (exp != null) {
                                                long eDate = exp.getDate() > 0 ? exp.getDate() : exp.getCreatedAt();
                                                if (eDate >= todayStart) totalExpenseToday += exp.getAmount();

                                                allHistoryItems.add(new UnifiedHistoryItem(
                                                    exp.getId(),
                                                    UnifiedHistoryItem.TYPE_EXPENSE,
                                                    exp.getCategory() != null ? exp.getCategory() : "ব্যবসার খরচ",
                                                    exp.getDescription() != null && !exp.getDescription().isEmpty() ? exp.getDescription() : "পেমেন্ট: " + exp.getPaymentMethod(),
                                                    exp.getAmount(),
                                                    0,
                                                    exp.getAmount(),
                                                    eDate,
                                                    exp.getPaymentMethod(),
                                                    "PAID",
                                                    exp
                                                ));
                                            }
                                        }
                                        final double kpiExpense = totalExpenseToday;
                                        tvKpiExpense.setText(UnitConverterHelper.formatCurrency(kpiExpense));

                                        // 5. Fetch Stock Movements
                                        db.collection("stockMovements")
                                            .whereEqualTo("userId", userId)
                                            .get()
                                            .addOnSuccessListener(smSnap -> {
                                                for (QueryDocumentSnapshot doc : smSnap) {
                                                    StockMovement sm = doc.toObject(StockMovement.class);
                                                    if (sm != null) {
                                                        long smDate = sm.getDate() > 0 ? sm.getDate() : sm.getCreatedAt();
                                                        String sign = (StockMovement.TYPE_PURCHASE.equals(sm.getType()) || StockMovement.TYPE_RETURN_CUSTOMER.equals(sm.getType())) ? "+ " : "- ";
                                                        allHistoryItems.add(new UnifiedHistoryItem(
                                                            sm.getId(),
                                                            UnifiedHistoryItem.TYPE_STOCK_MOVEMENT,
                                                            sm.getProductName() != null ? sm.getProductName() : "চালের স্টক",
                                                            sign + UnitConverterHelper.formatKg(sm.getQuantityKg()) + " (" + (sm.getReason() != null ? sm.getReason() : sm.getType()) + ")",
                                                            sm.getQuantityKg(),
                                                            0,
                                                            0,
                                                            smDate,
                                                            "Stock",
                                                            sm.getType(),
                                                            sm
                                                        ));
                                                    }
                                                }

                                                // Sort Chronologically Newest First
                                                allHistoryItems.sort((h1, h2) -> Long.compare(h2.getDate(), h1.getDate()));

                                                progressBar.setVisibility(View.GONE);
                                                swipeRefresh.setRefreshing(false);
                                                applyFilters();
                                            });
                                    });
                            });
                    });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            });
    }

    private void applyFilters() {
        String query = etSearchHistory.getText() != null ? etSearchHistory.getText().toString().trim().toLowerCase() : "";

        filteredHistoryItems.clear();

        for (UnifiedHistoryItem item : allHistoryItems) {
            boolean matchesCategory = "ALL".equals(currentCategoryFilter) || currentCategoryFilter.equals(item.getType());
            boolean matchesSearch = query.isEmpty() ||
                (item.getTitle() != null && item.getTitle().toLowerCase().contains(query)) ||
                (item.getSubtitle() != null && item.getSubtitle().toLowerCase().contains(query));

            if (matchesCategory && matchesSearch) {
                filteredHistoryItems.add(item);
            }
        }

        adapter.updateData(filteredHistoryItems);
        layoutEmptyState.setVisibility(filteredHistoryItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private long getTodayStartTimestamp() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    public void onItemClick(UnifiedHistoryItem item) {
        UnifiedDetailBottomSheet bottomSheet = UnifiedDetailBottomSheet.newInstance(item);
        bottomSheet.show(getSupportFragmentManager(), "UnifiedDetail");
    }
}
