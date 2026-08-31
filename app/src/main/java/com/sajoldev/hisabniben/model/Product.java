package com.sajoldev.hisabniben.model;

import java.util.HashMap;
import java.util.Map;

public class Product {
    private String id;
    private String userId;
    private String name;
    private String type;
    private double quantity;
    private double price;
    private double minStock;
    private long createdAt;

    public Product() {}

    public Product(String id, String userId, String name, String type, double quantity, double price, double minStock, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.minStock = minStock;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getMinStock() { return minStock; }
    public void setMinStock(double minStock) { this.minStock = minStock; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("name", name);
        map.put("type", type);
        map.put("quantity", quantity);
        map.put("price", price);
        map.put("minStock", minStock);
        map.put("createdAt", createdAt);
        return map;
    }
}
