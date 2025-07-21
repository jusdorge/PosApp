package com.example.posapp;

import static android.content.ContentValues.TAG;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.posapp.model.User;
import com.example.posapp.model.UserRole;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private SignInButton googleSignInButton;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        try {
            // تحقق من أن Firebase تم تهيئته
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.d(TAG, "Firebase not initialized, initializing now");
                FirebaseApp.initializeApp(this);
            }

            // تهيئة Firebase Auth و Firestore
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            userSession = UserSession.getInstance(this);
            
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
            Toast.makeText(this, "فشل في تهيئة Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // إنشاء مدير نظام افتراضي إذا لزم الأمر
        userSession.createDefaultAdminIfNeeded();
        // إنشاء مدير النظام الافتراضي إذا لم يكن موجوداً
        //ManualAdminCreator.createDefaultAdmin(getContext());
        //إنشاء مدير مخصص
//        ManualAdminCreator.createEmergencyAdmin(this,
//                "jusdorge@gmail.com",
//                "مدير النظام");
        // تهيئة Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // ربط العناصر
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        // التحقق من جلسة مستخدم موجودة
        if (userSession.isLoggedIn() && userSession.validateSession()) {
            startMainActivity();
            return;
        }

        loginButton.setOnClickListener(v -> loginUser());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        try {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } catch (Exception e) {
            Log.e(TAG, "Google Sign In failed", e);
            Toast.makeText(this, "فشل في تسجيل الدخول: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this,"فشل تسجيل الدخول باستخدام Google: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            loadUserData(firebaseUser.getEmail());
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "فشل تسجيل الدخول: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show();
            return;
        }

        // تسجيل الدخول باستخدام Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        loadUserData(email);
                    } else {
                        String errorMessage = "فشل تسجيل الدخول: ";
                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        }
                        
                        // عرض رسالة الخطأ مع خيار التشخيص
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("خطأ في تسجيل الدخول")
                                .setMessage(errorMessage + "\n\nهل تريد تشخيص المشكلة؟")
                                .setPositiveButton("تشخيص المشكلة", (dialog, which) -> {
                                    runDiagnostic();
                                })
                                .setNegativeButton("إعادة المحاولة", null)
                                .setNeutralButton("إنشاء مدير", (dialog, which) -> {
                                    showCreateAdminDialog(email);
                                })
                                .show();
                    }
                });
    }
    
    /**
     * تحميل بيانات المستخدم من Firestore
     */
    private void loadUserData(String email) {
        Toast.makeText(this, "جاري تحميل بيانات المستخدم...", Toast.LENGTH_SHORT).show();
        
        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        try {
                            // المستخدم موجود في قاعدة البيانات
                            QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                            User user = document.toObject(User.class);
                            user.setId(document.getId());
                            
                            // التحقق من صحة دور المستخدم وإصلاحه إذا لزم الأمر
                            if (user.getRole() == null) {
                                user.setRole(UserRole.EMPLOYEE); // دور افتراضي
                                updateUserInFirebase(user);
                            }
                            
                            // تسجيل دخول المستخدم في الجلسة
                            userSession.loginUser(user);
                            
                            Toast.makeText(this, "مرحباً " + user.getFullName(), Toast.LENGTH_SHORT).show();
                            startMainActivity();
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing user data", e);
                            // محاولة إصلاح البيانات التالفة
                            com.google.firebase.firestore.DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            handleCorruptedUserData(documentSnapshot, email);
                        }
                    } else {
                        // المستخدم غير موجود، قد نحتاج لإنشاء حساب جديد
                        showUserNotFoundDialog(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(this, "خطأ في تحميل بيانات المستخدم: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    mAuth.signOut(); // تسجيل خروج من Firebase Auth
                });
    }
    
    /**
     * عرض حوار عندما لا يوجد مستخدم في قاعدة البيانات
     */
    private void showUserNotFoundDialog(String email) {
        // فحص ما إذا كان هذا البريد الإلكتروني هو المدير الافتراضي
        if ("admin@posapp.com".equals(email)) {
            showAdminSetupDialog(email);
            return;
        }
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("حساب غير مسجل")
                .setMessage("هذا البريد الإلكتروني غير مسجل في النظام.\n\nاختر أحد الخيارات:")
                .setPositiveButton("إنشاء حساب", (dialog, which) -> {
                    createNewUserAccount(email);
                })
                .setNeutralButton("دخول كضيف", (dialog, which) -> {
                    createGuestAccount(email);
                })
                .setNegativeButton("مدير نظام", (dialog, which) -> {
                    showCreateAdminDialog(email);
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * حوار إعداد مدير النظام الافتراضي
     */
    private void showAdminSetupDialog(String email) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("إعداد مدير النظام")
                .setMessage("تم اكتشاف محاولة دخول بحساب المدير الافتراضي.\n\n" +
                           "سيتم ربط هذا الحساب بصلاحيات مدير النظام الكاملة.")
                .setPositiveButton("تأكيد الربط", (dialog, which) -> {
                    linkAccountToAdminProfile(email);
                })
                .setNegativeButton("إلغاء", (dialog, which) -> {
                    mAuth.signOut();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * ربط الحساب بملف مدير النظام
     */
    private void linkAccountToAdminProfile(String email) {
        // البحث عن مدير النظام في قاعدة البيانات
        db.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("role", com.example.posapp.model.UserRole.ADMIN.name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // المدير موجود، قم بتسجيل الدخول
                    com.google.firebase.firestore.QueryDocumentSnapshot document = 
                        (com.google.firebase.firestore.QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                    
                    try {
                        com.example.posapp.model.User user = document.toObject(com.example.posapp.model.User.class);
                        user.setId(document.getId());
                        
                        // تحديث آخر دخول
                        user.updateLastLogin();
                        updateUserInFirebase(user);
                        
                        // تسجيل دخول المستخدم
                        userSession.loginUser(user);
                        
                        Toast.makeText(this, "مرحباً " + user.getFullName() + " - مدير النظام", 
                                Toast.LENGTH_LONG).show();
                        startMainActivity();
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing admin user", e);
                        createNewAdminProfile(email);
                    }
                } else {
                    // المدير غير موجود، أنشئ ملف جديد
                    createNewAdminProfile(email);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error finding admin user", e);
                createNewAdminProfile(email);
            });
    }
    
    /**
     * إنشاء ملف مدير نظام جديد
     */
    private void createNewAdminProfile(String email) {
        com.example.posapp.model.User adminUser = new com.example.posapp.model.User(
            "admin_" + System.currentTimeMillis(),
            email,
            "مدير النظام",
            com.example.posapp.model.UserRole.ADMIN
        );
        
        db.collection("users")
            .add(adminUser)
            .addOnSuccessListener(documentReference -> {
                adminUser.setId(documentReference.getId());
                
                // تسجيل دخول المدير الجديد
                userSession.loginUser(adminUser);
                
                Toast.makeText(this, 
                    "تم إنشاء حساب مدير النظام بنجاح!\nمرحباً " + adminUser.getFullName(), 
                    Toast.LENGTH_LONG).show();
                    
                // إشعار في الـ logs
                Log.i(TAG, "New admin created: " + email);
                
                startMainActivity();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating admin profile", e);
                Toast.makeText(this, "خطأ في إنشاء ملف مدير النظام: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                mAuth.signOut();
            });
    }
    
    /**
     * إنشاء حساب ضيف مؤقت
     */
    private void createGuestAccount(String email) {
        String guestName = "ضيف - " + email.split("@")[0];
        
        User guestUser = new User(
            "guest_" + System.currentTimeMillis(), // username فريد
            email,
            guestName,
            UserRole.VIEWER // صلاحيات عرض فقط
        );
        
        // تسجيل دخول مؤقت بدون حفظ في قاعدة البيانات
        userSession.loginUser(guestUser);
        
        Toast.makeText(this, "مرحباً " + guestName + " (حساب مؤقت)", Toast.LENGTH_LONG).show();
        
        // إشعار المدير بوجود ضيف جديد
        notifyAdminOfNewGuest(email, guestName);
        
        startMainActivity();
    }
    
    /**
     * إنشاء حساب جديد للمستخدم
     */
    private void createNewUserAccount(String email) {
        // عرض حوار لجمع بيانات المستخدم
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_account, null);
        
        android.widget.EditText fullNameEdit = dialogView.findViewById(R.id.fullNameEditText);
        android.widget.EditText phoneEdit = dialogView.findViewById(R.id.phoneEditText);
        
        // تعبئة البريد الإلكتروني تلقائياً
        String suggestedName = email.split("@")[0]; // استخدام الجزء الأول من البريد
        fullNameEdit.setText(suggestedName);
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("إنشاء حساب جديد")
                .setView(dialogView)
                .setPositiveButton("إنشاء", (dialog, which) -> {
                    String fullName = fullNameEdit.getText().toString().trim();
                    String phone = phoneEdit.getText().toString().trim();
                    
                    if (fullName.isEmpty()) {
                        Toast.makeText(this, "يرجى إدخال الاسم الكامل", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // إنشاء مستخدم جديد بدور EMPLOYEE افتراضي
                    User newUser = new User(
                        suggestedName, // username
                        email,
                        fullName,
                        UserRole.EMPLOYEE // دور افتراضي
                    );
                    newUser.setPhone(phone);
                    
                    // حفظ في قاعدة البيانات
                    saveNewUserToDatabase(newUser);
                })
                .setNegativeButton("إلغاء", (dialog, which) -> {
                    mAuth.signOut();
                    dialog.dismiss();
                })
                .show();
    }
    
    /**
     * حفظ المستخدم الجديد في قاعدة البيانات
     */
    private void saveNewUserToDatabase(User user) {
        Toast.makeText(this, "جاري إنشاء الحساب...", Toast.LENGTH_SHORT).show();
        
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    user.setId(documentReference.getId());
                    
                    // تسجيل دخول المستخدم الجديد
                    userSession.loginUser(user);
                    
                    // إشعار المدير بالمستخدم الجديد
                    notifyAdminOfNewUser(user);
                    
                    Toast.makeText(this, "تم إنشاء الحساب بنجاح - مرحباً " + user.getFullName(), 
                            Toast.LENGTH_LONG).show();
                    startMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating new user account", e);
                    Toast.makeText(this, "خطأ في إنشاء الحساب: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                });
    }
    
    /**
     * معالجة البيانات التالفة للمستخدم
     */
    private void handleCorruptedUserData(com.google.firebase.firestore.DocumentSnapshot document, String email) {
        try {
            // قراءة البيانات الخام
            Map<String, Object> data = document.getData();
            
            // إنشاء مستخدم جديد بالبيانات الصحيحة
            User user = new User();
            user.setId(document.getId());
            user.setEmail(email);
            
            // استخراج الحقول الأساسية
            user.setFullName(getStringValue(data, "fullName", "مستخدم"));
            user.setUsername(getStringValue(data, "username", email.split("@")[0]));
            user.setPhone(getStringValue(data, "phone", ""));
            user.setActive(getBooleanValue(data, "isActive", true));
            
            // تحويل الدور القديم
            String oldRole = getStringValue(data, "role", "user");
            user.setRole(UserRole.fromLegacyRole(oldRole));
            
            // تحديث التوقيتات
            if (data.containsKey("createdAt")) {
                user.setCreatedAt((com.google.firebase.Timestamp) data.get("createdAt"));
            } else {
                user.setCreatedAt(com.google.firebase.Timestamp.now());
            }
            
            if (data.containsKey("lastLogin")) {
                user.setLastLogin((com.google.firebase.Timestamp) data.get("lastLogin"));
            }
            
            // تحديث البيانات في قاعدة البيانات
            updateUserInFirebase(user);
            
            // تسجيل دخول المستخدم
            userSession.loginUser(user);
            
            Toast.makeText(this, "تم إصلاح بيانات المستخدم - مرحباً " + user.getFullName(), 
                    Toast.LENGTH_LONG).show();
            startMainActivity();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to fix corrupted user data", e);
            Toast.makeText(this, "خطأ في بيانات المستخدم. يرجى التواصل مع مدير النظام.", 
                    Toast.LENGTH_LONG).show();
            mAuth.signOut();
        }
    }
    
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private boolean getBooleanValue(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * تحديث بيانات المستخدم في Firebase
     */
    private void updateUserInFirebase(User user) {
        if (user.getId() != null) {
            db.collection("users").document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user data", e);
                });
        }
    }

    /**
     * إشعار المدير بمستخدم جديد
     */
    private void notifyAdminOfNewUser(User newUser) {
        try {
            // إنشاء إشعار للمدير
            java.util.Map<String, Object> notification = new java.util.HashMap<>();
            notification.put("type", "new_user");
            notification.put("title", "مستخدم جديد انضم للنظام");
            notification.put("message", newUser.getFullName() + " (" + newUser.getEmail() + ") أنشأ حساب جديد");
            notification.put("userId", newUser.getId());
            notification.put("userEmail", newUser.getEmail());
            notification.put("timestamp", com.google.firebase.Timestamp.now());
            notification.put("isRead", false);
            
            // إرسال إشعار لجميع المديرين
            db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Admin notified of new user: " + newUser.getEmail());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to notify admin of new user", e);
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error creating admin notification", e);
        }
    }
    
    /**
     * إشعار المدير بضيف جديد
     */
    private void notifyAdminOfNewGuest(String guestEmail, String guestName) {
        try {
            // إنشاء إشعار للمدير
            java.util.Map<String, Object> notification = new java.util.HashMap<>();
            notification.put("type", "new_guest");
            notification.put("title", "ضيف جديد دخل النظام");
            notification.put("message", guestName + " (" + guestEmail + ") دخل كضيف مؤقت");
            notification.put("guestEmail", guestEmail);
            notification.put("guestName", guestName);
            notification.put("timestamp", com.google.firebase.Timestamp.now());
            notification.put("isRead", false);
            notification.put("priority", "low");
            
            // إرسال إشعار لجميع المديرين
            db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Admin notified of new guest: " + guestEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to notify admin of new guest", e);
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error creating guest notification", e);
        }
    }

    /**
     * عرض حوار إنشاء مدير نظام من حساب موجود
     */
    private void showCreateAdminDialog(String email) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("تحويل إلى مدير نظام")
                .setMessage("هل تريد تحويل هذا الحساب إلى مدير نظام؟\n\n" +
                           "البريد: " + email + "\n\n" +
                           "⚠️ هذا الإجراء يمنح صلاحيات كاملة على النظام!")
                .setPositiveButton("نعم، أنشئ مدير", (dialog, which) -> {
                    createNewAdminProfile(email);
                })
                .setNegativeButton("إلغاء", (dialog, which) -> {
                    mAuth.signOut();
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    /**
     * تشغيل أداة التشخيص
     */
    private void runDiagnostic() {
        Toast.makeText(this, "جاري التشخيص...", Toast.LENGTH_SHORT).show();
        
        FirebaseAuthDiagnostic.runDiagnostic(this, new FirebaseAuthDiagnostic.DiagnosticCallback() {
            @Override
            public void onResult(String diagnosis, String recommendation) {
                showDiagnosticResult(diagnosis, recommendation);
            }
            
            @Override
            public void onFixed(String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, "خطأ في التشخيص: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * عرض نتيجة التشخيص
     */
    private void showDiagnosticResult(String diagnosis, String recommendation) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("نتيجة التشخيص")
                .setMessage(diagnosis + "\n📋 التوصية:\n" + recommendation)
                .setPositiveButton("إصلاح تلقائي", (dialog, which) -> {
                    runQuickFix();
                })
                .setNegativeButton("إغلاق", null)
                .setNeutralButton("معلومات Firebase", (dialog, which) -> {
                    FirebaseAuthDiagnostic.showFirebaseInfo(this);
                })
                .show();
    }
    
    /**
     * تشغيل الإصلاح السريع
     */
    private void runQuickFix() {
        Toast.makeText(this, "جاري الإصلاح...", Toast.LENGTH_SHORT).show();
        
        FirebaseAuthDiagnostic.quickFix(this, new FirebaseAuthDiagnostic.DiagnosticCallback() {
            @Override
            public void onResult(String diagnosis, String recommendation) {
                new android.app.AlertDialog.Builder(LoginActivity.this)
                        .setTitle("نتيجة الإصلاح")
                        .setMessage(diagnosis + "\n\n" + recommendation)
                        .setPositiveButton("حسناً", null)
                        .show();
            }
            
            @Override
            public void onFixed(String message) {
                new android.app.AlertDialog.Builder(LoginActivity.this)
                        .setTitle("تم الإصلاح!")
                        .setMessage(message)
                        .setPositiveButton("حسناً", null)
                        .show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, "فشل الإصلاح: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

