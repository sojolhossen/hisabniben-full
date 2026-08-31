package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SaleItem implements Serializable {
    public static final String MODE_BAGS = "BAGS";
    public static final String MODE_KG = "KG";

    private String saleItemId;
    private String saleId;
    private String productId;
    private String productNameSnapshot;
    private String varietySnapshot;
    private String brandSnapshot;
    private String quantityMode = MODE_BAGS; // BAGS or KG
    private double bagQuantity;
    private double bagWeight = 50.0;
    private double totalKg;                  // Canonical inventory source of truth
    private double saleRatePerKg;
    private double itemTotal;
    private double costPerKg;                // Weighted Average Cost per KG
    private double estimatedProfit;

    public SaleItem() {}

    public SaleItem(String productId, String productNameSnapshot, String varietySnapshot, String quantityMode, double bagQuantity, double bagWeight, double totalKg, double saleRatePerKg, double costPerKg) {
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.varietySnapshot = varietySnapshot;
        this.quantityMode = quantityMode != null ? quantityMode : MODE_BAGS;
        this.bagQuantity = bagQuantity;
        this.bagWeight = bagWeight > 0 ? bagWeight : 50.0;
        this.totalKg = totalKg;
        this.saleRatePerKg = saleRatePerKg;
        this.costPerKg = costPerKg;
        calculateTotals();
    }

    public String getSaleItemId() { return saleItemId; }
    public void setSaleItemId(String saleItemId) { this.saleItemId = saleItemId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }

    public String getVarietySnapshot() { return varietySnapshot; }
    public void setVarietySnapshot(String varietySnapshot) { this.varietySnapshot = varietySnapshot; }

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
            this.itemTotal = this.totalKg * this.saleRatePerKg;
            this.estimatedProfit = (this.saleRatePerKg - this.costPerKg) * this.totalKg;
        }
    }

    public double getSaleRatePerKg() { return saleRatePerKg; }
    public void setSaleRatePerKg(double saleRatePerKg) { 
        this.saleRatePerKg = saleRatePerKg;
        calculateTotals();
    }

    public double getItemTotal() { return itemTotal; }
    public void setItemTotal(double itemTotal) { this.itemTotal = itemTotal; }

    public double getCostPerKg() { return costPerKg; }
    public void setCostPerKg(double costPerKg) { 
        this.costPerKg = costPerKg;
        calculateTotals();
    }

    public double getEstimatedProfit() { return estimatedProfit; }
    public void setEstimatedProfit(double estimatedProfit) { this.estimatedProfit = estimatedProfit; }

    public void calculateTotals() {
        if (MODE_BAGS.equals(quantityMode)) {
            this.totalKg = this.bagQuantity * (this.bagWeight > 0 ? this.bagWeight : 50.0);
        }
        this.itemTotal = this.totalKg * this.saleRatePerKg;
        this.estimatedProfit = (this.saleRatePerKg - this.costPerKg) * this.totalKg;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("saleItemId", saleItemId);
        map.put("saleId", saleId);
        map.put("productId", productId);
        map.put("productNameSnapshot", productNameSnapshot);
        map.put("varietySnapshot", varietySnapshot);
        map.put("brandSnapshot", brandSnapshot);
        map.put("quantityMode", quantityMode);
        map.put("bagQuantity", bagQuantity);
        map.put("bagWeight", bagWeight);
        map.put("totalKg", totalKg);
        map.put("saleRatePerKg", saleRatePerKg);
        map.put("itemTotal", itemTotal);
        map.put("costPerKg", costPerKg);
        map.put("estimatedProfit", estimatedProfit);
        return map;
    }
}
