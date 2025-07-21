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
} 