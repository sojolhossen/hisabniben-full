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
import android.widget.Button;
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
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.SaleItem;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class AddSaleDialog extends BottomSheetDialogFragment {

    private AutoCompleteTextView actvCustomer, actvRiceProduct;
    private MaterialButton btnCashCustomer, btnAddItem, btnSaveSale;
    private RadioGroup rgMode;
    private RadioButton rbBagsMode, rbKgMode;
    private LinearLayout layoutBagsInputs, containerItems;
    private TextInputLayout tilDirectKg;
    private TextInputEditText etBagCount, etBagWeight, etDirectKg, etSaleRate, etDiscount, etTransportCharge, etPaidAmount, etNotes;
    private TextView tvStockInfo, tvCalculatedTotalKg, tvNoItems, tvSubtotal, tvGrandTotal, tvDueAmount;
    private ProgressBar progressBar;

    private List<Customer> customerList = new ArrayList<>();
    private List<RiceProduct> riceProductList = new ArrayList<>();
    private Customer selectedCustomer;
    private RiceProduct selectedProduct;
    private List<SaleItem> addedItems = new ArrayList<>();

    private SessionManager sessionManager;
    private Runnable onSavedListener;

    public void setOnSaleSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_rice_sale, container, false);
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
        actvCustomer = view.findViewById(R.id.actvCustomer);
        btnCashCustomer = view.findViewById(R.id.btnCashCustomer);
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
        etSaleRate = view.findViewById(R.id.etSaleRate);
        tvCalculatedTotalKg = view.findViewById(R.id.tvCalculatedTotalKg);
        btnAddItem = view.findViewById(R.id.btnAddItem);
        containerItems = view.findViewById(R.id.containerItems);
        tvNoItems = view.findViewById(R.id.tvNoItems);

        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        etDiscount = view.findViewById(R.id.etDiscount);
        etTransportCharge = view.findViewById(R.id.etTransportCharge);
        tvGrandTotal = view.findViewById(R.id.tvGrandTotal);
        etPaidAmount = view.findViewById(R.id.etPaidAmount);
        tvDueAmount = view.findViewById(R.id.tvDueAmount);
        etNotes = view.findViewById(R.id.etNotes);
        btnSaveSale = view.findViewById(R.id.btnSaveSale);
        progressBar = view.findViewById(R.id.progressBar);

        setCashCustomerSelected();
    }

    private void setupListeners() {
        btnCashCustomer.setOnClickListener(v -> setCashCustomerSelected());

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
        etDiscount.addTextChangedListener(financialWatcher);
        etTransportCharge.addTextChangedListener(financialWatcher);
        etPaidAmount.addTextChangedListener(financialWatcher);

        btnAddItem.setOnClickListener(v -> addRiceItemToList());
        btnSaveSale.setOnClickListener(v -> confirmAndSaveSale());
    }

    private void setCashCustomerSelected() {
        selectedCustomer = null;
        actvCustomer.setText("ক্যাশ কাস্টমার (Cash Customer)", false);
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

        FirestoreManager.getInstance().getCustomersByUser(userId, new FirestoreManager.FirestoreListCallback<Customer>() {
            @Override
            public void onSuccess(List<Customer> result) {
                customerList = result;
                List<String> names = new ArrayList<>();
                names.add("ক্যাশ কাস্টমার (Cash Customer)");
                for (Customer c : customerList) names.add(c.getName() + " (" + c.getPhone() + ")");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                actvCustomer.setAdapter(adapter);
                actvCustomer.setOnItemClickListener((parent, view, position, id) -> {
                    if (position == 0) {
                        setCashCustomerSelected();
                    } else {
                        selectedCustomer = customerList.get(position - 1);
                    }
                });
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
                    etSaleRate.setText(String.valueOf(selectedProduct.getSellingRatePerKg()));
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

        double availableStockKg = selectedProduct.getCurrentStockKg();
        if (requestedKg > availableStockKg) {
            Toast.makeText(requireContext(), "স্টকে পর্যাপ্ত চাল নেই! বর্তমান স্টক: " + UnitConverterHelper.formatKg(availableStockKg), Toast.LENGTH_LONG).show();
            return;
        }

        double saleRatePerKg = parseDouble(etSaleRate.getText() != null ? etSaleRate.getText().toString() : "0");
        if (saleRatePerKg <= 0) {
            Toast.makeText(requireContext(), "বিক্রয় মূল্য লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        String mode = rbBagsMode.isChecked() ? SaleItem.MODE_BAGS : SaleItem.MODE_KG;
        double bagCount = parseDouble(etBagCount.getText() != null ? etBagCount.getText().toString() : "0");
        double bagWeight = parseDouble(etBagWeight.getText() != null ? etBagWeight.getText().toString() : "50");

        SaleItem item = new SaleItem(
            selectedProduct.getId(),
            selectedProduct.getName(),
            selectedProduct.getVariety(),
            mode,
            bagCount,
            bagWeight,
            requestedKg,
            saleRatePerKg,
            selectedProduct.getPurchaseRatePerKg()
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
            SaleItem item = addedItems.get(i);

            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_rice_sale_row, containerItems, false);
            TextView tvProductName = itemView.findViewById(R.id.tvProductName);
            TextView tvDetails = itemView.findViewById(R.id.tvDetails);
            TextView tvTotal = itemView.findViewById(R.id.tvTotal);
            ImageView btnRemove = itemView.findViewById(R.id.btnRemove);

            tvProductName.setText(item.getProductNameSnapshot() + " (" + item.getVarietySnapshot() + ")");
            String qtyDesc = SaleItem.MODE_BAGS.equals(item.getQuantityMode()) 
                ? (int)item.getBagQuantity() + " বস্তা (" + UnitConverterHelper.formatKg(item.getTotalKg()) + ")"
                : UnitConverterHelper.formatKg(item.getTotalKg());
            
            tvDetails.setText(qtyDesc + " @ " + UnitConverterHelper.formatCurrency(item.getSaleRatePerKg()) + "/KG");
            tvTotal.setText(UnitConverterHelper.formatCurrency(item.getItemTotal()));

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
        for (SaleItem item : addedItems) {
            subtotal += item.getItemTotal();
        }

        double discount = parseDouble(etDiscount.getText() != null ? etDiscount.getText().toString() : "0");
        double transport = parseDouble(etTransportCharge.getText() != null ? etTransportCharge.getText().toString() : "0");

        double grandTotal = Math.max(0, subtotal - discount + transport);
        
        String paidStr = etPaidAmount.getText() != null ? etPaidAmount.getText().toString().trim() : "";
        double paid = paidStr.isEmpty() ? grandTotal : parseDouble(paidStr);

        double due = Math.max(0, grandTotal - paid);

        tvSubtotal.setText(UnitConverterHelper.formatCurrency(subtotal));
        tvGrandTotal.setText(UnitConverterHelper.formatCurrency(grandTotal));
        tvDueAmount.setText(UnitConverterHelper.formatCurrency(due));
    }

    private void confirmAndSaveSale() {
        if (addedItems.isEmpty()) {
            Toast.makeText(requireContext(), "বিক্রির জন্য অন্তত একটি চাউলের আইটেম যোগ করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        double calcSubtotal = 0;
        double calcProfit = 0;
        for (SaleItem item : addedItems) {
            calcSubtotal += item.getItemTotal();
            calcProfit += item.getEstimatedProfit();
        }

        final double subtotal = calcSubtotal;
        final double estimatedProfit = calcProfit;
        final double discount = parseDouble(etDiscount.getText() != null ? etDiscount.getText().toString() : "0");
        final double transport = parseDouble(etTransportCharge.getText() != null ? etTransportCharge.getText().toString() : "0");
        final double grandTotal = Math.max(0, subtotal - discount + transport);

        String paidStr = etPaidAmount.getText() != null ? etPaidAmount.getText().toString().trim() : "";
        final double paidAmount = paidStr.isEmpty() ? grandTotal : parseDouble(paidStr);
        final double dueAmount = Math.max(0, grandTotal - paidAmount);

        final String customerName = selectedCustomer != null ? selectedCustomer.getName() : "ক্যাশ কাস্টমার (Cash Customer)";
        final String customerId = selectedCustomer != null ? selectedCustomer.getId() : Sale.CASH_CUSTOMER_ID;
        final String customerPhone = selectedCustomer != null ? selectedCustomer.getPhone() : "";

        // Build Confirmation Dialog
        new AlertDialog.Builder(requireContext())
            .setTitle("বিক্রির সারাংশ (Sale Summary)")
            .setMessage("ক্রেতা: " + customerName +
                "\nআইটেম সংখ্যা: " + addedItems.size() +
                "\nসর্বমোট মূল্য: " + UnitConverterHelper.formatCurrency(grandTotal) +
                "\nজমা: " + UnitConverterHelper.formatCurrency(paidAmount) +
                "\nবর্তমান বাকি: " + UnitConverterHelper.formatCurrency(dueAmount) +
                "\n\nআপনি কি চাল বিক্রি নিশ্চিত করতে চান?")
            .setPositiveButton("হ্যাঁ, বিক্রি করুন", (dialog, which) -> executeSaveSale(customerId, customerName, customerPhone, subtotal, discount, transport, grandTotal, paidAmount, dueAmount, estimatedProfit))
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void executeSaveSale(String customerId, String customerName, String customerPhone, double subtotal, double discount, double transport, double grandTotal, double paidAmount, double dueAmount, double estimatedProfit) {
        btnSaveSale.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Sale sale = new Sale();
        sale.setUserId(sessionManager.getUserId());
        sale.setCustomerId(customerId);
        sale.setCustomerName(customerName);
        sale.setCustomerPhone(customerPhone);
        sale.setItems(addedItems);
        sale.setGrossAmount(subtotal);
        sale.setDiscount(discount);
        sale.setTransportCharge(transport);
        sale.setGrandTotal(grandTotal);
        sale.setPaidAmount(paidAmount);
        sale.setDueAmount(dueAmount);
        sale.setEstimatedProfit(estimatedProfit);
        sale.setSaleDate(System.currentTimeMillis());
        sale.setNotes(etNotes.getText() != null ? etNotes.getText().toString().trim() : "");
        sale.calculatePaymentStatus();

        FirestoreManager.getInstance().createSale(sale, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "চাল বিক্রি সফলভাবে সম্পন্ন হয়েছে!", Toast.LENGTH_LONG).show();

                if (customerPhone != null && !customerPhone.trim().isEmpty()) {
                    double totalBags = 0;
                    double totalKg = 0;
                    for (SaleItem item : addedItems) {
                        totalBags += item.getBagQuantity();
                        totalKg += item.getTotalKg();
                    }
                    String bagsDesc = (totalBags > 0) ? ((int) totalBags + " বস্তা") : "চাল";
                    String bizName = com.sajoldev.hisabniben.util.SmsTemplateManager.getEffectiveSmsBusinessName(null, sessionManager);
                    String smsMessage = com.sajoldev.hisabniben.util.SmsTemplateManager.buildSaleSms(
                        customerName,
                        bagsDesc,
                        totalKg,
                        grandTotal,
                        paidAmount,
                        dueAmount,
                        bizName
                    );

                    com.sajoldev.hisabniben.util.SmsSenderHelper.sendSms(
                        requireContext(),
                        customerPhone,
                        customerName,
                        smsMessage,
                        "transaction",
                        "sale",
                        null
                    );
                }

                if (onSavedListener != null) onSavedListener.run();
                dismiss();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                btnSaveSale.setEnabled(true);
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
