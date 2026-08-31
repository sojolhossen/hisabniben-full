package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PurchaseItem implements Serializable {
    public static final String MODE_BAGS = "BAGS";
    public static final String MODE_KG = "KG";

    private String purchaseItemId;
    private String purchaseId;
    private String productId;
    private String productNameSnapshot;
    private String varietySnapshot;
    private String brandSnapshot;
    private String quantityMode = MODE_BAGS; // BAGS or KG
    private double bagQuantity;
    private double bagWeight = 50.0;
    private double totalKg;                  // Canonical inventory source of truth
    private double purchaseRatePerKg;
    private double totalAmount;
    private double directCostAllocation;    // Allocated transport/loading cost for WAC calculation
    private double effectiveCostPerKg;      // (totalAmount + directCostAllocation) / totalKg

    public PurchaseItem() {}

    public PurchaseItem(String productId, String productNameSnapshot, String varietySnapshot, String quantityMode, double bagQuantity, double bagWeight, double totalKg, double purchaseRatePerKg) {
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.varietySnapshot = varietySnapshot;
        this.quantityMode = quantityMode != null ? quantityMode : MODE_BAGS;
        this.bagQuantity = bagQuantity;
        this.bagWeight = bagWeight > 0 ? bagWeight : 50.0;
        this.totalKg = totalKg;
        this.purchaseRatePerKg = purchaseRatePerKg;
        calculateTotals();
    }

    public String getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(String purchaseItemId) { this.purchaseItemId = purchaseItemId; }

    public String getPurchaseId() { return purchaseId; }
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }

    // Alias for backward compatibility
    public String getProductName() { return productNameSnapshot; }
    public void setProductName(String productName) { this.productNameSnapshot = productName; }

    public String getVarietySnapshot() { return varietySnapshot; }
    public void setVarietySnapshot(String varietySnapshot) { this.varietySnapshot = varietySnapshot; }

    public String getVariety() { return varietySnapshot; }
    public void setVariety(String variety) { this.varietySnapshot = variety; }

    public String getBrandSnapshot() { return brandSnapshot; }
    public void setBrandSnapshot(String brandSnapshot) { this.brandSnapshot = brandSnapshot; }

    public String getQuantityMode() { return quantityMode; }
    public void setQuantityMode(String quantityMode) { 
        this.quantityMode = quantityMode;
        calculateTotals();
    }

    public double getBagQuantity() { return bagQuantity; }
    public void setBagQuantity(double bagQuantity) { 
        this.bagQuantity = bagQuantity; 
        calculateTotals();
    }

    public double getBagWeight() { return bagWeight; }
    public void setBagWeight(double bagWeight) { 
        this.bagWeight = bagWeight; 
        calculateTotals();
    }

    public double getTotalKg() { return totalKg; }
    public void setTotalKg(double totalKg) { 
        this.totalKg = totalKg;
        if (MODE_KG.equals(quantityMode)) {
            this.totalAmount = this.totalKg * this.purchaseRatePerKg;
            calculateEffectiveCost();
        }
    }

    public double getPurchaseRatePerKg() { return purchaseRatePerKg; }
    public void setPurchaseRatePerKg(double purchaseRatePerKg) { 
        this.purchaseRatePerKg = purchaseRatePerKg; 
        calculateTotals();
    }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getDirectCostAllocation() { return directCostAllocation; }
    public void setDirectCostAllocation(double directCostAllocation) { 
        this.directCostAllocation = directCostAllocation;
        calculateEffectiveCost();
    }

    public double getEffectiveCostPerKg() { return effectiveCostPerKg > 0 ? effectiveCostPerKg : purchaseRatePerKg; }
    public void setEffectiveCostPerKg(double effectiveCostPerKg) { this.effectiveCostPerKg = effectiveCostPerKg; }

    public void calculateTotals() {
        if (MODE_BAGS.equals(quantityMode)) {
            this.totalKg = this.bagQuantity * (this.bagWeight > 0 ? this.bagWeight : 50.0);
        }
        this.totalAmount = this.totalKg * this.purchaseRatePerKg;
        calculateEffectiveCost();
    }

    private void calculateEffectiveCost() {
        if (this.totalKg > 0) {
            this.effectiveCostPerKg = (this.totalAmount + this.directCostAllocation) / this.totalKg;
        } else {
            this.effectiveCostPerKg = this.purchaseRatePerKg;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("purchaseItemId", purchaseItemId);
        map.put("purchaseId", purchaseId);
        map.put("productId", productId);
        map.put("productNameSnapshot", productNameSnapshot);
        map.put("varietySnapshot", varietySnapshot);
        map.put("brandSnapshot", brandSnapshot);
        map.put("quantityMode", quantityMode);
        map.put("bagQuantity", bagQuantity);
        map.put("bagWeight", bagWeight);
        map.put("totalKg", totalKg);
        map.put("purchaseRatePerKg", purchaseRatePerKg);
        map.put("totalAmount", totalAmount);
        map.put("directCostAllocation", directCostAllocation);
        map.put("effectiveCostPerKg", getEffectiveCostPerKg());
        return map;
    }
}
