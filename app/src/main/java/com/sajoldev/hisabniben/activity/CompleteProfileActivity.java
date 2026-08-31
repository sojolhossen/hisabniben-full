package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {
    private TextInputLayout tilStoreName, tilStoreType, tilAddress, tilEmail;
    private TextInputEditText etStoreName, etAddress, etEmail;
    private AutoCompleteTextView actvStoreType;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private String[] storeTypes = {
        "চালের আড়ত (Rice Wholesale/Arat)",
        "পাইকারি চালের ব্যবসা (Rice Wholesaler)",
        "খুচরা চালের দোকান (Rice Retailer)",
        "চালের ডিলার / ডিস্ট্রিবিউটর (Rice Dealer)",
        "রাইস মিল (Rice Mill)",
        "অন্যান্য চাল ব্যবসা (Other Rice Business)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_complete_profile);
        com.sajoldev.hisabniben.util.ScreenSecurityHelper.allowScreenSharingAndRecording(this);

        initViews();
        initFirebase();
        setupWindowInsets();
        setupStoreTypeDropdown();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            int topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(view.getPaddingLeft(), topInsets, view.getPaddingRight(), view.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        tilStoreName = findViewById(R.id.tilStoreName);
        tilStoreType = findViewById(R.id.tilStoreType);
        tilAddress = findViewById(R.id.tilAddress);
        tilEmail = findViewById(R.id.tilEmail);
        etStoreName = findViewById(R.id.etStoreName);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        actvStoreType = findViewById(R.id.actvStoreType);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        sessionManager = SessionManager.getInstance(this);
    }

    private void setupStoreTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, storeTypes);
        actvStoreType.setAdapter(adapter);
    }

    private void saveProfile() {
        String storeName = etStoreName.getText() != null ? etStoreName.getText().toString().trim() : "";
        String storeType = actvStoreType.getText() != null ? actvStoreType.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        showLoading(true);

        String userId = sessionManager.getUserId();
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("storeName", storeName);
        userUpdates.put("storeType", storeType);
        userUpdates.put("address", address);
        if (!email.isEmpty()) {
            userUpdates.put("email", email);
        }

        db.collection("users")
            .document(userId)
            .update(userUpdates)
            .addOnCompleteListener(task -> {
                showLoading(false);
                if (task.isSuccessful()) {
                    sessionManager.updateStoreName(storeName);
                    Toast.makeText(CompleteProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                } else {
                    Toast.makeText(CompleteProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(CompleteProfileActivity.this, com.sajoldev.hisabniben.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
