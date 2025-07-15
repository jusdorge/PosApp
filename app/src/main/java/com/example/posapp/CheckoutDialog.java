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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.model.Customer;
import com.example.posapp.model.CustomerDebt;
import com.example.posapp.model.Invoice;
import com.example.posapp.model.InvoiceItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckoutDialog extends DialogFragment {
    private static final int CONTACT_PERMISSION_CODE = 100;
    private static final int CONTACT_PICK_CODE = 101;
    
    private List<InvoiceItem> invoiceItems;
    private double totalAmount;
    private OnInvoiceCompletedListener listener;
    
    private FirebaseFirestore db;
    private static TextInputEditText customerNameEditText;
    private static TextInputEditText phoneNumberEditText;

    private static Customer currentCustomer;
    public static void setCustomer(Customer newCurrentCustomer) {
        currentCustomer = newCurrentCustomer;
    }

    public interface OnInvoiceCompletedListener {
        void onInvoiceCompleted();
    }
    
    public static CheckoutDialog newInstance(List<InvoiceItem> items, double totalAmount) {
        CheckoutDialog dialog = new CheckoutDialog();
        dialog.invoiceItems = new ArrayList<>(items);
        dialog.totalAmount = totalAmount;
        return dialog;
    }
    
    public void setOnInvoiceCompletedListener(OnInvoiceCompletedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_checkout, null);
        
        // ربط عناصر الواجهة
        customerNameEditText = view.findViewById(R.id.customerNameEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        RadioGroup paymentMethodRadioGroup = view.findViewById(R.id.paymentMethodRadioGroup);
        TextView totalAmountTextView = view.findViewById(R.id.totalAmountTextView);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button confirmButton = view.findViewById(R.id.confirmButton);
        ImageButton contactPickerButton = view.findViewById(R.id.contactPickerButton);

        if (currentCustomer != null) {
            customerNameEditText.setText(currentCustomer.getName());
            phoneNumberEditText.setText(currentCustomer.getPhone());
        }
        // عرض المجموع
        totalAmountTextView.setText(String.format("%.2f ريال", totalAmount));
        
        // إضافة مستمع لزر اختيار جهات الاتصال
        contactPickerButton.setOnClickListener(v -> {
            // التحقق من إذن الوصول لجهات الاتصال
            if (checkContactPermission()) {
                // إذا كان الإذن ممنوحاً، فتح منتقي جهات الاتصال
                pickContact();
            } else {
                // طلب الإذن
                requestContactPermission();
            }
        });
        
        // مستمع الإلغاء
        cancelButton.setOnClickListener(v -> dismiss());
        
        // مستمع التأكيد
        confirmButton.setOnClickListener(v -> {
            String customerName = customerNameEditText.getText().toString().trim();
            String phoneNumber = phoneNumberEditText.getText().toString().trim();
            
            // تحقق من إدخال الاسم ورقم الهاتف
            if (TextUtils.isEmpty(customerName)) {
                customerNameEditText.setError("الرجاء إدخال اسم المشتري");
                return;
            }
            
            if (TextUtils.isEmpty(phoneNumber)) {
                phoneNumberEditText.setError("الرجاء إدخال رقم الهاتف");
                return;
            }
            
            // تحديد طريقة الدفع
            int selectedId = paymentMethodRadioGroup.getCheckedRadioButtonId();
            RadioButton selectedRadioButton = view.findViewById(selectedId);
            boolean isPaid = selectedRadioButton.getId() == R.id.cashRadioButton;
            
            // حفظ الفاتورة
            saveInvoice(customerName, phoneNumber, isPaid);
        });
        
        builder.setView(view);
        return builder.create();
    }
    
    // التحقق من إذن الوصول لجهات الاتصال
    private boolean checkContactPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED;
    }
    
    // طلب إذن الوصول لجهات الاتصال
    private void requestContactPermission() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                new String[]{Manifest.permission.READ_CONTACTS},
                CONTACT_PERMISSION_CODE
        );
    }
    
    // فتح منتقي جهات الاتصال
    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, CONTACT_PICK_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CONTACT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // تم منح الإذن، فتح منتقي جهات الاتصال
                pickContact();
            } else {
                Toast.makeText(getContext(), "تم رفض إذن الوصول لجهات الاتصال", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == CONTACT_PICK_CODE && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                Uri contactUri = data.getData();
                
                // استخراج الاسم
                String[] projection = {ContactsContract.Contacts.DISPLAY_NAME};
                Cursor cursor = requireActivity().getContentResolver().query(
                        contactUri,
                        projection,
                        null,
                        null,
                        null
                );
                
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    String name = cursor.getString(nameIndex);
                    customerNameEditText.setText(name);
                    
                    // استخراج رقم الهاتف
                    String contactId = contactUri.getLastPathSegment();
                    getContactPhone(contactId);
                    
                    cursor.close();
                }
            }
        }
    }
    
    // استخراج رقم هاتف جهة الاتصال
    private void getContactPhone(String contactId) {
        Cursor phoneCursor = requireActivity().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null
        );
        
        if (phoneCursor != null && phoneCursor.moveToFirst()) {
            int phoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String phoneNumber = phoneCursor.getString(phoneIndex);
            // إزالة المسافات والرموز غير الضرورية
            phoneNumber = phoneNumber.replaceAll("[\\s-()]", "");
            phoneNumberEditText.setText(phoneNumber);
            phoneCursor.close();
        }
    }
    
    private void saveInvoice(String customerName, String phoneNumber, boolean isPaid) {
        // إنشاء كائن الفاتورة
        Timestamp now = new Timestamp(new Date());
        Invoice invoice = new Invoice(customerName, phoneNumber, isPaid, totalAmount, now, invoiceItems);
        
        // البدء بعملية كتابة مجمعة (batch) لضمان اتساق البيانات
        WriteBatch batch = db.batch();
        
        // إضافة الفاتورة
        DocumentReference invoiceRef = db.collection("invoices").document();
        invoice.setId(invoiceRef.getId());
        batch.set(invoiceRef, invoice);
        
        // إذا كانت الفاتورة غير مدفوعة (دين)، أضف إلى العميل
        if (!isPaid) {
            // البحث عن العميل أولاً
            db.collection("customers")
                .whereEqualTo("phone", phoneNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // تحديث عملية الكتابة المجمعة
                    WriteBatch newBatch = db.batch();
                    newBatch.set(invoiceRef, invoice);
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        // إنشاء عميل جديد
                        Customer customer = new Customer(customerName, phoneNumber);
                        customer.addDebt(invoiceRef.getId(), totalAmount, now);
                        
                        DocumentReference customerRef = db.collection("customers").document();
                        customer.setId(customerRef.getId());
                        newBatch.set(customerRef, customer);
                    } else {
                        // تحديث العميل الموجود
                        DocumentReference customerRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                        
                        // إضافة الدين الجديد
                        CustomerDebt newDebt = new CustomerDebt(invoiceRef.getId(), totalAmount, now);
                        
                        // نحتاج لتحميل العميل بالكامل بدلاً من محاولة تحديث الديون مباشرة
                        Customer customer = queryDocumentSnapshots.getDocuments().get(0).toObject(Customer.class);
                        if (customer != null) {
                            // إضافة الدين الجديد إلى قائمة الديون
                            if (customer.getDebts() == null) {
                                customer.setDebts(new ArrayList<>());
                            }
                            customer.getDebts().add(newDebt);
                            customer.setTotalDebt(customer.getTotalDebt() + totalAmount);
                            
                            // تحديث العميل بالكامل
                            newBatch.set(customerRef, customer);
                        } else {
                            // في حالة حدوث خطأ في تحويل البيانات، نقوم بتحديث فقط المجموع الكلي للدين
                            newBatch.update(customerRef, "totalDebt", queryDocumentSnapshots.getDocuments().get(0).getDouble("totalDebt") + totalAmount);
                        }
                    }
                    
                    // تنفيذ عملية الكتابة المجمعة
                    commitBatchAndFinish(newBatch);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "حدث خطأ أثناء البحث عن العميل", Toast.LENGTH_SHORT).show();
                    // محاولة حفظ الفاتورة على الأقل
                    commitBatchAndFinish(batch);
                });
        } else {
            // إذا كان الدفع نقداً، فقط احفظ الفاتورة
            commitBatchAndFinish(batch);
        }
    }
    
    private void commitBatchAndFinish(WriteBatch batch) {
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "تم حفظ الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                // إعادة تعيين الزبون
                currentCustomer = null;
                customerNameEditText.setText("");
                phoneNumberEditText.setText("");
                if (listener != null) {
                    listener.onInvoiceCompleted();
                }
                dismiss();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في حفظ الفاتورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
} 