package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddExpenseDialog extends BottomSheetDialogFragment {

    private Runnable onSavedListener;

    public void setOnExpenseSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Delegate to AddTransactionDialog in Expense Mode
        AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_EXPENSE);
        if (onSavedListener != null) {
            dialog.setOnTransactionSavedListener(() -> onSavedListener.run());
        }
        dialog.show(getParentFragmentManager(), "AddExpenseDelegated");
        dismiss();
        return null;
    }
}
