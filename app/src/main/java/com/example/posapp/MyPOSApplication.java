package com.example.posapp;

import android.app.Application;
import android.util.Log;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class MyPOSApplication extends MultiDexApplication {
    private static final String TAG = "MyPOSApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // سجل رسالة لنعرف أن Application بدأ
        Log.d(TAG, "✓ Application onCreate started");

        // تعطيل معالج الأخطاء مؤقتاً للتشخيص
        // setupGlobalErrorHandler();

        // تهيئة Firebase مع معالجة الأخطاء
        try {
            Log.d(TAG, "Initializing Firebase...");
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "✅ Firebase initialized successfully");
            
            // تفعيل Offline Persistence لـ Firestore
            setupFirestoreOfflineSupport();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Firebase initialization failed", e);
        }
        
        // تفعيل معالج الأخطاء لـ MediaTek
        setupGlobalErrorHandler();
        
        Log.d(TAG, "✅ Application onCreate completed");
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "✓ attachBaseContext called");
        try {
            MultiDex.install(this);
            Log.d(TAG, "✓ MultiDex installed");
        } catch (Exception e) {
            Log.e(TAG, "❌ MultiDex installation failed", e);
        }
    }
    
    /**
     * إعداد معالج الأخطاء العام للتطبيق (نشط)
     * يتعامل مع أخطاء MediaTek والأخطاء الأخرى
     */
    private void setupGlobalErrorHandler() {
        Log.d(TAG, "Setting up global error handler...");
        
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable exception) {
                Log.e(TAG, "Uncaught exception occurred: ", exception);
                
                // التحقق من أخطاء MediaTek المعروفة
                if (isMediaTekError(exception)) {
                    Log.w(TAG, "MediaTek system error detected - ignoring");
                    // تجاهل أخطاء MediaTek المعروفة
                    return;
                }
                
                // للأخطاء الأخرى، نستخدم المعالج الافتراضي
                Log.e(TAG, "Passing exception to original handler");
                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, exception);
                }
            }
        });
        
        Log.d(TAG, "✓ Global error handler set up");
    }
    
    /**
     * فحص ما إذا كان الخطأ متعلق بـ MediaTek
     */
    private boolean isMediaTekError(Throwable exception) {
        if (exception == null) return false;
        
        String message = exception.getMessage();
        String className = exception.getClass().getName();
        
        // أخطاء MediaTek المعروفة
        return (message != null && (
            message.contains("com.mediatek.view.impl.MsyncFactoryImpl") ||
            message.contains("MsyncFactory") ||
            message.contains("mediatek") ||
            message.contains("mtk")
        )) || (
            className.contains("ClassNotFoundException") && 
            message != null && message.contains("mediatek")
        );
    }
    
    /**
     * إعداد دعم العمل بدون إنترنت لـ Firestore
     */
    private void setupFirestoreOfflineSupport() {
        try {
            Log.d(TAG, "Setting up Firestore offline support...");
            
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            // إعداد إعدادات Firestore للعمل بدون إنترنت
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)  // تفعيل التخزين المحلي
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)  // حجم التخزين غير محدود
                    .build();
            
            db.setFirestoreSettings(settings);
            
            Log.d(TAG, "✅ Firestore Offline Persistence enabled!");
            Log.d(TAG, "📱 App can now work offline and sync when back online");
            Log.d(TAG, "🔄 Data will be cached locally and synchronized automatically");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to setup Firestore offline support", e);
        }
    }
}