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

    // ============================================================================
    private static final String[] AUTHORIZED_ADMINS = {
        "jusdorge@gmail.com",  // 🔴 غير هذا لبريدك الإلكتروني الفعلي
        // "admin2@company.com",  // مثال لإضافة مدير آخر
        // يمكن إضافة المزيد حسب الحاجة
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "✓ LoginActivity onCreate started");
        
        try {
            setContentView(R.layout.activity_login);
            Log.d(TAG, "✓ Layout set");
            
            // تهيئة Firebase
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            userSession = UserSession.getInstance(this);
            Log.d(TAG, "✓ Firebase and UserSession initialized");
            
            // التحقق من تسجيل الدخول المسبق
            if (userSession.isLoggedIn() && userSession.validateSession()) {
                Log.d(TAG, "User already logged in - redirecting to MainActivity");
                startMainActivity();
                return;
            }
            
            // تهيئة العناصر
            initializeViews();
            Log.d(TAG, "✓ Views initialized");
            
            // إعداد Google Sign In
            setupGoogleSignIn();
            Log.d(TAG, "✓ Google Sign In setup");
            
            Log.d(TAG, "✓ LoginActivity onCreate completed");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in onCreate", e);
            Toast.makeText(this, getString(R.string.login_init_error, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }
    
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        
        // إعداد مستمعات الأحداث
        loginButton.setOnClickListener(v -> performEmailLogin());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }
    
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    
    private void performEmailLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_email_and_password), Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Attempting email login for: " + email);
        Toast.makeText(this, getString(R.string.logging_in), Toast.LENGTH_SHORT).show();
        
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email login successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loadUserData(user.getEmail());
                        }
                    } else {
                        Log.w(TAG, "Email login failed", task.getException());
                        Toast.makeText(this, getString(R.string.login_failed_with_message,
                                (task.getException() != null ? task.getException().getMessage() : getString(R.string.unknown_error))),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign In");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google sign in successful: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, getString(R.string.google_login_failed, e.getMessage()), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase auth with Google successful");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loadUserData(user.getEmail());
                        }
                    } else {
                        Log.w(TAG, "Firebase auth with Google failed", task.getException());
                        Toast.makeText(this, getString(R.string.auth_failed_with_message,
                                (task.getException() != null ? task.getException().getMessage() : getString(R.string.unknown_error))),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    /**
     * فحص ما إذا كان المستخدم مدير مصرح له
     */
    private boolean isAuthorizedAdmin(String email) {
        if (email == null) return false;
        
        for (String authorizedEmail : AUTHORIZED_ADMINS) {
            if (authorizedEmail.equalsIgnoreCase(email.trim())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * فحص صلاحية دور المستخدم مع الحماية الأمنية
     */
    private boolean validateUserRole(User user) {
        if (user == null || user.getEmail() == null) {
            return false;
        }
        
        String email = user.getEmail();
        UserRole currentRole = user.getRole();
        
        // إذا كان المستخدم مدير، يجب أن يكون في القائمة المصرح بها
        if (currentRole == UserRole.ADMIN) {
            boolean isAuthorized = isAuthorizedAdmin(email);
            
            if (!isAuthorized) {
                // تسجيل محاولة دخول غير مصرح بها كمدير
                Log.w(TAG, "⚠️ SECURITY ALERT: Unauthorized admin access attempt by: " + email);
                
                // تقليل صلاحيات المستخدم فوراً
                user.setRole(UserRole.EMPLOYEE);
                Log.i(TAG, "User role downgraded to EMPLOYEE for security: " + email);
                
                // إشعار أمني
                Toast.makeText(this, getString(R.string.unauthorized_admin_login_denied), Toast.LENGTH_LONG).show();
                
                return false;
            } else {
                Log.i(TAG, "✅ Authorized admin login: " + email);
                return true;
            }
        }
        
        // للأدوار الأخرى (موظف، مدير فرع، مشاهد) - مسموح
        return true;
    }
    
    /**
     * إصلاح الحقول المطلوبة مع حماية أمنية
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
        
        // إصلاح الدور مع حماية المدراء
        if (user.getRole() == null) {
            // تحديد الدور الافتراضي
            if (isAuthorizedAdmin(email)) {
                user.setRole(UserRole.ADMIN);
                Log.i(TAG, "✅ Authorized admin role assigned to: " + email);
            } else {
                user.setRole(UserRole.EMPLOYEE);
                Log.d(TAG, "Default employee role assigned to: " + email);
            }
            needsUpdate = true;
        } else {
            // التحقق من صحة الدور الموجود
            if (!validateUserRole(user)) {
                needsUpdate = true; // الدور تم تعديله في validateUserRole
            }
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
            Log.d(TAG, "Fixed required fields for user: " + email + " with role: " + user.getRole());
        }
    }
    
    /**
     * تحميل بيانات المستخدم من Firestore - الدالة الأساسية
     */
    private void loadUserData(String email) {
        Toast.makeText(this, getString(R.string.searching_for_user), Toast.LENGTH_SHORT).show();
        Log.d(TAG, "=== Starting user search for: " + email + " ===");
        
        // البحث الشامل بدون أي شروط إضافية
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int userCount = queryDocumentSnapshots.size();
                    Log.d(TAG, "Search result: Found " + userCount + " users with email: " + email);
                    
                    if (userCount > 0) {
                        // معالجة المستخدمين الموجودين
                        processFoundUsers(queryDocumentSnapshots, email);
                    } else {
                        // لا يوجد مستخدمين
                        Log.w(TAG, "No users found with email: " + email);
                        showUserNotFoundDialog(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Database query failed for email: " + email, e);
                    Toast.makeText(this, getString(R.string.database_connection_error_with_msg, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                    showDatabaseError(email, e);
                });
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
            String welcome = getString(R.string.welcome_user, user.getFullName());
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
        String message = getString(R.string.user_processing_error_message, userCount, email);
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.user_processing_error_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.auto_fix), (dialog, which) -> {
                    forceFixUser(email);
                })
                .setNegativeButton(getString(R.string.system_diagnostic), (dialog, which) -> {
                    runDiagnostic();
                })
                .setNeutralButton(getString(R.string.create_new), (dialog, which) -> {
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
                .setTitle(getString(R.string.database_error_title))
                .setMessage(getString(R.string.database_error_message_detailed, error.getMessage()))
                .setPositiveButton(getString(R.string.retry), (dialog, which) -> {
                    loadUserData(email);
                })
                .setNegativeButton(getString(R.string.system_diagnostic), (dialog, which) -> runDiagnostic())
                .show();
    }
    
    /**
     * إصلاح قسري للمستخدم
     */
    private void forceFixUser(String email) {
        Toast.makeText(this, getString(R.string.performing_forced_fix), Toast.LENGTH_SHORT).show();
        
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
                    Toast.makeText(this, getString(R.string.failed_to_fix_with_msg, e.getMessage()), Toast.LENGTH_LONG).show();
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
        Toast.makeText(this, getString(R.string.user_found_but_processing_failed), Toast.LENGTH_LONG).show();
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
                                Toast.makeText(this, getString(R.string.welcome_user_fixed, user.getFullName()), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, getString(R.string.search_user_error_with_msg, e.getMessage()),
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
            
            Toast.makeText(this, getString(R.string.welcome_user, user.getFullName()), Toast.LENGTH_SHORT).show();
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
                .setTitle(getString(R.string.search_error_title))
                .setMessage(getString(R.string.search_error_message, error))
                .setPositiveButton(getString(R.string.retry), (dialog, which) -> {
                    loadUserData(email);
                })
                .setNegativeButton(getString(R.string.system_diagnostic), (dialog, which) -> {
                    runDiagnostic();
                })
                .setNeutralButton(getString(R.string.create_account), (dialog, which) -> {
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
                .setTitle(getString(R.string.unregistered_account_title))
                .setMessage(getString(R.string.unregistered_account_message))
                .setPositiveButton(getString(R.string.create_account), (dialog, which) -> {
                    createNewUserAccount(email);
                })
                .setNeutralButton(getString(R.string.login_as_guest), (dialog, which) -> {
                    createGuestAccount(email);
                })
                .setNegativeButton(getString(R.string.system_admin_short), (dialog, which) -> {
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
                .setTitle(getString(R.string.setup_system_admin_title))
                .setMessage(getString(R.string.setup_system_admin_message))
                .setPositiveButton(getString(R.string.confirm_link), (dialog, which) -> {
                    linkAccountToAdminProfile(email);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
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
                        
                        Toast.makeText(this, getString(R.string.welcome_system_admin, user.getFullName()),
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
                    getString(R.string.system_admin_created_welcome, adminUser.getFullName()),
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
        String guestName = getString(R.string.guest_prefix) + email.split("@")[0];
        
        User guestUser = new User(
            "guest_" + System.currentTimeMillis(), // username فريد
            email,
            guestName,
            UserRole.VIEWER // صلاحيات عرض فقط
        );
        
        // تسجيل دخول مؤقت بدون حفظ في قاعدة البيانات
        userSession.loginUser(guestUser);
        
        Toast.makeText(this, getString(R.string.welcome_guest_temporary, guestName), Toast.LENGTH_LONG).show();
        
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
                .setTitle(getString(R.string.create_new_account_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.create), (dialog, which) -> {
                    String fullName = fullNameEdit.getText().toString().trim();
                    String phone = phoneEdit.getText().toString().trim();
                    
                    if (fullName.isEmpty()) {
                        Toast.makeText(this, getString(R.string.enter_full_name), Toast.LENGTH_SHORT).show();
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
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    mAuth.signOut();
                    dialog.dismiss();
                })
                .show();
    }
    
    /**
     * حفظ المستخدم الجديد في قاعدة البيانات
     */
    private void saveNewUserToDatabase(User user) {
        Toast.makeText(this, getString(R.string.creating_account), Toast.LENGTH_SHORT).show();
        
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    user.setId(documentReference.getId());
                    
                    // تسجيل دخول المستخدم الجديد
                    userSession.loginUser(user);
                    
                    // إشعار المدير بالمستخدم الجديد
                    notifyAdminOfNewUser(user);
                    
                    Toast.makeText(this, getString(R.string.account_created_welcome, user.getFullName()),
                            Toast.LENGTH_LONG).show();
                    startMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating new user account", e);
                    Toast.makeText(this, getString(R.string.error_creating_account, e.getMessage()),
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
            
            Toast.makeText(this, getString(R.string.user_data_fixed_welcome, user.getFullName()),
                    Toast.LENGTH_LONG).show();
            startMainActivity();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to fix corrupted user data", e);
            Toast.makeText(this, getString(R.string.user_data_error_contact_admin),
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
                .setTitle(getString(R.string.convert_to_system_admin_title))
                .setMessage(getString(R.string.convert_to_admin_message, email))
                .setPositiveButton(getString(R.string.yes_create_admin), (dialog, which) -> {
                    createNewAdminProfile(email);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
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
        Toast.makeText(this, getString(R.string.diagnosing), Toast.LENGTH_SHORT).show();
        
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
                Toast.makeText(LoginActivity.this, getString(R.string.diagnostic_error_with_msg, error), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * عرض نتيجة التشخيص
     */
    private void showDiagnosticResult(String diagnosis, String recommendation) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.diagnostic_result_title))
                .setMessage(getString(R.string.diagnostic_result_message, diagnosis, recommendation))
                .setPositiveButton(getString(R.string.auto_fix), (dialog, which) -> {
                    runQuickFix();
                })
                .setNegativeButton(getString(R.string.close), null)
                .setNeutralButton(getString(R.string.firebase_info), (dialog, which) -> {
                    FirebaseAuthDiagnostic.showFirebaseInfo(this);
                })
                .show();
    }
    
    /**
     * تشغيل الإصلاح السريع
     */
    private void runQuickFix() {
        Toast.makeText(this, getString(R.string.fixing), Toast.LENGTH_SHORT).show();
        
        FirebaseAuthDiagnostic.quickFix(this, new FirebaseAuthDiagnostic.DiagnosticCallback() {
            @Override
            public void onResult(String diagnosis, String recommendation) {
                new android.app.AlertDialog.Builder(LoginActivity.this)
                        .setTitle(getString(R.string.fix_result_title))
                        .setMessage(diagnosis + "\n\n" + recommendation)
                        .setPositiveButton(getString(R.string.ok), null)
                        .show();
            }
            
            @Override
            public void onFixed(String message) {
                new android.app.AlertDialog.Builder(LoginActivity.this)
                        .setTitle(getString(R.string.fixed_successfully))
                        .setMessage(message)
                        .setPositiveButton(getString(R.string.ok), null)
                        .show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, getString(R.string.fix_failed_with_msg, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

