package com.sajoldev.hisabniben.model;

import java.util.HashMap;
import java.util.Map;

public class Transaction implements java.io.Serializable {
    public static final String TYPE_PAYMENT = "payment";
    public static final String TYPE_BAKI = "baki";
    public static final String TYPE_CUSTOMER_PAYMENT = "customer_payment";
    public static final String TYPE_SUPPLIER_REFUND = "supplier_refund";
    public static final String TYPE_OWNER_INVESTMENT = "owner_investment";
    public static final String TYPE_OTHER_INCOME = "other_income";
    public static final String TYPE_EXPENSE = "expense";

    private String id;
    private String userId;
    private String customerId;
    private String customerName;
    private String supplierId;
    private String supplierName;
    private String type;
    private double amount;
    private double previousBaki;
    private double newBaki;
    private String paymentMethod = "Cash";
    private String reference;
    private String note;
    private long date;
    private long createdAt;

    public Transaction() {
    }

    public Transaction(String id, String userId, String customerId, String customerName, String type, double amount, double previousBaki, double newBaki, String note, long date, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.type = type;
        this.amount = amount;
        this.previousBaki = previousBaki;
        this.newBaki = newBaki;
        this.note = note;
        this.date = date;
        this.createdAt = createdAt;
    }

    public Transaction(String customerId, String customerName, String type, double amount, String note) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.date = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPreviousBaki() {
        return previousBaki;
    }

    public void setPreviousBaki(double previousBaki) {
        this.previousBaki = previousBaki;
    }

    public double getNewBaki() {
        return newBaki;
    }

    public void setNewBaki(double newBaki) {
        this.newBaki = newBaki;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("supplierId", supplierId);
        map.put("supplierName", supplierName);
        map.put("type", type);
        map.put("amount", amount);
        map.put("previousBaki", previousBaki);
        map.put("newBaki", newBaki);
        map.put("paymentMethod", paymentMethod);
        map.put("reference", reference);
        map.put("note", note);
        map.put("date", date);
        map.put("createdAt", createdAt);
        return map;
    }
}
