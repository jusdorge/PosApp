package com.example.posapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.model.Customer;
import com.example.posapp.utils.DialogUtils;
import com.example.posapp.utils.LocationUtils;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditCustomerDialog extends DialogFragment {
    private static final String ARG_CUSTOMER_ID = "customer_id";
    
    public interface OnCustomerUpdatedListener {
        void onCustomerUpdated();
    }
    
    private OnCustomerUpdatedListener listener;
    
    private EditText nameEditText;
    private EditText phoneEditText;
    private EditText emailEditText;
    private EditText addressEditText;
    private EditText notesEditText;
    private TextView locationTextView;
    private Button setLocationButton;
    private Button showLocationButton;
    private Button saveButton;
    private Button cancelButton;
    
    private FirebaseFirestore db;
    private String customerId;
    private Customer currentCustomer;
    private double latitude = 0.0;
    private double longitude = 0.0;
    
    public static EditCustomerDialog newInstance(String customerId) {
        EditCustomerDialog fragment = new EditCustomerDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setOnCustomerUpdatedListener(OnCustomerUpdatedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            customerId = getArguments().getString(ARG_CUSTOMER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_customer, null);
        
        initViews(view);
        setupClickListeners();
        loadCustomerData();
        
        builder.setView(view);
        builder.setTitle("تحرير بيانات العميل");
        
        return builder.create();
    }
    
    private void initViews(View view) {
        nameEditText = view.findViewById(R.id.nameEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        emailEditText = view.findViewById(R.id.emailEditText);
        addressEditText = view.findViewById(R.id.addressEditText);
        notesEditText = view.findViewById(R.id.notesEditText);
        locationTextView = view.findViewById(R.id.locationTextView);
        setLocationButton = view.findViewById(R.id.setLocationButton);
        showLocationButton = view.findViewById(R.id.showLocationButton);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
    }
    
    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveCustomer());
        cancelButton.setOnClickListener(v -> dismiss());
        setLocationButton.setOnClickListener(v -> setCustomerLocation());
        showLocationButton.setOnClickListener(v -> showLocationOnMap());
    }
    
    private void loadCustomerData() {
        if (customerId == null) {
            DialogUtils.showToastSafely(this, "خطأ: معرّف العميل غير متوفر");
            DialogUtils.dismissSafely(this);
            return;
        }
        
        db.collection("customers").document(customerId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentCustomer = documentSnapshot.toObject(Customer.class);
                    if (currentCustomer != null) {
                        currentCustomer.setId(documentSnapshot.getId());
                        displayCustomerData();
                    }
                } else {
                    DialogUtils.showToastSafely(this, "لم يتم العثور على العميل");
                    DialogUtils.dismissSafely(this);
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("EditCustomerDialog", "فشل في تحميل بيانات العميل", e);
                DialogUtils.showToastSafely(this, "فشل في تحميل بيانات العميل: " + e.getMessage());
                DialogUtils.dismissSafely(this);
            });
    }
    
    private void displayCustomerData() {
        nameEditText.setText(currentCustomer.getName() != null ? currentCustomer.getName() : "");
        phoneEditText.setText(currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "");
        emailEditText.setText(currentCustomer.getEmail() != null ? currentCustomer.getEmail() : "");
        addressEditText.setText(currentCustomer.getAddress() != null ? currentCustomer.getAddress() : "");
        notesEditText.setText(currentCustomer.getNotes() != null ? currentCustomer.getNotes() : "");
        
        latitude = currentCustomer.getLatitude();
        longitude = currentCustomer.getLongitude();
        
        updateLocationDisplay();
    }
    
    private void updateLocationDisplay() {
        if (latitude != 0.0 && longitude != 0.0) {
            locationTextView.setText(String.format(java.util.Locale.US, "%.4f, %.4f", latitude, longitude));
            showLocationButton.setEnabled(true);
            setLocationButton.setText(getString(R.string.update_location));
        } else {
            locationTextView.setText(getString(R.string.customer_location_not_set));
            showLocationButton.setEnabled(false);
            setLocationButton.setText(getString(R.string.set_location));
        }
    }
    
    private void saveCustomer() {
        // التحقق من البيانات الأساسية
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("اسم العميل مطلوب");
            nameEditText.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError("رقم الهاتف مطلوب");
            phoneEditText.requestFocus();
            return;
        }
        
        // تحضير البيانات المحدثة
        String email = emailEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        
        // تعطيل زر الحفظ أثناء الحفظ
        saveButton.setEnabled(false);
        saveButton.setText(getString(R.string.saving_data));
        
        // تحديث البيانات في Firestore
        db.collection("customers").document(customerId)
            .update(
                "name", name,
                "phone", phone,
                "email", email.isEmpty() ? null : email,
                "address", address.isEmpty() ? null : address,
                "notes", notes.isEmpty() ? null : notes,
                "latitude", latitude,
                "longitude", longitude
            )
            .addOnSuccessListener(aVoid -> {
                DialogUtils.showToastSafely(this, "تم تحديث بيانات العميل بنجاح");
                
                // إشعار المستمع
                if (listener != null) {
                    listener.onCustomerUpdated();
                }
                
                DialogUtils.dismissSafely(this);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("EditCustomerDialog", "فشل في تحديث بيانات العميل", e);
                DialogUtils.showToastSafely(this, "فشل في تحديث بيانات العميل: " + e.getMessage(), Toast.LENGTH_LONG);
                saveButton.setEnabled(true);
                saveButton.setText(getString(R.string.save));
            });
    }
    
    private void setCustomerLocation() {
        Intent intent = new Intent(getActivity(), SelectCustomerLocationActivity.class);
        if (latitude != 0.0 && longitude != 0.0) {
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
        }
        startActivityForResult(intent, 1001);
    }
    
    private void showLocationOnMap() {
        if (latitude != 0.0 && longitude != 0.0) {
            LocationUtils.openGoogleMaps(getContext(), latitude, longitude, currentCustomer.getName());
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            latitude = data.getDoubleExtra("latitude", 0);
            longitude = data.getDoubleExtra("longitude", 0);
            updateLocationDisplay();
        }
    }
}