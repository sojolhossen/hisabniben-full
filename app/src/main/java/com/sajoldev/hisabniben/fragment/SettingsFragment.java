package com.sajoldev.hisabniben.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.activity.AnalyticsActivity;
import com.sajoldev.hisabniben.activity.BuySmsActivity;
import com.sajoldev.hisabniben.activity.LoginActivity;
import com.sajoldev.hisabniben.activity.ProfileActivity;
import com.sajoldev.hisabniben.activity.ReportsActivity;
import com.sajoldev.hisabniben.activity.SmsBuyHistoryActivity;
import com.sajoldev.hisabniben.activity.SmsHistoryActivity;
import com.sajoldev.hisabniben.activity.SubscriptionActivity;
import com.sajoldev.hisabniben.dialog.RiceUnitSettingsDialog;
import com.sajoldev.hisabniben.dialog.StockSettingsDialog;
import com.sajoldev.hisabniben.util.ProfileImageHelper;
import com.sajoldev.hisabniben.util.SessionManager;

public class SettingsFragment extends Fragment {

    private ImageView ivProfileImage, ivStatusIcon;
    private TextView tvInitials, tvStoreName, tvUserName, tvStoreType, tvUserPhone;
    private TextView tvSubscriptionStatus, tvSubscriptionDays, tvSmsLimit;
    private TextView tvBagWeightSub, tvStockSub, tvAppVersion;
    private MaterialButton btnEditProfile, btnUpgrade, btnBuySms, btnSmsHistory, btnSmsBuyHistory, cardLogout;

    private MaterialCardView cardProfile, cardSubscription, cardSmsBalance;
    private LinearLayout cardProfileSettings, cardBagWeightSettings, cardStockSettings, cardTradeSettings;
    private LinearLayout cardReports, cardReport, cardTutorials, cardHelp, cardPrivacy, cardTerms;

    private SessionManager sessionManager;
    private ProfileImageHelper profileImageHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        profileImageHelper = ProfileImageHelper.getInstance(requireContext());

        initViews(view);
        setupClickListeners();
        loadUserData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initViews(View view) {
        cardProfile = view.findViewById(R.id.cardProfile);
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvInitials = view.findViewById(R.id.tvInitials);
        tvStoreName = view.findViewById(R.id.tvStoreName);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvStoreType = view.findViewById(R.id.tvStoreType);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        cardSubscription = view.findViewById(R.id.cardSubscription);
        ivStatusIcon = view.findViewById(R.id.ivStatusIcon);
        tvSubscriptionStatus = view.findViewById(R.id.tvSubscriptionStatus);
        tvSubscriptionDays = view.findViewById(R.id.tvSubscriptionDays);
        btnUpgrade = view.findViewById(R.id.btnUpgrade);

        cardSmsBalance = view.findViewById(R.id.cardSmsBalance);
        tvSmsLimit = view.findViewById(R.id.tvSmsLimit);
        btnBuySms = view.findViewById(R.id.btnBuySms);
        btnSmsHistory = view.findViewById(R.id.btnSmsHistory);
        btnSmsBuyHistory = view.findViewById(R.id.btnSmsBuyHistory);

        cardProfileSettings = view.findViewById(R.id.cardProfileSettings);
        cardBagWeightSettings = view.findViewById(R.id.cardBagWeightSettings);
        tvBagWeightSub = view.findViewById(R.id.tvBagWeightSub);
        cardStockSettings = view.findViewById(R.id.cardStockSettings);
        tvStockSub = view.findViewById(R.id.tvStockSub);
        cardTradeSettings = view.findViewById(R.id.cardTradeSettings);

        cardReports = view.findViewById(R.id.cardReports);
        cardReport = view.findViewById(R.id.cardReport);
        cardTutorials = view.findViewById(R.id.cardTutorials);
        cardHelp = view.findViewById(R.id.cardHelp);
        cardPrivacy = view.findViewById(R.id.cardPrivacy);
        cardTerms = view.findViewById(R.id.cardTerms);
        cardLogout = view.findViewById(R.id.cardLogout);
        tvAppVersion = view.findViewById(R.id.tvAppVersion);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> openProfile());
        cardProfile.setOnClickListener(v -> openProfile());
        if (cardProfileSettings != null) cardProfileSettings.setOnClickListener(v -> openProfile());

