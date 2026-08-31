package com.sajoldev.hisabniben.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.SmsHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsHistoryAdapter extends RecyclerView.Adapter<SmsHistoryAdapter.ViewHolder> {

    private ArrayList<SmsHistory> fullList = new ArrayList<>();
    private ArrayList<SmsHistory> filteredList = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a", new Locale("bn", "BD"));
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SmsHistory sms);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<SmsHistory> list) {
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
            for (SmsHistory item : fullList) {
                boolean matchesName = item.getCustomerName() != null && item.getCustomerName().toLowerCase().contains(lowerQuery);
                boolean matchesPhone = item.getCustomerPhone() != null && item.getCustomerPhone().contains(lowerQuery);
                boolean matchesMsg = item.getMessage() != null && item.getMessage().toLowerCase().contains(lowerQuery);
                if (matchesName || matchesPhone || matchesMsg) {
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
                .inflate(R.layout.item_sms_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmsHistory sms = filteredList.get(position);

        String name = sms.getCustomerName();
        if (name == null || name.trim().isEmpty()) {
            name = "ব্যবসায়িক কাস্টমার";
        }
        holder.tvCustomerName.setText(name);
        holder.tvPhone.setText(sms.getCustomerPhone() != null ? sms.getCustomerPhone() : "");
        holder.tvMessage.setText(sms.getMessage() != null ? sms.getMessage() : "");

        if (sms.getTimestamp() > 0) {
            holder.tvDate.setText(dateFormat.format(new Date(sms.getTimestamp())));
        } else {
            holder.tvDate.setText("");
        }

        // Category Badge
        String subType = sms.getSubType();
        if (subType == null) subType = sms.getType();
        if (subType == null) subType = "custom";

        switch (subType.toLowerCase()) {
            case "sale":
            case "বিক্রি":
                holder.tvTypeBadge.setText("বিক্রি");
                break;
            case "purchase":
            case "ক্রয়":
                holder.tvTypeBadge.setText("ক্রয়");
                break;
            case "payment":
            case "পেমেন্ট":
                holder.tvTypeBadge.setText("পেমেন্ট");
                break;
            case "baki":
            case "বাকি":
                holder.tvTypeBadge.setText("বাকি");
                break;
            case "return":
            case "রিটার্ন":
                holder.tvTypeBadge.setText("রিটার্ন");
                break;
            default:
                holder.tvTypeBadge.setText("কাস্টম");
                break;
        }

        // Status Badge
        String status = sms.getStatus();
        if (status == null || status.equalsIgnoreCase("sent") || status.equalsIgnoreCase("success")) {
            holder.tvStatusBadge.setText("পাঠানো হয়েছে");
            holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.success));
        } else if (status.equalsIgnoreCase("failed") || status.equalsIgnoreCase("error")) {
            holder.tvStatusBadge.setText("ব্যর্থ");
            holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.error));
        } else {
            holder.tvStatusBadge.setText("অপেক্ষমাণ");
            holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.warning));
        }

        holder.cardSms.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(sms);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardSms;
        TextView tvCustomerName, tvPhone, tvMessage, tvDate, tvTypeBadge, tvStatusBadge;

        ViewHolder(View itemView) {
            super(itemView);
            cardSms = itemView.findViewById(R.id.cardSms);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTypeBadge = itemView.findViewById(R.id.tvTypeBadge);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        }
    }
}