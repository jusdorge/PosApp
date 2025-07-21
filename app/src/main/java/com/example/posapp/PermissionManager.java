package com.example.posapp;

import com.example.posapp.model.Permission;
import com.example.posapp.model.Resource;
import com.example.posapp.model.User;
import com.example.posapp.model.UserRole;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * مدير الصلاحيات - يتحكم في صلاحيات المستخدمين ويفحص الأذونات
 */
public class PermissionManager {
    
    private static PermissionManager instance;
    private Map<UserRole, Map<Resource, Set<Permission>>> rolePermissions;
    
    private PermissionManager() {
        initializeDefaultPermissions();
    }
    
    public static synchronized PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }
    
    /**
     * تهيئة الصلاحيات الافتراضية لكل دور
     */
    private void initializeDefaultPermissions() {
        rolePermissions = new HashMap<>();
        
        // صلاحيات مدير النظام - كاملة على جميع الموارد
        Map<Resource, Set<Permission>> adminPermissions = new HashMap<>();
        for (Resource resource : Resource.values()) {
            adminPermissions.put(resource, new HashSet<>(Arrays.asList(Permission.values())));
        }
        rolePermissions.put(UserRole.ADMIN, adminPermissions);
        
        // صلاحيات المدير - واسعة مع بعض القيود
        Map<Resource, Set<Permission>> managerPermissions = new HashMap<>();
        
        // العملاء - كل الصلاحيات
        managerPermissions.put(Resource.CUSTOMERS, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.CREATE, Permission.UPDATE, Permission.DELETE, Permission.EXPORT
        )));
        
        // المنتجات - كل الصلاحيات
        managerPermissions.put(Resource.PRODUCTS, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.CREATE, Permission.UPDATE, Permission.DELETE, Permission.EXPORT, Permission.IMPORT
        )));
        
        // الفواتير - كل الصلاحيات
        managerPermissions.put(Resource.INVOICES, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.CREATE, Permission.UPDATE, Permission.DELETE, Permission.EXPORT
        )));
        
        // التقارير - قراءة وتقارير متقدمة
        managerPermissions.put(Resource.REPORTS, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.ADVANCED_REPORTS, Permission.EXPORT
        )));
        
        // المخزون - كل الصلاحيات
        managerPermissions.put(Resource.INVENTORY, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.CREATE, Permission.UPDATE, Permission.EXPORT
        )));
        
        // النسخ الاحتياطي - قراءة وتصدير
        managerPermissions.put(Resource.BACKUP, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.EXPORT, Permission.IMPORT
        )));
        
        // الإعدادات - قراءة وتحديث
        managerPermissions.put(Resource.SETTINGS, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.UPDATE
        )));
        
        // المستخدمين - قراءة فقط (لا يمكن إدارة المستخدمين)
        managerPermissions.put(Resource.USERS, new HashSet<>(Arrays.asList(
            Permission.READ
        )));
        
        rolePermissions.put(UserRole.MANAGER, managerPermissions);
        
        // صلاحيات الموظف - محدودة للعمليات اليومية
        Map<Resource, Set<Permission>> employeePermissions = new HashMap<>();
        
        // العملاء - قراءة وإنشاء وتحديث
        employeePermissions.put(Resource.CUSTOMERS, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.CREATE, Permission.UPDATE
        )));
        
        // المنتجات - قراءة وتحديث
        employeePermissions.put(Resource.PRODUCTS, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.UPDATE
        )));
        
        // الفواتير - كل الصلاحيات ما عدا الحذف
        employeePermissions.put(Resource.INVOICES, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.CREATE, Permission.UPDATE
        )));
        
        // التقارير - قراءة أساسية
        employeePermissions.put(Resource.REPORTS, new HashSet<>(Arrays.asList(
            Permission.READ
        )));
        
        // المخزون - قراءة وتحديث
        employeePermissions.put(Resource.INVENTORY, new HashSet<>(Arrays.asList(
            Permission.READ, Permission.UPDATE
        )));
        
        // باقي الموارد - قراءة فقط
        employeePermissions.put(Resource.BACKUP, new HashSet<>(Arrays.asList(Permission.READ)));
        employeePermissions.put(Resource.SETTINGS, new HashSet<>(Arrays.asList(Permission.READ)));
        employeePermissions.put(Resource.USERS, new HashSet<>(Arrays.asList(Permission.READ)));
        
        rolePermissions.put(UserRole.EMPLOYEE, employeePermissions);
        
        // صلاحيات المستخدم العادي - نفس صلاحيات الموظف (للتوافق مع البيانات القديمة)
        rolePermissions.put(UserRole.USER, employeePermissions);
        
        // صلاحيات العارض - قراءة فقط
        Map<Resource, Set<Permission>> viewerPermissions = new HashMap<>();
        for (Resource resource : Resource.values()) {
            if (resource != Resource.USERS && resource != Resource.SETTINGS) {
                viewerPermissions.put(resource, new HashSet<>(Arrays.asList(Permission.READ)));
            }
        }
        
        rolePermissions.put(UserRole.VIEWER, viewerPermissions);
    }
    
    /**
     * فحص ما إذا كان المستخدم له صلاحية معينة على مورد محدد
     */
    public boolean hasPermission(User user, Resource resource, Permission permission) {
        if (user == null || !user.isActive()) {
            return false;
        }
        
        // مدير النظام له صلاحيات كاملة
        if (user.isAdmin()) {
            return true;
        }
        
        // فحص الصلاحيات المخصصة أولاً
        if (user.hasCustomPermission(resource, permission)) {
            return true;
        }
        
        // فحص الصلاحيات الافتراضية للدور
        UserRole role = user.getRole();
        if (role != null && rolePermissions.containsKey(role)) {
            Map<Resource, Set<Permission>> resourcePermissions = rolePermissions.get(role);
            if (resourcePermissions.containsKey(resource)) {
                return resourcePermissions.get(resource).contains(permission);
            }
        }
        
        return false;
    }
    
    /**
     * فحص ما إذا كان المستخدم له أي من الصلاحيات المحددة
     */
    public boolean hasAnyPermission(User user, Resource resource, Permission... permissions) {
        for (Permission permission : permissions) {
            if (hasPermission(user, resource, permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * فحص ما إذا كان المستخدم له جميع الصلاحيات المحددة
     */
    public boolean hasAllPermissions(User user, Resource resource, Permission... permissions) {
        for (Permission permission : permissions) {
            if (!hasPermission(user, resource, permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * الحصول على جميع الصلاحيات للمستخدم على مورد محدد
     */
    public Set<Permission> getUserPermissions(User user, Resource resource) {
        Set<Permission> permissions = new HashSet<>();
        
        if (user == null || !user.isActive()) {
            return permissions;
        }
        
        // مدير النظام له صلاحيات كاملة
        if (user.isAdmin()) {
            permissions.addAll(Arrays.asList(Permission.values()));
            return permissions;
        }
        
        // إضافة الصلاحيات الافتراضية للدور
        UserRole role = user.getRole();
        if (role != null && rolePermissions.containsKey(role)) {
            Map<Resource, Set<Permission>> resourcePermissions = rolePermissions.get(role);
            if (resourcePermissions.containsKey(resource)) {
                permissions.addAll(resourcePermissions.get(resource));
            }
        }
        
        // إضافة الصلاحيات المخصصة
        if (user.getCustomPermissions() != null && 
            user.getCustomPermissions().containsKey(resource.getResourceKey())) {
            Set<String> customPerms = user.getCustomPermissions().get(resource.getResourceKey());
            for (String permName : customPerms) {
                try {
                    permissions.add(Permission.valueOf(permName));
                } catch (IllegalArgumentException e) {
                    // تجاهل الصلاحيات غير المعروفة
                }
            }
        }
        
        return permissions;
    }
    
    /**
     * الحصول على الصلاحيات الافتراضية لدور معين
     */
    public Map<Resource, Set<Permission>> getDefaultRolePermissions(UserRole role) {
        return rolePermissions.getOrDefault(role, new HashMap<>());
    }
    
    /**
     * فحص ما إذا كان يمكن للمستخدم إدارة مستخدم آخر
     */
    public boolean canManageUser(User currentUser, User targetUser) {
        if (currentUser == null || targetUser == null || !currentUser.isActive()) {
            return false;
        }
        
        // مدير النظام يمكنه إدارة أي مستخدم
        if (currentUser.isAdmin()) {
            return true;
        }
        
        // فحص صلاحية إدارة المستخدمين
        if (!hasPermission(currentUser, Resource.USERS, Permission.MANAGE_USERS)) {
            return false;
        }
        
        // لا يمكن إدارة مستخدم بدور أعلى أو مساوي
        return currentUser.getRole().hasHigherAuthorityThan(targetUser.getRole());
    }
    
    /**
     * فحص ما إذا كان يمكن للمستخدم تغيير دور مستخدم آخر إلى دور محدد
     */
    public boolean canAssignRole(User currentUser, UserRole targetRole) {
        if (currentUser == null || !currentUser.isActive()) {
            return false;
        }
        
        // مدير النظام يمكنه تعيين أي دور ما عدا مدير نظام آخر
        if (currentUser.isAdmin()) {
            return targetRole != UserRole.ADMIN;
        }
        
        // المستخدمون الآخرون يمكنهم تعيين أدوار أقل من دورهم فقط
        return currentUser.getRole().hasHigherAuthorityThan(targetRole);
    }
} 