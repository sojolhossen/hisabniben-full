package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.WalletAccount;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class WalletTransferDialog extends BottomSheetDialogFragment {

    private AutoCompleteTextView actvSourceAccount, actvDestAccount;
    private TextInputLayout tilSourceAccount, tilDestAccount, tilTransferAmount, tilTransferNote;
    private TextInputEditText etTransferAmount, etTransferNote;
    private TextView tvSourceBalanceHint;
    private MaterialButton btnSubmitTransfer;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private List<WalletAccount> accounts = new ArrayList<>();
    private WalletAccount selectedSourceAccount;
    private WalletAccount selectedDestAccount;

    private Runnable onSavedListener;

    public void setOnTransferSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_wallet_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();

        initViews(view);
        loadAccounts();
    }

    private void initViews(View view) {
        tilSourceAccount = view.findViewById(R.id.tilSourceAccount);
        actvSourceAccount = view.findViewById(R.id.actvSourceAccount);
        tvSourceBalanceHint = view.findViewById(R.id.tvSourceBalanceHint);

        tilDestAccount = view.findViewById(R.id.tilDestAccount);
        actvDestAccount = view.findViewById(R.id.actvDestAccount);

        tilTransferAmount = view.findViewById(R.id.tilTransferAmount);
        etTransferAmount = view.findViewById(R.id.etTransferAmount);

        tilTransferNote = view.findViewById(R.id.tilTransferNote);
        etTransferNote = view.findViewById(R.id.etTransferNote);

        btnSubmitTransfer = view.findViewById(R.id.btnSubmitTransfer);
        progressBar = view.findViewById(R.id.progressBar);

        btnSubmitTransfer.setOnClickListener(v -> submitTransfer());
    }

    private void loadAccounts() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        firestoreManager.getActiveWalletAccounts(userId, new FirestoreManager.FirestoreListCallback<WalletAccount>() {
            @Override
            public void onSuccess(List<WalletAccount> result) {
                accounts = result;
                List<String> names = new ArrayList<>();
                for (WalletAccount acc : accounts) {
                    names.add(acc.getAccountName() + " (৳" + String.format("%.0f", acc.getCurrentBalance()) + ")");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                actvSourceAccount.setAdapter(adapter);
                actvDestAccount.setAdapter(adapter);

                actvSourceAccount.setOnItemClickListener((parent, view, position, id) -> {
                    if (position >= 0 && position < accounts.size()) {
                        selectedSourceAccount = accounts.get(position);
                        tvSourceBalanceHint.setText("বর্তমান ব্যালেন্স: " + UnitConverterHelper.formatCurrency(selectedSourceAccount.getCurrentBalance()));
                    }
                });

                actvDestAccount.setOnItemClickListener((parent, view, position, id) -> {
                    if (position >= 0 && position < accounts.size()) {
                        selectedDestAccount = accounts.get(position);
                    }
                });
            }

            @Override public void onFailure(String error) {}
        });
    }

    private void submitTransfer() {
        if (selectedSourceAccount == null) {
            tilSourceAccount.setError("উৎস অ্যাকাউন্ট নির্বাচন করুন");
            return;
        }
        tilSourceAccount.setError(null);

        if (selectedDestAccount == null) {
            tilDestAccount.setError("গন্তব্য অ্যাকাউন্ট নির্বাচন করুন");
            return;
        }
        tilDestAccount.setError(null);

        if (selectedSourceAccount.getAccountId().equals(selectedDestAccount.getAccountId())) {
            tilDestAccount.setError("উৎস ও গন্তব্য অ্যাকাউন্ট আলাদা হতে হবে");
            return;
        }

        String amountStr = etTransferAmount.getText() != null ? etTransferAmount.getText().toString().trim() : "";
        if (TextUtils.isEmpty(amountStr)) {
            tilTransferAmount.setError("টাকার পরিমাণ লিখুন");
            return;
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            tilTransferAmount.setError("সঠিক টাকার পরিমাণ দিন");
            return;
        }

        if (amount <= 0) {
            tilTransferAmount.setError("পরিমাণ 0 এর চেয়ে বেশি হতে হবে");
            return;
        }

        if (selectedSourceAccount.getCurrentBalance() < amount) {
            tilTransferAmount.setError("উৎস অ্যাকাউন্টে পর্যাপ্ত টাকা নেই (" + UnitConverterHelper.formatCurrency(selectedSourceAccount.getCurrentBalance()) + ")");
            return;
        }
        tilTransferAmount.setError(null);

        String notes = etTransferNote.getText() != null ? etTransferNote.getText().toString().trim() : "";
        String userId = sessionManager.getUserId();

        showLoading(true);

        firestoreManager.executeAccountTransfer(
            userId,
            selectedSourceAccount.getAccountId(),
            selectedDestAccount.getAccountId(),
            amount,
            notes,
            "Transfer",
            new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "টাকা ট্রান্সফার সফল হয়েছে!", Toast.LENGTH_SHORT).show();
                    if (onSavedListener != null) onSavedListener.run();
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "ত্রুটি: " + error, Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmitTransfer.setEnabled(!show);
    }
}
