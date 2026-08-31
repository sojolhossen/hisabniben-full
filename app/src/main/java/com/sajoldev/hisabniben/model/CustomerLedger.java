package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CustomerLedger implements Serializable {
    public static final String TYPE_SALE = "SALE";           // Debit (+ Customer Due)
    public static final String TYPE_PAYMENT = "PAYMENT";     // Credit (- Customer Due)
    public static final String TYPE_RETURN = "RETURN";       // Credit (- Customer Due)
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";

    private String id;
    private String userId;
    private String customerId;
    private String customerName;
    private String type;
    private double debit;            // Amount owed by customer (Sales)
    private double credit;           // Amount paid by customer (Payments/Returns)
    private double balance;          // Running due balance
    private String referenceId;      // saleId, paymentId, returnId
    private String note;
    private long date;
    private long createdAt;

    public static final String TYPE_DEBIT_SALE = "SALE";

    public CustomerLedger() {}

    public CustomerLedger(String customerId, String type, double debit, double credit, double balance, String note) {
        this.customerId = customerId;
        this.type = type;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
        this.note = note;
        this.date = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getDebit() { return debit; }
    public void setDebit(double debit) { this.debit = debit; }

    public double getCredit() { return credit; }
    public void setCredit(double credit) { this.credit = credit; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("type", type);
        map.put("debit", debit);
        map.put("credit", credit);
        map.put("balance", balance);
        map.put("referenceId", referenceId);
        map.put("note", note);
        map.put("date", date);
        map.put("createdAt", createdAt);
        return map;
    }
}
