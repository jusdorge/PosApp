package com.example.posapp.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.example.posapp.model.OperationLog;
import com.example.posapp.model.User;
import com.example.posapp.UserSession;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * خدمة أرشيف العمليات - تسجيل ومتابعة جميع العمليات في النظام
 */
public class OperationLogService {
    private static final String TAG = "OperationLogService";
    private static final String COLLECTION_NAME = "operation_logs";
    
    private static OperationLogService instance;
    private final FirebaseFirestore db;
    private final Context context;
    
    private OperationLogService(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
    }
    
    public static synchronized OperationLogService getInstance(Context context) {
        if (instance == null) {
            instance = new OperationLogService(context);
        }
        return instance;
    }
    
    /**
     * تسجيل عملية جديدة
     */
    public CompletableFuture<Void> logOperation(OperationLog.OperationType operationType,
                                               OperationLog.EntityType entityType,
                                               String entityId,
                                               String description) {
        return logOperation(operationType, entityType, entityId, description, null, null, null);
    }
    
    /**
     * تسجيل عملية مع تفاصيل إضافية
     */
    public CompletableFuture<Void> logOperation(OperationLog.OperationType operationType,
                                               OperationLog.EntityType entityType,
                                               String entityId,
                                               String description,
                                               Map<String, Object> details,
                                               Map<String, Object> oldData,
                                               Map<String, Object> newData) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            // الحصول على معلومات المستخدم الحالي
            UserSession userSession = UserSession.getInstance(context);
            User currentUser = userSession.getCurrentUser();
            
            if (currentUser == null) {
                Log.w(TAG, "No current user found for operation logging");
                future.complete(null);
                return future;
            }
            
            // إنشاء سجل العملية
            OperationLog operationLog = new OperationLog(
                operationType, 
                entityType, 
                entityId, 
                currentUser.getId(), 
                currentUser.getFullName(), 
                description
            );
            
            // إضافة التفاصيل الإضافية
            operationLog.setDetails(details);
            operationLog.setOldData(oldData);
            operationLog.setNewData(newData);
            operationLog.setDeviceInfo(getDeviceInfo());
            operationLog.setAppVersion(getAppVersion());
            
            // حفظ في قاعدة البيانات
            DocumentReference docRef = db.collection(COLLECTION_NAME).document();
            operationLog.setId(docRef.getId());
            
