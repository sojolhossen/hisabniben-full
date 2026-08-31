package com.sajoldev.hisabniben.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.WalletTransaction;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {

    private final Context context;
    private List<WalletTransaction> transactions = new ArrayList<>();
    private OnTransactionClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    public interface OnTransactionClickListener {
        void onTransactionClick(WalletTransaction transaction);
    }

    public WalletTransactionAdapter(Context context) {
        this.context = context;
    }

    public void setTransactions(List<WalletTransaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallet_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransaction wt = transactions.get(position);
        holder.bind(wt);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTxDirectionIcon, tvTxTitle, tvTxCategory, tvTxAccount, tvTxAmount, tvTxRunningBalance, tvTxDateTime, tvTxStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTxDirectionIcon = itemView.findViewById(R.id.tvTxDirectionIcon);
            tvTxTitle = itemView.findViewById(R.id.tvTxTitle);
            tvTxCategory = itemView.findViewById(R.id.tvTxCategory);
            tvTxAccount = itemView.findViewById(R.id.tvTxAccount);
            tvTxAmount = itemView.findViewById(R.id.tvTxAmount);
            tvTxRunningBalance = itemView.findViewById(R.id.tvTxRunningBalance);
            tvTxDateTime = itemView.findViewById(R.id.tvTxDateTime);
            tvTxStatus = itemView.findViewById(R.id.tvTxStatus);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransactionClick(transactions.get(pos));
                }
            });
        }

        public void bind(WalletTransaction wt) {
            tvTxTitle.setText(wt.getTitle() != null && !wt.getTitle().isEmpty() ? wt.getTitle() : "লেনদেন");
            tvTxCategory.setText(wt.getCategory() != null ? wt.getCategory() : wt.getType());
            tvTxAccount.setText(wt.getAccountName() != null ? wt.getAccountName() : "Cash");
            tvTxRunningBalance.setText("ব্যালেন্স: " + UnitConverterHelper.formatCurrency(wt.getBalanceAfter()));
            tvTxDateTime.setText(dateFormat.format(new Date(wt.getTransactionDate() > 0 ? wt.getTransactionDate() : wt.getCreatedAt())));

            if (WalletTransaction.STATUS_REVERSED.equals(wt.getStatus())) {
                tvTxStatus.setText("বাতিল (Reversed)");
                tvTxStatus.setTextColor(context.getResources().getColor(R.color.error));
            } else {
                tvTxStatus.setText("সম্পন্ন");
                tvTxStatus.setTextColor(context.getResources().getColor(R.color.brand_green));
            }

            String dir = wt.getDirection();
            if (WalletTransaction.DIRECTION_IN.equals(dir)) {
                tvTxDirectionIcon.setText("↑");
                tvTxDirectionIcon.setTextColor(context.getResources().getColor(R.color.brand_green));
                tvTxDirectionIcon.setBackgroundResource(R.drawable.bg_rounded_light_green);
                tvTxAmount.setText("+" + UnitConverterHelper.formatCurrency(wt.getAmount()));
                tvTxAmount.setTextColor(context.getResources().getColor(R.color.brand_green));
            } else if (WalletTransaction.DIRECTION_OUT.equals(dir)) {
                tvTxDirectionIcon.setText("↓");
                tvTxDirectionIcon.setTextColor(context.getResources().getColor(R.color.error));
                tvTxDirectionIcon.setBackgroundResource(R.drawable.bg_rounded_light_red);
                tvTxAmount.setText("-" + UnitConverterHelper.formatCurrency(wt.getAmount()));
                tvTxAmount.setTextColor(context.getResources().getColor(R.color.error));
            } else {
                tvTxDirectionIcon.setText("↔");
                tvTxDirectionIcon.setTextColor(context.getResources().getColor(R.color.purple));
                tvTxDirectionIcon.setBackgroundResource(R.drawable.bg_rounded_light_blue);
                tvTxAmount.setText(UnitConverterHelper.formatCurrency(wt.getAmount()));
                tvTxAmount.setTextColor(context.getResources().getColor(R.color.purple));
            }
        }
    }
}
