package com.sajoldev.hisabniben.model;

import java.io.Serializable;

public class UnifiedHistoryItem implements Serializable {
    public static final String TYPE_SALE = "SALE";
    public static final String TYPE_PURCHASE = "PURCHASE";
    public static final String TYPE_MONEY_RECEIVE = "MONEY_RECEIVE";
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_STOCK_MOVEMENT = "STOCK_MOVEMENT";

    private String id;
    private String type;
    private String title;
    private String subtitle;
    private double amount;
    private double dueAmount;
    private double paidAmount;
    private long date;
    private String paymentMethod;
    private String status;
    private Object originalObject;

    public UnifiedHistoryItem() {}

    public UnifiedHistoryItem(String id, String type, String title, String subtitle, double amount, double dueAmount, double paidAmount, long date, String paymentMethod, String status, Object originalObject) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.amount = amount;
        this.dueAmount = dueAmount;
        this.paidAmount = paidAmount;
        this.date = date;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.originalObject = originalObject;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getDueAmount() { return dueAmount; }
    public void setDueAmount(double dueAmount) { this.dueAmount = dueAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getOriginalObject() { return originalObject; }
    public void setOriginalObject(Object originalObject) { this.originalObject = originalObject; }
}
