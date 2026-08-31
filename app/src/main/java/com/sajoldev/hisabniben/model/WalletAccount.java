package com.sajoldev.hisabniben.model;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class WalletAccount implements Serializable {

    public static final String TYPE_CASH = "CASH";
    public static final String TYPE_BKASH = "BKASH";
    public static final String TYPE_NAGAD = "NAGAD";
    public static final String TYPE_BANK = "BANK";
    public static final String TYPE_OTHER = "OTHER";

    private String accountId;
    private String accountName;
    private String accountType;
    private double openingBalance;
    private double currentBalance;
    private boolean isActive;
    private String userId;
    private long createdAt;
    private long updatedAt;

    public WalletAccount() {
        this.isActive = true;
    }

    public WalletAccount(String accountId, String accountName, String accountType, double openingBalance, double currentBalance, String userId) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.openingBalance = openingBalance;
        this.currentBalance = currentBalance;
        this.userId = userId;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }

    @PropertyName("isActive")
    public boolean isActive() { return isActive; }

    @PropertyName("isActive")
    public void setActive(boolean active) { isActive = active; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("accountId", accountId);
        map.put("accountName", accountName);
        map.put("accountType", accountType);
        map.put("openingBalance", openingBalance);
        map.put("currentBalance", currentBalance);
        map.put("isActive", isActive);
        map.put("userId", userId);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
