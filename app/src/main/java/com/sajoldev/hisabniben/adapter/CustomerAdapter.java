package com.sajoldev.hisabniben.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {
    private List<Customer> customers;
    private OnCustomerActionListener actionListener;
    private OnCustomerClickListener clickListener;
    private Customer selectedCustomer;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
        void onCustomerLongClick(Customer customer);
    }

    public interface OnCustomerActionListener {
        void onCustomerClick(Customer customer);
        void onSaleClick(Customer customer);
        void onCollectionClick(Customer customer);
        void onSmsClick(Customer customer);
        void onLedgerClick(Customer customer);
    }

    public CustomerAdapter(List<Customer> customers, OnCustomerActionListener actionListener) {
        this.customers = customers;
        this.actionListener = actionListener;
    }

    public CustomerAdapter(List<Customer> customers, OnCustomerClickListener clickListener) {
        this.customers = customers;
        this.clickListener = clickListener;
    }

    public CustomerAdapter(List<Customer> customers, OnCustomerClickListener clickListener, Customer selectedCustomer) {
        this.customers = customers;
        this.clickListener = clickListener;
        this.selectedCustomer = selectedCustomer;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customers.get(position);
        holder.bind(customer);
    }

    @Override
    public int getItemCount() {
        return customers != null ? customers.size() : 0;
    }

    public void updateData(List<Customer> newCustomers) {
        this.customers = newCustomers;
        notifyDataSetChanged();
    }

    class CustomerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCustomerBusinessName, tvCustomerName, tvCustomerTypeBadge;
        private final TextView tvCustomerPhone, tvCustomerLastDate, tvCustomerStatusBadge;
        private final MaterialButton btnCardSale, btnCardCollection, btnCardSms, btnCardLedger;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerBusinessName = itemView.findViewById(R.id.tvCustomerBusinessName);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvCustomerTypeBadge = itemView.findViewById(R.id.tvCustomerTypeBadge);
            tvCustomerPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvCustomerLastDate = itemView.findViewById(R.id.tvCustomerLastDate);
            tvCustomerStatusBadge = itemView.findViewById(R.id.tvCustomerStatusBadge);

            btnCardSale = itemView.findViewById(R.id.btnCardSale);
            btnCardCollection = itemView.findViewById(R.id.btnCardCollection);
            btnCardSms = itemView.findViewById(R.id.btnCardSms);
            btnCardLedger = itemView.findViewById(R.id.btnCardLedger);
        }

        public void bind(Customer customer) {
            String businessName = customer.getBusinessName() != null && !customer.getBusinessName().isEmpty() ? customer.getBusinessName() : customer.getName();
            tvCustomerBusinessName.setText(businessName);
            
            if (customer.getBusinessName() != null && !customer.getBusinessName().isEmpty()) {
                tvCustomerName.setText("স্বত্বাধিকারী: " + customer.getName());
                tvCustomerName.setVisibility(View.VISIBLE);
            } else {
                tvCustomerName.setVisibility(View.GONE);
            }

            String customerType = customer.getCustomerType() != null ? customer.getCustomerType() : "চালের দোকান";
            tvCustomerTypeBadge.setText("🏪 " + customerType);
            tvCustomerPhone.setText("📞 " + (customer.getPhone() != null ? customer.getPhone() : ""));

            if (customer.getUpdatedAt() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH);
                tvCustomerLastDate.setText(sdf.format(new Date(customer.getUpdatedAt())));
            } else {
                tvCustomerLastDate.setText("");
            }

            double baki = customer.getBaki();
            if (baki > 0) {
                tvCustomerStatusBadge.setText("বাকি " + UnitConverterHelper.formatCurrency(baki));
                tvCustomerStatusBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.baki_color));
                tvCustomerStatusBadge.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.bg_badge));
            } else if (baki < 0) {
                tvCustomerStatusBadge.setText("অ্যাডভান্স " + UnitConverterHelper.formatCurrency(Math.abs(baki)));
                tvCustomerStatusBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.success));
                tvCustomerStatusBadge.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.bg_badge));
            } else {
                tvCustomerStatusBadge.setText("কোনো বাকি নেই");
                tvCustomerStatusBadge.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary));
                tvCustomerStatusBadge.setBackground(itemView.getContext().getResources().getDrawable(R.drawable.bg_info_card));
            }

            itemView.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onCustomerClick(customer);
                if (clickListener != null) clickListener.onCustomerClick(customer);
            });

            itemView.setOnLongClickListener(v -> {
                if (clickListener != null) clickListener.onCustomerLongClick(customer);
                return true;
            });

            if (btnCardSale != null) {
                btnCardSale.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onSaleClick(customer);
                });
            }
            if (btnCardCollection != null) {
                btnCardCollection.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onCollectionClick(customer);
                });
            }
            if (btnCardSms != null) {
                btnCardSms.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onSmsClick(customer);
                });
            }
            if (btnCardLedger != null) {
                btnCardLedger.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onLedgerClick(customer);
                });
            }
        }
    }
}
