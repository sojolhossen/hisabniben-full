package com.sajoldev.hisabniben.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String storeName;
    private String storeType;
    private String address;
    private long trialStart;
    private long trialEnd;
    private boolean isPremium;
    private String subscriptionId;
    private Long subscriptionExpiryDate;
    private long createdAt;
    private long updatedAt;
    private boolean isAdmin;
    private Integer customerLimit;
    private Integer customerCount;
    private Integer transactionLimit;
    private Integer transactionCount;
    private Integer productLimit;
    private Integer productCount;
    private Integer smsLimit;
    private boolean trialUsed;
    private String subscriptionStatus;
    private String paymentMethod;
    private String transactionId;
    private String smsBusinessName;
    private String subscriptionPackageName;
    private String password;

    public User() {
    }

    public User(String uid, String name, String email, long trialStart, long trialEnd, boolean isPremium, String subscriptionId, long subscriptionExpiryDate, long createdAt, boolean isAdmin) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.trialStart = trialStart;
        this.trialEnd = trialEnd;
        this.isPremium = isPremium;
        this.subscriptionId = subscriptionId;
        this.subscriptionExpiryDate = subscriptionExpiryDate;
        this.createdAt = createdAt;
        this.isAdmin = isAdmin;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    @Exclude
    public String getShopName() { return storeName; }

    @Exclude
    public int getSmsBalance() { return smsLimit != null ? smsLimit : 0; }

    public String getStoreType() { return storeType; }
    public void setStoreType(String storeType) { this.storeType = storeType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public long getTrialStart() { return trialStart; }
    public void setTrialStart(Object trialStart) {
        if (trialStart instanceof com.google.firebase.Timestamp) {
            this.trialStart = ((com.google.firebase.Timestamp) trialStart).toDate().getTime();
        } else if (trialStart instanceof Number) {
            this.trialStart = ((Number) trialStart).longValue();
        }
    }

    public long getTrialEnd() { return trialEnd; }
    public void setTrialEnd(Object trialEnd) {
        if (trialEnd instanceof com.google.firebase.Timestamp) {
            this.trialEnd = ((com.google.firebase.Timestamp) trialEnd).toDate().getTime();
        } else if (trialEnd instanceof Number) {
            this.trialEnd = ((Number) trialEnd).longValue();
        }
    }

    @PropertyName("isPremium")
    public boolean isPremium() { return isPremium; }

    @PropertyName("isPremium")
    public void setPremium(boolean premium) { isPremium = premium; }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    public Long getSubscriptionExpiryDate() { return subscriptionExpiryDate; }
    public void setSubscriptionExpiryDate(Object subscriptionExpiryDate) {
        if (subscriptionExpiryDate instanceof com.google.firebase.Timestamp) {
            this.subscriptionExpiryDate = ((com.google.firebase.Timestamp) subscriptionExpiryDate).toDate().getTime();
        } else if (subscriptionExpiryDate instanceof Number) {
            this.subscriptionExpiryDate = ((Number) subscriptionExpiryDate).longValue();
        }
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) {
        if (createdAt instanceof com.google.firebase.Timestamp) {
            this.createdAt = ((com.google.firebase.Timestamp) createdAt).toDate().getTime();
        } else if (createdAt instanceof Number) {
            this.createdAt = ((Number) createdAt).longValue();
        }
    }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Object updatedAt) {
        if (updatedAt instanceof com.google.firebase.Timestamp) {
            this.updatedAt = ((com.google.firebase.Timestamp) updatedAt).toDate().getTime();
        } else if (updatedAt instanceof Number) {
            this.updatedAt = ((Number) updatedAt).longValue();
        }
    }

    @PropertyName("isAdmin")
    public boolean isAdmin() { return isAdmin; }

    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getCustomerLimit() { return customerLimit; }
    public void setCustomerLimit(Integer limit) { this.customerLimit = limit; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer count) { this.customerCount = count; }
    public Integer getTransactionLimit() { return transactionLimit; }
    public void setTransactionLimit(Integer limit) { this.transactionLimit = limit; }
    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer count) { this.transactionCount = count; }
    public Integer getProductLimit() { return productLimit; }
    public void setProductLimit(Integer limit) { this.productLimit = limit; }
    public Integer getProductCount() { return productCount; }
    public void setProductCount(Integer count) { this.productCount = count; }

    public Integer getSmsLimit() { return smsLimit; }
    public void setSmsLimit(Integer limit) { this.smsLimit = limit; }

    public boolean isTrialUsed() { return trialUsed; }
    public void setTrialUsed(boolean trialUsed) { this.trialUsed = trialUsed; }

    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String status) { this.subscriptionStatus = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String method) { this.paymentMethod = method; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String txId) { this.transactionId = txId; }

    public String getSmsBusinessName() { return smsBusinessName; }
    public void setSmsBusinessName(String smsBusinessName) { this.smsBusinessName = smsBusinessName; }

    public String getSubscriptionPackageName() { return subscriptionPackageName; }
    public void setSubscriptionPackageName(String subscriptionPackageName) { this.subscriptionPackageName = subscriptionPackageName; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone);
        map.put("storeName", storeName);
        map.put("storeType", storeType);
        map.put("address", address);
        map.put("trialStart", trialStart);
        map.put("trialEnd", trialEnd);
        map.put("trialUsed", trialUsed);
        map.put("subscriptionStatus", subscriptionStatus);
        map.put("isPremium", isPremium);
        map.put("subscriptionId", subscriptionId);
        map.put("subscriptionExpiryDate", subscriptionExpiryDate);
        map.put("subscriptionPackageName", subscriptionPackageName);
        map.put("paymentMethod", paymentMethod);
        map.put("transactionId", transactionId);
        map.put("smsBusinessName", smsBusinessName);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("isAdmin", isAdmin);
        map.put("customerLimit", customerLimit);
        map.put("customerCount", customerCount);
        map.put("transactionLimit", transactionLimit);
        map.put("transactionCount", transactionCount);
        map.put("productLimit", productLimit);
        map.put("productCount", productCount);
        map.put("smsLimit", smsLimit);
        return map;
    }

    @Exclude
    public boolean isOnTrial() {
        if ("TRIAL".equalsIgnoreCase(subscriptionStatus)) {
            return trialEnd > 0 && System.currentTimeMillis() < trialEnd;
        }
        return !isPremium && trialEnd > 0 && System.currentTimeMillis() < trialEnd;
    }

    @Exclude
    public long getRemainingTrialDays() {
        long remaining = trialEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / (1000 * 60 * 60 * 24) : 0;
    }
}
