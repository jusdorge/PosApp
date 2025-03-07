package com.example.posapp.model;

import com.google.firebase.Timestamp;

public class CustomerDebt {
    private String id;
    private double amount;
    private String description;
    private Timestamp date;
    private boolean isPayment; // true للمدفوعات، false للديون

    // Constructor without parameters (required for Firestore)
    public CustomerDebt() {
    }

    // Constructor with parameters
    public CustomerDebt(double amount, String description, Timestamp date, boolean isPayment) {
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.isPayment = isPayment;
    }

    public CustomerDebt(String id, double totalAmount, Timestamp now) {
        this.id = id;
        this.amount = totalAmount;
        this.date = now;
        this.isPayment = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public boolean isPayment() {
        return isPayment;
    }

    public void setPayment(boolean payment) {
        isPayment = payment;
    }
} 