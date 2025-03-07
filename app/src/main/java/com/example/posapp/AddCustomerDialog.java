package com.example.posapp;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.model.Customer;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddCustomerDialog extends DialogFragment {
    private static final int CONTACT_PERMISSION_CODE = 1001;
    private static final int CONTACT_PICK_CODE = 1002;
    
    private TextInputEditText customerNameEditText;
    private TextInputEditText phoneNumberEditText;
    private ImageButton contactPickerButton;
    private OnCustomerAddedListener listener;

    private FirebaseFirestore db;

    public interface OnCustomerAddedListener {
        void onCustomerAdded(Customer customer);
    }

    public void setOnCustomerAddedListener(OnCustomerAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_customer, null);

        customerNameEditText = view.findViewById(R.id.customerNameEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        contactPickerButton = view.findViewById(R.id.contactPickerButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button saveButton = view.findViewById(R.id.saveButton);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        contactPickerButton.setOnClickListener(v -> {
            // التحقق من صلاحيات الوصول إلى جهات الاتصال
            if (hasContactPermission()) {
                // فتح منتقي جهات الاتصال
                openContactPicker();
            } else {
                // طلب صلاحية الوصول إلى جهات الاتصال
                requestContactPermission();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> {
            String name = customerNameEditText.getText().toString().trim();
            String phone = phoneNumberEditText.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                customerNameEditText.setError("الرجاء إدخال اسم الزبون");
                return;
            }

            if (TextUtils.isEmpty(phone)) {
                phoneNumberEditText.setError("الرجاء إدخال رقم الهاتف");
                return;
            }

            // التحقق من وجود الرقم مسبقاً
            db.collection("customers")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        phoneNumberEditText.setError("هذا الرقم مسجل مسبقاً");
                    } else {
                        // إضافة زبون جديد
                        Customer customer = new Customer(name, phone);
                        
                        db.collection("customers")
                            .add(customer)
                            .addOnSuccessListener(documentReference -> {
                                customer.setId(documentReference.getId());
                                Toast.makeText(getContext(), "تمت إضافة الزبون بنجاح", Toast.LENGTH_SHORT).show();
                                
                                if (listener != null) {
                                    listener.onCustomerAdded(customer);
                                }
                                
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "فشل في إضافة الزبون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });

        return dialog;
    }

    private boolean hasContactPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.READ_CONTACTS},
                CONTACT_PERMISSION_CODE);
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, CONTACT_PICK_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CONTACT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // تم منح الصلاحية
                openContactPicker();
            } else {
                Toast.makeText(requireContext(), "تم رفض صلاحية الوصول إلى جهات الاتصال", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == CONTACT_PICK_CODE && resultCode == requireActivity().RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            
            // استخراج رقم الهاتف
            String[] projection = {
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            };
            
            Cursor cursor = requireActivity().getContentResolver().query(contactUri, projection, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                // استخراج رقم الهاتف واسم جهة الاتصال
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                
                String phoneNumber = cursor.getString(numberIndex);
                String contactName = cursor.getString(nameIndex);
                
                // تنظيف رقم الهاتف (إزالة الرموز والفراغات)
                phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                
                // تعبئة الحقول
                customerNameEditText.setText(contactName);
                phoneNumberEditText.setText(phoneNumber);
                
                cursor.close();
            }
        }
    }
} 