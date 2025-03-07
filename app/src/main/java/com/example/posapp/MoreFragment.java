package com.example.posapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class MoreFragment extends Fragment {
    private CardView customersManagementCard;
    private CardView productsManagementCard;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);
        
        customersManagementCard = view.findViewById(R.id.customersManagementCard);
        productsManagementCard = view.findViewById(R.id.productsManagementCard);
        
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
}
