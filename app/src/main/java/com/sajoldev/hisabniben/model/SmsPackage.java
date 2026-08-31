package com.sajoldev.hisabniben.model;

import java.io.Serializable;

public class SmsPackage implements Serializable {
    private String id;
    private String name;
    private int smsCount;
    private double price;
    private String status;
    private boolean popular;

    public SmsPackage() {}

    public SmsPackage(String id, String name, int smsCount, double price, String status) {
        this(id, name, smsCount, price, status, false);
    }

    public SmsPackage(String id, String name, int smsCount, double price, String status, boolean popular) {
        this.id = id;
        this.name = name;
        this.smsCount = smsCount;
        this.price = price;
        this.status = status;
        this.popular = popular;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSmsCount() { return smsCount; }
    public void setSmsCount(int smsCount) { this.smsCount = smsCount; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPopular() { return popular; }
    public void setPopular(boolean popular) { this.popular = popular; }
}