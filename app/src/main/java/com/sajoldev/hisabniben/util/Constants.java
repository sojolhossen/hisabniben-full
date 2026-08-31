package com.sajoldev.hisabniben.util;

public class Constants {
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_PHONE = "user_phone";
    public static final String KEY_IS_PREMIUM = "is_premium";
    public static final String KEY_IS_ADMIN = "is_admin";
    public static final String KEY_TRIAL_END = "trial_end";
    public static final String KEY_SUBSCRIPTION_EXPIRY = "subscription_expiry";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_OTP = "otp";
    public static final String KEY_OTP_TIMESTAMP = "otp_timestamp";
    
    public static final long OTP_VALIDITY_DURATION = 5 * 60 * 1000;
    public static final int TRIAL_DAYS = 7;
    public static final int MAX_CUSTOMERS_TRIAL = 5;
    public static final int MAX_TRANSACTIONS_TRIAL = 20;
    
    public static final int REQUEST_CUSTOMERS = 1001;
    public static final int REQUEST_TRANSACTIONS = 1002;
    public static final int REQUEST_SUBSCRIPTION = 1003;
    
    public static final String ACTION_NOTIFICATION = "com.sajoldev.hisabniben.NOTIFICATION";
    public static final String EXTRA_NOTIFICATION_TYPE = "notification_type";
    public static final String EXTRA_NOTIFICATION_TITLE = "notification_title";
    public static final String EXTRA_NOTIFICATION_MESSAGE = "notification_message";

    // Rice Business SMS Templates
    public static final String SMS_TEMPLATE_DUE_REMINDER = "প্রিয় {customer_name}, {business_name}-এ আপনার বর্তমান বাকী {due_amount}। অনুগ্রহ করে সুবিধামতো পরিশোধ করুন। ধন্যবাদ।";
    public static final String SMS_TEMPLATE_PAYMENT_CONFIRMATION = "প্রিয় {customer_name}, {business_name}-এ {paid_amount} জমা নেওয়া হয়েছে। আপনার বর্তমান বাকি {due_amount}। ধন্যবাদ।";
    public static final String SMS_TEMPLATE_SALE_STATEMENT = "প্রিয় {customer_name}, {business_name}-এ চাল ক্রয়ের মেমো #{invoice_number}: {total_bags} বস্তা ({total_kg})। মোট {total_amount}, জমা {paid_amount}, বাকী {due_amount}।";

    public static String buildSaleSmsMessage(String template, String customerName, String businessName, String invoiceNo, double totalKg, double totalBags, double totalAmount, double paidAmount, double dueAmount) {
        if (template == null) template = SMS_TEMPLATE_SALE_STATEMENT;
        return template
            .replace("{customer_name}", customerName != null ? customerName : "গ্রাহক")
            .replace("{business_name}", businessName != null ? businessName : "চালের ব্যবসা")
            .replace("{invoice_number}", invoiceNo != null ? invoiceNo : "")
            .replace("{total_kg}", UnitConverterHelper.formatKg(totalKg))
            .replace("{total_bags}", String.valueOf((int)totalBags))
            .replace("{total_amount}", UnitConverterHelper.formatCurrency(totalAmount))
            .replace("{paid_amount}", UnitConverterHelper.formatCurrency(paidAmount))
            .replace("{due_amount}", UnitConverterHelper.formatCurrency(dueAmount));
    }

    public static final String SMS_TEMPLATE_PURCHASE_STATEMENT = "প্রিয় {supplier_name}, {business_name}-এ চাল ক্রয়ের মেমো #{purchase_id}: {total_bags} বস্তা ({total_kg})। মোট {total_amount}, পরিশোধ {paid_amount}, বাকি {due_amount}।";

    public static String buildPurchaseSmsMessage(String template, String supplierName, String businessName, String purchaseId, double totalKg, double totalBags, double totalAmount, double paidAmount, double dueAmount) {
        if (template == null) template = SMS_TEMPLATE_PURCHASE_STATEMENT;
        return template
            .replace("{supplier_name}", supplierName != null ? supplierName : "মহাজন")
            .replace("{business_name}", businessName != null ? businessName : "চালের ব্যবসা")
            .replace("{purchase_id}", purchaseId != null ? purchaseId : "")
            .replace("{total_kg}", UnitConverterHelper.formatKg(totalKg))
            .replace("{total_bags}", String.valueOf((int)totalBags))
            .replace("{total_amount}", UnitConverterHelper.formatCurrency(totalAmount))
            .replace("{paid_amount}", UnitConverterHelper.formatCurrency(paidAmount))
            .replace("{due_amount}", UnitConverterHelper.formatCurrency(dueAmount));
    }
}
