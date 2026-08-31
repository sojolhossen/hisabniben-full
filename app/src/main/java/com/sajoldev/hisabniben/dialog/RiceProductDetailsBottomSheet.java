package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.StockMovement;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RiceProductDetailsBottomSheet extends BottomSheetDialogFragment {

    private RiceProduct product;
    private Runnable onSaleClickListener;
    private Runnable onPurchaseClickListener;

    private TextView tvDetailRiceName, tvDetailRiceMeta, tvDetailStatusBadge, tvDetailStockKg, tvDetailStockBags, tvDetailPurchaseRate, tvDetailSaleRate, tvDetailMinAlert, tvNoTimeline;
    private RecyclerView rvStockTimeline;
    private MaterialButton btnDetailSale, btnDetailPurchase;

    public static RiceProductDetailsBottomSheet newInstance(RiceProduct product) {
        RiceProductDetailsBottomSheet fragment = new RiceProductDetailsBottomSheet();
        fragment.product = product;
        return fragment;
    }

    public void setOnSaleClickListener(Runnable listener) {
        this.onSaleClickListener = listener;
    }

    public void setOnPurchaseClickListener(Runnable listener) {
        this.onPurchaseClickListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_rice_product_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        populateData();
        loadStockTimeline();
    }

    private void initViews(View view) {
        tvDetailRiceName = view.findViewById(R.id.tvDetailRiceName);
        tvDetailRiceMeta = view.findViewById(R.id.tvDetailRiceMeta);
        tvDetailStatusBadge = view.findViewById(R.id.tvDetailStatusBadge);
        tvDetailStockKg = view.findViewById(R.id.tvDetailStockKg);
        tvDetailStockBags = view.findViewById(R.id.tvDetailStockBags);
        tvDetailPurchaseRate = view.findViewById(R.id.tvDetailPurchaseRate);
        tvDetailSaleRate = view.findViewById(R.id.tvDetailSaleRate);
        tvDetailMinAlert = view.findViewById(R.id.tvDetailMinAlert);
        rvStockTimeline = view.findViewById(R.id.rvStockTimeline);
        tvNoTimeline = view.findViewById(R.id.tvNoTimeline);
        btnDetailSale = view.findViewById(R.id.btnDetailSale);
        btnDetailPurchase = view.findViewById(R.id.btnDetailPurchase);
    }

    private void populateData() {
        if (product == null) return;

        tvDetailRiceName.setText(product.getName());
        
        StringBuilder meta = new StringBuilder();
        if (product.getBrand() != null && !product.getBrand().isEmpty()) meta.append(product.getBrand()).append(" · ");
        if (product.getVariety() != null && !product.getVariety().isEmpty()) meta.append(product.getVariety()).append(" · ");
        if (product.getGrade() != null && !product.getGrade().isEmpty()) meta.append(product.getGrade());
        String metaStr = meta.toString();
        if (metaStr.endsWith(" · ")) metaStr = metaStr.substring(0, metaStr.length() - 3);
        tvDetailRiceMeta.setText(metaStr);

        double stockKg = product.getCurrentStockKg();
        tvDetailStockKg.setText(UnitConverterHelper.formatKg(stockKg));
        tvDetailStockBags.setText(UnitConverterHelper.formatStockBagsAndKg(stockKg, product.getDefaultBagWeight()));

        tvDetailPurchaseRate.setText(UnitConverterHelper.formatCurrency(product.getPurchaseRatePerKg()) + "/KG");
        tvDetailSaleRate.setText(UnitConverterHelper.formatCurrency(product.getSaleRatePerKg()) + "/KG");
        tvDetailMinAlert.setText(UnitConverterHelper.formatKg(product.getMinStockAlertKg()));

        if (stockKg <= 0) {
            tvDetailStatusBadge.setText("স্টক শেষ");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_error);
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
        } else if (stockKg <= product.getMinStockAlertKg()) {
            tvDetailStatusBadge.setText("কম স্টক");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_warning);
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
        } else {
            tvDetailStatusBadge.setText("স্টকে আছে");
            tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
        }

        btnDetailSale.setOnClickListener(v -> {
            dismiss();
            if (onSaleClickListener != null) onSaleClickListener.run();
        });

        btnDetailPurchase.setOnClickListener(v -> {
            dismiss();
            if (onPurchaseClickListener != null) onPurchaseClickListener.run();
        });
    }

    private void loadStockTimeline() {
        if (product == null || product.getId() == null) return;

        FirebaseFirestore.getInstance().collection("stockMovements")
            .whereEqualTo("productId", product.getId())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<StockMovement> movements = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    StockMovement sm = doc.toObject(StockMovement.class);
                    movements.add(sm);
                }

                if (movements.isEmpty()) {
                    tvNoTimeline.setVisibility(View.VISIBLE);
                    rvStockTimeline.setVisibility(View.GONE);
                } else {
                    tvNoTimeline.setVisibility(View.GONE);
                    rvStockTimeline.setVisibility(View.VISIBLE);
                    rvStockTimeline.setLayoutManager(new LinearLayoutManager(requireContext()));
                    rvStockTimeline.setAdapter(new TimelineAdapter(movements));
                }
            })
            .addOnFailureListener(e -> {
                tvNoTimeline.setVisibility(View.VISIBLE);
                rvStockTimeline.setVisibility(View.GONE);
            });
    }

    private class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {
        private List<StockMovement> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

        public TimelineAdapter(List<StockMovement> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_movement_timeline_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StockMovement sm = list.get(position);
            holder.tvReason.setText(sm.getReason() != null ? sm.getReason() : sm.getType());
            holder.tvDate.setText(sdf.format(new Date(sm.getCreatedAt() > 0 ? sm.getCreatedAt() : sm.getDate())));

            double qty = sm.getQuantityKg();
            if (qty >= 0) {
                holder.tvQty.setText("+" + UnitConverterHelper.formatKg(qty));
                holder.tvQty.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            } else {
                holder.tvQty.setText(UnitConverterHelper.formatKg(qty));
                holder.tvQty.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvReason, tvDate, tvQty;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvReason = itemView.findViewById(R.id.tvMovementReason);
                tvDate = itemView.findViewById(R.id.tvMovementDate);
                tvQty = itemView.findViewById(R.id.tvMovementQty);
            }
        }
    }
}
