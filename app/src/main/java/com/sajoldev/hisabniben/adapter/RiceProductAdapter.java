package com.sajoldev.hisabniben.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class RiceProductAdapter extends RecyclerView.Adapter<RiceProductAdapter.ViewHolder> {

    public interface OnRiceProductActionListener {
        void onSaleClick(RiceProduct product);
        void onPurchaseClick(RiceProduct product);
        void onStockClick(RiceProduct product);
        void onMoreClick(RiceProduct product, View anchorView);
    }

    private Context context;
    private List<RiceProduct> fullList;
    private List<RiceProduct> displayList;
    private OnRiceProductActionListener listener;

    public RiceProductAdapter(Context context, List<RiceProduct> products, OnRiceProductActionListener listener) {
        this.context = context;
        this.fullList = products != null ? products : new ArrayList<>();
        this.displayList = new ArrayList<>(this.fullList);
        this.listener = listener;
    }

    public void updateData(List<RiceProduct> newProducts) {
        this.fullList = newProducts != null ? newProducts : new ArrayList<>();
        this.displayList = new ArrayList<>(this.fullList);
        notifyDataSetChanged();
    }

    public void filter(String query, String statusFilter) {
        displayList.clear();
        String q = query != null ? query.trim().toLowerCase() : "";

        for (RiceProduct p : fullList) {
            boolean matchesSearch = q.isEmpty()
                || (p.getName() != null && p.getName().toLowerCase().contains(q))
                || (p.getVariety() != null && p.getVariety().toLowerCase().contains(q))
                || (p.getBrand() != null && p.getBrand().toLowerCase().contains(q))
                || (p.getGrade() != null && p.getGrade().toLowerCase().contains(q));

            boolean matchesStatus = true;
            if ("IN_STOCK".equals(statusFilter)) {
                matchesStatus = p.getCurrentStockKg() > p.getMinStockAlertKg();
            } else if ("LOW_STOCK".equals(statusFilter)) {
                matchesStatus = p.getCurrentStockKg() > 0 && p.getCurrentStockKg() <= p.getMinStockAlertKg();
            } else if ("OUT_OF_STOCK".equals(statusFilter)) {
                matchesStatus = p.getCurrentStockKg() <= 0;
            }

            if (matchesSearch && matchesStatus) {
                displayList.add(p);
            }
        }
        notifyDataSetChanged();
    }

    public int getDisplayItemCount() {
        return displayList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rice_product_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RiceProduct product = displayList.get(position);

        holder.tvRiceName.setText(product.getName());
        
        StringBuilder meta = new StringBuilder();
        if (product.getBrand() != null && !product.getBrand().isEmpty()) {
            meta.append("ব্র্যান্ড: ").append(product.getBrand()).append(" · ");
        }
        if (product.getVariety() != null && !product.getVariety().isEmpty()) {
            meta.append("জাত: ").append(product.getVariety()).append(" · ");
        }
        if (product.getGrade() != null && !product.getGrade().isEmpty()) {
            meta.append("গ্রেড: ").append(product.getGrade());
        }
        String metaText = meta.toString();
        if (metaText.endsWith(" · ")) {
            metaText = metaText.substring(0, metaText.length() - 3);
        }
        holder.tvRiceMeta.setText(metaText.isEmpty() ? "চাল পণ্য" : metaText);

        // Inventory Stock Facts
        double stockKg = product.getCurrentStockKg();
        holder.tvStockKg.setText(UnitConverterHelper.formatKg(stockKg));
        holder.tvStockBags.setText(UnitConverterHelper.formatStockBagsAndKg(stockKg, product.getDefaultBagWeight()));

        // Rates
        holder.tvPurchaseRate.setText(UnitConverterHelper.formatCurrency(product.getPurchaseRatePerKg()) + "/KG");
        holder.tvSaleRate.setText(UnitConverterHelper.formatCurrency(product.getSaleRatePerKg()) + "/KG");

        // Stock Status Badge
        if (stockKg <= 0) {
            holder.tvStockStatusBadge.setText("স্টক শেষ");
            holder.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_error);
            holder.tvStockStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
        } else if (stockKg <= product.getMinStockAlertKg()) {
            holder.tvStockStatusBadge.setText("কম স্টক (" + UnitConverterHelper.formatKg(stockKg) + ")");
            holder.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning);
            holder.tvStockStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning));
        } else {
            holder.tvStockStatusBadge.setText("স্টকে আছে");
            holder.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvStockStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
        }

        // Listeners
        holder.btnQuickSale.setOnClickListener(v -> {
            if (listener != null) listener.onSaleClick(product);
        });

        holder.btnQuickPurchase.setOnClickListener(v -> {
            if (listener != null) listener.onPurchaseClick(product);
        });

        holder.btnQuickStock.setOnClickListener(v -> {
            if (listener != null) listener.onStockClick(product);
        });

        holder.btnQuickMore.setOnClickListener(v -> {
            if (listener != null) listener.onMoreClick(product, v);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStockClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRiceName, tvRiceMeta, tvStockStatusBadge, tvStockKg, tvStockBags, tvPurchaseRate, tvSaleRate;
        MaterialButton btnQuickSale, btnQuickPurchase, btnQuickStock;
        ImageView btnQuickMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRiceName = itemView.findViewById(R.id.tvRiceName);
            tvRiceMeta = itemView.findViewById(R.id.tvRiceMeta);
            tvStockStatusBadge = itemView.findViewById(R.id.tvStockStatusBadge);
            tvStockKg = itemView.findViewById(R.id.tvStockKg);
            tvStockBags = itemView.findViewById(R.id.tvStockBags);
            tvPurchaseRate = itemView.findViewById(R.id.tvPurchaseRate);
            tvSaleRate = itemView.findViewById(R.id.tvSaleRate);
            btnQuickSale = itemView.findViewById(R.id.btnQuickSale);
            btnQuickPurchase = itemView.findViewById(R.id.btnQuickPurchase);
            btnQuickStock = itemView.findViewById(R.id.btnQuickStock);
            btnQuickMore = itemView.findViewById(R.id.btnQuickMore);
        }
    }
}
