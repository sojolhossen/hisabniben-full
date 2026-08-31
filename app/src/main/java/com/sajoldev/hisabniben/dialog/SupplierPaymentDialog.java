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
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SupplierPaymentDialog extends BottomSheetDialogFragment {

    private Supplier supplier;
    private Runnable onSavedListener;

    private TextView tvPaymentSupplierName, tvPaymentSupplierPhone, tvPaymentCurrentPayable;
    private TextInputLayout tilPaymentAmount, tilPaymentRef, tilPaymentNotes;
    private TextInputEditText etPaymentAmount, etPaymentRef, etPaymentNotes;
    private ChipGroup chipGroupPaymentMethod;
    private MaterialButton btnSubmitPayment;

    private SessionManager sessionManager;
    private FirebaseFirestore db;

    public static SupplierPaymentDialog newInstance(Supplier supplier) {
        SupplierPaymentDialog dialog = new SupplierPaymentDialog();
        Bundle args = new Bundle();
        args.putSerializable("supplier", supplier);
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
            supplier = (Supplier) getArguments().getSerializable("supplier");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_supplier_payment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());
        db = FirebaseFirestore.getInstance();

        initViews(view);
    }

    private void initViews(View view) {
        tvPaymentSupplierName = view.findViewById(R.id.tvPaymentSupplierName);
        tvPaymentSupplierPhone = view.findViewById(R.id.tvPaymentSupplierPhone);
        tvPaymentCurrentPayable = view.findViewById(R.id.tvPaymentCurrentPayable);

        tilPaymentAmount = view.findViewById(R.id.tilPaymentAmount);
        etPaymentAmount = view.findViewById(R.id.etPaymentAmount);
        chipGroupPaymentMethod = view.findViewById(R.id.chipGroupPaymentMethod);
        tilPaymentRef = view.findViewById(R.id.tilPaymentRef);
        etPaymentRef = view.findViewById(R.id.etPaymentRef);
        tilPaymentNotes = view.findViewById(R.id.tilPaymentNotes);
        etPaymentNotes = view.findViewById(R.id.etPaymentNotes);
        btnSubmitPayment = view.findViewById(R.id.btnSubmitPayment);

        if (supplier != null) {
            String businessName = supplier.getBusinessName() != null && !supplier.getBusinessName().isEmpty() ? supplier.getBusinessName() : supplier.getName();
            tvPaymentSupplierName.setText(businessName);
            tvPaymentSupplierPhone.setText("📞 " + (supplier.getPhone() != null ? supplier.getPhone() : ""));
            tvPaymentCurrentPayable.setText(UnitConverterHelper.formatCurrency(supplier.getCurrentPayable()));
        }

        btnSubmitPayment.setOnClickListener(v -> submitPayment());
    }

    private void submitPayment() {
        if (supplier == null || supplier.getId() == null) return;

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
            // Read supplier
            com.google.firebase.firestore.DocumentReference supplierRef = db.collection("suppliers").document(supplier.getId());
            com.google.firebase.firestore.DocumentSnapshot supplierSnap = transaction.get(supplierRef);

            String targetAccId = com.sajoldev.hisabniben.util.FirestoreManager.resolveAccountIdForMethod(finalMethod, null);
            com.google.firebase.firestore.DocumentReference walletAccRef = db.collection("users")
                .document(sessionManager.getUserId())
                .collection("walletAccounts")
                .document(targetAccId);
            com.google.firebase.firestore.DocumentSnapshot walletAccSnap = transaction.get(walletAccRef);

            double currentPayable = supplierSnap.exists() && supplierSnap.getDouble("currentPayable") != null ? supplierSnap.getDouble("currentPayable") : supplier.getCurrentPayable();
            double totalPaid = supplierSnap.exists() && supplierSnap.getDouble("totalPaid") != null ? supplierSnap.getDouble("totalPaid") : 0.0;

            double newPayable = currentPayable - finalPaymentAmount;
            double newTotalPaid = totalPaid + finalPaymentAmount;

            // Wallet Balance Update & Transaction Log with Negative Balance Check
            if (walletAccSnap != null && walletAccSnap.exists()) {
                com.sajoldev.hisabniben.model.WalletAccount walletAcc = walletAccSnap.toObject(com.sajoldev.hisabniben.model.WalletAccount.class);
                if (walletAcc != null) {
                    double oldBal = walletAcc.getCurrentBalance();
                    if (oldBal < finalPaymentAmount) {
                        throw new com.google.firebase.firestore.FirebaseFirestoreException(
                            "এই অ্যাকাউন্টে (" + walletAcc.getAccountName() + ") পর্যাপ্ত টাকা নেই। (বর্তমান ব্যালেন্স: ৳" + String.format("%.0f", oldBal) + ", পরিশোধ: ৳" + String.format("%.0f", finalPaymentAmount) + ")",
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                        );
                    }
                    double newBal = oldBal - finalPaymentAmount;
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
                    wt.setType(com.sajoldev.hisabniben.model.WalletTransaction.TYPE_SUPPLIER_PAYMENT);
                    wt.setDirection(com.sajoldev.hisabniben.model.WalletTransaction.DIRECTION_OUT);
                    wt.setCategory("Supplier Payment");
                    wt.setAmount(finalPaymentAmount);
                    wt.setBalanceBefore(oldBal);
                    wt.setBalanceAfter(newBal);
                    wt.setTitle(supplier.getName() + " কে টাকা পরিশোধ");
                    wt.setSupplierId(supplier.getId());
                    wt.setSupplierName(supplier.getName());
                    wt.setPaymentMethod(walletAcc.getAccountName());
                    wt.setReference(reference);
                    wt.setDescription(notes);
                    wt.setCreatedAt(System.currentTimeMillis());
                    wt.setTransactionDate(System.currentTimeMillis());

                    transaction.set(wtRef, wt.toMap());
                }
            }

            transaction.update(supplierRef, "currentPayable", newPayable, "totalPaid", newTotalPaid, "updatedAt", System.currentTimeMillis());

            // Create transaction entry
            String txId = UUID.randomUUID().toString();
            com.google.firebase.firestore.DocumentReference txRef = db.collection("transactions").document(txId);
            Map<String, Object> txData = new HashMap<>();
            txData.put("id", txId);
            txData.put("userId", sessionManager.getUserId());
            txData.put("supplierId", supplier.getId());
            txData.put("supplierName", supplier.getName());
            txData.put("type", "supplier_payment");
            txData.put("amount", finalPaymentAmount);
            txData.put("paymentMethod", finalMethod);
            txData.put("reference", reference);
            txData.put("notes", notes);
            txData.put("date", System.currentTimeMillis());

            transaction.set(txRef, txData);

            return newPayable;
        }).addOnSuccessListener(newPayable -> {
            Toast.makeText(requireContext(), "মহাজনকে টাকা পরিশোধ সফল হয়েছে!", Toast.LENGTH_SHORT).show();

            if (supplier.getPhone() != null && !supplier.getPhone().trim().isEmpty()) {
                SessionManager sm = SessionManager.getInstance(requireContext());
                String bizName = com.sajoldev.hisabniben.util.SmsTemplateManager.getEffectiveSmsBusinessName(null, sm);
                String smsMessage = com.sajoldev.hisabniben.util.SmsTemplateManager.buildSupplierPaymentSms(supplier.getName(), finalPaymentAmount, newPayable, bizName);

                com.sajoldev.hisabniben.util.SmsSenderHelper.sendSms(
                    requireContext(),
                    supplier.getPhone(),
                    supplier.getName(),
                    smsMessage,
                    "transaction",
                    "supplier_payment",
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
