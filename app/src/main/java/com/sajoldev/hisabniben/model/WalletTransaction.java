package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class WalletTransaction implements Serializable {

    public static final String DIRECTION_IN = "IN";
    public static final String DIRECTION_OUT = "OUT";
    public static final String DIRECTION_TRANSFER = "TRANSFER";

    // Money In Types
    public static final String TYPE_CUSTOMER_PAYMENT = "CUSTOMER_PAYMENT";
    public static final String TYPE_CUSTOMER_OPENING_BALANCE_ADJUSTMENT = "CUSTOMER_OPENING_BALANCE_ADJUSTMENT";
    public static final String TYPE_MANUAL_CASH_IN = "MANUAL_CASH_IN";
    public static final String TYPE_TRANSFER_IN = "TRANSFER_IN";
    public static final String TYPE_OTHER_INCOME = "OTHER_INCOME";
    public static final String TYPE_REFUND_RECEIVED = "REFUND_RECEIVED";

    // Money Out Types
    public static final String TYPE_SUPPLIER_PAYMENT = "SUPPLIER_PAYMENT";
    public static final String TYPE_PURCHASE_PAYMENT = "PURCHASE_PAYMENT";
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_MANUAL_CASH_OUT = "MANUAL_CASH_OUT";
    public static final String TYPE_TRANSFER_OUT = "TRANSFER_OUT";
    public static final String TYPE_OTHER_PAYMENT = "OTHER_PAYMENT";

    // Neutral / Special Types
    public static final String TYPE_OPENING_BALANCE = "OPENING_BALANCE";
    public static final String TYPE_REVERSAL = "REVERSAL";

    // Status
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REVERSED = "REVERSED";

    private String transactionId;
    private String accountId;
    private String accountName;
    private String userId;

    private String type;
    private String direction;
    private String category;

    private double amount;
    private double balanceBefore;
    private double balanceAfter;

    private String title;
    private String description;

    private String customerId;
    private String customerName;

    private String supplierId;
    private String supplierName;

    private String saleId;
    private String purchaseId;
    private String expenseId;
    private String transferId;

    private String paymentMethod;
    private String reference;

    private long createdAt;
    private long transactionDate;
    private String createdBy;

    private String status;
    private String reversalTransactionId;

    public WalletTransaction() {
        this.status = STATUS_COMPLETED;
        this.createdAt = System.currentTimeMillis();
        this.transactionDate = System.currentTimeMillis();
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(double balanceBefore) { this.balanceBefore = balanceBefore; }

    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getPurchaseId() { return purchaseId; }
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getTransferId() { return transferId; }
    public void setTransferId(String transferId) { this.transferId = transferId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) {
        if (createdAt instanceof com.google.firebase.Timestamp) {
            this.createdAt = ((com.google.firebase.Timestamp) createdAt).toDate().getTime();
        } else if (createdAt instanceof Number) {
            this.createdAt = ((Number) createdAt).longValue();
        }
    }

    public long getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Object transactionDate) {
        if (transactionDate instanceof com.google.firebase.Timestamp) {
            this.transactionDate = ((com.google.firebase.Timestamp) transactionDate).toDate().getTime();
        } else if (transactionDate instanceof Number) {
            this.transactionDate = ((Number) transactionDate).longValue();
        }
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReversalTransactionId() { return reversalTransactionId; }
    public void setReversalTransactionId(String reversalTransactionId) { this.reversalTransactionId = reversalTransactionId; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", transactionId);
        map.put("accountId", accountId);
        map.put("accountName", accountName);
        map.put("userId", userId);
        map.put("type", type);
        map.put("direction", direction);
        map.put("category", category);
        map.put("amount", amount);
        map.put("balanceBefore", balanceBefore);
        map.put("balanceAfter", balanceAfter);
        map.put("title", title);
        map.put("description", description);
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("supplierId", supplierId);
        map.put("supplierName", supplierName);
        map.put("saleId", saleId);
        map.put("purchaseId", purchaseId);
        map.put("expenseId", expenseId);
        map.put("transferId", transferId);
        map.put("paymentMethod", paymentMethod);
        map.put("reference", reference);
        map.put("createdAt", createdAt);
        map.put("transactionDate", transactionDate);
        map.put("createdBy", createdBy);
        map.put("status", status);
        map.put("reversalTransactionId", reversalTransactionId);
        return map;
    }
}
