package com.example.posapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Customer;
import com.example.posapp.model.CustomerDebt;
import com.example.posapp.model.Invoice;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomerDetailsDialog extends DialogFragment implements AddPaymentDialog.OnPaymentAddedListener {
    private static final String ARG_CUSTOMER_ID = "customer_id";
    
    // Interface لإشعار الـ Fragment عند حذف العميل
    public interface OnCustomerDeletedListener {
        void onCustomerDeleted();
    }
    
    private static OnCustomerDeletedListener customerDeletedListener;
    
    public static void setOnCustomerDeletedListener(OnCustomerDeletedListener listener) {
        customerDeletedListener = listener;
    }

    private TextView customerNameTextView;
    private TextView customerPhoneTextView;
    private TextView totalDebtTextView;
    private RecyclerView debtsRecyclerView;
    private RecyclerView invoicesRecyclerView;
    private Button closeButton;
    private Button addPaymentButton;
    private Button selectCustomerButton;
    private TextView tvCustomerLocation;
    private Button btnShowLocationOnMap;
    private Button btnSetLocation;
    private Button deleteCustomerButton;
    private Button generateQRButton;
    private Double latitude = null;
    private Double longitude = null;

    private FirebaseFirestore db;
    private String customerId;
    private Customer currentCustomer;
    private CustomerTransactionsAdapter transactionsAdapter;
    private CustomerInvoicesAdapter invoicesAdapter;
    private List<Invoice> customerInvoices = new ArrayList<>();

    public static CustomerDetailsDialog newInstance(String customerId) {
        CustomerDetailsDialog fragment = new CustomerDetailsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        fragment.setArguments(args);
        return fragment;
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
        View view = inflater.inflate(R.layout.dialog_customer_details, null);

        customerNameTextView = view.findViewById(R.id.customerNameTextView);
        customerPhoneTextView = view.findViewById(R.id.customerPhoneTextView);
        totalDebtTextView = view.findViewById(R.id.totalDebtTextView);
        debtsRecyclerView = view.findViewById(R.id.debtsRecyclerView);
        invoicesRecyclerView = view.findViewById(R.id.invoicesRecyclerView);
        selectCustomerButton = view.findViewById(R.id.selectCustomerButton);
        closeButton = view.findViewById(R.id.closeButton);
        addPaymentButton = view.findViewById(R.id.addPaymentButton);
        tvCustomerLocation = view.findViewById(R.id.tv_customer_location);
        btnShowLocationOnMap = view.findViewById(R.id.btn_show_location_on_map);
        btnSetLocation = view.findViewById(R.id.btn_set_location);
        deleteCustomerButton = view.findViewById(R.id.deleteCustomerButton);
        generateQRButton = view.findViewById(R.id.generateQRButton);

        debtsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        invoicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        selectCustomerButton.setOnClickListener(v -> selectCustomerForInvoice());
        closeButton.setOnClickListener(v -> dismiss());
        addPaymentButton.setOnClickListener(v -> showAddPaymentDialog());
        btnShowLocationOnMap.setOnClickListener(v -> showLocationOnMap());
        btnSetLocation.setOnClickListener(v -> setCustomerLocation());
        deleteCustomerButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        generateQRButton.setOnClickListener(v -> showCustomerQRCode());

        builder.setView(view);

        loadCustomerDetails();

        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "onCreateDialog finished");
        
        return builder.create();
    }

    private void loadCustomerDetails() {
        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "loadCustomerDetails called with customerId: " + customerId);
        
        if (customerId == null) {
            Toast.makeText(getContext(), "خطأ: معرّف الزبون غير متوفر", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        DocumentReference customerRef = db.collection("customers").document(customerId);
        customerRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentCustomer = documentSnapshot.toObject(Customer.class);
                        if (currentCustomer != null) {
                            currentCustomer.setId(documentSnapshot.getId());
                            displayCustomerDetails(currentCustomer);
                        }
                    } else {
                        Toast.makeText(getContext(), "لم يتم العثور على الزبون", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل في تحميل بيانات الزبون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dismiss();
                });
        
        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "loadCustomerDetails finished");
    }

    private void displayCustomerDetails(Customer customer) {
        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "displayCustomerDetails called with: " + customer.getName());
        
        customerNameTextView.setText(customer.getName());
        customerPhoneTextView.setText(customer.getPhone());
        totalDebtTextView.setText(String.format("%.2f دج", customer.getTotalDebt()));

        latitude = customer.getLatitude();
        longitude = customer.getLongitude();
        if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            tvCustomerLocation.setText(String.format("%.4f, %.4f", latitude, longitude));
            btnShowLocationOnMap.setEnabled(true);
            btnSetLocation.setText("تحديث");
        } else {
            tvCustomerLocation.setText("غير محدد");
            btnShowLocationOnMap.setEnabled(false);
            btnSetLocation.setText("تحديد");
        }

        // تفعيل/تعطيل زر الدفع بناءً على وجود ديون
        addPaymentButton.setEnabled(customer.getTotalDebt() > 0);

        if (customer.getDebts() != null && !customer.getDebts().isEmpty()) {
            List<CustomerDebt> transactions = new ArrayList<>(customer.getDebts());

            // ترتيب المعاملات حسب التاريخ (الأحدث أولاً)
            Collections.sort(transactions, (t1, t2) -> {
                if (t1.getDate() == null || t2.getDate() == null) return 0;
                return t2.getDate().compareTo(t1.getDate());
            });

            transactionsAdapter = new CustomerTransactionsAdapter(getContext(), transactions);
            debtsRecyclerView.setAdapter(transactionsAdapter);
        } else {
            // إذا لم تكن هناك معاملات، عرض قائمة فارغة
            transactionsAdapter = new CustomerTransactionsAdapter(getContext(), new ArrayList<>());
            debtsRecyclerView.setAdapter(transactionsAdapter);
        }

        // نستدعي تحميل الفواتير هنا بعد تحميل بيانات العميل
        loadCustomerInvoices();
        
        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "displayCustomerDetails finished");
    }

    private void showAddPaymentDialog() {
        AddPaymentDialog dialog = AddPaymentDialog.newInstance(currentCustomer);
        dialog.setOnPaymentAddedListener(this);
        dialog.show(getChildFragmentManager(), "AddPaymentDialog");
    }

    @Override
    public void onPaymentAdded() {
        // إعادة تحميل بيانات الزبون بعد إضافة الدفعة
        loadCustomerDetails();
    }

    private void loadCustomerInvoices() {
        if (customerId == null || currentCustomer == null) {
            // إذا كان العميل غير متاح، نعرض قائمة فارغة
            invoicesAdapter = new CustomerInvoicesAdapter(getContext(), new ArrayList<>());
            invoicesRecyclerView.setAdapter(invoicesAdapter);
            return;
        }

        // استعلام Firestore للحصول على جميع الفواتير التي تتطابق مع رقم هاتف العميل
        db.collection("invoices")
                .whereEqualTo("customerPhone", currentCustomer.getPhone())
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    customerInvoices.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Invoice invoice = document.toObject(Invoice.class);
                        if (invoice != null) {
                            invoice.setId(document.getId());
                            customerInvoices.add(invoice);
                        }
                    }

                    // إنشاء المحول وتعيينه للقائمة
                    invoicesAdapter = new CustomerInvoicesAdapter(getContext(), customerInvoices);
                    invoicesAdapter.setOnInvoiceClickListener(this::showInvoiceDetails);
                    invoicesRecyclerView.setAdapter(invoicesAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل في تحميل الفواتير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showInvoiceDetails(Invoice invoice) {
        // هنا يمكنك فتح حوار لعرض تفاصيل الفاتورة المحددة
        Toast.makeText(getContext(), "تم اختيار الفاتورة: " + invoice.getId(), Toast.LENGTH_SHORT).show();
    }

    private void selectCustomerForInvoice() {
        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "selectCustomerForInvoice called");
        
        Customer newCurrentCustomer = new Customer();
        newCurrentCustomer.setId(customerId);
        newCurrentCustomer.setName(customerNameTextView.getText().toString());
        newCurrentCustomer.setPhone(customerPhoneTextView.getText().toString());
        
        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "Selecting customer: " + newCurrentCustomer.getName());
        
        CounterFragment.setCustomer(newCurrentCustomer);

        Toast.makeText(getContext(), "تم اختيار الزبون للفاتورة: " + customerNameTextView.getText(), Toast.LENGTH_SHORT).show();
        
        // Debug log
        android.util.Log.d("CustomerDetailsDialog", "selectCustomerForInvoice finished");
        
        dismiss();
    }

    private void showLocationOnMap() {
        if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            Intent intent = new Intent(getActivity(), SelectCustomerLocationActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("view_only", true);
            startActivity(intent);
        }
    }

    private void setCustomerLocation() {
        Intent intent = new Intent(getActivity(), SelectCustomerLocationActivity.class);
        if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
        }
        startActivityForResult(intent, 1001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            double newLatitude = data.getDoubleExtra("latitude", 0);
            double newLongitude = data.getDoubleExtra("longitude", 0);
            
            if (newLatitude != 0 && newLongitude != 0) {
                // تحديث الموقع في قاعدة البيانات
                db.collection("customers").document(customerId)
                    .update("latitude", newLatitude, "longitude", newLongitude)
                    .addOnSuccessListener(aVoid -> {
                        latitude = newLatitude;
                        longitude = newLongitude;
                        tvCustomerLocation.setText(String.format("%.4f, %.4f", latitude, longitude));
                        btnShowLocationOnMap.setEnabled(true);
                        btnSetLocation.setText("تحديث");
                        Toast.makeText(getContext(), "تم تحديث موقع العميل", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "فشل تحديث الموقع: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        if (currentCustomer == null) return;
        
        new AlertDialog.Builder(requireContext())
                .setTitle("تأكيد الحذف")
                .setMessage("هل أنت متأكد من حذف العميل \"" + currentCustomer.getName() + "\"؟\n\n" +
                           "سيتم حذف:\n" +
                           "• جميع بيانات العميل\n" +
                           "• جميع فواتير العميل\n" +
                           "• سجل المعاملات\n\n" +
                           "هذا الإجراء لا يمكن التراجع عنه!")
                .setPositiveButton("حذف", (dialog, which) -> deleteCustomer())
                .setNegativeButton("إلغاء", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteCustomer() {
        if (currentCustomer == null) return;
        
        // عرض مؤشر التحميل
        deleteCustomerButton.setEnabled(false);
        deleteCustomerButton.setText("جاري الحذف...");
        
        // حذف جميع فواتير العميل أولاً
        db.collection("invoices")
                .whereEqualTo("customerPhone", currentCustomer.getPhone())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // حذف جميع الفواتير
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        db.collection("invoices").document(document.getId()).delete();
                    }
                    
                    // بعد حذف الفواتير، احذف العميل
                    db.collection("customers").document(customerId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "تم حذف العميل \"" + currentCustomer.getName() + "\" بنجاح", Toast.LENGTH_SHORT).show();
                                dismiss();
                                
                                // إشعار الأنشطة الأخرى بحذف العميل
                                notifyCustomerDeleted();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "فشل في حذف العميل: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                deleteCustomerButton.setEnabled(true);
                                deleteCustomerButton.setText("حذف العميل");
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل في حذف فواتير العميل: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    deleteCustomerButton.setEnabled(true);
                    deleteCustomerButton.setText("حذف العميل");
                });
    }

    private void notifyCustomerDeleted() {
        // إذا كان هذا العميل مختاراً في CounterFragment، قم بإلغاء اختياره
        if (CounterFragment.activeInstance != null) {
            CounterFragment.setCustomer(null);
        }
        // إشعار الـ Fragment الذي قام بإظهار هذا الحوار عند حذف العميل
        if (customerDeletedListener != null) {
            customerDeletedListener.onCustomerDeleted();
        }
    }

    private void showCustomerQRCode() {
        if (currentCustomer == null) {
            Toast.makeText(getContext(), "لا توجد معلومات عميل", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrData = generateCustomerQRData();
        Bitmap qrBitmap = generateQRCode(qrData);

        if (qrBitmap != null) {
            showQRCodeDialog(qrBitmap);
        } else {
            Toast.makeText(getContext(), "فشل في إنشاء QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateCustomerQRData() {
        StringBuilder qrData = new StringBuilder();
        
        qrData.append("CUSTOMER_INFO\n");
        qrData.append("Name: ").append(currentCustomer.getName() != null ? currentCustomer.getName() : "N/A").append("\n");
        qrData.append("Phone: ").append(currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "N/A").append("\n");
        qrData.append("Total_Debt: ").append(String.format("%.2f DZD", currentCustomer.getTotalDebt())).append("\n");
        
        if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            qrData.append("Location: ").append(String.format("%.6f,%.6f", latitude, longitude)).append("\n");
        }
        
        qrData.append("Customer_ID: ").append(currentCustomer.getId() != null ? currentCustomer.getId() : "N/A");
        
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

    private void showQRCodeDialog(Bitmap qrBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        
        ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(qrBitmap);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setPadding(20, 20, 20, 20);
        
        builder.setTitle("QR Code للعميل: " + currentCustomer.getName())
                .setView(imageView)
                .setPositiveButton("شاشة الطباعة", (dialog, which) -> openPrintScreen(qrBitmap))
                .setNeutralButton("مشاركة", (dialog, which) -> shareQRCode(qrBitmap))
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void shareQRCode(Bitmap qrBitmap) {
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
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Code للعميل: " + currentCustomer.getName());
                shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "QR Code للعميل: " + currentCustomer.getName() + "\n" +
                    "الهاتف: " + (currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "غير محدد") + "\n" +
                    "إجمالي الدين: " + String.format("%.2f دج", currentCustomer.getTotalDebt()));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                // التحقق من وجود تطبيقات للمشاركة
                if (shareIntent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(Intent.createChooser(shareIntent, "مشاركة QR Code"));
                } else {
                    Toast.makeText(getContext(), "لا توجد تطبيقات للمشاركة", Toast.LENGTH_SHORT).show();
                }
                
            } catch (Exception fileProviderException) {
                // طريقة بديلة: مشاركة النص فقط
                shareQRCodeAsText();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "فشل في مشاركة QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void printQRCode(Bitmap qrBitmap) {
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
        printIntent.putExtra("customer_name", currentCustomer.getName());
        printIntent.putExtra("customer_phone", currentCustomer.getPhone());
        printIntent.putExtra("customer_debt", currentCustomer.getTotalDebt());
        
        startActivity(printIntent);
    }

    private void openPrintScreen(Bitmap qrBitmap) {
        Intent printIntent = new Intent(getContext(), QRCodePrintActivity.class);
        
        // تحويل Bitmap إلى byte array للإرسال
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        
        printIntent.putExtra("qr_bitmap", byteArray);
        printIntent.putExtra("customer_name", currentCustomer.getName());
        printIntent.putExtra("customer_phone", currentCustomer.getPhone());
        printIntent.putExtra("customer_debt", currentCustomer.getTotalDebt());
        
        startActivity(printIntent);
    }

    private void shareQRCodeAsText() {
        try {
            String qrData = generateCustomerQRData();
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "معلومات العميل - QR Code");
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "معلومات العميل:\n" +
                "الاسم: " + currentCustomer.getName() + "\n" +
                "الهاتف: " + (currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "غير محدد") + "\n" +
                "إجمالي الدين: " + String.format("%.2f دج", currentCustomer.getTotalDebt()) + "\n\n" +
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