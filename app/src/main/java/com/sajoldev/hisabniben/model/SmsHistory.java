package com.sajoldev.hisabniben.model;

import java.io.Serializable;

public class SmsHistory implements Serializable {
    private String id;
    private String userId;
    private String customerPhone;
    private String customerName;
    private String message;
    private String type; // custom or transaction
    private String subType; // sale, purchase, payment, baki, return, custom
    private String status; // sent, failed, pending
    private String transactionId;
    private String businessNameUsed;
    private long timestamp;

    public SmsHistory() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getBusinessNameUsed() { return businessNameUsed; }
    public void setBusinessNameUsed(String businessNameUsed) { this.businessNameUsed = businessNameUsed; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}