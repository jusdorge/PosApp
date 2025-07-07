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
import android.widget.TextView;
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

import java.util.HashMap;
import java.util.Map;

public class AddCustomerDialog extends DialogFragment {
    private static final int CONTACT_PERMISSION_CODE = 1001;
    private static final int CONTACT_PICK_CODE = 1002;
    private static final int LOCATION_PICK_CODE = 2001;

    private TextInputEditText customerNameEditText;
    private TextInputEditText phoneNumberEditText;
    private ImageButton contactPickerButton;
    private Button btnSelectLocation;
    private TextView tvSelectedLocation;
    private OnCustomerAddedListener listener;

    private FirebaseFirestore db;
    private Double latitude = null;
    private Double longitude = null;

    // لحفظ حالة الDialog
    private String tempName = "";
    private String tempPhone = "";

    public interface OnCustomerAddedListener {
        void onCustomerAdded(Customer customer);
    }

    public void setOnCustomerAddedListener(OnCustomerAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // الاحتفاظ بالحالة عند تغيير الاتجاه
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_customer, null);

        initializeViews(view);
        setupListeners();

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // استعادة البيانات المؤقتة إذا كانت موجودة
        if (!TextUtils.isEmpty(tempName)) {
            customerNameEditText.setText(tempName);
        }
        if (!TextUtils.isEmpty(tempPhone)) {
            phoneNumberEditText.setText(tempPhone);
        }

        return dialog;
    }

    private void initializeViews(View view) {
        customerNameEditText = view.findViewById(R.id.customerNameEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        contactPickerButton = view.findViewById(R.id.contactPickerButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button saveButton = view.findViewById(R.id.saveButton);
        btnSelectLocation = view.findViewById(R.id.btn_select_location);
        tvSelectedLocation = view.findViewById(R.id.tv_selected_location);

        cancelButton.setOnClickListener(v -> dismiss());
        saveButton.setOnClickListener(v -> saveCustomer());
    }

    private void setupListeners() {
        contactPickerButton.setOnClickListener(v -> {
            if (hasContactPermission()) {
                openContactPicker();
            } else {
                requestContactPermission();
            }
        });

        btnSelectLocation.setOnClickListener(v -> {
            // حفظ البيانات المؤقتة قبل فتح شاشة الموقع
            tempName = customerNameEditText.getText().toString();
            tempPhone = phoneNumberEditText.getText().toString();

            Intent intent = new Intent(getActivity(), SelectCustomerLocationActivity.class);
            // إرسال الموقع الحالي إذا كان موجودًا
            if (latitude != null && longitude != null) {
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
            }
            startActivityForResult(intent, LOCATION_PICK_CODE);
        });
    }

    private void saveCustomer() {
        String name = customerNameEditText.getText().toString().trim();
        String phone = phoneNumberEditText.getText().toString().trim();

        if (!validateInput(name, phone)) {
            return;
        }

        // إظهار مؤشر التحميل
        showProgressDialog();

        // التحقق من وجود الرقم مسبقاً
        db.collection("customers")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    dismissProgressDialog();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        phoneNumberEditText.setError("هذا الرقم مسجل مسبقاً");
                    } else {
                        addNewCustomer(name, phone);
                    }
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Toast.makeText(getContext(), "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInput(String name, String phone) {
        if (TextUtils.isEmpty(name)) {
            customerNameEditText.setError("الرجاء إدخال اسم الزبون");
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneNumberEditText.setError("الرجاء إدخال رقم الهاتف");
            return false;
        }

        // التحقق من صحة رقم الهاتف
        if (phone.length() < 9) {
            phoneNumberEditText.setError("رقم الهاتف غير صحيح");
            return false;
        }

        return true;
    }

    private void addNewCustomer(String name, String phone) {
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("name", name);
        customerData.put("phone", phone);
        customerData.put("totalDebt", 0.0);

        if (latitude != null && longitude != null) {
            customerData.put("latitude", latitude);
            customerData.put("longitude", longitude);
        }

        db.collection("customers")
                .add(customerData)
                .addOnSuccessListener(documentReference -> {
                    Customer customer = new Customer(name, phone);
                    customer.setId(documentReference.getId());
                    if (latitude != null && longitude != null) {
                        customer.setLatitude(latitude);
                        customer.setLongitude(longitude);
                    }

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
            handleContactSelection(data);
        }

        if (requestCode == LOCATION_PICK_CODE && resultCode == requireActivity().RESULT_OK && data != null) {
            handleLocationSelection(data);
        }
    }

    private void handleContactSelection(Intent data) {
        Uri contactUri = data.getData();
        if (contactUri == null) return;

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };

        try (Cursor cursor = requireActivity().getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                String phoneNumber = cursor.getString(numberIndex);
                String contactName = cursor.getString(nameIndex);

                // تنظيف رقم الهاتف
                phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");

                customerNameEditText.setText(contactName);
                phoneNumberEditText.setText(phoneNumber);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "خطأ في قراءة جهة الاتصال", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLocationSelection(Intent data) {
        latitude = data.getDoubleExtra("latitude", 0);
        longitude = data.getDoubleExtra("longitude", 0);

        if (latitude != 0 && longitude != 0) {
            tvSelectedLocation.setText(String.format("الموقع: %.4f, %.4f", latitude, longitude));
            btnSelectLocation.setText("تغيير الموقع");
        }
    }

    // dialog تحميل بسيط
    private AlertDialog progressDialog;

    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(R.layout.dialog_progress);
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // حفظ البيانات المؤقتة
        if (customerNameEditText != null) {
            tempName = customerNameEditText.getText().toString();
        }
        if (phoneNumberEditText != null) {
            tempPhone = phoneNumberEditText.getText().toString();
        }
    }
}