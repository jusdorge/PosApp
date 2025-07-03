package com.example.posapp.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Customer implements Serializable {
    private String id;
    private String name;
    private String phone;
    private double totalDebt;
    private List<CustomerDebt> debts;
    private double latitude;
    private double longitude;
    
    // Empty constructor needed for Firestore
    public Customer() {
        this.debts = new ArrayList<>();
    }
    
    public Customer(String name, String phone) {
        this.name = name;
        this.phone = phone;
        this.totalDebt = 0;
        this.debts = new ArrayList<>();
    }
    
    public void addDebt(String invoiceId, double amount, Timestamp date) {
        CustomerDebt debt = new CustomerDebt(amount, invoiceId, date, false);
        this.debts.add(debt);
        this.totalDebt += amount;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public double getTotalDebt() { return totalDebt; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }
    
    public List<CustomerDebt> getDebts() { return debts; }
    public void setDebts(List<CustomerDebt> debts) { this.debts = debts; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
} 