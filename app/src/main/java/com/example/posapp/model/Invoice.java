package com.example.posapp.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Invoice implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String customInvoiceNumber; // رقم الفاتورة المخصص (مثل AM001)
    private String createdByUserId; // معرف المستخدم الذي أنشأ الفاتورة
    private String customerName;
    private String customerPhone;
    private boolean isPaid;
    private double totalAmount;
    private Timestamp date;
    private List<InvoiceItem> items;
    private Map<String, Object> additionalInfo;
    
    // طريقة الدفع الجديدة
    private PaymentMethod paymentMethod;
    
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
        // تحويل من النظام القديم
        this.paymentMethod = PaymentMethod.fromLegacyPaid(isPaid);
    }
    
    // Constructor جديد يدعم طريقة الدفع
    public Invoice(String customerName, String customerPhone, PaymentMethod paymentMethod, 
                   double totalAmount, Timestamp date, List<InvoiceItem> items) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.paymentMethod = paymentMethod;
        this.isPaid = paymentMethod.isLegacyPaid(); // للتوافق مع النظام القديم
        this.totalAmount = totalAmount;
        this.date = date;
        this.items = items;
    }
    
    // Constructor محدث يدعم الرقم المخصص ومعرف المنشئ
    public Invoice(String customerName, String customerPhone, PaymentMethod paymentMethod, 
                   double totalAmount, Timestamp date, List<InvoiceItem> items, 
                   String customInvoiceNumber, String createdByUserId) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.paymentMethod = paymentMethod;
        this.isPaid = paymentMethod.isLegacyPaid(); // للتوافق مع النظام القديم
        this.totalAmount = totalAmount;
        this.date = date;
        this.items = items;
        this.customInvoiceNumber = customInvoiceNumber;
        this.createdByUserId = createdByUserId;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomInvoiceNumber() { return customInvoiceNumber; }
    public void setCustomInvoiceNumber(String customInvoiceNumber) { this.customInvoiceNumber = customInvoiceNumber; }
    
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { 
        this.isPaid = paid;
        // تحديث طريقة الدفع إذا لم تكن محددة
        if (this.paymentMethod == null) {
            this.paymentMethod = PaymentMethod.fromLegacyPaid(paid);
        }
    }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }
    
    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }
    
    public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(Map<String, Object> additionalInfo) { this.additionalInfo = additionalInfo; }
    
    // طريقة الدفع الجديدة
    public PaymentMethod getPaymentMethod() { 
        // إذا لم تكن محددة، استخدم النظام القديم
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.fromLegacyPaid(isPaid);
        }
        return paymentMethod; 
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) { 
        this.paymentMethod = paymentMethod;
        // تحديث isPaid للتوافق مع النظام القديم
        this.isPaid = paymentMethod.isLegacyPaid();
    }
    
    // دالة مساعدة للحصول على طريقة الدفع مع الأيقونة
    public String getPaymentMethodDisplay() {
        return getPaymentMethod().getDisplayWithIcon();
    }
    
    /**
     * الحصول على رقم الفاتورة للعرض (المخصص أو Firestore ID)
     * @return رقم الفاتورة للعرض
     */
    public String getDisplayNumber() {
        if (customInvoiceNumber != null && !customInvoiceNumber.trim().isEmpty()) {
            return customInvoiceNumber;
        }
        return id != null ? id : "غير محدد";
    }
    
    /**
     * فحص ما إذا كانت الفاتورة تستخدم نظام الترقيم الجديد
     * @return true إذا كانت تستخدم الرقم المخصص
     */
    public boolean hasCustomNumber() {
        return customInvoiceNumber != null && !customInvoiceNumber.trim().isEmpty();
    }
} 