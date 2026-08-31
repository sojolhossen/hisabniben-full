package com.sajoldev.hisabniben.fragment;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.activity.BuySmsActivity;
import com.sajoldev.hisabniben.activity.ReportsActivity;
import com.sajoldev.hisabniben.activity.SubscriptionActivity;
import com.sajoldev.hisabniben.activity.TransactionHistoryActivity;
import com.sajoldev.hisabniben.activity.TutorialListActivity;
import com.sajoldev.hisabniben.activity.WalletDashboardActivity;
import com.sajoldev.hisabniben.adapter.NotificationAdapter;
import com.sajoldev.hisabniben.model.Notification;
import com.sajoldev.hisabniben.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;

    private MaterialCardView cardUnreadSummary;
    private TextView tvUnreadSummaryText;
    private ChipGroup chipGroupCategory;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Notification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;
    private SessionManager sessionManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy · hh:mm a", new Locale("bn", "BD"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupCategoryChips();
        setupSwipeRefresh();
        loadNotifications();
    }

    private void initViews(View view) {
        rvNotifications = view.findViewById(R.id.rvNotifications);
        emptyView = view.findViewById(R.id.emptyView);
        progressBar = view.findViewById(R.id.progressBar);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        cardUnreadSummary = view.findViewById(R.id.cardUnreadSummary);
        tvUnreadSummaryText = view.findViewById(R.id.tvUnreadSummaryText);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
    }

    private void setupRecyclerView() {
        if (rvNotifications != null) {
            adapter = new NotificationAdapter(notifications);
            adapter.setOnNotificationClickListener(this);
            rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvNotifications.setAdapter(adapter);
        }
    }

    private void setupCategoryChips() {
        if (chipGroupCategory == null) return;

        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                adapter.filterByCategory("all");
            } else if (checkedId == R.id.chipAnnouncement) {
                adapter.filterByCategory("announcement");
            } else if (checkedId == R.id.chipFeatureUpdate) {
                adapter.filterByCategory("feature_update");
            } else if (checkedId == R.id.chipSystem) {
                adapter.filterByCategory("system");
            } else if (checkedId == R.id.chipSubscription) {
                adapter.filterByCategory("subscription");
            } else if (checkedId == R.id.chipOffer) {
                adapter.filterByCategory("offer");
            } else if (checkedId == R.id.chipBusinessTip) {
                adapter.filterByCategory("business_tip");
            } else {
                adapter.filterByCategory("all");
            }
            updateEmptyState();
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary);
            swipeRefresh.setOnRefreshListener(this::loadNotifications);
        }
    }

    public void markAllAsRead() {
        if (sessionManager != null) {
            sessionManager.setLastNotificationReadTime(System.currentTimeMillis());
            if (adapter != null) {
                adapter.updateData(notifications, requireContext());
            }
            updateUnreadSummary();
            Toast.makeText(requireContext(), "সব নোটিফিকেশন পড়া হয়েছে", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotifications() {
        showLoading(true);

        String userId = sessionManager.getUserId();

        db.collection("notification_history")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }

                    notifications.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (var doc : queryDocumentSnapshots.getDocuments()) {
                            String target = doc.getString("target");
                            List<String> targetUserIds = (List<String>) doc.get("targetUserIds");

                            boolean isForCurrentUser = "all".equals(target) || "premium".equals(target) || "trial".equals(target) || "expired".equals(target);

                            if (!isForCurrentUser && targetUserIds != null && !targetUserIds.isEmpty()) {
                                isForCurrentUser = targetUserIds.contains(userId);
                            }

                            if (isForCurrentUser) {
                                Notification notification = null;
                                try {
                                    notification = doc.toObject(Notification.class);
                                } catch (Exception e) {
                                    // Fallback manual parsing if custom deserialization fails
                                    notification = new Notification();
                                    notification.setTitle(doc.getString("title"));
                                    notification.setMessage(doc.getString("message"));
                                    notification.setType(doc.getString("type"));
                                    notification.setPriority(doc.getString("priority"));
                                    notification.setActionType(doc.getString("actionType"));
                                    notification.setActionTarget(doc.getString("actionTarget"));
                                    notification.setDeepLink(doc.getString("deepLink"));
                                    notification.setImageUrl(doc.getString("imageUrl"));
                                    if (doc.get("read") instanceof Boolean) {
                                        notification.setRead(doc.getBoolean("read"));
                                    }
                                    notification.setCreatedAt(doc.get("createdAt"));
                                }

                                if (notification != null) {
                                    notification.setId(doc.getId());
                                    if (!sessionManager.isNotificationDeleted(doc.getId())) {
                                        notifications.add(notification);
                                    }
                                }
                            }
                        }
                    }

                    if (adapter != null) {
                        adapter.updateData(notifications, requireContext());
                    }
                    updateUnreadSummary();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    if (swipeRefresh != null) {
                        swipeRefresh.setRefreshing(false);
                    }
                    updateEmptyState();
                });
    }

    public void deleteNotification(Notification notification) {
        if (notification == null || sessionManager == null) return;

        if (notification.getId() != null) {
            sessionManager.setNotificationDeleted(notification.getId());
        }

        notifications.remove(notification);

        if (adapter != null) {
            adapter.updateData(notifications, requireContext());
        }

        updateUnreadSummary();
        updateEmptyState();
        Toast.makeText(requireContext(), "নোটিফিকেশনটি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
    }

    private void updateUnreadSummary() {
        if (adapter == null || tvUnreadSummaryText == null) return;
        int unread = adapter.getUnreadCount();

        if (unread > 0) {
            tvUnreadSummaryText.setText("আপনার " + unread + "টি নতুন নোটিফিকেশন আছে");
            tvUnreadSummaryText.setTextColor(getResources().getColor(R.color.primary));
        } else {
            tvUnreadSummaryText.setText("সব নোটিফিকেশন পড়া হয়েছে");
            tvUnreadSummaryText.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }

    @Override
    public void onNotificationClick(Notification notification) {
        if (notification != null && sessionManager != null) {
            notification.setRead(true);
            sessionManager.setNotificationRead(notification.getId());
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            updateUnreadSummary();
        }
        showNotificationDetails(notification);
    }

    @Override
    public void onCtaClick(Notification notification) {
        if (notification != null && sessionManager != null) {
            notification.setRead(true);
            sessionManager.setNotificationRead(notification.getId());
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            updateUnreadSummary();
        }
        handleDeepLink(notification);
    }

    private void showNotificationDetails(Notification notification) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_details, null);
        dialog.setContentView(dialogView);

        TextView tvCategoryBadge = dialogView.findViewById(R.id.tvCategoryBadge);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        ImageView ivImage = dialogView.findViewById(R.id.ivImage);

        MaterialButton btnCta = dialogView.findViewById(R.id.btnCta);
        MaterialButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        String type = notification.getType() != null ? notification.getType().toLowerCase() : "announcement";
        switch (type) {
            case "feature_update":
                tvCategoryBadge.setText("ফিচার আপডেট");
                break;
            case "system":
                tvCategoryBadge.setText("সিস্টেম");
                break;
            case "subscription":
                tvCategoryBadge.setText("সাবস্ক্রিপশন");
                break;
            case "offer":
                tvCategoryBadge.setText("অফার");
                break;
            case "business_tip":
                tvCategoryBadge.setText("ব্যবসায়িক টিপস");
                break;
            default:
                tvCategoryBadge.setText("ঘোষণা");
                break;
        }

        tvTitle.setText(notification.getTitle() != null ? notification.getTitle() : "নোটিফিকেশন");
        tvMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");
        tvDate.setText(notification.getCreatedAt() > 0 ? dateFormat.format(new Date(notification.getCreatedAt())) : "");

        if (notification.getImageUrl() != null && !notification.getImageUrl().isEmpty()) {
            ivImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(notification.getImageUrl()).into(ivImage);
        } else {
            ivImage.setVisibility(View.GONE);
        }

        String actionType = notification.getActionType();
        if (actionType != null && !actionType.isEmpty()) {
            btnCta.setVisibility(View.VISIBLE);
            btnCta.setOnClickListener(v -> {
                dialog.dismiss();
                handleDeepLink(notification);
            });
        } else {
            btnCta.setVisibility(View.GONE);
        }

        MaterialButton btnDeleteNotification = dialogView.findViewById(R.id.btnDeleteNotification);
        if (btnDeleteNotification != null) {
            btnDeleteNotification.setOnClickListener(v -> {
                dialog.dismiss();
                deleteNotification(notification);
            });
        }

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void handleDeepLink(Notification notification) {
        String actionType = notification.getActionType();
        if (actionType == null) return;

        try {
            if (actionType.contains("SUBSCRIPTION")) {
                startActivity(new Intent(requireContext(), SubscriptionActivity.class));
            } else if (actionType.contains("SMS")) {
                startActivity(new Intent(requireContext(), BuySmsActivity.class));
            } else if (actionType.contains("REPORTS")) {
                startActivity(new Intent(requireContext(), ReportsActivity.class));
            } else if (actionType.contains("TUTORIAL")) {
                startActivity(new Intent(requireContext(), TutorialListActivity.class));
            } else if (actionType.contains("WALLET")) {
                startActivity(new Intent(requireContext(), WalletDashboardActivity.class));
            } else if (actionType.contains("TRANSACTION") || actionType.contains("SALES") || actionType.contains("PURCHASE") || actionType.contains("STOCK") || actionType.contains("CUSTOMER")) {
                startActivity(new Intent(requireContext(), TransactionHistoryActivity.class));
            } else if (actionType.contains("URL") && notification.getDeepLink() != null && !notification.getDeepLink().isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(notification.getDeepLink()));
                startActivity(browserIntent);
            } else if (notification.getActionTarget() != null && notification.getActionTarget().startsWith("http")) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(notification.getActionTarget()));
                startActivity(browserIntent);
            } else {
                Toast.makeText(requireContext(), notification.getTitle(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), notification.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter == null || adapter.getItemCount() == 0;
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (rvNotifications != null) {
            rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}