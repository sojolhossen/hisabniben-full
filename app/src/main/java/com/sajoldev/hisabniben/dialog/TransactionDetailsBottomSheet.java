package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailsBottomSheet extends BottomSheetDialogFragment {
    private Transaction transaction;

    public static TransactionDetailsBottomSheet newInstance(Transaction transaction) {
        TransactionDetailsBottomSheet sheet = new TransactionDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("transaction", transaction);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transaction = (Transaction) getArguments().getSerializable("transaction");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_transaction_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (transaction == null) return;

        TextView tvCustomerName = view.findViewById(R.id.tvCustomerName);
        TextView tvDate = view.findViewById(R.id.tvDate);
        TextView tvType = view.findViewById(R.id.tvType);
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        TextView tvPreviousBaki = view.findViewById(R.id.tvPreviousBaki);
        TextView tvNewBaki = view.findViewById(R.id.tvNewBaki);
        TextView tvNote = view.findViewById(R.id.tvNote);

        tvCustomerName.setText(transaction.getCustomerName() != null ? transaction.getCustomerName() : "Unknown");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", new Locale("bn", "BD"));
        tvDate.setText(sdf.format(new Date(transaction.getCreatedAt())));
        
        String type = transaction.getType();
        if ("payment".equalsIgnoreCase(type)) {
            tvType.setText("Payment");
            tvType.setTextColor(getResources().getColor(R.color.success, null));
        } else {
            tvType.setText("Baki (Due)");
            tvType.setTextColor(getResources().getColor(R.color.baki_color, null));
        }
        
        tvAmount.setText(formatCurrency(transaction.getAmount()));
        
        tvPreviousBaki.setText(formatCurrency(transaction.getPreviousBaki()));
        tvNewBaki.setText(formatCurrency(transaction.getNewBaki()));
        
        String note = transaction.getNote();
        tvNote.setText(note != null && !note.isEmpty() ? note : "No note");
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("bn", "BD"));
        format.setCurrency(Currency.getInstance("BDT"));
        return format.format(amount);
    }
}
