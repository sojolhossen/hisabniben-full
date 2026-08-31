package com.sajoldev.hisabniben.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.SupplierViewHolder> {
    private List<Supplier> suppliers;
    private OnSupplierActionListener actionListener;
    private OnSupplierClickListener clickListener;

    public interface OnSupplierClickListener {
        void onSupplierClick(Supplier supplier);
        void onSupplierLongClick(Supplier supplier);
    }

    public interface OnSupplierActionListener {
        void onSupplierClick(Supplier supplier);
        void onPurchaseClick(Supplier supplier);
        void onPaymentClick(Supplier supplier);
        void onLedgerClick(Supplier supplier);
    }

    public SupplierAdapter(List<Supplier> suppliers, OnSupplierActionListener actionListener) {
        this.suppliers = suppliers;
        this.actionListener = actionListener;
    }

    public SupplierAdapter(List<Supplier> suppliers, OnSupplierClickListener clickListener) {
        this.suppliers = suppliers;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public SupplierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supplier, parent, false);
        return new SupplierViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SupplierViewHolder holder, int position) {
        Supplier supplier = suppliers.get(position);
        holder.bind(supplier);
    }

    @Override
    public int getItemCount() {
        return suppliers != null ? suppliers.size() : 0;
    }

    public void updateData(List<Supplier> newSuppliers) {
        this.suppliers = newSuppliers;
        notifyDataSetChanged();
    }

    class SupplierViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSupplierBusinessName, tvSupplierName, tvSupplierTypeBadge;
        private final TextView tvSupplierPhone, tvSupplierLastDate, tvSupplierStatusBadge;
        private final MaterialButton btnCardPurchase, btnCardPayment, btnCardLedger, btnCardMore;

        public SupplierViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSupplierBusinessName = itemView.findViewById(R.id.tvSupplierBusinessName);
            tvSupplierName = itemView.findViewById(R.id.tvSupplierName);
            tvSupplierTypeBadge = itemView.findViewById(R.id.tvSupplierTypeBadge);
            tvSupplierPhone = itemView.findViewById(R.id.tvSupplierPhone);
            tvSupplierLastDate = itemView.findViewById(R.id.tvSupplierLastDate);
            tvSupplierStatusBadge = itemView.findViewById(R.id.tvSupplierStatusBadge);

            btnCardPurchase = itemView.findViewById(R.id.btnCardPurchase);
            btnCardPayment = itemView.findViewById(R.id.btnCardPayment);
            btnCardLedger = itemView.findViewById(R.id.btnCardLedger);
            btnCardMore = itemView.findViewById(R.id.btnCardMore);
        }

        public void bind(Supplier supplier) {
            String businessName = supplier.getBusinessName() != null && !supplier.getBusinessName().isEmpty() ? supplier.getBusinessName() : supplier.getName();
            tvSupplierBusinessName.setText(businessName);

            if (supplier.getBusinessName() != null && !supplier.getBusinessName().isEmpty()) {
                tvSupplierName.setText("মালিক: " + supplier.getName());
                tvSupplierName.setVisibility(View.VISIBLE);
            } else {
                tvSupplierName.setVisibility(View.GONE);
            }

            String supplierType = supplier.getSupplierType() != null ? supplier.getSupplierType() : "চাল মিল";
            tvSupplierTypeBadge.setText("🏭 " + supplierType);
            tvSupplierPhone.setText("📞 " + (supplier.getPhone() != null ? supplier.getPhone() : ""));

            if (supplier.getUpdatedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH);
                tvSupplierLastDate.setText(sdf.format(new Date(supplier.getUpdatedAt())));
            } else {
                tvSupplierLastDate.setText("");
            }

            double payable = supplier.getCurrentPayable();
            if (payable > 0) {
                tvSupplierStatusBadge.setText("পাওনা " + UnitConverterHelper.formatCurrency(payable));
                tvSupplierStatusBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.warning));
                tvSupplierStatusBadge.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.bg_badge));
            } else if (payable < 0) {
                tvSupplierStatusBadge.setText("অগ্রিম " + UnitConverterHelper.formatCurrency(Math.abs(payable)));
                tvSupplierStatusBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.success));
                tvSupplierStatusBadge.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.bg_badge));
            } else {
                tvSupplierStatusBadge.setText("কোনো পাওনা নেই");
                tvSupplierStatusBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary));
                tvSupplierStatusBadge.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.bg_info_card));
            }

            itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onSupplierClick(supplier);
                if (clickListener != null) clickListener.onSupplierClick(supplier);
            });

            itemView.setOnLongClickListener(v -> {
                if (clickListener != null) clickListener.onSupplierLongClick(supplier);
                return true;
            });

            if (btnCardPurchase != null) {
                btnCardPurchase.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onPurchaseClick(supplier);
                });
            }
            if (btnCardPayment != null) {
                btnCardPayment.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onPaymentClick(supplier);
                });
            }
            if (btnCardLedger != null) {
                btnCardLedger.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onLedgerClick(supplier);
                });
            }
            if (btnCardMore != null) {
                btnCardMore.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onSupplierClick(supplier);
                });
            }
        }
    }
}
