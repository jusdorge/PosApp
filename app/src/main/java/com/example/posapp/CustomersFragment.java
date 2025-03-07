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

import com.example.posapp.model.Customer;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomersFragment extends Fragment implements CustomerAdapter.OnCustomerClickListener {
    private RecyclerView customersRecyclerView;
    private EditText searchCustomerEditText;
    private Button addCustomerButton;
    private TextView emptyCustomersTextView;

    private CustomerAdapter customerAdapter;
    private List<Customer> customerList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customers, container, false);

        customersRecyclerView = view.findViewById(R.id.customersRecyclerView);
        searchCustomerEditText = view.findViewById(R.id.searchCustomerEditText);
        addCustomerButton = view.findViewById(R.id.addCustomerButton);
        emptyCustomersTextView = view.findViewById(R.id.emptyCustomersTextView);

        // إعداد قائمة الزبائن
        customerList = new ArrayList<>();
        customerAdapter = new CustomerAdapter(customerList);
        customerAdapter.setOnCustomerClickListener(this);

        customersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        customersRecyclerView.setAdapter(customerAdapter);

        // إعداد Firestore
        db = FirebaseFirestore.getInstance();

        // تحميل الزبائن
        loadCustomers();

        // إعداد البحث
        searchCustomerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                customerAdapter.filterCustomers(s.toString());
                updateEmptyView();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // زر إضافة زبون جديد
        addCustomerButton.setOnClickListener(v -> {
            showAddCustomerDialog();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCustomers();
    }

    private void loadCustomers() {
        db.collection("customers")
            .orderBy("name")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                customerList.clear();
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Customer customer = document.toObject(Customer.class);
                    customer.setId(document.getId());
                    customerList.add(customer);
                }
                
                customerAdapter.updateCustomers(customerList);
                updateEmptyView();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في تحميل الزبائن: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                updateEmptyView();
            });
    }

    private void updateEmptyView() {
        if (customerAdapter.getItemCount() == 0) {
            customersRecyclerView.setVisibility(View.GONE);
            emptyCustomersTextView.setVisibility(View.VISIBLE);
            
            if (!searchCustomerEditText.getText().toString().isEmpty()) {
                emptyCustomersTextView.setText("لا توجد نتائج للبحث");
            } else {
                emptyCustomersTextView.setText("لا يوجد زبائن");
            }
        } else {
            customersRecyclerView.setVisibility(View.VISIBLE);
            emptyCustomersTextView.setVisibility(View.GONE);
        }
    }

    private void showAddCustomerDialog() {
        AddCustomerDialog dialog = new AddCustomerDialog();
        dialog.setOnCustomerAddedListener(customer -> {
            loadCustomers(); // إعادة تحميل القائمة بعد إضافة زبون
        });
        dialog.show(getChildFragmentManager(), "AddCustomerDialog");
    }

    @Override
    public void onCustomerClick(Customer customer, int position) {
        CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(customer.getId());
        dialog.show(getChildFragmentManager(), "CustomerDetailsDialog");
    }
} 