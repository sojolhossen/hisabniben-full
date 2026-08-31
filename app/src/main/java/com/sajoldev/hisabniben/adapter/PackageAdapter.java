package com.sajoldev.hisabniben.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.SubscriptionPackage;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.PackageViewHolder> {
    private List<SubscriptionPackage> packages;
    private final OnPackageClickListener listener;

    public interface OnPackageClickListener {
        void onPackageClick(SubscriptionPackage pkg);
    }

    public PackageAdapter(OnPackageClickListener context, OnPackageClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<SubscriptionPackage> packages) {
        this.packages = packages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_package, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        SubscriptionPackage pkg = packages.get(position);
        holder.bind(pkg, listener, position);
    }

    @Override
    public int getItemCount() {
        return packages != null ? packages.size() : 0;
    }

    static class PackageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvDuration, tvPrice, tvDescription, tvPopular;
        private final LinearLayout layoutFeatures;
        private final MaterialButton btnSubscribe;

        public PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPopular = itemView.findViewById(R.id.tvPopular);
            layoutFeatures = itemView.findViewById(R.id.layoutFeatures);
            btnSubscribe = itemView.findViewById(R.id.btnSubscribe);
        }

        public void bind(SubscriptionPackage pkg, OnPackageClickListener listener, int position) {
            tvName.setText(pkg.getName());
            if (pkg.getDurationDays() >= 999) {
                tvDuration.setText("Life-Time");
            } else {
                tvDuration.setText(pkg.getDurationDays() + " দিন");
            }
            tvPrice.setText(formatCurrency(pkg.getPrice()));
            tvDescription.setText(pkg.getDescription());
            
            if (position == 1) {
                tvPopular.setVisibility(View.VISIBLE);
                btnSubscribe.setBackgroundTintList(itemView.getContext().getResources().getColorStateList(R.color.primary, itemView.getContext().getTheme()));
            } else {
                tvPopular.setVisibility(View.GONE);
            }
            
            layoutFeatures.removeAllViews();
            List<String> features = pkg.getFeatures();
            if (features == null || features.isEmpty()) {
                features = new ArrayList<>();
                features.add("Unlimited Customers");
                features.add("Unlimited Transactions");
                features.add("Reports & Analytics");
                features.add("Priority Support");
            }
            for (String feature : features) {
                TextView tv = new TextView(itemView.getContext());
                tv.setText("• " + feature);
                tv.setTextSize(12);
                tv.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary, itemView.getContext().getTheme()));
                tv.setPadding(0, 4, 0, 4);
                layoutFeatures.addView(tv);
            }
            
            btnSubscribe.setOnClickListener(v -> listener.onPackageClick(pkg));
        }

        private String formatCurrency(double amount) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("bn", "BD"));
            formatter.setCurrency(Currency.getInstance("BDT"));
            return formatter.format(amount).replace("BDT", "৳");
        }
    }
}
