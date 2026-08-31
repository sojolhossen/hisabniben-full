package com.sajoldev.hisabniben.dialog;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTransactionDialog extends BottomSheetDialogFragment {
    public static final String MODE_RECEIVE = "RECEIVE";
    public static final String MODE_EXPENSE = "EXPENSE";

    private static final String ARG_INITIAL_MODE = "initial_mode";

    public static AddTransactionDialog newInstance(String initialMode) {
        AddTransactionDialog dialog = new AddTransactionDialog();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_MODE, initialMode);
        dialog.setArguments(args);
        return dialog;
    }

    public static AddTransactionDialog newInstance(List<Customer> customers) {
        return newInstance(MODE_RECEIVE);
    }

    private MaterialButtonToggleGroup toggleGroupMode;
    private MaterialButton btnModeReceive, btnModeExpense;
    private TextView tvHeaderTitle, tvAmountHint, tvCustomerDueBadge, tvSupplierPayableBadge, tvOverpaymentWarning;
    private TextView tvSummaryType, tvSummaryAmount, tvSummarySource, tvSummaryMethod;
    private TextInputEditText etAmount, etReferenceNote;
    private TextInputLayout tilCustomer, tilSupplier, tilExpenseCategory, tilReferenceNote;
    private AutoCompleteTextView actvCustomer, actvSupplier, actvExpenseCategory;
    private ChipGroup chipGroupQuickAmount, cgReceiveSource, cgPaymentMethod;
    private Chip chipQuick500, chipQuick1000, chipQuick2000, chipQuick5000, chipQuick10000;
    private Chip chipSourceCustomer, chipSourceSupplier, chipSourceOtherIncome, chipSourceOwner, chipSourceOther;
    private Chip chipPayCash, chipPayBkash, chipPayNagad, chipPayBank, chipPayOther;
    private Chip chipDatePicker;
    private LinearLayout layoutReceiveSection, layoutExpenseSection, layoutCustomerPicker, layoutSupplierPicker;
    private LinearLayout layoutCustomerDueContainer, layoutSupplierPayableContainer;
    private MaterialCardView cardSummary;
    private MaterialButton btnSaveTransaction;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private String currentMode = MODE_RECEIVE;

    private List<Customer> customers = new ArrayList<>();
    private List<Supplier> suppliers = new ArrayList<>();
    private Customer selectedCustomer;
    private Supplier selectedSupplier;

    private Calendar selectedCalendar = Calendar.getInstance();
    private OnTransactionSavedListener listener;

    private final String[] expenseCategories = {
        Expense.CAT_TRANSPORT,
        Expense.CAT_LABOUR,
        Expense.CAT_LOADING,
        Expense.CAT_UNLOADING,
        Expense.CAT_WAREHOUSE_RENT,
        Expense.CAT_SHOP_RENT,
        Expense.CAT_ELECTRICITY,
        Expense.CAT_MILL,
        Expense.CAT_PACKAGING,
        Expense.CAT_SALARY,
        Expense.CAT_REPAIR,
        Expense.CAT_MOBILE_INTERNET,
        Expense.CAT_PAYMENT_CHARGE,
        Expense.CAT_OTHER
    };

    public interface OnTransactionSavedListener {
        void onTransactionSaved();
    }

    public void setOnTransactionSavedListener(OnTransactionSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_money_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();

        initViews(view);
        setupListeners();
        loadCustomers();
        loadSuppliers();
        setupDatePicker();

        if (getArguments() != null && MODE_EXPENSE.equals(getArguments().getString(ARG_INITIAL_MODE))) {
            toggleGroupMode.check(R.id.btnModeExpense);
            switchMode(MODE_EXPENSE);
        } else {
            switchMode(MODE_RECEIVE);
        }
    }

    private void initViews(View view) {
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        toggleGroupMode = view.findViewById(R.id.toggleGroupMode);
        btnModeReceive = view.findViewById(R.id.btnModeReceive);
        btnModeExpense = view.findViewById(R.id.btnModeExpense);

        tvAmountHint = view.findViewById(R.id.tvAmountHint);
        etAmount = view.findViewById(R.id.etAmount);

        chipGroupQuickAmount = view.findViewById(R.id.chipGroupQuickAmount);
        chipQuick500 = view.findViewById(R.id.chipQuick500);
        chipQuick1000 = view.findViewById(R.id.chipQuick1000);
        chipQuick2000 = view.findViewById(R.id.chipQuick2000);
        chipQuick5000 = view.findViewById(R.id.chipQuick5000);
        chipQuick10000 = view.findViewById(R.id.chipQuick10000);

        layoutReceiveSection = view.findViewById(R.id.layoutReceiveSection);
        cgReceiveSource = view.findViewById(R.id.cgReceiveSource);
        chipSourceCustomer = view.findViewById(R.id.chipSourceCustomer);
        chipSourceSupplier = view.findViewById(R.id.chipSourceSupplier);
        chipSourceOtherIncome = view.findViewById(R.id.chipSourceOtherIncome);
        chipSourceOwner = view.findViewById(R.id.chipSourceOwner);
        chipSourceOther = view.findViewById(R.id.chipSourceOther);

        layoutCustomerPicker = view.findViewById(R.id.layoutCustomerPicker);
        tilCustomer = view.findViewById(R.id.tilCustomer);
        actvCustomer = view.findViewById(R.id.actvCustomer);
        layoutCustomerDueContainer = view.findViewById(R.id.layoutCustomerDueContainer);
        tvCustomerDueBadge = view.findViewById(R.id.tvCustomerDueBadge);
        tvOverpaymentWarning = view.findViewById(R.id.tvOverpaymentWarning);

        layoutSupplierPicker = view.findViewById(R.id.layoutSupplierPicker);
        tilSupplier = view.findViewById(R.id.tilSupplier);
        actvSupplier = view.findViewById(R.id.actvSupplier);
        layoutSupplierPayableContainer = view.findViewById(R.id.layoutSupplierPayableContainer);
        tvSupplierPayableBadge = view.findViewById(R.id.tvSupplierPayableBadge);

        layoutExpenseSection = view.findViewById(R.id.layoutExpenseSection);
        tilExpenseCategory = view.findViewById(R.id.tilExpenseCategory);
        actvExpenseCategory = view.findViewById(R.id.actvExpenseCategory);

        cgPaymentMethod = view.findViewById(R.id.cgPaymentMethod);
        chipPayCash = view.findViewById(R.id.chipPayCash);
        chipPayBkash = view.findViewById(R.id.chipPayBkash);
        chipPayNagad = view.findViewById(R.id.chipPayNagad);
        chipPayBank = view.findViewById(R.id.chipPayBank);
        chipPayOther = view.findViewById(R.id.chipPayOther);

        chipDatePicker = view.findViewById(R.id.chipDatePicker);
        tilReferenceNote = view.findViewById(R.id.tilReferenceNote);
        etReferenceNote = view.findViewById(R.id.etReferenceNote);

        cardSummary = view.findViewById(R.id.cardSummary);
        tvSummaryType = view.findViewById(R.id.tvSummaryType);
        tvSummaryAmount = view.findViewById(R.id.tvSummaryAmount);
        tvSummarySource = view.findViewById(R.id.tvSummarySource);
        tvSummaryMethod = view.findViewById(R.id.tvSummaryMethod);

        btnSaveTransaction = view.findViewById(R.id.btnSaveTransaction);
        progressBar = view.findViewById(R.id.progressBar);

        // Expense categories adapter
        ArrayAdapter<String> expenseAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, expenseCategories);
        actvExpenseCategory.setAdapter(expenseAdapter);
    }

    private void setupListeners() {
        toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnModeExpense) {
                switchMode(MODE_EXPENSE);
            } else {
                switchMode(MODE_RECEIVE);
            }
        });

        // Quick Amount Chips
        chipQuick500.setOnClickListener(v -> setQuickAmount(500));
        chipQuick1000.setOnClickListener(v -> setQuickAmount(1000));
        chipQuick2000.setOnClickListener(v -> setQuickAmount(2000));
        chipQuick5000.setOnClickListener(v -> setQuickAmount(5000));
        chipQuick10000.setOnClickListener(v -> setQuickAmount(10000));

        // Receive Source Toggle
        cgReceiveSource.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipSourceSupplier) {
                layoutCustomerPicker.setVisibility(View.GONE);
                layoutSupplierPicker.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.chipSourceCustomer) {
                layoutCustomerPicker.setVisibility(View.VISIBLE);
                layoutSupplierPicker.setVisibility(View.GONE);
            } else {
                layoutCustomerPicker.setVisibility(View.GONE);
                layoutSupplierPicker.setVisibility(View.GONE);
            }
            updateSummary();
        });

        // Customer Selection Listener
        actvCustomer.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < customers.size()) {
                selectedCustomer = customers.get(position);
                double due = selectedCustomer.getBaki() > 0 ? selectedCustomer.getBaki() : selectedCustomer.getCurrentBalance();
                layoutCustomerDueContainer.setVisibility(View.VISIBLE);
                tvCustomerDueBadge.setText(UnitConverterHelper.formatCurrency(due));
                checkOverpayment();
                updateSummary();
            }
        });

        // Supplier Selection Listener
        actvSupplier.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < suppliers.size()) {
                selectedSupplier = suppliers.get(position);
                layoutSupplierPayableContainer.setVisibility(View.VISIBLE);
                tvSupplierPayableBadge.setText(UnitConverterHelper.formatCurrency(selectedSupplier.getCurrentPayable()));
                updateSummary();
            }
        });

        // Text Watchers for Live Summary & Overpayment check
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkOverpayment();
                updateSummary();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        actvExpenseCategory.setOnItemClickListener((parent, view, position, id) -> updateSummary());
        cgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> updateSummary());

        btnSaveTransaction.setOnClickListener(v -> saveTransaction());
    }

    private void switchMode(String mode) {
        currentMode = mode;
        if (MODE_EXPENSE.equals(mode)) {
            tvHeaderTitle.setText("ব্যবসার খরচ যোগ করুন (Add Expense)");
            tvAmountHint.setText("কত টাকা খরচ হয়েছে?");
            layoutReceiveSection.setVisibility(View.GONE);
            layoutExpenseSection.setVisibility(View.VISIBLE);
            tvSummaryType.setText("খরচ");
            tvSummaryType.setTextColor(getResources().getColor(R.color.error));

            chipQuick500.setText("৳200");
            chipQuick1000.setText("৳500");
            chipQuick2000.setText("৳1,000");
            chipQuick5000.setText("৳2,000");
            chipQuick10000.setText("৳5,000");
        } else {
            tvHeaderTitle.setText("টাকা জমা যোগ করুন (Money Receive)");
            tvAmountHint.setText("কত টাকা জমা হয়েছে?");
            layoutReceiveSection.setVisibility(View.VISIBLE);
            layoutExpenseSection.setVisibility(View.GONE);
            tvSummaryType.setText("টাকা জমা");
            tvSummaryType.setTextColor(getResources().getColor(R.color.brand_green));

            chipQuick500.setText("৳500");
            chipQuick1000.setText("৳1,000");
            chipQuick2000.setText("৳2,000");
            chipQuick5000.setText("৳5,000");
            chipQuick10000.setText("৳10,000");
        }
        updateSummary();
    }

    private void setQuickAmount(double amount) {
        if (MODE_EXPENSE.equals(currentMode)) {
            if (amount == 500) amount = 200;
            else if (amount == 1000) amount = 500;
            else if (amount == 2000) amount = 1000;
            else if (amount == 5000) amount = 2000;
            else if (amount == 10000) amount = 5000;
        }
        etAmount.setText(String.valueOf((long) amount));
        checkOverpayment();
        updateSummary();
    }

    private void checkOverpayment() {
        if (MODE_RECEIVE.equals(currentMode) && cgReceiveSource.getCheckedChipId() == R.id.chipSourceCustomer && selectedCustomer != null) {
            double enteredAmount = parseDouble(etAmount.getText() != null ? etAmount.getText().toString() : "0");
            double currentDue = selectedCustomer.getBaki() > 0 ? selectedCustomer.getBaki() : selectedCustomer.getCurrentBalance();
            if (enteredAmount > currentDue && currentDue > 0) {
                tvOverpaymentWarning.setText("⚠️ এই পরিমাণ টাকা (৳" + String.format("%.0f", enteredAmount) + ") বর্তমান বাকি (৳" + String.format("%.0f", currentDue) + ") থেকে বেশি।");
                tvOverpaymentWarning.setVisibility(View.VISIBLE);
            } else {
                tvOverpaymentWarning.setVisibility(View.GONE);
            }
        } else {
            tvOverpaymentWarning.setVisibility(View.GONE);
        }
    }

    private void updateSummary() {
        double amount = parseDouble(etAmount.getText() != null ? etAmount.getText().toString() : "0");
        tvSummaryAmount.setText(UnitConverterHelper.formatCurrency(amount));

        if (MODE_EXPENSE.equals(currentMode)) {
            String category = actvExpenseCategory.getText() != null ? actvExpenseCategory.getText().toString().trim() : "";
            tvSummarySource.setText(category.isEmpty() ? "খাত নির্বাচন করা হয়নি" : category);
        } else {
            int checkedSource = cgReceiveSource.getCheckedChipId();
            if (checkedSource == R.id.chipSourceCustomer) {
                tvSummarySource.setText(selectedCustomer != null ? selectedCustomer.getName() : "কাস্টমার নির্বাচন করা হয়নি");
            } else if (checkedSource == R.id.chipSourceSupplier) {
                tvSummarySource.setText(selectedSupplier != null ? selectedSupplier.getName() : "মহাজন নির্বাচন করা হয়নি");
            } else if (checkedSource == R.id.chipSourceOtherIncome) {
                tvSummarySource.setText("অন্যান্য আয়");
            } else if (checkedSource == R.id.chipSourceOwner) {
                tvSummarySource.setText("মালিকের বিনিয়োগ");
            } else {
                tvSummarySource.setText("অন্যান্য");
            }
        }

        int checkedMethod = cgPaymentMethod.getCheckedChipId();
        if (checkedMethod == R.id.chipPayBkash) tvSummaryMethod.setText("bKash");
        else if (checkedMethod == R.id.chipPayNagad) tvSummaryMethod.setText("Nagad");
        else if (checkedMethod == R.id.chipPayBank) tvSummaryMethod.setText("Bank");
        else if (checkedMethod == R.id.chipPayOther) tvSummaryMethod.setText("Other");
        else tvSummaryMethod.setText("Cash");
    }

    private void setupDatePicker() {
        updateDateLabel();
        chipDatePicker.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        chipDatePicker.setText("📅 " + sdf.format(selectedCalendar.getTime()));
    }

    private void loadCustomers() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        firestoreManager.getCustomersByUser(userId, new FirestoreManager.FirestoreListCallback<Customer>() {
            @Override
            public void onSuccess(List<Customer> result) {
                result.sort((c1, c2) -> Long.compare(c2.getUpdatedAt(), c1.getUpdatedAt()));
                customers = result;
                List<String> names = new ArrayList<>();
                for (Customer c : customers) {
                    double due = c.getBaki() > 0 ? c.getBaki() : c.getCurrentBalance();
                    names.add(c.getName() + " (বাকি: ৳" + String.format("%.0f", due) + ")");
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                actvCustomer.setAdapter(adapter);
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void loadSuppliers() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        firestoreManager.getSuppliersByUser(userId, new FirestoreManager.FirestoreListCallback<Supplier>() {
            @Override
            public void onSuccess(List<Supplier> result) {
                suppliers = result;
                List<String> names = new ArrayList<>();
                for (Supplier s : suppliers) {
                    names.add(s.getName() + " (পাওনা: ৳" + String.format("%.0f", s.getCurrentPayable()) + ")");
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                actvSupplier.setAdapter(adapter);
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(requireContext(), "টাকার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = parseDouble(amountStr);
        if (amount <= 0) {
            Toast.makeText(requireContext(), "সঠিক টাকার পরিমাণ দিন", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etReferenceNote.getText() != null ? etReferenceNote.getText().toString().trim() : "";
        String paymentMethod = tvSummaryMethod.getText().toString();
        long txDate = selectedCalendar.getTimeInMillis();
        String userId = sessionManager.getUserId();

        showLoading(true);

        if (MODE_EXPENSE.equals(currentMode)) {
            String category = actvExpenseCategory.getText() != null ? actvExpenseCategory.getText().toString().trim() : "";
            if (TextUtils.isEmpty(category)) {
                showLoading(false);
                Toast.makeText(requireContext(), "খরচের খাত নির্বাচন করুন", Toast.LENGTH_SHORT).show();
                return;
            }

            Expense expense = new Expense();
            expense.setUserId(userId);
            expense.setCategory(category);
            expense.setAmount(amount);
            expense.setPaymentMethod(paymentMethod);
            expense.setDate(txDate);
            expense.setDescription(note);

            Transaction tx = new Transaction();
            tx.setUserId(userId);
            tx.setType(Transaction.TYPE_EXPENSE);
            tx.setAmount(amount);
            tx.setPaymentMethod(paymentMethod);
            tx.setNote("ব্যবসার খরচ: " + category + (note.isEmpty() ? "" : " (" + note + ")"));
            tx.setDate(txDate);

            firestoreManager.saveCashFlowExpense(expense, tx, new FirestoreManager.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "লেনদেন সফলভাবে সংরক্ষণ হয়েছে!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onTransactionSaved();
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "লেনদেন সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Receive Mode
            int checkedSource = cgReceiveSource.getCheckedChipId();

            Transaction tx = new Transaction();
            tx.setUserId(userId);
            tx.setAmount(amount);
            tx.setPaymentMethod(paymentMethod);
            tx.setDate(txDate);
            tx.setNote(note);

            String custId = null;
            String suppId = null;

            if (checkedSource == R.id.chipSourceCustomer) {
                if (selectedCustomer == null) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "কাস্টমার নির্বাচন করুন", Toast.LENGTH_SHORT).show();
                    return;
                }
                custId = selectedCustomer.getId();
                tx.setCustomerId(selectedCustomer.getId());
                tx.setCustomerName(selectedCustomer.getName());
                tx.setType(Transaction.TYPE_CUSTOMER_PAYMENT);
            } else if (checkedSource == R.id.chipSourceSupplier) {
                if (selectedSupplier == null) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "মহাজন নির্বাচন করুন", Toast.LENGTH_SHORT).show();
                    return;
                }
                suppId = selectedSupplier.getId();
                tx.setSupplierId(selectedSupplier.getId());
                tx.setSupplierName(selectedSupplier.getName());
                tx.setType(Transaction.TYPE_SUPPLIER_REFUND);
            } else if (checkedSource == R.id.chipSourceOtherIncome) {
                tx.setType(Transaction.TYPE_OTHER_INCOME);
            } else if (checkedSource == R.id.chipSourceOwner) {
                tx.setType(Transaction.TYPE_OWNER_INVESTMENT);
            } else {
                tx.setType("OTHER_RECEIVE");
            }

            firestoreManager.saveCashFlowReceive(tx, custId, suppId, new FirestoreManager.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "লেনদেন সফলভাবে সংরক্ষণ হয়েছে!", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onTransactionSaved();
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "লেনদেন সংরক্ষণ করা যায়নি। আবার চেষ্টা করুন।", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveTransaction.setEnabled(!show);
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }
}
