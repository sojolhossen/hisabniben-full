package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SupplierLedger implements Serializable {
    public static final String TYPE_PURCHASE = "PURCHASE";   // Credit (+ Supplier Payable)
    public static final String TYPE_PAYMENT = "PAYMENT";     // Debit (- Supplier Payable)
    public static final String TYPE_RETURN = "RETURN";       // Debit (- Supplier Payable)
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";

    private String id;
    private String userId;
    private String supplierId;
    private String supplierName;
    private String type;
    private double debit;            // Amount paid to supplier / returned (reduces payable)
    private double credit;           // Amount owed to supplier (Purchase increases payable)
    private double balance;          // Running payable balance
    private String referenceId;      // purchaseId, paymentId, returnId
    private String note;
    private long date;
    private long createdAt;

    public static final String TYPE_CREDIT_PURCHASE = "PURCHASE";

    public SupplierLedger() {}

    public SupplierLedger(String supplierId, String type, double debit, double credit, double balance, String note) {
        this.supplierId = supplierId;
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

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

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
        map.put("supplierId", supplierId);
        map.put("supplierName", supplierName);
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
