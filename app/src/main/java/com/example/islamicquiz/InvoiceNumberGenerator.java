package com.example.islamicquiz;

import android.content.Context;
import android.util.Log;

import com.example.islamicquiz.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * مولد أرقام الفواتير المخصص
 * ينشئ أرقام فواتير مركبة من رموز المستخدم (من البريد الإلكتروني) + رقم تسلسلي
 * 
 * أمثلة على رموز المستخدمين من البريد الإلكتروني:
 * - ahmed.mohamed@company.com → AHME001, AHME002...
 * - sara.ali@example.org → SARA001, SARA002...
 * - m.hassan123@gmail.com → MHAS001, MHAS002...
 * - user123@domain.com → USER001, USER002...
 * 
 * إذا لم يكن هناك بريد إلكتروني، يستخدم الاسم الكامل:
 * - أحمد محمد → AM001, AM002...
 */
public class InvoiceNumberGenerator {
    private static final String TAG = "InvoiceNumberGenerator";
    private static final String COUNTER_COLLECTION = "invoice_counters";
    
    private FirebaseFirestore db;
    private Context context;
    
    public InvoiceNumberGenerator(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * إنشاء رقم فاتورة جديد للمستخدم الحالي
     * @param user المستخدم
     * @return CompletableFuture يحتوي على رقم الفاتورة الجديد
     */
    public CompletableFuture<String> generateInvoiceNumber(User user) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        if (user == null || user.getId() == null) {
            future.completeExceptionally(new IllegalArgumentException("User or User ID cannot be null"));
            return future;
        }
        
        String userCode = generateUserCode(user);
        Log.d(TAG, "Generated user code: " + userCode + " for user: " + user.getFullName());
        
        // الحصول على العداد الحالي للمستخدم وزيادته
        getNextSequenceNumber(user.getId(), userCode)
            .thenAccept(sequenceNumber -> {
                String invoiceNumber = userCode + String.format("%03d", sequenceNumber);
                Log.d(TAG, "Generated invoice number: " + invoiceNumber);
                future.complete(invoiceNumber);
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Error generating invoice number", throwable);
                future.completeExceptionally(throwable);
                return null;
            });
        
        return future;
    }
    
    /**
     * إنشاء رمز المستخدم من البريد الإلكتروني
     * 
     * أمثلة:
     * - ahmed.mohamed@gmail.com → AHME
     * - sara123@company.org → SARA  
     * - m.hassan@example.com → MHAS
     * - user@domain.co.uk → USER
     * - test123@site.net → TEST
     * 
     * @param user المستخدم
     * @return رمز المستخدم (2-4 أحرف)
     */
    private String generateUserCode(User user) {
        String email = user.getEmail();
        
        // إذا لم يكن هناك بريد إلكتروني، استخدم الاسم الكامل كبديل
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return generateUserCodeFromName(user);
        }
        
        // استخراج الجزء الأول من البريد الإلكتروني (قبل @)
        String localPart = email.split("@")[0].trim();
        
        // تنظيف البريد من الأرقام والرموز الخاصة للحصول على الأحرف فقط
        String cleanPart = localPart.replaceAll("[^a-zA-Z]", "");
        
        // إذا لم تبق أحرف كافية، استخدم الجزء الأصلي مع معالجة خاصة
        if (cleanPart.length() < 2) {
            cleanPart = localPart.replaceAll("[^a-zA-Z0-9]", "");
        }
        
        StringBuilder userCode = new StringBuilder();
        
        if (cleanPart.length() >= 4) {
            // استخدم أول 4 أحرف
            userCode.append(cleanPart.substring(0, 4).toUpperCase());
        } else if (cleanPart.length() >= 2) {
            // استخدم الأحرف المتاحة وأضف أحرف من نهاية البريد إذا لزم الأمر
            userCode.append(cleanPart.toUpperCase());
            
            // إذا كان أقل من 3 أحرف، أضف أحرف من نهاية الجزء المحلي
            if (userCode.length() < 3 && localPart.length() > cleanPart.length()) {
                String remaining = localPart.replaceAll("[a-zA-Z]", "");
                for (char c : remaining.toCharArray()) {
                    if (Character.isDigit(c) && userCode.length() < 4) {
                        userCode.append(c);
                    }
                }
            }
        } else {
            // إذا لم نحصل على أحرف كافية، استخدم backup من الاسم
            return generateUserCodeFromName(user);
        }
        
        // التأكد من أن الرمز بطول 2-4 أحرف
        String result = userCode.toString();
        if (result.length() < 2) {
            // إضافة رقم أو حرف للوصول للحد الأدنى
            result += "1";
        }
        if (result.length() > 4) {
            result = result.substring(0, 4);
        }
        
