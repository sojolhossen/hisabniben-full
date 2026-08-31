package com.sajoldev.hisabniben.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Expense implements Serializable {
    public static final String CAT_TRANSPORT = "পরিবহন";
    public static final String CAT_LABOUR = "শ্রমিক";
    public static final String CAT_LOADING = "লোডিং";
    public static final String CAT_UNLOADING = "আনলোডিং";
    public static final String CAT_WAREHOUSE_RENT = "গুদাম ভাড়া";
    public static final String CAT_SHOP_RENT = "দোকান ভাড়া";
    public static final String CAT_ELECTRICITY = "বিদ্যুৎ";
    public static final String CAT_MILL = "মিল খরচ";
    public static final String CAT_PACKAGING = "প্যাকেজিং";
    public static final String CAT_SALARY = "বেতন";
    public static final String CAT_REPAIR = "মেরামত";
    public static final String CAT_MOBILE_INTERNET = "মোবাইল/ইন্টারনেট";
    public static final String CAT_PAYMENT_CHARGE = "ব্যাংক/পেমেন্ট চার্জ";
    public static final String CAT_OTHER = "অন্যান্য";

    private String id;
    private String userId;
    private String category;
    private double amount;
    private String paymentMethod;
    private long date;
    private String description;
    private String reference;
    private long createdAt;

    public Expense() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("category", category);
        map.put("amount", amount);
        map.put("paymentMethod", paymentMethod);
        map.put("date", date);
        map.put("description", description);
        map.put("reference", reference);
        map.put("createdAt", createdAt);
        return map;
    }
}
