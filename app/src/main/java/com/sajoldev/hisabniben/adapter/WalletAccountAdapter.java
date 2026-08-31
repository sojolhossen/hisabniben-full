package com.sajoldev.hisabniben.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.WalletAccount;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class WalletAccountAdapter extends RecyclerView.Adapter<WalletAccountAdapter.ViewHolder> {

    private final Context context;
    private List<WalletAccount> accounts = new ArrayList<>();
    private OnAccountClickListener clickListener;
    private OnAccountActionListener actionListener;

    public interface OnAccountClickListener {
        void onAccountClick(WalletAccount account);
    }

    public interface OnAccountActionListener {
        void onEditClick(WalletAccount account);
        void onDeleteClick(WalletAccount account);
    }

    public WalletAccountAdapter(Context context) {
        this.context = context;
    }

    public void setAccounts(List<WalletAccount> accounts) {
        this.accounts = accounts != null ? accounts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnAccountClickListener(OnAccountClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnAccountActionListener(OnAccountActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallet_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletAccount account = accounts.get(position);
        holder.bind(account);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAccountIcon, tvAccountName, tvAccountType, tvAccountBalance, tvAccountStatus;
        private final View btnEditAccount, btnDeleteAccount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAccountIcon = itemView.findViewById(R.id.tvAccountIcon);
            tvAccountName = itemView.findViewById(R.id.tvAccountName);
            tvAccountType = itemView.findViewById(R.id.tvAccountType);
            tvAccountBalance = itemView.findViewById(R.id.tvAccountBalance);
            tvAccountStatus = itemView.findViewById(R.id.tvAccountStatus);
            btnEditAccount = itemView.findViewById(R.id.btnEditAccount);
            btnDeleteAccount = itemView.findViewById(R.id.btnDeleteAccount);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onAccountClick(accounts.get(pos));
                }
            });

            if (btnEditAccount != null) {
                btnEditAccount.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && actionListener != null) {
                        actionListener.onEditClick(accounts.get(pos));
                    }
                });
            }

            if (btnDeleteAccount != null) {
                btnDeleteAccount.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && actionListener != null) {
                        actionListener.onDeleteClick(accounts.get(pos));
                    }
                });
            }
        }

        public void bind(WalletAccount account) {
            tvAccountName.setText(account.getAccountName());
            tvAccountBalance.setText(UnitConverterHelper.formatCurrency(account.getCurrentBalance()));

            String type = account.getAccountType() != null ? account.getAccountType().toUpperCase() : "CASH";
            switch (type) {
                case WalletAccount.TYPE_BKASH:
                    tvAccountIcon.setText("📱");
                    tvAccountType.setText("bKash মোবাইল ওয়ালেট");
                    break;
                case WalletAccount.TYPE_NAGAD:
                    tvAccountIcon.setText("📲");
                    tvAccountType.setText("Nagad মোবাইল ওয়ালেট");
                    break;
                case WalletAccount.TYPE_BANK:
                    tvAccountIcon.setText("🏦");
                    tvAccountType.setText("ব্যাংক হিসাব");
                    break;
                case WalletAccount.TYPE_OTHER:
                    tvAccountIcon.setText("💼");
                    tvAccountType.setText("অন্যান্য ফান্ড / ওয়ালেট");
                    break;
                default:
                    tvAccountIcon.setText("💵");
                    tvAccountType.setText("নগদ ক্যাশ বাক্স");
                    break;
            }

            if (account.isActive()) {
                tvAccountStatus.setText("সক্রিয়");
                tvAccountStatus.setTextColor(context.getResources().getColor(R.color.brand_green));
            } else {
                tvAccountStatus.setText("নিষ্ক্রিয়");
                tvAccountStatus.setTextColor(context.getResources().getColor(R.color.text_secondary));
            }
        }
    }
}
