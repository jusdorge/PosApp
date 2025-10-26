package com.example.islamicquiz.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;

/**
 * نموذج يمثل زيارة بائع لعميل عند مسح رمز العميل
 */
public class CustomerVisit implements Serializable {
    private String id;
    private String sellerId;
    private String sellerName;
    private String customerId;
    private String customerName;
    private Timestamp timestamp;
    private String source; // e.g. "scan"

    // Empty constructor for Firestore
    public CustomerVisit() {}

    public CustomerVisit(String sellerId,
                          String sellerName,
                          String customerId,
                          String customerName,
                          Timestamp timestamp,
                          String source) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.customerId = customerId;
        this.customerName = customerName;
        this.timestamp = timestamp;
        this.source = source;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

