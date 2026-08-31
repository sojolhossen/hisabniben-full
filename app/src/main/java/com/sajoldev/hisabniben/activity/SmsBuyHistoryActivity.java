package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.SmsBuyHistoryAdapter;
import com.sajoldev.hisabniben.model.SmsBuyHistory;
import com.sajoldev.hisabniben.util.SessionManager;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsBuyHistoryActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvCurrentSms, tvTotalPurchasedSms, tvTotalSpentAmount;
    private MaterialButton btnBuySms, btnEmptyBuySms;
    private EditText etSearch;
    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private LinearLayout tvEmpty;

    private SmsBuyHistoryAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", new Locale("bn", "BD"));
    private DecimalFormat numberFormat = new DecimalFormat("#,##0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_sms_buy_history);

        db = FirebaseFirestore.getInstance();
        userId = SessionManager.getInstance(this).getUserId();

        initViews();
        setupWindowInsets();
        setupClickListeners();
        setupSearchListener();

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SmsBuyHistoryAdapter();
        adapter.setOnItemClickListener(this::showDetailsDialog);
        rvHistory.setAdapter(adapter);

        loadCurrentSmsBalance();
        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentSmsBalance();
        loadHistory();
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
        tvTotalPurchasedSms = findViewById(R.id.tvTotalPurchasedSms);
        tvTotalSpentAmount = findViewById(R.id.tvTotalSpentAmount);
        btnBuySms = findViewById(R.id.btnBuySms);
        btnEmptyBuySms = findViewById(R.id.btnEmptyBuySms);

        etSearch = findViewById(R.id.etSearch);
        rvHistory = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnBuySms.setOnClickListener(v -> startActivity(new Intent(this, BuySmsActivity.class)));
        btnEmptyBuySms.setOnClickListener(v -> startActivity(new Intent(this, BuySmsActivity.class)));
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCurrentSmsBalance() {
        if (userId == null) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long smsLimit = documentSnapshot.getLong("smsLimit");
                        int remaining = smsLimit != null ? smsLimit.intValue() : 10;
                        tvCurrentSms.setText(remaining + " SMS");
                    }
                });
    }

    private void loadHistory() {
        if (userId == null) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("payment_requests")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    List<SmsBuyHistory> list = new ArrayList<>();
                    int totalApprovedSms = 0;
                    double totalApprovedSpent = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SmsBuyHistory history = doc.toObject(SmsBuyHistory.class);
                        if (history != null) {
                            history.setId(doc.getId());
                            list.add(history);

                            if ("approved".equalsIgnoreCase(history.getStatus())) {
                                totalApprovedSms += history.getSmsCount();
                                totalApprovedSpent += history.getAmount();
                            }
                        }
                    }

                    tvTotalPurchasedSms.setText(numberFormat.format(totalApprovedSms) + " SMS");
                    tvTotalSpentAmount.setText("৳" + numberFormat.format(totalApprovedSpent));

                    if (list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvHistory.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                        adapter.setData(list);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                });
    }

    private void showDetailsDialog(SmsBuyHistory history) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sms_buy_details, null);
        dialog.setContentView(dialogView);

        MaterialCardView cardStatusBanner = dialogView.findViewById(R.id.cardStatusBanner);
        TextView tvStatusBannerMessage = dialogView.findViewById(R.id.tvStatusBannerMessage);

        TextView tvPackageName = dialogView.findViewById(R.id.tvPackageName);
        TextView tvSmsCount = dialogView.findViewById(R.id.tvSmsCount);
        TextView tvAmount = dialogView.findViewById(R.id.tvAmount);
        TextView tvPaymentMethod = dialogView.findViewById(R.id.tvPaymentMethod);
        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        TextView tvTransactionId = dialogView.findViewById(R.id.tvTransactionId);
        TextView tvSenderNumber = dialogView.findViewById(R.id.tvSenderNumber);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);

        MaterialButton btnRetryBuySms = dialogView.findViewById(R.id.btnRetryBuySms);
        MaterialButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        tvPackageName.setText(history.getPackageName() != null ? history.getPackageName() : "SMS Pack");
        tvSmsCount.setText(history.getSmsCount() + " SMS");
        tvAmount.setText("৳" + (int) history.getAmount());
        tvPaymentMethod.setText(history.getPaymentMethodName() != null ? history.getPaymentMethodName() : "bKash");
        tvTransactionId.setText(history.getTransactionId() != null ? history.getTransactionId() : "N/A");
        tvSenderNumber.setText(history.getSenderNumber() != null ? history.getSenderNumber() : "N/A");
        tvDate.setText(history.getCreatedAt() > 0 ? dateFormat.format(new Date(history.getCreatedAt())) : "N/A");

        String status = history.getStatus();
        if ("approved".equalsIgnoreCase(status)) {
            tvStatus.setText("APPROVED (অনুমোদিত)");
            tvStatus.setTextColor(Color.parseColor("#15803D"));
            cardStatusBanner.setStrokeColor(Color.parseColor("#15803D"));
            tvStatusBannerMessage.setText("পেমেন্ট অনুমোদিত। আপনার একাউন্টে SMS সফলভাবে যোগ করা হয়েছে।");
            btnRetryBuySms.setVisibility(View.GONE);
        } else if ("rejected".equalsIgnoreCase(status)) {
            tvStatus.setText("REJECTED (বাতিল)");
            tvStatus.setTextColor(Color.parseColor("#DC2626"));
            cardStatusBanner.setStrokeColor(Color.parseColor("#DC2626"));
            String reason = history.getRejectionReason();
            if (reason != null && !reason.trim().isEmpty()) {
                tvStatusBannerMessage.setText("এই পেমেন্ট রিকোয়েস্ট অনুমোদিত হয়নি।\nকারণ: " + reason);
            } else {
                tvStatusBannerMessage.setText("এই পেমেন্ট রিকোয়েস্ট অনুমোদিত হয়নি। তথ্য পুনরায় পরীক্ষা করে আবার কিনুন।");
            }
            btnRetryBuySms.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText("PENDING (অপেক্ষমাণ)");
            tvStatus.setTextColor(Color.parseColor("#D97706"));
            cardStatusBanner.setStrokeColor(Color.parseColor("#D97706"));
            tvStatusBannerMessage.setText("আপনার পেমেন্ট রিকোয়েস্ট যাচাই করা হচ্ছে। অনুগ্রহ করে কিছুটা সময় অপেক্ষা করুন।");
            btnRetryBuySms.setVisibility(View.GONE);
        }

        btnRetryBuySms.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(SmsBuyHistoryActivity.this, BuySmsActivity.class));
        });

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
