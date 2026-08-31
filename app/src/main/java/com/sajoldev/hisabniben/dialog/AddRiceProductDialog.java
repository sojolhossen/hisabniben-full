package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;

public class AddRiceProductDialog extends BottomSheetDialogFragment {

    private TextInputEditText etProductName, etBrand, etCustomBagWeight, etPurchaseRate, etSaleRate, etOpeningStock, etMinStockAlert, etNotes;
    private AutoCompleteTextView actvVariety, actvGrade;
    private RadioGroup rgBagWeight;
    private RadioButton rbBag25, rbBag50, rbBag75, rbBagCustom;
    private TextInputLayout tilCustomBagWeight;
    private MaterialButton btnSaveProduct;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private Runnable onSavedListener;

    private final String[] varietyOptions = {"মিনিকোট (Miniket)", "নাজিরশাইল (Nazirshail)", "কাটারীভোগ (Katari)", "বিআর-২৮ (BRRI-28)", "বিআর-২৯ (BRRI-29)", "স্বর্ণা (Swarna)", "জিরোশাইল (Jirashail)", "বাশকাটি (Baskati)", "চিনিগুড়া (Chinigura)", "অন্যান্য (Custom)"};
    private final String[] gradeOptions = {"Premium (প্রিমিয়াম)", "Grade-A (গ্রেড-এ)", "Standard (সাধারণ)"};

    public void setOnProductSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupDropdowns();
        setupListeners();
    }

    private void initViews(View view) {
        etProductName = view.findViewById(R.id.etProductName);
        actvVariety = view.findViewById(R.id.actvVariety);
        etBrand = view.findViewById(R.id.etBrand);
        actvGrade = view.findViewById(R.id.actvGrade);
        rgBagWeight = view.findViewById(R.id.rgBagWeight);
        rbBag25 = view.findViewById(R.id.rbBag25);
        rbBag50 = view.findViewById(R.id.rbBag50);
        rbBag75 = view.findViewById(R.id.rbBag75);
        rbBagCustom = view.findViewById(R.id.rbBagCustom);
        tilCustomBagWeight = view.findViewById(R.id.tilCustomBagWeight);
        etCustomBagWeight = view.findViewById(R.id.etCustomBagWeight);
        etPurchaseRate = view.findViewById(R.id.etPurchaseRate);
        etSaleRate = view.findViewById(R.id.etSaleRate);
        etOpeningStock = view.findViewById(R.id.etOpeningStock);
        etMinStockAlert = view.findViewById(R.id.etMinStockAlert);
        etNotes = view.findViewById(R.id.etNotes);
        btnSaveProduct = view.findViewById(R.id.btnSaveProduct);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupDropdowns() {
        ArrayAdapter<String> varietyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, varietyOptions);
        actvVariety.setAdapter(varietyAdapter);

        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, gradeOptions);
        actvGrade.setAdapter(gradeAdapter);
    }

    private void setupListeners() {
        rgBagWeight.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBagCustom) {
                tilCustomBagWeight.setVisibility(View.VISIBLE);
            } else {
                tilCustomBagWeight.setVisibility(View.GONE);
            }
        });

        btnSaveProduct.setOnClickListener(v -> saveRiceProduct());
    }

    private void saveRiceProduct() {
        String name = etProductName.getText() != null ? etProductName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            etProductName.setError("চালের নাম লিখুন");
            return;
        }

        String variety = actvVariety.getText().toString().trim();
        if (variety.isEmpty()) {
            actvVariety.setError("চালের জাত নির্বাচন করুন");
            return;
        }

        String saleRateStr = etSaleRate.getText() != null ? etSaleRate.getText().toString().trim() : "";
        if (saleRateStr.isEmpty()) {
            etSaleRate.setError("বিক্রয় মূল্য লিখুন");
            return;
        }

        double saleRate = parseDouble(saleRateStr);
        if (saleRate <= 0) {
            etSaleRate.setError("সঠিক বিক্রয় মূল্য দিন");
            return;
        }

        double purchaseRate = parseDouble(etPurchaseRate.getText() != null ? etPurchaseRate.getText().toString() : "0");
        double openingStockKg = parseDouble(etOpeningStock.getText() != null ? etOpeningStock.getText().toString() : "0");
        double minStockAlertKg = parseDouble(etMinStockAlert.getText() != null ? etMinStockAlert.getText().toString() : "100");

        double defaultBagWeight = 50.0;
        int checkedBagId = rgBagWeight.getCheckedRadioButtonId();
        if (checkedBagId == R.id.rbBag25) {
            defaultBagWeight = 25.0;
        } else if (checkedBagId == R.id.rbBag50) {
            defaultBagWeight = 50.0;
        } else if (checkedBagId == R.id.rbBag75) {
            defaultBagWeight = 75.0;
        } else if (checkedBagId == R.id.rbBagCustom) {
            defaultBagWeight = parseDouble(etCustomBagWeight.getText() != null ? etCustomBagWeight.getText().toString() : "50");
            if (defaultBagWeight <= 0) defaultBagWeight = 50.0;
        }

        btnSaveProduct.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        RiceProduct product = new RiceProduct();
        product.setUserId(sessionManager.getUserId());
        product.setName(name);
        product.setVariety(variety);
        product.setBrand(etBrand.getText() != null ? etBrand.getText().toString().trim() : "");
        product.setGrade(actvGrade.getText().toString().trim());
        product.setDefaultBagWeight(defaultBagWeight);
        product.setPurchaseRatePerKg(purchaseRate);
        product.setSaleRatePerKg(saleRate);
        product.setCurrentStockKg(openingStockKg);
        product.setMinStockAlertKg(minStockAlertKg);
        product.setNotes(etNotes.getText() != null ? etNotes.getText().toString().trim() : "");

        FirestoreManager.getInstance().createRiceProduct(product, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "নতুন চাল সফলভাবে যোগ করা হয়েছে!", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.run();
                dismiss();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnSaveProduct.setEnabled(true);
                Toast.makeText(requireContext(), "ত্রুটি: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
