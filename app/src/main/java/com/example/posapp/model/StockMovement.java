package com.example.posapp.model;

import com.google.firebase.Timestamp;

public class StockMovement {
    private String id;
    private String productId;
    private String productName;
    private int quantity;
    private String movementType; // "IN" للدخول, "OUT" للخروج, "ADJUSTMENT" للتعديل
    private String reason; // سبب الحركة (بيع، شراء، تعديل، إرجاع، تلف)
    private String referenceId; // معرف المرجع (معرف الفاتورة مثلاً)
    private Timestamp timestamp;
    private String userId; // معرف المستخدم الذي قام بالحركة
    private String notes; // ملاحظات
    private int quantityBefore; // الكمية قبل الحركة
    private int quantityAfter; // الكمية بعد الحركة
    
    // Empty constructor needed for Firestore
    public StockMovement() {
    }
    
    public StockMovement(String productId, String productName, int quantity, String movementType, 
                        String reason, String referenceId, String userId, String notes,
                        int quantityBefore, int quantityAfter) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.movementType = movementType;
        this.reason = reason;
        this.referenceId = referenceId;
        this.userId = userId;
        this.notes = notes;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.timestamp = Timestamp.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public int getQuantityBefore() { return quantityBefore; }
    public void setQuantityBefore(int quantityBefore) { this.quantityBefore = quantityBefore; }
    
    public int getQuantityAfter() { return quantityAfter; }
    public void setQuantityAfter(int quantityAfter) { this.quantityAfter = quantityAfter; }
    
    // Helper methods
    public boolean isIncoming() {
        return "IN".equals(movementType);
    }
    
    public boolean isOutgoing() {
        return "OUT".equals(movementType);
    }
    
    public boolean isAdjustment() {
        return "ADJUSTMENT".equals(movementType);
    }
    
    // Constants for movement types
    public static final String MOVEMENT_IN = "IN";
    public static final String MOVEMENT_OUT = "OUT";
    public static final String MOVEMENT_ADJUSTMENT = "ADJUSTMENT";
    
    // Constants for reasons
    public static final String REASON_SALE = "بيع";
    public static final String REASON_PURCHASE = "شراء";
    public static final String REASON_ADJUSTMENT = "تعديل";
    public static final String REASON_RETURN = "إرجاع";
    public static final String REASON_DAMAGE = "تلف";
    public static final String REASON_INITIAL_STOCK = "مخزون أولي";
} 