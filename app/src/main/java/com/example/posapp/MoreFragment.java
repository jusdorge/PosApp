package com.example.posapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.posapp.model.Customer;
import com.example.posapp.model.Permission;
import com.example.posapp.model.Resource;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class MoreFragment extends Fragment {
    private static final int QR_SCANNER_REQUEST_CODE = 1002;
    private static final int FILE_PICKER_REQUEST_CODE = 1003;
    
    private CardView customersManagementCard;
    private CardView productsManagementCard;
    private CardView allInvoicesCard;
    private CardView qrScannerCard;
    private CardView exportDataCard;
    private CardView importDataCard;
    private CardView userManagementCard;
    private CardView notificationsCard;
    private TextView notificationBadge;
    private CardView bmpGalleryCard;
    private CardView operationLogsCard;
    private CardView languageSelectionCard;
    private CardView logoutCard;
    
    private FirebaseFirestore db;
    private CSVExportImportHelper csvHelper;
    private String currentImportType; // لتتبع نوع الاستيراد الحالي
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);
        
        db = FirebaseFirestore.getInstance();
        csvHelper = new CSVExportImportHelper(getContext());
        
        customersManagementCard = view.findViewById(R.id.customersManagementCard);
        productsManagementCard = view.findViewById(R.id.productsManagementCard);
        allInvoicesCard = view.findViewById(R.id.allInvoicesCard);
        qrScannerCard = view.findViewById(R.id.qrScannerCard);
        exportDataCard = view.findViewById(R.id.exportDataCard);
        importDataCard = view.findViewById(R.id.importDataCard);
        userManagementCard = view.findViewById(R.id.userManagementCard);
        notificationsCard = view.findViewById(R.id.notificationsCard);
        notificationBadge = view.findViewById(R.id.notificationBadge);
        bmpGalleryCard = view.findViewById(R.id.bmpGalleryCard);
        operationLogsCard = view.findViewById(R.id.operationLogsCard);
        languageSelectionCard = view.findViewById(R.id.languageSelectionCard);
        logoutCard = view.findViewById(R.id.logoutCard);
        
        // إخفاء جميع الكارتات افتراضياً حتى يتم فحص الصلاحيات
        hideAllCards();
        
        setupClickListeners();
        
        // تأخير قصير لضمان اكتمال تحميل بيانات المستخدم
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                setupCardVisibilityByPermissions();
            }
        }, 500);
        
        return view;
    }
    
    private void setupClickListeners() {
        customersManagementCard.setOnClickListener(v -> {
            PermissionHelper.executeWithPermission(getContext(), Resource.CUSTOMERS, Permission.READ, 
                this::navigateToCustomersManagement);
        });
        
        productsManagementCard.setOnClickListener(v -> {
            PermissionHelper.executeWithPermission(getContext(), Resource.PRODUCTS, Permission.READ, 
                this::navigateToProductsManagement);
        });
        
        allInvoicesCard.setOnClickListener(v -> {
            PermissionHelper.executeWithPermission(getContext(), Resource.INVOICES, Permission.READ, 
                this::openAllInvoicesActivity);
        });
        
        qrScannerCard.setOnClickListener(v -> {
            PermissionHelper.executeWithAnyPermission(getContext(), Resource.CUSTOMERS, 
                this::openQRScanner, Permission.READ, Permission.CREATE);
        });
        
        exportDataCard.setOnClickListener(v -> {
            PermissionHelper.executeWithPermission(getContext(), Resource.BACKUP, Permission.EXPORT, 
                this::showExportOptionsDialog);
        });
        
        importDataCard.setOnClickListener(v -> {
            PermissionHelper.executeWithPermission(getContext(), Resource.BACKUP, Permission.IMPORT, 
                this::showImportOptionsDialog);
        });
        
        userManagementCard.setOnClickListener(v -> {
            PermissionHelper.executeWithPermission(getContext(), Resource.USERS, Permission.MANAGE_USERS, 
                this::openUserManagement);
        });
        
        notificationsCard.setOnClickListener(v -> {
            PermissionHelper.executeWithAnyPermission(getContext(), Resource.USERS, 
                this::openNotifications, Permission.MANAGE_USERS, Permission.READ);
        });

        logoutCard.setOnClickListener(v -> {
            performLogout();
        });

        // معرض الفواتير BMP
        if (bmpGalleryCard != null) {
            bmpGalleryCard.setOnClickListener(v -> openBMPGallery());
        }
        
        // أرشيف العمليات
        if (operationLogsCard != null) {
            operationLogsCard.setOnClickListener(v -> openOperationLogs());
        }
        
        // اختيار اللغة
        if (languageSelectionCard != null) {
            languageSelectionCard.setOnClickListener(v -> openLanguageSelection());
        }
        
        // إدارة المستخدمين
        if (userManagementCard != null) {
        }
    }
    
    private void setupCardVisibilityByPermissions() {
        // استخدام فحص صامت لتجنب رسائل الخطأ أثناء التحميل
        if (PermissionHelper.hasPermission(getContext(), Resource.CUSTOMERS, Permission.READ)) {
            customersManagementCard.setVisibility(View.VISIBLE);
        } else {
            customersManagementCard.setVisibility(View.GONE);
        }
        
        if (PermissionHelper.hasPermission(getContext(), Resource.PRODUCTS, Permission.READ)) {
            productsManagementCard.setVisibility(View.VISIBLE);
        } else {
            productsManagementCard.setVisibility(View.GONE);
        }
        
        if (PermissionHelper.hasPermission(getContext(), Resource.INVOICES, Permission.READ)) {
            allInvoicesCard.setVisibility(View.VISIBLE);
        } else {
            allInvoicesCard.setVisibility(View.GONE);
        }
        
        if (PermissionHelper.hasAnyPermission(getContext(), Resource.CUSTOMERS, Permission.READ, Permission.CREATE)) {
            qrScannerCard.setVisibility(View.VISIBLE);
        } else {
            qrScannerCard.setVisibility(View.GONE);
        }
        
        if (PermissionHelper.hasPermission(getContext(), Resource.BACKUP, Permission.EXPORT)) {
            exportDataCard.setVisibility(View.VISIBLE);
        } else {
            exportDataCard.setVisibility(View.GONE);
        }
        
        if (PermissionHelper.hasPermission(getContext(), Resource.BACKUP, Permission.IMPORT)) {
            importDataCard.setVisibility(View.VISIBLE);
        } else {
            importDataCard.setVisibility(View.GONE);
        }
        
        if (PermissionHelper.hasPermission(getContext(), Resource.USERS, Permission.MANAGE_USERS)) {
            userManagementCard.setVisibility(View.VISIBLE);
        } else {
            userManagementCard.setVisibility(View.GONE);
        }
        
        // عرض كارت الإشعارات للمديرين والموظفين
        if (PermissionHelper.hasAnyPermission(getContext(), Resource.USERS, Permission.MANAGE_USERS, Permission.READ)) {
            notificationsCard.setVisibility(View.VISIBLE);
            updateNotificationBadge();
        } else {
            notificationsCard.setVisibility(View.GONE);
        }
        
        // عرض كارت تسجيل الخروج للجميع (إذا كانوا مسجلين دخول)
        UserSession userSession = UserSession.getInstance(getContext());
        if (userSession != null && userSession.isLoggedIn()) {
            logoutCard.setVisibility(View.VISIBLE);
        } else {
            logoutCard.setVisibility(View.GONE);
        }

        // معرض الفواتير BMP - متاح للجميع (لعرض الفواتير فقط)
        if (bmpGalleryCard != null) {
            bmpGalleryCard.setVisibility(View.VISIBLE);
        }
        
        // أرشيف العمليات - متاح لجميع المستخدمين المسجلين
        if (operationLogsCard != null && userSession != null && userSession.isLoggedIn()) {
            operationLogsCard.setVisibility(View.VISIBLE);
        } else if (operationLogsCard != null) {
            operationLogsCard.setVisibility(View.GONE);
        }
        
        // اختيار اللغة - متاح للجميع
        if (languageSelectionCard != null) {
            languageSelectionCard.setVisibility(View.VISIBLE);
        }
        
        // إدارة المستخدمين - للمديرين فقط
        if (userManagementCard != null) {
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // تحديث رؤية الكارتات عند العودة للواجهة (بعد تسجيل الدخول مثلاً)
        refreshCardVisibility();
        
        // تحديث شارة الإشعارات عند عودة الشاشة
        updateNotificationBadge();
        
        // عرض رسالة تحذيرية للمستخدمين الضيوف
        showGuestWarningIfNeeded();
    }
    
    /**
     * عرض رسالة تحذيرية للمستخدمين الضيوف
     */
    private void showGuestWarningIfNeeded() {
        UserSession userSession = UserSession.getInstance(getContext());
        if (userSession.isGuestUser() && getContext() != null) {
            // عرض Toast للضيوف مرة واحدة فقط
            String prefKey = "guest_warning_shown_" + userSession.getCurrentUser().getUsername();
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("guest_prefs", android.content.Context.MODE_PRIVATE);
            
            if (!prefs.getBoolean(prefKey, false)) {
                Toast.makeText(getContext(), getString(R.string.guest_user_warning), Toast.LENGTH_LONG).show();
                
                // تحديد أنه تم عرض التحذير
                prefs.edit().putBoolean(prefKey, true).apply();
            }
        }
    }
    
    /**
     * تحديث رؤية الكارتات حسب الصلاحيات الحالية
     */
    public void refreshCardVisibility() {
        if (isAdded() && getContext() != null) {
            setupCardVisibilityByPermissions();
        }
    }
    
    /**
     * إخفاء جميع الكارتات (حالة افتراضية قبل فحص الصلاحيات)
     */
    private void hideAllCards() {
        if (customersManagementCard != null) customersManagementCard.setVisibility(View.GONE);
        if (productsManagementCard != null) productsManagementCard.setVisibility(View.GONE);
        if (allInvoicesCard != null) allInvoicesCard.setVisibility(View.GONE);
        if (qrScannerCard != null) qrScannerCard.setVisibility(View.GONE);
        if (exportDataCard != null) exportDataCard.setVisibility(View.GONE);
        if (importDataCard != null) importDataCard.setVisibility(View.GONE);
        if (userManagementCard != null) userManagementCard.setVisibility(View.GONE);
        if (notificationsCard != null) notificationsCard.setVisibility(View.GONE);
        if (bmpGalleryCard != null) bmpGalleryCard.setVisibility(View.GONE);
        if (operationLogsCard != null) operationLogsCard.setVisibility(View.GONE);
        if (languageSelectionCard != null) languageSelectionCard.setVisibility(View.GONE);
        if (logoutCard != null) logoutCard.setVisibility(View.GONE);
    }
    
    private void showExportOptionsDialog() {
        String[] options = {
            getString(R.string.export_all),
            getString(R.string.export_customers),
            getString(R.string.export_products),
            getString(R.string.export_invoices)
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.excel_export))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportAllData();
                            break;
                        case 1:
                            exportCustomers();
                            break;
                        case 2:
                            exportProducts();
                            break;
                        case 3:
                            exportInvoices();
                            break;
                    }
                })
                .show();
    }
    
    private void showImportOptionsDialog() {
        String[] options = {
            getString(R.string.import_customers),
            getString(R.string.import_products),
            "إنشاء قالب العملاء",
            "إنشاء قالب المنتجات"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.excel_import))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentImportType = "customers";
                            openFilePicker();
                            break;
                        case 1:
                            currentImportType = "products";
                            openFilePicker();
                            break;
                        case 2:
                            createTemplate("customers");
                            break;
                        case 3:
                            createTemplate("products");
                            break;
                    }
                })
                .show();
    }
    
    private void exportAllData() {
        Toast.makeText(getContext(), getString(R.string.export_in_progress), Toast.LENGTH_SHORT).show();
        
        csvHelper.exportAllData(new CSVExportImportHelper.ExportListener() {
            @Override
            public void onExportSuccess(File file) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_success), Toast.LENGTH_LONG).show();
                        showShareFileDialog(file);
                    });
                }
            }
            
            @Override
            public void onExportFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_failed) + ": " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void exportCustomers() {
        Toast.makeText(getContext(), getString(R.string.export_in_progress), Toast.LENGTH_SHORT).show();
        
        csvHelper.exportCustomers(new CSVExportImportHelper.ExportListener() {
            @Override
            public void onExportSuccess(File file) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_success), Toast.LENGTH_LONG).show();
                        showShareFileDialog(file);
                    });
                }
            }
            
            @Override
            public void onExportFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_failed) + ": " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void exportProducts() {
        Toast.makeText(getContext(), getString(R.string.export_in_progress), Toast.LENGTH_SHORT).show();
        
        csvHelper.exportProducts(new CSVExportImportHelper.ExportListener() {
            @Override
            public void onExportSuccess(File file) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_success), Toast.LENGTH_LONG).show();
                        showShareFileDialog(file);
                    });
                }
            }
            
            @Override
            public void onExportFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_failed) + ": " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void exportInvoices() {
        Toast.makeText(getContext(), getString(R.string.export_in_progress), Toast.LENGTH_SHORT).show();
        
        csvHelper.exportInvoices(new CSVExportImportHelper.ExportListener() {
            @Override
            public void onExportSuccess(File file) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_success), Toast.LENGTH_LONG).show();
                        showShareFileDialog(file);
                    });
                }
            }
            
            @Override
            public void onExportFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.export_failed) + ": " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void showShareFileDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.export_success_title))
                .setMessage(getString(R.string.file_saved_path_share_question, file.getAbsolutePath(), getString(R.string.csv_encoding_info)))
                .setPositiveButton(getString(R.string.share), (dialog, which) -> {
                    csvHelper.shareFile(file);
                })
                .setNegativeButton(getString(R.string.ok), null)
                .show();
    }
    
    private void createTemplate(String templateType) {
        Toast.makeText(getContext(), getString(R.string.creating_template), Toast.LENGTH_SHORT).show();
        
        csvHelper.createImportTemplate(templateType, new CSVExportImportHelper.ExportListener() {
            @Override
            public void onExportSuccess(File file) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showTemplateCreatedDialog(file, templateType);
                    });
                }
            }
            
            @Override
            public void onExportFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.failed_create_template, error), Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void showTemplateCreatedDialog(File file, String templateType) {
        String templateName = "customers".equals(templateType) ? "العملاء" : "المنتجات";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.template_created_title))
                .setMessage(getString(R.string.template_created_message, templateName, getString(R.string.csv_template_info), getString(R.string.csv_encoding_info)))
                .setPositiveButton(getString(R.string.share_template), (dialog, which) -> {
                    csvHelper.shareFile(file);
                })
                .setNegativeButton(getString(R.string.ok), null)
                .show();
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/csv");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // إضافة أنواع ملفات أخرى كبديل
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), FILE_PICKER_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), getString(R.string.no_file_picker_app_found), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateToCustomersManagement() {
        // استبدال الواجهة الحالية بواجهة إدارة الزبائن
        getParentFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new CustomersFragment())
            .addToBackStack(null)
            .commit();
    }
    
    private void navigateToProductsManagement() {
        // استبدال الواجهة الحالية بواجهة إدارة المنتجات
        getParentFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new ProductsManagementFragment())
            .addToBackStack(null)
            .commit();
    }
    
    private void openAllInvoicesActivity() {
        Intent intent = new Intent(getActivity(), AllInvoicesActivity.class);
        startActivity(intent);
    }
    
    private void openQRScanner() {
        Intent intent = new Intent(getContext(), QRScannerActivity.class);
        startActivityForResult(intent, QR_SCANNER_REQUEST_CODE);
    }
    
    private void openUserManagement() {
        Intent intent = new Intent(getActivity(), UserManagementActivity.class);
        startActivity(intent);
    }
    
    /**
     * فتح صفحة الإشعارات
     */
    private void openNotifications() {
        // يمكن إنشاء Activity مخصص للإشعارات أو عرض dialog
        showNotificationsDialog();
    }

    /**
     * تنفيذ عملية تسجيل الخروج
     */
    private void performLogout() {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_confirm_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    // استدعاء MainActivity لتنفيذ تسجيل الخروج
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).logoutUser();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
    
    /**
     * تحديث شارة الإشعارات
     */
    private void updateNotificationBadge() {
        if (db != null && notificationBadge != null) {
            db.collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int unreadCount = queryDocumentSnapshots.size();
                    if (unreadCount > 0) {
                        notificationBadge.setText(getString(R.string.notification_new_count, unreadCount));
                        notificationBadge.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        notificationBadge.setText(getString(R.string.notifications_none));
                        notificationBadge.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    }
                })
                .addOnFailureListener(e -> {
                    notificationBadge.setText(getString(R.string.notifications_load_error));
                });
        }
        
        // فحص المخزون المنخفض وإنشاء إشعارات
        checkLowStockAndCreateNotifications();
    }
    
    /**
     * فحص المخزون المنخفض وإنشاء إشعارات تلقائية
     */
    private void checkLowStockAndCreateNotifications() {
        if (db == null) return;
        
        db.collection("products")
            .whereLessThanOrEqualTo("stock", 5) // المنتجات التي مخزونها 5 أو أقل
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String productName = document.getString("name");
                    Long stock = document.getLong("stock");
                    String productId = document.getId();
                    
                    if (productName != null && stock != null) {
                        // فحص ما إذا كان هناك إشعار حديث لهذا المنتج
                        checkAndCreateLowStockNotification(productId, productName, stock.intValue());
                    }
                }
            })
            .addOnFailureListener(e -> {
                NetworkErrorHandler.handleFirestoreError(getContext(), e, getString(R.string.low_stock_check));
            });
    }
    
    /**
     * إنشاء إشعار للمخزون المنخفض إذا لم يكن موجوداً
     */
    private void checkAndCreateLowStockNotification(String productId, String productName, int stock) {
        // فحص وجود إشعار حديث (آخر 24 ساعة) لنفس المنتج
        long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        com.google.firebase.Timestamp oneDayAgoTimestamp = new com.google.firebase.Timestamp(oneDayAgo / 1000, 0);
        
        db.collection("notifications")
            .whereEqualTo("type", "low_stock")
            .whereEqualTo("productId", productId)
            .whereGreaterThan("timestamp", oneDayAgoTimestamp)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    // لا يوجد إشعار حديث، إنشاء إشعار جديد
                    createLowStockNotification(productId, productName, stock);
                }
            });
    }
    
    /**
     * إنشاء إشعار المخزون المنخفض
     */
    private void createLowStockNotification(String productId, String productName, int stock) {
        java.util.Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("type", "low_stock");
        notification.put("productId", productId);
        notification.put("title", getString(R.string.low_stock_alert_title));
        notification.put("message", getString(R.string.low_stock_alert_message, productName, stock));
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("isRead", false);
        notification.put("priority", stock <= 2 ? "high" : "medium");
        
        db.collection("notifications")
            .add(notification)
            .addOnSuccessListener(doc -> {
                android.util.Log.d("MoreFragment", "Low stock notification created for: " + productName);
            })
            .addOnFailureListener(e -> {
                NetworkErrorHandler.handleFirestoreError(getContext(), e, getString(R.string.create_low_stock_notification));
            });
    }
    
    /**
     * عرض حوار الإشعارات
     */
    private void showNotificationsDialog() {
        if (db == null) return;
        
        db.collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Toast.makeText(getContext(), getString(R.string.no_notifications), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                StringBuilder notifications = new StringBuilder();
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String title = document.getString("title");
                    String message = document.getString("message");
                    Boolean isRead = document.getBoolean("isRead");
                    
                    notifications.append(isRead != null && !isRead ? "🔴 " : "✅ ");
                    notifications.append(title).append("\n");
                    notifications.append(message).append("\n\n");
                }
                
                new android.app.AlertDialog.Builder(getContext())
                    .setTitle(getString(R.string.notifications_title))
                    .setMessage(notifications.toString())
                    .setPositiveButton(getString(R.string.mark_as_read), (dialog, which) -> {
                        markAllNotificationsAsRead();
                    })
                    .setNegativeButton(getString(R.string.close), null)
                    .show();
            })
            .addOnFailureListener(e -> {
                NetworkErrorHandler.handleFirestoreError(getContext(), e, getString(R.string.loading_notifications));
            });
    }
    
    /**
     * تمييز جميع الإشعارات كمقروءة
     */
    private void markAllNotificationsAsRead() {
        if (db == null) return;
        
        db.collection("notifications")
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    db.collection("notifications").document(document.getId())
                        .update("isRead", true);
                }
                
                Toast.makeText(getContext(), "تم تمييز الإشعارات كمقروءة", Toast.LENGTH_SHORT).show();
                updateNotificationBadge();
            });
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == QR_SCANNER_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                String customerId = data.getStringExtra("customer_id");
                String qrData = data.getStringExtra("qr_data");
                
                if (customerId != null && !customerId.trim().isEmpty()) {
                    loadCustomerById(customerId);
                } else {
                    Toast.makeText(getContext(), getString(R.string.error_reading_customer_id), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                importDataFromFile(fileUri);
            }
        }
    }
    
    private void importDataFromFile(Uri fileUri) {
        Toast.makeText(getContext(), getString(R.string.import_in_progress), Toast.LENGTH_SHORT).show();
        
        CSVExportImportHelper.ImportListener importListener = new CSVExportImportHelper.ImportListener() {
            @Override
            public void onImportSuccess() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.import_success), Toast.LENGTH_LONG).show();
                    });
                }
            }
            
            @Override
            public void onImportFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.import_failed) + ": " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        };
        
        if ("customers".equals(currentImportType)) {
            csvHelper.importCustomersFromFile(fileUri, importListener);
        } else if ("products".equals(currentImportType)) {
            csvHelper.importProductsFromFile(fileUri, importListener);
        }
    }
    
    private void loadCustomerById(String customerId) {
        // إظهار مؤشر التحميل
        Toast.makeText(getContext(), getString(R.string.loading_customer_data), Toast.LENGTH_SHORT).show();
        
        db.collection("customers").document(customerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Customer customer = documentSnapshot.toObject(Customer.class);
                        if (customer != null) {
                            customer.setId(documentSnapshot.getId());
                            
                            // تعيين العميل في CounterFragment والانتقال إليه
                            CounterFragment.setCustomer(customer);
                            
                            // الانتقال إلى CounterFragment
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).switchToCounterFragment();
                            }
                            
                            Toast.makeText(getContext(), getString(R.string.customer_selected_toast, customer.getName()), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), getString(R.string.error_parsing_customer_data), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), getString(R.string.customer_not_found_database), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.failed_load_customer_data, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * إنشاء مدير النظام يدوياً (للمطورين فقط)
     * يمكن استدعاؤها من خلال Logcat أو في حالات الطوارئ
     */
    @SuppressWarnings("unused")
    public void createEmergencyAdminDialog() {
        // فحص وجود مديرين أولاً
        ManualAdminCreator.checkAdminExists(getContext(), new ManualAdminCreator.AdminCheckCallback() {
            @Override
            public void onResult(boolean hasAdmin, int adminCount) {
                if (hasAdmin) {
                    Toast.makeText(getContext(),
                        getString(R.string.admins_already_exist_count, adminCount),
                        Toast.LENGTH_LONG).show();
                    return;
                }
                
                // إذا لم يوجد أي مدير، اعرض خيارات الإنشاء
                showEmergencyAdminOptions();
            }
            
            @Override
            public void onError(Exception error) {
                Toast.makeText(getContext(), getString(R.string.admin_check_error_with_msg, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * عرض خيارات إنشاء مدير النظام
     */
    private void showEmergencyAdminOptions() {
        String[] options = {
            getString(R.string.default_admin_option),
            getString(R.string.custom_admin_option)
        };
        
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.create_system_admin_title))
                .setMessage(getString(R.string.no_admins_found_message))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // إنشاء المدير الافتراضي
                            ManualAdminCreator.createDefaultAdmin(getContext());
                            break;
                        case 1:
                            // إنشاء مدير مخصص
                            showCustomAdminDialog();
                            break;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
    
    /**
     * حوار إنشاء مدير مخصص
     */
    private void showCustomAdminDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_account, null);
        
        android.widget.EditText fullNameEdit = dialogView.findViewById(R.id.fullNameEditText);
        android.widget.EditText phoneEdit = dialogView.findViewById(R.id.phoneEditText);
        
        // تغيير hint لحقل الهاتف ليصبح للبريد الإلكتروني
        phoneEdit.setHint(getString(R.string.admin_email_hint));
        phoneEdit.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.create_custom_admin_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.create), (dialog, which) -> {
                    String fullName = fullNameEdit.getText().toString().trim();
                    String email = phoneEdit.getText().toString().trim();
                    
                    if (fullName.isEmpty() || email.isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(getContext(), getString(R.string.enter_valid_email), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    ManualAdminCreator.createEmergencyAdmin(getContext(), email, fullName);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /**
     * فتح معرض الصور BMP
     */
    private void openBMPGallery() {
        Intent intent = new Intent(getContext(), BMPGalleryActivity.class);
        startActivity(intent);
    }
    
    /**
     * فتح أرشيف العمليات
     */
    private void openOperationLogs() {
        Intent intent = new Intent(getContext(), OperationLogsActivity.class);
        startActivity(intent);
    }
    
    /**
     * فتح حوار اختيار اللغة
     */
    private void openLanguageSelection() {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        
        android.util.Log.d("MoreFragment", "فتح حوار اختيار اللغة");
        
        LanguageSelectionDialog dialog = LanguageSelectionDialog.newInstance();
        dialog.setOnLanguageSelectedListener(languageCode -> {
            android.util.Log.d("MoreFragment", "تم اختيار اللغة: " + languageCode);
            
            // عرض رسالة تأكيد وإعادة تشغيل النشاط
            new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.language_applied))
                .setMessage(getString(R.string.language_changed_restart_required))
                .setPositiveButton(getString(R.string.restart_app), (d, which) -> {
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setCancelable(false)
                .show();
        });
        
        try {
            dialog.show(getParentFragmentManager(), "LanguageSelectionDialog");
        } catch (Exception e) {
            android.util.Log.e("MoreFragment", getString(R.string.error_showing_language_dialog), e);
            Toast.makeText(getContext(), getString(R.string.error_showing_language_options), Toast.LENGTH_SHORT).show();
        }
    }
}
