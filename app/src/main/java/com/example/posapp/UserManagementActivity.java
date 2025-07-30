package com.example.posapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.posapp.model.Permission;
import com.example.posapp.model.Resource;
import com.example.posapp.model.User;
import com.example.posapp.model.UserRole;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {
    private static final String TAG = "UserManagement";
    
    private ListView usersListView;
    private FloatingActionButton addUserFab;
    private UserManagementAdapter adapter;
    private List<User> usersList;
    private FirebaseFirestore db;
    private UserSession userSession;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        
        // فحص الصلاحيات
        if (!PermissionHelper.checkPermissionWithMessage(this, Resource.USERS, Permission.MANAGE_USERS)) {
            finish();
            return;
        }
        
        initializeViews();
        setupToolbar();
        loadUsers();
    }
    
    private void initializeViews() {
        usersListView = findViewById(R.id.usersListView);
        addUserFab = findViewById(R.id.addUserFab);
        
        db = FirebaseFirestore.getInstance();
        userSession = UserSession.getInstance(this);
        usersList = new ArrayList<>();
        
        adapter = new UserManagementAdapter(this, usersList);
        usersListView.setAdapter(adapter);
        
        // إخفاء زر الإضافة إذا لم تكن هناك صلاحية
        PermissionHelper.setViewVisibilityByPermission(this, addUserFab, Resource.USERS, Permission.CREATE);
        
        addUserFab.setOnClickListener(v -> showAddUserDialog());
        
        usersListView.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = usersList.get(position);
            showUserDetailsDialog(selectedUser);
        });
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("إدارة المستخدمين");
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // إضافة قائمة الأدوات
        toolbar.inflateMenu(R.menu.menu_user_management);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_fix_users) {
                showFixUsersDialog();
                return true;
            }
            return false;
        });
    }
    
    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    usersList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setId(document.getId());
                        usersList.add(user);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users", e);
                    Toast.makeText(this, "خطأ في تحميل المستخدمين: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    private void showAddUserDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        
        EditText usernameEdit = dialogView.findViewById(R.id.usernameEditText);
        EditText emailEdit = dialogView.findViewById(R.id.emailEditText);
        EditText fullNameEdit = dialogView.findViewById(R.id.fullNameEditText);
        EditText phoneEdit = dialogView.findViewById(R.id.phoneEditText);
        
        // Spinner للأدوار
        android.widget.Spinner roleSpinner = dialogView.findViewById(R.id.roleSpinner);
        UserRole[] availableRoles = getAvailableRoles();
        android.widget.ArrayAdapter<UserRole> roleAdapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, availableRoles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);
        
        new AlertDialog.Builder(this)
                .setTitle("إضافة مستخدم جديد")
                .setView(dialogView)
                .setPositiveButton("إضافة", (dialog, which) -> {
                    String username = usernameEdit.getText().toString().trim();
                    String email = emailEdit.getText().toString().trim();
                    String fullName = fullNameEdit.getText().toString().trim();
                    String phone = phoneEdit.getText().toString().trim();
                    UserRole selectedRole = (UserRole) roleSpinner.getSelectedItem();
                    
                    if (validateUserInput(username, email, fullName, selectedRole)) {
                        createUser(username, email, fullName, phone, selectedRole);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
    
    private UserRole[] getAvailableRoles() {
        List<UserRole> availableRoles = new ArrayList<>();
        
        for (UserRole role : UserRole.values()) {
            if (userSession.canAssignRole(role)) {
                availableRoles.add(role);
            }
        }
        
        return availableRoles.toArray(new UserRole[0]);
    }
    
    private boolean validateUserInput(String username, String email, String fullName, UserRole role) {
        if (username.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال اسم المستخدم", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "يرجى إدخال بريد إلكتروني صحيح", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (fullName.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال الاسم الكامل", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (role == null) {
            Toast.makeText(this, "يرجى اختيار دور المستخدم", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void createUser(String username, String email, String fullName, String phone, UserRole role) {
        User newUser = new User(username, email, fullName, role);
        newUser.setPhone(phone);
        newUser.setCreatedBy(userSession.getCurrentUser().getId());
        
        db.collection("users")
                .add(newUser)
                .addOnSuccessListener(documentReference -> {
                    newUser.setId(documentReference.getId());
                    usersList.add(newUser);
                    adapter.notifyDataSetChanged();
                    
                    Toast.makeText(this, "تم إنشاء المستخدم بنجاح", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user", e);
                    Toast.makeText(this, "خطأ في إنشاء المستخدم: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    private void showUserDetailsDialog(User user) {
        if (!userSession.canManageUser(user)) {
            Toast.makeText(this, "ليس لديك صلاحية لإدارة هذا المستخدم", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] options = {"تعديل البيانات", "تغيير الدور", "تفعيل/تعطيل", "حذف المستخدم"};
        
        new AlertDialog.Builder(this)
                .setTitle(user.getFullName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditUserDialog(user);
                            break;
                        case 1:
                            showChangeRoleDialog(user);
                            break;
                        case 2:
                            toggleUserStatus(user);
                            break;
                        case 3:
                            showDeleteUserDialog(user);
                            break;
                    }
                })
                .show();
    }
    
    private void showEditUserDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);
        
        EditText fullNameEdit = dialogView.findViewById(R.id.fullNameEditText);
        EditText phoneEdit = dialogView.findViewById(R.id.phoneEditText);
        
        fullNameEdit.setText(user.getFullName());
        phoneEdit.setText(user.getPhone());
        
        new AlertDialog.Builder(this)
                .setTitle("تعديل " + user.getFullName())
                .setView(dialogView)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String newFullName = fullNameEdit.getText().toString().trim();
                    String newPhone = phoneEdit.getText().toString().trim();
                    
                    if (!newFullName.isEmpty()) {
                        user.setFullName(newFullName);
                        user.setPhone(newPhone);
                        updateUser(user);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
    
    private void showChangeRoleDialog(User user) {
        UserRole[] availableRoles = getAvailableRoles();
        String[] roleNames = new String[availableRoles.length];
        
        for (int i = 0; i < availableRoles.length; i++) {
            roleNames[i] = availableRoles[i].getDisplayName();
        }
        
        new AlertDialog.Builder(this)
                .setTitle("تغيير دور " + user.getFullName())
                .setItems(roleNames, (dialog, which) -> {
                    UserRole newRole = availableRoles[which];
                    if (userSession.canAssignRole(newRole)) {
                        user.setRole(newRole);
                        updateUser(user);
                    } else {
                        Toast.makeText(this, "ليس لديك صلاحية لتعيين هذا الدور", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
    
    private void toggleUserStatus(User user) {
        user.setActive(!user.isActive());
        updateUser(user);
        
        String status = user.isActive() ? "تم تفعيل" : "تم تعطيل";
        Toast.makeText(this, status + " المستخدم", Toast.LENGTH_SHORT).show();
    }
    
    private void showDeleteUserDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("حذف المستخدم")
                .setMessage("هل أنت متأكد من حذف المستخدم: " + user.getFullName() + "؟\n\nهذا الإجراء لا يمكن التراجع عنه.")
                .setPositiveButton("حذف", (dialog, which) -> {
                    deleteUser(user);
                })
                .setNegativeButton("إلغاء", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    private void updateUser(User user) {
        db.collection("users").document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "تم تحديث المستخدم", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user", e);
                    Toast.makeText(this, "خطأ في تحديث المستخدم: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    private void deleteUser(User user) {
        db.collection("users").document(user.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    usersList.remove(user);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "تم حذف المستخدم", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting user", e);
                    Toast.makeText(this, "خطأ في حذف المستخدم: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    /**
     * عرض حوار إصلاح المستخدمين
     */
    private void showFixUsersDialog() {
        String[] options = {
            "إصلاح جميع المستخدمين", 
            "تشخيص سريع", 
            "اختبار تسجيل الدخول"
        };
        
        new AlertDialog.Builder(this)
                .setTitle("أدوات المستخدمين")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showFullFixDialog();
                            break;
                        case 1:
                            performQuickDiagnosis();
                            break;
                        case 2:
                            showLoginTestDialog();
                            break;
                    }
                })
                .show();
    }
    
    private void showFullFixDialog() {
        new AlertDialog.Builder(this)
                .setTitle("إصلاح المستخدمين")
                .setMessage("هذه الوظيفة ستقوم بإصلاح جميع المستخدمين في النظام:\n\n" +
                           "• تفعيل الحسابات غير المفعلة\n" +
                           "• إضافة الأدوار المفقودة\n" +
                           "• إصلاح البيانات التالفة\n\n" +
                           "هل تريد المتابعة؟")
                .setPositiveButton("إصلاح الآن", (dialog, which) -> {
                    fixAllUsers();
                })
                .setNegativeButton("إلغاء", null)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .show();
    }
    
    /**
     * تشخيص سريع للمستخدمين
     */
    private void performQuickDiagnosis() {
        Toast.makeText(this, "جاري التشخيص السريع...", Toast.LENGTH_SHORT).show();
        
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalUsers = queryDocumentSnapshots.size();
                    int activeUsers = 0;
                    int inactiveUsers = 0;
                    int usersWithoutRole = 0;
                    int corruptedUsers = 0;
                    
                    StringBuilder report = new StringBuilder();
                    report.append("📊 تقرير التشخيص السريع:\n\n");
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            User user = document.toObject(User.class);
                            
                            if (user.isActive()) {
                                activeUsers++;
                            } else {
                                inactiveUsers++;
                            }
                            
                            if (user.getRole() == null) {
                                usersWithoutRole++;
                            }
                            
                        } catch (Exception e) {
                            corruptedUsers++;
                        }
                    }
                    
                    report.append("👥 إجمالي المستخدمين: ").append(totalUsers).append("\n");
                    report.append("✅ نشطين: ").append(activeUsers).append("\n");
                    report.append("❌ غير نشطين: ").append(inactiveUsers).append("\n");
                    report.append("⚠️ بدون دور: ").append(usersWithoutRole).append("\n");
                    report.append("🚫 تالفين: ").append(corruptedUsers).append("\n\n");
                    
                    if (inactiveUsers > 0 || usersWithoutRole > 0 || corruptedUsers > 0) {
                        report.append("💡 يُنصح بتشغيل إصلاح المستخدمين.");
                    } else {
                        report.append("✨ جميع المستخدمين في حالة جيدة!");
                    }
                    
                    new AlertDialog.Builder(this)
                            .setTitle("نتيجة التشخيص")
                            .setMessage(report.toString())
                            .setPositiveButton("حسناً", null)
                            .setNeutralButton("إصلاح الآن", (dialog, which) -> fixAllUsers())
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل التشخيص: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    
    /**
     * اختبار تسجيل الدخول
     */
    private void showLoginTestDialog() {
        android.widget.EditText emailInput = new android.widget.EditText(this);
        emailInput.setHint("أدخل البريد الإلكتروني للاختبار");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        new AlertDialog.Builder(this)
                .setTitle("اختبار تسجيل الدخول")
                .setMessage("أدخل البريد الإلكتروني لاختبار إمكانية تسجيل الدخول:")
                .setView(emailInput)
                .setPositiveButton("اختبار", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (!email.isEmpty()) {
                        testUserLogin(email);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
    
    /**
     * اختبار تسجيل دخول مستخدم
     */
    private void testUserLogin(String email) {
        Toast.makeText(this, "جاري اختبار: " + email, Toast.LENGTH_SHORT).show();
        
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showTestResult(email, "❌ فشل", "لم يتم العثور على المستخدم في قاعدة البيانات", null);
                        return;
                    }
                    
                    try {
                        com.google.firebase.firestore.QueryDocumentSnapshot document = 
                            (com.google.firebase.firestore.QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        
                        User user = document.toObject(User.class);
                        user.setId(document.getId());
                        
                        // فحص حالة المستخدم
                        StringBuilder status = new StringBuilder();
                        boolean canLogin = true;
                        
                        status.append("✅ المستخدم موجود\n");
                        status.append("📧 البريد: ").append(user.getEmail()).append("\n");  
                        status.append("👤 الاسم: ").append(user.getFullName() != null ? user.getFullName() : "غير محدد").append("\n");
                        
                        if (user.isActive()) {
                            status.append("✅ الحساب نشط\n");
                        } else {
                            status.append("❌ الحساب غير نشط\n");
                            canLogin = false;
                        }
                        
                        if (user.getRole() != null) {
                            status.append("✅ الدور: ").append(user.getRole().getDisplayName()).append("\n");
                        } else {
                            status.append("⚠️ الدور: غير محدد\n");
                            canLogin = false;
                        }
                        
                        String result = canLogin ? "✅ نجح" : "⚠️ يحتاج إصلاح";
                        showTestResult(email, result, status.toString(), canLogin ? null : user);
                        
                    } catch (Exception e) {
                        showTestResult(email, "❌ فشل", "خطأ في قراءة بيانات المستخدم: " + e.getMessage(), null);
                    }
                })
                .addOnFailureListener(e -> {
                    showTestResult(email, "❌ فشل", "خطأ في البحث: " + e.getMessage(), null);
                });
    }
    
    /**
     * عرض نتيجة الاختبار
     */
    private void showTestResult(String email, String result, String details, User userToFix) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("نتيجة اختبار " + email)
                .setMessage("النتيجة: " + result + "\n\n" + details)
                .setPositiveButton("حسناً", null);
        
        if (userToFix != null) {
            builder.setNeutralButton("إصلاح هذا المستخدم", (dialog, which) -> {
                fixSingleUser(userToFix);
            });
        }
        
        builder.show();
    }
    
    /**
     * إصلاح مستخدم واحد
     */
    private void fixSingleUser(User user) {
        boolean needsUpdate = false;
        
        // إصلاح التفعيل
        if (!user.isActive()) {
            user.setActive(true);
            needsUpdate = true;
        }
        
        // إصلاح الدور
        if (user.getRole() == null) {
            user.setRole(UserRole.EMPLOYEE);
            needsUpdate = true;
        }
        
        // إصلاح الاسم
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            user.setFullName(user.getEmail().split("@")[0]);
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            updateUser(user);
            Toast.makeText(this, "تم إصلاح المستخدم: " + user.getEmail(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "المستخدم لا يحتاج إصلاح", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * إصلاح جميع المستخدمين في النظام
     */
    private void fixAllUsers() {
        // إظهار progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("جاري إصلاح المستخدمين...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();
        
                 db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalUsers = queryDocumentSnapshots.size();
                    final int[] fixedUsers = {0};
                    final int[] processedUsers = {0};
                    
                    progressDialog.setMax(totalUsers);
                    
                    Log.d(TAG, "Starting to fix " + totalUsers + " users");
                    
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // محاولة قراءة المستخدم
                            User user = document.toObject(User.class);
                            user.setId(document.getId());
                            
                            boolean needsUpdate = false;
                            StringBuilder fixLog = new StringBuilder();
                            
                            // فحص وإصلاح البيانات
                            
                            // 1. إصلاح حالة التفعيل
                            if (!user.isActive()) {
                                user.setActive(true);
                                needsUpdate = true;
                                fixLog.append("تم تفعيل الحساب، ");
                            }
                            
                            // 2. إصلاح الدور المفقود
                            if (user.getRole() == null) {
                                user.setRole(UserRole.EMPLOYEE); // دور افتراضي
                                needsUpdate = true;
                                fixLog.append("تم إضافة دور افتراضي، ");
                            }
                            
                            // 3. إصلاح التوقيتات المفقودة
                            if (user.getCreatedAt() == null) {
                                user.setCreatedAt(com.google.firebase.Timestamp.now());
                                needsUpdate = true;
                                fixLog.append("تم إضافة تاريخ الإنشاء، ");
                            }
                            
                            // 4. إصلاح الاسم المفقود
                            if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
                                String emailName = user.getEmail() != null ? user.getEmail().split("@")[0] : "مستخدم";
                                user.setFullName(emailName);
                                needsUpdate = true;
                                fixLog.append("تم إضافة اسم افتراضي، ");
                            }
                            
                            // 5. إصلاح اسم المستخدم المفقود
                            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                                String emailName = user.getEmail() != null ? user.getEmail().split("@")[0] : "user";
                                user.setUsername(emailName);
                                needsUpdate = true;
                                fixLog.append("تم إضافة اسم مستخدم افتراضي، ");
                            }
                            
                            if (needsUpdate) {
                                fixedUsers[0]++;
                                Log.d(TAG, "Fixing user: " + user.getEmail() + " - " + fixLog.toString());
                                
                                // تحديث المستخدم في قاعدة البيانات
                                db.collection("users").document(user.getId())
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Successfully fixed user: " + user.getEmail());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to fix user: " + user.getEmail(), e);
                                        });
                                
                                // تحديث المستخدم في القائمة المحلية
                                for (int i = 0; i < usersList.size(); i++) {
                                    if (usersList.get(i).getId().equals(user.getId())) {
                                        usersList.set(i, user);
                                        break;
                                    }
                                }
                            }
                            
                            processedUsers[0]++;
                            progressDialog.setProgress(processedUsers[0]);
                            
                            // إذا انتهينا من معالجة جميع المستخدمين
                            if (processedUsers[0] == totalUsers) {
                                progressDialog.dismiss();
                                
                                // تحديث واجهة المستخدم
                                runOnUiThread(() -> {
                                    adapter.notifyDataSetChanged();
                                    
                                    String message = "تم إصلاح " + fixedUsers[0] + " من أصل " + totalUsers + " مستخدم";
                                    Toast.makeText(UserManagementActivity.this, message, Toast.LENGTH_LONG).show();
                                    
                                    // عرض تقرير مفصل
                                    showFixReport(totalUsers, fixedUsers[0]);
                                });
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user document: " + document.getId(), e);
                            
                            // محاولة إصلاح البيانات التالفة
                            try {
                                fixCorruptedUser(document);
                                fixedUsers[0]++;
                            } catch (Exception fixError) {
                                Log.e(TAG, "Failed to fix corrupted user: " + document.getId(), fixError);
                            }
                            
                            processedUsers[0]++;
                            progressDialog.setProgress(processedUsers[0]);
                            
                            if (processedUsers[0] == totalUsers) {
                                progressDialog.dismiss();
                                runOnUiThread(() -> {
                                    adapter.notifyDataSetChanged();
                                    String message = "تم إصلاح " + fixedUsers[0] + " من أصل " + totalUsers + " مستخدم";
                                    Toast.makeText(UserManagementActivity.this, message, Toast.LENGTH_LONG).show();
                                    showFixReport(totalUsers, fixedUsers[0]);
                                });
                            }
                        }
                    }
                    
                    if (totalUsers == 0) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "لا يوجد مستخدمين للإصلاح", Toast.LENGTH_SHORT).show();
                    }
                    
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error loading users for fix", e);
                    Toast.makeText(this, "خطأ في تحميل المستخدمين: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    /**
     * إصلاح مستخدم تالف
     */
    private void fixCorruptedUser(com.google.firebase.firestore.QueryDocumentSnapshot document) throws Exception {
        java.util.Map<String, Object> data = document.getData();
        
        // إنشاء مستخدم جديد بالبيانات الصحيحة
        User user = new User();
        user.setId(document.getId());
        
        // استخراج البيانات الأساسية بأمان
        user.setEmail(getStringValue(data, "email", "unknown@example.com"));
        user.setFullName(getStringValue(data, "fullName", "مستخدم"));
        user.setUsername(getStringValue(data, "username", user.getEmail().split("@")[0]));
        user.setPhone(getStringValue(data, "phone", ""));
        user.setActive(getBooleanValue(data, "isActive", true));
        
        // تحويل الدور
        String roleString = getStringValue(data, "role", "EMPLOYEE");
        try {
            user.setRole(UserRole.valueOf(roleString.toUpperCase()));
        } catch (Exception e) {
            user.setRole(UserRole.EMPLOYEE); // دور افتراضي
        }
        
        // التوقيتات
        if (data.containsKey("createdAt") && data.get("createdAt") instanceof com.google.firebase.Timestamp) {
            user.setCreatedAt((com.google.firebase.Timestamp) data.get("createdAt"));
        } else {
            user.setCreatedAt(com.google.firebase.Timestamp.now());
        }
        
        if (data.containsKey("lastLogin") && data.get("lastLogin") instanceof com.google.firebase.Timestamp) {
            user.setLastLogin((com.google.firebase.Timestamp) data.get("lastLogin"));
        }
        
        // حفظ البيانات المُصلحة
        db.collection("users").document(user.getId()).set(user);
        
        Log.d(TAG, "Fixed corrupted user: " + user.getEmail());
    }
    
    private String getStringValue(java.util.Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private boolean getBooleanValue(java.util.Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * عرض تقرير الإصلاح
     */
    private void showFixReport(int totalUsers, int fixedUsers) {
        String report = "تقرير إصلاح المستخدمين:\n\n" +
                       "• إجمالي المستخدمين: " + totalUsers + "\n" +
                       "• تم إصلاحهم: " + fixedUsers + "\n" +
                       "• سليمين: " + (totalUsers - fixedUsers) + "\n\n" +
                       "يمكن للمستخدمين الآن تسجيل الدخول بنجاح.";
        
        new AlertDialog.Builder(this)
                .setTitle("تم الإصلاح!")
                .setMessage(report)
                .setPositiveButton("حسناً", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
} 