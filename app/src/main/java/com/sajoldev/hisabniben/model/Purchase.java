package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Purchase implements Serializable {
    public static final String PAYMENT_STATUS_PAID = "PAID";
    public static final String PAYMENT_STATUS_PARTIAL = "PARTIAL";
    public static final String PAYMENT_STATUS_DUE = "DUE";

    public static final String PURCHASE_STATUS_CONFIRMED = "CONFIRMED";
    public static final String PURCHASE_STATUS_CANCELLED = "CANCELLED";
    public static final String PURCHASE_STATUS_RETURNED = "RETURNED";

    private String id;
    private String invoiceNo;
    private String userId;
    private String supplierId;
    private String supplierName;
    private String supplierPhone;
    private List<PurchaseItem> items = new ArrayList<>();
    private double grossAmount;
    private double discount;
    private double transportCost;
    private double labourCost;
    private double otherCost;
    private double grandTotal;
    private double paidAmount;
    private double dueAmount;
    private String paymentStatus = PAYMENT_STATUS_PAID;
    private String purchaseStatus = PURCHASE_STATUS_CONFIRMED;
    private String paymentMethod = "Cash";
    private String referenceNo;
    private long purchaseDate;
    private String notes;
    private long createdAt;
    private long updatedAt;

    public Purchase() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSupplierPhone() { return supplierPhone; }
    public void setSupplierPhone(String supplierPhone) { this.supplierPhone = supplierPhone; }

    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) { this.items = items; }

    public double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(double grossAmount) { this.grossAmount = grossAmount; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTransportCost() { return transportCost; }
    public void setTransportCost(double transportCost) { this.transportCost = transportCost; }

    public double getLabourCost() { return labourCost; }
    public void setLabourCost(double labourCost) { this.labourCost = labourCost; }

    public double getOtherCost() { return otherCost; }
    public void setOtherCost(double otherCost) { this.otherCost = otherCost; }

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

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPurchaseStatus() { return purchaseStatus; }
    public void setPurchaseStatus(String purchaseStatus) { this.purchaseStatus = purchaseStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public long getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(long purchaseDate) { this.purchaseDate = purchaseDate; }

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
        map.put("supplierId", supplierId);
        map.put("supplierName", supplierName);
        map.put("supplierPhone", supplierPhone);
        
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        if (items != null) {
            for (PurchaseItem item : items) {
                itemMaps.add(item.toMap());
            }
        }
        map.put("items", itemMaps);
        map.put("grossAmount", grossAmount);
        map.put("discount", discount);
        map.put("transportCost", transportCost);
        map.put("labourCost", labourCost);
        map.put("otherCost", otherCost);
        map.put("grandTotal", grandTotal);
        map.put("paidAmount", paidAmount);
        map.put("dueAmount", dueAmount);
        map.put("paymentStatus", paymentStatus);
        map.put("purchaseStatus", purchaseStatus);
        map.put("paymentMethod", paymentMethod);
        map.put("referenceNo", referenceNo);
        map.put("purchaseDate", purchaseDate);
        map.put("notes", notes);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
