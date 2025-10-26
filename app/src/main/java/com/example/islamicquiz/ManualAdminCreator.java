package com.example.islamicquiz;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.islamicquiz.model.User;
import com.example.islamicquiz.model.UserRole;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * فئة مساعدة لإنشاء مدير النظام يدوياً في حالات الطوارئ
 */
public class ManualAdminCreator {
    private static final String TAG = "ManualAdminCreator";
    
    /**
     * إنشاء مدير نظام يدوياً
     * استخدم هذه الطريقة فقط إذا لم تعمل الطريقة التلقائية
     */
    public static void createEmergencyAdmin(Context context, String email, String fullName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // التحقق من عدم وجود مدير بهذا البريد
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    Toast.makeText(context, "مستخدم بهذا البريد موجود بالفعل", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // إنشاء مدير نظام جديد
                User emergencyAdmin = new User(
                    "admin_" + System.currentTimeMillis(), // اسم مستخدم فريد
                    email,
                    fullName,
                    UserRole.ADMIN
                );
                
                // حفظ في قاعدة البيانات
                db.collection("users")
                    .add(emergencyAdmin)
                    .addOnSuccessListener(documentReference -> {
                        emergencyAdmin.setId(documentReference.getId());
                        Log.d(TAG, "Emergency admin created: " + emergencyAdmin.getEmail());
                        
                        Toast.makeText(context, 
                            "تم إنشاء مدير النظام بنجاح!\n" +
                            "البريد: " + email + "\n" +
                            "يرجى إنشاء كلمة مرور في Firebase Auth", 
                            Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating emergency admin", e);
                        Toast.makeText(context, "فشل في إنشاء مدير النظام: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking existing users", e);
                Toast.makeText(context, "خطأ في فحص المستخدمين الموجودين", Toast.LENGTH_LONG).show();
            });
    }
    
    /**
     * إنشاء مدير النظام الافتراضي
     */
    public static void createDefaultAdmin(Context context) {
        createEmergencyAdmin(context, "admin@posapp.com", "مدير النظام الافتراضي");
    }
    
    /**
     * فحص وجود أي مدير نظام
     */
    public static void checkAdminExists(Context context, AdminCheckCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users")
            .whereEqualTo("role", UserRole.ADMIN.name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean hasAdmin = !queryDocumentSnapshots.isEmpty();
                int adminCount = queryDocumentSnapshots.size();
                
                callback.onResult(hasAdmin, adminCount);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking admin existence", e);
                callback.onError(e);
            });
    }
    
    /**
     * واجهة للتحقق من وجود المديرين
     */
    public interface AdminCheckCallback {
        void onResult(boolean hasAdmin, int adminCount);
        void onError(Exception error);
    }
} 