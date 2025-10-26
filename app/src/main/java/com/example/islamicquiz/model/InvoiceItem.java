package com.example.islamicquiz.model;

import java.io.Serializable;

public class InvoiceItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private String productId;
    private String productName;
    private double price;
    private int quantity;
    
    public InvoiceItem() {
        // Empty constructor needed for Firestore
    }
    
    public InvoiceItem(String productId, String productName, double price, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    // حساب المجموع
    public double getTotal() {
        return price * quantity;
    }
} 