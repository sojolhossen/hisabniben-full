package com.sajoldev.hisabniben.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Expense;
import com.sajoldev.hisabniben.model.Purchase;
import com.sajoldev.hisabniben.model.PurchaseItem;
import com.sajoldev.hisabniben.model.Sale;
import com.sajoldev.hisabniben.model.SaleItem;
import com.sajoldev.hisabniben.model.StockMovement;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.model.UnifiedHistoryItem;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UnifiedDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_HISTORY_ITEM = "history_item";

    public static UnifiedDetailBottomSheet newInstance(UnifiedHistoryItem item) {
        UnifiedDetailBottomSheet dialog = new UnifiedDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_HISTORY_ITEM, item);
        dialog.setArguments(args);
        return dialog;
    }

    private TextView tvDetailTitle, tvDetailDate;
    private LinearLayout containerDetailFields;
    private MaterialButton btnShareDetail, btnCloseDetail;
    private UnifiedHistoryItem item;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_unified_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            item = (UnifiedHistoryItem) getArguments().getSerializable(ARG_HISTORY_ITEM);
        }

        if (item == null) {
            dismiss();
            return;
        }

        tvDetailTitle = view.findViewById(R.id.tvDetailTitle);
        tvDetailDate = view.findViewById(R.id.tvDetailDate);
        containerDetailFields = view.findViewById(R.id.containerDetailFields);
        btnShareDetail = view.findViewById(R.id.btnShareDetail);
        btnCloseDetail = view.findViewById(R.id.btnCloseDetail);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy · hh:mm a", Locale.ENGLISH);
        tvDetailDate.setText(sdf.format(new Date(item.getDate())));

        btnCloseDetail.setOnClickListener(v -> dismiss());
        btnShareDetail.setOnClickListener(v -> shareDetails());

        renderDetails();
    }

    private void renderDetails() {
        containerDetailFields.removeAllViews();
        Object orig = item.getOriginalObject();

        if (orig instanceof Sale) {
            Sale sale = (Sale) orig;
            tvDetailTitle.setText("চাল বিক্রির বিস্তারিত (Invoice #" + (sale.getInvoiceNo() != null ? sale.getInvoiceNo() : "") + ")");

            addFieldRow("ক্রেতা/কাস্টমার:", sale.getCustomerName());
            if (sale.getCustomerPhone() != null && !sale.getCustomerPhone().isEmpty()) {
                addFieldRow("মোবাইল নম্বর:", sale.getCustomerPhone());
            }

            if (sale.getItems() != null) {
                for (SaleItem si : sale.getItems()) {
                    addFieldRow("চালের ধরন:", si.getProductNameSnapshot() + " (" + si.getVarietySnapshot() + ")");
                    addFieldRow("পরিমাণ (বস্তা & KG):", (int) si.getBagQuantity() + " বস্তা (" + UnitConverterHelper.formatKg(si.getTotalKg()) + ")");
                    addFieldRow("বিক্রি দর/KG:", UnitConverterHelper.formatCurrency(si.getSaleRatePerKg()) + "/KG");
                }
            }

            addFieldRow("সর্বমোট বিক্রিমূল্য:", UnitConverterHelper.formatCurrency(sale.getGrandTotal()));
            addFieldRow("জমা (Paid):", UnitConverterHelper.formatCurrency(sale.getPaidAmount()));
            addFieldRow("বর্তমান বাকি (Due):", UnitConverterHelper.formatCurrency(sale.getDueAmount()));
            addFieldRow("পেমেন্ট মাধ্যম:", sale.getPaymentMethod());

        } else if (orig instanceof Purchase) {
            Purchase p = (Purchase) orig;
            tvDetailTitle.setText("চাল ক্রয়ের বিস্তারিত (Memo #" + (p.getInvoiceNo() != null ? p.getInvoiceNo() : "") + ")");

            addFieldRow("মহাজন/সাপ্লায়ার:", p.getSupplierName());
            if (p.getItems() != null) {
                for (PurchaseItem pi : p.getItems()) {
                    addFieldRow("চালের নাম:", pi.getProductName() + " (" + pi.getVariety() + ")");
                    addFieldRow("পরিমাণ (বস্তা & KG):", (int) pi.getBagQuantity() + " বস্তা (" + UnitConverterHelper.formatKg(pi.getTotalKg()) + ")");
                    addFieldRow("ক্রয় দর/KG:", UnitConverterHelper.formatCurrency(pi.getPurchaseRatePerKg()) + "/KG");
                }
            }

            addFieldRow("সর্বমোট ক্রয়মূল্য:", UnitConverterHelper.formatCurrency(p.getGrandTotal()));
            addFieldRow("পরিশোধ (Paid):", UnitConverterHelper.formatCurrency(p.getPaidAmount()));
            addFieldRow("মহাজন পাওনা (Payable):", UnitConverterHelper.formatCurrency(p.getDueAmount()));

        } else if (orig instanceof Transaction) {
            Transaction tx = (Transaction) orig;
            tvDetailTitle.setText("টাকা জমা / লেনদেনের বিস্তারিত");

            addFieldRow("গ্রাহক/উৎস:", item.getTitle());
            addFieldRow("টাকার পরিমাণ:", UnitConverterHelper.formatCurrency(tx.getAmount()));
            addFieldRow("জের/বাকি স্থিতি:", UnitConverterHelper.formatCurrency(tx.getNewBaki()));
            addFieldRow("পেমেন্ট মাধ্যম:", tx.getPaymentMethod());
            if (tx.getNote() != null && !tx.getNote().isEmpty()) {
                addFieldRow("বিবরণ / নোট:", tx.getNote());
            }

        } else if (orig instanceof Expense) {
            Expense exp = (Expense) orig;
            tvDetailTitle.setText("ব্যবসার খরচের বিস্তারিত");

            addFieldRow("খরচের খাত:", exp.getCategory());
            addFieldRow("টাকার পরিমাণ:", UnitConverterHelper.formatCurrency(exp.getAmount()));
            addFieldRow("পেমেন্ট মাধ্যম:", exp.getPaymentMethod());
            if (exp.getDescription() != null && !exp.getDescription().isEmpty()) {
                addFieldRow("বিবরণ / নোট:", exp.getDescription());
            }
        } else if (orig instanceof StockMovement) {
            StockMovement sm = (StockMovement) orig;
            tvDetailTitle.setText("চাল স্টক পরিবর্তনের বিস্তারিত");

            addFieldRow("চালের নাম:", sm.getProductName());
            addFieldRow("পরিবর্তনের পরিমাণ:", UnitConverterHelper.formatKg(sm.getQuantityKg()));
            addFieldRow("পরিবর্তনের পর বর্তমান স্টক:", UnitConverterHelper.formatKg(sm.getNewStockKg()));
            addFieldRow("কারণ / নোট:", sm.getReason());
        }
    }

    private void addFieldRow(String label, String value) {
        if (value == null || value.isEmpty()) return;
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_stock_movement_timeline_row, containerDetailFields, false);
        TextView tvLabel = row.findViewById(R.id.tvMovementReason);
        TextView tvVal = row.findViewById(R.id.tvMovementQty);
        TextView tvDate = row.findViewById(R.id.tvMovementDate);

        tvLabel.setText(label);
        tvVal.setText(value);
        tvDate.setVisibility(View.GONE);

        containerDetailFields.addView(row);
    }

    private void shareDetails() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, tvDetailTitle.getText().toString());
        intent.putExtra(Intent.EXTRA_TEXT, tvDetailTitle.getText().toString() + "\n" + item.getTitle() + "\nপরিমাণ: " + UnitConverterHelper.formatCurrency(item.getAmount()) + "\nহিসাব নিবেন — চালের ব্যবসা পরিচালনা অ্যাপ");
        startActivity(Intent.createChooser(intent, "শেয়ার করুন"));
    }
}
