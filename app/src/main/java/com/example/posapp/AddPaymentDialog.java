package com.example.posapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.model.Customer;
import com.example.posapp.model.CustomerDebt;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddPaymentDialog extends DialogFragment {
    private static final String ARG_CUSTOMER = "customer";
    
    private TextInputEditText paymentAmountEditText;
    private TextInputEditText paymentDescriptionEditText;
    private TextView customerInfoTextView;
    private Button savePaymentButton;
    private Button cancelPaymentButton;
    
    private Customer customer;
    private FirebaseFirestore db;
    private OnPaymentAddedListener listener;
    
    public interface OnPaymentAddedListener {
        void onPaymentAdded();
    }
    
    public static AddPaymentDialog newInstance(Customer customer) {
        AddPaymentDialog fragment = new AddPaymentDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CUSTOMER, customer);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setOnPaymentAddedListener(OnPaymentAddedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customer = (Customer) getArguments().getSerializable(ARG_CUSTOMER);
        }
        db = FirebaseFirestore.getInstance();
    }
    
    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_payment, null);
        
        paymentAmountEditText = view.findViewById(R.id.paymentAmountEditText);
        paymentDescriptionEditText = view.findViewById(R.id.paymentDescriptionEditText);
        customerInfoTextView = view.findViewById(R.id.customerInfoTextView);
        savePaymentButton = view.findViewById(R.id.savePaymentButton);
        cancelPaymentButton = view.findViewById(R.id.cancelPaymentButton);
        
        if (customer != null) {
            customerInfoTextView.setText(String.format("%s - إجمالي الدين: %.2f دج", 
                    customer.getName(), customer.getTotalDebt()));
        }
        
        cancelPaymentButton.setOnClickListener(v -> dismiss());
        
        savePaymentButton.setOnClickListener(v -> {
            if (validateInput()) {
                savePayment();
            }
        });
        
        builder.setView(view);
        return builder.create();
    }
    
    private boolean validateInput() {
        String amountString = paymentAmountEditText.getText().toString().trim();
        if (TextUtils.isEmpty(amountString)) {
            paymentAmountEditText.setError("الرجاء إدخال مبلغ الدفع");
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountString);
            if (amount <= 0) {
                paymentAmountEditText.setError("يجب أن يكون المبلغ أكبر من صفر");
                return false;
            }
            
            if (amount > customer.getTotalDebt()) {
                paymentAmountEditText.setError("المبلغ أكبر من إجمالي الدين");
                return false;
            }
        } catch (NumberFormatException e) {
            paymentAmountEditText.setError("مبلغ غير صالح");
            return false;
        }
        
        return true;
    }
    
    private void savePayment() {
        double amount = Double.parseDouble(paymentAmountEditText.getText().toString().trim());
        String description = paymentDescriptionEditText.getText().toString().trim();
        if (TextUtils.isEmpty(description)) {
            description = "دفعة";
        }
        
        CustomerDebt payment = new CustomerDebt(amount, description, Timestamp.now(), true);
        
        DocumentReference customerRef = db.collection("customers").document(customer.getId());
        
        // إضافة المدفوعات إلى قائمة معاملات العميل
        if (customer.getDebts() == null) {
            customer.setDebts(new ArrayList<>());
        }
        customer.getDebts().add(payment);
        
        // تحديث إجمالي الدين
        double newTotalDebt = customer.getTotalDebt() - amount;
        customer.setTotalDebt(newTotalDebt);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("debts", customer.getDebts());
        updates.put("totalDebt", newTotalDebt);
        
        customerRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "تمت إضافة الدفعة بنجاح", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onPaymentAdded();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل في إضافة الدفعة: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
} 