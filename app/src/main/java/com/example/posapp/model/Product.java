package com.example.posapp.model;

public class Product {
    private String id;
    private String name;
    private String category;      // فئة المنتج
    private double sellingPrice;  // سعر البيع
    private double costPrice;     // سعر الشراء
    private int quantity;         // الكمية الحالية
    private int minQuantity;      // الحد الأدنى للمخزون
    private String barcode;       // الباركود
    private String imageUrl;      // رابط الصورة
    
    public Product() {
        // Empty constructor needed for Firestore
    }
    
    public Product(String id, String name, String category, double sellingPrice, 
                  double costPrice, int quantity, int minQuantity, String barcode, String imageUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
        this.quantity = quantity;
        this.minQuantity = minQuantity;
        this.barcode = barcode;
        this.imageUrl = imageUrl;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
    
    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    // حساب الربح للوحدة
    public double getUnitProfit() {
        return sellingPrice - costPrice;
    }
    
    // التحقق من حالة المخزون
    public boolean isLowStock() {
        return quantity <= minQuantity;
    }
} 