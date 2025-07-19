package com.example.posapp;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Customer;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SelectCustomerDialog extends DialogFragment {
    private EditText searchEditText;
    private RecyclerView customersRecyclerView;
    private Button addNewCustomerButton;
    private Button cancelButton;
    
    private FirebaseFirestore db;
    private CustomerAdapter customerAdapter;
    private List<Customer> allCustomers = new ArrayList<>();
    private OnCustomerSelectedListener listener;

    public interface OnCustomerSelectedListener {
        void onCustomerSelected(Customer customer);
    }

    public void setOnCustomerSelectedListener(OnCustomerSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_select_customer, null);

        searchEditText = view.findViewById(R.id.searchEditText);
        customersRecyclerView = view.findViewById(R.id.customersRecyclerView);
        addNewCustomerButton = view.findViewById(R.id.addNewCustomerButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // إعداد RecyclerView
        customersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        customerAdapter = new CustomerAdapter(new ArrayList<>());
        customerAdapter.setOnCustomerClickListener((customer, position) -> {
            if (listener != null) {
                listener.onCustomerSelected(customer);
            }
            dismiss();
        });
        customersRecyclerView.setAdapter(customerAdapter);

        // إعداد البحث
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                customerAdapter.filterCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // زر إضافة عميل جديد
        addNewCustomerButton.setOnClickListener(v -> {
            AddCustomerDialog addDialog = new AddCustomerDialog();
            addDialog.setOnCustomerAddedListener(customer -> {
                allCustomers.add(customer);
                customerAdapter.updateCustomers(allCustomers);
                if (listener != null) {
                    listener.onCustomerSelected(customer);
                }
                dismiss();
            });
            addDialog.show(getChildFragmentManager(), "AddCustomerDialog");
        });

        cancelButton.setOnClickListener(v -> dismiss());

        // تسجيل مستمع حذف العملاء
        CustomerDetailsDialog.setOnCustomerDeletedListener(() -> {
            // إعادة تحميل قائمة العملاء عند حذف عميل
            loadCustomers();
        });

        // تحميل العملاء من قاعدة البيانات
        loadCustomers();

        return dialog;
    }

    private void loadCustomers() {
        db.collection("customers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allCustomers.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Customer customer = document.toObject(Customer.class);
                        customer.setId(document.getId());
                        allCustomers.add(customer);
                    }
                    customerAdapter.updateCustomers(allCustomers);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل في تحميل العملاء: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
} 