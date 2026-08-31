package com.sajoldev.hisabniben.dialog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.util.FirestoreManager;

public class AddSupplierDialog extends BottomSheetDialogFragment {
    private static final int REQUEST_READ_CONTACTS = 100;
    
    private TextView tvTitle;
    private TextInputLayout tilName, tilBusinessName, tilPhone, tilSupplierType, tilAddress, tilInitialPayable, tilNotes;
    private TextInputEditText etName, etBusinessName, etPhone, etAddress, etInitialPayable, etNotes;
    private AutoCompleteTextView actvSupplierType;
    private Button btnSave;

    private String userId;
    private Runnable onSavedListener;

    private final String[] supplierTypes = {
        "চাল মিল",
        "চালের আড়ত",
        "পাইকার",
        "ডিস্ট্রিবিউটর",
        "অন্যান্য"
    };

    public static AddSupplierDialog newInstance(String userId) {
        AddSupplierDialog dialog = new AddSupplierDialog();
        Bundle args = new Bundle();
        args.putString("user_id", userId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnSupplierSavedListener(Runnable listener) {
        this.onSavedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("user_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_supplier, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tvTitle);
        tilName = view.findViewById(R.id.tilName);
        tilBusinessName = view.findViewById(R.id.tilBusinessName);
        tilPhone = view.findViewById(R.id.tilPhone);
        tilSupplierType = view.findViewById(R.id.tilSupplierType);
        tilAddress = view.findViewById(R.id.tilAddress);
        tilInitialPayable = view.findViewById(R.id.tilInitialPayable);
        tilNotes = view.findViewById(R.id.tilNotes);

        etName = view.findViewById(R.id.etName);
        etBusinessName = view.findViewById(R.id.etBusinessName);
        etPhone = view.findViewById(R.id.etPhone);
        actvSupplierType = view.findViewById(R.id.actvSupplierType);
        etAddress = view.findViewById(R.id.etAddress);
        etInitialPayable = view.findViewById(R.id.etInitialPayable);
        etNotes = view.findViewById(R.id.etNotes);
        btnSave = view.findViewById(R.id.btnSave);

        if (tvTitle != null) tvTitle.setText("নতুন চাল মহাজন যোগ করুন");

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, supplierTypes);
        if (actvSupplierType != null) {
            actvSupplierType.setAdapter(typeAdapter);
        }

        View btnDelete = view.findViewById(R.id.btnDelete);
        if (btnDelete != null) btnDelete.setVisibility(View.GONE);

        if (tilPhone != null) {
            tilPhone.setEndIconOnClickListener(v -> pickContact());
        }

        btnSave.setOnClickListener(v -> saveSupplier());
    }

    private void pickContact() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
        } else {
            startContactPicker();
        }
    }

    private void startContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startContactPicker();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_READ_CONTACTS && resultCode == android.app.Activity.RESULT_OK && data != null) {
            String contactId = null;
            try (Cursor cursor = requireContext().getContentResolver().query(data.getData(), new String[]{ContactsContract.Contacts._ID}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                }
            }
            if (contactId != null) {
                fetchContactDetails(contactId);
            }
        }
    }

    private void fetchContactDetails(String contactId) {
        try {
            String phoneNumber = "";
            String displayName = "";
            
            try (Cursor nameCursor = requireContext().getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                    ContactsContract.Contacts._ID + " = ?",
                    new String[]{contactId},
                    null)) {
                if (nameCursor != null && nameCursor.moveToFirst()) {
                    displayName = nameCursor.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                }
            }

            try (Cursor phoneCursor = requireContext().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null)) {
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
            }

            if (displayName != null && !displayName.isEmpty()) etName.setText(displayName);
            if (phoneNumber != null && !phoneNumber.isEmpty()) etPhone.setText(phoneNumber);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error reading contact", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSupplier() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            tilName.setError("মহাজনের নাম লিখুন");
            return;
        }
        if (phone.isEmpty()) {
            tilPhone.setError("মোবাইল নম্বর লিখুন");
            return;
        }

        btnSave.setEnabled(false);
        String businessName = etBusinessName.getText() != null ? etBusinessName.getText().toString().trim() : "";
        String supplierType = actvSupplierType != null ? actvSupplierType.getText().toString().trim() : "চাল মিল";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        double initialPayable = 0;
        try {
            if (etInitialPayable.getText() != null && !etInitialPayable.getText().toString().isEmpty()) {
                initialPayable = Double.parseDouble(etInitialPayable.getText().toString().trim());
            }
        } catch (NumberFormatException e) {
            initialPayable = 0;
        }

        Supplier supplier = new Supplier();
        supplier.setUserId(userId);
        supplier.setName(name);
        supplier.setBusinessName(businessName);
        supplier.setSupplierType(supplierType);
        supplier.setPhone(phone);
        supplier.setAddress(address);
        supplier.setNotes(notes);
        supplier.setCurrentPayable(initialPayable);
        supplier.setOpeningBalance(initialPayable);

        FirestoreManager.getInstance().createSupplier(supplier, new FirestoreManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Toast.makeText(requireContext(), "মহাজন সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show();
                if (onSavedListener != null) onSavedListener.run();
                dismiss();
            }

            @Override
            public void onFailure(String error) {
                btnSave.setEnabled(true);
                Toast.makeText(requireContext(), "ত্রুটি: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
