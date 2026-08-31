package com.sajoldev.hisabniben.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.User;
import com.sajoldev.hisabniben.util.ProfileImageHelper;
import com.sajoldev.hisabniben.util.SessionManager;

public class ProfileActivity extends AppCompatActivity {
    private ImageView btnBack;
    private ImageView ivProfileImage;
    private TextView tvInitials, tvUserName, tvUserEmail;
    private TextView tvStoreName, tvStoreType, tvAddress, tvSmsBusinessName;
    private View layoutSmsName;
    private TextView tvSubscriptionStatus, tvSmsBalance;
    private ImageView ivStatusIcon;
    private ImageView fabChangeImage;

    private SessionManager sessionManager;
    private ProfileImageHelper profileImageHelper;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_profile);

        initImagePicker();
        initViews();
        setupWindowInsets();
        loadUserData();
        loadProfileImage();
    }

    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                Bitmap bitmap;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                                    bitmap = ImageDecoder.decodeBitmap(source);
                                } else {
                                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                }
                                saveProfileImage(bitmap);
                            } catch (Exception e) {
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            int topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            View headerView = findViewById(R.id.headerLayout);
            if (headerView != null) {
                headerView.setPadding(headerView.getPaddingLeft(), topInsets, headerView.getPaddingRight(), headerView.getPaddingBottom());
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvInitials = findViewById(R.id.tvInitials);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvStoreName = findViewById(R.id.tvStoreName);
        tvSmsBusinessName = findViewById(R.id.tvSmsBusinessName);
        layoutSmsName = findViewById(R.id.layoutSmsName);
        tvStoreType = findViewById(R.id.tvStoreType);
        tvAddress = findViewById(R.id.tvAddress);
        tvSubscriptionStatus = findViewById(R.id.tvSubscriptionStatus);
        tvSmsBalance = findViewById(R.id.tvSmsBalance);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        fabChangeImage = findViewById(R.id.fabChangeImage);

        sessionManager = SessionManager.getInstance(this);
        profileImageHelper = ProfileImageHelper.getInstance(this);

        btnBack.setOnClickListener(v -> finish());
        fabChangeImage.setOnClickListener(v -> showImagePickerDialog());
        if (layoutSmsName != null) {
            layoutSmsName.setOnClickListener(v -> {
                com.sajoldev.hisabniben.dialog.SmsIdentityDialog dialog = new com.sajoldev.hisabniben.dialog.SmsIdentityDialog();
                dialog.setOnSavedListener(this::loadUserData);
                dialog.show(getSupportFragmentManager(), "SmsIdentityProfile");
            });
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Remove Photo"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Profile Photo")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openCamera();
                            break;
                        case 1:
                            openGallery();
                            break;
                        case 2:
                            removeProfileImage();
                            break;
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), result -> {
                if (result != null) {
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(result, 200, 200, true);
                    saveProfileImage(scaledBitmap);
                }
            }).launch(null);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveProfileImage(Bitmap bitmap) {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
        profileImageHelper.saveProfileImage(userId, scaledBitmap);
        
        ivProfileImage.setImageBitmap(scaledBitmap);
        ivProfileImage.setVisibility(View.VISIBLE);
        tvInitials.setVisibility(View.GONE);
        
        Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
    }

    private void loadProfileImage() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        Bitmap bitmap = profileImageHelper.getProfileImage(userId);
        if (bitmap != null) {
            ivProfileImage.setImageBitmap(bitmap);
            ivProfileImage.setVisibility(View.VISIBLE);
            tvInitials.setVisibility(View.GONE);
        } else {
            ivProfileImage.setVisibility(View.GONE);
            tvInitials.setVisibility(View.VISIBLE);
        }
    }

    private void removeProfileImage() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        profileImageHelper.deleteProfileImage(userId);
        ivProfileImage.setVisibility(View.GONE);
        tvInitials.setVisibility(View.VISIBLE);
        
        Toast.makeText(this, "Profile photo removed", Toast.LENGTH_SHORT).show();
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        tvUserName.setText(sessionManager.getUserName());
        tvUserEmail.setText(sessionManager.getUserEmail());

        String name = sessionManager.getUserName();
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            if (parts.length >= 2) {
                tvInitials.setText(("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase());
            } else {
                tvInitials.setText(name.substring(0, Math.min(2, name.length())).toUpperCase());
            }
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        tvStoreName.setText(user.getStoreName() != null ? user.getStoreName() : "N/A");
                        if (tvSmsBusinessName != null) {
                            tvSmsBusinessName.setText(com.sajoldev.hisabniben.util.SmsTemplateManager.getEffectiveSmsBusinessName(user, sessionManager));
                        }
                        tvStoreType.setText(user.getStoreType() != null ? user.getStoreType() : "N/A");
                        tvAddress.setText(user.getAddress() != null ? user.getAddress() : "N/A");

                        Long smsLimit = documentSnapshot.getLong("smsLimit");
                        int sms = smsLimit != null ? smsLimit.intValue() : 10;
                        tvSmsBalance.setText(sms + " SMS");

                        updateSubscriptionStatus(user);
                    }
                }
            });
    }

    private void updateSubscriptionStatus(User user) {
        if (user.isPremium()) {
            ivStatusIcon.setImageResource(R.drawable.ic_star);
            tvSubscriptionStatus.setText("Premium");
        } else if (!user.isPremium() && user.getTrialEnd() > System.currentTimeMillis()) {
            ivStatusIcon.setImageResource(R.drawable.ic_star_outline);
            long remaining = (user.getTrialEnd() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
            tvSubscriptionStatus.setText("Free Trial (" + remaining + " days)");
        } else {
            ivStatusIcon.setImageResource(R.drawable.ic_star_outline);
            tvSubscriptionStatus.setText("Trial Expired");
        }
    }
}