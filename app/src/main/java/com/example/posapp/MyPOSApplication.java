package com.example.posapp;

import android.app.Application;
import android.util.Log;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import com.google.firebase.FirebaseApp;

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
}