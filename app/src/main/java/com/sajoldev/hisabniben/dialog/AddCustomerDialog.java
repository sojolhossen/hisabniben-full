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
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;

import java.util.UUID;

public class AddCustomerDialog extends BottomSheetDialogFragment {
    private static final int REQUEST_READ_CONTACTS = 100;
    
    private TextView tvTitle;
    private TextInputLayout tilName, tilBusinessName, tilPhone, tilCustomerType, tilAddress, tilInitialBaki, tilNotes;
    private TextInputEditText etName, etBusinessName, etPhone, etAddress, etInitialBaki, etNotes;
    private AutoCompleteTextView actvCustomerType;
    private Button btnSave;

    private FirebaseFirestore db;
    private String userId;
    private OnCustomerAddedListener listener;

    private final String[] customerTypes = {
        "চালের দোকান",
        "পাইকারি ক্রেতা",
        "হোটেল/রেস্টুরেন্ট",
        "ডিলার",
        "ডিস্ট্রিবিউটর",
        "অন্যান্য"
    };

    public interface OnCustomerAddedListener {
        void onCustomerAdded(String customerId, String customerName);
    }

    public static AddCustomerDialog newInstance(String userId) {
        AddCustomerDialog dialog = new AddCustomerDialog();
        Bundle args = new Bundle();
        args.putString("user_id", userId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnCustomerAddedListener(OnCustomerAddedListener listener) {
        this.listener = listener;
    }

    public void setOnCustomerSavedListener(Runnable onSaved) {
        this.listener = (customerId, customerName) -> {
            if (onSaved != null) onSaved.run();
        };
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
        return inflater.inflate(R.layout.dialog_add_edit_customer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        initViews(view);
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tvTitle);
        tilName = view.findViewById(R.id.tilName);
        tilBusinessName = view.findViewById(R.id.tilBusinessName);
        tilPhone = view.findViewById(R.id.tilPhone);
        tilCustomerType = view.findViewById(R.id.tilCustomerType);
        tilAddress = view.findViewById(R.id.tilAddress);
        tilInitialBaki = view.findViewById(R.id.tilInitialBaki);
        tilNotes = view.findViewById(R.id.tilNotes);

        etName = view.findViewById(R.id.etName);
        etBusinessName = view.findViewById(R.id.etBusinessName);
        etPhone = view.findViewById(R.id.etPhone);
        actvCustomerType = view.findViewById(R.id.actvCustomerType);
        etAddress = view.findViewById(R.id.etAddress);
        etInitialBaki = view.findViewById(R.id.etInitialBaki);
        etNotes = view.findViewById(R.id.etNotes);
        btnSave = view.findViewById(R.id.btnSave);
        
        tvTitle.setText("নতুন চালের ক্রেতা যোগ করুন");
        
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, customerTypes);
        if (actvCustomerType != null) {
            actvCustomerType.setAdapter(typeAdapter);
        }
        
        View btnDelete = view.findViewById(R.id.btnDelete);
        if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        
        if (tilPhone != null) {
            tilPhone.setEndIconOnClickListener(v -> pickContact());
        }
        
        btnSave.setOnClickListener(v -> saveCustomer());
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
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startContactPicker();
            } else {
                Toast.makeText(requireContext(), "Contact permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_READ_CONTACTS && resultCode == android.app.Activity.RESULT_OK && data != null) {
            String contactId = null;
            Cursor cursor = null;
            try {
                cursor = requireContext().getContentResolver().query(
                        data.getData(),
                        new String[]{ContactsContract.Contacts._ID},
                        null, null, null
                );
                if (cursor != null && cursor.moveToFirst()) {
                    contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            if (contactId != null) {
                fetchContactDetails(contactId);
            }
        }
    }

    private void fetchContactDetails(String contactId) {
        Cursor phoneCursor = null;
        Cursor nameCursor = null;
        try {
            String phoneNumber = "";
            String displayName = "";
            
            nameCursor = requireContext().getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                    ContactsContract.Contacts._ID + " = ?",
                    new String[]{contactId},
                    null
            );
            if (nameCursor != null && nameCursor.moveToFirst()) {
                displayName = nameCursor.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            }
            
            phoneCursor = requireContext().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null
            );
            if (phoneCursor != null && phoneCursor.moveToFirst()) {
                phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
            
            if (displayName != null && !displayName.isEmpty()) etName.setText(displayName);
            if (phoneNumber != null && !phoneNumber.isEmpty()) etPhone.setText(phoneNumber);
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error reading contact", Toast.LENGTH_SHORT).show();
        } finally {
            if (phoneCursor != null) phoneCursor.close();
            if (nameCursor != null) nameCursor.close();
        }
    }

    private void saveCustomer() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            tilName.setError("ক্রেতার নাম লিখুন");
            return;
        }
        if (phone.isEmpty()) {
            tilPhone.setError("মোবাইল নম্বর লিখুন");
            return;
        }
        
        tilName.setError(null);
        tilPhone.setError(null);
        btnSave.setEnabled(false);

        String businessName = etBusinessName.getText() != null ? etBusinessName.getText().toString().trim() : "";
        String customerType = actvCustomerType != null ? actvCustomerType.getText().toString().trim() : "চালের দোকান";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        double initialBaki = 0;
        try {
            if (etInitialBaki.getText() != null && !etInitialBaki.getText().toString().isEmpty()) {
                initialBaki = Double.parseDouble(etInitialBaki.getText().toString().trim());
            }
        } catch (NumberFormatException e) {
            initialBaki = 0;
        }
        
        String customerId = UUID.randomUUID().toString();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setUid(customerId);
        customer.setUserId(userId);
        customer.setName(name);
        customer.setBusinessName(businessName);
        customer.setCustomerType(customerType);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setNotes(notes);
        customer.setOpeningBalance(initialBaki);
        customer.setBaki(initialBaki);
        customer.setCreatedAt(System.currentTimeMillis());
        customer.setUpdatedAt(System.currentTimeMillis());
        
        db.collection("customers")
            .document(customerId)
            .set(customer.toMap())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(requireContext(), "ক্রেতা সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onCustomerAdded(customerId, name);
                dismiss();
            })
            .addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                Toast.makeText(requireContext(), "ত্রুটি: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
