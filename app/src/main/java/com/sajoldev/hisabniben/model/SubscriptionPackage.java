package com.sajoldev.hisabniben.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriptionPackage {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";

    private String id;
    private String name;
    private double price;
    private int durationDays;
    private String description;
    private String status;
    private String playStoreProductId;
    private List<String> features;
    private long createdAt;
    private long updatedAt;

    public SubscriptionPackage() {
        this.features = new ArrayList<>();
    }

    public SubscriptionPackage(String id, String name, double price, int durationDays, String description, String status, String playStoreProductId, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.durationDays = durationDays;
        this.description = description;
        this.status = status;
        this.playStoreProductId = playStoreProductId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPlayStoreProductId() {
        return playStoreProductId;
    }

    public void setPlayStoreProductId(String playStoreProductId) {
        this.playStoreProductId = playStoreProductId;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("price", price);
        map.put("durationDays", durationDays);
        map.put("description", description);
        map.put("status", status);
        map.put("playStoreProductId", playStoreProductId);
        map.put("features", features);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}
