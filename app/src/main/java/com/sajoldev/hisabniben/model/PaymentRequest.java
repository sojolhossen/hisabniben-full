package com.sajoldev.hisabniben.model;

public class PaymentRequest {
    private String id;
    private String userId;
    private String userName;
    private String packageId;
    private String packageName;
    private String paymentMethodId;
    private String paymentMethodName;
    private String senderNumber;
    private String transactionId;
    private int amount;
    private int smsCount;
    private String status;
    private long createdAt;
    private String notes;

    public PaymentRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    public String getSenderNumber() { return senderNumber; }
    public void setSenderNumber(String senderNumber) { this.senderNumber = senderNumber; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public int getSmsCount() { return smsCount; }
    public void setSmsCount(int smsCount) { this.smsCount = smsCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}