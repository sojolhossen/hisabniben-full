package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomerPaymentDialog extends BottomSheetDialogFragment {

    private Customer customer;
    private Runnable onSavedListener;

    private TextView tvPaymentCustomerName, tvPaymentCustomerPhone, tvPaymentCurrentDue;
    private TextInputLayout tilPaymentAmount, tilPaymentRef, tilPaymentNotes;
    private TextInputEditText etPaymentAmount, etPaymentRef, etPaymentNotes;
    private ChipGroup chipGroupPaymentMethod;
    private MaterialButton btnSubmitPayment;

    private SessionManager sessionManager;
    private FirebaseFirestore db;

    public static CustomerPaymentDialog newInstance(Customer customer) {
        CustomerPaymentDialog dialog = new CustomerPaymentDialog();
        Bundle args = new Bundle();
        args.putSerializable("customer", customer);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnPaymentSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customer = (Customer) getArguments().getSerializable("customer");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_customer_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());
        db = FirebaseFirestore.getInstance();

        initViews(view);
    }

    private void initViews(View view) {
        tvPaymentCustomerName = view.findViewById(R.id.tvPaymentCustomerName);
        tvPaymentCustomerPhone = view.findViewById(R.id.tvPaymentCustomerPhone);
        tvPaymentCurrentDue = view.findViewById(R.id.tvPaymentCurrentDue);

        tilPaymentAmount = view.findViewById(R.id.tilPaymentAmount);
        etPaymentAmount = view.findViewById(R.id.etPaymentAmount);
        chipGroupPaymentMethod = view.findViewById(R.id.chipGroupPaymentMethod);
        tilPaymentRef = view.findViewById(R.id.tilPaymentRef);
        etPaymentRef = view.findViewById(R.id.etPaymentRef);
        tilPaymentNotes = view.findViewById(R.id.tilPaymentNotes);
        etPaymentNotes = view.findViewById(R.id.etPaymentNotes);
        btnSubmitPayment = view.findViewById(R.id.btnSubmitPayment);

        if (customer != null) {
            String businessName = customer.getBusinessName() != null && !customer.getBusinessName().isEmpty() ? customer.getBusinessName() : customer.getName();
            tvPaymentCustomerName.setText(businessName);
            tvPaymentCustomerPhone.setText("📞 " + (customer.getPhone() != null ? customer.getPhone() : ""));
            tvPaymentCurrentDue.setText(UnitConverterHelper.formatCurrency(customer.getBaki()));
        }

        btnSubmitPayment.setOnClickListener(v -> submitPayment());
    }

    private void submitPayment() {
        if (customer == null || customer.getId() == null) return;

        String amountStr = etPaymentAmount.getText() != null ? etPaymentAmount.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            tilPaymentAmount.setError("টাকার পরিমাণ লিখুন");
            return;
        }

        double paymentAmount = 0;
        try {
            paymentAmount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            tilPaymentAmount.setError("সঠিক পরিমাণ লিখুন");
            return;
        }

        if (paymentAmount <= 0) {
            tilPaymentAmount.setError("পরিমাণ 0 এর চেয়ে বেশি হতে হবে");
            return;
        }

        tilPaymentAmount.setError(null);
        btnSubmitPayment.setEnabled(false);

        String paymentMethod = "Cash";
        int checkedChip = chipGroupPaymentMethod.getCheckedChipId();
        if (checkedChip == R.id.chipPayBkash) paymentMethod = "bKash";
        else if (checkedChip == R.id.chipPayNagad) paymentMethod = "Nagad";
        else if (checkedChip == R.id.chipPayBank) paymentMethod = "Bank";
        else if (checkedChip == R.id.chipPayOther) paymentMethod = "Other";

        String reference = etPaymentRef.getText() != null ? etPaymentRef.getText().toString().trim() : "";
        String notes = etPaymentNotes.getText() != null ? etPaymentNotes.getText().toString().trim() : "";

        final double finalPaymentAmount = paymentAmount;
        final String finalMethod = paymentMethod;

        db.runTransaction(transaction -> {
            // Read customer
            com.google.firebase.firestore.DocumentReference customerRef = db.collection("customers").document(customer.getId());
            com.google.firebase.firestore.DocumentSnapshot customerSnap = transaction.get(customerRef);

            String targetAccId = com.sajoldev.hisabniben.util.FirestoreManager.resolveAccountIdForMethod(finalMethod, null);
            com.google.firebase.firestore.DocumentReference walletAccRef = db.collection("users")
                .document(sessionManager.getUserId())
                .collection("walletAccounts")
                .document(targetAccId);
            com.google.firebase.firestore.DocumentSnapshot walletAccSnap = transaction.get(walletAccRef);

            double currentBaki = customerSnap.exists() && customerSnap.getDouble("baki") != null ? customerSnap.getDouble("baki") : customer.getBaki();
            double newBaki = currentBaki - finalPaymentAmount;

            transaction.update(customerRef, "baki", newBaki, "updatedAt", System.currentTimeMillis());

            // Wallet Balance Update & Transaction Log
            if (walletAccSnap != null && walletAccSnap.exists()) {
                com.sajoldev.hisabniben.model.WalletAccount walletAcc = walletAccSnap.toObject(com.sajoldev.hisabniben.model.WalletAccount.class);
                if (walletAcc != null) {
                    double oldBal = walletAcc.getCurrentBalance();
                    double newBal = oldBal + finalPaymentAmount;
                    transaction.update(walletAccRef, "currentBalance", newBal, "updatedAt", System.currentTimeMillis());

                    com.google.firebase.firestore.DocumentReference wtRef = db.collection("users")
                        .document(sessionManager.getUserId())
                        .collection("walletTransactions")
                        .document();

                    com.sajoldev.hisabniben.model.WalletTransaction wt = new com.sajoldev.hisabniben.model.WalletTransaction();
                    wt.setTransactionId(wtRef.getId());
                    wt.setAccountId(walletAcc.getAccountId());
                    wt.setAccountName(walletAcc.getAccountName());
                    wt.setUserId(sessionManager.getUserId());
                    wt.setType(com.sajoldev.hisabniben.model.WalletTransaction.TYPE_CUSTOMER_PAYMENT);
                    wt.setDirection(com.sajoldev.hisabniben.model.WalletTransaction.DIRECTION_IN);
                    wt.setCategory("Customer Payment");
                    wt.setAmount(finalPaymentAmount);
                    wt.setBalanceBefore(oldBal);
                    wt.setBalanceAfter(newBal);
                    wt.setTitle(customer.getName() + " এর কাছ থেকে টাকা জমা");
                    wt.setCustomerId(customer.getId());
                    wt.setCustomerName(customer.getName());
                    wt.setPaymentMethod(walletAcc.getAccountName());
                    wt.setReference(reference);
                    wt.setDescription(notes);
                    wt.setCreatedAt(System.currentTimeMillis());
                    wt.setTransactionDate(System.currentTimeMillis());

                    transaction.set(wtRef, wt.toMap());
                }
            }

            // Create legacy transaction entry
            String txId = UUID.randomUUID().toString();
            com.google.firebase.firestore.DocumentReference txRef = db.collection("transactions").document(txId);
            Map<String, Object> txData = new HashMap<>();
            txData.put("id", txId);
            txData.put("userId", sessionManager.getUserId());
            txData.put("customerId", customer.getId());
            txData.put("customerName", customer.getName());
            txData.put("type", "payment");
            txData.put("amount", finalPaymentAmount);
            txData.put("paymentMethod", finalMethod);
            txData.put("reference", reference);
            txData.put("notes", notes);
            txData.put("date", System.currentTimeMillis());

            transaction.set(txRef, txData);

            return newBaki;
        }).addOnSuccessListener(newBaki -> {
            Toast.makeText(requireContext(), "টাকা জমা নেওয়া সফল হয়েছে!", Toast.LENGTH_SHORT).show();

            if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
                SessionManager sm = SessionManager.getInstance(requireContext());
                String bizName = com.sajoldev.hisabniben.util.SmsTemplateManager.getEffectiveSmsBusinessName(null, sm);
                String smsMessage = (newBaki <= 0)
                    ? com.sajoldev.hisabniben.util.SmsTemplateManager.buildFullPaymentSms(customer.getName(), finalPaymentAmount, bizName)
                    : com.sajoldev.hisabniben.util.SmsTemplateManager.buildCustomerPaymentSms(customer.getName(), finalPaymentAmount, newBaki, bizName);

                com.sajoldev.hisabniben.util.SmsSenderHelper.sendSms(
                    requireContext(),
                    customer.getPhone(),
                    customer.getName(),
                    smsMessage,
                    "transaction",
                    "payment",
                    null
                );
            }

            if (onSavedListener != null) onSavedListener.run();
            dismiss();
        }).addOnFailureListener(e -> {
            btnSubmitPayment.setEnabled(true);
            Toast.makeText(requireContext(), "ত্রুটি: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
