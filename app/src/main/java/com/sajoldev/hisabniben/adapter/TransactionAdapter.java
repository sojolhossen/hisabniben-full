package com.sajoldev.hisabniben.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactions;
    private List<Customer> customers;
    private final OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
        this.transactions = transactions;
        this.customers = null;
        this.listener = listener;
    }

    public void updateData(List<Transaction> transactions, List<Customer> customers) {
        this.transactions = transactions;
        this.customers = customers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
        holder.itemView.setOnClickListener(v -> listener.onTransactionClick(transaction));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate, tvMonth, tvCustomerName, tvType, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvType = itemView.findViewById(R.id.tvType);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }

        public void bind(Transaction transaction) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
            
            Date date = new Date(transaction.getDate());
            tvDate.setText(dateFormat.format(date));
            tvMonth.setText(monthFormat.format(date));

            tvCustomerName.setText(transaction.getCustomerName());
            
            if (Transaction.TYPE_PAYMENT.equals(transaction.getType())) {
                tvType.setText("Payment");
                tvType.setTextColor(itemView.getContext().getResources().getColor(R.color.success, itemView.getContext().getTheme()));
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.success, itemView.getContext().getTheme()));
                animateAmount(tvAmount, 0, (int) transaction.getAmount(), "+", "৳");
            } else {
                tvType.setText("Baki");
                tvType.setTextColor(itemView.getContext().getResources().getColor(R.color.baki_color, itemView.getContext().getTheme()));
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(R.color.baki_color, itemView.getContext().getTheme()));
                animateAmount(tvAmount, 0, (int) transaction.getAmount(), "-", "৳");
            }
        }

        private String formatCurrency(double amount) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("bn", "BD"));
            formatter.setCurrency(Currency.getInstance("BDT"));
            return formatter.format(amount).replace("BDT", "৳");
        }

        private void animateAmount(TextView textView, int startValue, int endValue, String sign, String prefix) {
            ValueAnimator animator = ValueAnimator.ofInt(startValue, endValue);
            animator.setDuration(800);
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            
            animator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                textView.setText(sign + prefix + value);
            });
            
            animator.start();
        }
    }
}
