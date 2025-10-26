package com.example.islamicquiz.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.Map;

/**
 * نموذج أرشيف العمليات - يسجل جميع العمليات المهمة في النظام
 */
public class OperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    // معرف السجل
    private String id;
    
    // نوع العملية
    private OperationType operationType;
    
    // نوع الكائن المتأثر
    private EntityType entityType;
    
    // معرف الكائن المتأثر
    private String entityId;
    
    // معرف المستخدم الذي قام بالعملية
    private String userId;
    
    // اسم المستخدم
    private String userName;
    
    // تاريخ ووقت العملية
    private Timestamp timestamp;
    
    // وصف العملية
    private String description;
    
    // تفاصيل إضافية (JSON format)
    private Map<String, Object> details;
    
    // البيانات القديمة (للتحديث والحذف)
    private Map<String, Object> oldData;
    
    // البيانات الجديدة (للإنشاء والتحديث)
    private Map<String, Object> newData;
    
    // معلومات إضافية عن الجلسة
    private String deviceInfo;
    private String appVersion;
    
    // أنواع العمليات
    public enum OperationType {
        CREATE("إنشاء"),
        UPDATE("تحديث"),
        DELETE("حذف"),
        VIEW("عرض"),
        PRINT("طباعة"),
        EXPORT("تصدير"),
        LOGIN("تسجيل دخول"),
        LOGOUT("تسجيل خروج"),
        BACKUP("نسخ احتياطي"),
        RESTORE("استعادة");
        
        private final String arabicName;
        
        OperationType(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() {
            return arabicName;
        }
    }
    
    // أنواع الكائنات
    public enum EntityType {
        INVOICE("فاتورة"),
        PRODUCT("منتج"),
        CUSTOMER("عميل"),
        USER("مستخدم"),
        REPORT("تقرير"),
        SETTING("إعداد"),
        DATABASE("قاعدة البيانات");
        
        private final String arabicName;
        
        EntityType(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() {
            return arabicName;
        }
    }

    // Constructors
    public OperationLog() {
        // Required empty constructor for Firestore
    }

    public OperationLog(OperationType operationType, EntityType entityType, String entityId, 
                       String userId, String userName, String description) {
        this.operationType = operationType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userId = userId;
        this.userName = userName;
        this.description = description;
        this.timestamp = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Map<String, Object> getOldData() {
        return oldData;
    }

    public void setOldData(Map<String, Object> oldData) {
        this.oldData = oldData;
    }

    public Map<String, Object> getNewData() {
        return newData;
    }

    public void setNewData(Map<String, Object> newData) {
        this.newData = newData;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * إنشاء وصف مفصل للعملية
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(operationType.getArabicName())
          .append(" ")
          .append(entityType.getArabicName());
        
        if (description != null && !description.isEmpty()) {
            sb.append(": ").append(description);
        }
        
        return sb.toString();
    }

    /**
     * فحص ما إذا كانت العملية حساسة (حذف، تحديث)
     */
    public boolean isCriticalOperation() {
        return operationType == OperationType.DELETE || 
               operationType == OperationType.UPDATE ||
               operationType == OperationType.RESTORE;
    }

    @Override
    public String toString() {
        return "OperationLog{" +
                "operationType=" + operationType +
                ", entityType=" + entityType +
                ", entityId='" + entityId + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
}