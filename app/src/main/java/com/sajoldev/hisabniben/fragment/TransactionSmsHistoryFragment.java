package com.sajoldev.hisabniben.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.SmsHistoryAdapter;
import com.sajoldev.hisabniben.model.SmsHistory;
import com.sajoldev.hisabniben.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionSmsHistoryFragment extends Fragment {

    private RecyclerView rvSmsHistory;
    private TextView tvEmpty;
    private SmsHistoryAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", new Locale("bn", "BD"));

    public static TransactionSmsHistoryFragment newInstance() {
        return new TransactionSmsHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_custom_sms_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSmsHistory = view.findViewById(R.id.rvSmsHistory);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        db = FirebaseFirestore.getInstance();
        userId = SessionManager.getInstance(requireContext()).getUserId();

        rvSmsHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SmsHistoryAdapter();
        adapter.setOnItemClickListener(this::showSmsDetails);
        rvSmsHistory.setAdapter(adapter);

        loadTransactionSmsHistory();
    }

    public void filter(String query) {
        if (adapter != null) {
            adapter.filter(query);
        }
    }

    private void showSmsDetails(SmsHistory sms) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sms_details, null);
        dialog.setContentView(dialogView);

        TextView tvCustomerName = dialogView.findViewById(R.id.tvCustomerName);
        TextView tvPhone = dialogView.findViewById(R.id.tvPhone);
        TextView tvType = dialogView.findViewById(R.id.tvType);
        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        MaterialButton btnCopyMessage = dialogView.findViewById(R.id.btnCopyMessage);
        MaterialButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        String name = sms.getCustomerName() != null ? sms.getCustomerName() : "ব্যবসায়িক কাস্টমার";
        tvCustomerName.setText(name);
        tvPhone.setText(sms.getCustomerPhone() != null ? sms.getCustomerPhone() : "N/A");

        String subType = sms.getSubType() != null ? sms.getSubType() : "বিক্রয় SMS";
        tvType.setText("Transaction SMS (" + subType + ")");
        tvStatus.setText("পাঠানো হয়েছে");
        tvDate.setText(sms.getTimestamp() > 0 ? dateFormat.format(new Date(sms.getTimestamp())) : "N/A");
        tvMessage.setText(sms.getMessage() != null ? sms.getMessage() : "");

        btnCopyMessage.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("SMS Message", sms.getMessage());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "মেসেজ কপি করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadTransactionSmsHistory() {
        if (userId == null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("SMS হিস্ট্রি দেখতে লগইন করুন");
            rvSmsHistory.setVisibility(View.GONE);
            return;
        }

        db.collection("sms_history")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "transaction")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<SmsHistory> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SmsHistory sms = doc.toObject(SmsHistory.class);
                        if (sms != null) {
                            sms.setId(doc.getId());
                            list.add(sms);
                        }
                    }

                    if (list.isEmpty()) {
                        tvEmpty.setText("এখনো কোনো Transaction SMS পাঠানো হয়নি");
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvSmsHistory.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvSmsHistory.setVisibility(View.VISIBLE);
                        adapter.setData(list);
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setText("SMS ইতিহাস লোড করা যায়নি");
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvSmsHistory.setVisibility(View.GONE);
                });
    }
}