package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

public class NegativeBalanceWarningDialog extends DialogFragment {

    private String accountName;
    private double currentBalance;
    private double requiredAmount;
    private Runnable onSelectOtherAccountListener;

    public static NegativeBalanceWarningDialog newInstance(String accountName, double currentBalance, double requiredAmount) {
        NegativeBalanceWarningDialog dialog = new NegativeBalanceWarningDialog();
        Bundle args = new Bundle();
        args.putString("accountName", accountName);
        args.putDouble("currentBalance", currentBalance);
        args.putDouble("requiredAmount", requiredAmount);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnSelectOtherAccountListener(Runnable listener) {
        this.onSelectOtherAccountListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            accountName = getArguments().getString("accountName", "Cash");
            currentBalance = getArguments().getDouble("currentBalance", 0.0);
            requiredAmount = getArguments().getDouble("requiredAmount", 0.0);
        }
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_negative_balance_warning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvWarnAccountName = view.findViewById(R.id.tvWarnAccountName);
        TextView tvWarnCurrentBalance = view.findViewById(R.id.tvWarnCurrentBalance);
        TextView tvWarnRequiredAmount = view.findViewById(R.id.tvWarnRequiredAmount);
        TextView tvWarnShortageAmount = view.findViewById(R.id.tvWarnShortageAmount);

        MaterialButton btnCancelWarning = view.findViewById(R.id.btnCancelWarning);
        MaterialButton btnSelectOtherAccount = view.findViewById(R.id.btnSelectOtherAccount);

        tvWarnAccountName.setText(accountName);
        tvWarnCurrentBalance.setText(UnitConverterHelper.formatCurrency(currentBalance));
        tvWarnRequiredAmount.setText(UnitConverterHelper.formatCurrency(requiredAmount));
        tvWarnShortageAmount.setText(UnitConverterHelper.formatCurrency(Math.max(0, requiredAmount - currentBalance)));

        btnCancelWarning.setOnClickListener(v -> dismiss());

        btnSelectOtherAccount.setOnClickListener(v -> {
            if (onSelectOtherAccountListener != null) {
                onSelectOtherAccountListener.run();
            }
            dismiss();
        });
    }
}
