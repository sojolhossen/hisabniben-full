package com.sajoldev.hisabniben.util;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.activity.BuySmsActivity;
import com.sajoldev.hisabniben.model.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class SmsSenderHelper {

    public interface SmsCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public static void sendSms(Context context, String recipientPhone, String recipientName, String message, String type, String subType, SmsCallback callback) {
        if (recipientPhone == null || recipientPhone.trim().isEmpty()) {
            if (callback != null) callback.onError("প্রাপকের ফোন নম্বর পাওয়া যায়নি");
            return;
        }

        SessionManager sessionManager = SessionManager.getInstance(context);
        String userId = sessionManager.getUserId();
        if (userId == null) {
            if (callback != null) callback.onError("ব্যবহারকারী লগইন অবস্থায় নেই");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Fetch User Data to verify balance and effective SMS Business Name
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(userDoc -> {
                if (!userDoc.exists()) {
                    if (callback != null) callback.onError("ব্যবহারকারীর তথ্য পাওয়া যায়নি");
                    return;
                }

                User user = userDoc.toObject(User.class);
                final String effectiveBusinessName = SmsTemplateManager.getEffectiveSmsBusinessName(user, sessionManager);

                Long smsLimit = userDoc.getLong("smsLimit");
                int currentBalance = smsLimit != null ? smsLimit.intValue() : 0;
                int smsCount = calculateSmsCount(message);

                // 2. Check SMS Balance Protection
                if (currentBalance < smsCount) {
                    showInsufficientSmsDialog(context, currentBalance, smsCount);
                    if (callback != null) callback.onError("আপনার SMS ব্যালেন্স শেষ হয়ে গেছে।");
                    return;
                }

                // 3. Fetch SMS API settings
                db.collection("settings").document("sms_api")
                    .get()
                    .addOnSuccessListener(apiDoc -> {
                        if (!apiDoc.exists()) {
                            Toast.makeText(context, "SMS API কনফিগার করা হয়নি", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onError("SMS API not configured");
                            return;
                        }

                        String apiKey = apiDoc.getString("apiKey");
                        String senderId = apiDoc.getString("senderId");

                        if (apiKey == null || apiKey.isEmpty() || senderId == null || senderId.isEmpty()) {
                            Toast.makeText(context, "SMS API কি পাওয়া যায়নি। অ্যাডমিনের সাথে যোগাযোগ করুন।", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onError("SMS API keys missing");
                            return;
                        }

                        // 4. Send SMS via BulkSMSBD API
                        executeHttpSend(context, userId, recipientPhone, recipientName, message, type, subType, apiKey, senderId, smsCount, currentBalance, effectiveBusinessName, callback);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError("SMS API সেটিংস লোড করতে ব্যর্থ: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                if (callback != null) callback.onError("ব্যালেন্স পরীক্ষা করতে ব্যর্থ: " + e.getMessage());
            });
    }

    private static int calculateSmsCount(String message) {
        if (message == null || message.isEmpty()) return 1;
        // Standard unicode SMS length logic (70 chars per segment for Unicode/Bangla)
        int length = message.length();
        if (length <= 70) return 1;
        if (length <= 134) return 2;
        return (int) Math.ceil((double) length / 67.0);
    }

    private static void showInsufficientSmsDialog(Context context, int currentBalance, int required) {
        new Handler(Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(context)
                .setTitle("SMS ব্যালেন্স শেষ!")
                .setMessage("আপনার SMS ব্যালেন্স শেষ হয়ে গেছে। নতুন বার্তা পাঠাতে SMS রিচার্জ করুন।\n\nবর্তমান ব্যালেন্স: " + currentBalance + " SMS")
                .setPositiveButton("SMS কিনুন", (dialog, which) -> {
                    Intent intent = new Intent(context, BuySmsActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("বাতিল", null)
                .show();
        });
    }

    private static void executeHttpSend(Context context, String userId, String phone, String recipientName, String message, String type, String subType, String apiKey, String senderId, int smsCount, int currentBalance, String businessNameUsed, SmsCallback callback) {
        new Thread(() -> {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (!cleanPhone.startsWith("880")) {
                if (cleanPhone.startsWith("0")) {
                    cleanPhone = "88" + cleanPhone;
                } else {
                    cleanPhone = "880" + cleanPhone;
                }
            }

            try {
                String encodedMessage = URLEncoder.encode(message, "UTF-8");
                String url = "http://bulksmsbd.net/api/smsapi?api_key=" + apiKey 
                        + "&type=text&number=" + cleanPhone 
                        + "&senderid=" + senderId 
                        + "&message=" + encodedMessage;

                Log.d("SmsSenderHelper", "Sending SMS to: " + cleanPhone + " | BusinessName: " + businessNameUsed);

                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    String responseStr = response.toString().trim();
                    Log.d("SmsSenderHelper", "API Response: " + responseStr);

                    boolean isSuccess = false;
                    String failReason = "";

                    try {
                        org.json.JSONObject jsonObj = new org.json.JSONObject(responseStr);
                        int apiCode = jsonObj.optInt("response_code", 0);
                        String messageStr = jsonObj.optString("message", "");
                        String successMsg = jsonObj.optString("success_message", "");
                        String errorMsg = jsonObj.optString("error_message", "");

                        if (apiCode == 202 || apiCode == 200 || 
                            successMsg.toLowerCase().contains("success") || 
                            messageStr.toLowerCase().contains("success") ||
                            messageStr.toLowerCase().contains("submitted")) {
                            isSuccess = true;
                        } else {
                            isSuccess = false;
                            failReason = !errorMsg.isEmpty() ? errorMsg : (!messageStr.isEmpty() ? messageStr : "Gateway error code: " + apiCode);
                        }
                    } catch (Exception parseEx) {
                        String lowerResp = responseStr.toLowerCase();
                        if (lowerResp.contains("success") || lowerResp.contains("202") || lowerResp.contains("submitted")) {
                            isSuccess = true;
                        } else {
                            isSuccess = false;
                            failReason = responseStr;
                        }
                    }

                    if (isSuccess) {
                        // ONLY deduct SMS balance when gateway confirms real success (202 / Submitted)
                        int newBalance = Math.max(0, currentBalance - smsCount);
                        deductSmsBalance(userId, newBalance);
                        saveSmsHistory(userId, phone, recipientName, message, type, subType, "sent", businessNameUsed);

                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "SMS সফলভাবে পাঠানো হয়েছে!", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onSuccess();
                        });
                    } else {
                        // DO NOT DEDUCT BALANCE! Record failure reason
                        Log.e("SmsSenderHelper", "SMS dispatch failed: " + failReason);
                        saveSmsHistory(userId, phone, recipientName, message, type, subType, "failed (" + failReason + ")", businessNameUsed);
                        final String finalReason = failReason;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "SMS পাঠাতে ব্যর্থ: " + finalReason + "\n(ব্যালেন্স কাটা হয়নি)", Toast.LENGTH_LONG).show();
                            if (callback != null) callback.onError(finalReason);
                        });
                    }
                } else {
                    Log.e("SmsSenderHelper", "HTTP Error: " + responseCode);
                    saveSmsHistory(userId, phone, recipientName, message, type, subType, "failed (HTTP " + responseCode + ")", businessNameUsed);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, "SMS নেটওয়ার্ক ত্রুটি (HTTP " + responseCode + ")। ব্যালেন্স কাটা হয়নি।", Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onError("HTTP Error: " + responseCode);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("SmsSenderHelper", "Error sending SMS: " + e.getMessage());
                saveSmsHistory(userId, phone, recipientName, message, type, subType, "failed", businessNameUsed);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "SMS নেটওয়ার্ক ত্রুটি: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    private static void deductSmsBalance(String userId, int newBalance) {
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .update("smsLimit", newBalance)
            .addOnSuccessListener(aVoid -> Log.d("SmsSenderHelper", "Deducted balance to: " + newBalance))
            .addOnFailureListener(e -> Log.e("SmsSenderHelper", "Failed to deduct balance: " + e.getMessage()));
    }

    private static void saveSmsHistory(String userId, String phone, String recipientName, String message, String type, String subType, String status, String businessNameUsed) {
        Map<String, Object> history = new HashMap<>();
        history.put("userId", userId);
        history.put("customerPhone", phone);
        history.put("customerName", recipientName != null ? recipientName : "গ্রাহক");
        history.put("message", message);
        history.put("type", type != null ? type : "transaction");
        history.put("subType", subType != null ? subType : "general");
        history.put("status", status);
        history.put("businessNameUsed", businessNameUsed);
        history.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
            .collection("sms_history")
            .add(history)
            .addOnSuccessListener(docRef -> Log.d("SmsSenderHelper", "Logged to sms_history"))
            .addOnFailureListener(e -> Log.e("SmsSenderHelper", "Failed to log history: " + e.getMessage()));
    }
}
