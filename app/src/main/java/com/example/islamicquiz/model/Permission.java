package com.example.islamicquiz.model;

/**
 * أنواع الصلاحيات في النظام
 */
public enum Permission {
    /**
     * صلاحية القراءة والعرض
     */
    READ("قراءة", "عرض البيانات"),
    
    /**
     * صلاحية الإنشاء والإضافة
     */
    CREATE("إنشاء", "إضافة بيانات جديدة"),
    
    /**
     * صلاحية التعديل والتحديث
     */
    UPDATE("تعديل", "تحديث البيانات الموجودة"),
    
    /**
     * صلاحية الحذف
     */
    DELETE("حذف", "حذف البيانات"),
    
    /**
     * صلاحية التصدير
     */
    EXPORT("تصدير", "تصدير البيانات"),
    
    /**
     * صلاحية الاستيراد
     */
    IMPORT("استيراد", "استيراد البيانات"),
    
    /**
     * صلاحية إدارة المستخدمين
     */
    MANAGE_USERS("إدارة المستخدمين", "إضافة وتعديل حسابات المستخدمين"),
    
    /**
     * صلاحية عرض التقارير المتقدمة
     */
    ADVANCED_REPORTS("التقارير المتقدمة", "عرض التقارير التفصيلية والإحصائيات");
    
    private final String displayName;
    private final String description;
    
    Permission(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
} 