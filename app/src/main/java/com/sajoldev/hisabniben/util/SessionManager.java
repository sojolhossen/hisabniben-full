package com.sajoldev.hisabniben.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "HisabNibenSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_IS_PREMIUM = "is_premium";
    private static final String KEY_IS_ADMIN = "is_admin";
    private static final String KEY_TRIAL_END = "trial_end";
    private static final String KEY_SUBSCRIPTION_EXPIRY = "subscription_expiry";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_CUSTOMER_COUNT = "customer_count";
    private static final String KEY_TRANSACTION_COUNT = "transaction_count";
    private static final String KEY_STORE_NAME = "store_name";
    private static final String KEY_LAST_NOTIFICATION_READ = "last_notification_read";
    private static final String KEY_CUSTOMER_LIMIT = "customer_limit";
    private static final String KEY_TRANSACTION_LIMIT = "transaction_limit";
    private static final String KEY_PRODUCT_LIMIT = "product_limit";

    private static final String KEY_DEFAULT_BAG_WEIGHT = "default_bag_weight";
    private static final String KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold";
    private static final String KEY_ALLOW_NEGATIVE_STOCK = "allow_negative_stock";
    private static final String KEY_DEFAULT_SALE_UNIT = "default_sale_unit";
    private static final String KEY_DEFAULT_PURCHASE_UNIT = "default_purchase_unit";
    private static final String KEY_DEFAULT_PAYMENT_METHOD = "default_payment_method";
    private static final String KEY_SMS_BUSINESS_NAME = "sms_business_name";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void createLoginSession(String userId, String name, String email, String storeName, boolean isPremium, boolean isAdmin, long trialEnd, long subscriptionExpiry) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, "");
        editor.putString(KEY_STORE_NAME, storeName != null ? storeName : "");
        editor.putBoolean(KEY_IS_PREMIUM, isPremium);
        editor.putBoolean(KEY_IS_ADMIN, isAdmin);
        editor.putLong(KEY_TRIAL_END, trialEnd);
        editor.putLong(KEY_SUBSCRIPTION_EXPIRY, subscriptionExpiry);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public void updatePremiumStatus(boolean isPremium, long subscriptionExpiry) {
        editor.putBoolean(KEY_IS_PREMIUM, isPremium);
        editor.putLong(KEY_SUBSCRIPTION_EXPIRY, subscriptionExpiry);
        editor.apply();
    }
    
    public void updateTrialStatus(long trialEnd) {
        editor.putLong(KEY_TRIAL_END, trialEnd);
        editor.apply();
    }

    public void updateUserInfo(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public void updateUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public void updateCustomerCount(int count) {
        editor.putInt(KEY_CUSTOMER_COUNT, count);
        editor.apply();
    }

    public void updateTransactionCount(int count) {
        editor.putInt(KEY_TRANSACTION_COUNT, count);
        editor.apply();
    }
    
    public int getCustomerLimit() {
        return prefs.getInt(KEY_CUSTOMER_LIMIT, 10);
    }
    
    public void setCustomerLimit(int limit) {
        editor.putInt(KEY_CUSTOMER_LIMIT, limit);
        editor.apply();
    }
    
    public int getTransactionLimit() {
        return prefs.getInt(KEY_TRANSACTION_LIMIT, 100);
    }
    
    public void setTransactionLimit(int limit) {
        editor.putInt(KEY_TRANSACTION_LIMIT, limit);
        editor.apply();
    }
    
    public int getProductLimit() {
        return prefs.getInt(KEY_PRODUCT_LIMIT, 10);
    }
    
    public void setProductLimit(int limit) {
        editor.putInt(KEY_PRODUCT_LIMIT, limit);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getStoreName() {
        return prefs.getString(KEY_STORE_NAME, "");
    }

    public void updateStoreName(String storeName) {
        editor.putString(KEY_STORE_NAME, storeName);
        editor.apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public boolean isPremium() {
        return prefs.getBoolean(KEY_IS_PREMIUM, false);
    }

    public boolean isAdmin() {
        return prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    public long getTrialEnd() {
        return prefs.getLong(KEY_TRIAL_END, 0);
    }

    public long getSubscriptionExpiry() {
        return prefs.getLong(KEY_SUBSCRIPTION_EXPIRY, 0);
    }

    public int getCustomerCount() {
        return prefs.getInt(KEY_CUSTOMER_COUNT, 0);
    }

    public int getTransactionCount() {
        return prefs.getInt(KEY_TRANSACTION_COUNT, 0);
    }

    public boolean isOnTrial() {
        long trialEnd = getTrialEnd();
        return !isPremium() && trialEnd > 0 && System.currentTimeMillis() < trialEnd;
    }
    
    public boolean isLimited() {
        return !isPremium() && !isOnTrial();
    }

    public long getRemainingTrialDays() {
        long remaining = getTrialEnd() - System.currentTimeMillis();
        return remaining > 0 ? remaining / (1000 * 60 * 60 * 24) : 0;
    }

    public long getRemainingSubscriptionDays() {
        long remaining = getSubscriptionExpiry() - System.currentTimeMillis();
        return remaining > 0 ? remaining / (1000 * 60 * 60 * 24) : 0;
    }

    public String getSubscriptionPackageName() {
        return prefs.getString("package_name", "প্রিমিয়াম প্যাকেজ");
    }

    public void setSubscriptionPackageName(String name) {
        editor.putString("package_name", name);
        editor.apply();
    }

    public long getLastNotificationReadTime() {
        return prefs.getLong(KEY_LAST_NOTIFICATION_READ, 0);
    }

    public void setLastNotificationReadTime(long time) {
        editor.putLong(KEY_LAST_NOTIFICATION_READ, time);
        editor.apply();
    }

    public boolean isNotificationRead(String notifId) {
        if (notifId == null) return false;
        return prefs.getBoolean("read_notif_" + notifId, false);
    }

    public void setNotificationRead(String notifId) {
        if (notifId == null) return;
        editor.putBoolean("read_notif_" + notifId, true);
        editor.apply();
    }

    public boolean isNotificationDeleted(String notifId) {
        if (notifId == null) return false;
        return prefs.getBoolean("deleted_notif_" + notifId, false);
    }

    public void setNotificationDeleted(String notifId) {
        if (notifId == null) return;
        editor.putBoolean("deleted_notif_" + notifId, true);
        editor.apply();
    }

    public String getFcmToken() {
        return prefs.getString("fcm_token", null);
    }

    public void setFcmToken(String token) {
        editor.putString("fcm_token", token);
        editor.apply();
    }

    public int getDefaultBagWeight() {
        return prefs.getInt(KEY_DEFAULT_BAG_WEIGHT, 50);
    }

    public void setDefaultBagWeight(int kg) {
        editor.putInt(KEY_DEFAULT_BAG_WEIGHT, kg);
        editor.apply();
    }

    public int getLowStockThreshold() {
        return prefs.getInt(KEY_LOW_STOCK_THRESHOLD, 100);
    }

    public void setLowStockThreshold(int kg) {
        editor.putInt(KEY_LOW_STOCK_THRESHOLD, kg);
        editor.apply();
    }

    public boolean getAllowNegativeStock() {
        return prefs.getBoolean(KEY_ALLOW_NEGATIVE_STOCK, false);
    }

    public void setAllowNegativeStock(boolean allow) {
        editor.putBoolean(KEY_ALLOW_NEGATIVE_STOCK, allow);
        editor.apply();
    }

    public String getDefaultSaleUnit() {
        return prefs.getString(KEY_DEFAULT_SALE_UNIT, "KG");
    }

    public void setDefaultSaleUnit(String unit) {
        editor.putString(KEY_DEFAULT_SALE_UNIT, unit);
        editor.apply();
    }

    public String getDefaultPurchaseUnit() {
        return prefs.getString(KEY_DEFAULT_PURCHASE_UNIT, "KG");
    }

    public void setDefaultPurchaseUnit(String unit) {
        editor.putString(KEY_DEFAULT_PURCHASE_UNIT, unit);
        editor.apply();
    }

    public String getDefaultPaymentMethod() {
        return prefs.getString(KEY_DEFAULT_PAYMENT_METHOD, "Cash");
    }

    public void setDefaultPaymentMethod(String method) {
        editor.putString(KEY_DEFAULT_PAYMENT_METHOD, method);
        editor.apply();
    }

    public String getSmsBusinessName() {
        return prefs.getString(KEY_SMS_BUSINESS_NAME, "");
    }

    public void setSmsBusinessName(String name) {
        editor.putString(KEY_SMS_BUSINESS_NAME, name);
        editor.apply();
    }

    public boolean isVideoWatched(String videoId) {
        if (videoId == null || videoId.isEmpty()) return false;
        return prefs.getBoolean("watched_video_" + videoId, false);
    }

    public void markVideoAsWatched(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        editor.putBoolean("watched_video_" + videoId, true);
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