            docRef.set(operationLog)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Operation logged successfully: " + operationLog.getDetailedDescription());
                    future.complete(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to log operation", e);
                    future.completeExceptionally(e);
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error creating operation log", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * تسجيل عملية إنشاء
     */
    public CompletableFuture<Void> logCreate(OperationLog.EntityType entityType, 
                                           String entityId, 
                                           String description, 
                                           Map<String, Object> newData) {
        return logOperation(OperationLog.OperationType.CREATE, entityType, entityId, description, null, null, newData);
    }
    
    /**
     * تسجيل عملية تحديث
     */
    public CompletableFuture<Void> logUpdate(OperationLog.EntityType entityType, 
                                           String entityId, 
                                           String description, 
                                           Map<String, Object> oldData, 
                                           Map<String, Object> newData) {
        return logOperation(OperationLog.OperationType.UPDATE, entityType, entityId, description, null, oldData, newData);
    }
    
    /**
     * تسجيل عملية حذف
     */
    public CompletableFuture<Void> logDelete(OperationLog.EntityType entityType, 
                                           String entityId, 
                                           String description, 
                                           Map<String, Object> deletedData) {
        return logOperation(OperationLog.OperationType.DELETE, entityType, entityId, description, null, deletedData, null);
    }
    
    /**
     * تسجيل عملية عرض/قراءة
     */
    public CompletableFuture<Void> logView(OperationLog.EntityType entityType, 
                                         String entityId, 
                                         String description) {
        return logOperation(OperationLog.OperationType.VIEW, entityType, entityId, description);
    }
    
    /**
     * تسجيل عملية طباعة
     */
    public CompletableFuture<Void> logPrint(OperationLog.EntityType entityType, 
                                          String entityId, 
                                          String description) {
        return logOperation(OperationLog.OperationType.PRINT, entityType, entityId, description);
    }
    
    /**
     * الحصول على سجل العمليات للمستخدم الحالي
     */
    public CompletableFuture<List<OperationLog>> getUserOperationLogs(int limit) {
        CompletableFuture<List<OperationLog>> future = new CompletableFuture<>();
        
        UserSession userSession = UserSession.getInstance(context);
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser == null) {
            Log.w(TAG, "لا يوجد مستخدم مسجل دخول - إرجاع قائمة فارغة");
            future.complete(new ArrayList<>());
            return future;
        }
        
        Log.d(TAG, "جارٍ تحميل سجل العمليات للمستخدم: " + currentUser.getFullName());
        
        // استخدام طريقة بديلة لتجنب الحاجة لـ composite index
        // نحصل على العمليات للمستخدم أولاً، ثم نرتبها محلياً
        db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", currentUser.getId())
            .get(Source.DEFAULT) // محاولة من الخادم أولاً ثم من الكاش
            .addOnSuccessListener(queryDocumentSnapshots -> {
                try {
                    List<OperationLog> logs = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            OperationLog log = document.toObject(OperationLog.class);
                            if (log != null) {
                                log.setId(document.getId());
                                logs.add(log);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "خطأ في تحويل وثيقة العملية: " + document.getId(), e);
                        }
                    }
                    
                    // ترتيب العمليات حسب التاريخ (الأحدث أولاً)
                    logs.sort((log1, log2) -> {
                        if (log1.getTimestamp() == null) return 1;
                        if (log2.getTimestamp() == null) return -1;
                        return log2.getTimestamp().compareTo(log1.getTimestamp());
                    });
                    
                    // تطبيق الحد الأقصى
                    List<OperationLog> limitedLogs = logs.size() > limit ? 
                        logs.subList(0, limit) : logs;
                    
                    Log.d(TAG, "تم تحميل " + limitedLogs.size() + " عملية بنجاح");
                    future.complete(limitedLogs);
                    
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في معالجة سجل العمليات", e);
                    future.completeExceptionally(e);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "فشل في تحميل سجل العمليات من Firestore", e);
                
                // محاولة من الكاش المحلي في حالة فشل الاتصال
                tryLoadFromCache(currentUser.getId(), limit, future);
            });
            
        return future;
    }
    
    /**
     * محاولة تحميل العمليات من الكاش المحلي
     */
    private void tryLoadFromCache(String userId, int limit, CompletableFuture<List<OperationLog>> future) {
        Log.d(TAG, "محاولة تحميل العمليات من الكاش المحلي...");
        
        db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .get(Source.CACHE)
            .addOnSuccessListener(queryDocumentSnapshots -> {
                try {
                    List<OperationLog> logs = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            OperationLog log = document.toObject(OperationLog.class);
                            if (log != null) {
                                log.setId(document.getId());
                                logs.add(log);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "خطأ في تحويل وثيقة من الكاش: " + document.getId(), e);
                        }
                    }
                    
                    // ترتيب وتحديد العدد
                    logs.sort((log1, log2) -> {
                        if (log1.getTimestamp() == null) return 1;
                        if (log2.getTimestamp() == null) return -1;
                        return log2.getTimestamp().compareTo(log1.getTimestamp());
                    });
                    
                    List<OperationLog> limitedLogs = logs.size() > limit ? 
                        logs.subList(0, limit) : logs;
                    
                    Log.i(TAG, "تم تحميل " + limitedLogs.size() + " عملية من الكاش المحلي");
                    future.complete(limitedLogs);
                    
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في معالجة الكاش", e);
                    future.completeExceptionally(new Exception("فشل في تحميل البيانات من الخادم والكاش المحلي", e));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "فشل في تحميل العمليات من الكاش أيضاً", e);
                future.completeExceptionally(new Exception("لا يمكن الوصول لسجل العمليات - تحقق من الاتصال بالإنترنت", e));
            });
    }
    
    /**
     * الحصول على سجل العمليات لكائن معين
     */
    public CompletableFuture<List<OperationLog>> getEntityOperationLogs(OperationLog.EntityType entityType, 
                                                                       String entityId) {
        CompletableFuture<List<OperationLog>> future = new CompletableFuture<>();
        
        Log.d(TAG, "جارٍ تحميل سجل العمليات للكائن: " + entityType.name() + "/" + entityId);
        
        // استخدام فلترة بسيطة ثم ترتيب محلي لتجنب الحاجة لـ composite index
        db.collection(COLLECTION_NAME)
            .whereEqualTo("entityType", entityType.name())
            .whereEqualTo("entityId", entityId)
            .get(Source.DEFAULT)
            .addOnSuccessListener(queryDocumentSnapshots -> {
                try {
                    List<OperationLog> logs = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            OperationLog log = document.toObject(OperationLog.class);
                            if (log != null) {
                                log.setId(document.getId());
                                logs.add(log);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "خطأ في تحويل وثيقة العملية: " + document.getId(), e);
                        }
                    }
                    
                    // ترتيب العمليات حسب التاريخ (الأحدث أولاً)
                    logs.sort((log1, log2) -> {
                        if (log1.getTimestamp() == null) return 1;
                        if (log2.getTimestamp() == null) return -1;
                        return log2.getTimestamp().compareTo(log1.getTimestamp());
                    });
                    
                    Log.d(TAG, "تم تحميل " + logs.size() + " عملية للكائن " + entityType.name());
                    future.complete(logs);
                    
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في معالجة سجل العمليات للكائن", e);
                    future.completeExceptionally(e);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "فشل في تحميل سجل العمليات للكائن: " + entityType.name() + "/" + entityId, e);
                future.completeExceptionally(e);
            });
            
        return future;
    }
    
    /**
     * الحصول على جميع العمليات الحساسة (للمديرين)
     */
    public CompletableFuture<List<OperationLog>> getCriticalOperations(int limit) {
        CompletableFuture<List<OperationLog>> future = new CompletableFuture<>();
        
        Log.d(TAG, "جارٍ تحميل العمليات الحساسة (الحد: " + limit + ")");
        
        // قائمة أنواع العمليات الحساسة
        java.util.Set<String> criticalTypes = new java.util.HashSet<>(java.util.Arrays.asList(
            OperationLog.OperationType.DELETE.name(),
            OperationLog.OperationType.UPDATE.name(),
            OperationLog.OperationType.RESTORE.name()
        ));
        
        // استخدام فلترة بسيطة ثم ترتيب محلي لتجنب الحاجة لـ composite index
        db.collection(COLLECTION_NAME)
            .whereIn("operationType", new ArrayList<>(criticalTypes))
            .get(Source.DEFAULT)
            .addOnSuccessListener(queryDocumentSnapshots -> {
                try {
                    List<OperationLog> logs = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            OperationLog log = document.toObject(OperationLog.class);
                            if (log != null && log.getOperationType() != null && 
                                criticalTypes.contains(log.getOperationType().name())) {
                                log.setId(document.getId());
                                logs.add(log);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "خطأ في تحويل وثيقة العملية الحساسة: " + document.getId(), e);
                        }
                    }
                    
                    // ترتيب العمليات حسب التاريخ (الأحدث أولاً)
                    logs.sort((log1, log2) -> {
                        if (log1.getTimestamp() == null) return 1;
                        if (log2.getTimestamp() == null) return -1;
                        return log2.getTimestamp().compareTo(log1.getTimestamp());
                    });
                    
                    // تطبيق الحد الأقصى
                    List<OperationLog> limitedLogs = logs.size() > limit ? 
                        logs.subList(0, limit) : logs;
                    
                    Log.d(TAG, "تم تحميل " + limitedLogs.size() + " عملية حساسة بنجاح");
                    future.complete(limitedLogs);
                    
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في معالجة العمليات الحساسة", e);
                    future.completeExceptionally(e);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "فشل في تحميل العمليات الحساسة", e);
                future.completeExceptionally(e);
            });
            
        return future;
    }
    
    /**
     * تنظيف السجلات القديمة (أكثر من 90 يوم)
     */
    public CompletableFuture<Integer> cleanOldLogs() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // حساب التاريخ قبل 90 يوم
        long ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
        Timestamp cutoffDate = new Timestamp(ninetyDaysAgo / 1000, 0);
        
        db.collection(COLLECTION_NAME)
            .whereLessThan("timestamp", cutoffDate)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int deletedCount = 0;
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    document.getReference().delete();
                    deletedCount++;
                }
                
                Log.d(TAG, "Cleaned " + deletedCount + " old operation logs");
                future.complete(deletedCount);
            })
            .addOnFailureListener(future::completeExceptionally);
            
        return future;
    }
    
    /**
     * الحصول على معلومات الجهاز
     */
    private String getDeviceInfo() {
        return "Android " + Build.VERSION.RELEASE + 
               " (" + Build.MODEL + ", " + Build.MANUFACTURER + ")";
    }
    
    /**
     * الحصول على إصدار التطبيق
     */
    private String getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName + " (" + packageInfo.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }
    
    /**
     * تحويل كائن إلى Map للحفظ في السجل
     */
    public static Map<String, Object> objectToMap(Object obj) {
        Map<String, Object> map = new HashMap<>();
        
        if (obj == null) {
            return map;
        }
        
        // يمكن تحسين هذه الدالة لاحقاً لاستخدام Reflection
        // للآن سنكتفي بالبيانات الأساسية
        map.put("className", obj.getClass().getSimpleName());
        map.put("toString", obj.toString());
        
        return map;
    }
}