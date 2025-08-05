package com.example.posapp.utils;

import android.content.Context;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;

/**
 * أدوات مساعدة للتعامل مع الـ Dialogs بأمان
 */
public class DialogUtils {
    
    /**
     * إظهار Toast بأمان من DialogFragment مع فحص الـ context
     * 
     * @param dialogFragment الـ DialogFragment
     * @param message النص المراد إظهاره
     */
    public static void showToastSafely(DialogFragment dialogFragment, String message) {
        showToastSafely(dialogFragment, message, Toast.LENGTH_SHORT);
    }
    
    /**
     * إظهار Toast بأمان من DialogFragment مع فحص الـ context
     * 
     * @param dialogFragment الـ DialogFragment
     * @param message النص المراد إظهاره
     * @param duration مدة عرض Toast
     */
    public static void showToastSafely(DialogFragment dialogFragment, String message, int duration) {
        try {
            // فحص أن الـ DialogFragment ما زال مرتبطاً بـ Activity
            if (dialogFragment != null && 
                dialogFragment.isAdded() && 
                !dialogFragment.isDetached() && 
                dialogFragment.getContext() != null) {
                
                Toast.makeText(dialogFragment.getContext(), message, duration).show();
            } else {
                // في حالة عدم توفر الـ context، اطبع في الـ log
                android.util.Log.i("DialogUtils", "Toast message (context unavailable): " + message);
            }
        } catch (Exception e) {
            // في حالة حدوث أي خطأ، اطبع في الـ log
            android.util.Log.e("DialogUtils", "Error showing toast: " + message, e);
        }
    }
    
    /**
     * إظهار Toast بأمان من Context عادي مع فحص الـ context
     * 
     * @param context الـ Context
     * @param message النص المراد إظهاره
     */
    public static void showToastSafely(Context context, String message) {
        showToastSafely(context, message, Toast.LENGTH_SHORT);
    }
    
    /**
     * إظهار Toast بأمان من Context عادي مع فحص الـ context
     * 
     * @param context الـ Context
     * @param message النص المراد إظهاره
     * @param duration مدة عرض Toast
     */
    public static void showToastSafely(Context context, String message, int duration) {
        try {
            if (context != null) {
                Toast.makeText(context, message, duration).show();
            } else {
                // في حالة عدم توفر الـ context، اطبع في الـ log
                android.util.Log.i("DialogUtils", "Toast message (context unavailable): " + message);
            }
        } catch (Exception e) {
            // في حالة حدوث أي خطأ، اطبع في الـ log
            android.util.Log.e("DialogUtils", "Error showing toast: " + message, e);
        }
    }
    
    /**
     * إغلاق DialogFragment بأمان مع فحص الحالة
     * 
     * @param dialogFragment الـ DialogFragment المراد إغلاقه
     */
    public static void dismissSafely(DialogFragment dialogFragment) {
        try {
            if (dialogFragment != null && 
                dialogFragment.isAdded() && 
                !dialogFragment.isDetached() &&
                dialogFragment.getFragmentManager() != null) {
                
                dialogFragment.dismiss();
            } else {
                android.util.Log.i("DialogUtils", "Cannot dismiss dialog - not in valid state");
            }
        } catch (Exception e) {
            android.util.Log.e("DialogUtils", "Error dismissing dialog", e);
        }
    }
    
    /**
     * فحص ما إذا كان DialogFragment في حالة آمنة للتعامل معه
     * 
     * @param dialogFragment الـ DialogFragment
     * @return true إذا كان آمناً للاستخدام
     */
    public static boolean isDialogFragmentSafe(DialogFragment dialogFragment) {
        return dialogFragment != null && 
               dialogFragment.isAdded() && 
               !dialogFragment.isDetached() && 
               dialogFragment.getContext() != null &&
               dialogFragment.getFragmentManager() != null;
    }
}