        btnUpgrade.setOnClickListener(v -> openSubscription());
        cardSubscription.setOnClickListener(v -> openSubscription());

        btnBuySms.setOnClickListener(v -> startActivity(new Intent(requireContext(), BuySmsActivity.class)));
        btnSmsHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), SmsHistoryActivity.class)));
        btnSmsBuyHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), SmsBuyHistoryActivity.class)));

        cardBagWeightSettings.setOnClickListener(v -> {
            RiceUnitSettingsDialog dialog = new RiceUnitSettingsDialog();
            dialog.setOnSavedListener(this::updateRiceSettingsSubtitles);
            dialog.show(getChildFragmentManager(), "RiceUnitSettings");
        });

        cardStockSettings.setOnClickListener(v -> {
            StockSettingsDialog dialog = new StockSettingsDialog();
            dialog.setOnSavedListener(this::updateRiceSettingsSubtitles);
            dialog.show(getChildFragmentManager(), "StockSettings");
        });

        cardTradeSettings.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "ডিফল্ট বিক্রয় ও ক্রয় ইউনিট: KG / বস্তা", Toast.LENGTH_SHORT).show();
        });

        cardReports.setOnClickListener(v -> startActivity(new Intent(requireContext(), AnalyticsActivity.class)));
        cardReport.setOnClickListener(v -> startActivity(new Intent(requireContext(), ReportsActivity.class)));

        if (cardTutorials != null) {
            cardTutorials.setOnClickListener(v -> startActivity(new Intent(requireContext(), com.sajoldev.hisabniben.activity.TutorialListActivity.class)));
        }

        cardHelp.setOnClickListener(v -> showContactSupportDialog());
        cardPrivacy.setOnClickListener(v -> openUrl("https://sites.google.com/view/hisabnibenprivacypolicy/home"));
        cardTerms.setOnClickListener(v -> openUrl("https://sites.google.com/view/hisabnibentremscondition/home"));

        cardLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void openProfile() {
        startActivity(new Intent(requireContext(), ProfileActivity.class));
    }

    private void openSubscription() {
        startActivity(new Intent(requireContext(), SubscriptionActivity.class));
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            syncUserDataFromFirestore(userId);
        }

        String storeName = sessionManager.getStoreName();
        String userName = sessionManager.getUserName();
        String phone = sessionManager.getUserPhone();

        tvStoreName.setText(storeName != null && !storeName.isEmpty() ? storeName : "রহিম রাইস ট্রেডার্স");
        tvUserName.setText("স্বত্বাধিকারী: " + (userName != null && !userName.isEmpty() ? userName : "মো: আব্দুর রহিম"));
        if (tvUserPhone != null && phone != null && !phone.isEmpty()) {
            tvUserPhone.setText("📞 " + phone);
        }

        String nameForInitials = userName != null && !userName.isEmpty() ? userName : storeName;
        if (nameForInitials != null && !nameForInitials.isEmpty()) {
            String[] parts = nameForInitials.split(" ");
            if (parts.length >= 2) {
                tvInitials.setText(("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase());
            } else {
                tvInitials.setText(nameForInitials.substring(0, Math.min(2, nameForInitials.length())).toUpperCase());
            }
        }

        loadProfileImage();
        updateSubscriptionStatus();
        loadSmsLimit();
        updateRiceSettingsSubtitles();

        if (tvAppVersion != null && isAdded()) {
            try {
                String versionName = requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                tvAppVersion.setText("HisabNiben Version " + versionName);
            } catch (Exception e) {
                tvAppVersion.setText("HisabNiben Version 2.1.0");
            }
        }
    }

    private void updateRiceSettingsSubtitles() {
        if (tvBagWeightSub != null) {
            tvBagWeightSub.setText("ডিফল্ট: ১ বস্তা = " + sessionManager.getDefaultBagWeight() + " KG (নতুন বিক্রির ক্ষেত্রে প্রযোজ্য)");
        }
        if (tvStockSub != null) {
            String negStockText = sessionManager.getAllowNegativeStock() ? "অনুমোদিত" : "বন্ধ";
            tvStockSub.setText("Negative Stock: " + negStockText + " | Low Stock Alert (" + sessionManager.getLowStockThreshold() + " KG)");
        }
    }

    private void loadProfileImage() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        Bitmap bitmap = profileImageHelper.getProfileImage(userId);
        if (bitmap != null) {
            ivProfileImage.setImageBitmap(bitmap);
            ivProfileImage.setVisibility(View.VISIBLE);
            tvInitials.setVisibility(View.GONE);
        } else {
            ivProfileImage.setVisibility(View.GONE);
            tvInitials.setVisibility(View.VISIBLE);
        }
    }

    private void loadSmsLimit() {
        String userId = sessionManager.getUserId();
        if (userId == null || tvSmsLimit == null) return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        Long smsLimit = documentSnapshot.getLong("smsLimit");
                        int remaining = smsLimit != null ? smsLimit.intValue() : 10;
                        tvSmsLimit.setText(remaining + " SMS");
                    }
                });
    }

    private void syncUserDataFromFirestore(String userId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || !documentSnapshot.exists()) return;

                    com.sajoldev.hisabniben.model.User user = documentSnapshot.toObject(com.sajoldev.hisabniben.model.User.class);
                    if (user != null) {
                        if (user.getStoreName() != null && !user.getStoreName().isEmpty()) {
                            tvStoreName.setText(user.getStoreName());
                        }
                        if (user.getStoreType() != null && !user.getStoreType().isEmpty()) {
                            tvStoreType.setText("🏪 " + user.getStoreType());
                        }

                        sessionManager.updatePremiumStatus(
                                user.isPremium(),
                                user.getSubscriptionExpiryDate() != null ? user.getSubscriptionExpiryDate() : 0
                        );

                        long trialEnd = user.getTrialEnd();
                        if (trialEnd > System.currentTimeMillis()) {
                            sessionManager.updateTrialStatus(trialEnd);
                        } else {
                            sessionManager.updateTrialStatus(1);
                        }

                        updateSubscriptionStatus();
                    }
                });
    }

    private void updateSubscriptionStatus() {
        if (!isAdded()) return;

        if (sessionManager.isPremium()) {
            ivStatusIcon.setImageResource(R.drawable.ic_star);
            tvSubscriptionStatus.setText("Premium User");
            tvSubscriptionDays.setText("আপনার Premium সুবিধা সক্রিয়");
            btnUpgrade.setText("প্ল্যান দেখুন");
        } else if (sessionManager.isOnTrial()) {
            ivStatusIcon.setImageResource(R.drawable.ic_star_outline);
            tvSubscriptionStatus.setText("ফ্রি ট্রায়াল (Free Trial)");
            long remainingDays = sessionManager.getRemainingTrialDays();
            tvSubscriptionDays.setText("আর " + remainingDays + " দিন বাকি");
            btnUpgrade.setText("Upgrade করুন");
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_star_outline);
            tvSubscriptionStatus.setText("ট্রায়াল শেষ");
            tvSubscriptionDays.setText("Premium চালুর জন্য আপগ্রেড করুন");
            btnUpgrade.setText("Upgrade করুন");
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("লগআউট করতে চান?")
                .setMessage("আপনি কি হিসাব নিবেন অ্যাকাউন্ট থেকে লগআউট করতে চান?")
                .setPositiveButton("লগআউট", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void showContactSupportDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Contact Support (সহায়তা)")
                .setMessage("Email: support@hisabniben.com\n\nআমাদের কাস্টমার কেয়ার টিম ২৪ ঘণ্টার মধ্যে সহায়তা করবে।")
                .setPositiveButton("ঠিক আছে", null)
                .show();
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
