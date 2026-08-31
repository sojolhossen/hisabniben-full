package com.sajoldev.hisabniben.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.SmsBuyHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsBuyHistoryAdapter extends RecyclerView.Adapter<SmsBuyHistoryAdapter.ViewHolder> {

    private ArrayList<SmsBuyHistory> fullList = new ArrayList<>();
    private ArrayList<SmsBuyHistory> filteredList = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", new Locale("bn", "BD"));
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SmsBuyHistory history);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<SmsBuyHistory> list) {
        this.fullList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (SmsBuyHistory item : fullList) {
                boolean matchesPkg = item.getPackageName() != null && item.getPackageName().toLowerCase().contains(lowerQuery);
                boolean matchesTrx = item.getTransactionId() != null && item.getTransactionId().toLowerCase().contains(lowerQuery);
                boolean matchesMethod = item.getPaymentMethodName() != null && item.getPaymentMethodName().toLowerCase().contains(lowerQuery);
                boolean matchesStatus = item.getStatus() != null && item.getStatus().toLowerCase().contains(lowerQuery);
                if (matchesPkg || matchesTrx || matchesMethod || matchesStatus) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sms_buy_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmsBuyHistory history = filteredList.get(position);

        String pkgName = history.getPackageName() != null ? history.getPackageName() : "SMS Pack";
        holder.tvPackageName.setText(pkgName + " · " + history.getSmsCount() + " SMS");
        holder.tvPrice.setText("৳" + (int) history.getAmount());

        String method = history.getPaymentMethodName() != null ? history.getPaymentMethodName() : "bKash";
        String trx = history.getTransactionId() != null ? history.getTransactionId() : "N/A";
        holder.tvPaymentDetails.setText(method + " · TrxID: " + trx);

        if (history.getCreatedAt() > 0) {
            holder.tvDate.setText(dateFormat.format(new Date(history.getCreatedAt())));
        } else {
            holder.tvDate.setText("");
        }

        String status = history.getStatus();
        if ("approved".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("APPROVED (অনুমোদিত)");
            holder.tvStatus.setTextColor(Color.parseColor("#15803D"));
        } else if ("rejected".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("REJECTED (বাতিল)");
            holder.tvStatus.setTextColor(Color.parseColor("#DC2626"));
        } else {
            holder.tvStatus.setText("PENDING (অপেক্ষমাণ)");
            holder.tvStatus.setTextColor(Color.parseColor("#D97706"));
        }

        holder.cardItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(history);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardItem;
        TextView tvPackageName, tvPaymentDetails, tvPrice, tvStatus, tvDate;

        ViewHolder(View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.cardItem);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            tvPaymentDetails = itemView.findViewById(R.id.tvPaymentDetails);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
