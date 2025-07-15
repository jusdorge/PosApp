package com.example.posapp;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;

public class MyPOSApplication extends Application {
    private static final String TAG = "MyPOSApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // سجل رسالة لنعرف أن Application بدأ
        Log.d(TAG, "Application onCreate started");

        // تهيئة Firebase مع معالجة الأخطاء
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }
    }
}