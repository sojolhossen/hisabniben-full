package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RiceProduct implements Serializable {
    private String id;
    private String userId;
    private String name;              // e.g. Miniket Premium 50KG
    private String variety;           // Miniket, Nazirshail, Katari, BRRI, Swarna, Jirashail, Other
    private String brand;             // Mill/Brand name e.g. Rashid, Erfan, Akij
    private String quality;           // Premium, Grade-A, Standard
    private String packagingType;     // Plastic Bag, Jute Bag, Loose
    private double defaultBagWeight;  // e.g. 50.0 KG, 25.0 KG, 75.0 KG
    private String defaultUnit;       // KG or Bag
    private double purchaseRatePerKg; // Purchase price per KG
    private double saleRatePerKg;     // Sale price per KG
    private double currentStockKg;    // Total stock in KG
    private double currentStockBags;  // Total stock in Bags
    private double minStockAlertKg;   // Threshold for low stock warning
    private String supplierId;
    private String supplierName;
    private String notes;
    private boolean active = true;
    private long createdAt;
    private long updatedAt;

    public RiceProduct() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public String getGrade() { return quality; }
    public void setGrade(String grade) { this.quality = grade; }

    public String getPackagingType() { return packagingType; }
    public void setPackagingType(String packagingType) { this.packagingType = packagingType; }

    public double getDefaultBagWeight() { return defaultBagWeight > 0 ? defaultBagWeight : 50.0; }
    public void setDefaultBagWeight(double defaultBagWeight) { this.defaultBagWeight = defaultBagWeight; }

    public String getDefaultUnit() { return defaultUnit != null ? defaultUnit : "KG"; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; }

    public double getPurchaseRatePerKg() { return purchaseRatePerKg; }
    public void setPurchaseRatePerKg(double purchaseRatePerKg) { this.purchaseRatePerKg = purchaseRatePerKg; }

    public double getSaleRatePerKg() { return saleRatePerKg; }
    public double getSellingRatePerKg() { return saleRatePerKg; }
    public void setSaleRatePerKg(double saleRatePerKg) { this.saleRatePerKg = saleRatePerKg; }

    public double getCurrentStockKg() { return currentStockKg; }
    public void setCurrentStockKg(double currentStockKg) { 
        this.currentStockKg = currentStockKg;
        double weight = getDefaultBagWeight();
        if (weight > 0) {
            this.currentStockBags = currentStockKg / weight;
        }
    }

    public double getCurrentStockBags() { return currentStockBags; }
    public void setCurrentStockBags(double currentStockBags) { 
        this.currentStockBags = currentStockBags;
        double weight = getDefaultBagWeight();
        if (weight > 0) {
            this.currentStockKg = currentStockBags * weight;
        }
    }

    public double getMinStockAlertKg() { return minStockAlertKg; }
    public void setMinStockAlertKg(double minStockAlertKg) { this.minStockAlertKg = minStockAlertKg; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("name", name);
        map.put("variety", variety);
        map.put("brand", brand);
        map.put("quality", quality);
        map.put("packagingType", packagingType);
        map.put("defaultBagWeight", defaultBagWeight);
        map.put("defaultUnit", defaultUnit);
        map.put("purchaseRatePerKg", purchaseRatePerKg);
        map.put("saleRatePerKg", saleRatePerKg);
        map.put("currentStockKg", currentStockKg);
        map.put("currentStockBags", currentStockBags);
        map.put("minStockAlertKg", minStockAlertKg);
        map.put("supplierId", supplierId);
        map.put("supplierName", supplierName);
        map.put("notes", notes);
        map.put("active", active);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
