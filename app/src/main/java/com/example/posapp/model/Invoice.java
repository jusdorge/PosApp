package com.example.posapp.model;

import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Map;

public class Invoice {
    private String id;
    private String customerName;
    private String customerPhone;
    private boolean isPaid;
    private double totalAmount;
    private Timestamp date;
    private List<InvoiceItem> items;
    private Map<String, Object> additionalInfo;
    
    // Empty constructor needed for Firestore
    public Invoice() {
    }
    
    public Invoice(String customerName, String customerPhone, boolean isPaid, 
                   double totalAmount, Timestamp date, List<InvoiceItem> items) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.isPaid = isPaid;
        this.totalAmount = totalAmount;
        this.date = date;
        this.items = items;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }
    
    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
    
    public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(Map<String, Object> additionalInfo) { this.additionalInfo = additionalInfo; }
} 