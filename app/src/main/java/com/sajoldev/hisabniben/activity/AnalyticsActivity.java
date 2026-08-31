package com.sajoldev.hisabniben.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.PurchaseItem;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.SaleItem;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AnalyticsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ProgressBar progressBar;
    private ChipGroup chipGroupPeriod;

    private TextView tvKpiSales, tvKpiPurchase, tvKpiProfit, tvKpiExpense;
    private TextView tvNetProfit, tvProfitMargin, tvProfitGrowth;
    private TextView tvSalesKg, tvSalesBags, tvPurchaseKg, tvPurchaseBags;
    private TextView tvCustomerDue, tvCustomerCount, tvSupplierPayable, tvSupplierCount;
    private TextView tvStockKg, tvStockBags, tvLowStockCount;
    private TextView tvHealthTrend, tvHealthStatus;

    private MaterialCardView cardCustomerDue, cardSupplierPayable, cardStock;
    private LinearLayout layoutTopSellingRice;

    private BarChart barChartSalesVsPurchase;
    private LineChart lineChartProfitTrend;
    private PieChart pieChartExpenses;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String userId;

    private long startDateMillis = 0;
    private long endDateMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_analytics);

        sessionManager = SessionManager.getInstance(this);
        userId = sessionManager.getUserId();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupPeriodSelector();
        setupChartsStyle();

        // Default: Today
        setPeriodToday();
        loadAnalyticsData();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            int topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            View headerView = findViewById(R.id.headerLayout);
            if (headerView != null) {
                headerView.setPadding(headerView.getPaddingLeft(), topInsets, headerView.getPaddingRight(), headerView.getPaddingBottom());
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        chipGroupPeriod = findViewById(R.id.chipGroupPeriod);

        tvKpiSales = findViewById(R.id.tvKpiSales);
        tvKpiPurchase = findViewById(R.id.tvKpiPurchase);
        tvKpiProfit = findViewById(R.id.tvKpiProfit);
        tvKpiExpense = findViewById(R.id.tvKpiExpense);

        tvNetProfit = findViewById(R.id.tvNetProfit);
        tvProfitMargin = findViewById(R.id.tvProfitMargin);
        tvProfitGrowth = findViewById(R.id.tvProfitGrowth);

        tvSalesKg = findViewById(R.id.tvSalesKg);
        tvSalesBags = findViewById(R.id.tvSalesBags);
        tvPurchaseKg = findViewById(R.id.tvPurchaseKg);
        tvPurchaseBags = findViewById(R.id.tvPurchaseBags);

        tvCustomerDue = findViewById(R.id.tvCustomerDue);
        tvCustomerCount = findViewById(R.id.tvCustomerCount);
        tvSupplierPayable = findViewById(R.id.tvSupplierPayable);
        tvSupplierCount = findViewById(R.id.tvSupplierCount);

        tvStockKg = findViewById(R.id.tvStockKg);
        tvStockBags = findViewById(R.id.tvStockBags);
        tvLowStockCount = findViewById(R.id.tvLowStockCount);

        tvHealthTrend = findViewById(R.id.tvHealthTrend);
        tvHealthStatus = findViewById(R.id.tvHealthStatus);

        cardCustomerDue = findViewById(R.id.cardCustomerDue);
        cardSupplierPayable = findViewById(R.id.cardSupplierPayable);
        cardStock = findViewById(R.id.cardStock);
        layoutTopSellingRice = findViewById(R.id.layoutTopSellingRice);

        barChartSalesVsPurchase = findViewById(R.id.barChartSalesVsPurchase);
        lineChartProfitTrend = findViewById(R.id.lineChartProfitTrend);
        pieChartExpenses = findViewById(R.id.pieChartExpenses);

        btnBack.setOnClickListener(v -> finish());
        cardCustomerDue.setOnClickListener(v -> finish());
        cardSupplierPayable.setOnClickListener(v -> finish());
        cardStock.setOnClickListener(v -> finish());
    }

    private void setupPeriodSelector() {
        chipGroupPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipToday) {
                setPeriodToday();
            } else if (checkedId == R.id.chipWeek) {
                setPeriodWeek();
            } else if (checkedId == R.id.chipMonth) {
                setPeriodMonth();
            } else if (checkedId == R.id.chipLastMonth) {
                setPeriodLastMonth();
            } else if (checkedId == R.id.chipCustom) {
                showCustomDatePicker();
                return;
            }
            loadAnalyticsData();
        });
    }

    private void setPeriodToday() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDateMillis = cal.getTimeInMillis();
        endDateMillis = System.currentTimeMillis();
    }

    private void setPeriodWeek() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.add(Calendar.DAY_OF_YEAR, -6);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDateMillis = cal.getTimeInMillis();
        endDateMillis = System.currentTimeMillis();
    }

    private void setPeriodMonth() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDateMillis = cal.getTimeInMillis();
        endDateMillis = System.currentTimeMillis();
    }

    private void setPeriodLastMonth() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDateMillis = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        endDateMillis = cal.getTimeInMillis();
    }

    private void showCustomDatePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("তারিখের রেঞ্জ নির্বাচন করুন")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                startDateMillis = selection.first;
                endDateMillis = selection.second + (24 * 60 * 60 * 1000 - 1);
                loadAnalyticsData();
            }
        });

        picker.show(getSupportFragmentManager(), "CustomDateRange");
    }

    private void setupChartsStyle() {
        if (barChartSalesVsPurchase != null) {
            barChartSalesVsPurchase.getDescription().setEnabled(false);
            barChartSalesVsPurchase.setDrawGridBackground(false);
            barChartSalesVsPurchase.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            barChartSalesVsPurchase.getXAxis().setDrawGridLines(false);
            barChartSalesVsPurchase.getAxisLeft().setDrawGridLines(true);
            barChartSalesVsPurchase.getAxisRight().setEnabled(false);
            barChartSalesVsPurchase.setFitBars(true);
        }

        if (lineChartProfitTrend != null) {
            lineChartProfitTrend.getDescription().setEnabled(false);
            lineChartProfitTrend.setDrawGridBackground(false);
            lineChartProfitTrend.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            lineChartProfitTrend.getXAxis().setDrawGridLines(false);
            lineChartProfitTrend.getAxisLeft().setDrawGridLines(true);
            lineChartProfitTrend.getAxisRight().setEnabled(false);
        }

        if (pieChartExpenses != null) {
            pieChartExpenses.setDrawHoleEnabled(true);
            pieChartExpenses.setHoleColor(Color.WHITE);
            pieChartExpenses.setHoleRadius(48f);
            pieChartExpenses.setTransparentCircleRadius(52f);
            pieChartExpenses.getDescription().setEnabled(false);
            pieChartExpenses.getLegend().setEnabled(true);
        }
    }

    private void loadAnalyticsData() {
        if (userId == null) return;
        progressBar.setVisibility(View.VISIBLE);

        loadSalesAndPurchasesData();
        loadReceivablesAndPayablesData();
        loadStockData();
        loadExpenseData();
    }

    private void loadSalesAndPurchasesData() {
        db.collection("sales")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(salesSnap -> {
                    List<Sale> salesList = new ArrayList<>();
                    double totalSalesAmount = 0;
                    double totalSalesKg = 0;
                    double totalSalesBags = 0;
                    double totalCogs = 0;

                    Map<String, ProductSalesHolder> productSalesMap = new HashMap<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : salesSnap.getDocuments()) {
                        Sale sale = doc.toObject(Sale.class);
                        if (sale != null && sale.getCreatedAt() >= startDateMillis && sale.getCreatedAt() <= endDateMillis) {
                            salesList.add(sale);
                            totalSalesAmount += sale.getGrandTotal();

                            if (sale.getItems() != null) {
                                for (SaleItem item : sale.getItems()) {
                                    totalSalesKg += item.getTotalKg();
                                    totalSalesBags += item.getBagQuantity();
                                    totalCogs += (item.getCostPerKg() * item.getTotalKg());

                                    String pName = item.getProductNameSnapshot() != null ? item.getProductNameSnapshot() : "চাল";
                                    ProductSalesHolder holder = productSalesMap.getOrDefault(pName, new ProductSalesHolder(pName));
                                    holder.totalKg += item.getTotalKg();
                                    holder.totalAmount += item.getItemTotal();
                                    productSalesMap.put(pName, holder);
                                }
                            }
                        }
                    }

                    final double finalSalesAmount = totalSalesAmount;
                    final double finalSalesKg = totalSalesKg;
                    final double finalSalesBags = totalSalesBags;
                    final double finalCogs = totalCogs;

                    db.collection("purchases")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(purchasesSnap -> {
                                double totalPurchaseAmount = 0;
                                double totalPurchaseKg = 0;
                                double totalPurchaseBags = 0;

                                for (com.google.firebase.firestore.DocumentSnapshot doc : purchasesSnap.getDocuments()) {
                                    Purchase purchase = doc.toObject(Purchase.class);
                                    if (purchase != null && purchase.getCreatedAt() >= startDateMillis && purchase.getCreatedAt() <= endDateMillis) {
                                        totalPurchaseAmount += purchase.getGrandTotal();
                                        if (purchase.getItems() != null) {
                                            for (PurchaseItem item : purchase.getItems()) {
                                                totalPurchaseKg += item.getTotalKg();
                                                totalPurchaseBags += item.getBagQuantity();
                                            }
                                        }
                                    }
                                }

                                updateSalesAndPurchaseUi(finalSalesAmount, totalPurchaseAmount, finalSalesKg, finalSalesBags, totalPurchaseKg, totalPurchaseBags, finalCogs);
                                populateTopSellingRiceList(new ArrayList<>(productSalesMap.values()));
                            });
                });
    }

    private void updateSalesAndPurchaseUi(double salesAmount, double purchaseAmount, double salesKg, double salesBags, double purchaseKg, double purchaseBags, double cogs) {
        tvKpiSales.setText(UnitConverterHelper.formatCurrency(salesAmount));
        tvKpiPurchase.setText(UnitConverterHelper.formatCurrency(purchaseAmount));

        DecimalFormat df = new DecimalFormat("#,##0.#");
        tvSalesKg.setText(UnitConverterHelper.formatKg(salesKg));
        tvSalesBags.setText(df.format(salesBags) + " বস্তা");

        tvPurchaseKg.setText(UnitConverterHelper.formatKg(purchaseKg));
        tvPurchaseBags.setText(df.format(purchaseBags) + " বস্তা");

        double grossProfit = salesAmount - cogs;
        double netProfit = grossProfit;
        tvKpiProfit.setText(UnitConverterHelper.formatCurrency(netProfit));
        tvNetProfit.setText(UnitConverterHelper.formatCurrency(netProfit));

        double margin = salesAmount > 0 ? (netProfit / salesAmount) * 100.0 : 0.0;
        tvProfitMargin.setText(String.format(Locale.ENGLISH, "মার্জিন: %.1f%%", margin));

        updateSalesVsPurchaseBarChart(salesAmount, purchaseAmount);
    }

    private void loadReceivablesAndPayablesData() {
        db.collection("customers")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(custSnap -> {
                    double totalDue = 0;
                    int dueCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : custSnap.getDocuments()) {
                        Customer c = doc.toObject(Customer.class);
                        if (c != null && c.getBaki() > 0) {
                            totalDue += c.getBaki();
                            dueCount++;
                        }
                    }
                    tvCustomerDue.setText(UnitConverterHelper.formatCurrency(totalDue));
                    tvCustomerCount.setText(dueCount + " জন ক্রেতা");
                });

        db.collection("suppliers")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(suppSnap -> {
                    double totalPayable = 0;
                    int payableCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : suppSnap.getDocuments()) {
                        Supplier s = doc.toObject(Supplier.class);
                        if (s != null && s.getCurrentPayable() > 0) {
                            totalPayable += s.getCurrentPayable();
                            payableCount++;
                        }
                    }
                    tvSupplierPayable.setText(UnitConverterHelper.formatCurrency(totalPayable));
                    tvSupplierCount.setText(payableCount + " জন মহাজন");
                });
    }

    private void loadStockData() {
        db.collection("riceProducts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(prodSnap -> {
                    double totalStockKg = 0;
                    double totalStockBags = 0;
                    int lowStockCount = 0;

                    int threshold = sessionManager.getLowStockThreshold();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : prodSnap.getDocuments()) {
                        RiceProduct p = doc.toObject(RiceProduct.class);
                        if (p != null) {
                            totalStockKg += p.getCurrentStockKg();
                            totalStockBags += p.getCurrentStockBags();
                            if (p.getCurrentStockKg() <= threshold) {
                                lowStockCount++;
                            }
                        }
                    }

                    DecimalFormat df = new DecimalFormat("#,##0.#");
                    tvStockKg.setText(UnitConverterHelper.formatKg(totalStockKg));
                    tvStockBags.setText("(" + df.format(totalStockBags) + " বস্তা)");
                    tvLowStockCount.setText(lowStockCount + "টি চাল স্টক কম");
                });
    }

    private void loadExpenseData() {
        db.collection("expenses")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(expSnap -> {
                    progressBar.setVisibility(View.GONE);
                    double totalExpense = 0;
                    Map<String, Double> categoryMap = new HashMap<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : expSnap.getDocuments()) {
                        Long date = doc.getLong("date");
                        Double amount = doc.getDouble("amount");
                        String cat = doc.getString("category");

                        if (amount != null && date != null && date >= startDateMillis && date <= endDateMillis) {
                            totalExpense += amount;
                            String categoryName = cat != null ? cat : "অন্যান্য";
                            categoryMap.put(categoryName, categoryMap.getOrDefault(categoryName, 0.0) + amount);
                        }
                    }

                    tvKpiExpense.setText(UnitConverterHelper.formatCurrency(totalExpense));
                    updateExpensePieChart(categoryMap);
                }).addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }

    private void updateSalesVsPurchaseBarChart(double sales, double purchase) {
        if (barChartSalesVsPurchase == null) return;

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) sales));
        entries.add(new BarEntry(1f, (float) purchase));

        BarDataSet dataSet = new BarDataSet(entries, "বিক্রি ও ক্রয়");
        dataSet.setColors(Color.parseColor("#22C55E"), Color.parseColor("#F59E0B"));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        String[] labels = {"বিক্রি ৳", "ক্রয় ৳"};
        barChartSalesVsPurchase.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartSalesVsPurchase.setData(barData);
        barChartSalesVsPurchase.invalidate();
    }

    private void updateExpensePieChart(Map<String, Double> categoryMap) {
        if (pieChartExpenses == null || categoryMap.isEmpty()) return;

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int[] palette = {
            Color.parseColor("#3B82F6"), Color.parseColor("#EF4444"), Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"), Color.parseColor("#8B5CF6"), Color.parseColor("#EC4899")
        };
        int idx = 0;

        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            colors.add(palette[idx % palette.length]);
            idx++;
        }

        PieDataSet dataSet = new PieDataSet(entries, "খরচের খাত");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieChartExpenses.setData(pieData);
        pieChartExpenses.invalidate();
    }

    private void populateTopSellingRiceList(List<ProductSalesHolder> list) {
        if (layoutTopSellingRice == null) return;
        layoutTopSellingRice.removeAllViews();

        Collections.sort(list, (o1, o2) -> Double.compare(o2.totalKg, o1.totalKg));

        int count = Math.min(5, list.size());
        for (int i = 0; i < count; i++) {
            ProductSalesHolder item = list.get(i);
            TextView tvItem = new TextView(this);
            tvItem.setPadding(0, 8, 0, 8);
            tvItem.setTextSize(13);
            tvItem.setTextColor(getResources().getColor(R.color.text_primary));
            tvItem.setText((i + 1) + ". " + item.name + " — " + UnitConverterHelper.formatKg(item.totalKg) + " (" + UnitConverterHelper.formatCurrency(item.totalAmount) + ")");
            layoutTopSellingRice.addView(tvItem);
        }
    }

    private static class ProductSalesHolder {
        String name;
        double totalKg;
        double totalAmount;

        ProductSalesHolder(String name) {
            this.name = name;
        }
    }
}
