package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.User;
import com.sajoldev.hisabniben.util.SessionManager;

public class SmsIdentityDialog extends BottomSheetDialogFragment {

    private TextView tvOriginalBusinessName, tvCharCount, tvSmsPreview, tvWarningText;
    private MaterialCardView cardLongNameWarning;
    private TextInputEditText etSmsBusinessName;
    private MaterialButton btnSaveSmsName;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private String originalStoreName = "";
    private Runnable onSavedListener;

    public void setOnSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
            android.widget.FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<android.widget.FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_sms_identity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupListeners();
        loadUserData();
    }

    private void initViews(View view) {
        tvOriginalBusinessName = view.findViewById(R.id.tvOriginalBusinessName);
        tvCharCount = view.findViewById(R.id.tvCharCount);
        tvSmsPreview = view.findViewById(R.id.tvSmsPreview);
        tvWarningText = view.findViewById(R.id.tvWarningText);
        cardLongNameWarning = view.findViewById(R.id.cardLongNameWarning);
        etSmsBusinessName = view.findViewById(R.id.etSmsBusinessName);
        btnSaveSmsName = view.findViewById(R.id.btnSaveSmsName);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        etSmsBusinessName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLivePreview(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSaveSmsName.setOnClickListener(v -> saveSmsIdentity());
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        originalStoreName = sessionManager.getStoreName();
        if (originalStoreName == null || originalStoreName.isEmpty()) {
            originalStoreName = "সজল রাইস ট্রেডার্স";
        }
        tvOriginalBusinessName.setText(originalStoreName);

        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            if (user.getStoreName() != null && !user.getStoreName().isEmpty()) {
                                originalStoreName = user.getStoreName();
                                tvOriginalBusinessName.setText(originalStoreName);
                            }

                            String currentSmsName = user.getSmsBusinessName();
                            if (currentSmsName != null && !currentSmsName.isEmpty()) {
                                etSmsBusinessName.setText(currentSmsName);
                                etSmsBusinessName.setSelection(currentSmsName.length());
                            } else {
                                etSmsBusinessName.setText(originalStoreName);
                            }

                            if (originalStoreName.length() > 25 && (currentSmsName == null || currentSmsName.isEmpty())) {
                                cardLongNameWarning.setVisibility(View.VISIBLE);
                            } else {
                                cardLongNameWarning.setVisibility(View.GONE);
                            }

                            updateLivePreview(etSmsBusinessName.getText() != null ? etSmsBusinessName.getText().toString() : "");
                        }
                    }
                })
                .addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }

    private void updateLivePreview(String smsName) {
        String displayName = smsName != null && !smsName.trim().isEmpty() ? smsName.trim() : originalStoreName;
        int length = smsName != null ? smsName.length() : 0;

        tvCharCount.setText(length + " / 30");

        String preview = "আপনার কাছে 10 বস্তা (500 KG) চাল বিক্রি করা হয়েছে।\n" +
                "মোট মূল্য ৳32,500।\n" +
                "জমা ৳10,000।\n" +
                "বর্তমান বাকি ৳22,500।\n\n" +
                "— " + displayName;

        tvSmsPreview.setText(preview);
    }

    private void saveSmsIdentity() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        String smsName = etSmsBusinessName.getText() != null ? etSmsBusinessName.getText().toString().trim() : "";
        if (smsName.isEmpty()) {
            Toast.makeText(requireContext(), "অনুগ্রহ করে SMS ব্যবসার নাম লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveSmsName.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("smsBusinessName", smsName)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    sessionManager.setSmsBusinessName(smsName);
                    Toast.makeText(requireContext(), "SMS ব্যবসার পরিচয় সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show();
                    if (onSavedListener != null) onSavedListener.run();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveSmsName.setEnabled(true);
                    Toast.makeText(requireContext(), "সংরক্ষণে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
