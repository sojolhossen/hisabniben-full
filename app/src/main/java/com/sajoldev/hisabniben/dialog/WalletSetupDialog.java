package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.WalletAccount;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class WalletSetupDialog extends BottomSheetDialogFragment {

    private TextInputEditText etCashOpening, etBkashOpening, etNagadOpening, etBankOpening;
    private MaterialButton btnSaveWalletSetup;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private Runnable onSavedListener;

    public void setOnWalletSetupSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_wallet_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();

        initViews(view);
    }

    private void initViews(View view) {
        etCashOpening = view.findViewById(R.id.etCashOpening);
        etBkashOpening = view.findViewById(R.id.etBkashOpening);
        etNagadOpening = view.findViewById(R.id.etNagadOpening);
        etBankOpening = view.findViewById(R.id.etBankOpening);

        btnSaveWalletSetup = view.findViewById(R.id.btnSaveWalletSetup);
        progressBar = view.findViewById(R.id.progressBar);

        btnSaveWalletSetup.setOnClickListener(v -> saveSetup());
    }

    private void saveSetup() {
        double cash = parseDouble(etCashOpening.getText() != null ? etCashOpening.getText().toString() : "0");
        double bkash = parseDouble(etBkashOpening.getText() != null ? etBkashOpening.getText().toString() : "0");
        double nagad = parseDouble(etNagadOpening.getText() != null ? etNagadOpening.getText().toString() : "0");
        double bank = parseDouble(etBankOpening.getText() != null ? etBankOpening.getText().toString() : "0");

        Map<String, Double> balances = new HashMap<>();
        balances.put("account_cash", cash);
        balances.put(WalletAccount.TYPE_CASH, cash);

        balances.put("account_bkash", bkash);
        balances.put(WalletAccount.TYPE_BKASH, bkash);

        balances.put("account_nagad", nagad);
        balances.put(WalletAccount.TYPE_NAGAD, nagad);

        balances.put("account_bank", bank);
        balances.put(WalletAccount.TYPE_BANK, bank);

        String userId = sessionManager.getUserId();
        if (userId == null) return;

        showLoading(true);

        firestoreManager.setOpeningBalances(userId, balances, new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                showLoading(false);
                Toast.makeText(requireContext(), "প্রারম্ভিক ব্যালেন্স সফলভাবে সেট করা হয়েছে!", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.run();
                dismiss();
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(requireContext(), "ত্রুটি: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveWalletSetup.setEnabled(!show);
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
