package com.sajoldev.hisabniben.dialog;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.WalletTransaction;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WalletTransactionDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TRANSACTION = "wallet_transaction";

    private WalletTransaction transaction;
    private Runnable onReversedListener;

    private TextView tvDetailTitle, tvDetailAmount, tvDetailAccount, tvDetailCategory, tvPartyLabel, tvDetailParty;
    private TextView tvDetailBalanceBefore, tvDetailBalanceAfter, tvDetailDateTime, tvDetailReferenceNote;
    private RelativeLayout layoutPartyRow;
    private MaterialButton btnReverseTransaction;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    public static WalletTransactionDetailsBottomSheet newInstance(WalletTransaction transaction) {
        WalletTransactionDetailsBottomSheet dialog = new WalletTransactionDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRANSACTION, transaction);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnTransactionReversedListener(Runnable listener) {
        this.onReversedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            transaction = (WalletTransaction) getArguments().getSerializable(ARG_TRANSACTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_wallet_transaction_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();

        initViews(view);
        displayDetails();
    }

    private void initViews(View view) {
        tvDetailTitle = view.findViewById(R.id.tvDetailTitle);
        tvDetailAmount = view.findViewById(R.id.tvDetailAmount);
        tvDetailAccount = view.findViewById(R.id.tvDetailAccount);
        tvDetailCategory = view.findViewById(R.id.tvDetailCategory);
        layoutPartyRow = view.findViewById(R.id.layoutPartyRow);
        tvPartyLabel = view.findViewById(R.id.tvPartyLabel);
        tvDetailParty = view.findViewById(R.id.tvDetailParty);
        tvDetailBalanceBefore = view.findViewById(R.id.tvDetailBalanceBefore);
        tvDetailBalanceAfter = view.findViewById(R.id.tvDetailBalanceAfter);
        tvDetailDateTime = view.findViewById(R.id.tvDetailDateTime);
        tvDetailReferenceNote = view.findViewById(R.id.tvDetailReferenceNote);
        btnReverseTransaction = view.findViewById(R.id.btnReverseTransaction);

        btnReverseTransaction.setOnClickListener(v -> confirmReversal());
    }

    private void displayDetails() {
        if (transaction == null) return;

        tvDetailTitle.setText(transaction.getTitle() != null && !transaction.getTitle().isEmpty() ? transaction.getTitle() : "লেনদেনের বিবরণ");

        String dir = transaction.getDirection();
        if (WalletTransaction.DIRECTION_IN.equals(dir)) {
            tvDetailAmount.setText("+" + UnitConverterHelper.formatCurrency(transaction.getAmount()));
            tvDetailAmount.setTextColor(getResources().getColor(R.color.brand_green));
        } else if (WalletTransaction.DIRECTION_OUT.equals(dir)) {
            tvDetailAmount.setText("-" + UnitConverterHelper.formatCurrency(transaction.getAmount()));
            tvDetailAmount.setTextColor(getResources().getColor(R.color.error));
        } else {
            tvDetailAmount.setText(UnitConverterHelper.formatCurrency(transaction.getAmount()));
            tvDetailAmount.setTextColor(getResources().getColor(R.color.purple));
        }

        tvDetailAccount.setText(transaction.getAccountName() != null ? transaction.getAccountName() : "Cash");
        tvDetailCategory.setText(transaction.getCategory() != null ? transaction.getCategory() : transaction.getType());

        String party = null;
        if (transaction.getCustomerName() != null && !transaction.getCustomerName().isEmpty()) {
            party = transaction.getCustomerName();
            tvPartyLabel.setText("কাস্টমার:");
        } else if (transaction.getSupplierName() != null && !transaction.getSupplierName().isEmpty()) {
            party = transaction.getSupplierName();
            tvPartyLabel.setText("মহাজন / সাপ্লায়ার:");
        }

        if (party != null) {
            layoutPartyRow.setVisibility(View.VISIBLE);
            tvDetailParty.setText(party);
        } else {
            layoutPartyRow.setVisibility(View.GONE);
        }

        tvDetailBalanceBefore.setText(UnitConverterHelper.formatCurrency(transaction.getBalanceBefore()));
        tvDetailBalanceAfter.setText(UnitConverterHelper.formatCurrency(transaction.getBalanceAfter()));
        tvDetailDateTime.setText(dateFormat.format(new Date(transaction.getTransactionDate() > 0 ? transaction.getTransactionDate() : transaction.getCreatedAt())));

        StringBuilder refNote = new StringBuilder();
        if (transaction.getReference() != null && !transaction.getReference().isEmpty()) {
            refNote.append("Ref: ").append(transaction.getReference());
        }
        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            if (refNote.length() > 0) refNote.append(" | ");
            refNote.append(transaction.getDescription());
        }
        tvDetailReferenceNote.setText(refNote.length() > 0 ? refNote.toString() : "নেই");

        if (WalletTransaction.STATUS_REVERSED.equals(transaction.getStatus()) || WalletTransaction.TYPE_REVERSAL.equals(transaction.getType())) {
            btnReverseTransaction.setEnabled(false);
            btnReverseTransaction.setText("এই লেনদেনটি রিভার্স করা হয়েছে");
            btnReverseTransaction.setStrokeColorResource(R.color.divider);
            btnReverseTransaction.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void confirmReversal() {
        if (transaction == null || transaction.getTransactionId() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("লেনদেন রিভার্স (বাতিল) করবেন?");
        builder.setMessage("এই লেনদেনটি বাতিলের মাধ্যমে " + transaction.getAccountName() + " অ্যাকাউন্টের ব্যালেন্সের বিপরীত অ্যাডজাস্টমেন্ট হবে। আপনি কি নিশ্চিত?");

        final EditText inputReason = new EditText(requireContext());
        inputReason.setHint("বাতিলের কারণ লিখুন (যেমন: ভুল এন্ট্রি)");
        inputReason.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(inputReason);

        builder.setPositiveButton("হ্যাঁ, রিভার্স করুন", (dialog, which) -> {
            String reason = inputReason.getText() != null ? inputReason.getText().toString().trim() : "ভুল এন্ট্রি";
            if (reason.isEmpty()) reason = "ভুল এন্ট্রি";

            btnReverseTransaction.setEnabled(false);
            String userId = sessionManager.getUserId();

            firestoreManager.reverseWalletTransaction(userId, transaction.getTransactionId(), reason, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(requireContext(), "লেনদেন রিভার্স করা হয়েছে!", Toast.LENGTH_SHORT).show();
                    if (onReversedListener != null) onReversedListener.run();
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    btnReverseTransaction.setEnabled(true);
                    Toast.makeText(requireContext(), "ত্রুটি: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });

        builder.setNegativeButton("বাতিল", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