        return result;
    }
    
    /**
     * إنشاء رمز المستخدم من الاسم الكامل (طريقة بديلة)
     * @param user المستخدم
     * @return رمز المستخدم
     */
    private String generateUserCodeFromName(User user) {
        String fullName = user.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            // استخدام اسم المستخدم كبديل
            fullName = user.getUsername();
            if (fullName == null || fullName.trim().isEmpty()) {
                return "USR"; // رمز افتراضي
            }
        }
        
        // تنظيف الاسم من المسافات والأحرف الخاصة
        String cleanName = fullName.trim().replaceAll("[^\\p{L}\\s]", "");
        String[] parts = cleanName.split("\\s+");
        
        StringBuilder userCode = new StringBuilder();
        
        if (parts.length == 1) {
            // اسم واحد فقط - استخدم أول 3 أحرف
            String name = parts[0];
            if (name.length() >= 3) {
                userCode.append(name.substring(0, 3).toUpperCase());
            } else {
                userCode.append(name.toUpperCase());
                // إضافة X للوصول لـ 3 أحرف
                while (userCode.length() < 3) {
                    userCode.append("X");
                }
            }
        } else if (parts.length == 2) {
            // اسم + اسم العائلة - استخدم أول حرفين من كل اسم
            String firstName = parts[0];
            String lastName = parts[1];
            
            userCode.append(firstName.substring(0, Math.min(2, firstName.length())).toUpperCase());
            userCode.append(lastName.substring(0, Math.min(2, lastName.length())).toUpperCase());
        } else {
            // أكثر من اسمين - استخدم أول حرف من كل اسم (الأول + الأخير + الأوسط)
            userCode.append(parts[0].substring(0, 1).toUpperCase());
            userCode.append(parts[parts.length - 1].substring(0, 1).toUpperCase());
            if (parts.length > 2) {
                userCode.append(parts[1].substring(0, 1).toUpperCase());
            }
        }
        
        // التأكد من أن الرمز بطول 2-4 أحرف
        String result = userCode.toString();
        if (result.length() > 4) {
            result = result.substring(0, 4);
        }
        
        return result;
    }
    
    /**
     * الحصول على الرقم التسلسلي التالي للمستخدم
     * @param userId معرف المستخدم
     * @param userCode رمز المستخدم
     * @return CompletableFuture يحتوي على الرقم التسلسلي التالي
     */
    private CompletableFuture<Integer> getNextSequenceNumber(String userId, String userCode) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        String counterId = userId; // استخدام معرف المستخدم كمعرف العداد
        
        db.collection(COUNTER_COLLECTION)
            .document(counterId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                int currentCount = 0;
                String storedUserCode = userCode;
                
                if (documentSnapshot.exists()) {
                    Long count = documentSnapshot.getLong("count");
                    currentCount = count != null ? count.intValue() : 0;
                    
                    // فحص ما إذا كان رمز المستخدم قد تغير
                    String existingUserCode = documentSnapshot.getString("userCode");
                    if (existingUserCode != null && !existingUserCode.equals(userCode)) {
                        Log.w(TAG, "User code changed from " + existingUserCode + " to " + userCode + " for user " + userId);
                        storedUserCode = userCode; // استخدام الرمز الجديد
                    }
                }
                
                // زيادة العداد
                int nextCount = currentCount + 1;
                
                // تحديث العداد في قاعدة البيانات
                Map<String, Object> counterData = new HashMap<>();
                counterData.put("count", nextCount);
                counterData.put("userCode", storedUserCode);
                counterData.put("userId", userId);
                counterData.put("lastUpdated", com.google.firebase.Timestamp.now());
                
                db.collection(COUNTER_COLLECTION)
                    .document(counterId)
                    .set(counterData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Counter updated successfully. Next sequence: " + nextCount);
                        future.complete(nextCount);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating counter", e);
                        future.completeExceptionally(e);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting counter", e);
                future.completeExceptionally(e);
            });
        
        return future;
    }
    
    /**
     * فحص ما إذا كان رقم الفاتورة موجود مسبقاً
     * @param invoiceNumber رقم الفاتورة
     * @return CompletableFuture<Boolean> true إذا كان الرقم موجود
     */
    public CompletableFuture<Boolean> isInvoiceNumberExists(String invoiceNumber) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        db.collection("invoices")
            .whereEqualTo("customInvoiceNumber", invoiceNumber)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                future.complete(!querySnapshot.isEmpty());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking invoice number existence", e);
                future.completeExceptionally(e);
            });
        
        return future;
    }
    
    /**
     * الحصول على إحصائيات الفواتير للمستخدم
     * @param userId معرف المستخدم
     * @return CompletableFuture يحتوي على عدد الفواتير
     */
    public CompletableFuture<Integer> getUserInvoiceCount(String userId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        db.collection(COUNTER_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long count = documentSnapshot.getLong("count");
                    future.complete(count != null ? count.intValue() : 0);
                } else {
                    future.complete(0);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user invoice count", e);
                future.completeExceptionally(e);
            });
        
        return future;
    }
    
    /**
     * إعادة تعيين عداد المستخدم (للمديرين فقط)
     * @param userId معرف المستخدم
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> resetUserCounter(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // فحص صلاحيات المدير
        UserSession userSession = UserSession.getInstance(context);
        if (!userSession.isAdmin()) {
            future.completeExceptionally(new SecurityException("Only admins can reset counters"));
            return future;
        }
        
        db.collection(COUNTER_COLLECTION)
            .document(userId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Counter reset successfully for user: " + userId);
                future.complete(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error resetting counter", e);
                future.completeExceptionally(e);
            });
        
        return future;
    }
}