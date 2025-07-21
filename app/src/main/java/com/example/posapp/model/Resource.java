package com.example.posapp.model;

/**
 * الموارد المختلفة في النظام التي يمكن التحكم في الصلاحيات عليها
 */
public enum Resource {
    /**
     * العملاء
     */
    CUSTOMERS("العملاء", "customers"),
    
    /**
     * المنتجات
     */
    PRODUCTS("المنتجات", "products"),
    
    /**
     * الفواتير
     */
    INVOICES("الفواتير", "invoices"),
    
    /**
     * التقارير
     */
    REPORTS("التقارير", "reports"),
    
    /**
     * إدارة المخزون
     */
    INVENTORY("إدارة المخزون", "inventory"),
    
    /**
     * الإعدادات
     */
    SETTINGS("الإعدادات", "settings"),
    
    /**
     * المستخدمين
     */
    USERS("المستخدمين", "users"),
    
    /**
     * النسخ الاحتياطي والاستيراد/التصدير
     */
    BACKUP("النسخ الاحتياطي", "backup");
    
    private final String displayName;
    private final String resourceKey;
    
    Resource(String displayName, String resourceKey) {
        this.displayName = displayName;
        this.resourceKey = resourceKey;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getResourceKey() {
        return resourceKey;
    }
} 