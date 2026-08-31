package com.sajoldev.hisabniben.util;

import com.sajoldev.hisabniben.model.User;

public class SmsTemplateManager {

    /**
     * Priority Resolution:
     * 1. smsBusinessName (if set)
     * 2. businessName / storeName (if set)
     * 3. Session stored smsBusinessName / storeName
     * 4. Safe generic fallback (e.g. "চালের ব্যবসা"). NEVER "HisabNiben"!
     */
    public static String getEffectiveSmsBusinessName(User user, SessionManager sessionManager) {
        if (user != null && user.getSmsBusinessName() != null && !user.getSmsBusinessName().trim().isEmpty()) {
            return user.getSmsBusinessName().trim();
        }
        if (user != null && user.getStoreName() != null && !user.getStoreName().trim().isEmpty()) {
            return user.getStoreName().trim();
        }
        if (sessionManager != null && sessionManager.getSmsBusinessName() != null && !sessionManager.getSmsBusinessName().trim().isEmpty()) {
            return sessionManager.getSmsBusinessName().trim();
        }
        if (sessionManager != null && sessionManager.getStoreName() != null && !sessionManager.getStoreName().trim().isEmpty()) {
            return sessionManager.getStoreName().trim();
        }
        return "চালের ব্যবসা";
    }

    /**
     * A. Rice Sale Confirmation
     */
    public static String buildSaleSms(String customerName, String itemsSummaryOrBags, double totalKg, double totalAmount, double paidAmount, double dueAmount, String smsBusinessName) {
        String name = customerName != null && !customerName.isEmpty() ? customerName : "গ্রাহক";
        String bizName = smsBusinessName != null && !smsBusinessName.isEmpty() ? smsBusinessName : "চালের ব্যবসা";
        
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(", আপনার কাছে ").append(itemsSummaryOrBags);
        if (totalKg > 0) {
            sb.append(" (").append(UnitConverterHelper.formatKg(totalKg)).append(")");
        }
        sb.append(" চাল বিক্রি করা হয়েছে।\n");
        sb.append("মোট মূল্য ").append(UnitConverterHelper.formatCurrency(totalAmount)).append("।\n");
        sb.append("জমা ").append(UnitConverterHelper.formatCurrency(paidAmount)).append("।\n");
        sb.append("বর্তমান বাকি ").append(UnitConverterHelper.formatCurrency(dueAmount)).append("।\n\n");
        sb.append("— ").append(bizName);
        return sb.toString();
    }

    /**
     * B. Customer Payment Received
     */
    public static String buildCustomerPaymentSms(String customerName, double paidAmount, double dueAmount, String smsBusinessName) {
        String name = customerName != null && !customerName.isEmpty() ? customerName : "গ্রাহক";
        String bizName = smsBusinessName != null && !smsBusinessName.isEmpty() ? smsBusinessName : "চালের ব্যবসা";

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(", আপনার কাছ থেকে ").append(UnitConverterHelper.formatCurrency(paidAmount)).append(" টাকা জমা নেওয়া হয়েছে।\n");
        sb.append("বর্তমান বাকি ").append(UnitConverterHelper.formatCurrency(dueAmount)).append("।\n\n");
        sb.append("— ").append(bizName);
        return sb.toString();
    }

    /**
     * C. Due Reminder
     */
    public static String buildDueReminderSms(String customerName, double dueAmount, String smsBusinessName) {
        String name = customerName != null && !customerName.isEmpty() ? customerName : "গ্রাহক";
        String bizName = smsBusinessName != null && !smsBusinessName.isEmpty() ? smsBusinessName : "চালের ব্যবসা";

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(", আপনার বর্তমান বাকি ").append(UnitConverterHelper.formatCurrency(dueAmount)).append("।\n");
        sb.append("অনুগ্রহ করে সুবিধামতো বকেয়া পরিশোধ করুন।\n\n");
        sb.append("— ").append(bizName);
        return sb.toString();
    }

    /**
     * D. Full Payment
     */
    public static String buildFullPaymentSms(String customerName, double totalPaid, String smsBusinessName) {
        String name = customerName != null && !customerName.isEmpty() ? customerName : "গ্রাহক";
        String bizName = smsBusinessName != null && !smsBusinessName.isEmpty() ? smsBusinessName : "চালের ব্যবসা";

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(", আপনার পূর্বের বকেয়া ").append(UnitConverterHelper.formatCurrency(totalPaid)).append(" সম্পূর্ণ পরিশোধ হয়েছে।\n");
        sb.append("আপনাকে ধন্যবাদ।\n\n");
        sb.append("— ").append(bizName);
        return sb.toString();
    }

    /**
     * E. Customer Statement
     */
    public static String buildCustomerStatementSms(String customerName, double totalSale, double totalPaid, double currentDue, String smsBusinessName) {
        String name = customerName != null && !customerName.isEmpty() ? customerName : "গ্রাহক";
        String bizName = smsBusinessName != null && !smsBusinessName.isEmpty() ? smsBusinessName : "চালের ব্যবসা";

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(", আপনার হিসাব:\n");
        sb.append("মোট বিক্রি ").append(UnitConverterHelper.formatCurrency(totalSale)).append("\n");
        sb.append("মোট জমা ").append(UnitConverterHelper.formatCurrency(totalPaid)).append("\n");
        sb.append("বর্তমান বাকি ").append(UnitConverterHelper.formatCurrency(currentDue)).append("\n\n");
        sb.append("— ").append(bizName);
        return sb.toString();
    }

    /**
     * F. Supplier Purchase Confirmation
     */
    public static String buildPurchaseSms(String supplierName, String itemsSummaryOrBags, double totalKg, double totalAmount, double paidAmount, double dueAmount, String smsBusinessName) {
        String name = supplierName != null && !supplierName.isEmpty() ? supplierName : "মহাজন";
        String bizName = smsBusinessName != null && !smsBusinessName.isEmpty() ? smsBusinessName : "চালের ব্যবসা";

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(", আজ আপনার কাছ থেকে ").append(itemsSummaryOrBags);
        if (totalKg > 0) {
            sb.append(" (").append(UnitConverterHelper.formatKg(totalKg)).append(")");
        }
        sb.append(" চাল কেনা হয়েছে।\n");
        sb.append("মোট মূল্য ").append(UnitConverterHelper.formatCurrency(totalAmount)).append("।\n");
        sb.append("পরিশোধ ").append(UnitConverterHelper.formatCurrency(paidAmount)).append("।\n");
        sb.append("বর্তমান পাওনা ").append(UnitConverterHelper.formatCurrency(dueAmount)).append("।\n\n");
        sb.append("— ").append(bizName);
        return sb.toString();
    }

    /**
     * G. Supplier Payment Sent
     */
    public static String buildSupplierPaymentSms(String supplierName, double paidAmount, double dueAmount, String smsBusinessName) {
        String name = supplierName != null && !supplierName.isEmpty() ? supplierName : "মহাজন";
        String bizName = smsBusinessName != null && !smsBusinessName.isEmpty() ? smsBusinessName : "চালের ব্যবসা";

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(", আপনার পাওনা থেকে ").append(UnitConverterHelper.formatCurrency(paidAmount)).append(" টাকা পরিশোধ করা হয়েছে।\n");
        sb.append("বর্তমান পাওনা ").append(UnitConverterHelper.formatCurrency(dueAmount)).append("।\n\n");
        sb.append("— ").append(bizName);
        return sb.toString();
    }
}
