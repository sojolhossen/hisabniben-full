package com.sajoldev.hisabniben.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.PaymentMethodAdapter;
import com.sajoldev.hisabniben.model.PaymentMethod;
import com.sajoldev.hisabniben.model.SmsPackage;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {
    private TextView tvPackageName, tvSmsCount, tvPrice;
    private TextView tvPaymentMethodName, tvAccountNumber, tvAccountType, tvInstructions;
    private RecyclerView rvPaymentMethods;
    private MaterialCardView cardPaymentDetails;
    private TextInputEditText etSenderNumber, etTransactionId;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private SmsPackage selectedPackage;
    private PaymentMethod selectedMethod;
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private PaymentMethodAdapter adapter;
    private SessionManager sessionManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_payment);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        selectedPackage = (SmsPackage) getIntent().getSerializableExtra("package");

        initViews();
        setupWindowInsets();
        loadPaymentMethods();
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
        tvPackageName = findViewById(R.id.tvPackageName);
        tvSmsCount = findViewById(R.id.tvSmsCount);
        tvPrice = findViewById(R.id.tvPrice);
        rvPaymentMethods = findViewById(R.id.rvPaymentMethods);
        cardPaymentDetails = findViewById(R.id.cardPaymentDetails);
        tvPaymentMethodName = findViewById(R.id.tvPaymentMethodName);
        tvAccountNumber = findViewById(R.id.tvAccountNumber);
        tvAccountType = findViewById(R.id.tvAccountType);
        tvInstructions = findViewById(R.id.tvInstructions);
        etSenderNumber = findViewById(R.id.etSenderNumber);
        etTransactionId = findViewById(R.id.etTransactionId);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        ImageView btnCopyNumber = findViewById(R.id.btnCopyNumber);

        btnBack.setOnClickListener(v -> finish());
        
        btnCopyNumber.setOnClickListener(v -> {
            String number = tvAccountNumber.getText().toString().replace("Account Number: ", "").trim();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Account Number", number);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Number copied!", Toast.LENGTH_SHORT).show();
        });

        if (selectedPackage != null) {
            tvPackageName.setText(selectedPackage.getName());
            tvSmsCount.setText(selectedPackage.getSmsCount() + " SMS");
            tvPrice.setText("৳" + (int) selectedPackage.getPrice());
        }

        rvPaymentMethods.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new PaymentMethodAdapter(paymentMethods, this::onPaymentMethodSelected);
        rvPaymentMethods.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> submitPaymentRequest());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadPaymentMethods() {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("payment_methods")
            .whereEqualTo("active", true)
            .get()
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    paymentMethods.clear();
                    if (task.getResult() != null && !task.getResult().isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult()) {
                            PaymentMethod method = doc.toObject(PaymentMethod.class);
                            method.setId(doc.getId());
                            paymentMethods.add(method);
                        }
                    } else {
                        paymentMethods.add(new PaymentMethod("bkash", "bKash", "01XXXXXXXXX", "Personal", "Send money to the account number above", "bkash", true));
                        paymentMethods.add(new PaymentMethod("nagad", "Nagad", "01XXXXXXXXX", "Personal", "Send money via Nagad to the account number", "nagad", true));
                    }
                    adapter.notifyDataSetChanged();
                }
            });
    }

    private void onPaymentMethodSelected(PaymentMethod method) {
        selectedMethod = method;
        cardPaymentDetails.setVisibility(View.VISIBLE);
        tvPaymentMethodName.setText(method.getName());
        tvAccountNumber.setText("Account Number: " + method.getAccountNumber());
        tvAccountType.setText("Type: " + method.getAccountType());
        tvInstructions.setText(method.getInstructions());
    }

    private void submitPaymentRequest() {
        String senderNumber = etSenderNumber.getText() != null ? etSenderNumber.getText().toString().trim() : "";
        String transactionId = etTransactionId.getText() != null ? etTransactionId.getText().toString().trim() : "";

        if (selectedMethod == null) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }
        if (senderNumber.isEmpty()) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        if (transactionId.isEmpty()) {
            Toast.makeText(this, "Please enter transaction ID", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        String userId = sessionManager.getUserId();
        String userName = sessionManager.getUserName();

        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("userName", userName != null ? userName : "Unknown");
        request.put("packageId", selectedPackage.getId());
        request.put("packageName", selectedPackage.getName());
        request.put("paymentMethodId", selectedMethod.getId());
        request.put("paymentMethodName", selectedMethod.getName());
        request.put("senderNumber", senderNumber);
        request.put("transactionId", transactionId);
        request.put("amount", (int) selectedPackage.getPrice());
        request.put("smsCount", selectedPackage.getSmsCount());
        request.put("status", "pending");
        request.put("createdAt", System.currentTimeMillis());

        db.collection("payment_requests")
            .add(request)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Payment request submitted! Admin will verify shortly.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to submit request. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}