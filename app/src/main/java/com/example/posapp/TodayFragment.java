package com.example.posapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Invoice;
import com.example.posapp.model.OperationLog;
import com.example.posapp.service.OperationLogService;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.Intent;
import android.app.ProgressDialog;

public class TodayFragment extends Fragment implements InvoiceListAdapter.OnInvoiceClickListener {
    private RecyclerView invoicesRecyclerView;
    private TextView emptyInvoicesTextView;
    private TextView totalSalesTextView;
    private TextView cashSalesTextView;
    private TextView creditSalesTextView;
    private TextView invoiceCountTextView;
    private TextView todayDateTextView;
    private Button addNewInvoiceButton;
    
    private InvoiceListAdapter adapter;
    private List<Invoice> invoiceList;
    
    private FirebaseFirestore db;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today, container, false);
        
        // ربط عناصر الواجهة
        invoicesRecyclerView = view.findViewById(R.id.invoicesRecyclerView);
        emptyInvoicesTextView = view.findViewById(R.id.emptyInvoicesTextView);
        totalSalesTextView = view.findViewById(R.id.totalSalesTextView);
        cashSalesTextView = view.findViewById(R.id.cashSalesTextView);
        creditSalesTextView = view.findViewById(R.id.creditSalesTextView);
        invoiceCountTextView = view.findViewById(R.id.invoiceCountTextView);
        todayDateTextView = view.findViewById(R.id.todayDateTextView);
        addNewInvoiceButton = view.findViewById(R.id.addNewInvoiceButton);
        
        // إعداد قائمة الفواتير
        invoiceList = new ArrayList<>();
        adapter = new InvoiceListAdapter(invoiceList);
        adapter.setOnInvoiceClickListener(this);
        
        invoicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        invoicesRecyclerView.setAdapter(adapter);
        
        // إعداد Firestore
        db = FirebaseFirestore.getInstance();
        
        // عرض تاريخ اليوم بأرقام عربية
        String todayDate = ArabicNumberUtils.formatLongDateWithArabicNumbers(new Date());
        todayDateTextView.setText("فواتير " + todayDate);
        
        // إعداد زر إنشاء فاتورة جديدة
        addNewInvoiceButton.setOnClickListener(v -> openNewInvoice());
        
        // تحميل فواتير اليوم
        loadTodayInvoices();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // تسجيل مستمع إضافة الفواتير
        CheckoutDialog.setOnInvoiceAddedListener(() -> {
            // إعادة تحميل فواتير اليوم عند إضافة فاتورة جديدة
            loadTodayInvoices();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // إلغاء تسجيل المستمع
        CheckoutDialog.setOnInvoiceAddedListener(null);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // إعادة تحميل البيانات عند العودة للواجهة
        loadTodayInvoices();
    }
    
    private void loadTodayInvoices() {
        // الحصول على بداية ونهاية اليوم الحالي
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();
        
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfDay = calendar.getTime();
        
        Timestamp startTimestamp = new Timestamp(startOfDay);
        Timestamp endTimestamp = new Timestamp(endOfDay);
        
        // استعلام لجلب فواتير اليوم
        db.collection("invoices")
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .orderBy("date", Query.Direction.DESCENDING) // أحدث الفواتير أولاً
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                invoiceList.clear();
                
                if (queryDocumentSnapshots.isEmpty()) {
                    // عرض رسالة إذا لم تكن هناك فواتير
                    showEmptyView(true);
                    updateSummary(0, 0.0, 0.0, 0.0);
                    return;
                }
                
                double totalSales = 0.0;
                double cashSales = 0.0;
                double creditSales = 0.0;
                
                for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                    Invoice invoice = queryDocumentSnapshots.getDocuments().get(i).toObject(Invoice.class);
                    if (invoice != null) {
                        invoice.setId(queryDocumentSnapshots.getDocuments().get(i).getId());
                        invoiceList.add(invoice);
                        
                        double amount = invoice.getTotalAmount();
                        totalSales += amount;
                        
                        // تحديد نوع الدفع
                        if (isInvoicePaid(invoice)) {
                            cashSales += amount;
                        } else {
                            creditSales += amount;
                        }
                    }
                }
                
                adapter.notifyDataSetChanged();
                showEmptyView(false);
                updateSummary(invoiceList.size(), totalSales, cashSales, creditSales);
            })
            .addOnFailureListener(e -> {
                showEmptyView(true);
                Toast.makeText(getContext(), "حدث خطأ أثناء تحميل الفواتير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showEmptyView(boolean isEmpty) {
        if (isEmpty) {
            invoicesRecyclerView.setVisibility(View.GONE);
            emptyInvoicesTextView.setVisibility(View.VISIBLE);
        } else {
            invoicesRecyclerView.setVisibility(View.VISIBLE);
            emptyInvoicesTextView.setVisibility(View.GONE);
        }
    }
    
    private void updateSummary(int count, double total, double cashSales, double creditSales) {
        invoiceCountTextView.setText("عدد الفواتير: " + count);
        totalSalesTextView.setText("إجمالي المبيعات: " + CurrencyUtils.formatCurrency(total));
        
        // عرض تفصيل المبيعات النقدية والدين
        if (cashSalesTextView != null) {
            cashSalesTextView.setText("💵 المبيعات النقدية: " + CurrencyUtils.formatCurrency(cashSales));
        }
        if (creditSalesTextView != null) {
            creditSalesTextView.setText("📝 المبيعات بالدين: " + CurrencyUtils.formatCurrency(creditSales));
        }
    }
    
    /**
     * تحديد ما إذا كانت الفاتورة مدفوعة نقداً أم بالدين
     */
    private boolean isInvoicePaid(Invoice invoice) {
        // فحص طريقة الدفع الجديدة أولاً
        if (invoice.getPaymentMethod() != null) {
            return invoice.getPaymentMethod().isLegacyPaid();
        }
        // الرجوع للطريقة القديمة
        return invoice.isPaid();
    }
    
    @Override
    public void onInvoiceClick(Invoice invoice, int position) {
        // عرض خيارات التعامل مع الفاتورة
        showInvoiceOptionsDialog(invoice, position);
    }
    
    /**
     * عرض خيارات الفاتورة (طباعة، تعديل، إضافة منتجات، حذف)
     */
    private void showInvoiceOptionsDialog(Invoice invoice, int position) {
        String invoiceNumber = invoice.getDisplayNumber();
        String customerName = invoice.getCustomerName();
        String totalAmount = CurrencyUtils.formatCurrency(invoice.getTotalAmount());
        
        // إنشاء قائمة الخيارات
        String[] options = {"طباعة", "إضافة منتجات", "تحميل في الكاونتر", "حذف الفاتورة"};
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("فاتورة " + invoiceNumber)
                .setMessage("العميل: " + customerName + "\nالمجموع: " + totalAmount + "\n\nاختر العملية:")
                .setIcon(android.R.drawable.ic_menu_edit)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // طباعة
                            Intent intent = InvoicePrintActivity.createIntent(getContext(), invoice.getId());
                            startActivity(intent);
                            break;
                        case 1: // إضافة منتجات
                            openAddProductsDialog(invoice, position);
                            break;
                        case 2: // تحميل في الكاونتر
                            loadInvoiceInCounter(invoice);
                            break;
                        case 3: // حذف الفاتورة
                            showDeleteInvoiceConfirmDialog(invoice, position);
                            break;
                    }
                })
                .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    private void showInvoiceDetails(Invoice invoice) {
        // هنا يمكن إضافة كود لعرض تفاصيل الفاتورة في نافذة منبثقة
        // سنقوم بتنفيذها لاحقاً إذا طلب المستخدم
    }
    
    /**
     * فتح شاشة إنشاء فاتورة جديدة
     */
    private void openNewInvoice() {
        // إظهار خيارات للمستخدم: إنشاء فاتورة جديدة أو إضافة منتج سريع
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("إنشاء فاتورة جديدة")
                .setMessage("كيف تريد إضافة منتج جديد؟")
                .setIcon(android.R.drawable.ic_input_add)
                .setPositiveButton("الذهاب للكاونتر", (dialog, which) -> {
                    // مسح الفاتورة الحالية في الكاونتر وبدء فاتورة جديدة
                    CounterFragment.clearInvoice();
                    CounterFragment.clearCustomer();
                    
                    // الانتقال إلى شاشة الكاونتر
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).switchToCounterFragment();
                        Toast.makeText(getContext(), "🆕 تم إنشاء فاتورة جديدة في الكاونتر", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("إضافة منتج سريع", (dialog, which) -> {
                    // فتح حوار اختيار المنتجات مباشرة
                    openQuickProductSelection();
                })
                .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    /**
     * فتح حوار اختيار المنتجات السريع
     */
    private void openQuickProductSelection() {
        QuickInvoiceDialog dialog = new QuickInvoiceDialog();
        dialog.setOnInvoiceCreatedListener(() -> {
            // إعادة تحميل فواتير اليوم عند إنشاء فاتورة جديدة
            loadTodayInvoices();
            Toast.makeText(getContext(), "✅ تم إنشاء الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getChildFragmentManager(), "QuickInvoiceDialog");
    }
    
    /**
     * فتح حوار إضافة منتجات للفاتورة الموجودة
     */
    private void openAddProductsDialog(Invoice invoice, int position) {
        AddProductsToInvoiceDialog dialog = AddProductsToInvoiceDialog.newInstance(invoice);
        dialog.setOnProductsAddedListener((updatedInvoice) -> {
            // تحديث الفاتورة في القائمة
            invoiceList.set(position, updatedInvoice);
            adapter.notifyItemChanged(position);
            
            // إعادة تحميل فواتير اليوم لتحديث الملخص
            loadTodayInvoices();
            
            Toast.makeText(getContext(), "✅ تم إضافة المنتجات للفاتورة بنجاح", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getChildFragmentManager(), "AddProductsToInvoiceDialog");
    }
    
    /**
     * تحميل الفاتورة في الكاونتر للتعديل الشامل
     */
    private void loadInvoiceInCounter(Invoice invoice) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("تحميل في الكاونتر")
                .setMessage("سيتم تحميل هذه الفاتورة في الكاونتر للتعديل.\n\nملاحظة: أي فاتورة حالية في الكاونتر ستُمسح.\n⚠️ سيتم تحديث الفاتورة الأصلية وليس إنشاء فاتورة جديدة.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("تحميل", (dialog, which) -> {
                    // البحث عن معلومات العميل أولاً
                    com.example.posapp.model.Customer customerToLoad = null;
                    
                    if (!invoice.getCustomerName().equals("مجهول") &&
                        !invoice.getCustomerPhone().isEmpty()) {
                        
                        // البحث عن العميل وتحميله
                        db.collection("customers")
                            .whereEqualTo("phone", invoice.getCustomerPhone())
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                com.example.posapp.model.Customer customer = null;
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    customer = queryDocumentSnapshots.getDocuments().get(0)
                                            .toObject(com.example.posapp.model.Customer.class);
                                    if (customer != null) {
                                        customer.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                                    }
                                }
                                
                                // تحميل الفاتورة مع معلومات العميل
                                CounterFragment.loadExistingInvoice(invoice.getId(), invoice.getItems(), customer);
                                
                                // الانتقال إلى شاشة الكاونتر
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).switchToCounterFragment();
                                    Toast.makeText(getContext(), "✅ تم تحميل الفاتورة للتعديل - رقم: " + invoice.getDisplayNumber(), Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // تحميل الفاتورة بدون معلومات العميل
                                CounterFragment.loadExistingInvoice(invoice.getId(), invoice.getItems(), null);
                                
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).switchToCounterFragment();
                                    Toast.makeText(getContext(), "✅ تم تحميل الفاتورة للتعديل (بدون معلومات العميل)", Toast.LENGTH_LONG).show();
                                }
                            });
                    } else {
                        // تحميل الفاتورة مباشرة بدون عميل
                        CounterFragment.loadExistingInvoice(invoice.getId(), invoice.getItems(), null);
                        
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).switchToCounterFragment();
                            Toast.makeText(getContext(), "✅ تم تحميل الفاتورة للتعديل - رقم: " + invoice.getDisplayNumber(), Toast.LENGTH_LONG).show();
                        }
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
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
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
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("جاري حذف الفاتورة...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // الحصول على خدمة تسجيل العمليات
        OperationLogService operationLogService = OperationLogService.getInstance(getContext());
        
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
                        android.util.Log.d("TodayFragment", "Invoice deletion logged successfully");
                    }).exceptionally(throwable -> {
                        android.util.Log.e("TodayFragment", "Failed to log invoice deletion", throwable);
                        return null;
                    });
                    
                    // إزالة الفاتورة من القائمة
                    if (position >= 0 && position < invoiceList.size()) {
                        invoiceList.remove(position);
                        if (adapter != null) {
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, invoiceList.size());
                        }
                    }
                    
                    // إعادة حساب الإحصائيات
                    updateSummary(invoiceList.size(), calculateTotalSales(), calculateCashSales(), calculateCreditSales());
                    
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "✅ تم حذف الفاتورة " + invoice.getDisplayNumber() + " نهائياً", 
                            Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "❌ فشل في حذف الفاتورة: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    android.util.Log.e("TodayFragment", "Failed to delete invoice", e);
                });
    }
    
    /**
     * حساب إجمالي المبيعات
     */
    private double calculateTotalSales() {
        double total = 0.0;
        for (Invoice invoice : invoiceList) {
            total += invoice.getTotalAmount();
        }
        return total;
    }
    
    /**
     * حساب المبيعات النقدية
     */
    private double calculateCashSales() {
        double total = 0.0;
        for (Invoice invoice : invoiceList) {
            if (invoice.getPaymentMethod() != null && 
                invoice.getPaymentMethod().name().equals("CASH")) {
                total += invoice.getTotalAmount();
            }
        }
        return total;
    }
    
    /**
     * حساب المبيعات الآجلة
     */
    private double calculateCreditSales() {
        double total = 0.0;
        for (Invoice invoice : invoiceList) {
            if (invoice.getPaymentMethod() != null && 
                invoice.getPaymentMethod().name().equals("DEBT")) {
                total += invoice.getTotalAmount();
            }
        }
        return total;
    }
}
