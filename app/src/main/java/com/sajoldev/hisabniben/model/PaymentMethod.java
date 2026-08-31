package com.sajoldev.hisabniben.model;

public class PaymentMethod {
    private String id;
    private String name;
    private String accountNumber;
    private String accountType;
    private String instructions;
    private String icon;
    private boolean active;

    public PaymentMethod() {}

    public PaymentMethod(String id, String name, String accountNumber, String accountType, String instructions, String icon, boolean active) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.instructions = instructions;
        this.icon = icon;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}