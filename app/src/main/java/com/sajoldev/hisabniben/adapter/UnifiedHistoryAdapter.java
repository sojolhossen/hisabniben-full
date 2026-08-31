package com.sajoldev.hisabniben.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.UnifiedHistoryItem;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UnifiedHistoryAdapter extends RecyclerView.Adapter<UnifiedHistoryAdapter.ViewHolder> {

    private List<UnifiedHistoryItem> items;
    private OnHistoryItemClickListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM · hh:mm a", Locale.ENGLISH);

    public interface OnHistoryItemClickListener {
        void onItemClick(UnifiedHistoryItem item);
    }

    public UnifiedHistoryAdapter(List<UnifiedHistoryItem> items, OnHistoryItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<UnifiedHistoryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unified_history_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UnifiedHistoryItem item = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        holder.tvSubtitle.setText(item.getSubtitle() != null ? item.getSubtitle() : "");
        holder.tvAmount.setText(UnitConverterHelper.formatCurrency(item.getAmount()));
        holder.tvDate.setText(sdf.format(new Date(item.getDate())));

        String type = item.getType();

        if (UnifiedHistoryItem.TYPE_SALE.equals(type)) {
            holder.tvTypeBadge.setText("চাল বিক্রি");
            holder.tvTypeBadge.setTextColor(ctx.getResources().getColor(R.color.primary, null));
            holder.ivTypeIcon.setImageResource(R.drawable.ic_payment);
            holder.layoutIconCircle.setBackgroundResource(R.drawable.circle_background_primary);

            if (item.getDueAmount() > 0) {
                holder.tvDueBadge.setText("বাকি " + UnitConverterHelper.formatCurrency(item.getDueAmount()));
                holder.tvDueBadge.setTextColor(ctx.getResources().getColor(R.color.baki_color, null));
                holder.tvDueBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvDueBadge.setText("পরিশোধিত (PAID)");
                holder.tvDueBadge.setTextColor(ctx.getResources().getColor(R.color.success, null));
                holder.tvDueBadge.setVisibility(View.VISIBLE);
            }
        } else if (UnifiedHistoryItem.TYPE_PURCHASE.equals(type)) {
            holder.tvTypeBadge.setText("চাল ক্রয়");
            holder.tvTypeBadge.setTextColor(ctx.getResources().getColor(R.color.purple, null));
            holder.ivTypeIcon.setImageResource(R.drawable.ic_product);
            holder.layoutIconCircle.setBackgroundResource(R.drawable.circle_background_purple);

            if (item.getDueAmount() > 0) {
                holder.tvDueBadge.setText("পাওনা " + UnitConverterHelper.formatCurrency(item.getDueAmount()));
                holder.tvDueBadge.setTextColor(ctx.getResources().getColor(R.color.warning, null));
                holder.tvDueBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvDueBadge.setText("সম্পূর্ণ পরিশোধিত");
                holder.tvDueBadge.setTextColor(ctx.getResources().getColor(R.color.success, null));
                holder.tvDueBadge.setVisibility(View.VISIBLE);
            }
        } else if (UnifiedHistoryItem.TYPE_MONEY_RECEIVE.equals(type)) {
            holder.tvTypeBadge.setText("টাকা জমা");
            holder.tvTypeBadge.setTextColor(ctx.getResources().getColor(R.color.success, null));
            holder.ivTypeIcon.setImageResource(R.drawable.ic_money);
            holder.layoutIconCircle.setBackgroundResource(R.drawable.circle_background_success);

            holder.tvDueBadge.setText("মাধ্যম: " + (item.getPaymentMethod() != null ? item.getPaymentMethod() : "Cash"));
            holder.tvDueBadge.setTextColor(ctx.getResources().getColor(R.color.text_secondary, null));
            holder.tvDueBadge.setVisibility(View.VISIBLE);
        } else if (UnifiedHistoryItem.TYPE_EXPENSE.equals(type)) {
            holder.tvTypeBadge.setText("খরচ");
            holder.tvTypeBadge.setTextColor(ctx.getResources().getColor(R.color.error, null));
            holder.ivTypeIcon.setImageResource(R.drawable.ic_baki);
            holder.layoutIconCircle.setBackgroundResource(R.drawable.circle_background_baki);

            holder.tvDueBadge.setText("মাধ্যম: " + (item.getPaymentMethod() != null ? item.getPaymentMethod() : "Cash"));
            holder.tvDueBadge.setTextColor(ctx.getResources().getColor(R.color.text_secondary, null));
            holder.tvDueBadge.setVisibility(View.VISIBLE);
        } else if (UnifiedHistoryItem.TYPE_STOCK_MOVEMENT.equals(type)) {
            holder.tvTypeBadge.setText("স্টক পরিবর্তন");
            holder.tvTypeBadge.setTextColor(ctx.getResources().getColor(R.color.card_customers, null));
            holder.ivTypeIcon.setImageResource(R.drawable.ic_stock);
            holder.layoutIconCircle.setBackgroundResource(R.drawable.circle_background_primary);

            holder.tvDueBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout layoutIconCircle;
        ImageView ivTypeIcon;
        TextView tvTypeBadge, tvDate, tvTitle, tvSubtitle, tvDueBadge, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutIconCircle = itemView.findViewById(R.id.layoutIconCircle);
            ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            tvTypeBadge = itemView.findViewById(R.id.tvTypeBadge);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvDueBadge = itemView.findViewById(R.id.tvDueBadge);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
