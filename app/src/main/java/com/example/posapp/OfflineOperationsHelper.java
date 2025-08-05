package com.example.posapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

/**
 * مساعد العمليات بدون اتصال - يتعامل مع العمليات المحلية ويوفر ملاحظات للمستخدم
 */
public class OfflineOperationsHelper {
    private static final String TAG = "OfflineOperationsHelper";
    private static OfflineOperationsHelper instance;
    
    private NetworkStatusManager networkStatusManager;
    private Context context;
    
    private OfflineOperationsHelper(Context context) {
        this.context = context.getApplicationContext();
        this.networkStatusManager = NetworkStatusManager.getInstance(context);
    }
    
    public static synchronized OfflineOperationsHelper getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineOperationsHelper(context);
        }
        return instance;
    }
    
    /**
     * تنفيذ عملية قراءة مع دعم العمل بدون اتصال
     */
    public void performOfflineRead(String collection, String documentId, 
                                   OnOfflineOperationListener listener) {
        boolean isOnline = networkStatusManager.isOnline();
        
        Log.d(TAG, "Performing read operation for " + collection + "/" + documentId + 
                   " (Online: " + isOnline + ")");
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Source source = isOnline ? Source.DEFAULT : Source.CACHE;
        
        db.collection(collection).document(documentId)
                .get(source)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onSuccess();
                        if (!isOnline) {
                            showOfflineMessage("تم تحميل البيانات من التخزين المحلي");
                        }
                    } else {
                        listener.onError("Document not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Read operation failed", e);
                    if (!isOnline) {
                        showOfflineMessage("البيانات غير متوفرة محلياً");
                    }
                    listener.onError(e.getMessage());
                });
    }
    
    /**
     * تنفيذ عملية كتابة مع دعم العمل بدون اتصال
     */
    public void performOfflineWrite(String operationType, OnOfflineOperationListener listener) {
        boolean isOnline = networkStatusManager.isOnline();
        
        Log.d(TAG, "Performing write operation: " + operationType + " (Online: " + isOnline + ")");
        
        if (isOnline) {
            showOnlineMessage("جاري حفظ " + operationType + "...");
        } else {
            showOfflineMessage("تم حفظ " + operationType + " محلياً - ستتم المزامنة عند عودة الاتصال");
        }
        
        // Firebase Firestore تتعامل مع العمليات بدون اتصال تلقائياً
        // لذلك نستدعي listener.onSuccess() مباشرة
        listener.onSuccess();
    }
    
    /**
     * تنفيذ عملية حذف مع دعم العمل بدون اتصال
     */
    public void performOfflineDelete(String itemType, OnOfflineOperationListener listener) {
        boolean isOnline = networkStatusManager.isOnline();
        
        Log.d(TAG, "Performing delete operation for " + itemType + " (Online: " + isOnline + ")");
        
        if (isOnline) {
            showOnlineMessage("جاري حذف " + itemType + "...");
        } else {
            showOfflineMessage("تم حذف " + itemType + " محلياً - ستتم المزامنة عند عودة الاتصال");
        }
        
        listener.onSuccess();
    }
    
    /**
     * عرض رسالة للعمليات عبر الإنترنت
     */
    private void showOnlineMessage(String message) {
        Toast.makeText(context, "🌐 " + message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * عرض رسالة للعمليات المحلية
     */
    private void showOfflineMessage(String message) {
        Toast.makeText(context, "📱 " + message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * التحقق من حالة الشبكة وعرض رسالة مناسبة
     */
    public void checkNetworkAndShowStatus() {
        boolean isOnline = networkStatusManager.isOnline();
        String message = networkStatusManager.getNetworkStatusDescription();
        
        if (isOnline) {
            showOnlineMessage(message);
        } else {
            showOfflineMessage(message);
        }
    }
    
    /**
     * عرض نصائح للعمل بدون اتصال
     */
    public void showOfflineTips() {
        if (!networkStatusManager.isOnline()) {
            String tips = "💡 نصائح الوضع المحلي:\n" +
                         "• يمكنك إنشاء وتعديل الفواتير\n" +
                         "• البيانات محفوظة محلياً\n" +
                         "• ستتم المزامنة عند عودة الاتصال";
            
            Toast.makeText(context, tips, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * الحصول على نص حالة الشبكة
     */
    public String getNetworkStatusText() {
        boolean isOnline = networkStatusManager.isOnline();
        return isOnline ? 
            context.getString(R.string.network_online) : 
            context.getString(R.string.network_offline);
    }
    
    /**
     * التحقق من أن العملية يمكن تنفيذها بدون اتصال
     */
    public boolean canOperateOffline(String operationType) {
        // معظم عمليات نقطة البيع يمكن تنفيذها بدون اتصال
        // باستثناء العمليات التي تتطلب بيانات خارجية
        switch (operationType.toLowerCase()) {
            case "login":
            case "register":
            case "sync":
                return false;
            case "invoice":
            case "product":
            case "customer":
            case "payment":
                return true;
            default:
                return true;
        }
    }
    
    /**
     * واجهة مستمع العمليات بدون اتصال
     */
    public interface OnOfflineOperationListener {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * إنشاء مستمع بسيط للعمليات
     */
    public static OnOfflineOperationListener createSimpleListener(Context context, String successMessage) {
        return new OnOfflineOperationListener() {
            @Override
            public void onSuccess() {
                if (successMessage != null && !successMessage.isEmpty()) {
                    Toast.makeText(context, "✅ " + successMessage, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(context, "❌ خطأ: " + error, Toast.LENGTH_LONG).show();
            }
        };
    }
}