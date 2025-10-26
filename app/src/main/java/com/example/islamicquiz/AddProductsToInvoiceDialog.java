package com.example.islamicquiz;

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

import com.example.islamicquiz.model.Invoice;
import com.example.islamicquiz.model.InvoiceItem;
import com.example.islamicquiz.model.Product;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class AddProductsToInvoiceDialog extends DialogFragment implements InvoiceItemAdapter.OnProductAddListener {
    
    private static final String ARG_INVOICE = "invoice";
    
    private RecyclerView productsRecyclerView;
    private EditText searchProductEditText;
    private TextView invoiceInfoTextView;
    private TextView selectedProductsTextView;
    private TextView additionalAmountTextView;
    private TextView newTotalTextView;
    private Button addProductsButton;
    private Button cancelButton;
    
    private InvoiceItemAdapter productAdapter;
    private List<Product> productList;
    private List<InvoiceItem> newSelectedItems;
    private double additionalAmount = 0.0;
    
    private Invoice originalInvoice;
    private FirebaseFirestore db;
    private OnProductsAddedListener onProductsAddedListener;
    
    public interface OnProductsAddedListener {
        void onProductsAdded(Invoice updatedInvoice);
    }
    
    public static AddProductsToInvoiceDialog newInstance(Invoice invoice) {
        AddProductsToInvoiceDialog fragment = new AddProductsToInvoiceDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_INVOICE, invoice);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setOnProductsAddedListener(OnProductsAddedListener listener) {
        this.onProductsAddedListener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            originalInvoice = (Invoice) getArguments().getSerializable(ARG_INVOICE);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_products_to_invoice, container, false);
        
        // ربط العناصر
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView);
        searchProductEditText = view.findViewById(R.id.searchProductEditText);
        invoiceInfoTextView = view.findViewById(R.id.invoiceInfoTextView);
        selectedProductsTextView = view.findViewById(R.id.selectedProductsTextView);
        additionalAmountTextView = view.findViewById(R.id.additionalAmountTextView);
        newTotalTextView = view.findViewById(R.id.newTotalTextView);
        addProductsButton = view.findViewById(R.id.addProductsButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        
        // إعداد المتغيرات
        productList = new ArrayList<>();
        newSelectedItems = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        
        // إعداد RecyclerView
        productAdapter = new InvoiceItemAdapter(productList, this);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        productsRecyclerView.setAdapter(productAdapter);
        
        // إعداد معلومات الفاتورة
        setupInvoiceInfo();
        
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
        addProductsButton.setOnClickListener(v -> addProductsToInvoice());
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
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 1.0);
            dialog.getWindow().setLayout(width, height);
        }
    }
    
    private void setupInvoiceInfo() {
        if (originalInvoice != null) {
            String invoiceInfo = "فاتورة: " + originalInvoice.getDisplayNumber() + "\n" +
                               "العميل: " + originalInvoice.getCustomerName() + "\n" +
                               "المجموع الحالي: " + CurrencyUtils.formatCurrency(originalInvoice.getTotalAmount()) + "\n" +
                               "عدد المنتجات: " + (originalInvoice.getItems() != null ? originalInvoice.getItems().size() : 0);
            invoiceInfoTextView.setText(invoiceInfo);
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
        // البحث عن المنتج في القائمة المختارة الجديدة
        boolean found = false;
        for (InvoiceItem item : newSelectedItems) {
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
            newSelectedItems.add(newItem);
        }
        
        // تحديث العرض
        updateSelectedItems();
        
        Toast.makeText(getContext(), "✅ تم إضافة " + product.getName(), Toast.LENGTH_SHORT).show();
    }
    
    private void updateSelectedItems() {
        // حساب المجموع الإضافي
        additionalAmount = 0.0;
        for (InvoiceItem item : newSelectedItems) {
            additionalAmount += item.getQuantity() * item.getPrice();
        }
        
        // تحديث النصوص
        selectedProductsTextView.setText("المنتجات الجديدة: " + newSelectedItems.size());
        additionalAmountTextView.setText("مبلغ إضافي: " + CurrencyUtils.formatCurrency(additionalAmount));
        
        // عرض المجموع الجديد
        if (originalInvoice != null && newTotalTextView != null) {
            double newTotal = originalInvoice.getTotalAmount() + additionalAmount;
            newTotalTextView.setText("المجموع الجديد: " + CurrencyUtils.formatCurrency(newTotal));
        }
        
        // تفعيل/تعطيل زر الإضافة
        addProductsButton.setEnabled(!newSelectedItems.isEmpty());
    }
    
    private void addProductsToInvoice() {
        if (newSelectedItems.isEmpty()) {
            Toast.makeText(getContext(), "الرجاء اختيار منتج واحد على الأقل", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (originalInvoice == null) {
            Toast.makeText(getContext(), "خطأ: بيانات الفاتورة الأصلية مفقودة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // إظهار مؤشر التحميل
        addProductsButton.setEnabled(false);
        addProductsButton.setText("جاري الإضافة...");
        
        // إنشاء الفاتورة المحدثة
        Invoice updatedInvoice = createUpdatedInvoice();
        
        // حفظ التحديثات في قاعدة البيانات
        saveUpdatedInvoice(updatedInvoice);
    }
    
    private Invoice createUpdatedInvoice() {
        // نسخ الفاتورة الأصلية
        List<InvoiceItem> allItems = new ArrayList<>();
        if (originalInvoice.getItems() != null) {
            allItems.addAll(originalInvoice.getItems());
        }
        
        // دمج المنتجات الجديدة مع الموجودة
        for (InvoiceItem newItem : newSelectedItems) {
            boolean found = false;
            
            // البحث عن نفس المنتج في الفاتورة الأصلية
            for (InvoiceItem existingItem : allItems) {
                if (existingItem.getProductId().equals(newItem.getProductId())) {
                    // زيادة الكمية إذا كان المنتج موجوداً
                    existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
                    found = true;
                    break;
                }
            }
            
            // إضافة منتج جديد إذا لم يكن موجوداً
            if (!found) {
                allItems.add(newItem);
            }
        }
        
        // إنشاء فاتورة محدثة
        Invoice updatedInvoice = new Invoice(
            originalInvoice.getCustomerName(),
            originalInvoice.getCustomerPhone(),
            originalInvoice.getPaymentMethod(),
            originalInvoice.getTotalAmount() + additionalAmount,
            originalInvoice.getDate(),
            allItems,
            originalInvoice.getCustomInvoiceNumber(),
            originalInvoice.getCreatedByUserId()
        );
        
        updatedInvoice.setId(originalInvoice.getId());
        updatedInvoice.setPaid(originalInvoice.isPaid());
        
        return updatedInvoice;
    }
    
    private void saveUpdatedInvoice(Invoice updatedInvoice) {
        WriteBatch batch = db.batch();
        
        // تحديث الفاتورة
        DocumentReference invoiceRef = db.collection("invoices").document(updatedInvoice.getId());
        batch.set(invoiceRef, updatedInvoice);
        
        // تحديث مخزون المنتجات (فقط للمنتجات الجديدة)
        for (InvoiceItem newItem : newSelectedItems) {
            DocumentReference productRef = db.collection("products").document(newItem.getProductId());
            
            // العثور على المنتج الأصلي لتحديث الكمية
            for (Product product : productList) {
                if (product.getId().equals(newItem.getProductId())) {
                    int newQuantity = product.getQuantity() - newItem.getQuantity();
                    batch.update(productRef, "quantity", Math.max(0, newQuantity));
                    break;
                }
            }
        }
        
        // حفظ جميع التغييرات
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "✅ تم إضافة المنتجات للفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                
                if (onProductsAddedListener != null) {
                    onProductsAddedListener.onProductsAdded(updatedInvoice);
                }
                
                dismiss();
            })
            .addOnFailureListener(e -> {
                addProductsButton.setEnabled(true);
                addProductsButton.setText("إضافة المنتجات");
                Toast.makeText(getContext(), "فشل في إضافة المنتجات: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}