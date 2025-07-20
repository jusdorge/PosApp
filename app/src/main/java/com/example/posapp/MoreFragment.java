package com.example.posapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.posapp.model.Customer;
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
        
        setupClickListeners();
        
        return view;
    }
    
    private void setupClickListeners() {
        customersManagementCard.setOnClickListener(v -> {
            navigateToCustomersManagement();
        });
        
        productsManagementCard.setOnClickListener(v -> {
            navigateToProductsManagement();
        });
        
        allInvoicesCard.setOnClickListener(v -> {
            openAllInvoicesActivity();
        });
        
        qrScannerCard.setOnClickListener(v -> {
            openQRScanner();
        });
        
        exportDataCard.setOnClickListener(v -> {
            showExportOptionsDialog();
        });
        
        importDataCard.setOnClickListener(v -> {
            showImportOptionsDialog();
        });
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
        builder.setTitle("نجح التصدير")
                .setMessage("تم حفظ الملف في: " + file.getAbsolutePath() + "\n\n" +
                           getString(R.string.csv_encoding_info) + "\n\nهل تريد مشاركته؟")
                .setPositiveButton("مشاركة", (dialog, which) -> {
                    csvHelper.shareFile(file);
                })
                .setNegativeButton("حسناً", null)
                .show();
    }
    
    private void createTemplate(String templateType) {
        Toast.makeText(getContext(), "جاري إنشاء القالب...", Toast.LENGTH_SHORT).show();
        
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
                        Toast.makeText(getContext(), "فشل في إنشاء القالب: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    private void showTemplateCreatedDialog(File file, String templateType) {
        String templateName = "customers".equals(templateType) ? "العملاء" : "المنتجات";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("تم إنشاء القالب")
                .setMessage("تم إنشاء قالب " + templateName + " بنجاح!\n\n" +
                           getString(R.string.csv_template_info) + "\n\n" +
                           getString(R.string.csv_encoding_info))
                .setPositiveButton("مشاركة القالب", (dialog, which) -> {
                    csvHelper.shareFile(file);
                })
                .setNegativeButton("حسناً", null)
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
            Toast.makeText(getContext(), "لم يتم العثور على تطبيق لاختيار الملفات", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "خطأ في قراءة معرف العميل", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getContext(), "جاري تحميل بيانات العميل...", Toast.LENGTH_SHORT).show();
        
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
                            
                            Toast.makeText(getContext(), "تم اختيار العميل: " + customer.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "خطأ في تحليل بيانات العميل", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "العميل غير موجود في قاعدة البيانات", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل في تحميل بيانات العميل: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
