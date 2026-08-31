package com.sajoldev.hisabniben.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.SmsPackage;

import java.util.List;
import java.util.Locale;

public class SmsPackageAdapter extends RecyclerView.Adapter<SmsPackageAdapter.ViewHolder> {
    private List<SmsPackage> packages;
    private OnPackageClickListener listener;

    public interface OnPackageClickListener {
        void onPackageClick(SmsPackage pkg);
    }

    public SmsPackageAdapter(List<SmsPackage> packages, OnPackageClickListener listener) {
        this.packages = packages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_sms_package, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmsPackage pkg = packages.get(position);
        holder.tvPackageName.setText(pkg.getName() != null ? pkg.getName() : "SMS Pack");
        holder.tvSmsCount.setText(pkg.getSmsCount() + " SMS");
        holder.tvPrice.setText("৳" + (int) pkg.getPrice());

        if (pkg.getSmsCount() > 0) {
            double ratePerSms = pkg.getPrice() / pkg.getSmsCount();
            holder.tvPricePerSms.setText(String.format(Locale.ENGLISH, "৳%.2f / SMS", ratePerSms));
        } else {
            holder.tvPricePerSms.setVisibility(View.GONE);
        }

        if (pkg.isPopular()) {
            holder.tvPopularBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvPopularBadge.setVisibility(View.GONE);
        }

        holder.btnBuy.setOnClickListener(v -> {
            if (listener != null) listener.onPackageClick(pkg);
        });
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPackageName, tvSmsCount, tvPrice, tvPricePerSms, tvPopularBadge;
        MaterialButton btnBuy;

        ViewHolder(View itemView) {
            super(itemView);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            tvSmsCount = itemView.findViewById(R.id.tvSmsCount);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvPricePerSms = itemView.findViewById(R.id.tvPricePerSms);
            tvPopularBadge = itemView.findViewById(R.id.tvPopularBadge);
            btnBuy = itemView.findViewById(R.id.btnBuy);
        }
    }
}