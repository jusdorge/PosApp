package com.example.islamicquiz;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.islamicquiz.model.Invoice;
import com.example.islamicquiz.model.OperationLog;
import com.example.islamicquiz.model.Customer;
import com.example.islamicquiz.service.OperationLogService;
import com.example.islamicquiz.utils.LocationUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import android.app.ProgressDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllInvoicesActivity extends AppCompatActivity {
    
    private RecyclerView invoicesRecyclerView;
    private AllInvoicesAdapter invoicesAdapter;
    private List<Invoice> invoicesList;
    
    private Button btnStartDate;
    private Button btnEndDate;
    private Button btnFilter;
    private Button btnClearFilter;
    private TextView tvTotalInvoices;
    private TextView tvTotalAmount;
    
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    
    private Date startDate;
    private Date endDate;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_invoices);
        
        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("جميع الفواتير");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // تهيئة المتغيرات
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        calendar = Calendar.getInstance();
        invoicesList = new ArrayList<>();
        
        // ربط عناصر الواجهة
        initViews();
        
        // إعداد RecyclerView
        setupRecyclerView();
        
        // إعداد الأزرار
        setupButtons();
        
        // تحميل جميع الفواتير
        loadAllInvoices();
    }
    
    private void initViews() {
        invoicesRecyclerView = findViewById(R.id.invoicesRecyclerView);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnFilter = findViewById(R.id.btnFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        tvTotalInvoices = findViewById(R.id.tvTotalInvoices);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
    }
    
    private void setupRecyclerView() {
        invoicesAdapter = new AllInvoicesAdapter(invoicesList);
        invoicesAdapter.setOnInvoiceClickListener(this::onInvoiceClick);
        invoicesAdapter.setOnInvoiceButtonClickListener(new AllInvoicesAdapter.OnInvoiceButtonClickListener() {
            @Override
            public void onPrintClick(Invoice invoice, int position) {
                Intent intent = InvoicePrintActivity.createIntent(AllInvoicesActivity.this, invoice.getId());
                startActivity(intent);
            }

            @Override
            public void onLoadInCounterClick(Invoice invoice, int position) {
                loadInvoiceInCounter(invoice);
            }

            @Override
            public void onAddProductsClick(Invoice invoice, int position) {
                openAddProductsDialog(invoice, position);
            }

            @Override
            public void onCustomerLocationClick(Invoice invoice, int position) {
                openCustomerLocationOnMap(invoice);
            }

            @Override
            public void onDeleteClick(Invoice invoice, int position) {
                showDeleteInvoiceConfirmDialog(invoice, position);
            }
        });
        invoicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        invoicesRecyclerView.setAdapter(invoicesAdapter);
    }
    
    private void setupButtons() {
        btnStartDate.setOnClickListener(v -> showStartDatePicker());
        btnEndDate.setOnClickListener(v -> showEndDatePicker());
        btnFilter.setOnClickListener(v -> filterInvoicesByDate());
        btnClearFilter.setOnClickListener(v -> clearFilters());
    }
    
    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 0, 0, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startDate = calendar.getTime();
                btnStartDate.setText("من: " + dateFormat.format(startDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 23, 59, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                endDate = calendar.getTime();
                btnEndDate.setText("إلى: " + dateFormat.format(endDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void filterInvoicesByDate() {
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "يرجى اختيار تاريخ البداية والنهاية", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (startDate.after(endDate)) {
            Toast.makeText(this, "تاريخ البداية يجب أن يكون قبل تاريخ النهاية", Toast.LENGTH_SHORT).show();
            return;
        }
        
        loadInvoicesByDateRange();
    }
    
    private void clearFilters() {
        startDate = null;
        endDate = null;
        btnStartDate.setText("اختر تاريخ البداية");
        btnEndDate.setText("اختر تاريخ النهاية");
        loadAllInvoices();
    }
    
    private void loadAllInvoices() {
        db.collection("invoices")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    invoicesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Invoice invoice = document.toObject(Invoice.class);
                        invoice.setId(document.getId());
                        invoicesList.add(invoice);
                    }
                    invoicesAdapter.notifyDataSetChanged();
                    updateSummary();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل الفواتير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadInvoicesByDateRange() {
        Timestamp startTimestamp = new Timestamp(startDate);
        Timestamp endTimestamp = new Timestamp(endDate);
        
        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThanOrEqualTo("date", endTimestamp)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    invoicesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Invoice invoice = document.toObject(Invoice.class);
                        invoice.setId(document.getId());
                        invoicesList.add(invoice);
                    }
                    invoicesAdapter.notifyDataSetChanged();
                    updateSummary();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل الفواتير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateSummary() {
        int totalInvoices = invoicesList.size();
        double totalAmount = 0.0;
        
        for (Invoice invoice : invoicesList) {
            totalAmount += invoice.getTotalAmount();
        }
        
        tvTotalInvoices.setText("عدد الفواتير: " + totalInvoices);
                    tvTotalAmount.setText("المجموع الكلي: " + CurrencyUtils.formatCurrency(totalAmount));
    }
    
    private void openInvoicePrintActivity(Invoice invoice) {
        Intent intent = InvoicePrintActivity.createIntent(this, invoice.getId());
        startActivity(intent);
    }

    /**
     * معالجة النقر على الفاتورة - عرض خيارات العمليات
     */
    private void onInvoiceClick(Invoice invoice, int position) {
        showInvoiceOptionsDialog(invoice, position);
    }

    /**
     * طريقة بديلة لعرض خيارات الفاتورة
     */
    private void showInvoiceOptionsDialog(Invoice invoice, int position) {
        String invoiceNumber = invoice.getDisplayNumber();
        String customerName = invoice.getCustomerName();
        String totalAmount = CurrencyUtils.formatCurrency(invoice.getTotalAmount());
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("فاتورة " + invoiceNumber);
        builder.setMessage("العميل: " + customerName + "\nالمجموع: " + totalAmount + "\n\nاختر العملية:");
        
        // إضافة الأزرار الأساسية
        builder.setPositiveButton("طباعة", (dialog, which) -> {
            Intent intent = InvoicePrintActivity.createIntent(this, invoice.getId());
            startActivity(intent);
        });
        
        builder.setNeutralButton("تحميل في الكاونتر", (dialog, which) -> {
            loadInvoiceInCounter(invoice);
        });
        
        builder.setNegativeButton("المزيد من الخيارات", (dialog, which) -> {
            showMoreOptionsDialog(invoice, position);
        });
        
        builder.show();
    }

    /**
     * عرض خيارات إضافية للفاتورة
     */
    private void showMoreOptionsDialog(Invoice invoice, int position) {
        String[] options = {"إضافة منتجات", "📍 موقع العميل", "حذف الفاتورة"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("خيارات إضافية");
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // إضافة منتجات
                    openAddProductsDialog(invoice, position);
                    break;
                case 1: // موقع العميل
                    openCustomerLocationOnMap(invoice);
                    break;
                case 2: // حذف الفاتورة
                    showDeleteInvoiceConfirmDialog(invoice, position);
                    break;
            }
        });
        
        builder.setNegativeButton("رجوع", (dialog, which) -> {
            showInvoiceOptionsDialog(invoice, position);
        });
        
        builder.show();
    }

    /**
     * فتح حوار إضافة منتجات للفاتورة الموجودة
     */
    private void openAddProductsDialog(Invoice invoice, int position) {
        AddProductsToInvoiceDialog dialog = AddProductsToInvoiceDialog.newInstance(invoice);
        dialog.setOnProductsAddedListener((updatedInvoice) -> {
            // تحديث الفاتورة في القائمة
            invoicesList.set(position, updatedInvoice);
            invoicesAdapter.notifyItemChanged(position);
            
            // إعادة تحديث الملخص
            updateSummary();
            
            Toast.makeText(this, "✅ تم إضافة المنتجات للفاتورة بنجاح", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "AddProductsToInvoiceDialog");
    }

    /**
     * تحميل الفاتورة في الكاونتر للتعديل الشامل
     */
    private void loadInvoiceInCounter(Invoice invoice) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("تحميل في الكاونتر")
                .setMessage("سيتم تحميل هذه الفاتورة في الكاونتر للتعديل.\n\nملاحظة: أي فاتورة حالية في الكاونتر ستُمسح.\n⚠️ سيتم تحديث الفاتورة الأصلية وليس إنشاء فاتورة جديدة.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("تحميل", (dialog, which) -> {
                    // البحث عن معلومات العميل أولاً
                    com.example.islamicquiz.model.Customer customerToLoad = null;
                    
                    if (!invoice.getCustomerName().equals("مجهول") &&
                        !invoice.getCustomerPhone().isEmpty()) {
                        
                        // البحث عن العميل وتحميله
                        db.collection("customers")
                            .whereEqualTo("phone", invoice.getCustomerPhone())
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                com.example.islamicquiz.model.Customer customer = null;
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    customer = queryDocumentSnapshots.getDocuments().get(0)
                                            .toObject(com.example.islamicquiz.model.Customer.class);
                                    if (customer != null) {
                                        customer.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                                    }
                                }
                                
                                // تحميل الفاتورة مع معلومات العميل
                                CounterFragment.loadExistingInvoice(invoice.getId(), invoice.getItems(), customer);
                                
                                // العودة إلى MainActivity والانتقال للكاونتر
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.putExtra("switch_to_counter", true);
                                startActivity(intent);
                                Toast.makeText(this, "✅ تم تحميل الفاتورة للتعديل - رقم: " + invoice.getDisplayNumber(), Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // تحميل الفاتورة بدون معلومات العميل
                                CounterFragment.loadExistingInvoice(invoice.getId(), invoice.getItems(), null);
                                
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.putExtra("switch_to_counter", true);
                                startActivity(intent);
                                Toast.makeText(this, "✅ تم تحميل الفاتورة للتعديل (بدون معلومات العميل)", Toast.LENGTH_LONG).show();
                                finish();
                            });
                    } else {
                        // تحميل الفاتورة مباشرة بدون عميل
                        CounterFragment.loadExistingInvoice(invoice.getId(), invoice.getItems(), null);
                        
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("switch_to_counter", true);
                        startActivity(intent);
                        Toast.makeText(this, "✅ تم تحميل الفاتورة للتعديل - رقم: " + invoice.getDisplayNumber(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * عرض حوار تأكيد حذف الفاتورة
     */
    private void showDeleteInvoiceConfirmDialog(Invoice invoice, int position) {
        String invoiceNumber = invoice.getDisplayNumber();
        String customerName = invoice.getCustomerName();
        String totalAmount = CurrencyUtils.formatCurrency(invoice.getTotalAmount());
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ حذف فاتورة")
                .setMessage("هل أنت متأكد من حذف الفاتورة نهائياً؟\n\n" +
                        "📄 رقم الفاتورة: " + invoiceNumber + "\n" +
                        "👤 العميل: " + customerName + "\n" +
                        "💰 المبلغ: " + totalAmount + "\n\n" +
                        "⚠️ تحذير: هذه العملية لا يمكن التراجع عنها!\n" +
                        "سيتم حذف الفاتورة ومحتوياتها نهائياً من النظام.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("🗑️ حذف نهائي", (dialog, which) -> {
                    deleteInvoice(invoice, position);
                })
                .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * حذف الفاتورة من قاعدة البيانات
     */
    private void deleteInvoice(Invoice invoice, int position) {
        // إظهار progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("جاري حذف الفاتورة...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // الحصول على خدمة تسجيل العمليات
        OperationLogService operationLogService = OperationLogService.getInstance(this);
        
        // تحضير بيانات الفاتورة للأرشيف
        java.util.Map<String, Object> deletedData = new java.util.HashMap<>();
        deletedData.put("invoiceNumber", invoice.getDisplayNumber());
        deletedData.put("customerName", invoice.getCustomerName());
        deletedData.put("customerPhone", invoice.getCustomerPhone());
        deletedData.put("totalAmount", invoice.getTotalAmount());
        deletedData.put("itemsCount", invoice.getItems() != null ? invoice.getItems().size() : 0);
        deletedData.put("paymentMethod", invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : "UNKNOWN");
        deletedData.put("createdDate", invoice.getDate());
        
        // حذف الفاتورة من قاعدة البيانات
        db.collection("invoices").document(invoice.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // تسجيل عملية الحذف في الأرشيف
                    String description = "حذف فاتورة رقم " + invoice.getDisplayNumber() + 
                                       " للعميل " + invoice.getCustomerName() + 
                                       " بقيمة " + CurrencyUtils.formatCurrency(invoice.getTotalAmount());
                    
                    operationLogService.logDelete(
                        OperationLog.EntityType.INVOICE,
                        invoice.getId(),
                        description,
                        deletedData
                    ).thenRun(() -> {
                        android.util.Log.d("AllInvoicesActivity", "Invoice deletion logged successfully");
                    }).exceptionally(throwable -> {
                        android.util.Log.e("AllInvoicesActivity", "Failed to log invoice deletion", throwable);
                        return null;
                    });
                    
                    // إزالة الفاتورة من القائمة
                    if (position >= 0 && position < invoicesList.size()) {
                        invoicesList.remove(position);
                        if (invoicesAdapter != null) {
                            invoicesAdapter.notifyItemRemoved(position);
                            invoicesAdapter.notifyItemRangeChanged(position, invoicesList.size());
                        }
                    }
                    
                    // إعادة حساب الإحصائيات
                    updateSummary();
                    
                    progressDialog.dismiss();
                    Toast.makeText(this, "✅ تم حذف الفاتورة " + invoice.getDisplayNumber() + " نهائياً", 
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "❌ فشل في حذف الفاتورة: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    android.util.Log.e("AllInvoicesActivity", "Failed to delete invoice", e);
                });
    }
    
    /**
     * فتح موقع العميل على الخريطة
     */
    private void openCustomerLocationOnMap(Invoice invoice) {
        if (invoice == null) {
            Toast.makeText(this, "بيانات الفاتورة غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // التحقق من وجود بيانات العميل
        String customerPhone = invoice.getCustomerPhone();
        if (customerPhone == null || customerPhone.isEmpty() || customerPhone.equals("مجهول")) {
            Toast.makeText(this, "هذه الفاتورة لا تحتوي على بيانات عميل محددة", Toast.LENGTH_LONG).show();
            return;
        }
        
        // البحث عن العميل في قاعدة البيانات
        db.collection("customers")
            .whereEqualTo("phone", customerPhone)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    Toast.makeText(this, "لم يتم العثور على بيانات العميل في النظام", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // الحصول على بيانات العميل
                Customer customer = queryDocumentSnapshots.getDocuments().get(0).toObject(Customer.class);
                if (customer != null) {
                    customer.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                    
                    // التحقق من صلاحيات الموقع
                    if (!LocationUtils.hasLocationPermission(this)) {
                        // طلب الصلاحيات
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("صلاحيات الموقع")
                            .setMessage("يحتاج التطبيق لصلاحية الوصول للموقع لإظهار المسار إلى العميل.\n\nهل تريد منح الصلاحية؟")
                            .setPositiveButton("نعم", (dialog, which) -> {
                                LocationUtils.requestLocationPermission(this);
                                Toast.makeText(this, "بعد منح الصلاحية، جرب مرة أخرى", Toast.LENGTH_LONG).show();
                            })
                            .setNegativeButton("لا", (dialog, which) -> {
                                // فتح موقع العميل فقط بدون المسار
                                LocationUtils.openGoogleMaps(this, customer.getLatitude(), customer.getLongitude(), customer.getName());
                            })
                            .show();
                        return;
                    }
                    
                    // فتح المسار إلى العميل
                    LocationUtils.openNavigationToCustomer(this, customer);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "خطأ في البحث عن بيانات العميل: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 