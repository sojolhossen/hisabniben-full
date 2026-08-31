package com.sajoldev.hisabniben.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
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

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ReportPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_TYPE = "report_type";
    public static final String TYPE_SALES = "sales";
    public static final String TYPE_PURCHASE = "purchase";
    public static final String TYPE_PROFIT_LOSS = "profit_loss";
    public static final String TYPE_CUSTOMER = "customer";
    public static final String TYPE_SUPPLIER = "supplier";
    public static final String TYPE_STOCK = "stock";
    public static final String TYPE_EXPENSE = "expense";
    public static final String TYPE_RETURN = "return";
    public static final String TYPE_LEDGER = "ledger";

    private ImageView btnBack;
    private TextView tvTitle, tvSubtitle, tvPageIndicator, tvNoData;
    private ChipGroup chipGroupPeriod;
    private ViewPager2 pdfViewPager;
    private ProgressBar progressBar;
    private View emptyState;
    private LinearLayout layoutActionButtons;
    private MaterialButton btnPdfDownload, btnShareReport, btnPrintReport;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private String userId;
    private String reportType;
    private String reportTitleBengali;

    private long startDateMillis = 0;
    private long endDateMillis = 0;
    private File generatedPdfFile;

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_report_preview);

        reportType = getIntent().getStringExtra(EXTRA_REPORT_TYPE);
        if (reportType == null) reportType = TYPE_SALES;

        sessionManager = SessionManager.getInstance(this);
        userId = sessionManager.getUserId();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupPeriodSelector();
        setupClickListeners();

        // Default: Month
        setPeriodMonth();
        loadReportData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePdfRenderer();
    }

    private void closePdfRenderer() {
        if (pdfRenderer != null) {
            try { pdfRenderer.close(); } catch (Exception ignored) {}
            pdfRenderer = null;
        }
        if (pdfDescriptor != null) {
            try { pdfDescriptor.close(); } catch (Exception ignored) {}
            pdfDescriptor = null;
        }
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
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        tvNoData = findViewById(R.id.tvNoData);

        chipGroupPeriod = findViewById(R.id.chipGroupPeriod);
        pdfViewPager = findViewById(R.id.pdfViewPager);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        layoutActionButtons = findViewById(R.id.layoutActionButtons);

        btnPdfDownload = findViewById(R.id.btnPdfDownload);
        btnShareReport = findViewById(R.id.btnShareReport);
        btnPrintReport = findViewById(R.id.btnPrintReport);

        switch (reportType) {
            case TYPE_SALES:
                reportTitleBengali = "বিক্রির রিপোর্ট (Sales Report)";
                break;
            case TYPE_PURCHASE:
                reportTitleBengali = "ক্রয়ের রিপোর্ট (Purchase Report)";
                break;
            case TYPE_PROFIT_LOSS:
                reportTitleBengali = "লাভ-ক্ষতির রিপোর্ট (Profit & Loss)";
                break;
            case TYPE_CUSTOMER:
                reportTitleBengali = "ক্রেতার রিপোর্ট (Customer Report)";
                break;
            case TYPE_SUPPLIER:
                reportTitleBengali = "মহাজনের রিপোর্ট (Supplier Report)";
                break;
            case TYPE_STOCK:
                reportTitleBengali = "স্টক রিপোর্ট (Stock Report)";
                break;
            case TYPE_EXPENSE:
                reportTitleBengali = "খরচের রিপোর্ট (Expense Report)";
                break;
            case TYPE_RETURN:
                reportTitleBengali = "রিটার্ন রিপোর্ট (Return Report)";
                break;
            default:
                reportTitleBengali = "লেজার রিপোর্ট (Ledger Report)";
                break;
        }

        tvTitle.setText(reportTitleBengali);
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
            } else if (checkedId == R.id.chipYear) {
                setPeriodYear();
            } else if (checkedId == R.id.chipCustom) {
                showCustomDatePicker();
                return;
            }
            loadReportData();
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

    private void setPeriodYear() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startDateMillis = cal.getTimeInMillis();

        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
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
                loadReportData();
            }
        });

        picker.show(getSupportFragmentManager(), "CustomDateRange");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPdfDownload.setOnClickListener(v -> {
            if (generatedPdfFile != null && generatedPdfFile.exists()) {
                Toast.makeText(this, "PDF সংরক্ষিত হয়েছে: " + generatedPdfFile.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        btnShareReport.setOnClickListener(v -> {
            if (generatedPdfFile != null && generatedPdfFile.exists()) {
                sharePdfFile(generatedPdfFile);
            }
        });

        btnPrintReport.setOnClickListener(v -> {
            if (generatedPdfFile != null && generatedPdfFile.exists()) {
                printPdfFile(generatedPdfFile);
            }
        });

        pdfViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (pdfRenderer != null) {
                    tvPageIndicator.setText((position + 1) + "/" + pdfRenderer.getPageCount());
                }
            }
        });
    }

    private void loadReportData() {
        if (userId == null) return;
        showProgress();

        if (TYPE_SALES.equals(reportType)) {
            loadSalesReport();
        } else if (TYPE_PURCHASE.equals(reportType)) {
            loadPurchaseReport();
        } else if (TYPE_CUSTOMER.equals(reportType)) {
            loadCustomerReport();
        } else if (TYPE_SUPPLIER.equals(reportType)) {
            loadSupplierReport();
        } else if (TYPE_STOCK.equals(reportType)) {
            loadStockReport();
        } else {
            loadSalesReport(); // Default fallback
        }
    }

    private void loadSalesReport() {
        db.collection("sales")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(salesSnap -> {
                    List<Sale> filteredSales = new ArrayList<>();
                    double totalAmount = 0;
                    double totalKg = 0;
                    double totalBags = 0;
                    double totalDue = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : salesSnap.getDocuments()) {
                        Sale sale = doc.toObject(Sale.class);
                        if (sale != null && sale.getCreatedAt() >= startDateMillis && sale.getCreatedAt() <= endDateMillis) {
                            filteredSales.add(sale);
                            totalAmount += sale.getGrandTotal();
                            totalDue += sale.getDueAmount();

                            if (sale.getItems() != null) {
                                for (SaleItem item : sale.getItems()) {
                                    totalKg += item.getTotalKg();
                                    totalBags += item.getBagQuantity();
                                }
                            }
                        }
                    }

                    if (filteredSales.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    generatePdfForSales(filteredSales, totalAmount, totalKg, totalBags, totalDue);
                    displayPdfPreview();
                }).addOnFailureListener(e -> showEmptyState());
    }

    private void loadPurchaseReport() {
        db.collection("purchases")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(purchaseSnap -> {
                    List<Purchase> filteredPurchases = new ArrayList<>();
                    double totalAmount = 0;
                    double totalKg = 0;
                    double totalBags = 0;
                    double totalPayable = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : purchaseSnap.getDocuments()) {
                        Purchase purchase = doc.toObject(Purchase.class);
                        if (purchase != null && purchase.getCreatedAt() >= startDateMillis && purchase.getCreatedAt() <= endDateMillis) {
                            filteredPurchases.add(purchase);
                            totalAmount += purchase.getGrandTotal();
                            totalPayable += purchase.getDueAmount();

                            if (purchase.getItems() != null) {
                                for (PurchaseItem item : purchase.getItems()) {
                                    totalKg += item.getTotalKg();
                                    totalBags += item.getBagQuantity();
                                }
                            }
                        }
                    }

                    if (filteredPurchases.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    generatePdfForPurchases(filteredPurchases, totalAmount, totalKg, totalBags, totalPayable);
                    displayPdfPreview();
                }).addOnFailureListener(e -> showEmptyState());
    }

    private void loadCustomerReport() {
        db.collection("customers")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(custSnap -> {
                    List<Customer> customerList = custSnap.toObjects(Customer.class);
                    if (customerList.isEmpty()) {
                        showEmptyState();
                        return;
                    }
                    generatePdfForCustomers(customerList);
                    displayPdfPreview();
                }).addOnFailureListener(e -> showEmptyState());
    }

    private void loadSupplierReport() {
        db.collection("suppliers")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(suppSnap -> {
                    List<Supplier> supplierList = suppSnap.toObjects(Supplier.class);
                    if (supplierList.isEmpty()) {
                        showEmptyState();
                        return;
                    }
                    generatePdfForSuppliers(supplierList);
                    displayPdfPreview();
                }).addOnFailureListener(e -> showEmptyState());
    }

    private void loadStockReport() {
        db.collection("riceProducts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(prodSnap -> {
                    List<RiceProduct> productList = prodSnap.toObjects(RiceProduct.class);
                    if (productList.isEmpty()) {
                        showEmptyState();
                        return;
                    }
                    generatePdfForStock(productList);
                    displayPdfPreview();
                }).addOnFailureListener(e -> showEmptyState());
    }

    private void generatePdfForSales(List<Sale> sales, double totalAmount, double totalKg, double totalBags, double totalDue) {
        try {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 36;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            Paint primaryPaint = new Paint(); primaryPaint.setColor(Color.parseColor("#15803D")); primaryPaint.setFakeBoldText(true); primaryPaint.setTextSize(16);
            Paint darkPaint = new Paint(); darkPaint.setColor(Color.parseColor("#1E293B")); darkPaint.setTextSize(9);
            Paint grayPaint = new Paint(); grayPaint.setColor(Color.parseColor("#64748B")); grayPaint.setTextSize(9);
            Paint linePaint = new Paint(); linePaint.setColor(Color.parseColor("#E2E8F0")); linePaint.setStrokeWidth(1);

            int y = 40;
            String storeName = sessionManager.getStoreName() != null ? sessionManager.getStoreName() : "HisabNiben Rice Store";
            canvas.drawText(storeName, margin, y, primaryPaint);
            y += 18;

            canvas.drawText("বিক্রির বিস্তারিত রিপোর্ট (Sales Report)", margin, y, darkPaint);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateRangeStr = "তারিখ: " + sdf.format(new Date(startDateMillis)) + " - " + sdf.format(new Date(endDateMillis));
            canvas.drawText(dateRangeStr, pageWidth - margin - 180, y, grayPaint);
            y += 15;

            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
            y += 20;

            // Summary Card
            Paint bgPaint = new Paint(); bgPaint.setColor(Color.parseColor("#F8FAFC"));
            canvas.drawRect(margin, y, pageWidth - margin, y + 45, bgPaint);

            Paint sumValPaint = new Paint(); sumValPaint.setColor(Color.parseColor("#15803D")); sumValPaint.setTextSize(12); sumValPaint.setFakeBoldText(true);
            canvas.drawText("মোট বিক্রি: " + UnitConverterHelper.formatCurrency(totalAmount), margin + 12, y + 26, sumValPaint);
            canvas.drawText("মোট চাল: " + UnitConverterHelper.formatKg(totalKg) + " (" + (int)totalBags + " বস্তা)", margin + 200, y + 26, darkPaint);
            canvas.drawText("মোট বাকি: " + UnitConverterHelper.formatCurrency(totalDue), margin + 390, y + 26, darkPaint);
            y += 60;

            // Table Headers
            Paint headPaint = new Paint(); headPaint.setColor(Color.parseColor("#475569")); headPaint.setTextSize(9); headPaint.setFakeBoldText(true);
            canvas.drawText("তারিখ", margin, y, headPaint);
            canvas.drawText("ক্রেতার নাম", margin + 65, y, headPaint);
            canvas.drawText("কেজি", margin + 210, y, headPaint);
            canvas.drawText("বস্তা", margin + 270, y, headPaint);
            canvas.drawText("মোট মূল্য", margin + 330, y, headPaint);
            canvas.drawText("জমা", margin + 410, y, headPaint);
            canvas.drawText("বাকি", margin + 480, y, headPaint);

            y += 8;
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
            y += 16;

            DecimalFormat df = new DecimalFormat("#,##0.#");
            int rowCount = Math.min(25, sales.size());

            for (int i = 0; i < rowCount; i++) {
                Sale s = sales.get(i);
                canvas.drawText(sdf.format(new Date(s.getCreatedAt())), margin, y, darkPaint);

                String cName = s.getCustomerName() != null ? s.getCustomerName() : "ক্যাশ কাস্টমার";
                if (cName.length() > 18) cName = cName.substring(0, 16) + "..";
                canvas.drawText(cName, margin + 65, y, darkPaint);

                double saleKg = 0, saleBags = 0;
                if (s.getItems() != null) {
                    for (SaleItem item : s.getItems()) {
                        saleKg += item.getTotalKg();
                        saleBags += item.getBagQuantity();
                    }
                }

                canvas.drawText(df.format(saleKg), margin + 210, y, darkPaint);
                canvas.drawText(df.format(saleBags), margin + 270, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(s.getGrandTotal()), margin + 330, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(s.getPaidAmount()), margin + 410, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(s.getDueAmount()), margin + 480, y, darkPaint);

                y += 18;
                if (y > pageHeight - 50) break;
            }

            document.finishPage(page);

            generatedPdfFile = new File(getExternalFilesDir(null), "sales_report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(generatedPdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generatePdfForPurchases(List<Purchase> purchases, double totalAmount, double totalKg, double totalBags, double totalPayable) {
        try {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 36;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            Paint primaryPaint = new Paint(); primaryPaint.setColor(Color.parseColor("#D97706")); primaryPaint.setFakeBoldText(true); primaryPaint.setTextSize(16);
            Paint darkPaint = new Paint(); darkPaint.setColor(Color.parseColor("#1E293B")); darkPaint.setTextSize(9);
            Paint grayPaint = new Paint(); grayPaint.setColor(Color.parseColor("#64748B")); grayPaint.setTextSize(9);
            Paint linePaint = new Paint(); linePaint.setColor(Color.parseColor("#E2E8F0")); linePaint.setStrokeWidth(1);

            int y = 40;
            String storeName = sessionManager.getStoreName() != null ? sessionManager.getStoreName() : "HisabNiben Rice Store";
            canvas.drawText(storeName, margin, y, primaryPaint);
            y += 18;

            canvas.drawText("ক্রয়ের বিস্তারিত রিপোর্ট (Purchase Report)", margin, y, darkPaint);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            canvas.drawText("তারিখ: " + sdf.format(new Date(startDateMillis)) + " - " + sdf.format(new Date(endDateMillis)), pageWidth - margin - 180, y, grayPaint);
            y += 15;

            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
            y += 20;

            // Summary Card
            Paint bgPaint = new Paint(); bgPaint.setColor(Color.parseColor("#FFFBEB"));
            canvas.drawRect(margin, y, pageWidth - margin, y + 45, bgPaint);

            Paint sumValPaint = new Paint(); sumValPaint.setColor(Color.parseColor("#D97706")); sumValPaint.setTextSize(12); sumValPaint.setFakeBoldText(true);
            canvas.drawText("মোট ক্রয়: " + UnitConverterHelper.formatCurrency(totalAmount), margin + 12, y + 26, sumValPaint);
            canvas.drawText("মোট চাল: " + UnitConverterHelper.formatKg(totalKg) + " (" + (int)totalBags + " বস্তা)", margin + 200, y + 26, darkPaint);
            canvas.drawText("মহাজনের পাওনা: " + UnitConverterHelper.formatCurrency(totalPayable), margin + 390, y + 26, darkPaint);
            y += 60;

            // Table Headers
            Paint headPaint = new Paint(); headPaint.setColor(Color.parseColor("#475569")); headPaint.setTextSize(9); headPaint.setFakeBoldText(true);
            canvas.drawText("তারিখ", margin, y, headPaint);
            canvas.drawText("মহাজনের নাম", margin + 65, y, headPaint);
            canvas.drawText("কেজি", margin + 210, y, headPaint);
            canvas.drawText("বস্তা", margin + 270, y, headPaint);
            canvas.drawText("ক্রয় মূল্য", margin + 330, y, headPaint);
            canvas.drawText("পরিশোধ", margin + 410, y, headPaint);
            canvas.drawText("পাওনা", margin + 480, y, headPaint);

            y += 8;
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
            y += 16;

            DecimalFormat df = new DecimalFormat("#,##0.#");
            int rowCount = Math.min(25, purchases.size());

            for (int i = 0; i < rowCount; i++) {
                Purchase p = purchases.get(i);
                canvas.drawText(sdf.format(new Date(p.getCreatedAt())), margin, y, darkPaint);

                String sName = p.getSupplierName() != null ? p.getSupplierName() : "চাল সরবরাহকারী";
                if (sName.length() > 18) sName = sName.substring(0, 16) + "..";
                canvas.drawText(sName, margin + 65, y, darkPaint);

                double purKg = 0, purBags = 0;
                if (p.getItems() != null) {
                    for (PurchaseItem item : p.getItems()) {
                        purKg += item.getTotalKg();
                        purBags += item.getBagQuantity();
                    }
                }

                canvas.drawText(df.format(purKg), margin + 210, y, darkPaint);
                canvas.drawText(df.format(purBags), margin + 270, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(p.getGrandTotal()), margin + 330, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(p.getPaidAmount()), margin + 410, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(p.getDueAmount()), margin + 480, y, darkPaint);

                y += 18;
                if (y > pageHeight - 50) break;
            }

            document.finishPage(page);

            generatedPdfFile = new File(getExternalFilesDir(null), "purchase_report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(generatedPdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generatePdfForCustomers(List<Customer> customers) {
        try {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 36;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            Paint primaryPaint = new Paint(); primaryPaint.setColor(Color.parseColor("#2563EB")); primaryPaint.setFakeBoldText(true); primaryPaint.setTextSize(16);
            Paint darkPaint = new Paint(); darkPaint.setColor(Color.parseColor("#1E293B")); darkPaint.setTextSize(9);
            Paint linePaint = new Paint(); linePaint.setColor(Color.parseColor("#E2E8F0")); linePaint.setStrokeWidth(1);

            int y = 40;
            canvas.drawText(sessionManager.getStoreName() != null ? sessionManager.getStoreName() : "HisabNiben", margin, y, primaryPaint);
            y += 18;
            canvas.drawText("ক্রেতার রিপোর্ট (Customer Summary)", margin, y, darkPaint);
            y += 20;

            Paint headPaint = new Paint(); headPaint.setColor(Color.parseColor("#475569")); headPaint.setTextSize(9); headPaint.setFakeBoldText(true);
            canvas.drawText("ক্রেতার নাম", margin, y, headPaint);
            canvas.drawText("মোবাইল নম্বর", margin + 180, y, headPaint);
            canvas.drawText("ঠিকানা", margin + 300, y, headPaint);
            canvas.drawText("বর্তমান বাকি ৳", margin + 450, y, headPaint);

            y += 8;
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
            y += 16;

            for (Customer c : customers) {
                canvas.drawText(c.getName() != null ? c.getName() : "-", margin, y, darkPaint);
                canvas.drawText(c.getPhone() != null ? c.getPhone() : "-", margin + 180, y, darkPaint);
                canvas.drawText(c.getAddress() != null ? c.getAddress() : "-", margin + 300, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(c.getBaki()), margin + 450, y, darkPaint);
                y += 18;
                if (y > pageHeight - 50) break;
            }

            document.finishPage(page);

            generatedPdfFile = new File(getExternalFilesDir(null), "customers_report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(generatedPdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generatePdfForSuppliers(List<Supplier> suppliers) {
        try {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 36;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            Paint primaryPaint = new Paint(); primaryPaint.setColor(Color.parseColor("#7C3AED")); primaryPaint.setFakeBoldText(true); primaryPaint.setTextSize(16);
            Paint darkPaint = new Paint(); darkPaint.setColor(Color.parseColor("#1E293B")); darkPaint.setTextSize(9);
            Paint linePaint = new Paint(); linePaint.setColor(Color.parseColor("#E2E8F0")); linePaint.setStrokeWidth(1);

            int y = 40;
            canvas.drawText(sessionManager.getStoreName() != null ? sessionManager.getStoreName() : "HisabNiben", margin, y, primaryPaint);
            y += 18;
            canvas.drawText("মহাজনের রিপোর্ট (Supplier Report)", margin, y, darkPaint);
            y += 20;

            Paint headPaint = new Paint(); headPaint.setColor(Color.parseColor("#475569")); headPaint.setTextSize(9); headPaint.setFakeBoldText(true);
            canvas.drawText("ব্যবসার নাম / মহাজন", margin, y, headPaint);
            canvas.drawText("ধরণ", margin + 180, y, headPaint);
            canvas.drawText("মোবাইল", margin + 300, y, headPaint);
            canvas.drawText("বর্তমান পাওনা ৳", margin + 430, y, headPaint);

            y += 8;
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
            y += 16;

            for (Supplier s : suppliers) {
                String name = s.getBusinessName() != null ? s.getBusinessName() : s.getName();
                canvas.drawText(name != null ? name : "-", margin, y, darkPaint);
                canvas.drawText(s.getSupplierType() != null ? s.getSupplierType() : "চাল মিল", margin + 180, y, darkPaint);
                canvas.drawText(s.getPhone() != null ? s.getPhone() : "-", margin + 300, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(s.getCurrentPayable()), margin + 430, y, darkPaint);
                y += 18;
                if (y > pageHeight - 50) break;
            }

            document.finishPage(page);

            generatedPdfFile = new File(getExternalFilesDir(null), "suppliers_report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(generatedPdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generatePdfForStock(List<RiceProduct> products) {
        try {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595;
            int pageHeight = 842;
            int margin = 36;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            Paint primaryPaint = new Paint(); primaryPaint.setColor(Color.parseColor("#16A34A")); primaryPaint.setFakeBoldText(true); primaryPaint.setTextSize(16);
            Paint darkPaint = new Paint(); darkPaint.setColor(Color.parseColor("#1E293B")); darkPaint.setTextSize(9);
            Paint linePaint = new Paint(); linePaint.setColor(Color.parseColor("#E2E8F0")); linePaint.setStrokeWidth(1);

            int y = 40;
            canvas.drawText(sessionManager.getStoreName() != null ? sessionManager.getStoreName() : "HisabNiben", margin, y, primaryPaint);
            y += 18;
            canvas.drawText("বর্তমান চালের স্টক রিপোর্ট (Stock Report)", margin, y, darkPaint);
            y += 20;

            Paint headPaint = new Paint(); headPaint.setColor(Color.parseColor("#475569")); headPaint.setTextSize(9); headPaint.setFakeBoldText(true);
            canvas.drawText("চালের নাম ও ব্র্যন্ড", margin, y, headPaint);
            canvas.drawText("স্টক কেজি", margin + 180, y, headPaint);
            canvas.drawText("স্টক বস্তা", margin + 270, y, headPaint);
            canvas.drawText("WAC ক্রয়মূল্য", margin + 360, y, headPaint);
            canvas.drawText("বিক্রয় মূল্য", margin + 450, y, headPaint);

            y += 8;
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint);
            y += 16;

            DecimalFormat df = new DecimalFormat("#,##0.#");
            for (RiceProduct p : products) {
                String pName = p.getName() != null ? p.getName() : "চাল";
                canvas.drawText(pName, margin, y, darkPaint);
                canvas.drawText(df.format(p.getCurrentStockKg()) + " KG", margin + 180, y, darkPaint);
                canvas.drawText(df.format(p.getCurrentStockBags()) + " বস্তা", margin + 270, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(p.getPurchaseRatePerKg()) + "/KG", margin + 360, y, darkPaint);
                canvas.drawText(UnitConverterHelper.formatCurrency(p.getSaleRatePerKg()) + "/KG", margin + 450, y, darkPaint);
                y += 18;
                if (y > pageHeight - 50) break;
            }

            document.finishPage(page);

            generatedPdfFile = new File(getExternalFilesDir(null), "stock_report_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(generatedPdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayPdfPreview() {
        if (generatedPdfFile == null || !generatedPdfFile.exists()) {
            showEmptyState();
            return;
        }

        try {
            closePdfRenderer();
            pdfDescriptor = ParcelFileDescriptor.open(generatedPdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                pdfRenderer = new PdfRenderer(pdfDescriptor);
                pdfViewPager.setAdapter(new PdfPageAdapter(pdfRenderer));
                pdfViewPager.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                layoutActionButtons.setVisibility(View.VISIBLE);
                tvPageIndicator.setText("1/" + pdfRenderer.getPageCount());
            }
        } catch (Exception e) {
            showEmptyState();
        } finally {
            hideProgress();
        }
    }

    private void sharePdfFile(File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, reportTitleBengali);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন: " + reportTitleBengali));
    }

    private void printPdfFile(File file) {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager != null) {
            String jobName = getString(R.string.app_name) + " " + reportTitleBengali;
            PrintDocumentAdapter printAdapter = new PrintDocumentAdapter() {
                @Override
                public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, android.os.CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
                    if (cancellationSignal.isCanceled()) {
                        callback.onLayoutCancelled();
                        return;
                    }
                    PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build();
                    callback.onLayoutFinished(info, true);
                }

                @Override
                public void onWrite(android.print.PageRange[] pages, ParcelFileDescriptor destination, android.os.CancellationSignal cancellationSignal, WriteResultCallback callback) {
                    try (java.io.InputStream input = new java.io.FileInputStream(file);
                         java.io.OutputStream output = new java.io.FileOutputStream(destination.getFileDescriptor())) {
                        byte[] buf = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = input.read(buf)) > 0) {
                            output.write(buf, 0, bytesRead);
                        }
                        callback.onWriteFinished(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES});
                    } catch (Exception e) {
                        callback.onWriteFailed(e.getMessage());
                    }
                }
            };
            printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        pdfViewPager.setVisibility(View.GONE);
        layoutActionButtons.setVisibility(View.GONE);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        hideProgress();
        pdfViewPager.setVisibility(View.GONE);
        layoutActionButtons.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    // PDF Page Adapter
    private class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder> {
        private final PdfRenderer renderer;

        PdfPageAdapter(PdfRenderer renderer) {
            this.renderer = renderer;
        }

        @NonNull
        @Override
        public PdfPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(ReportPreviewActivity.this);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackgroundColor(Color.WHITE);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            return new PdfPageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull PdfPageViewHolder holder, int position) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                PdfRenderer.Page page = renderer.openPage(position);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                holder.imageView.setImageBitmap(bitmap);
                page.close();
            }
        }

        @Override
        public int getItemCount() {
            return renderer != null ? renderer.getPageCount() : 0;
        }

        class PdfPageViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;

            PdfPageViewHolder(ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }
}
