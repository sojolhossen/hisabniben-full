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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.util.SessionManager;

public class RiceUnitSettingsDialog extends BottomSheetDialogFragment {

    private ChipGroup chipGroupBagWeight;
    private Chip chip25Kg, chip50Kg, chip75Kg, chipCustomKg;
    private TextInputLayout tilCustomWeight;
    private TextInputEditText etCustomWeight;
    private MaterialButton btnSave;

    private SessionManager sessionManager;
    private Runnable onSavedListener;

    public void setOnSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_rice_unit_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());

        chipGroupBagWeight = view.findViewById(R.id.chipGroupBagWeight);
        chip25Kg = view.findViewById(R.id.chip25Kg);
        chip50Kg = view.findViewById(R.id.chip50Kg);
        chip75Kg = view.findViewById(R.id.chip75Kg);
        chipCustomKg = view.findViewById(R.id.chipCustomKg);
        tilCustomWeight = view.findViewById(R.id.tilCustomWeight);
        etCustomWeight = view.findViewById(R.id.etCustomWeight);
        btnSave = view.findViewById(R.id.btnSave);

        int currentWeight = sessionManager.getDefaultBagWeight();
        if (currentWeight == 25) chip25Kg.setChecked(true);
        else if (currentWeight == 50) chip50Kg.setChecked(true);
        else if (currentWeight == 75) chip75Kg.setChecked(true);
        else {
            chipCustomKg.setChecked(true);
            tilCustomWeight.setVisibility(View.VISIBLE);
            etCustomWeight.setText(String.valueOf(currentWeight));
        }

        chipGroupBagWeight.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCustomKg) {
                tilCustomWeight.setVisibility(View.VISIBLE);
            } else {
                tilCustomWeight.setVisibility(View.GONE);
            }
        });

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        int checkedId = chipGroupBagWeight.getCheckedChipId();
        int bagWeight = 50;

        if (checkedId == R.id.chip25Kg) bagWeight = 25;
        else if (checkedId == R.id.chip50Kg) bagWeight = 50;
        else if (checkedId == R.id.chip75Kg) bagWeight = 75;
        else if (checkedId == R.id.chipCustomKg) {
            String customStr = etCustomWeight.getText() != null ? etCustomWeight.getText().toString().trim() : "";
            try {
                bagWeight = Integer.parseInt(customStr);
            } catch (NumberFormatException e) {
                tilCustomWeight.setError("সঠিক ওজন (KG) লিখুন");
                return;
            }
        }

        if (bagWeight <= 0) {
            tilCustomWeight.setError("বস্তার ওজন 0 এর চেয়ে বেশি হতে হবে");
            return;
        }

        sessionManager.setDefaultBagWeight(bagWeight);
        Toast.makeText(requireContext(), "বস্তার ডিফল্ট ওজন ১ বস্তা = " + bagWeight + " KG সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show();

        if (onSavedListener != null) onSavedListener.run();
        dismiss();
    }
}
