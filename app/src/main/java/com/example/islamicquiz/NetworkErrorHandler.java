package com.example.islamicquiz;

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
        
        String userMessage = context.getString(R.string.unexpected_error);
        boolean showRetryOption = true;
        
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            
            switch (firestoreException.getCode()) {
                case PERMISSION_DENIED:
                    userMessage = context.getString(R.string.no_permission_access_data);
                    showRetryOption = false;
                    break;
                case UNAVAILABLE:
                    userMessage = context.getString(R.string.service_unavailable_try_later);
                    break;
                case DEADLINE_EXCEEDED:
                    userMessage = context.getString(R.string.deadline_exceeded_check_internet);
                    break;
                case UNAUTHENTICATED:
                    userMessage = context.getString(R.string.session_expired_login_again);
                    showRetryOption = false;
                    break;
                case NOT_FOUND:
                    userMessage = context.getString(R.string.data_not_found);
                    showRetryOption = false;
                    break;
                case ALREADY_EXISTS:
                    userMessage = context.getString(R.string.data_already_exists);
                    showRetryOption = false;
                    break;
                case RESOURCE_EXHAUSTED:
                    userMessage = context.getString(R.string.resource_exhausted_try_later);
                    break;
                default:
                    if (!isNetworkAvailable(context)) {
                        userMessage = context.getString(R.string.no_internet_check_connection);
                    } else {
                        userMessage = context.getString(R.string.service_error_with_operation, operation);
                    }
                    break;
            }
        } else if (exception instanceof FirebaseNetworkException) {
            userMessage = context.getString(R.string.network_error_check_connection);
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
            .setTitle(activity.getString(R.string.error_in_operation, operation))
            .setMessage(activity.getString(R.string.retry_question_with_message, message))
            .setPositiveButton(activity.getString(R.string.retry), (dialog, which) -> {
                // يمكن للأنشطة تنفيذ منطق إعادة المحاولة الخاص بها
                Toast.makeText(activity, activity.getString(R.string.please_retry_operation), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    /**
     * معالجة أخطاء البيانات الفارغة أو المفقودة
     */
    public static void handleDataError(Context context, String dataType) {
        if (context == null) return;
        
        String message = context.getString(R.string.data_error_missing_or_corrupt, dataType);
        
        if (context instanceof android.app.Activity) {
            new android.app.AlertDialog.Builder((android.app.Activity) context)
                .setTitle(context.getString(R.string.data_error_title))
                .setMessage(context.getString(R.string.data_error_message_with_support, message))
                .setPositiveButton(context.getString(R.string.ok), null)
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
        
        String message = context.getString(R.string.sync_failed_for_operation, syncOperation);
        
        if (!isNetworkAvailable(context)) {
            message += " - " + context.getString(R.string.no_internet_suffix);
        } else {
            message += " - " + context.getString(R.string.server_error_suffix);
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        android.util.Log.e(TAG, "Sync error in " + syncOperation, exception);
    }
}