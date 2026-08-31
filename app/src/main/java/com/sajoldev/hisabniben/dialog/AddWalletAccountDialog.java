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

public class AddWalletAccountDialog extends BottomSheetDialogFragment {

    private TextView tvDialogTitle;
    private TextInputLayout tilAccountName, tilAccountType, tilOpeningBalance;
    private TextInputEditText etAccountName, etOpeningBalance;
    private AutoCompleteTextView actvAccountType;
    private MaterialButton btnSubmitAccount;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private Runnable onSavedListener;
    private WalletAccount accountToEdit;

    private final String[] accountTypes = {
        WalletAccount.TYPE_CASH,
        WalletAccount.TYPE_BKASH,
        WalletAccount.TYPE_NAGAD,
        WalletAccount.TYPE_BANK,
        WalletAccount.TYPE_OTHER
    };

    public static AddWalletAccountDialog newInstance(@Nullable WalletAccount accountToEdit) {
        AddWalletAccountDialog dialog = new AddWalletAccountDialog();
        if (accountToEdit != null) {
            Bundle args = new Bundle();
            args.putSerializable("accountToEdit", accountToEdit);
            dialog.setArguments(args);
        }
        return dialog;
    }

    public void setOnAccountSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            accountToEdit = (WalletAccount) getArguments().getSerializable("accountToEdit");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_wallet_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();

        initViews(view);
        populateDataForEdit();
    }

    private void initViews(View view) {
        tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        tilAccountName = view.findViewById(R.id.tilAccountName);
        etAccountName = view.findViewById(R.id.etAccountName);

        tilAccountType = view.findViewById(R.id.tilAccountType);
        actvAccountType = view.findViewById(R.id.actvAccountType);

        tilOpeningBalance = view.findViewById(R.id.tilOpeningBalance);
        etOpeningBalance = view.findViewById(R.id.etOpeningBalance);

        btnSubmitAccount = view.findViewById(R.id.btnSubmitAccount);
        progressBar = view.findViewById(R.id.progressBar);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, accountTypes);
        actvAccountType.setAdapter(adapter);

        btnSubmitAccount.setOnClickListener(v -> saveAccount());
    }

    private void populateDataForEdit() {
        if (accountToEdit != null) {
            if (tvDialogTitle != null) tvDialogTitle.setText("অ্যাকাউন্ট সম্পাদনা করুন (Edit Account)");
            if (etAccountName != null) etAccountName.setText(accountToEdit.getAccountName());
            if (actvAccountType != null && accountToEdit.getAccountType() != null) {
                actvAccountType.setText(accountToEdit.getAccountType(), false);
            }
            if (etOpeningBalance != null) {
                etOpeningBalance.setText(String.valueOf(accountToEdit.getCurrentBalance()));
            }
            if (btnSubmitAccount != null) btnSubmitAccount.setText("আপডেট করুন (Update)");
        }
    }

    private void saveAccount() {
        String name = etAccountName.getText() != null ? etAccountName.getText().toString().trim() : "";
        if (TextUtils.isEmpty(name)) {
            tilAccountName.setError("অ্যাকাউন্টের নাম লিখুন");
            return;
        }
        tilAccountName.setError(null);

        String type = actvAccountType.getText() != null ? actvAccountType.getText().toString().trim().toUpperCase() : WalletAccount.TYPE_OTHER;
        if (TextUtils.isEmpty(type)) type = WalletAccount.TYPE_OTHER;

        String openingStr = etOpeningBalance.getText() != null ? etOpeningBalance.getText().toString().trim() : "0";
        double balance = 0;
        try {
            if (!openingStr.isEmpty()) balance = Double.parseDouble(openingStr);
        } catch (NumberFormatException e) {
            tilOpeningBalance.setError("সঠিক পরিমাণ দিন");
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null) return;

        showLoading(true);

        if (accountToEdit != null) {
            // EDIT MODE
            accountToEdit.setAccountName(name);
            accountToEdit.setAccountType(type);
            accountToEdit.setCurrentBalance(balance);

            firestoreManager.updateWalletAccount(userId, accountToEdit, new FirestoreManager.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "অ্যাকাউন্ট তথ্য আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
                    if (onSavedListener != null) onSavedListener.run();
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "আপডেটে ত্রুটি: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // CREATE MODE
            WalletAccount account = new WalletAccount();
            account.setAccountName(name);
            account.setAccountType(type);
            account.setOpeningBalance(balance);
            account.setCurrentBalance(balance);
            account.setUserId(userId);
            account.setActive(true);

            firestoreManager.createWalletAccount(userId, account, new FirestoreManager.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "নতুন অ্যাকাউন্ট তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
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
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmitAccount.setEnabled(!show);
    }
}
