package com.example.posapp.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * نموذج المستخدم في النظام
 */
public class User implements Serializable {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp lastLogin;
    private String createdBy;
    
    // صلاحيات مخصصة للمستخدم (تتجاوز الصلاحيات الافتراضية للدور)
    private Map<String, Set<String>> customPermissions;
    
    // Empty constructor needed for Firestore
    public User() {
        this.customPermissions = new HashMap<>();
    }
    
    public User(String username, String email, String fullName, UserRole role) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
        this.createdAt = Timestamp.now();
        this.customPermissions = new HashMap<>();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public Map<String, Set<String>> getCustomPermissions() { return customPermissions; }
    public void setCustomPermissions(Map<String, Set<String>> customPermissions) { 
        this.customPermissions = customPermissions != null ? customPermissions : new HashMap<>(); 
    }
    
    // طرق مساعدة للصلاحيات المخصصة
    
    /**
     * إضافة صلاحية مخصصة للمستخدم
     */
    public void addCustomPermission(Resource resource, Permission permission) {
        if (customPermissions == null) {
            customPermissions = new HashMap<>();
        }
        
        Set<String> resourcePermissions = customPermissions.computeIfAbsent(
            resource.getResourceKey(), k -> new HashSet<>()
        );
        resourcePermissions.add(permission.name());
    }
    
    /**
     * إزالة صلاحية مخصصة من المستخدم
     */
    public void removeCustomPermission(Resource resource, Permission permission) {
        if (customPermissions != null && customPermissions.containsKey(resource.getResourceKey())) {
            Set<String> resourcePermissions = customPermissions.get(resource.getResourceKey());
            if (resourcePermissions != null) {
                resourcePermissions.remove(permission.name());
                if (resourcePermissions.isEmpty()) {
                    customPermissions.remove(resource.getResourceKey());
                }
            }
        }
    }
    
    /**
     * فحص ما إذا كان المستخدم لديه صلاحية مخصصة
     */
    public boolean hasCustomPermission(Resource resource, Permission permission) {
        if (customPermissions == null) return false;
        
        Set<String> resourcePermissions = customPermissions.get(resource.getResourceKey());
        return resourcePermissions != null && resourcePermissions.contains(permission.name());
    }
    
    /**
     * تحديث آخر تسجيل دخول
     */
    public void updateLastLogin() {
        this.lastLogin = Timestamp.now();
    }
    
    /**
     * فحص ما إذا كان المستخدم مدير نظام
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    /**
     * فحص ما إذا كان المستخدم مدير أو أعلى
     */
    public boolean isManagerOrAbove() {
        return role != null && role.hasAuthorityOf(UserRole.MANAGER);
    }
    
    @Override
    public String toString() {
        return fullName + " (" + (role != null ? role.getDisplayName() : "غير محدد") + ")";
    }
} 