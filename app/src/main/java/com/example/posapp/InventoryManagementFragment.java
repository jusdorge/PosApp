package com.example.posapp;

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

public class InventoryManagementFragment extends Fragment {
    private RecyclerView inventoryRecyclerView;
    private EditText searchInventoryEditText;
    private TextView totalProductsTextView;
    private TextView lowStockAlertTextView;
    private Button viewStockMovementsButton;
    private Button exportInventoryButton;

    private InventoryAdapter inventoryAdapter;
    private List<Product> inventoryList;
    private List<Product> filteredInventoryList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory_management, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        inventoryList = new ArrayList<>();
        filteredInventoryList = new ArrayList<>();

        // Initialize UI components
        initViews(view);
        setupRecyclerView();
        setupSearchListener();
        setupButtons();

        // Load inventory data
        loadInventoryData();

        return view;
    }

    private void initViews(View view) {
        inventoryRecyclerView = view.findViewById(R.id.inventoryRecyclerView);
        searchInventoryEditText = view.findViewById(R.id.searchInventoryEditText);
        totalProductsTextView = view.findViewById(R.id.totalProductsTextView);
        lowStockAlertTextView = view.findViewById(R.id.lowStockAlertTextView);
        viewStockMovementsButton = view.findViewById(R.id.viewStockMovementsButton);
        exportInventoryButton = view.findViewById(R.id.exportInventoryButton);
    }

    private void setupRecyclerView() {
        inventoryAdapter = new InventoryAdapter(filteredInventoryList);
        inventoryAdapter.setOnStockAdjustmentListener(this::showStockAdjustmentDialog);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        inventoryRecyclerView.setAdapter(inventoryAdapter);
    }

    private void setupSearchListener() {
        searchInventoryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterInventory(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        viewStockMovementsButton.setOnClickListener(v -> {
            // Navigate to Stock Movements Fragment
            StockMovementsFragment stockMovementsFragment = new StockMovementsFragment();
            getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, stockMovementsFragment)
                .addToBackStack(null)
                .commit();
        });

        exportInventoryButton.setOnClickListener(v -> {
            exportInventoryReport();
        });
    }

    private void loadInventoryData() {
        db.collection("products")
            .orderBy("name")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                inventoryList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Product product = document.toObject(Product.class);
                    product.setId(document.getId());
                    inventoryList.add(product);
                }
                
                filteredInventoryList.clear();
                filteredInventoryList.addAll(inventoryList);
                inventoryAdapter.notifyDataSetChanged();
                
                updateInventorySummary();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في تحميل بيانات المخزون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void filterInventory(String query) {
        filteredInventoryList.clear();
        if (query.isEmpty()) {
            filteredInventoryList.addAll(inventoryList);
        } else {
            for (Product product : inventoryList) {
                if (product.getName().toLowerCase().contains(query.toLowerCase()) ||
                    product.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                    product.getBarcode().toLowerCase().contains(query.toLowerCase())) {
                    filteredInventoryList.add(product);
                }
            }
        }
        inventoryAdapter.notifyDataSetChanged();
        updateInventorySummary();
    }

    private void updateInventorySummary() {
        int totalProducts = filteredInventoryList.size();
        int lowStockCount = 0;
        
        for (Product product : filteredInventoryList) {
            if (product.isLowStock()) {
                lowStockCount++;
            }
        }
        
        totalProductsTextView.setText("إجمالي المنتجات: " + totalProducts);
        
        if (lowStockCount > 0) {
            lowStockAlertTextView.setText("تحذير: " + lowStockCount + " منتج بكمية منخفضة");
            lowStockAlertTextView.setVisibility(View.VISIBLE);
        } else {
            lowStockAlertTextView.setVisibility(View.GONE);
        }
    }

    private void showStockAdjustmentDialog(Product product) {
        StockAdjustmentDialog dialog = StockAdjustmentDialog.newInstance(product);
        dialog.setOnStockAdjustedListener(() -> {
            loadInventoryData(); // Reload data after adjustment
        });
        dialog.show(getChildFragmentManager(), "StockAdjustmentDialog");
    }

    private void exportInventoryReport() {
        // Generate and export inventory report
        StringBuilder report = new StringBuilder();
        report.append("تقرير المخزون\n");
        report.append("==================\n\n");
        
        for (Product product : inventoryList) {
            report.append("المنتج: ").append(product.getName()).append("\n");
            report.append("الكمية: ").append(product.getQuantity()).append("\n");
            report.append("الحد الأدنى: ").append(product.getMinQuantity()).append("\n");
            report.append("الحالة: ").append(product.isLowStock() ? "منخفض" : "عادي").append("\n");
            report.append("قيمة المخزون: ").append(product.getQuantity() * product.getCostPrice()).append(" دج\n");
            report.append("==================\n");
        }
        
        // Here you can implement file export functionality
        // For now, show a toast
        Toast.makeText(getContext(), "تم إنشاء التقرير بنجاح", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInventoryData();
    }
} 