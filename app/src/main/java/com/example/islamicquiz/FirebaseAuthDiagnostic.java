package com.example.islamicquiz;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.islamicquiz.model.User;
import com.example.islamicquiz.model.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * أداة تشخيص وإصلاح مشاكل Firebase Auth
 */
public class FirebaseAuthDiagnostic {
    private static final String TAG = "FirebaseAuthDiagnostic";
    
    public interface DiagnosticCallback {
        void onResult(String diagnosis, String recommendation);
        void onFixed(String message);
        void onError(String error);
    }
    
    /**
     * تشخيص شامل لحالة Firebase Auth و Firestore
     */
    public static void runDiagnostic(Context context, DiagnosticCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        StringBuilder diagnosis = new StringBuilder("🔍 تشخيص Firebase:\n\n");
        
        // فحص Firebase Auth
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            diagnosis.append("✅ Firebase Auth: متصل\n");
            diagnosis.append("📧 البريد: ").append(currentUser.getEmail()).append("\n");
            diagnosis.append("🆔 UID: ").append(currentUser.getUid()).append("\n\n");
            
            // فحص وجود المستخدم في Firestore
            String email = currentUser.getEmail();
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        diagnosis.append("❌ Firestore: المستخدم غير موجود\n");
                        diagnosis.append("💡 الحل: إنشاء ملف في Firestore\n");
                        
                        callback.onResult(diagnosis.toString(), 
                            "يجب إنشاء ملف مستخدم في Firestore لهذا البريد");
                    } else {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        User user = document.toObject(User.class);
                        
                        diagnosis.append("✅ Firestore: المستخدم موجود\n");
                        diagnosis.append("👤 الاسم: ").append(user.getFullName()).append("\n");
                        diagnosis.append("🎭 الدور: ").append(user.getRole().getDisplayName()).append("\n");
                        diagnosis.append("🟢 الحالة: ").append(user.isActive() ? "نشط" : "معطل").append("\n");
                        
                        callback.onResult(diagnosis.toString(), "النظام يعمل بشكل صحيح!");
                    }
                })
                .addOnFailureListener(e -> {
                    diagnosis.append("❌ خطأ في الوصول لـ Firestore\n");
                    callback.onError("خطأ في الوصول لقاعدة البيانات: " + e.getMessage());
                });
                
        } else {
            diagnosis.append("❌ Firebase Auth: غير متصل\n");
            
            // فحص وجود مدير افتراضي في Firestore
            db.collection("users")
                .whereEqualTo("role", UserRole.ADMIN.name())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        diagnosis.append("❌ Firestore: لا يوجد أي مدير نظام\n");
                        diagnosis.append("💡 الحل: إنشاء مدير نظام افتراضي\n");
                        
                        callback.onResult(diagnosis.toString(), 
                            "يجب إنشاء مدير نظام افتراضي");
                    } else {
                        diagnosis.append("✅ Firestore: يوجد ").append(queryDocumentSnapshots.size()).append(" مدير\n");
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            User admin = document.toObject(User.class);
                            diagnosis.append("👤 مدير: ").append(admin.getFullName())
                                    .append(" (").append(admin.getEmail()).append(")\n");
                        }
                        
                        callback.onResult(diagnosis.toString(), 
                            "يجب إنشاء حساب Firebase Auth لأحد المديرين الموجودين");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onError("خطأ في الوصول لقاعدة البيانات: " + e.getMessage());
                });
        }
    }
    
    /**
     * إصلاح سريع للمشاكل الشائعة
     */
    public static void quickFix(Context context, DiagnosticCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // إنشاء مدير نظام افتراضي إذا لم يوجد
        db.collection("users")
            .whereEqualTo("role", UserRole.ADMIN.name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    // إنشاء مدير افتراضي
                    User defaultAdmin = new User(
                        "admin",
                        "admin@posapp.com", 
                        "مدير النظام الافتراضي",
                        UserRole.ADMIN
                    );
                    
                    db.collection("users")
                        .add(defaultAdmin)
                        .addOnSuccessListener(documentReference -> {
                            Log.i(TAG, "Default admin created: " + documentReference.getId());
                            callback.onFixed("تم إنشاء مدير النظام الافتراضي!\n\n" +
                                           "البريد: admin@posapp.com\n" +
                                           "يجب إنشاء حساب Firebase Auth بنفس البريد");
                        })
                        .addOnFailureListener(e -> {
                            callback.onError("فشل في إنشاء المدير الافتراضي: " + e.getMessage());
                        });
                } else {
                    callback.onResult("يوجد " + queryDocumentSnapshots.size() + " مدير في النظام", 
                                    "تحقق من وجود حساب Firebase Auth مطابق");
                }
            })
            .addOnFailureListener(e -> {
                callback.onError("خطأ في الوصول لقاعدة البيانات: " + e.getMessage());
            });
    }
    
    /**
     * عرض معلومات Firebase للمطور
     */
    public static void showFirebaseInfo(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        String info = "📱 معلومات Firebase:\n\n";
        
        if (currentUser != null) {
            info += "✅ مسجل دخول: نعم\n";
            info += "📧 البريد: " + currentUser.getEmail() + "\n";
            info += "🆔 UID: " + currentUser.getUid() + "\n";
            info += "✅ مفعّل: " + (currentUser.isEmailVerified() ? "نعم" : "لا") + "\n";
        } else {
            info += "❌ مسجل دخول: لا\n";
        }
        
        info += "\n📊 حالة الاتصال: " + (auth.getApp() != null ? "متصل" : "غير متصل");
        
        Toast.makeText(context, info, Toast.LENGTH_LONG).show();
        Log.i(TAG, info);
    }
} 