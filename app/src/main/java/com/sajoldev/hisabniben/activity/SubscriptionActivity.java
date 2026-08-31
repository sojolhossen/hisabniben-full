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
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.PackageAdapter;
import com.sajoldev.hisabniben.model.SubscriptionPackage;
import com.sajoldev.hisabniben.model.User;
import com.sajoldev.hisabniben.util.BillingManager;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionActivity extends AppCompatActivity implements PackageAdapter.OnPackageClickListener {
    private ImageView btnBack;
    private ImageView ivStatusIcon;
    private TextView tvCurrentStatus, tvExpiryDate, tvHelplineNumber;
    private View cardSupportHelp;
    private RecyclerView rvPackages;
    private ProgressBar progressBar;
    private boolean storeIconLoaded = false;
    private String supportPhone = "01700000000";

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private BillingManager billingManager;
    private PackageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("SubscriptionActivity", "onCreate() started");
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_subscription);

        initViews();
        initFirebase();
        setupWindowInsets();
        setupRecyclerView();
        setupClickListeners();
        loadStoreIcon();
        loadSupportHelpline();
        updateCurrentStatus();
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
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvExpiryDate = findViewById(R.id.tvExpiryDate);
        cardSupportHelp = findViewById(R.id.cardSupportHelp);
        tvHelplineNumber = findViewById(R.id.tvHelplineNumber);
        rvPackages = findViewById(R.id.rvPackages);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> finish());
        if (cardSupportHelp != null) {
            cardSupportHelp.setOnClickListener(v -> dialSupportNumber());
        }
    }

    private void dialSupportNumber() {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:" + supportPhone.replaceAll("[^0-9+]", "")));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "কল করা সম্ভব হয়নি: " + supportPhone, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSupportHelpline() {
        FirebaseFirestore.getInstance()
            .collection("settings")
            .document("support")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String phone = doc.getString("phone");
                    if (phone != null && !phone.isEmpty()) {
                        supportPhone = phone;
                        if (tvHelplineNumber != null) {
                            tvHelplineNumber.setText("📞 " + phone + " (কল করতে ট্যাপ করুন)");
                        }
                    }
                }
            });
    }

    private void initFirebase() {
        sessionManager = SessionManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        billingManager = BillingManager.getInstance(this);
    }

    private void loadStoreIcon() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;
        
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        updateStoreIcon(user.getStoreType());
                    }
                }
            });
    }
    
    private void updateStoreIcon(String storeType) {
        if (storeType == null || storeType.isEmpty()) {
            return;
        }
        
        int imageRes;
        switch (storeType) {
            case "Grocery Shop":
                imageRes = R.drawable.grocery_shop;
                break;
            case "Medical Store":
                imageRes = R.drawable.medical_store;
                break;
            case "Clothing Shop":
                imageRes = R.drawable.clothing_shop;
                break;
            case "Electronics":
                imageRes = R.drawable.electronics_shop;
                break;
            case "Bakery":
                imageRes = R.drawable.bakery;
                break;
            case "Book Shop":
                imageRes = R.drawable.book_shop;
                break;
            case "Footwear":
                imageRes = R.drawable.footwear;
                break;
            case "Mobile Shop":
                imageRes = R.drawable.mobile_shop;
                break;
            default:
                return;
        }
        
        ivStatusIcon.setImageResource(imageRes);
        ivStatusIcon.clearColorFilter();
        storeIconLoaded = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPackages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) {
            billingManager.endConnection();
        }
    }

    private void setupRecyclerView() {
        adapter = new PackageAdapter(this, this);
        rvPackages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPackages.setAdapter(adapter);
    }

    private void setupClickListeners() {
    }

    private void updateCurrentStatus() {
        if (storeIconLoaded) {
            return;
        }
        
        if (sessionManager.isPremium()) {
            ivStatusIcon.setImageResource(R.drawable.ic_star);
            ivStatusIcon.setColorFilter(getResources().getColor(R.color.premium, getTheme()));
            tvCurrentStatus.setText("Premium");
            tvCurrentStatus.setTextColor(getResources().getColor(R.color.premium, getTheme()));
            
            long remainingDays = sessionManager.getRemainingSubscriptionDays();
            if (remainingDays >= 999) {
                tvExpiryDate.setText("Life-Time");
            } else {
                tvExpiryDate.setText(remainingDays > 0 ? remainingDays + " দিন বাকি" : "Active");
            }
        } else if (sessionManager.isOnTrial()) {
            ivStatusIcon.setImageResource(R.drawable.ic_star_outline);
            ivStatusIcon.setColorFilter(getResources().getColor(R.color.trial, getTheme()));
            tvCurrentStatus.setText("Free Trial");
            tvCurrentStatus.setTextColor(getResources().getColor(R.color.trial, getTheme()));
            
            long remainingDays = sessionManager.getRemainingTrialDays();
            tvExpiryDate.setText(remainingDays + " days remaining");
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_star_outline);
            ivStatusIcon.setColorFilter(getResources().getColor(R.color.expired, getTheme()));
            tvCurrentStatus.setText("Trial Expired");
            tvCurrentStatus.setTextColor(getResources().getColor(R.color.expired, getTheme()));
            
            tvExpiryDate.setText("Upgrade to continue");
        }
    }

    private List<SubscriptionPackage> availablePackages = new ArrayList<>();

    private void loadPackages() {
        android.util.Log.d("SubscriptionActivity", "loadPackages() called");
        showLoading(true);

        firestoreManager.getActivePackages(new FirestoreManager.FirestoreListCallback<SubscriptionPackage>() {
            @Override
            public void onSuccess(List<SubscriptionPackage> result) {
                android.util.Log.e("SubscriptionActivity", "onSuccess: Found " + result.size() + " packages in Firestore");
                Toast.makeText(SubscriptionActivity.this, "Found " + result.size() + " active packages", Toast.LENGTH_SHORT).show();
                availablePackages = result;
                adapter.updateData(result);
                showLoading(false);
                
                if (result.isEmpty()) {
                    android.util.Log.w("SubscriptionActivity", "No active packages found in Firestore 'packages' collection");
                    Toast.makeText(SubscriptionActivity.this, "No packages available. Contact admin.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("SubscriptionActivity", "onFailure: " + error);
                Toast.makeText(SubscriptionActivity.this, error, Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPackageClick(SubscriptionPackage pkg) {
        if (pkg.getPlayStoreProductId() != null && !pkg.getPlayStoreProductId().isEmpty()) {
            billingManager.setCallback(new BillingManager.BillingCallback() {
                @Override
                public void onBillingReady() {
                    billingManager.launchSubscriptionFlow(SubscriptionActivity.this, pkg.getPlayStoreProductId(), pkg.getDurationDays());
                }

                @Override
                public void onPurchaseSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(SubscriptionActivity.this, "Purchase successful!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onPurchaseError(String error) {
                    runOnUiThread(() -> {
                        if (error.contains("not available") || error.contains("Play Store")) {
                            Toast.makeText(SubscriptionActivity.this, "Please install the app from Google Play Store for purchases", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SubscriptionActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onSubscriptionStatus(boolean isPremium, long expiryDate) {
                    runOnUiThread(() -> {
                        sessionManager.updatePremiumStatus(isPremium, expiryDate);
                        if (pkg != null && pkg.getName() != null) {
                            sessionManager.setSubscriptionPackageName(pkg.getName());
                        }
                        
                        String userId = sessionManager.getUserId();
                        firestoreManager.upgradeUserToPremium(userId, "", expiryDate, pkg != null ? pkg.getName() : "", new FirestoreManager.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    updateCurrentStatus();
                                    Toast.makeText(SubscriptionActivity.this, "Premium activated!", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(SubscriptionActivity.this, error, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    });
                }
            });

            List<String> productIds = new ArrayList<>();
            for (SubscriptionPackage p : availablePackages) {
                if (p.getPlayStoreProductId() != null && !p.getPlayStoreProductId().isEmpty()) {
                    productIds.add(p.getPlayStoreProductId());
                }
            }
            
            // If the current clicked package ID is not in the list (should not happen), add it
            if (!productIds.contains(pkg.getPlayStoreProductId())) {
                productIds.add(pkg.getPlayStoreProductId());
            }

            billingManager.startConnection(productIds);
        } else {
            Toast.makeText(this, "Contact admin for this plan", Toast.LENGTH_LONG).show();
        }
    }
}
