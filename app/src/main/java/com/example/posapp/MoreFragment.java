package com.example.posapp;

import android.content.Intent;
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

public class MoreFragment extends Fragment {
    private static final int QR_SCANNER_REQUEST_CODE = 1002;
    
    private CardView customersManagementCard;
    private CardView productsManagementCard;
    private CardView allInvoicesCard;
    private CardView qrScannerCard;
    
    private FirebaseFirestore db;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);
        
        db = FirebaseFirestore.getInstance();
        
        customersManagementCard = view.findViewById(R.id.customersManagementCard);
        productsManagementCard = view.findViewById(R.id.productsManagementCard);
        allInvoicesCard = view.findViewById(R.id.allInvoicesCard);
        qrScannerCard = view.findViewById(R.id.qrScannerCard);
        
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
