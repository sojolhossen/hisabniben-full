package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sale implements Serializable {
    public static final String PAYMENT_STATUS_PAID = "PAID";
    public static final String PAYMENT_STATUS_PARTIAL = "PARTIAL";
    public static final String PAYMENT_STATUS_DUE = "DUE";

    public static final String SALE_STATUS_CONFIRMED = "CONFIRMED";
    public static final String SALE_STATUS_CANCELLED = "CANCELLED";
    public static final String SALE_STATUS_RETURNED = "RETURNED";

    public static final String CASH_CUSTOMER_ID = "CASH_CUSTOMER";

    private String id;
    private String invoiceNo;
    private String userId;
    private String customerId = CASH_CUSTOMER_ID;
    private String customerName = "ক্যাশ কাস্টমার (Cash Customer)";
    private String customerPhone;
    private List<SaleItem> items = new ArrayList<>();
    private double grossAmount;
    private double discount;
    private double transportCharge;
    private double grandTotal;
    private double paidAmount;
    private double dueAmount;
    private double estimatedProfit;
    private String paymentStatus = PAYMENT_STATUS_PAID;
    private String saleStatus = SALE_STATUS_CONFIRMED;
    private String paymentMethod = "Cash";
    private String referenceNo;
    private long saleDate;
    private String notes;
    private long createdAt;
    private long updatedAt;

    public Sale() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }

    public double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(double grossAmount) { this.grossAmount = grossAmount; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTransportCharge() { return transportCharge; }
    public void setTransportCharge(double transportCharge) { this.transportCharge = transportCharge; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { 
        this.paidAmount = paidAmount;
        calculatePaymentStatus();
    }

    public double getDueAmount() { return dueAmount; }
    public void setDueAmount(double dueAmount) { 
        this.dueAmount = dueAmount;
        calculatePaymentStatus();
    }

    public double getEstimatedProfit() { return estimatedProfit; }
    public void setEstimatedProfit(double estimatedProfit) { this.estimatedProfit = estimatedProfit; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getSaleStatus() { return saleStatus; }
    public void setSaleStatus(String saleStatus) { this.saleStatus = saleStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public long getSaleDate() { return saleDate; }
    public void setSaleDate(long saleDate) { this.saleDate = saleDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public void calculatePaymentStatus() {
        if (dueAmount <= 0) {
            paymentStatus = PAYMENT_STATUS_PAID;
        } else if (paidAmount > 0) {
            paymentStatus = PAYMENT_STATUS_PARTIAL;
        } else {
            paymentStatus = PAYMENT_STATUS_DUE;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("invoiceNo", invoiceNo);
        map.put("userId", userId);
        map.put("customerId", customerId);
        map.put("customerName", customerName);
        map.put("customerPhone", customerPhone);

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        if (items != null) {
            for (SaleItem item : items) {
                itemMaps.add(item.toMap());
            }
        }
        map.put("items", itemMaps);
        map.put("grossAmount", grossAmount);
        map.put("discount", discount);
        map.put("transportCharge", transportCharge);
        map.put("grandTotal", grandTotal);
        map.put("paidAmount", paidAmount);
        map.put("dueAmount", dueAmount);
        map.put("estimatedProfit", estimatedProfit);
        map.put("paymentStatus", paymentStatus);
        map.put("saleStatus", saleStatus);
        map.put("paymentMethod", paymentMethod);
        map.put("referenceNo", referenceNo);
        map.put("saleDate", saleDate);
        map.put("notes", notes);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
