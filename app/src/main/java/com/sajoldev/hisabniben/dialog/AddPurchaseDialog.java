package com.sajoldev.hisabniben.dialog;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.PurchaseItem;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class AddPurchaseDialog extends BottomSheetDialogFragment {

    private AutoCompleteTextView actvSupplier, actvRiceProduct;
    private MaterialButton btnAddSupplier, btnAddItem, btnSavePurchase;
    private RadioGroup rgMode;
    private RadioButton rbBagsMode, rbKgMode;
    private LinearLayout layoutBagsInputs, containerItems;
    private TextInputLayout tilDirectKg;
    private TextInputEditText etBagCount, etBagWeight, etDirectKg, etPurchaseRate, etTransportCost, etLabourCost, etPaidAmount, etNotes;
    private TextView tvStockInfo, tvCalculatedTotalKg, tvNoItems, tvSubtotal, tvGrandTotal, tvDueAmount;
    private ProgressBar progressBar;

    private List<Supplier> supplierList = new ArrayList<>();
    private List<RiceProduct> riceProductList = new ArrayList<>();
    private Supplier selectedSupplier;
    private RiceProduct selectedProduct;
    private List<PurchaseItem> addedItems = new ArrayList<>();

    private SessionManager sessionManager;
    private Runnable onSavedListener;

    public void setOnPurchaseSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_rice_purchase, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
            android.widget.FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupListeners();
        loadDropdownData();
    }

    private void initViews(View view) {
        actvSupplier = view.findViewById(R.id.actvSupplier);
        btnAddSupplier = view.findViewById(R.id.btnAddSupplier);
        actvRiceProduct = view.findViewById(R.id.actvRiceProduct);
        tvStockInfo = view.findViewById(R.id.tvStockInfo);
        rgMode = view.findViewById(R.id.rgMode);
        rbBagsMode = view.findViewById(R.id.rbBagsMode);
        rbKgMode = view.findViewById(R.id.rbKgMode);
        layoutBagsInputs = view.findViewById(R.id.layoutBagsInputs);
        tilDirectKg = view.findViewById(R.id.tilDirectKg);
        etBagCount = view.findViewById(R.id.etBagCount);
        etBagWeight = view.findViewById(R.id.etBagWeight);
        etDirectKg = view.findViewById(R.id.etDirectKg);
        etPurchaseRate = view.findViewById(R.id.etPurchaseRate);
        tvCalculatedTotalKg = view.findViewById(R.id.tvCalculatedTotalKg);
        btnAddItem = view.findViewById(R.id.btnAddItem);
        containerItems = view.findViewById(R.id.containerItems);
        tvNoItems = view.findViewById(R.id.tvNoItems);

        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        etTransportCost = view.findViewById(R.id.etTransportCost);
        etLabourCost = view.findViewById(R.id.etLabourCost);
        tvGrandTotal = view.findViewById(R.id.tvGrandTotal);
        etPaidAmount = view.findViewById(R.id.etPaidAmount);
        tvDueAmount = view.findViewById(R.id.tvDueAmount);
        etNotes = view.findViewById(R.id.etNotes);
        btnSavePurchase = view.findViewById(R.id.btnSavePurchase);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnAddSupplier.setOnClickListener(v -> {
            AddSupplierDialog dialog = new AddSupplierDialog();
            dialog.setOnSupplierSavedListener(this::loadDropdownData);
            dialog.show(getChildFragmentManager(), "AddSupplierInline");
        });

        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBagsMode) {
                layoutBagsInputs.setVisibility(View.VISIBLE);
                tilDirectKg.setVisibility(View.GONE);
            } else {
                layoutBagsInputs.setVisibility(View.GONE);
                tilDirectKg.setVisibility(View.VISIBLE);
            }
            updateCalculatedItemTotalKg();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateCalculatedItemTotalKg(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etBagCount.addTextChangedListener(watcher);
        etBagWeight.addTextChangedListener(watcher);
        etDirectKg.addTextChangedListener(watcher);

        TextWatcher financialWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateFinancialTotals(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        etTransportCost.addTextChangedListener(financialWatcher);
        etLabourCost.addTextChangedListener(financialWatcher);
        etPaidAmount.addTextChangedListener(financialWatcher);

        btnAddItem.setOnClickListener(v -> addRiceItemToList());
        btnSavePurchase.setOnClickListener(v -> confirmAndSavePurchase());
    }

    private void updateCalculatedItemTotalKg() {
        double totalKg = getItemCalculatedTotalKg();
        tvCalculatedTotalKg.setText("মোট: " + UnitConverterHelper.formatKg(totalKg));
    }

    private double getItemCalculatedTotalKg() {
        if (rbBagsMode.isChecked()) {
            double bags = parseDouble(etBagCount.getText() != null ? etBagCount.getText().toString() : "0");
            double weight = parseDouble(etBagWeight.getText() != null ? etBagWeight.getText().toString() : "50");
            return bags * weight;
        } else {
            return parseDouble(etDirectKg.getText() != null ? etDirectKg.getText().toString() : "0");
        }
    }

    private void loadDropdownData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        FirestoreManager.getInstance().getSuppliersByUser(userId, new FirestoreManager.FirestoreListCallback<Supplier>() {
            @Override
            public void onSuccess(List<Supplier> result) {
                supplierList = result;
                List<String> names = new ArrayList<>();
                for (Supplier s : supplierList) names.add(s.getName() + " (" + s.getPhone() + ")");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                actvSupplier.setAdapter(adapter);
                actvSupplier.setOnItemClickListener((parent, view, position, id) -> selectedSupplier = supplierList.get(position));
            }
            @Override public void onFailure(String error) {}
        });

        FirestoreManager.getInstance().getRiceProductsByUser(userId, new FirestoreManager.FirestoreListCallback<RiceProduct>() {
            @Override
            public void onSuccess(List<RiceProduct> result) {
                riceProductList = result;
                List<String> names = new ArrayList<>();
                for (RiceProduct p : riceProductList) {
                    names.add(p.getName() + " [" + p.getVariety() + "]");
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                actvRiceProduct.setAdapter(adapter);
                actvRiceProduct.setOnItemClickListener((parent, view, position, id) -> {
                    selectedProduct = riceProductList.get(position);
                    tvStockInfo.setText("বর্তমান স্টক: " + UnitConverterHelper.formatStockBagsAndKg(selectedProduct.getCurrentStockKg(), selectedProduct.getDefaultBagWeight()));
                    etPurchaseRate.setText(String.valueOf(selectedProduct.getPurchaseRatePerKg()));
                    etBagWeight.setText(String.valueOf(selectedProduct.getDefaultBagWeight()));
                });
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void addRiceItemToList() {
        if (selectedProduct == null) {
            Toast.makeText(requireContext(), "চাল পণ্য নির্বাচন করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        double requestedKg = getItemCalculatedTotalKg();
        if (requestedKg <= 0) {
            Toast.makeText(requireContext(), "সঠিক কেজির পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        double purchaseRatePerKg = parseDouble(etPurchaseRate.getText() != null ? etPurchaseRate.getText().toString() : "0");
        if (purchaseRatePerKg <= 0) {
            Toast.makeText(requireContext(), "ক্রয় মূল্য লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        String mode = rbBagsMode.isChecked() ? PurchaseItem.MODE_BAGS : PurchaseItem.MODE_KG;
        double bagCount = parseDouble(etBagCount.getText() != null ? etBagCount.getText().toString() : "0");
        double bagWeight = parseDouble(etBagWeight.getText() != null ? etBagWeight.getText().toString() : "50");

        PurchaseItem item = new PurchaseItem(
            selectedProduct.getId(),
            selectedProduct.getName(),
            selectedProduct.getVariety(),
            mode,
            bagCount,
            bagWeight,
            requestedKg,
            purchaseRatePerKg
        );

        addedItems.add(item);
        renderItemsList();
        calculateFinancialTotals();

        // Reset Product Input Fields
        actvRiceProduct.setText("", false);
        selectedProduct = null;
        tvStockInfo.setText("বর্তমান স্টক: 0 KG");
    }

    private void renderItemsList() {
        containerItems.removeAllViews();
        if (addedItems.isEmpty()) {
            containerItems.addView(tvNoItems);
            return;
        }

        for (int i = 0; i < addedItems.size(); i++) {
            final int index = i;
            PurchaseItem item = addedItems.get(i);

            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_rice_purchase_row, containerItems, false);
            TextView tvProductName = itemView.findViewById(R.id.tvProductName);
            TextView tvDetails = itemView.findViewById(R.id.tvDetails);
            TextView tvTotal = itemView.findViewById(R.id.tvTotal);
            ImageView btnRemove = itemView.findViewById(R.id.btnRemove);

            tvProductName.setText(item.getProductNameSnapshot() + " (" + item.getVarietySnapshot() + ")");
            String qtyDesc = PurchaseItem.MODE_BAGS.equals(item.getQuantityMode()) 
                ? (int)item.getBagQuantity() + " বস্তা (" + UnitConverterHelper.formatKg(item.getTotalKg()) + ")"
                : UnitConverterHelper.formatKg(item.getTotalKg());
            
            tvDetails.setText(qtyDesc + " @ " + UnitConverterHelper.formatCurrency(item.getPurchaseRatePerKg()) + "/KG");
            tvTotal.setText(UnitConverterHelper.formatCurrency(item.getTotalAmount()));

            btnRemove.setOnClickListener(v -> {
                addedItems.remove(index);
                renderItemsList();
                calculateFinancialTotals();
            });

            containerItems.addView(itemView);
        }
    }

    private void calculateFinancialTotals() {
        double subtotal = 0;
        double totalKgSum = 0;
        for (PurchaseItem item : addedItems) {
            subtotal += item.getTotalAmount();
            totalKgSum += item.getTotalKg();
        }

        double transport = parseDouble(etTransportCost.getText() != null ? etTransportCost.getText().toString() : "0");
        double labour = parseDouble(etLabourCost.getText() != null ? etLabourCost.getText().toString() : "0");
        double directCostTotal = transport + labour;

        // Allocate direct acquisition cost proportionally to items for WAC
        if (totalKgSum > 0) {
            for (PurchaseItem item : addedItems) {
                double allocatedCost = (item.getTotalKg() / totalKgSum) * directCostTotal;
                item.setDirectCostAllocation(allocatedCost);
            }
        }

        double grandTotal = subtotal + directCostTotal;
        
        String paidStr = etPaidAmount.getText() != null ? etPaidAmount.getText().toString().trim() : "";
        double paid = paidStr.isEmpty() ? grandTotal : parseDouble(paidStr);

        double due = Math.max(0, grandTotal - paid);

        tvSubtotal.setText(UnitConverterHelper.formatCurrency(subtotal));
        tvGrandTotal.setText(UnitConverterHelper.formatCurrency(grandTotal));
        tvDueAmount.setText(UnitConverterHelper.formatCurrency(due));
    }

    private void confirmAndSavePurchase() {
        if (selectedSupplier == null) {
            Toast.makeText(requireContext(), "সাপ্লায়ার / চাল মহাজন নির্বাচন করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        if (addedItems.isEmpty()) {
            Toast.makeText(requireContext(), "চাল ক্রয়ের জন্য অন্তত একটি আইটেম যোগ করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        double calcSubtotal = 0;
        for (PurchaseItem item : addedItems) {
            calcSubtotal += item.getTotalAmount();
        }

        final double subtotal = calcSubtotal;
        final double transport = parseDouble(etTransportCost.getText() != null ? etTransportCost.getText().toString() : "0");
        final double labour = parseDouble(etLabourCost.getText() != null ? etLabourCost.getText().toString() : "0");
        final double grandTotal = subtotal + transport + labour;

        String paidStr = etPaidAmount.getText() != null ? etPaidAmount.getText().toString().trim() : "";
        final double paidAmount = paidStr.isEmpty() ? grandTotal : parseDouble(paidStr);
        final double dueAmount = Math.max(0, grandTotal - paidAmount);

        final String supplierName = selectedSupplier.getName();
        final String supplierId = selectedSupplier.getId();
        final String supplierPhone = selectedSupplier.getPhone();

        // Build Confirmation Dialog
        new AlertDialog.Builder(requireContext())
            .setTitle("ক্রয়ের সারাংশ (Purchase Summary)")
            .setMessage("সাপ্লায়ার: " + supplierName +
                "\nআইটেম সংখ্যা: " + addedItems.size() +
                "\nপণ্য মূল্য: " + UnitConverterHelper.formatCurrency(subtotal) +
                "\nপরিবহন ও লেবার: " + UnitConverterHelper.formatCurrency(transport + labour) +
                "\nসর্বমোট মূল্য: " + UnitConverterHelper.formatCurrency(grandTotal) +
                "\nপরিশোধ: " + UnitConverterHelper.formatCurrency(paidAmount) +
                "\nসাপ্লায়ার পাওনা: " + UnitConverterHelper.formatCurrency(dueAmount) +
                "\n\nআপনি কি চাল ক্রয় নিশ্চিত করতে চান?")
            .setPositiveButton("হ্যাঁ, ক্রয় করুন", (dialog, which) -> executeSavePurchase(supplierId, supplierName, supplierPhone, subtotal, transport, labour, grandTotal, paidAmount, dueAmount))
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void executeSavePurchase(String supplierId, String supplierName, String supplierPhone, double subtotal, double transport, double labour, double grandTotal, double paidAmount, double dueAmount) {
        btnSavePurchase.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Purchase purchase = new Purchase();
        purchase.setUserId(sessionManager.getUserId());
        purchase.setSupplierId(supplierId);
        purchase.setSupplierName(supplierName);
        purchase.setSupplierPhone(supplierPhone);
        purchase.setItems(addedItems);
        purchase.setGrossAmount(subtotal);
        purchase.setTransportCost(transport);
        purchase.setLabourCost(labour);
        purchase.setGrandTotal(grandTotal);
        purchase.setPaidAmount(paidAmount);
        purchase.setDueAmount(dueAmount);
        purchase.setPurchaseDate(System.currentTimeMillis());
        purchase.setNotes(etNotes.getText() != null ? etNotes.getText().toString().trim() : "");
        purchase.calculatePaymentStatus();

        FirestoreManager.getInstance().createPurchase(purchase, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "চাল ক্রয় সফলভাবে সম্পন্ন হয়েছে!", Toast.LENGTH_LONG).show();

                if (supplierPhone != null && !supplierPhone.trim().isEmpty()) {
                    double totalBags = 0;
                    double totalKg = 0;
                    for (PurchaseItem item : addedItems) {
                        totalBags += item.getBagQuantity();
                        totalKg += item.getTotalKg();
                    }
                    String bagsDesc = (totalBags > 0) ? ((int) totalBags + " বস্তা") : "চাল";
                    String bizName = com.sajoldev.hisabniben.util.SmsTemplateManager.getEffectiveSmsBusinessName(null, sessionManager);
                    String smsMessage = com.sajoldev.hisabniben.util.SmsTemplateManager.buildPurchaseSms(
                        supplierName,
                        bagsDesc,
                        totalKg,
                        grandTotal,
                        paidAmount,
                        dueAmount,
                        bizName
                    );

                    com.sajoldev.hisabniben.util.SmsSenderHelper.sendSms(
                        requireContext(),
                        supplierPhone,
                        supplierName,
                        smsMessage,
                        "transaction",
                        "purchase",
                        null
                    );
                }

                if (onSavedListener != null) onSavedListener.run();
                dismiss();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnSavePurchase.setEnabled(true);
                Toast.makeText(requireContext(), "ত্রুটি: " + error, Toast.LENGTH_LONG).show();
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
