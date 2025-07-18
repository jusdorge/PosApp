package com.example.posapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.posapp.model.InvoiceItem;
import com.example.posapp.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ItemsFragment extends Fragment implements InvoiceItemAdapter.OnProductAddListener {
    private RecyclerView productsRecyclerView;
    private EditText searchProductEdit;
    private ImageButton scanBarcodeButton;
    private LinearLayout addProductSection;
    private TextView noResultsTextView;
    private Button addNewProductButton;
    
    private InvoiceItemAdapter productAdapter;
    
    private List<Product> productList;
    private List<Product> filteredProductList;
    
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_items, container, false);

        db = FirebaseFirestore.getInstance();
        productList = new ArrayList<>();
        filteredProductList = new ArrayList<>();

        // ربط العناصر من واجهة المستخدم
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView);
        searchProductEdit = view.findViewById(R.id.searchProductEdit);
        scanBarcodeButton = view.findViewById(R.id.scanBarcodeButton);
        addProductSection = view.findViewById(R.id.addProductSection);
        noResultsTextView = view.findViewById(R.id.noResultsTextView);
        addNewProductButton = view.findViewById(R.id.addNewProductButton);

        // إعداد محول المنتجات
        productAdapter = new InvoiceItemAdapter(filteredProductList, this);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        productsRecyclerView.setAdapter(productAdapter);

        // إعداد مستمع البحث
        searchProductEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // إعداد مستمع زر الباركود
        scanBarcodeButton.setOnClickListener(v -> {
            // هنا يمكن إضافة وظيفة مسح الباركود
            Toast.makeText(getContext(), "سيتم تنفيذ وظيفة مسح الباركود لاحقاً", Toast.LENGTH_SHORT).show();
        });

        // إعداد مستمع زر إضافة منتوج جديد
        addNewProductButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddProductActivity.class);
            startActivity(intent);
        });

        // إعداد مستمع النقر على شريط البحث
        searchProductEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && searchProductEdit.getText().toString().trim().isEmpty()) {
                showAddProductSection("أدخل اسم المنتج للبحث أو إضافة منتوج جديد");
            }
        });

        // تحميل المنتجات
        loadProducts();

        return view;
    }

    private void loadProducts() {
        db.collection("products").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && isAdded()) {
                productList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Product product = document.toObject(Product.class);
                    productList.add(product);
                }
                filteredProductList.clear();
                filteredProductList.addAll(productList);
                productAdapter.notifyDataSetChanged();
            } else if (isAdded()) {
                Toast.makeText(getContext(), "فشل في تحميل المنتجات", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterProducts(String query) {
        filteredProductList.clear();
        if (query.isEmpty()) {
            filteredProductList.addAll(productList);
            hideAddProductSection();
        } else {
            boolean foundResults = false;
            for (Product product : productList) {
                if (product.getName().toLowerCase().contains(query.toLowerCase())){
                    filteredProductList.add(product);
                    foundResults = true;
                }
            }
            
            // إظهار قسم إضافة المنتوج الجديد إذا لم توجد نتائج
            if (!foundResults && !query.trim().isEmpty()) {
                showAddProductSection("لا توجد نتائج للبحث عن \"" + query + "\"");
            } else {
                hideAddProductSection();
            }
        }
        productAdapter.notifyDataSetChanged();
    }

    private void showAddProductSection(String message) {
        noResultsTextView.setText(message);
        addProductSection.setVisibility(View.VISIBLE);
    }

    private void hideAddProductSection() {
        addProductSection.setVisibility(View.GONE);
    }

    @Override
    public void onProductAdd(Product product, int quantity) {
        // إنشاء عنصر فاتورة جديد
        InvoiceItem invoiceItem = new InvoiceItem(
                product.getId(),
                product.getName(),
                product.getDefaultPrice(),
                quantity
        );
        
        // إضافة المنتج إلى الفاتورة في CounterFragment
        CounterFragment.addToInvoice(invoiceItem);
        
        // إظهار رسالة للمستخدم
        Toast.makeText(getContext(), "تمت إضافة " + quantity + " من " + product.getName() + " إلى الفاتورة", Toast.LENGTH_SHORT).show();
        
        // التبديل إلى CounterFragment لعرض الفاتورة (اختياري)
        ((MainActivity) requireActivity()).switchToCounterFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        // تنظيف حقل البحث عند العودة للشاشة
        if (searchProductEdit != null) {
            searchProductEdit.setText("");
        }
        // إخفاء قسم إضافة المنتج
        if (addProductSection != null) {
            addProductSection.setVisibility(View.GONE);
        }
        // إعادة تحميل المنتجات عند العودة من AddProductActivity
        loadProducts();
    }
}
