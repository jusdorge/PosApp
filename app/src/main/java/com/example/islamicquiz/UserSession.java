package com.example.islamicquiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.islamicquiz.model.Permission;
import com.example.islamicquiz.model.Resource;
import com.example.islamicquiz.model.User;
import com.example.islamicquiz.model.UserRole;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

/**
 * إدارة جلسة المستخدم الحالي
 */
public class UserSession {
    private static final String TAG = "UserSession";
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USER_DATA = "user_data";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGIN_TIME = "login_time";
    
    private static UserSession instance;
    private Context context;
    private SharedPreferences sharedPreferences;
    private User currentUser;
    private PermissionManager permissionManager;
    private FirebaseFirestore db;
    
    private UserSession(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.permissionManager = PermissionManager.getInstance();
        this.db = FirebaseFirestore.getInstance();
        loadUserFromPreferences();
    }
    
    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context);
        }
        return instance;
    }
    
    /**
     * تسجيل دخول المستخدم
     */
    public void loginUser(User user) {
        this.currentUser = user;
        
        // تحديث آخر تسجيل دخول
        user.updateLastLogin();
        
        // حفظ في SharedPreferences
        saveUserToPreferences(user);
        
        // تحديث في Firebase
        updateUserInFirebase(user);
        
        Log.d(TAG, "User logged in: " + user.getUsername());
    }
    
    /**
     * تسجيل خروج المستخدم
     */
    public void logoutUser() {
        this.currentUser = null;
        clearUserFromPreferences();
        Log.d(TAG, "User logged out");
    }
    
    /**
     * الحصول على المستخدم الحالي
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * فحص ما إذا كان المستخدم مسجل دخول
     */
    public boolean isLoggedIn() {
        return currentUser != null && currentUser.isActive() && 
               currentUser.getRole() != null &&
               sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * فحص صلاحية المستخدم الحالي
     */
    public boolean hasPermission(Resource resource, Permission permission) {
        if (!isLoggedIn()) {
            return false;
        }
        return permissionManager.hasPermission(currentUser, resource, permission);
    }
    
    /**
     * فحص ما إذا كان المستخدم الحالي له أي من الصلاحيات المحددة
     */
    public boolean hasAnyPermission(Resource resource, Permission... permissions) {
        if (!isLoggedIn()) {
            return false;
        }
        return permissionManager.hasAnyPermission(currentUser, resource, permissions);
    }
    
    /**
     * فحص ما إذا كان المستخدم الحالي مدير نظام
     */
    public boolean isAdmin() {
        return isLoggedIn() && currentUser.isAdmin();
    }
    
    /**
     * فحص ما إذا كان المستخدم الحالي مدير أو أعلى
     */
    public boolean isManagerOrAbove() {
        return isLoggedIn() && currentUser.isManagerOrAbove();
    }
    
    /**
     * تحديث بيانات المستخدم الحالي
     */
    public void updateCurrentUser(User updatedUser) {
        if (currentUser != null && currentUser.getId().equals(updatedUser.getId())) {
            this.currentUser = updatedUser;
            saveUserToPreferences(updatedUser);
            updateUserInFirebase(updatedUser);
        }
    }
    
    /**
     * الحصول على دور المستخدم الحالي
     */
    public UserRole getCurrentUserRole() {
        return isLoggedIn() ? currentUser.getRole() : null;
    }
    
    /**
     * الحصول على اسم المستخدم الحالي
     */
    public String getCurrentUserName() {
        return isLoggedIn() ? currentUser.getFullName() : null;
    }
    
    /**
     * فحص ما إذا كان يمكن للمستخدم الحالي إدارة مستخدم آخر
     */
    public boolean canManageUser(User targetUser) {
        if (!isLoggedIn()) {
            return false;
        }
        return permissionManager.canManageUser(currentUser, targetUser);
    }
    
    /**
     * فحص ما إذا كان يمكن للمستخدم الحالي تعيين دور محدد
     */
    public boolean canAssignRole(UserRole role) {
        if (!isLoggedIn()) {
            return false;
        }
        return permissionManager.canAssignRole(currentUser, role);
    }
    
    /**
     * التحقق من صحة الجلسة
     */
    public boolean validateSession() {
        if (!isLoggedIn()) {
            return false;
        }
        
        // التحقق من انتهاء صلاحية الجلسة (24 ساعة)
        long loginTime = sharedPreferences.getLong(KEY_LOGIN_TIME, 0);
        long currentTime = System.currentTimeMillis();
        long sessionDuration = currentTime - loginTime;
        long maxSessionDuration = 24 * 60 * 60 * 1000; // 24 ساعة
        
        if (sessionDuration > maxSessionDuration) {
            logoutUser();
            return false;
        }
        
        return true;
    }
    
    /**
     * تجديد الجلسة
     */
    public void refreshSession() {
        if (isLoggedIn()) {
            sharedPreferences.edit()
                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                .apply();
        }
    }
    
    private void saveUserToPreferences(User user) {
        try {
            Gson gson = new Gson();
            String userJson = gson.toJson(user);
            
            sharedPreferences.edit()
                .putString(KEY_USER_DATA, userJson)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
                .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving user to preferences", e);
        }
    }
    
    private void loadUserFromPreferences() {
        try {
            String userJson = sharedPreferences.getString(KEY_USER_DATA, null);
            if (userJson != null) {
                Gson gson = new Gson();
                currentUser = gson.fromJson(userJson, User.class);
                
                // التحقق من صحة الجلسة
                if (!validateSession()) {
                    currentUser = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user from preferences", e);
            clearUserFromPreferences();
        }
    }
    
    private void clearUserFromPreferences() {
        sharedPreferences.edit()
            .remove(KEY_USER_DATA)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_LOGIN_TIME)
            .apply();
    }
    
    private void updateUserInFirebase(User user) {
        if (user.getId() != null) {
            db.collection("users").document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User updated in Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user in Firebase", e);
                });
        }
    }
    
    /**
     * إنشاء مستخدم افتراضي (مدير نظام) إذا لم يكن موجوداً
     */
    public void createDefaultAdminIfNeeded() {
        db.collection("users")
            .whereEqualTo("role", UserRole.ADMIN.name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    // إنشاء مدير نظام افتراضي
                    User defaultAdmin = new User(
                        "admin",
                        "admin@posapp.com",
                        context.getString(R.string.system_admin),
                        UserRole.ADMIN
                    );
                    
                    db.collection("users")
                        .add(defaultAdmin)
                        .addOnSuccessListener(documentReference -> {
                            defaultAdmin.setId(documentReference.getId());
                            Log.d(TAG, "Default admin created with ID: " + documentReference.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error creating default admin", e);
                        });
                }
                
                // إصلاح البيانات القديمة
                fixLegacyUserRoles();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking for admin users", e);
            });
    }
    
    /**
     * إصلاح الأدوار القديمة في قاعدة البيانات
     */
    private void fixLegacyUserRoles() {
        db.collection("users")
            .whereEqualTo("role", "user") // البحث عن القيم القديمة
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " legacy user records to fix");
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // تحديث الدور القديم إلى EMPLOYEE
                        db.collection("users").document(document.getId())
                            .update("role", UserRole.EMPLOYEE.name())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Fixed legacy role for user: " + document.getId());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fixing legacy role for user: " + document.getId(), e);
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking for legacy user roles", e);
            });
    }

    /**
     * فحص ما إذا كان المستخدم الحالي ضيف مؤقت
     */
    public boolean isGuestUser() {
        return isLoggedIn() && currentUser.getUsername() != null && 
               currentUser.getUsername().startsWith("guest_");
    }
    
    /**
     * الحصول على رسالة حالة المستخدم
     */
    public String getUserStatusMessage() {
        if (!isLoggedIn()) {
            return context.getString(R.string.not_logged_in);
        }
        
        if (isGuestUser()) {
            return context.getString(R.string.guest_account_limited);
        }
        
        return currentUser.getRole().getDisplayName();
    }
} 