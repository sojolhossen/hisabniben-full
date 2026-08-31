package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StockMovement implements Serializable {
    public static final String TYPE_PURCHASE = "PURCHASE";
    public static final String TYPE_SALE = "SALE";
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";
    public static final String TYPE_RETURN_CUSTOMER = "RETURN_CUSTOMER";
    public static final String TYPE_RETURN_SUPPLIER = "RETURN_SUPPLIER";
    public static final String TYPE_DAMAGE = "DAMAGE";

    private String id;
    private String userId;
    private String productId;
    private String productName;
    private String type;             // PURCHASE, SALE, ADJUSTMENT, etc.
    private double quantityKg;       // + or - in KG
    private double quantityBags;     // + or - in Bags
    private double previousStockKg;  // Stock KG before movement
    private double newStockKg;       // Stock KG after movement
    private String referenceId;      // e.g. saleId, purchaseId, returnId
    private String reason;
    private long date;
    private long createdAt;

    public StockMovement() {}

    public StockMovement(String productId, String productName, String type, double quantityKg, double newStockKg, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantityKg = quantityKg;
        this.newStockKg = newStockKg;
        this.reason = reason;
        this.date = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    public StockMovement(String productId, String productName, String variety, String type, double quantityKg, double newStockKg, String reason) {
        this.productId = productId;
        this.productName = productName != null ? (productName + (variety != null ? " (" + variety + ")" : "")) : "";
        this.type = type;
        this.quantityKg = quantityKg;
        this.newStockKg = newStockKg;
        this.reason = reason;
        this.date = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

    public double getQuantityBags() { return quantityBags; }
    public void setQuantityBags(double quantityBags) { this.quantityBags = quantityBags; }

    public double getPreviousStockKg() { return previousStockKg; }
    public void setPreviousStockKg(double previousStockKg) { this.previousStockKg = previousStockKg; }

    public double getNewStockKg() { return newStockKg; }
    public void setNewStockKg(double newStockKg) { this.newStockKg = newStockKg; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

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
        map.put("productId", productId);
        map.put("productName", productName);
        map.put("type", type);
        map.put("quantityKg", quantityKg);
        map.put("quantityBags", quantityBags);
        map.put("previousStockKg", previousStockKg);
        map.put("newStockKg", newStockKg);
        map.put("referenceId", referenceId);
        map.put("reason", reason);
        map.put("date", date);
        map.put("createdAt", createdAt);
        return map;
    }
}
