package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {
    private TextInputLayout tilName, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etPhone, etPassword, etConfirmPassword;
    private MaterialButton btnSignup;
    private ProgressBar progressBar;
    private TextView tvLogin;
    private CheckBox cbTerms;
    private TextView tvPrivacyPolicy, tvTermsConditions;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    
    private String phoneNumber;
    private String generatedOtp;
    private String userName;
    private String userPassword;
    private CountDownTimer resendTimer;
    private static final long RESEND_TIME = 60_000;

    private String smsApiKey;
    private String senderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFFF54927);
        }
        setContentView(R.layout.activity_signup);
        com.sajoldev.hisabniben.util.ScreenSecurityHelper.allowScreenSharingAndRecording(this);

        initViews();
        initFirebase();
        setupWindowInsets();

        btnSignup.setOnClickListener(v -> signUp());
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tilName.setError(null);
            }
        });
        tvPrivacyPolicy.setOnClickListener(v -> openUrl("https://sites.google.com/view/hisabnibenprivacypolicy/home"));
        tvTermsConditions.setOnClickListener(v -> openUrl("https://sites.google.com/view/hisabnibentremscondition/home"));
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
        tilName = findViewById(R.id.tilName);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);
        progressBar = findViewById(R.id.progressBar);
        tvLogin = findViewById(R.id.tvLogin);
        cbTerms = findViewById(R.id.cbTerms);
        tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy);
        tvTermsConditions = findViewById(R.id.tvTermsConditions);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        sessionManager = SessionManager.getInstance(this);
        loadSmsSettings();
    }

    private void loadSmsSettings() {
        btnSignup.setEnabled(false);
        db.collection("settings").document("sms_api")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    btnSignup.setEnabled(true);
                    if (documentSnapshot.exists()) {
                        smsApiKey = documentSnapshot.getString("apiKey");
                        senderId = documentSnapshot.getString("senderId");
                    }
                })
                .addOnFailureListener(e -> {
                    btnSignup.setEnabled(true);
                });
    }

    private void signUp() {
        userName = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        userPassword = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
        
        if (!cbTerms.isChecked()) {
            tilName.setError("Please accept Privacy Policy and Terms");
            return;
        }
        
        if (TextUtils.isEmpty(userName)) {
            tilName.setError("Please enter your name");
            return;
        }
        
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Please enter phone number");
            return;
        }
        
        if (phone.length() < 10) {
            tilPhone.setError("Please enter valid phone number");
            return;
        }
        
        if (TextUtils.isEmpty(userPassword)) {
            tilPassword.setError("Please enter password");
            return;
        }
        
        if (userPassword.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }
        
        if (!userPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return;
        }
        
        tilName.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        
        phoneNumber = formatPhoneNumber(phone);
        
        checkUserExists(phoneNumber);
    }
    
    private void checkUserExists(String phone) {
        showLoading(true);
        
        db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    if (!queryDocumentSnapshots.isEmpty()) {
                        tilPhone.setError("This phone number is already registered");
                        Toast.makeText(SignupActivity.this, "Phone number already registered", Toast.LENGTH_SHORT).show();
                    } else {
                        sendOtp();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    sendOtp();
                });
    }

    private void sendOtp() {
        if (smsApiKey == null || smsApiKey.isEmpty() || senderId == null || senderId.isEmpty()) {
            Toast.makeText(this, "SMS service not available. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        generatedOtp = String.format("%06d", new Random().nextInt(999999));
        
        String message = "Your HisabNiben OTP is: " + generatedOtp + ". Valid for 5 minutes.";
        
        sendSmsViaApi(phoneNumber, message, () -> runOnUiThread(() -> {
            showOtpDialog();
        }), errorMsg -> runOnUiThread(() -> {
            Toast.makeText(SignupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
        }));
    }

    private void sendSmsViaApi(String phone, String message, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        if (smsApiKey == null || senderId == null) {
            onError.accept("SMS service not configured");
            return;
        }

        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.startsWith("880")) {
            // Already has 880
        } else if (cleanPhone.startsWith("0")) {
            cleanPhone = "880" + cleanPhone.substring(1);
        } else if (cleanPhone.length() == 10 && cleanPhone.startsWith("1")) {
            cleanPhone = "880" + cleanPhone;
        } else {
            cleanPhone = "880" + cleanPhone;
        }

        final String finalPhone = cleanPhone;

        new Thread(() -> {
            try {
                String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
                String url = "http://bulksmsbd.net/api/smsapi?api_key=" + smsApiKey 
                        + "&type=text&number=" + finalPhone 
                        + "&senderid=" + senderId 
                        + "&message=" + encodedMessage;
                
                java.net.URL urlObj = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    if (response.toString().contains("success") || response.toString().contains("OK") || response.toString().contains("1")) {
                        onSuccess.run();
                    } else {
                        onError.accept("Failed to send OTP: " + response.toString());
                    }
                } else {
                    onError.accept("Failed to send OTP. Please try again.");
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                onError.accept("Error: " + e.getMessage());
            }
        }).start();
    }

    private void showOtpDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_otp_verify, null);
        
        TextView tvPhoneDisplay = dialogView.findViewById(R.id.tvPhoneDisplay);
        TextInputEditText etOtp = dialogView.findViewById(R.id.etOtp);
        TextView tvResendOtp = dialogView.findViewById(R.id.tvResendOtp);
        TextView tvCountdown = dialogView.findViewById(R.id.tvCountdown);
        MaterialButton btnVerify = dialogView.findViewById(R.id.btnVerify);
        ProgressBar dialogProgress = dialogView.findViewById(R.id.progressBar);
        
        tvPhoneDisplay.setText(phoneNumber);
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        
        tvResendOtp.setOnClickListener(v -> {
            sendOtp();
            Toast.makeText(this, "OTP resent", Toast.LENGTH_SHORT).show();
        });
        
        startResendTimer(tvResendOtp, tvCountdown);
        
        btnVerify.setOnClickListener(v -> {
            String code = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";
            
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (code.length() != 6) {
                Toast.makeText(this, "Please enter 6-digit code", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (code.equals(generatedOtp)) {
                dialog.dismiss();
                createUser();
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }

    private void startResendTimer(TextView tvResend, TextView tvCountdown) {
        tvResend.setVisibility(View.GONE);
        tvCountdown.setVisibility(View.VISIBLE);
        
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        
        resendTimer = new CountDownTimer(RESEND_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvCountdown.setText("Resend in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                tvResend.setVisibility(View.VISIBLE);
                tvCountdown.setVisibility(View.GONE);
            }
        }.start();
    }

    private void createUser() {
        showLoading(true);

        // Fetch configurable trial duration from Firestore settings
        db.collection("settings").document("system")
                .get()
                .addOnCompleteListener(task -> {
                    long trialDays = 7;
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Long days = task.getResult().getLong("trialDurationDays");
                        if (days != null && days > 0) {
                            trialDays = days;
                        }
                    }

                    final long finalTrialDays = trialDays;
                    final long durationMs = finalTrialDays * 24L * 60L * 60L * 1000L;
                    final long now = System.currentTimeMillis();
                    final long trialEnd = now + durationMs;

                    String userId = db.collection("users").document().getId();

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", userId);
                    userMap.put("name", userName);
                    userMap.put("phone", phoneNumber);
                    userMap.put("password", userPassword);
                    userMap.put("trialStart", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    userMap.put("trialEnd", trialEnd);
                    userMap.put("trialUsed", true);
                    userMap.put("subscriptionStatus", "TRIAL");
                    userMap.put("isPremium", false);
                    userMap.put("onTrial", true);
                    userMap.put("premium", false);
                    userMap.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    userMap.put("isAdmin", false);
                    userMap.put("smsLimit", 5);
                    userMap.put("customerLimit", 10);
                    userMap.put("productLimit", 10);
                    userMap.put("transactionLimit", 100);
                    userMap.put("remainingTrialDays", finalTrialDays);

                    db.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                sessionManager.createLoginSession(userId, userName, "", phoneNumber, false, false, trialEnd, 5);
                                Toast.makeText(SignupActivity.this, "Account created with " + finalTrialDays + "-day trial!", Toast.LENGTH_SHORT).show();
                                navigateToCompleteProfile();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(SignupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
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

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!show);
    }

    private void navigateToCompleteProfile() {
        Intent intent = new Intent(SignupActivity.this, CompleteProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
