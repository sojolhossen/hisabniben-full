package com.sajoldev.hisabniben.model;

import java.util.HashMap;
import java.util.Map;

public class Customer implements java.io.Serializable {
    private String id;
    private String uid;
    private String userId;
    private String name;
    private String phone;
    private String address;
    private double baki;
    private long createdAt;
    private long updatedAt;

    public Customer() {
    }

    public Customer(String id, String userId, String name, String phone, String address, double baki, long createdAt, long updatedAt) {
        this.id = id;
        this.uid = id;
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.baki = baki;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid != null ? uid : id;
    }

    public void setUid(String uid) {
        this.uid = uid;
        if (this.id == null) {
            this.id = uid;
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getBaki() {
        return baki;
    }

    public double getCurrentBalance() {
        return baki;
    }

    public double getTotalPaid() {
        return 0.0;
    }

    public void setBaki(double baki) {
        this.baki = baki;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String businessName;
    private String customerType;
    private String notes;
    private double openingBalance;

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("uid", uid != null ? uid : id);
        map.put("userId", userId);
        map.put("name", name);
        map.put("businessName", businessName);
        map.put("customerType", customerType);
        map.put("phone", phone);
        map.put("address", address);
        map.put("notes", notes);
        map.put("openingBalance", openingBalance);
        map.put("baki", baki);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
