package com.sajoldev.hisabniben.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Notification;
import com.sajoldev.hisabniben.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> fullList = new ArrayList<>();
    private List<Notification> filteredList = new ArrayList<>();
    private OnNotificationClickListener listener;
    private long lastReadTime = 0;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onCtaClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications) {
        this.fullList = new ArrayList<>(notifications);
        this.filteredList = new ArrayList<>(notifications);
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Notification> notifications, Context context) {
        this.fullList = new ArrayList<>(notifications);
        this.filteredList = new ArrayList<>(notifications);
        if (context != null) {
            this.lastReadTime = SessionManager.getInstance(context).getLastNotificationReadTime();
        }
        notifyDataSetChanged();
    }

    public void filterByCategory(String categoryKey) {
        filteredList.clear();
        if (categoryKey == null || categoryKey.isEmpty() || "all".equalsIgnoreCase(categoryKey) || "সব".equals(categoryKey)) {
            filteredList.addAll(fullList);
        } else {
            for (Notification n : fullList) {
                String type = n.getType() != null ? n.getType() : "announcement";
                if (matchesCategory(type, categoryKey)) {
                    filteredList.add(n);
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesCategory(String type, String categoryKey) {
        switch (categoryKey.toLowerCase()) {
            case "announcement":
            case "ঘোষণা":
                return "announcement".equalsIgnoreCase(type);
            case "feature_update":
            case "ফিচার আপডেট":
                return "feature_update".equalsIgnoreCase(type);
            case "system":
            case "সিস্টেম":
                return "system".equalsIgnoreCase(type);
            case "subscription":
            case "সাবস্ক্রিপশন":
                return "subscription".equalsIgnoreCase(type);
            case "offer":
            case "অফার":
                return "offer".equalsIgnoreCase(type);
            case "business_tip":
            case "ব্যবসায়িক টিপস":
                return "business_tip".equalsIgnoreCase(type);
            default:
                return true;
        }
    }

    public int getUnreadCount(Context context) {
        int count = 0;
        com.sajoldev.hisabniben.util.SessionManager sessionManager = com.sajoldev.hisabniben.util.SessionManager.getInstance(context);
        for (Notification n : fullList) {
            boolean isRead = n.isRead() || sessionManager.isNotificationRead(n.getId()) || (lastReadTime > 0 && n.getCreatedAt() <= lastReadTime);
            if (!isRead) {
                count++;
            }
        }
        return count;
    }

    public int getUnreadCount() {
        if (!fullList.isEmpty()) {
            return getUnreadCount(null);
        }
        return 0;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = filteredList.get(position);
        holder.bind(notification, lastReadTime, listener);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardNotification;
        private final ImageView ivIcon;
        private final TextView tvCategoryBadge, tvPriorityBadge, tvTime, tvTitle, tvMessage;
        private final View viewUnreadDot;
        private final MaterialButton btnCta;

        NotificationViewHolder(View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvPriorityBadge = itemView.findViewById(R.id.tvPriorityBadge);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            btnCta = itemView.findViewById(R.id.btnCta);
        }

        void bind(Notification notification, long lastReadTime, OnNotificationClickListener listener) {
            tvTitle.setText(notification.getTitle() != null ? notification.getTitle() : "নোটিফিকেশন");
            tvMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");

            // Unread State
            com.sajoldev.hisabniben.util.SessionManager sessionManager = com.sajoldev.hisabniben.util.SessionManager.getInstance(itemView.getContext());
            boolean isRead = notification.isRead() || sessionManager.isNotificationRead(notification.getId()) || (lastReadTime > 0 && notification.getCreatedAt() <= lastReadTime);
            boolean isUnread = !isRead;

            if (isUnread) {
                viewUnreadDot.setVisibility(View.VISIBLE);
                tvTitle.setTypeface(null, Typeface.BOLD);
                cardNotification.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.white));
            } else {
                viewUnreadDot.setVisibility(View.GONE);
                tvTitle.setTypeface(null, Typeface.NORMAL);
                cardNotification.setCardBackgroundColor(itemView.getContext().getResources().getColor(R.color.background));
            }

            // Category & Icon Mapping
            String type = notification.getType() != null ? notification.getType().toLowerCase() : "announcement";
            switch (type) {
                case "feature_update":
                case "feature":
                    tvCategoryBadge.setText("ফিচার আপডেট");
                    ivIcon.setImageResource(R.drawable.ic_star);
                    break;
                case "system":
                    tvCategoryBadge.setText("সিস্টেম");
                    ivIcon.setImageResource(R.drawable.ic_document);
                    break;
                case "subscription":
                case "payment_reminder":
                    tvCategoryBadge.setText("সাবস্ক্রিপশন / পেমেন্ট");
                    ivIcon.setImageResource(R.drawable.ic_money);
                    break;
                case "offer":
                case "promotional":
                    tvCategoryBadge.setText("অফার / প্রোমোশন");
                    ivIcon.setImageResource(R.drawable.ic_star);
                    break;
                case "business_tip":
                case "tutorial":
                    tvCategoryBadge.setText("টিউটোরিয়াল / টিপস");
                    ivIcon.setImageResource(R.drawable.ic_chart);
                    break;
                case "maintenance":
                    tvCategoryBadge.setText("মেইনটেন্যান্স");
                    ivIcon.setImageResource(R.drawable.ic_document);
                    break;
                case "security_alert":
                    tvCategoryBadge.setText("নিরাপত্তা এলার্ট");
                    ivIcon.setImageResource(R.drawable.ic_document);
                    break;
                default:
                    tvCategoryBadge.setText("ঘোষণা");
                    ivIcon.setImageResource(R.drawable.ic_notification);
                    break;
            }

            // Priority Badge
            if ("urgent".equalsIgnoreCase(notification.getPriority())) {
                tvPriorityBadge.setVisibility(View.VISIBLE);
            } else {
                tvPriorityBadge.setVisibility(View.GONE);
            }

            // Human-friendly Time Formatting
            tvTime.setText(formatHumanTime(notification.getCreatedAt()));

            // CTA Button
            String actionType = notification.getActionType();
            if (actionType != null && !actionType.isEmpty()) {
                btnCta.setVisibility(View.VISIBLE);
                if (actionType.contains("SUBSCRIPTION")) {
                    btnCta.setText("প্যাকেজ দেখুন");
                } else if (actionType.contains("SMS")) {
                    btnCta.setText("SMS কিনুন");
                } else if (actionType.contains("SALES")) {
                    btnCta.setText("চাল বিক্রি করুন");
                } else if (actionType.contains("PURCHASE")) {
                    btnCta.setText("চাল ক্রয় করুন");
                } else if (actionType.contains("REPORTS")) {
                    btnCta.setText("রিপোর্ট দেখুন");
                } else {
                    btnCta.setText("এখনই দেখুন");
                }

                btnCta.setOnClickListener(v -> {
                    if (listener != null) listener.onCtaClick(notification);
                });
            } else {
                btnCta.setVisibility(View.GONE);
            }

            cardNotification.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(notification);
            });
        }

        private String formatHumanTime(long timestamp) {
            if (timestamp <= 0) return "";

            Calendar now = Calendar.getInstance();
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(timestamp);

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", new Locale("bn", "BD"));
            String timeStr = timeFormat.format(new Date(timestamp));

            if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
                return "আজ · " + timeStr;
            }

            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            if (yesterday.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    yesterday.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
                return "গতকাল · " + timeStr;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM · hh:mm a", new Locale("bn", "BD"));
            return dateFormat.format(new Date(timestamp));
        }
    }
}