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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProductsManagementFragment extends Fragment {
    private RecyclerView productsRecyclerView;
    private EditText searchProductEditText;
    private Button addProductButton;
    private TextView emptyProductsTextView;

    private ProductManagementAdapter productAdapter;
    private List<Product> productList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products_management, container, false);

        productsRecyclerView = view.findViewById(R.id.productsRecyclerView);
        searchProductEditText = view.findViewById(R.id.searchProductEditText);
        addProductButton = view.findViewById(R.id.addProductButton);
        emptyProductsTextView = view.findViewById(R.id.emptyProductsTextView);

        // إعداد قائمة المنتجات
        productList = new ArrayList<>();
        productAdapter = new ProductManagementAdapter(productList);
        productAdapter.setOnProductEditListener(this::editProduct);

        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        productsRecyclerView.setAdapter(productAdapter);

        // إعداد Firestore
        db = FirebaseFirestore.getInstance();

        // تحميل المنتجات
        loadProducts();

        // إعداد البحث
        searchProductEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                productAdapter.filterProducts(s.toString());
                updateEmptyView();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // زر إضافة منتج جديد
        addProductButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddProductActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {
        db.collection("products")
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
                updateEmptyView();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في تحميل المنتجات: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                updateEmptyView();
            });
    }

    private void updateEmptyView() {
        if (productAdapter.getItemCount() == 0) {
            productsRecyclerView.setVisibility(View.GONE);
            emptyProductsTextView.setVisibility(View.VISIBLE);
            
            if (!searchProductEditText.getText().toString().isEmpty()) {
                emptyProductsTextView.setText("لا توجد نتائج للبحث");
            } else {
                emptyProductsTextView.setText("لا توجد منتجات");
            }
        } else {
            productsRecyclerView.setVisibility(View.VISIBLE);
            emptyProductsTextView.setVisibility(View.GONE);
        }
    }

    private void editProduct(Product product) {
        Intent intent = new Intent(getContext(), EditProductActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }
} 