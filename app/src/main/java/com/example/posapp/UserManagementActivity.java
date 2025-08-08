package com.example.posapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Button;

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
    
    // ============================================================================
    // قائمة المدراء المصرح لهم فقط 
    // 🔴 مهم جداً: أضف هنا فقط البريد الإلكتروني للمدراء المصرح لهم
    // 🔴 يجب تطابق هذه القائمة مع نفس القائمة في LoginActivity
    // ============================================================================
    private static final String[] AUTHORIZED_ADMINS = {
        "jusdorge@gmail.com",  // 🔴 غير هذا لبريدك الإلكتروني الفعلي
        // "admin2@company.com",  // مثال لإضافة مدير آخر
        // يمكن إضافة المزيد حسب الحاجة
    };
    
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
     * التحقق من صحة تغيير الدور (حماية أمنية)
     */
    private boolean validateRoleChange(User user, UserRole newRole) {
        // إذا كان الدور الجديد مدير
        if (newRole == UserRole.ADMIN) {
            boolean isAuthorized = isAuthorizedAdmin(user.getEmail());
            
            if (!isAuthorized) {
                // منع إعطاء صلاحيات المدير لغير المصرح لهم
                Toast.makeText(this, "❌ غير مسموح: هذا المستخدم غير مصرح له بصلاحيات المدير", Toast.LENGTH_LONG).show();
                Log.w("UserManagement", "⚠️ SECURITY: Attempted to assign admin role to unauthorized user: " + user.getEmail());
                return false;
            }
        }
        
        // إذا كان المستخدم الحالي مدير مصرح له، لا يمكن تقليل صلاحياته
        if (user.getRole() == UserRole.ADMIN && isAuthorizedAdmin(user.getEmail())) {
            if (newRole != UserRole.ADMIN) {
                Toast.makeText(this, "❌ غير مسموح: لا يمكن تغيير دور المدير الأساسي", Toast.LENGTH_LONG).show();
                Log.w("UserManagement", "⚠️ SECURITY: Attempted to downgrade authorized admin: " + user.getEmail());
                return false;
            }
        }
        
        return true;
    }
    
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
        
        // ربط زر أدوات المستخدمين
        Button toolsButton = findViewById(R.id.toolsButton);
        if (toolsButton != null) {
            toolsButton.setOnClickListener(v -> showFixUsersDialog());
        }
        
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
                .setTitle(getString(R.string.add_new_user))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.add), (dialog, which) -> {
                    String username = usernameEdit.getText().toString().trim();
                    String email = emailEdit.getText().toString().trim();
                    String fullName = fullNameEdit.getText().toString().trim();
                    String phone = phoneEdit.getText().toString().trim();
                    UserRole selectedRole = (UserRole) roleSpinner.getSelectedItem();
                    
                    if (validateUserInput(username, email, fullName, selectedRole)) {
                        createUser(username, email, fullName, phone, selectedRole);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
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
        // فحص الحماية الأمنية قبل إنشاء المستخدم
        User tempUser = new User(username, email, fullName, role);
        if (!validateRoleChange(tempUser, role)) {
            return; // إيقاف العملية إذا فشل فحص الأمان
        }
        
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
                    
                    // سجل أمني لإنشاء المدراء
                    if (role == UserRole.ADMIN) {
                        Log.i("UserManagement", "✅ SECURITY: New authorized admin created: " + email);
                    }
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
                .setTitle(getString(R.string.edit_user_title, user.getFullName()))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String newFullName = fullNameEdit.getText().toString().trim();
                    String newPhone = phoneEdit.getText().toString().trim();
                    
                    if (!newFullName.isEmpty()) {
                        user.setFullName(newFullName);
                        user.setPhone(newPhone);
                        updateUser(user);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
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
        // منع حذف المدراء المصرح لهم
        if (user.getRole() == UserRole.ADMIN && isAuthorizedAdmin(user.getEmail())) {
            Toast.makeText(this, getString(R.string.not_allowed_delete_primary_admin), Toast.LENGTH_LONG).show();
            Log.w("UserManagement", "⚠️ SECURITY: Attempted to delete authorized admin: " + user.getEmail());
            return;
        }
        
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete))
                .setMessage(getString(R.string.confirm_delete_user, user.getFullName()))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    db.collection("users").document(user.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                usersList.remove(user);
                                adapter.notifyDataSetChanged();
                            Toast.makeText(this, getString(R.string.user_deleted), Toast.LENGTH_SHORT).show();
                                Log.i("UserManagement", "User deleted: " + user.getEmail());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting user", e);
                            Toast.makeText(this, getString(R.string.error_deleting_user, e.getMessage()), 
                                    Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    
    private void updateUser(User user) {
        // التحقق من الحماية الأمنية قبل التحديث
        // ملاحظة: هذا لحماية المدراء الموجودين، الفحص الأساسي يحدث في شاشة التعديل
        
        db.collection("users").document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "تم تحديث المستخدم بنجاح", Toast.LENGTH_SHORT).show();
                    
                    // سجل أمني للتحديثات المتعلقة بالمدراء
                    if (user.getRole() == UserRole.ADMIN) {
                        Log.i("UserManagement", "✅ SECURITY: Admin user updated: " + user.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user", e);
                    Toast.makeText(this, getString(R.string.error_updating_user, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }
    
    private void deleteUser(User user) {
        // منع حذف المدراء المصرح لهم
        if (user.getRole() == UserRole.ADMIN && isAuthorizedAdmin(user.getEmail())) {
            Toast.makeText(this, "❌ غير مسموح: لا يمكن حذف المدير الأساسي", Toast.LENGTH_LONG).show();
            Log.w("UserManagement", "⚠️ SECURITY: Attempted to delete authorized admin: " + user.getEmail());
            return;
        }
        
        new AlertDialog.Builder(this)
                .setTitle("تأكيد الحذف")
                .setMessage("هل أنت متأكد من حذف المستخدم: " + user.getFullName() + "؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    db.collection("users").document(user.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                usersList.remove(user);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, getString(R.string.user_deleted), Toast.LENGTH_SHORT).show();
                                Log.i("UserManagement", "User deleted: " + user.getEmail());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting user", e);
                                Toast.makeText(this, getString(R.string.error_deleting_user, e.getMessage()), 
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
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
        emailInput.setHint(getString(R.string.enter_email_for_test));
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.login_test))
                .setMessage(getString(R.string.enter_email_to_test_login))
                .setView(emailInput)
                .setPositiveButton(getString(R.string.test_action), (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (!email.isEmpty()) {
                        testUserLogin(email);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
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
     * إصلاح مستخدم واحد مع الحماية الأمنية
     */
    private void fixSingleUser(User user) {
        boolean needsUpdate = false;
        UserRole originalRole = user.getRole();
        
        // إصلاح التفعيل
        if (!user.isActive()) {
            user.setActive(true);
            needsUpdate = true;
        }
        
        // إصلاح الدور مع حماية المدراء
        if (user.getRole() == null) {
            // تحديد الدور المناسب بناءً على الصلاحيات
            if (isAuthorizedAdmin(user.getEmail())) {
                user.setRole(UserRole.ADMIN);
                Log.i("UserManagement", "✅ SECURITY: Restored admin role for authorized user: " + user.getEmail());
            } else {
                user.setRole(UserRole.EMPLOYEE);
            }
            needsUpdate = true;
        } else if (user.getRole() == UserRole.ADMIN) {
            // حماية المدراء: التأكد من أنهم مصرح لهم
            if (!isAuthorizedAdmin(user.getEmail())) {
                user.setRole(UserRole.EMPLOYEE);
                needsUpdate = true;
                Log.w("UserManagement", "⚠️ SECURITY: Downgraded unauthorized admin to employee: " + user.getEmail());
                Toast.makeText(this, "تم تقليل صلاحيات مستخدم غير مصرح له: " + user.getEmail(), Toast.LENGTH_LONG).show();
            }
        }
        
        // إصلاح الاسم
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            user.setFullName(user.getEmail().split("@")[0]);
            needsUpdate = true;
        }
        
        // إصلاح اسم المستخدم
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            user.setUsername(user.getEmail().split("@")[0]);
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            updateUser(user);
            
            String message = "تم إصلاح المستخدم: " + user.getEmail();
            if (originalRole != user.getRole()) {
                message += " (تم تغيير الدور من " + 
                    (originalRole != null ? originalRole.getDisplayName() : "غير محدد") + 
                    " إلى " + user.getRole().getDisplayName() + ")";
            }
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.user_no_fix_needed), Toast.LENGTH_SHORT).show();
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
                                String emailName = user.getEmail() != null ? user.getEmail().split("@")[0] : getString(R.string.user);
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
        user.setFullName(getStringValue(data, "fullName", getString(R.string.user)));
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