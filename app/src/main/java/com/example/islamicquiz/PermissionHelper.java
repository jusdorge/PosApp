package com.example.islamicquiz;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.example.islamicquiz.model.Permission;
import com.example.islamicquiz.model.Resource;

/**
 * فئة مساعدة للتحقق من الصلاحيات في الواجهات
 */
public class PermissionHelper {
    
    /**
     * فحص الصلاحية وإظهار رسالة خطأ إذا لم تكن متوفرة
     */
    public static boolean checkPermissionWithMessage(Context context, Resource resource, Permission permission) {
        UserSession userSession = UserSession.getInstance(context);
        
        if (!userSession.isLoggedIn()) {
            // تجنب عرض رسائل خطأ أثناء تحميل البيانات
            return false;
        }
        
        if (!userSession.hasPermission(resource, permission)) {
            showPermissionDeniedDialog(context, 
                "ليس لديك صلاحية " + permission.getDisplayName() + " على " + resource.getDisplayName());
            return false;
        }
        
        return true;
    }
    
    /**
     * فحص أي من الصلاحيات المحددة
     */
    public static boolean checkAnyPermissionWithMessage(Context context, Resource resource, Permission... permissions) {
        UserSession userSession = UserSession.getInstance(context);
        
        if (!userSession.isLoggedIn()) {
            showPermissionDeniedDialog(context, "يجب تسجيل الدخول أولاً");
            return false;
        }
        
        if (!userSession.hasAnyPermission(resource, permissions)) {
            StringBuilder permissionNames = new StringBuilder();
            for (int i = 0; i < permissions.length; i++) {
                permissionNames.append(permissions[i].getDisplayName());
                if (i < permissions.length - 1) {
                    permissionNames.append(" أو ");
                }
            }
            
            showPermissionDeniedDialog(context, 
                "ليس لديك صلاحية " + permissionNames.toString() + " على " + resource.getDisplayName());
            return false;
        }
        
        return true;
    }
    
    /**
     * فحص صامت للصلاحية (بدون رسالة خطأ)
     */
    public static boolean hasPermission(Context context, Resource resource, Permission permission) {
        UserSession userSession = UserSession.getInstance(context);
        return userSession.isLoggedIn() && userSession.validateSession() && 
               userSession.hasPermission(resource, permission);
    }
    
    /**
     * فحص صامت لأي من الصلاحيات
     */
    public static boolean hasAnyPermission(Context context, Resource resource, Permission... permissions) {
        UserSession userSession = UserSession.getInstance(context);
        return userSession.isLoggedIn() && userSession.validateSession() && 
               userSession.hasAnyPermission(resource, permissions);
    }
    
    /**
     * إخفاء أو إظهار عنصر واجهة حسب الصلاحية
     */
    public static void setViewVisibilityByPermission(Context context, View view, Resource resource, Permission permission) {
        if (hasPermission(context, resource, permission)) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
    
    /**
     * تفعيل أو تعطيل عنصر واجهة حسب الصلاحية
     */
    public static void setViewEnabledByPermission(Context context, View view, Resource resource, Permission permission) {
        view.setEnabled(hasPermission(context, resource, permission));
    }
    
    /**
     * إخفاء أو إظهار عنصر واجهة حسب أي من الصلاحيات
     */
    public static void setViewVisibilityByAnyPermission(Context context, View view, Resource resource, Permission... permissions) {
        if (hasAnyPermission(context, resource, permissions)) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }
    
    /**
     * فحص ما إذا كان المستخدم مدير أو أعلى
     */
    public static boolean isManagerOrAbove(Context context) {
        UserSession userSession = UserSession.getInstance(context);
        return userSession.isLoggedIn() && userSession.isManagerOrAbove();
    }
    
    /**
     * فحص ما إذا كان المستخدم مدير نظام
     */
    public static boolean isAdmin(Context context) {
        UserSession userSession = UserSession.getInstance(context);
        return userSession.isLoggedIn() && userSession.isAdmin();
    }
    
    /**
     * عرض حوار رفض الصلاحية
     */
    private static void showPermissionDeniedDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setTitle("صلاحية مرفوضة")
                .setMessage(message)
                .setPositiveButton("حسناً", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    /**
     * عرض رسالة Toast للصلاحية المرفوضة
     */
    public static void showPermissionDeniedToast(Context context, String message) {
        Toast.makeText(context, "صلاحية مرفوضة: " + message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * تنفيذ إجراء مع فحص الصلاحية
     */
    public static void executeWithPermission(Context context, Resource resource, Permission permission, Runnable action) {
        if (checkPermissionWithMessage(context, resource, permission)) {
            action.run();
        }
    }
    
    /**
     * تنفيذ إجراء مع فحص أي من الصلاحيات
     */
    public static void executeWithAnyPermission(Context context, Resource resource, Runnable action, Permission... permissions) {
        if (checkAnyPermissionWithMessage(context, resource, permissions)) {
            action.run();
        }
    }
} 