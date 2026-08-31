package com.sajoldev.hisabniben.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.util.SessionManager;
import com.onesignal.OneSignal;

public class LoginActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    
    private TextInputLayout tilPhone, tilPassword;
    private TextInputEditText etPhone, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvSignUp;

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFFF54927);
        }
        setContentView(R.layout.activity_login);
        com.sajoldev.hisabniben.util.ScreenSecurityHelper.allowScreenSharingAndRecording(this);

        initViews();
        initFirebase();
        setupWindowInsets();
        
        if (sessionManager.isLoggedIn()) {
            checkNotificationPermission();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.sajoldev.hisabniben.util.ScreenSecurityHelper.allowScreenSharingAndRecording(this);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            int topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(view.getPaddingLeft(), topInsets, view.getPaddingRight(), view.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvSignUp = findViewById(R.id.tvSignUp);

        btnLogin.setOnClickListener(v -> loginUser());
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        sessionManager = SessionManager.getInstance(this);
    }

    private void loginUser() {
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Please enter phone number");
            return;
        }

        if (phone.length() < 10) {
            tilPhone.setError("Please enter valid phone number");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Please enter password");
            return;
        }

        tilPhone.setError(null);
        tilPassword.setError(null);
        showLoading(true);

        String cleanPhone = formatPhoneNumber(phone);

        db.collection("users")
                .whereEqualTo("phone", cleanPhone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String userPassword = userDoc.getString("password");
                        
                        if (userPassword == null || !userPassword.equals(password)) {
                            showLoading(false);
                            tilPassword.setError("Invalid password");
                            Toast.makeText(LoginActivity.this, "Invalid password", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        String userId = userDoc.getId();
                        
                        long trialEnd = com.sajoldev.hisabniben.util.FirestoreUtil.getLongOrDefault(userDoc, "trialEnd", 0);
                        long subExpiry = com.sajoldev.hisabniben.util.FirestoreUtil.getLongOrDefault(userDoc, "subscriptionExpiryDate", 0);
                        String pkgName = userDoc.getString("subscriptionPackageName");
                        if (pkgName == null || pkgName.isEmpty()) {
                            pkgName = userDoc.getString("packageName");
                        }
                        Boolean onTrial = userDoc.getBoolean("onTrial");
                        Boolean isPremiumDoc = userDoc.getBoolean("isPremium");
                        boolean isPremium = isPremiumDoc != null && isPremiumDoc;
                        
                        if (onTrial != null && onTrial && trialEnd > 0 && System.currentTimeMillis() > trialEnd) {
                            db.collection("users").document(userId)
                                .update("onTrial", false, "remainingTrialDays", 0)
                                .addOnSuccessListener(aVoid -> {});
                        }
                        
                        sessionManager.createLoginSession(
                                userId,
                                userDoc.getString("name"),
                                cleanPhone,
                                userDoc.getString("storeName") != null ? userDoc.getString("storeName") : "",
                                isPremium,
                                userDoc.getBoolean("isAdmin") != null && userDoc.getBoolean("isAdmin"),
                                trialEnd,
                                subExpiry
                        );
                        
                        if (pkgName != null && !pkgName.isEmpty()) {
                            sessionManager.setSubscriptionPackageName(pkgName);
                        }
                        
                        String storeName = userDoc.getString("storeName");
                        showLoading(false);
                        
                        if (storeName == null || storeName.isEmpty()) {
                            navigateToCompleteProfile();
                        } else {
                            OneSignal.initWithContext(LoginActivity.this);
                            OneSignal.login(userId);
                            checkNotificationPermission();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(LoginActivity.this, "No account found. Please signup first.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatPhoneNumber(String phone) {
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.startsWith("880")) {
            // Already has 880
        } else if (phone.startsWith("0")) {
            phone = "880" + phone.substring(1);
        } else if (phone.length() == 10 && phone.startsWith("1")) {
            phone = "880" + phone;
        } else {
            phone = "880" + phone;
        }
        return phone;
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_REQUEST);
            } else {
                navigateToDashboard();
            }
        } else {
            navigateToDashboard();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            navigateToDashboard();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, com.sajoldev.hisabniben.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCompleteProfile() {
        Intent intent = new Intent(LoginActivity.this, CompleteProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
