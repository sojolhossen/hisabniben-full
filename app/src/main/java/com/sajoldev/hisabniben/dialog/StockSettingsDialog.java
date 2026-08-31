package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.util.SessionManager;

public class StockSettingsDialog extends BottomSheetDialogFragment {

    private SwitchMaterial switchNegativeStock;
    private TextInputLayout tilLowStockThreshold;
    private TextInputEditText etLowStockThreshold;
    private MaterialButton btnSaveStockSettings;

    private SessionManager sessionManager;
    private Runnable onSavedListener;

    public void setOnSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_stock_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());

        switchNegativeStock = view.findViewById(R.id.switchNegativeStock);
        tilLowStockThreshold = view.findViewById(R.id.tilLowStockThreshold);
        etLowStockThreshold = view.findViewById(R.id.etLowStockThreshold);
        btnSaveStockSettings = view.findViewById(R.id.btnSaveStockSettings);

        switchNegativeStock.setChecked(!sessionManager.getAllowNegativeStock());
        etLowStockThreshold.setText(String.valueOf(sessionManager.getLowStockThreshold()));

        btnSaveStockSettings.setOnClickListener(v -> saveStockSettings());
    }

    private void saveStockSettings() {
        boolean preventNegativeStock = switchNegativeStock.isChecked();
        sessionManager.setAllowNegativeStock(!preventNegativeStock);

        String thresholdStr = etLowStockThreshold.getText() != null ? etLowStockThreshold.getText().toString().trim() : "";
        int threshold = 100;
        try {
            threshold = Integer.parseInt(thresholdStr);
        } catch (NumberFormatException e) {
            threshold = 100;
        }

        sessionManager.setLowStockThreshold(threshold);
        Toast.makeText(requireContext(), "স্টক সেটিংস সফলভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show();

        if (onSavedListener != null) onSavedListener.run();
        dismiss();
    }
}
