package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.SmsPackageAdapter;
import com.sajoldev.hisabniben.model.SmsPackage;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuySmsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvCurrentSms, tvLowBalanceMessage, tvMonthlyUsageText, tvCurrentSmsBusinessName;
    private MaterialCardView cardLowBalanceAlert, cardUsageSummary, cardSmsIdentity;
    private MaterialButton btnSmsHistory, btnBuyHistory, btnBottomBuyHistory;
    private RecyclerView rvPackages;
    private ProgressBar progressBar;

    private SmsPackageAdapter adapter;
    private List<SmsPackage> packages = new ArrayList<>();
    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_buy_sms);

        sessionManager = SessionManager.getInstance(this);
        userId = sessionManager.getUserId();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupClickListeners();

        rvPackages.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new SmsPackageAdapter(packages, this::showPaymentConfirmationDialog);
        rvPackages.setAdapter(adapter);

        loadCurrentSmsBalance();
        loadSmsPackages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentSmsBalance();
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
        tvCurrentSms = findViewById(R.id.tvCurrentSms);
        cardLowBalanceAlert = findViewById(R.id.cardLowBalanceAlert);
        tvLowBalanceMessage = findViewById(R.id.tvLowBalanceMessage);
        cardUsageSummary = findViewById(R.id.cardUsageSummary);
        tvMonthlyUsageText = findViewById(R.id.tvMonthlyUsageText);
        cardSmsIdentity = findViewById(R.id.cardSmsIdentity);
        tvCurrentSmsBusinessName = findViewById(R.id.tvCurrentSmsBusinessName);

        btnSmsHistory = findViewById(R.id.btnSmsHistory);
        btnBuyHistory = findViewById(R.id.btnBuyHistory);
        btnBottomBuyHistory = findViewById(R.id.btnBottomBuyHistory);

        rvPackages = findViewById(R.id.rvPackages);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (cardSmsIdentity != null) {
            cardSmsIdentity.setOnClickListener(v -> openSmsIdentityDialog());
        }

        btnSmsHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, SmsHistoryActivity.class));
        });

        btnBuyHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, SmsBuyHistoryActivity.class));
        });

        btnBottomBuyHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, SmsBuyHistoryActivity.class));
        });
    }

    private void openSmsIdentityDialog() {
        com.sajoldev.hisabniben.dialog.SmsIdentityDialog dialog = new com.sajoldev.hisabniben.dialog.SmsIdentityDialog();
        dialog.setOnSavedListener(this::loadCurrentSmsBalance);
        dialog.show(getSupportFragmentManager(), "SmsIdentityDialog");
    }

    private void loadCurrentSmsBalance() {
        if (userId == null) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.sajoldev.hisabniben.model.User user = documentSnapshot.toObject(com.sajoldev.hisabniben.model.User.class);
                        String smsBizName = com.sajoldev.hisabniben.util.SmsTemplateManager.getEffectiveSmsBusinessName(user, sessionManager);
                        if (tvCurrentSmsBusinessName != null) {
                            tvCurrentSmsBusinessName.setText("SMS-এ প্রদর্শিত নাম: " + smsBizName);
                        }

                        Long smsLimit = documentSnapshot.getLong("smsLimit");
                        int remaining = smsLimit != null ? smsLimit.intValue() : 10;
                        tvCurrentSms.setText(remaining + " SMS Available");

                        if (remaining <= 0) {
                            cardLowBalanceAlert.setVisibility(View.VISIBLE);
                            tvLowBalanceMessage.setText("আপনার SMS ব্যালেন্স শেষ! ক্রেতা ও মহাজনকে নিয়মিত বার্তা পাঠাতে নতুন SMS প্যাক কিনুন।");
                        } else if (remaining < 10) {
                            cardLowBalanceAlert.setVisibility(View.VISIBLE);
                            tvLowBalanceMessage.setText("আপনার SMS ব্যালেন্স প্রায় শেষ (মাত্র " + remaining + " SMS বাকি)! নতুন প্যাক কিনে রাখুন।");
                        } else {
                            cardLowBalanceAlert.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void loadSmsPackages() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("sms_packages")
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    packages.clear();

                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            SmsPackage pkg = doc.toObject(SmsPackage.class);
                            if (pkg != null) {
                                pkg.setId(doc.getId());
                                packages.add(pkg);
                            }
                        }
                        Collections.sort(packages, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                    }

                    if (packages.isEmpty()) {
                        packages.add(new SmsPackage("default_1", "Basic Pack", 50, 29, "active", false));
                        packages.add(new SmsPackage("default_2", "Standard Pack", 100, 49, "active", true));
                        packages.add(new SmsPackage("default_3", "Premium Pack", 200, 89, "active", false));
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void showPaymentConfirmationDialog(SmsPackage pkg) {
        new AlertDialog.Builder(this)
                .setTitle("প্যাকেজ ক্রয় নিশ্চিতকরণ")
                .setMessage("আপনি " + pkg.getName() + " (" + pkg.getSmsCount() + " SMS) ৳" + (int) pkg.getPrice() + " টাকায় ক্রয় করতে যাচ্ছেন।\n\nপেমেন্ট সম্পন্ন করতে এগিয়ে যেতে চান?")
                .setPositiveButton("পেমেন্ট করতে এগিয়ে যান", (dialog, which) -> {
                    Intent intent = new Intent(BuySmsActivity.this, PaymentActivity.class);
                    intent.putExtra("package", pkg);
                    startActivity(intent);
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }
}