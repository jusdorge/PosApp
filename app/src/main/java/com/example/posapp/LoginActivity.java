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
     * تحميل بيانات المستخدم من Firestore - نسخة محسنة
     */
    private void loadUserData(String email) {
        Toast.makeText(this, "جاري البحث عن المستخدم...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "=== Starting comprehensive user search for: " + email + " ===");
        
        // البحث الشامل بدون أي شروط إضافية
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int userCount = queryDocumentSnapshots.size();
                    Log.d(TAG, "Search result: Found " + userCount + " users with email: " + email);
                    
                    if (userCount > 0) {
                        // طباعة تفاصيل كل مستخدم للتشخيص
                        int userIndex = 0;
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            userIndex++;
                            Log.d(TAG, "User #" + userIndex + " - Document ID: " + document.getId());
                            
                            try {
                                java.util.Map<String, Object> data = document.getData();
                                Log.d(TAG, "  Raw data: " + data.toString());
                                
                                // محاولة قراءة المستخدم
                                User user = parseUserFromDocument(document, email);
                                if (user != null) {
                                    Log.d(TAG, "  Parsed user: " + user.getFullName() + " (Active: " + user.isActive() + ")");
                                    
                                    // تسجيل الدخول بنجاح
                                    performUserLogin(user);
                                    return;
                                }
                                
                            } catch (Exception e) {
                                Log.e(TAG, "  Error processing user #" + userIndex + ": " + e.getMessage());
                                
                                // محاولة الإصلاح اليدوي
                                User manualUser = createUserFromRawData(document, email);
                                if (manualUser != null) {
                                    Log.d(TAG, "  Manually created user: " + manualUser.getFullName());
                                    performUserLogin(manualUser);
                                    return;
                                }
                            }
                        }
                        
                        // إذا وصلنا هنا، فلم نتمكن من معالجة أي مستخدم
                        Log.e(TAG, "Failed to process any of the " + userCount + " found users");
                        showUserProcessingError(email, userCount);
                        
                    } else {
                        // لا يوجد مستخدمين
                        Log.w(TAG, "No users found with email: " + email);
                        showUserNotFoundDialog(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Database query failed for email: " + email, e);
                    Toast.makeText(this, "خطأ في الاتصال بقاعدة البيانات: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    showDatabaseError(email, e);
                });
    }
    
    /**
     * تحليل المستخدم من المستند
     */
    private User parseUserFromDocument(com.google.firebase.firestore.QueryDocumentSnapshot document, String email) {
        try {
            User user = document.toObject(User.class);
            user.setId(document.getId());
            
            // التأكد من أن البريد الإلكتروني صحيح
            if (user.getEmail() == null || !user.getEmail().equals(email)) {
                user.setEmail(email);
            }
            
            // إصلاح الحقول المطلوبة
            fixRequiredFields(user, email);
            
            return user;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse user from document: " + document.getId(), e);
            return null;
        }
    }
    
    /**
     * إنشاء مستخدم من البيانات الخام
     */
    private User createUserFromRawData(com.google.firebase.firestore.QueryDocumentSnapshot document, String email) {
        try {
            java.util.Map<String, Object> data = document.getData();
            
            User user = new User();
            user.setId(document.getId());
            user.setEmail(email);
            
            // استخراج البيانات بأمان
            user.setFullName(extractString(data, "fullName", email.split("@")[0]));
            user.setUsername(extractString(data, "username", email.split("@")[0]));
            user.setPhone(extractString(data, "phone", ""));
            
            // التعامل مع الدور
            String roleStr = extractString(data, "role", "EMPLOYEE");
            user.setRole(parseUserRole(roleStr));
            
            // التعامل مع حالة التفعيل
            user.setActive(extractBoolean(data, "isActive", true));
            
            // التوقيتات
            user.setCreatedAt(extractTimestamp(data, "createdAt", com.google.firebase.Timestamp.now()));
            
            Log.d(TAG, "Manually created user: " + user.getFullName() + " with role: " + user.getRole());
            
            return user;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create user from raw data", e);
            return null;
        }
    }
    
    /**
     * إصلاح الحقول المطلوبة
     */
    private void fixRequiredFields(User user, String email) {
        boolean needsUpdate = false;
        
        // إصلاح الاسم
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            user.setFullName(email.split("@")[0]);
            needsUpdate = true;
        }
        
        // إصلاح اسم المستخدم
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            user.setUsername(email.split("@")[0]);
            needsUpdate = true;
        }
        
        // إصلاح الدور
        if (user.getRole() == null) {
            user.setRole(UserRole.EMPLOYEE);
            needsUpdate = true;
        }
        
        // إصلاح التفعيل
        if (!user.isActive()) {
            user.setActive(true);
            needsUpdate = true;
        }
        
        // إصلاح التوقيت
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(com.google.firebase.Timestamp.now());
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            Log.d(TAG, "Fixed required fields for user: " + email);
        }
    }
    
    /**
     * تسجيل دخول المستخدم
     */
    private void performUserLogin(User user) {
        try {
            // تحديث آخر دخول
            user.updateLastLogin();
            
            // حفظ التحديثات في قاعدة البيانات
            updateUserInFirebase(user);
            
            // تسجيل دخول المستخدم في الجلسة
            userSession.loginUser(user);
            
            // رسالة الترحيب
            String welcome = "مرحباً " + user.getFullName();
            if (user.getRole() != null) {
                welcome += " (" + user.getRole().getDisplayName() + ")";
            }
            
            Toast.makeText(this, welcome, Toast.LENGTH_LONG).show();
            Log.d(TAG, "✓ User login successful: " + user.getEmail());
            
            // الانتقال للشاشة الرئيسية
            startMainActivity();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during user login process", e);
            Toast.makeText(this, "خطأ في عملية تسجيل الدخول: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    // مساعدات لاستخراج البيانات
    private String extractString(java.util.Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return (value != null && !value.toString().trim().isEmpty()) ? value.toString() : defaultValue;
    }
    
    private boolean extractBoolean(java.util.Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    private com.google.firebase.Timestamp extractTimestamp(java.util.Map<String, Object> data, String key, com.google.firebase.Timestamp defaultValue) {
        Object value = data.get(key);
        if (value instanceof com.google.firebase.Timestamp) {
            return (com.google.firebase.Timestamp) value;
        }
        return defaultValue;
    }
    
    private UserRole parseUserRole(String roleStr) {
        if (roleStr == null) return UserRole.EMPLOYEE;
        
        try {
            // محاولة التحويل المباشر
            return UserRole.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            // التحويل من القيم القديمة
            switch (roleStr.toLowerCase()) {
                case "admin":
                case "مدير":
                    return UserRole.ADMIN;
                case "manager":
                case "مدير فرع":
                    return UserRole.MANAGER;
                case "employee":
                case "موظف":
                default:
                    return UserRole.EMPLOYEE;
            }
        }
    }
    
    /**
     * عرض خطأ معالجة المستخدم
     */
    private void showUserProcessingError(String email, int userCount) {
        String message = "تم العثور على " + userCount + " مستخدم بالبريد الإلكتروني " + email + 
                        " ولكن فشل في معالجة بياناتهم.\n\nماذا تريد أن تفعل؟";
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("خطأ في معالجة المستخدم")
                .setMessage(message)
                .setPositiveButton("إصلاح تلقائي", (dialog, which) -> {
                    forceFixUser(email);
                })
                .setNegativeButton("تشخيص النظام", (dialog, which) -> {
                    runDiagnostic();
                })
                .setNeutralButton("إنشاء جديد", (dialog, which) -> {
                    createNewUserAccount(email);
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * عرض خطأ قاعدة البيانات
     */
    private void showDatabaseError(String email, Exception error) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("خطأ في قاعدة البيانات")
                .setMessage("فشل في الاتصال بقاعدة البيانات:\n" + error.getMessage() + 
                           "\n\nتحقق من اتصال الإنترنت وإعدادات Firebase.")
                .setPositiveButton("إعادة المحاولة", (dialog, which) -> {
                    loadUserData(email);
                })
                .setNegativeButton("تشخيص", (dialog, which) -> {
                    runDiagnostic();
                })
                .show();
    }
    
    /**
     * إصلاح قسري للمستخدم
     */
    private void forceFixUser(String email) {
        Toast.makeText(this, "جاري الإصلاح القسري...", Toast.LENGTH_SHORT).show();
        
        // إنشاء مستخدم جديد بالحد الأدنى من البيانات
        User newUser = new User();
        newUser.setId("fixed_" + System.currentTimeMillis());
        newUser.setEmail(email);
        newUser.setFullName(email.split("@")[0]);
        newUser.setUsername(email.split("@")[0]);
        newUser.setRole(UserRole.EMPLOYEE);
        newUser.setActive(true);
        newUser.setCreatedAt(com.google.firebase.Timestamp.now());
        
        // حفظ في قاعدة البيانات
        db.collection("users")
                .add(newUser)
                .addOnSuccessListener(documentReference -> {
                    newUser.setId(documentReference.getId());
                    performUserLogin(newUser);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في الإصلاح: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    
    /**
     * معالجة المستخدمين الموجودين
     */
    private void processFoundUsers(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots, String email) {
        Log.d(TAG, "Processing " + queryDocumentSnapshots.size() + " found users");
        
        // البحث عن أول مستخدم صالح
        for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
            try {
                User user = document.toObject(User.class);
                user.setId(document.getId());
                
                Log.d(TAG, "Processing user: " + user.getFullName());
                Log.d(TAG, "- Active: " + user.isActive());
                Log.d(TAG, "- Role: " + (user.getRole() != null ? user.getRole().name() : "null"));
                
                // إصلاح المستخدم إذا لزم الأمر
                boolean wasFixed = fixUserIfNeeded(user);
                
                if (wasFixed) {
                    Log.d(TAG, "User was fixed, updating in database...");
                    updateUserInFirebase(user);
                }
                
                // تسجيل دخول المستخدم
                loginUserSuccessfully(user, wasFixed);
                return;
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing user document: " + document.getId(), e);
                
                // محاولة إصلاح البيانات التالفة
                try {
                    User fixedUser = fixCorruptedUserData(document, email);
                    if (fixedUser != null) {
                        loginUserSuccessfully(fixedUser, true);
                        return;
                    }
                } catch (Exception fixError) {
                    Log.e(TAG, "Failed to fix corrupted user: " + document.getId(), fixError);
                }
            }
        }
        
        // إذا وصلنا هنا، فلم نتمكن من معالجة أي مستخدم
        Log.e(TAG, "Failed to process any of the found users");
        Toast.makeText(this, "تم العثور على المستخدم ولكن فشل في معالجة البيانات", Toast.LENGTH_LONG).show();
        showUserNotFoundDialog(email);
    }
    
    /**
     * إصلاح المستخدم إذا لزم الأمر
     */
    private boolean fixUserIfNeeded(User user) {
        boolean needsUpdate = false;
        StringBuilder fixLog = new StringBuilder();
        
        // إصلاح حالة التفعيل
        if (!user.isActive()) {
            user.setActive(true);
            needsUpdate = true;
            fixLog.append("تفعيل الحساب، ");
        }
        
        // إصلاح الدور
        if (user.getRole() == null) {
            user.setRole(UserRole.EMPLOYEE);
            needsUpdate = true;
            fixLog.append("إضافة دور افتراضي، ");
        }
        
        // إصلاح الاسم
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            String emailName = user.getEmail() != null ? user.getEmail().split("@")[0] : "مستخدم";
            user.setFullName(emailName);
            needsUpdate = true;
            fixLog.append("إضافة اسم افتراضي، ");
        }
        
        // إصلاح اسم المستخدم
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            String emailName = user.getEmail() != null ? user.getEmail().split("@")[0] : "user";
            user.setUsername(emailName);
            needsUpdate = true;
            fixLog.append("إضافة اسم مستخدم، ");
        }
        
        // إصلاح التوقيتات
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(com.google.firebase.Timestamp.now());
            needsUpdate = true;
            fixLog.append("إضافة تاريخ الإنشاء، ");
        }
        
        if (needsUpdate) {
            Log.d(TAG, "Fixed user " + user.getEmail() + ": " + fixLog.toString());
        }
        
        return needsUpdate;
    }
    
    /**
     * إصلاح البيانات التالفة
     */
    private User fixCorruptedUserData(com.google.firebase.firestore.QueryDocumentSnapshot document, String email) throws Exception {
        Log.d(TAG, "Attempting to fix corrupted user data for: " + email);
        
        java.util.Map<String, Object> data = document.getData();
        
        User user = new User();
        user.setId(document.getId());
        user.setEmail(email);
        
        // استخراج البيانات بأمان
        user.setFullName(getStringValue(data, "fullName", email.split("@")[0]));
        user.setUsername(getStringValue(data, "username", email.split("@")[0]));
        user.setPhone(getStringValue(data, "phone", ""));
        user.setActive(true); // تفعيل تلقائي
        
        // تحويل الدور
        String roleString = getStringValue(data, "role", "EMPLOYEE");
        try {
            if (roleString.equals("admin") || roleString.equals("ADMIN")) {
                user.setRole(UserRole.ADMIN);
            } else if (roleString.equals("manager") || roleString.equals("MANAGER")) {
                user.setRole(UserRole.MANAGER);
            } else {
                user.setRole(UserRole.EMPLOYEE);
            }
        } catch (Exception e) {
            user.setRole(UserRole.EMPLOYEE);
        }
        
        // التوقيتات
        user.setCreatedAt(com.google.firebase.Timestamp.now());
        user.updateLastLogin();
        
        // حفظ البيانات المُصلحة
        updateUserInFirebase(user);
        
        Log.d(TAG, "Successfully fixed corrupted user: " + email);
        return user;
    }
    
    /**
     * تسجيل دخول المستخدم بنجاح
     */
    private void loginUserSuccessfully(User user, boolean wasFixed) {
        // تحديث آخر دخول
        user.updateLastLogin();
        if (!wasFixed) {
            updateUserInFirebase(user);
        }
        
        // تسجيل دخول المستخدم في الجلسة
        userSession.loginUser(user);
        
        String welcomeMessage = "مرحباً " + user.getFullName();
        if (wasFixed) {
            welcomeMessage += " (تم إصلاح الحساب)";
        }
        
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show();
        Log.d(TAG, "User logged in successfully: " + user.getEmail());
        
        startMainActivity();
    }
    
    /**
     * البحث الاحتياطي عن المستخدم بدون شرط isActive
     */
    private void fallbackUserSearch(String email) {
        Log.d(TAG, "Performing fallback search for email: " + email);
        
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Found user in fallback search, count: " + queryDocumentSnapshots.size());
                        
                        // فحص جميع المستخدمين الموجودين
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                User user = document.toObject(User.class);
                                user.setId(document.getId());
                                
                                Log.d(TAG, "User found - Name: " + user.getFullName() + 
                                     ", Active: " + user.isActive() + 
                                     ", Role: " + (user.getRole() != null ? user.getRole().name() : "null"));
                                
                                // إصلاح المستخدم إذا لزم الأمر
                                boolean needsUpdate = false;
                                
                                // إصلاح حالة التفعيل
                                if (!user.isActive()) {
                                    user.setActive(true);
                                    needsUpdate = true;
                                    Log.d(TAG, "Fixed isActive field for user: " + email);
                                }
                                
                                // إصلاح الدور
                                if (user.getRole() == null) {
                                    user.setRole(UserRole.EMPLOYEE);
                                    needsUpdate = true;
                                    Log.d(TAG, "Fixed role field for user: " + email);
                                }
                                
                                // تحديث المستخدم في قاعدة البيانات إذا لزم الأمر
                                if (needsUpdate) {
                                    updateUserInFirebase(user);
                                }
                                
                                // تسجيل دخول المستخدم
                                userSession.loginUser(user);
                                Toast.makeText(this, "مرحباً " + user.getFullName() + " (تم إصلاح الحساب)", Toast.LENGTH_LONG).show();
                                startMainActivity();
                                return;
                                
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user in fallback search", e);
                                // محاولة إصلاح البيانات التالفة
                                handleCorruptedUserData(document, email);
                                return;
                            }
                        }
                    } else {
                        Log.d(TAG, "No user found in fallback search either");
                        // المستخدم غير موجود نهائياً
                        showUserNotFoundDialog(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in fallback user search", e);
                    Toast.makeText(this, "خطأ في البحث عن المستخدم: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    
                    // عرض خيارات المساعدة
                    showSearchFailureDialog(email, e.getMessage());
                });
    }
    
    /**
     * معالجة المستخدم الموجود
     */
    private void handleFoundUser(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots, String email) {
        try {
            // المستخدم موجود في قاعدة البيانات
            QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
            User user = document.toObject(User.class);
            user.setId(document.getId());
            
            Log.d(TAG, "User loaded - Name: " + user.getFullName() + ", Role: " + 
                 (user.getRole() != null ? user.getRole().name() : "null"));
            
            // التحقق من صحة دور المستخدم وإصلاحه إذا لزم الأمر
            if (user.getRole() == null) {
                user.setRole(UserRole.EMPLOYEE); // دور افتراضي
                updateUserInFirebase(user);
                Log.d(TAG, "Fixed null role for user: " + email);
            }
            
            // تحديث آخر دخول
            user.updateLastLogin();
            updateUserInFirebase(user);
            
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
    }
    
    /**
     * عرض حوار فشل البحث مع خيارات المساعدة
     */
    private void showSearchFailureDialog(String email, String error) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("خطأ في البحث")
                .setMessage("فشل في البحث عن المستخدم:\n" + error + "\n\nماذا تريد أن تفعل؟")
                .setPositiveButton("إعادة المحاولة", (dialog, which) -> {
                    loadUserData(email);
                })
                .setNegativeButton("تشخيص النظام", (dialog, which) -> {
                    runDiagnostic();
                })
                .setNeutralButton("إنشاء حساب", (dialog, which) -> {
                    createNewUserAccount(email);
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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

