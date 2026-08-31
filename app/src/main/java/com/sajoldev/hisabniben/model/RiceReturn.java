package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RiceReturn implements Serializable {
    public static final String TYPE_CUSTOMER_RETURN = "CUSTOMER_RETURN";
    public static final String TYPE_SUPPLIER_RETURN = "SUPPLIER_RETURN";

    private String id;
    private String userId;
    private String returnType;       // CUSTOMER_RETURN or SUPPLIER_RETURN
    private String partyId;          // customerId or supplierId
    private String partyName;
    private String productId;
    private String productName;
    private double bagQuantity;
    private double bagWeight;
    private double totalKg;
    private double ratePerKg;
    private double totalAmount;
    private String originalInvoiceNo;
    private String reason;
    private long date;
    private long createdAt;

    public RiceReturn() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public String getPartyId() { return partyId; }
    public void setPartyId(String partyId) { this.partyId = partyId; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getBagQuantity() { return bagQuantity; }
    public void setBagQuantity(double bagQuantity) { this.bagQuantity = bagQuantity; }

    public double getBagWeight() { return bagWeight; }
    public void setBagWeight(double bagWeight) { this.bagWeight = bagWeight; }

    public double getTotalKg() { return totalKg; }
    public void setTotalKg(double totalKg) { this.totalKg = totalKg; }

    public double getRatePerKg() { return ratePerKg; }
    public void setRatePerKg(double ratePerKg) { this.ratePerKg = ratePerKg; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getOriginalInvoiceNo() { return originalInvoiceNo; }
    public void setOriginalInvoiceNo(String originalInvoiceNo) { this.originalInvoiceNo = originalInvoiceNo; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("returnType", returnType);
        map.put("partyId", partyId);
        map.put("partyName", partyName);
        map.put("productId", productId);
        map.put("productName", productName);
        map.put("bagQuantity", bagQuantity);
        map.put("bagWeight", bagWeight);
        map.put("totalKg", totalKg);
        map.put("ratePerKg", ratePerKg);
        map.put("totalAmount", totalAmount);
        map.put("originalInvoiceNo", originalInvoiceNo);
        map.put("reason", reason);
        map.put("date", date);
        map.put("createdAt", createdAt);
        return map;
    }
}
