package com.example.posapp;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Invoice;
import com.example.posapp.model.InvoiceItem;
import com.example.posapp.model.PaymentMethod;
import com.example.posapp.model.Product;
import com.example.posapp.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuickInvoiceDialog extends DialogFragment implements InvoiceItemAdapter.OnProductAddListener {
    
    private RecyclerView productsRecyclerView;
    private EditText searchProductEditText;
    private TextView selectedProductsTextView;
    private TextView totalAmountTextView;
    private EditText customerNameEditText;
    private EditText customerPhoneEditText;
    private Button createInvoiceButton;
    private Button cancelButton;
    
    private InvoiceItemAdapter productAdapter;
    private List<Product> productList;
    private List<InvoiceItem> selectedItems;
    private double totalAmount = 0.0;
    
    private FirebaseFirestore db;
    private OnInvoiceCreatedListener onInvoiceCreatedListener;
    
    public interface OnInvoiceCreatedListener {
        void onInvoiceCreated();
    }
    
    public void setOnInvoiceCreatedListener(OnInvoiceCreatedListener listener) {
        this.onInvoiceCreatedListener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_quick_invoice, container, false);
        
        // ربط العناصر
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView);
        searchProductEditText = view.findViewById(R.id.searchProductEditText);
        selectedProductsTextView = view.findViewById(R.id.selectedProductsTextView);
        totalAmountTextView = view.findViewById(R.id.totalAmountTextView);
        customerNameEditText = view.findViewById(R.id.customerNameEditText);
        customerPhoneEditText = view.findViewById(R.id.customerPhoneEditText);
        createInvoiceButton = view.findViewById(R.id.createInvoiceButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        
        // إعداد المتغيرات
        productList = new ArrayList<>();
        selectedItems = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        
        // إعداد RecyclerView
        productAdapter = new InvoiceItemAdapter(productList, this);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        productsRecyclerView.setAdapter(productAdapter);
        
        // إعداد البحث
        searchProductEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // إعداد الأزرار
        createInvoiceButton.setOnClickListener(v -> createQuickInvoice());
        cancelButton.setOnClickListener(v -> dismiss());
        
        // تحميل المنتجات
        loadProducts();
        
        // تحديث العرض الأولي
        updateSelectedItems();
        
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);
            dialog.getWindow().setLayout(width, height);
        }
    }
    
    private void loadProducts() {
        db.collection("products")
            .whereGreaterThan("quantity", 0) // فقط المنتجات المتوفرة
            .orderBy("quantity")
            .orderBy("name")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                productList.clear();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Product product = document.toObject(Product.class);
                    product.setId(document.getId());
                    productList.add(product);
                }
                
                productAdapter.updateProducts(productList);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في تحميل المنتجات: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void filterProducts(String query) {
        List<Product> filteredList = new ArrayList<>();
        
        if (query.isEmpty()) {
            filteredList.addAll(productList);
        } else {
            for (Product product : productList) {
                if (product.getName().toLowerCase().contains(query.toLowerCase()) ||
                    (product.getBarcode() != null && product.getBarcode().contains(query)) ||
                    (product.getCategory() != null && product.getCategory().toLowerCase().contains(query.toLowerCase()))) {
                    filteredList.add(product);
                }
            }
        }
        
        productAdapter.updateProducts(filteredList);
    }
    
    @Override
    public void onProductAdd(Product product, int quantity) {
        // البحث عن المنتج في القائمة المختارة
        boolean found = false;
        for (InvoiceItem item : selectedItems) {
            if (item.getProductId().equals(product.getId())) {
                // زيادة الكمية
                item.setQuantity(item.getQuantity() + quantity);
                found = true;
                break;
            }
        }
        
        // إضافة منتج جديد إذا لم يكن موجوداً
        if (!found) {
            InvoiceItem newItem = new InvoiceItem(
                product.getId(),
                product.getName(),
                product.getDefaultPrice(),
                quantity
            );
            selectedItems.add(newItem);
        }
        
        // تحديث العرض
        updateSelectedItems();
        
        Toast.makeText(getContext(), "✅ تم إضافة " + product.getName(), Toast.LENGTH_SHORT).show();
    }
    
    private void updateSelectedItems() {
        // حساب المجموع
        totalAmount = 0.0;
        for (InvoiceItem item : selectedItems) {
            totalAmount += item.getQuantity() * item.getPrice();
        }
        
        // تحديث النصوص
        selectedProductsTextView.setText("المنتجات المختارة: " + selectedItems.size());
        totalAmountTextView.setText("المجموع: " + CurrencyUtils.formatCurrency(totalAmount));
        
        // تفعيل/تعطيل زر الإنشاء
        createInvoiceButton.setEnabled(!selectedItems.isEmpty());
    }
    
    private void createQuickInvoice() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(getContext(), "الرجاء اختيار منتج واحد على الأقل", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // الحصول على معلومات العميل
        String tempCustomerName = customerNameEditText.getText().toString().trim();
        String tempCustomerPhone = customerPhoneEditText.getText().toString().trim();
        
        final String customerName = tempCustomerName.isEmpty() ? "مجهول" : tempCustomerName;
        final String customerPhone = tempCustomerPhone.isEmpty() ? "" : tempCustomerPhone;
        
        // الحصول على المستخدم الحالي
        UserSession userSession = UserSession.getInstance(getContext());
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(getContext(), "خطأ: لا يمكن تحديد المستخدم الحالي", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // إظهار مؤشر التحميل
        createInvoiceButton.setEnabled(false);
        createInvoiceButton.setText("جاري الإنشاء...");
        
        // إنشاء رقم الفاتورة المخصص
        InvoiceNumberGenerator numberGenerator = new InvoiceNumberGenerator(getContext());
        numberGenerator.generateInvoiceNumber(currentUser)
            .thenAccept(customInvoiceNumber -> {
                // إنشاء الفاتورة
                Timestamp now = new Timestamp(new Date());
                Invoice invoice = new Invoice(
                    customerName, 
                    customerPhone, 
                    PaymentMethod.CASH, // افتراضياً نقدي
                    totalAmount, 
                    now, 
                    new ArrayList<>(selectedItems),
                    customInvoiceNumber, 
                    currentUser.getId()
                );
                
                // حفظ الفاتورة في قاعدة البيانات
                saveQuickInvoice(invoice);
            })
            .exceptionally(throwable -> {
                createInvoiceButton.setEnabled(true);
                createInvoiceButton.setText("إنشاء الفاتورة");
                Toast.makeText(getContext(), "خطأ في إنشاء رقم الفاتورة: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            });
    }
    
    private void saveQuickInvoice(Invoice invoice) {
        WriteBatch batch = db.batch();
        
        // إضافة الفاتورة
        DocumentReference invoiceRef = db.collection("invoices").document();
        invoice.setId(invoiceRef.getId());
        batch.set(invoiceRef, invoice);
        
        // تحديث مخزون المنتجات
        for (InvoiceItem item : selectedItems) {
            DocumentReference productRef = db.collection("products").document(item.getProductId());
            
            // العثور على المنتج الأصلي لتحديث الكمية
            for (Product product : productList) {
                if (product.getId().equals(item.getProductId())) {
                    int newQuantity = product.getQuantity() - item.getQuantity();
                    batch.update(productRef, "quantity", Math.max(0, newQuantity));
                    break;
                }
            }
        }
        
        // معالجة العميل إذا كان لديه معلومات
        if (!invoice.getCustomerName().equals("مجهول") && 
            !invoice.getCustomerPhone().isEmpty()) {
            
            // البحث عن العميل وإضافته إذا لم يكن موجوداً
            db.collection("customers")
                .whereEqualTo("phone", invoice.getCustomerPhone())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // إضافة عميل جديد
                        com.example.posapp.model.Customer newCustomer = 
                            new com.example.posapp.model.Customer(
                                invoice.getCustomerName(), 
                                invoice.getCustomerPhone()
                            );
                        
                        DocumentReference customerRef = db.collection("customers").document();
                        newCustomer.setId(customerRef.getId());
                        batch.set(customerRef, newCustomer);
                    }
                    
                    // حفظ جميع التغييرات
                    commitBatch(batch);
                })
                .addOnFailureListener(e -> {
                    // حفظ الفاتورة حتى لو فشل في معالجة العميل
                    commitBatch(batch);
                });
        } else {
            // حفظ الفاتورة بدون معلومات عميل
            commitBatch(batch);
        }
    }
    
    private void commitBatch(WriteBatch batch) {
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "✅ تم إنشاء الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                
                if (onInvoiceCreatedListener != null) {
                    onInvoiceCreatedListener.onInvoiceCreated();
                }
                
                dismiss();
            })
            .addOnFailureListener(e -> {
                createInvoiceButton.setEnabled(true);
                createInvoiceButton.setText("إنشاء الفاتورة");
                Toast.makeText(getContext(), "فشل في حفظ الفاتورة: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}