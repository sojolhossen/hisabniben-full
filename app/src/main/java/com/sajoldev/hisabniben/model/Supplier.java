package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Supplier implements Serializable {
    private String id;
    private String userId;
    private String name;
    private String phone;
    private String businessName;
    private String address;
    private String supplierType; // Rice Mill, Wholesaler, Trader, Importer, Other
    private double openingBalance;
    private double totalPurchase;
    private double totalPaid;
    private double currentPayable;
    private long lastTransaction;
    private String notes;
    private long createdAt;
    private long updatedAt;

    public Supplier() {}

    public Supplier(String id, String userId, String name, String phone, String businessName, String address, double currentPayable) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.businessName = businessName;
        this.address = address;
        this.currentPayable = currentPayable;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getSupplierType() { return supplierType; }
    public void setSupplierType(String supplierType) { this.supplierType = supplierType; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public double getTotalPurchase() { return totalPurchase; }
    public void setTotalPurchase(double totalPurchase) { this.totalPurchase = totalPurchase; }

    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }

    public double getCurrentPayable() { return currentPayable; }
    public void setCurrentPayable(double currentPayable) { this.currentPayable = currentPayable; }

    public long getLastTransaction() { return lastTransaction; }
    public void setLastTransaction(long lastTransaction) { this.lastTransaction = lastTransaction; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("name", name);
        map.put("phone", phone);
        map.put("businessName", businessName);
        map.put("address", address);
        map.put("supplierType", supplierType);
        map.put("openingBalance", openingBalance);
        map.put("totalPurchase", totalPurchase);
        map.put("totalPaid", totalPaid);
        map.put("currentPayable", currentPayable);
        map.put("lastTransaction", lastTransaction);
        map.put("notes", notes);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
