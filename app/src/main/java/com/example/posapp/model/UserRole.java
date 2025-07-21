package com.example.posapp.model;

/**
 * أدوار المستخدمين في النظام
 */
public enum UserRole {
    /**
     * مدير النظام - له صلاحيات كاملة على جميع العمليات
     */
    ADMIN("مدير النظام", 4),
    
    /**
     * مدير - له صلاحيات واسعة مع بعض القيود
     */
    MANAGER("مدير", 3),
    
    /**
     * موظف - له صلاحيات محدودة للعمليات اليومية
     */
    EMPLOYEE("موظف", 2),
    
    /**
     * عارض - له صلاحيات عرض فقط
     */
    VIEWER("عارض", 1),
    
    /**
     * مستخدم عادي - نفس صلاحيات الموظف (للتوافق مع البيانات القديمة)
     */
    USER("مستخدم", 2);
    
    private final String displayName;
    private final int level;
    
    UserRole(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * فحص ما إذا كان هذا الدور له صلاحية أعلى من دور آخر
     */
    public boolean hasHigherAuthorityThan(UserRole other) {
        return this.level > other.level;
    }
    
    /**
     * فحص ما إذا كان هذا الدور له صلاحية أعلى أو مساوية لدور آخر
     */
    public boolean hasAuthorityOf(UserRole requiredRole) {
        return this.level >= requiredRole.level;
    }
    
    /**
     * تحويل القيم القديمة إلى أدوار جديدة
     */
    public static UserRole fromLegacyRole(String legacyRole) {
        if (legacyRole == null) return EMPLOYEE;
        
        switch (legacyRole.toLowerCase()) {
            case "admin":
                return ADMIN;
            case "manager":
                return MANAGER;
            case "user":
            case "employee":
                return EMPLOYEE;
            case "viewer":
                return VIEWER;
            default:
                return EMPLOYEE; // القيمة الافتراضية
        }
    }
} 