package com.example.posapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * فئة مساعدة للتعامل مع أخطاء الشبكة وFirebase
 */
public class NetworkErrorHandler {
    private static final String TAG = "NetworkErrorHandler";
    
    /**
     * التعامل مع أخطاء Firebase Firestore
     */
    public static void handleFirestoreError(Context context, Exception exception, String operation) {
        if (context == null) return;
        
        String userMessage = "حدث خطأ غير متوقع";
        boolean showRetryOption = true;
        
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            
            switch (firestoreException.getCode()) {
                case PERMISSION_DENIED:
                    userMessage = "ليس لديك صلاحية للوصول لهذه البيانات";
                    showRetryOption = false;
                    break;
                case UNAVAILABLE:
                    userMessage = "الخدمة غير متاحة حالياً. يرجى المحاولة لاحقاً";
                    break;
                case DEADLINE_EXCEEDED:
                    userMessage = "انتهت مهلة الاتصال. تحقق من سرعة الإنترنت";
                    break;
                case UNAUTHENTICATED:
                    userMessage = "انتهت صلاحية تسجيل الدخول. يرجى تسجيل الدخول مرة أخرى";
                    showRetryOption = false;
                    break;
                case NOT_FOUND:
                    userMessage = "البيانات المطلوبة غير موجودة";
                    showRetryOption = false;
                    break;
                case ALREADY_EXISTS:
                    userMessage = "البيانات موجودة مسبقاً";
                    showRetryOption = false;
                    break;
                case RESOURCE_EXHAUSTED:
                    userMessage = "تم تجاوز الحد المسموح. يرجى المحاولة لاحقاً";
                    break;
                default:
                    if (!isNetworkAvailable(context)) {
                        userMessage = "لا يوجد اتصال بالإنترنت. تحقق من شبكة WiFi أو بيانات الهاتف";
                    } else {
                        userMessage = "خطأ في الخدمة: " + operation;
                    }
                    break;
            }
        } else if (exception instanceof FirebaseNetworkException) {
            userMessage = "خطأ في الشبكة. تحقق من اتصال الإنترنت";
        } else if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            userMessage = getAuthErrorMessage(authException.getErrorCode());
            showRetryOption = authException.getErrorCode().equals("network-request-failed");
        }
        
        // عرض رسالة الخطأ للمستخدم
        if (showRetryOption && context instanceof android.app.Activity) {
            showErrorDialog((android.app.Activity) context, userMessage, operation);
        } else {
            Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show();
        }
        
        // تسجيل الخطأ
        android.util.Log.e(TAG, "Error in operation: " + operation, exception);
    }
    
    /**
     * رسائل أخطاء المصادقة
     */
    private static String getAuthErrorMessage(String errorCode) {
        switch (errorCode) {
            case "invalid-email":
                return "البريد الإلكتروني غير صحيح";
            case "user-disabled":
                return "تم تعطيل هذا الحساب";
            case "user-not-found":
                return "لا يوجد حساب بهذا البريد الإلكتروني";
            case "wrong-password":
                return "كلمة المرور غير صحيحة";
            case "weak-password":
                return "كلمة المرور ضعيفة. يجب أن تكون 6 أحرف على الأقل";
            case "email-already-in-use":
                return "هذا البريد الإلكتروني مستخدم مسبقاً";
            case "network-request-failed":
                return "خطأ في الشبكة. تحقق من اتصال الإنترنت";
            case "too-many-requests":
                return "تم تجاوز عدد المحاولات المسموح. يرجى المحاولة لاحقاً";
            default:
                return "خطأ في المصادقة: " + errorCode;
        }
    }
    
    /**
     * فحص توفر الإنترنت
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error checking network availability", e);
        }
        
        return false;
    }
    
    /**
     * عرض حوار خطأ مع خيار إعادة المحاولة
     */
    private static void showErrorDialog(android.app.Activity activity, String message, String operation) {
        if (activity == null || activity.isFinishing()) return;
        
        new android.app.AlertDialog.Builder(activity)
            .setTitle("خطأ في " + operation)
            .setMessage(message + "\n\nهل تريد إعادة المحاولة؟")
            .setPositiveButton("إعادة المحاولة", (dialog, which) -> {
                // يمكن للأنشطة تنفيذ منطق إعادة المحاولة الخاص بها
                Toast.makeText(activity, "يرجى إعادة تنفيذ العملية", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("إلغاء", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    /**
     * معالجة أخطاء البيانات الفارغة أو المفقودة
     */
    public static void handleDataError(Context context, String dataType) {
        if (context == null) return;
        
        String message = "خطأ في البيانات: " + dataType + " غير موجود أو تالف";
        
        if (context instanceof android.app.Activity) {
            new android.app.AlertDialog.Builder((android.app.Activity) context)
                .setTitle("خطأ في البيانات")
                .setMessage(message + "\n\nيرجى الاتصال بالدعم الفني إذا استمرت المشكلة.")
                .setPositiveButton("موافق", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
        
        android.util.Log.w(TAG, "Data error: " + message);
    }
    
    /**
     * معالجة أخطاء العمليات المتزامنة
     */
    public static void handleSyncError(Context context, String syncOperation, Exception exception) {
        if (context == null) return;
        
        String message = "فشل في مزامنة " + syncOperation;
        
        if (!isNetworkAvailable(context)) {
            message += " - لا يوجد اتصال بالإنترنت";
        } else {
            message += " - خطأ في الخادم";
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        android.util.Log.e(TAG, "Sync error in " + syncOperation, exception);
    }
}