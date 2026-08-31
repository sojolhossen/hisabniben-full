package com.sajoldev.hisabniben.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sajoldev.hisabniben.MainActivity;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.activity.NotificationActivity;
import com.sajoldev.hisabniben.activity.BuySmsActivity;
import com.sajoldev.hisabniben.activity.SubscriptionActivity;
import com.sajoldev.hisabniben.dialog.AddExpenseDialog;
import com.sajoldev.hisabniben.dialog.AddPurchaseDialog;
import com.sajoldev.hisabniben.dialog.AddRiceProductDialog;
import com.sajoldev.hisabniben.dialog.AddSaleDialog;
import com.sajoldev.hisabniben.dialog.AddSupplierDialog;
import com.sajoldev.hisabniben.dialog.AddTransactionDialog;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.PurchaseItem;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.SaleItem;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.model.User;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DashboardFragment extends Fragment {

    public static final String FILTER_TODAY = "TODAY";
    public static final String FILTER_THIS_WEEK = "THIS_WEEK";
    public static final String FILTER_THIS_MONTH = "THIS_MONTH";

    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvGreeting, tvBusinessName, tvSmsBalance, tvNotificationBadge, tvSelectedDatePeriod;
    private TextView tvPrimarySalesTitle, tvPrimarySalesAmount, tvSalesComparison;
    private TextView tvPurchasesTitle, tvTodayPurchases, tvCollectionsTitle, tvTodayCollections, tvDuesTitle,
            tvTodayDues, tvProfitTitle, tvTodayNetProfit;
    private TextView tvCustomerReceivable, tvSupplierPayable;
    private TextView tvStockTotalKg, tvStockTotalBags, tvStockTotalValue, tvStockVarietiesCount;
    private TextView tvLowStockMessage, tvNoTransactions;
    private LinearLayout layoutSmsBalance, layoutLowStockAlerts, containerRecentTransactions;
    private ImageView ivNotification, ivBusinessAvatar;
    private ChipGroup chipGroupPeriod;
    private MaterialCardView cardCustomerReceivable, cardSupplierPayable, cardRiceStockOverview;
    private MaterialButton btnQuickSale, btnQuickPurchase, btnQuickCollection, btnQuickSupplierPayment, btnQuickExpense,
            btnQuickAddRice;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private String currentPeriodFilter = FILTER_TODAY;
    private long lastFetchTime = 0;

    private LinearLayout layoutHeaderSubscription, layoutHeaderWarningBanner;
    private TextView tvHeaderPackageBadge, tvHeaderRemainingDays, btnHeaderRenewPackage, tvHeaderWarningText;
    private View cardVideoTutorialBanner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupListeners();
        loadUserData();
        loadDashboardData();
        updateNotificationBadge();
        updateSubscriptionHeaderStatus();
        checkTutorialBannerVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationBadge();
        updateSubscriptionHeaderStatus();
        checkTutorialBannerVisibility();
        
        // Cache optimization: Only auto-reload data if >30 seconds have elapsed
        if (System.currentTimeMillis() - lastFetchTime > 30000) {
            loadDashboardData();
        }
    }

    private void checkTutorialBannerVisibility() {
        if (cardVideoTutorialBanner == null || sessionManager == null)
            return;

        FirestoreManager.getInstance().getPublishedTutorialVideos(
                new FirestoreManager.FirestoreListCallback<com.sajoldev.hisabniben.model.TutorialVideo>() {
                    @Override
                    public void onSuccess(List<com.sajoldev.hisabniben.model.TutorialVideo> result) {
                        if (getContext() == null || !isAdded())
                            return;

                        boolean hasUnwatchedVideo = false;
                        if (result != null && !result.isEmpty()) {
                            for (com.sajoldev.hisabniben.model.TutorialVideo video : result) {
                                if (video != null && video.getId() != null
                                        && !sessionManager.isVideoWatched(video.getId())) {
                                    hasUnwatchedVideo = true;
                                    break;
                                }
                            }
                        }

                        cardVideoTutorialBanner.setVisibility(hasUnwatchedVideo ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(String error) {
                    }
                });
    }

    private void updateSubscriptionHeaderStatus() {
        if (tvHeaderPackageBadge == null || sessionManager == null)
            return;

        boolean isPremium = sessionManager.isPremium();
        boolean isOnTrial = sessionManager.isOnTrial();
        long remainingDays = 0;
        String packageName = "ফ্রি ট্রায়াল";

        if (isPremium) {
            packageName = sessionManager.getSubscriptionPackageName();
            if (packageName == null || packageName.isEmpty())
                packageName = "প্রিমিয়াম প্যাকেজ";
            remainingDays = sessionManager.getRemainingSubscriptionDays();
            tvHeaderPackageBadge.setText("👑 " + packageName);
            tvHeaderPackageBadge.setBackgroundResource(R.drawable.bg_badge_success);
        } else if (isOnTrial) {
            packageName = "7 দিনের ট্রায়াল";
            remainingDays = sessionManager.getRemainingTrialDays();
            tvHeaderPackageBadge.setText("⏳ " + packageName);
            tvHeaderPackageBadge.setBackgroundResource(R.drawable.bg_badge_warning);
        } else {
            packageName = "মেয়াদউত্তীর্ণ (Expired)";
            remainingDays = 0;
            tvHeaderPackageBadge.setText("⚠️ " + packageName);
            tvHeaderPackageBadge.setBackgroundResource(R.drawable.bg_badge_error);
        }

        if (isPremium && remainingDays >= 999) {
            tvHeaderRemainingDays.setText("Life-Time");
            if (layoutHeaderWarningBanner != null) {
                layoutHeaderWarningBanner.setVisibility(View.GONE);
            }
        } else if (isPremium || isOnTrial) {
            tvHeaderRemainingDays.setText("মেয়াদ বাকি: " + remainingDays + " দিন");
            if (layoutHeaderWarningBanner != null && tvHeaderWarningText != null) {
                if (remainingDays <= 5) {
                    layoutHeaderWarningBanner.setVisibility(View.VISIBLE);
                    tvHeaderWarningText.setText("⚠️ আপনার " + packageName + " এর মেয়াদ আর " + remainingDays
                            + " দিন পর শেষ হবে! এখনই রিনিউ করুন।");
                } else {
                    layoutHeaderWarningBanner.setVisibility(View.GONE);
                }
            }
        } else {
            tvHeaderRemainingDays.setText("আপনার প্যাকেজের মেয়াদ শেষ!");
            if (layoutHeaderWarningBanner != null && tvHeaderWarningText != null) {
                layoutHeaderWarningBanner.setVisibility(View.VISIBLE);
                tvHeaderWarningText.setText(
                        "⚠️ আপনার ট্রায়াল/প্যাকেজের মেয়াদ শেষ হয়ে গেছে! অ্যাপের সকল ফিচার চালু রাখতে প্যাকেজ বেছে নিন।");
            }
        }
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge == null || sessionManager == null || db == null)
            return;

        String userId = sessionManager.getUserId();
        long lastReadTime = sessionManager.getLastNotificationReadTime();

        db.collection("notification_history")
                .limit(20)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (getContext() == null || !isAdded())
                        return;

                    int unreadCount = 0;
                    if (!snapshots.isEmpty()) {
                        for (var doc : snapshots.getDocuments()) {
                            String target = doc.getString("target");
                            java.util.List<String> targetUserIds = (java.util.List<String>) doc.get("targetUserIds");

                            boolean isForUser = "all".equals(target) || "premium".equals(target)
                                    || "trial".equals(target) || "expired".equals(target);
                            if (!isForUser && targetUserIds != null) {
                                isForUser = targetUserIds.contains(userId);
                            }

                            if (isForUser) {
                                String notifId = doc.getId();
                                boolean isDeleted = sessionManager.isNotificationDeleted(notifId);
                                if (!isDeleted) {
                                    boolean isRead = sessionManager.isNotificationRead(notifId);
                                    Long createdAt = com.sajoldev.hisabniben.util.FirestoreUtil.getLongOrTimestamp(doc,
                                            "createdAt");
                                    if (!isRead && (createdAt == null || createdAt > lastReadTime)) {
                                        unreadCount++;
                                    }
                                }
                            }
                        }
                    }

                    if (unreadCount > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvNotificationBadge != null)
                        tvNotificationBadge.setVisibility(View.GONE);
                });
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);

        ivBusinessAvatar = view.findViewById(R.id.ivBusinessAvatar);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvBusinessName = view.findViewById(R.id.tvBusinessName);
        layoutSmsBalance = view.findViewById(R.id.layoutSmsBalance);
        tvSmsBalance = view.findViewById(R.id.tvSmsBalance);
        ivNotification = view.findViewById(R.id.ivNotification);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
        chipGroupPeriod = view.findViewById(R.id.chipGroupPeriod);
        tvSelectedDatePeriod = view.findViewById(R.id.tvSelectedDatePeriod);

        tvPrimarySalesTitle = view.findViewById(R.id.tvPrimarySalesTitle);
        tvPrimarySalesAmount = view.findViewById(R.id.tvPrimarySalesAmount);
        tvSalesComparison = view.findViewById(R.id.tvSalesComparison);

        tvPurchasesTitle = view.findViewById(R.id.tvPurchasesTitle);
        tvTodayPurchases = view.findViewById(R.id.tvTodayPurchases);
        tvCollectionsTitle = view.findViewById(R.id.tvCollectionsTitle);
        tvTodayCollections = view.findViewById(R.id.tvTodayCollections);
        tvDuesTitle = view.findViewById(R.id.tvDuesTitle);
        tvTodayDues = view.findViewById(R.id.tvTodayDues);
        tvProfitTitle = view.findViewById(R.id.tvProfitTitle);
        tvTodayNetProfit = view.findViewById(R.id.tvTodayNetProfit);

        cardCustomerReceivable = view.findViewById(R.id.cardCustomerReceivable);
        tvCustomerReceivable = view.findViewById(R.id.tvCustomerReceivable);
        cardSupplierPayable = view.findViewById(R.id.cardSupplierPayable);
        tvSupplierPayable = view.findViewById(R.id.tvSupplierPayable);

        cardRiceStockOverview = view.findViewById(R.id.cardRiceStockOverview);
        tvStockTotalKg = view.findViewById(R.id.tvStockTotalKg);
        tvStockTotalBags = view.findViewById(R.id.tvStockTotalBags);
        tvStockTotalValue = view.findViewById(R.id.tvStockTotalValue);
        tvStockVarietiesCount = view.findViewById(R.id.tvStockVarietiesCount);

        layoutLowStockAlerts = view.findViewById(R.id.layoutLowStockAlerts);
        tvLowStockMessage = view.findViewById(R.id.tvLowStockMessage);

        btnQuickSale = view.findViewById(R.id.btnQuickSale);
        btnQuickPurchase = view.findViewById(R.id.btnQuickPurchase);
        btnQuickCollection = view.findViewById(R.id.btnQuickCollection);
        btnQuickSupplierPayment = view.findViewById(R.id.btnQuickSupplierPayment);
        btnQuickExpense = view.findViewById(R.id.btnQuickExpense);
        btnQuickAddRice = view.findViewById(R.id.btnQuickAddRice);

        layoutHeaderSubscription = view.findViewById(R.id.layoutHeaderSubscription);
        tvHeaderPackageBadge = view.findViewById(R.id.tvHeaderPackageBadge);
        tvHeaderRemainingDays = view.findViewById(R.id.tvHeaderRemainingDays);
        btnHeaderRenewPackage = view.findViewById(R.id.btnHeaderRenewPackage);
        layoutHeaderWarningBanner = view.findViewById(R.id.layoutHeaderWarningBanner);
        tvHeaderWarningText = view.findViewById(R.id.tvHeaderWarningText);

        cardVideoTutorialBanner = view.findViewById(R.id.cardVideoTutorialBanner);

        containerRecentTransactions = view.findViewById(R.id.containerRecentTransactions);
        tvNoTransactions = view.findViewById(R.id.tvNoTransactions);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);

        if (btnHeaderRenewPackage != null) {
            btnHeaderRenewPackage.setOnClickListener(v -> {
                if (getContext() != null) {
                    startActivity(new Intent(getContext(), SubscriptionActivity.class));
                }
            });
        }

        if (getView() != null) {
            View btnOpenTutorialsBanner = getView().findViewById(R.id.btnOpenTutorialsBanner);
            View.OnClickListener openTutorialsListener = v -> {
                if (getContext() != null) {
                    startActivity(
                            new Intent(getContext(), com.sajoldev.hisabniben.activity.TutorialListActivity.class));
                }
            };
            if (cardVideoTutorialBanner != null)
                cardVideoTutorialBanner.setOnClickListener(openTutorialsListener);
            if (btnOpenTutorialsBanner != null)
                btnOpenTutorialsBanner.setOnClickListener(openTutorialsListener);
        }

        if (getView() != null) {
            View btnOpenDrawer = getView().findViewById(R.id.btnOpenDrawer);
            if (btnOpenDrawer != null) {
                btnOpenDrawer.setOnClickListener(v -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openDrawer();
                    }
                });
            }
        }

        if (ivBusinessAvatar != null) {
            ivBusinessAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openDrawer();
                }
            });
        }

        layoutSmsBalance.setOnClickListener(v -> {
            if (getContext() != null)
                startActivity(new Intent(getContext(), BuySmsActivity.class));
        });
        ivNotification.setOnClickListener(v -> {
            if (getContext() != null)
                startActivity(new Intent(getContext(), NotificationActivity.class));
        });

        chipGroupPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipThisWeek) {
                currentPeriodFilter = FILTER_THIS_WEEK;
            } else if (checkedId == R.id.chipThisMonth) {
                currentPeriodFilter = FILTER_THIS_MONTH;
            } else {
                currentPeriodFilter = FILTER_TODAY;
            }
            updatePeriodTitles();
            loadDashboardData();
        });

        // Quick Actions with SubscriptionGuard
        btnQuickSale.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;
            com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(context, () -> {
                if (!isAdded())
                    return;
                AddSaleDialog dialog = new AddSaleDialog();
                dialog.setOnSaleSavedListener(this::loadDashboardData);
                dialog.show(getChildFragmentManager(), "AddSaleFromDash");
            });
        });

        btnQuickPurchase.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;
            com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(context, () -> {
                if (!isAdded())
                    return;
                AddPurchaseDialog dialog = new AddPurchaseDialog();
                dialog.setOnPurchaseSavedListener(this::loadDashboardData);
                dialog.show(getChildFragmentManager(), "AddPurchaseFromDash");
            });
        });

        btnQuickCollection.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;
            com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(context, () -> {
                if (!isAdded())
                    return;
                AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_RECEIVE);
                dialog.setOnTransactionSavedListener(this::loadDashboardData);
                dialog.show(getChildFragmentManager(), "AddReceiveFromDash");
            });
        });

        btnQuickSupplierPayment.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;
            com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(context, () -> {
                if (!isAdded())
                    return;
                AddSupplierDialog dialog = new AddSupplierDialog();
                dialog.setOnSupplierSavedListener(this::loadDashboardData);
                dialog.show(getChildFragmentManager(), "AddSupplierFromDash");
            });
        });

        btnQuickExpense.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;
            com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(context, () -> {
                if (!isAdded())
                    return;
                AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_EXPENSE);
                dialog.setOnTransactionSavedListener(this::loadDashboardData);
                dialog.show(getChildFragmentManager(), "AddExpenseFromDash");
            });
        });

        btnQuickAddRice.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null)
                return;
            com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(context, () -> {
                if (!isAdded())
                    return;
                AddRiceProductDialog dialog = new AddRiceProductDialog();
                dialog.setOnProductSavedListener(this::loadDashboardData);
                dialog.show(getChildFragmentManager(), "AddRiceProductFromDash");
            });
        });

        cardRiceStockOverview.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_stock);
            }
        });

        if (getView() != null) {
            View cardWalletDashboard = getView().findViewById(R.id.cardWalletDashboard);
            TextView tvOpenWalletDashboard = getView().findViewById(R.id.tvOpenWalletDashboard);

            View.OnClickListener openWalletListener = v -> {
                Context context = getContext();
                if (context == null)
                    return;
                com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(context, () -> {
                    if (getContext() != null) {
                        startActivity(new Intent(getContext(),
                                com.sajoldev.hisabniben.activity.WalletDashboardActivity.class));
                    }
                });
            };
            if (cardWalletDashboard != null)
                cardWalletDashboard.setOnClickListener(openWalletListener);
            if (tvOpenWalletDashboard != null)
                tvOpenWalletDashboard.setOnClickListener(openWalletListener);
        }
    }

    private void updatePeriodTitles() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        String dateStr = sdf.format(new Date());

        if (FILTER_THIS_WEEK.equals(currentPeriodFilter)) {
            tvSelectedDatePeriod.setText("এই সপ্তাহের হিসাব");
            tvPrimarySalesTitle.setText("এই সপ্তাহের চাল বিক্রি (Sales)");
            tvPurchasesTitle.setText("এই সপ্তাহের ক্রয়");
            tvCollectionsTitle.setText("সপ্তাহে জমা");
            tvDuesTitle.setText("সপ্তাহের বাকি");
            tvProfitTitle.setText("সপ্তাহের নিট লাভ");
        } else if (FILTER_THIS_MONTH.equals(currentPeriodFilter)) {
            tvSelectedDatePeriod.setText("এই মাসের হিসাব");
            tvPrimarySalesTitle.setText("এই মাসের চাল বিক্রি (Sales)");
            tvPurchasesTitle.setText("এই মাসের ক্রয়");
            tvCollectionsTitle.setText("মাসে জমা");
            tvDuesTitle.setText("মাসের বাকি");
            tvProfitTitle.setText("মাসের নিট লাভ");
        } else {
            tvSelectedDatePeriod.setText("আজ, " + dateStr);
            tvPrimarySalesTitle.setText("আজকের চাল বিক্রি (Sales)");
            tvPurchasesTitle.setText("আজকের ক্রয়");
            tvCollectionsTitle.setText("আজ জমা");
            tvDuesTitle.setText("আজকের বাকি");
            tvProfitTitle.setText("আজকের নিট লাভ");
        }
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        FirestoreManager.getInstance().getUser(userId, new FirestoreManager.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded() || getContext() == null)
                    return;
                if (user != null) {
                    sessionManager.updatePremiumStatus(
                        user.isPremium(),
                        user.getSubscriptionExpiryDate() != null ? user.getSubscriptionExpiryDate() : 0
                    );
                    if (user.getSubscriptionPackageName() != null && !user.getSubscriptionPackageName().isEmpty()) {
                        sessionManager.setSubscriptionPackageName(user.getSubscriptionPackageName());
                    }
                    updateSubscriptionHeaderStatus();

                    if (user.getShopName() != null && !user.getShopName().isEmpty()) {
                        tvBusinessName.setText(user.getShopName());
                    }
                    if (user.getName() != null && !user.getName().isEmpty()) {
                        tvGreeting.setText("আসসালামু আলাইকুম, " + user.getName() + " ভাই");
                    }
                    tvSmsBalance.setText(user.getSmsBalance() + " SMS");
                }
            }

            @Override
            public void onFailure(String error) {
            }
        });
    }

    private void loadDashboardData() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        lastFetchTime = System.currentTimeMillis();
        loadWalletSummaryData();

        long startTimestamp = getPeriodStartTimestamp(currentPeriodFilter);

        if (progressBar != null && !swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.GONE);
        }

        // 1. Aggregates for Sales (with Timestamp query optimization)
        db.collection("sales")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(salesSnap -> {
                    if (!isAdded() || getContext() == null)
                        return;
                    double totalSales = 0;
                    double totalSalesDues = 0;
                    double totalCogs = 0;
                    double totalSalesKg = 0;

                    for (QueryDocumentSnapshot doc : salesSnap) {
                        Sale sale = doc.toObject(Sale.class);
                        if (sale != null && !Sale.SALE_STATUS_CANCELLED.equals(sale.getSaleStatus())) {
                            long sDate = sale.getSaleDate() > 0 ? sale.getSaleDate() : sale.getCreatedAt();
                            if (sDate >= startTimestamp) {
                                totalSales += sale.getGrandTotal();
                                totalSalesDues += sale.getDueAmount();

                                if (sale.getItems() != null) {
                                    for (SaleItem item : sale.getItems()) {
                                        totalSalesKg += item.getTotalKg();
                                        totalCogs += item.getTotalKg() * item.getCostPerKg();
                                    }
                                }
                            }
                        }
                    }

                    final double salesSum = totalSales;
                    final double salesDuesSum = totalSalesDues;
                    final double cogsSum = totalCogs;
                    final double salesKgSum = totalSalesKg;

                    tvPrimarySalesAmount.setText(UnitConverterHelper.formatCurrency(salesSum));
                    tvSalesComparison.setText(UnitConverterHelper.formatKg(salesKgSum) + " চাল বিক্রি হয়েছে");
                    tvTodayDues.setText(UnitConverterHelper.formatCurrency(salesDuesSum));

                    // 2. Aggregates for Purchases
                    db.collection("purchases")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(purchasesSnap -> {
                                if (!isAdded() || getContext() == null)
                                    return;
                                double totalPurchases = 0;
                                for (QueryDocumentSnapshot doc : purchasesSnap) {
                                    Purchase p = doc.toObject(Purchase.class);
                                    if (p != null
                                            && !Purchase.PURCHASE_STATUS_CANCELLED.equals(p.getPurchaseStatus())) {
                                        long pDate = p.getPurchaseDate() > 0 ? p.getPurchaseDate() : p.getCreatedAt();
                                        if (pDate >= startTimestamp) {
                                            totalPurchases += p.getGrandTotal();
                                        }
                                    }
                                }
                                tvTodayPurchases.setText(UnitConverterHelper.formatCurrency(totalPurchases));

                                // 3. Expenses Aggregation
                                db.collection("expenses")
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(expensesSnap -> {
                                            if (!isAdded() || getContext() == null)
                                                return;
                                            double totalExpenses = 0;
                                            for (QueryDocumentSnapshot doc : expensesSnap) {
                                                Expense exp = doc.toObject(Expense.class);
                                                if (exp != null) {
                                                    long eDate = exp.getDate() > 0 ? exp.getDate() : exp.getCreatedAt();
                                                    if (eDate >= startTimestamp) {
                                                        totalExpenses += exp.getAmount();
                                                    }
                                                }
                                            }

                                            double netProfit = salesSum - cogsSum - totalExpenses;
                                            tvTodayNetProfit.setText(UnitConverterHelper.formatCurrency(netProfit));
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null)
                        return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                });

        // 4. Customer Receivables & Collections
        FirestoreManager.getInstance().getCustomersByUser(userId,
                new FirestoreManager.FirestoreListCallback<Customer>() {
                    @Override
                    public void onSuccess(List<Customer> customers) {
                        if (!isAdded() || getContext() == null)
                            return;
                        double receivable = 0;
                        double collections = 0;
                        for (Customer c : customers) {
                            if (c.getBaki() > 0)
                                receivable += c.getBaki();
                            else if (c.getCurrentBalance() > 0)
                                receivable += c.getCurrentBalance();
                            collections += c.getTotalPaid();
                        }
                        tvCustomerReceivable.setText(UnitConverterHelper.formatCurrency(receivable));
                        tvTodayCollections.setText(UnitConverterHelper.formatCurrency(collections));
                    }

                    @Override
                    public void onFailure(String error) {
                    }
                });

        // 5. Supplier Payables
        FirestoreManager.getInstance().getSuppliersByUser(userId,
                new FirestoreManager.FirestoreListCallback<Supplier>() {
                    @Override
                    public void onSuccess(List<Supplier> suppliers) {
                        if (!isAdded() || getContext() == null)
                            return;
                        double payable = 0;
                        for (Supplier s : suppliers) {
                            payable += s.getCurrentPayable();
                        }
                        tvSupplierPayable.setText(UnitConverterHelper.formatCurrency(payable));
                    }

                    @Override
                    public void onFailure(String error) {
                    }
                });

        // 6. Rice Products & Stock Overview
        FirestoreManager.getInstance().getRiceProductsByUser(userId,
                new FirestoreManager.FirestoreListCallback<RiceProduct>() {
                    @Override
                    public void onSuccess(List<RiceProduct> products) {
                        if (!isAdded() || getContext() == null)
                            return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        double totalStockKg = 0;
                        double totalStockValue = 0;
                        StringBuilder lowStockAlerts = new StringBuilder();
                        int lowStockCount = 0;

                        for (RiceProduct p : products) {
                            totalStockKg += p.getCurrentStockKg();
                            totalStockValue += p.getCurrentStockKg()
                                    * (p.getPurchaseRatePerKg() > 0 ? p.getPurchaseRatePerKg() : p.getSaleRatePerKg());

                            if (p.getCurrentStockKg() <= p.getMinStockAlertKg()) {
                                lowStockCount++;
                                lowStockAlerts.append("⚠️ ").append(p.getName()).append(" — ")
                                        .append(UnitConverterHelper.formatKg(p.getCurrentStockKg())).append(" বাকি\n");
                            }
                        }

                        tvStockTotalKg.setText(UnitConverterHelper.formatKg(totalStockKg));
                        tvStockTotalBags.setText(UnitConverterHelper.formatStockBagsAndKg(totalStockKg, 50.0));
                        tvStockTotalValue.setText(UnitConverterHelper.formatCurrency(totalStockValue));
                        tvStockVarietiesCount.setText(products.size() + " ধরনের চাল");

                        if (lowStockCount > 0) {
                            tvLowStockMessage.setText(lowStockAlerts.toString().trim());
                            if (isAdded() && getContext() != null) {
                                tvLowStockMessage.setTextColor(getContext().getResources().getColor(R.color.error));
                            }
                        } else {
                            tvLowStockMessage.setText("সব চালের স্টক পর্যাপ্ত আছে");
                            if (isAdded() && getContext() != null) {
                                tvLowStockMessage
                                        .setTextColor(getContext().getResources().getColor(R.color.text_secondary));
                            }
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        if (!isAdded() || getContext() == null)
                            return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                    }
                });

        // 7. Optimized Recent Transactions (limit query)
        loadRecentTransactions(userId);
    }

    private void loadRecentTransactions(String userId) {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || getContext() == null)
                        return;
                    
                    List<Transaction> txList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Transaction tx = doc.toObject(Transaction.class);
                        if (tx != null)
                            txList.add(tx);
                    }

                    txList.sort((t1, t2) -> Long.compare(t2.getDate(), t1.getDate()));

                    containerRecentTransactions.removeAllViews();
                    if (txList.isEmpty()) {
                        containerRecentTransactions.addView(tvNoTransactions);
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH);
                        int limit = Math.min(5, txList.size());
                        for (int i = 0; i < limit; i++) {
                            Transaction tx = txList.get(i);
                            if (getContext() == null)
                                break;
                            View row = LayoutInflater.from(getContext()).inflate(
                                    R.layout.item_stock_movement_timeline_row, containerRecentTransactions, false);
                            TextView tvReason = row.findViewById(R.id.tvMovementReason);
                            TextView tvDate = row.findViewById(R.id.tvMovementDate);
                            TextView tvQty = row.findViewById(R.id.tvMovementQty);

                            tvReason.setText(
                                    (tx.getCustomerName() != null ? tx.getCustomerName() + " — " : "") + tx.getType());
                            tvDate.setText(sdf.format(new Date(tx.getDate())));
                            tvQty.setText(UnitConverterHelper.formatCurrency(tx.getAmount()));
                            if (isAdded() && getContext() != null) {
                                tvQty.setTextColor(getContext().getResources()
                                        .getColor("DUE".equals(tx.getType()) ? R.color.baki_color : R.color.success));
                            }

                            containerRecentTransactions.addView(row);
                        }
                    }
                });
    }

    private long getPeriodStartTimestamp(String periodFilter) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (FILTER_THIS_WEEK.equals(periodFilter)) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
        } else if (FILTER_THIS_MONTH.equals(periodFilter)) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    private void loadWalletSummaryData() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        FirestoreManager.getInstance().getWalletAccounts(userId,
                new FirestoreManager.FirestoreListCallback<com.sajoldev.hisabniben.model.WalletAccount>() {
                    @Override
                    public void onSuccess(List<com.sajoldev.hisabniben.model.WalletAccount> accounts) {
                        if (!isAdded() || getContext() == null || getView() == null)
                            return;
                        double total = 0;
                        double cash = 0;
                        double bkash = 0;
                        double nagad = 0;
                        double bank = 0;

                        for (com.sajoldev.hisabniben.model.WalletAccount acc : accounts) {
                            if (acc.isActive()) {
                                total += acc.getCurrentBalance();
                                String type = acc.getAccountType() != null ? acc.getAccountType().toUpperCase()
                                        : "CASH";
                                switch (type) {
                                    case com.sajoldev.hisabniben.model.WalletAccount.TYPE_BKASH:
                                        bkash += acc.getCurrentBalance();
                                        break;
                                    case com.sajoldev.hisabniben.model.WalletAccount.TYPE_NAGAD:
                                        nagad += acc.getCurrentBalance();
                                        break;
                                    case com.sajoldev.hisabniben.model.WalletAccount.TYPE_BANK:
                                        bank += acc.getCurrentBalance();
                                        break;
                                    default:
                                        cash += acc.getCurrentBalance();
                                        break;
                                }
                            }
                        }

                        TextView tvDashTotalWalletMoney = getView().findViewById(R.id.tvDashTotalWalletMoney);
                        TextView tvDashCashBalance = getView().findViewById(R.id.tvDashCashBalance);
                        TextView tvDashBkashBalance = getView().findViewById(R.id.tvDashBkashBalance);
                        TextView tvDashNagadBalance = getView().findViewById(R.id.tvDashNagadBalance);
                        TextView tvDashBankBalance = getView().findViewById(R.id.tvDashBankBalance);

                        if (tvDashTotalWalletMoney != null)
                            tvDashTotalWalletMoney
                                    .setText(com.sajoldev.hisabniben.util.UnitConverterHelper.formatCurrency(total));
                        if (tvDashCashBalance != null)
                            tvDashCashBalance
                                    .setText(com.sajoldev.hisabniben.util.UnitConverterHelper.formatCurrency(cash));
                        if (tvDashBkashBalance != null)
                            tvDashBkashBalance
                                    .setText(com.sajoldev.hisabniben.util.UnitConverterHelper.formatCurrency(bkash));
                        if (tvDashNagadBalance != null)
                            tvDashNagadBalance
                                    .setText(com.sajoldev.hisabniben.util.UnitConverterHelper.formatCurrency(nagad));
                        if (tvDashBankBalance != null)
                            tvDashBankBalance
                                    .setText(com.sajoldev.hisabniben.util.UnitConverterHelper.formatCurrency(bank));
                    }

                    @Override
                    public void onFailure(String error) {
                    }
                });
    }
}
