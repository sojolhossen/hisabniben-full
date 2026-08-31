package com.sajoldev.hisabniben.dialog;

import android.app.AlertDialog;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.util.SessionManager;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerDetailsBottomSheet extends BottomSheetDialogFragment {
    private Customer customer;
    private OnCustomerActionListener listener;
    private double totalPayment = 0;
    private int totalTransactions = 0;

    public interface OnCustomerActionListener {
        void onEditClick(Customer customer);
        void onDeleteClick(Customer customer);
        void onActivityClick(Customer customer);
    }

    public static CustomerDetailsBottomSheet newInstance(Customer customer) {
        CustomerDetailsBottomSheet sheet = new CustomerDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString("customer_id", customer.getId());
        args.putString("name", customer.getName());
        args.putString("phone", customer.getPhone());
        args.putString("address", customer.getAddress());
        args.putDouble("baki", customer.getBaki());
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnCustomerActionListener(OnCustomerActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customer = new Customer();
            customer.setId(getArguments().getString("customer_id"));
            customer.setName(getArguments().getString("name"));
            customer.setPhone(getArguments().getString("phone"));
            customer.setAddress(getArguments().getString("address"));
            customer.setBaki(getArguments().getDouble("baki"));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_customer_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvPhone = view.findViewById(R.id.tvPhone);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        TextView tvTotalBaki = view.findViewById(R.id.tvTotalBaki);
        TextView tvTotalPayment = view.findViewById(R.id.tvTotalPayment);
        TextView tvTotalTransaction = view.findViewById(R.id.tvTotalTransaction);
        
        MaterialButton btnSendSms = view.findViewById(R.id.btnSendSms);
        MaterialButton btnEdit = view.findViewById(R.id.btnEdit);
        MaterialButton btnDelete = view.findViewById(R.id.btnDelete);
        MaterialButton btnActivity = view.findViewById(R.id.btnActivity);
        MaterialButton btnBakiReminder = view.findViewById(R.id.btnBakiReminder);
        
        tvName.setText(customer.getName());
        tvPhone.setText(customer.getPhone() != null && !customer.getPhone().isEmpty() ? customer.getPhone() : "No phone");
        tvAddress.setText(customer.getAddress() != null && !customer.getAddress().isEmpty() ? customer.getAddress() : "No address");
        tvTotalBaki.setText(formatCurrency(customer.getBaki()));
        
        loadCustomerStats();
        
        btnSendSms.setOnClickListener(v -> showSmsDialog());
        
        btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(customer);
            dismiss();
        });
        
        btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(customer);
            dismiss();
        });
        
        btnActivity.setOnClickListener(v -> {
            if (listener != null) listener.onActivityClick(customer);
            dismiss();
        });
        
        btnBakiReminder.setOnClickListener(v -> showBakiReminderDialog());
    }

    private void loadCustomerStats() {
        if (customer == null || customer.getId() == null) return;
        
        FirebaseFirestore.getInstance()
            .collection("transactions")
            .whereEqualTo("customerId", customer.getId())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                totalTransactions = queryDocumentSnapshots.size();
                totalPayment = 0;
                
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    String type = doc.getString("type");
                    Double amount = doc.getDouble("amount");
                    if (amount == null) amount = 0.0;
                    
                    if ("payment".equalsIgnoreCase(type)) {
                        totalPayment += amount;
                    }
                }
                
                View view = getView();
                if (view != null) {
                    TextView tvTotalPayment = view.findViewById(R.id.tvTotalPayment);
                    TextView tvTotalTransaction = view.findViewById(R.id.tvTotalTransaction);
                    tvTotalPayment.setText(formatCurrency(totalPayment));
                    tvTotalTransaction.setText(String.valueOf(totalTransactions));
                }
            });
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("bn", "BD"));
        format.setCurrency(Currency.getInstance("BDT"));
        return format.format(amount);
    }

    private void showSmsDialog() {
        if (customer.getPhone() == null || customer.getPhone().isEmpty()) {
            Toast.makeText(getContext(), "Customer has no phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_sms, null);
        
        TextView tvCustomerName = dialogView.findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = dialogView.findViewById(R.id.tvCustomerPhone);
        TextInputEditText etMessage = dialogView.findViewById(R.id.etMessage);
        TextView tvCharCount = dialogView.findViewById(R.id.tvCharCount);
        TextView tvSmsCost = dialogView.findViewById(R.id.tvSmsCost);
        TextView tvBalance = dialogView.findViewById(R.id.tvBalance);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSend);
        
        tvCustomerName.setText(customer.getName());
        tvCustomerPhone.setText(customer.getPhone());
        
        loadSmsBalance(tvBalance);
        
        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvCharCount.setText(length + " characters");
                int smsCost = length > 70 ? 2 : 1;
                tvSmsCost.setText("Cost: " + smsCost + " SMS" + (smsCost > 1 ? "s" : ""));
                if (smsCost > 1) {
                    tvSmsCost.setTextColor(requireContext().getResources().getColor(R.color.warning, null));
                } else {
                    tvSmsCost.setTextColor(requireContext().getResources().getColor(R.color.success, null));
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int smsCount = message.length() > 160 ? 2 : 1;
            checkSmsBalanceAndSend(customer.getPhone(), message, smsCount);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showBakiReminderDialog() {
        if (customer.getPhone() == null || customer.getPhone().isEmpty()) {
            Toast.makeText(getContext(), "Customer has no phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_baki_reminder, null);
        
        TextView tvCustomerName = dialogView.findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = dialogView.findViewById(R.id.tvCustomerPhone);
        TextInputEditText etMessage = dialogView.findViewById(R.id.etMessage);
        TextView tvCharCount = dialogView.findViewById(R.id.tvCharCount);
        TextView tvSmsCost = dialogView.findViewById(R.id.tvSmsCost);
        TextView tvBalance = dialogView.findViewById(R.id.tvBalance);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSend);
        
        tvCustomerName.setText(customer.getName());
        tvCustomerPhone.setText(customer.getPhone());
        
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String bizName = com.sajoldev.hisabniben.util.SmsTemplateManager.getEffectiveSmsBusinessName(null, sessionManager);
        String defaultMessage = com.sajoldev.hisabniben.util.SmsTemplateManager.buildDueReminderSms(customer.getName(), customer.getBaki(), bizName);
        etMessage.setText(defaultMessage);
        
        loadSmsBalance(tvBalance);
        
        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvCharCount.setText(length + " characters");
                int smsCost = length > 70 ? 2 : 1;
                tvSmsCost.setText("Cost: " + smsCost + " SMS" + (smsCost > 1 ? "s" : ""));
                if (smsCost > 1) {
                    tvSmsCost.setTextColor(requireContext().getResources().getColor(R.color.warning, null));
                } else {
                    tvSmsCost.setTextColor(requireContext().getResources().getColor(R.color.success, null));
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "বার্তা প্রদান করুন", Toast.LENGTH_SHORT).show();
                return;
            }
            
            com.sajoldev.hisabniben.util.SmsSenderHelper.sendSms(
                requireContext(),
                customer.getPhone(),
                customer.getName(),
                message,
                "transaction",
                "due_reminder",
                null
            );
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void sendSms(String phoneNumber, String message, int smsCount) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(getContext(), "Customer has no phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("settings").document("sms_api")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String apiKey = documentSnapshot.getString("apiKey");
                    String senderId = documentSnapshot.getString("senderId");
                    sendSmsWithSettings(phoneNumber, message, apiKey, senderId, smsCount);
                } else {
                    Toast.makeText(getContext(), "SMS API not configured", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load SMS settings", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void checkSmsBalanceAndSend(String phoneNumber, String message, int smsCount) {
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String userId = sessionManager.getUserId();
        if (userId == null) return;
        
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long smsLimit = documentSnapshot.getLong("smsLimit");
                    int currentLimit = smsLimit != null ? smsLimit.intValue() : 0;
                    
                    if (currentLimit < smsCount) {
                        Toast.makeText(getContext(), "Insufficient SMS balance. Need " + smsCount + " but have " + currentLimit, Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    sendSms(phoneNumber, message, smsCount);
                } else {
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error checking balance", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void sendSmsWithSettings(String phoneNumber, String message, String apiKey, String senderId, int smsCount) {
        if (apiKey == null || apiKey.isEmpty() || senderId == null || senderId.isEmpty()) {
            android.util.Log.e("SMS", "API key or sender ID is missing");
            Toast.makeText(getContext(), "SMS API not configured. Please contact admin.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final int smsToDeduct = smsCount;
        
        new Thread(() -> {
            String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
            if (!cleanPhone.startsWith("880")) {
                if (cleanPhone.startsWith("0")) {
                    cleanPhone = "88" + cleanPhone;
                } else {
                    cleanPhone = "880" + cleanPhone;
                }
            }
            
            try {
                String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
                String url = "http://bulksmsbd.net/api/smsapi?api_key=" + apiKey 
                        + "&type=text&number=" + cleanPhone 
                        + "&senderid=" + senderId 
                        + "&message=" + encodedMessage;
                
                android.util.Log.d("SMS", "Sending custom SMS to: " + cleanPhone);
                
                java.net.URL urlObj = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                
                int responseCode = conn.getResponseCode();
                android.util.Log.d("SMS", "Response Code: " + responseCode);
                
                if (responseCode == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    android.util.Log.d("SMS", "Response: " + response.toString());
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "SMS sent successfully!", Toast.LENGTH_SHORT).show();
                            updateSmsBalance(smsToDeduct);
                            String type = message.contains("outstanding balance") ? "baki_reminder" : "custom";
                            saveSmsToHistory(phoneNumber, message, type);
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Failed to send SMS", Toast.LENGTH_SHORT).show());
                    }
                }
                
                conn.disconnect();
                
            } catch (Exception e) {
                android.util.Log.e("SMS", "Error: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }
    
    private void updateSmsBalance(int smsCount) {
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String userId = sessionManager.getUserId();
        if (userId == null) return;
        
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long smsLimit = documentSnapshot.getLong("smsLimit");
                    int currentLimit = smsLimit != null ? smsLimit.intValue() : 10;
                    int newLimit = Math.max(0, currentLimit - smsCount);
                    
                    FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .update("smsLimit", newLimit)
                        .addOnSuccessListener(aVoid -> 
                            android.util.Log.d("SMS", "SMS balance updated to: " + newLimit + " (deducted " + smsCount + ")"))
                        .addOnFailureListener(e -> 
                            android.util.Log.e("SMS", "Failed to update SMS balance: " + e.getMessage()));
                }
            });
    }

    private void saveSmsToHistory(String phone, String message, String type) {
        if (customer == null) return;
        
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String userId = sessionManager.getUserId();
        if (userId == null) return;
        
        Map<String, Object> smsHistory = new HashMap<>();
        smsHistory.put("userId", userId);
        smsHistory.put("customerPhone", phone);
        smsHistory.put("customerName", customer.getName());
        smsHistory.put("message", message);
        smsHistory.put("type", type);
        smsHistory.put("timestamp", System.currentTimeMillis());
        
        FirebaseFirestore.getInstance().collection("sms_history")
            .add(smsHistory)
            .addOnSuccessListener(docRef -> android.util.Log.d("SMS", "SMS saved to history"))
            .addOnFailureListener(e -> android.util.Log.e("SMS", "Failed to save SMS: " + e.getMessage()));
    }
    
    private void loadSmsBalance(TextView tvBalance) {
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String userId = sessionManager.getUserId();
        if (userId == null) {
            tvBalance.setText("Balance: --");
            return;
        }
        
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long smsLimit = documentSnapshot.getLong("smsLimit");
                    int balance = smsLimit != null ? smsLimit.intValue() : 0;
                    tvBalance.setText("Balance: " + balance + " SMS");
                    
                    if (balance <= 0) {
                        tvBalance.setTextColor(requireContext().getResources().getColor(R.color.error, null));
                    } else if (balance <= 5) {
                        tvBalance.setTextColor(requireContext().getResources().getColor(R.color.warning, null));
                    } else {
                        tvBalance.setTextColor(requireContext().getResources().getColor(R.color.success, null));
                    }
                } else {
                    tvBalance.setText("Balance: --");
                }
            })
            .addOnFailureListener(e -> tvBalance.setText("Balance: --"));
    }
}
