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
            future.complete(new ArrayList<>());
            return future;
        }
        
        db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", currentUser.getId())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<OperationLog> logs = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    OperationLog log = document.toObject(OperationLog.class);
                    logs.add(log);
                }
                future.complete(logs);
            })
            .addOnFailureListener(future::completeExceptionally);
            
        return future;
    }
    
    /**
     * الحصول على سجل العمليات لكائن معين
     */
    public CompletableFuture<List<OperationLog>> getEntityOperationLogs(OperationLog.EntityType entityType, 
                                                                       String entityId) {
        CompletableFuture<List<OperationLog>> future = new CompletableFuture<>();
        
        db.collection(COLLECTION_NAME)
            .whereEqualTo("entityType", entityType.name())
            .whereEqualTo("entityId", entityId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<OperationLog> logs = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    OperationLog log = document.toObject(OperationLog.class);
                    logs.add(log);
                }
                future.complete(logs);
            })
            .addOnFailureListener(future::completeExceptionally);
            
        return future;
    }
    
    /**
     * الحصول على جميع العمليات الحساسة (للمديرين)
     */
    public CompletableFuture<List<OperationLog>> getCriticalOperations(int limit) {
        CompletableFuture<List<OperationLog>> future = new CompletableFuture<>();
        
        db.collection(COLLECTION_NAME)
            .whereIn("operationType", 
                java.util.Arrays.asList(
                    OperationLog.OperationType.DELETE.name(),
                    OperationLog.OperationType.UPDATE.name(),
                    OperationLog.OperationType.RESTORE.name()
                )
            )
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<OperationLog> logs = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    OperationLog log = document.toObject(OperationLog.class);
                    logs.add(log);
                }
                future.complete(logs);
            })
            .addOnFailureListener(future::completeExceptionally);
            
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