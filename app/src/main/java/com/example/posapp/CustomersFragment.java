package com.example.posapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

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

public class CustomersFragment extends Fragment implements CustomerAdapter.OnCustomerClickListener, CustomerAdapter.OnQRCodeClickListener {
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
        customerAdapter.setOnQRCodeClickListener(this);

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // تسجيل مستمع حذف العملاء
        CustomerDetailsDialog.setOnCustomerDeletedListener(() -> {
            // إعادة تحميل قائمة العملاء عند حذف عميل
            loadCustomers();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // إلغاء تسجيل المستمع
        CustomerDetailsDialog.setOnCustomerDeletedListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        // تنظيف حقل البحث عند العودة للشاشة
        if (searchCustomerEditText != null) {
            searchCustomerEditText.setText("");
        }
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
        // Debug log
        android.util.Log.d("CustomersFragment", "onCustomerClick called with customer: " + customer.getName());
        
        CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(customer.getId());
        dialog.show(getChildFragmentManager(), "CustomerDetailsDialog");
        
        // Debug log
        android.util.Log.d("CustomersFragment", "onCustomerClick finished");
    }

    @Override
    public void onQRCodeClick(Customer customer) {
        showCustomerQRCode(customer);
    }

    private void showCustomerQRCode(Customer customer) {
        String qrData = generateCustomerQRData(customer);
        Bitmap qrBitmap = generateQRCode(qrData);

        if (qrBitmap != null) {
            showQRCodeDialog(customer, qrBitmap);
        } else {
            Toast.makeText(getContext(), "فشل في إنشاء QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateCustomerQRData(Customer customer) {
        StringBuilder qrData = new StringBuilder();
        
        qrData.append("CUSTOMER_INFO\n");
        qrData.append("Name: ").append(customer.getName() != null ? customer.getName() : "N/A").append("\n");
        qrData.append("Phone: ").append(customer.getPhone() != null ? customer.getPhone() : "N/A").append("\n");
        qrData.append("Total_Debt: ").append(String.format("%.2f DZD", customer.getTotalDebt())).append("\n");
        qrData.append("Customer_ID: ").append(customer.getId() != null ? customer.getId() : "N/A");
        
        return qrData.toString();
    }

    private Bitmap generateQRCode(String data) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300, hints);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showQRCodeDialog(Customer customer, Bitmap qrBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(qrBitmap);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setPadding(20, 20, 20, 20);
        
        builder.setTitle("QR Code للعميل: " + customer.getName())
                .setView(imageView)
                .setPositiveButton("شاشة الطباعة", (dialog, which) -> openPrintScreen(customer, qrBitmap))
                .setNeutralButton("مشاركة", (dialog, which) -> shareQRCode(customer, qrBitmap))
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void shareQRCode(Customer customer, Bitmap qrBitmap) {
        try {
            if (qrBitmap == null) {
                Toast.makeText(getContext(), "QR Code غير متوفر", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // إنشاء مجلد الصور في الذاكرة المؤقتة
            java.io.File cachePath = new java.io.File(getContext().getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            
            // حفظ QR Code في الذاكرة المؤقتة
            String fileName = "customer_qr_" + System.currentTimeMillis() + ".png";
            java.io.File file = new java.io.File(cachePath, fileName);
            
            java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            
            // التحقق من وجود الملف
            if (!file.exists()) {
                Toast.makeText(getContext(), "فشل في حفظ QR Code", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                // إنشاء URI للملف
                android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                        getContext(), 
                        "com.example.posapp.fileprovider", 
                        file);
                
                // إنشاء Intent للمشاركة
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Code للعميل: " + customer.getName());
                shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "QR Code للعميل: " + customer.getName() + "\n" +
                    "الهاتف: " + (customer.getPhone() != null ? customer.getPhone() : "غير محدد") + "\n" +
                    "إجمالي الدين: " + String.format("%.2f دج", customer.getTotalDebt()));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                // التحقق من وجود تطبيقات للمشاركة
                if (shareIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(Intent.createChooser(shareIntent, "مشاركة QR Code"));
                } else {
                    Toast.makeText(getContext(), "لا توجد تطبيقات للمشاركة", Toast.LENGTH_SHORT).show();
                }
                
            } catch (Exception fileProviderException) {
                // طريقة بديلة: مشاركة النص فقط
                shareQRCodeAsText(customer);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "فشل في مشاركة QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void printQRCode(Customer customer, Bitmap qrBitmap) {
        if (qrBitmap == null) {
            Toast.makeText(getContext(), "QR Code غير متوفر", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // إنشاء Intent لطباعة QR Code
        Intent printIntent = new Intent(getContext(), QRCodePrintActivity.class);
        
        // تحويل Bitmap إلى byte array للإرسال
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        
        printIntent.putExtra("qr_bitmap", byteArray);
        printIntent.putExtra("customer_name", customer.getName());
        printIntent.putExtra("customer_phone", customer.getPhone());
        printIntent.putExtra("customer_debt", customer.getTotalDebt());
        
        startActivity(printIntent);
    }

    private void openPrintScreen(Customer customer, Bitmap qrBitmap) {
        Intent printIntent = new Intent(getContext(), QRCodePrintActivity.class);
        
        // تحويل Bitmap إلى byte array للإرسال
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        
        printIntent.putExtra("qr_bitmap", byteArray);
        printIntent.putExtra("customer_name", customer.getName());
        printIntent.putExtra("customer_phone", customer.getPhone());
        printIntent.putExtra("customer_debt", customer.getTotalDebt());
        
        startActivity(printIntent);
    }

    private void shareQRCodeAsText(Customer customer) {
        try {
            String qrData = generateCustomerQRData(customer);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "معلومات العميل - QR Code");
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "معلومات العميل:\n" +
                "الاسم: " + customer.getName() + "\n" +
                "الهاتف: " + (customer.getPhone() != null ? customer.getPhone() : "غير محدد") + "\n" +
                "إجمالي الدين: " + String.format("%.2f دج", customer.getTotalDebt()) + "\n\n" +
                "QR Code Data:\n" + qrData);
            
            if (shareIntent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(Intent.createChooser(shareIntent, "مشاركة معلومات العميل"));
            } else {
                Toast.makeText(getContext(), "لا توجد تطبيقات للمشاركة", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "فشل في مشاركة معلومات العميل", Toast.LENGTH_SHORT).show();
        }
    }
} 