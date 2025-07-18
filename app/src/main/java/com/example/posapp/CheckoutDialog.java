package com.example.posapp;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Customer;
import com.example.posapp.model.CustomerDebt;
import com.example.posapp.model.Invoice;
import com.example.posapp.model.InvoiceItem;
import com.example.posapp.model.Product;
import com.example.posapp.model.StockMovement;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckoutDialog extends DialogFragment implements CustomerSearchAdapter.OnCustomerSelectListener {
    private static final int CONTACT_PERMISSION_CODE = 100;
    private static final int CONTACT_PICK_CODE = 101;

    public CheckoutDialog() {
        super();

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

    }

    private List<InvoiceItem> invoiceItems;
    private double totalAmount;
    private OnInvoiceCompletedListener listener;

    private FirebaseFirestore db;
    private TextInputEditText customerNameEditText;
    private TextInputEditText phoneNumberEditText;
    private RecyclerView searchResultsRecyclerView;
    private TextView noResultsTextView;
    private Button addNewCustomerButton;
    private Button searchContactsButton;

    private CustomerSearchAdapter searchAdapter;
    private List<Customer> searchResults;
    private Customer selectedCustomer;

    public interface OnInvoiceCompletedListener {
        void onInvoiceCompleted();
    }

    // Interface لإشعار الأجزاء الأخرى عند إضافة فاتورة جديدة
    public interface OnInvoiceAddedListener {
        void onInvoiceAdded();
    }
    
    private static OnInvoiceAddedListener invoiceAddedListener;
    
    public static void setOnInvoiceAddedListener(OnInvoiceAddedListener listener) {
        invoiceAddedListener = listener;
    }

    public static CheckoutDialog newInstance(List<InvoiceItem> items, double totalAmount) {
        CheckoutDialog dialog = new CheckoutDialog();
        dialog.invoiceItems = new ArrayList<>(items);
        dialog.totalAmount = totalAmount;
        

        return dialog;
    }

    public static CheckoutDialog newInstance(List<InvoiceItem> items, double totalAmount, Customer customer) {
        CheckoutDialog dialog = new CheckoutDialog();
        dialog.invoiceItems = new ArrayList<>(items);
        dialog.totalAmount = totalAmount;
        dialog.selectedCustomer = customer;
        

        return dialog;
    }

    public void setOnInvoiceCompletedListener(OnInvoiceCompletedListener listener) {
        this.listener = listener;
    }

    public void setSelectedCustomer(Customer customer) {
        selectedCustomer = customer;
        
        // تحديث الحقول إذا كانت الواجهة جاهزة
        if (customerNameEditText != null && phoneNumberEditText != null) {
            updateCustomerFields();
            hideSearchResults();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        searchResults = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_checkout_search, null);

        // ربط عناصر الواجهة
        customerNameEditText = view.findViewById(R.id.customerNameEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        noResultsTextView = view.findViewById(R.id.noResultsTextView);
        addNewCustomerButton = view.findViewById(R.id.addNewCustomerButton);
        searchContactsButton = view.findViewById(R.id.searchContactsButton);
        RadioGroup paymentMethodRadioGroup = view.findViewById(R.id.paymentMethodRadioGroup);
        TextView totalAmountTextView = view.findViewById(R.id.totalAmountTextView);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button confirmButton = view.findViewById(R.id.confirmButton);

        // إعداد RecyclerView للبحث
        searchAdapter = new CustomerSearchAdapter(searchResults);
        searchAdapter.setOnCustomerSelectListener(this);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setAdapter(searchAdapter);

        // إذا كان هناك عميل محدد مسبقاً
        if (selectedCustomer != null) {
            updateCustomerFields();
            hideSearchResults();
        } else {
            // إذا لم يكن هناك عميل محدد، تحقق من CounterFragment
            Customer currentCustomer = CounterFragment.getCurrentCustomer();
            if (currentCustomer != null) {
                selectedCustomer = currentCustomer;
                updateCustomerFields();
                hideSearchResults();
            }
        }

        // عرض المجموع
        totalAmountTextView.setText(String.format("%.2f ريال", totalAmount));

        // إضافة مستمع للبحث في حقل الاسم
        customerNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    searchCustomers(s.toString());
                } else {
                    hideSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // إضافة مستمع للبحث في حقل رقم الهاتف
        phoneNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    searchCustomersByPhone(s.toString());
                } else if (customerNameEditText.getText().toString().isEmpty()) {
                    hideSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // زر إضافة عميل جديد
        addNewCustomerButton.setOnClickListener(v -> {
            showAddCustomerDialog();
        });

        // زر البحث في جهات الاتصال
        searchContactsButton.setOnClickListener(v -> {
            if (checkContactPermission()) {
                pickContact();
            } else {
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

    @Override
    public void onStart() {
        super.onStart();
        
        // تحديث الحقول مرة أخرى في حالة لم تكن جاهزة من قبل
        if (selectedCustomer != null) {
            updateCustomerFields();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // تحديث الحقول مرة أخرى في حالة لم تكن جاهزة من قبل
        if (selectedCustomer != null) {
            updateCustomerFields();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // تحديث الحقول بعد أن تصبح الـ views جاهزة
        if (selectedCustomer != null) {
            updateCustomerFields();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // تنظيف المتغيرات عند إغلاق الـ dialog
        selectedCustomer = null;
        if (searchResults != null) {
            searchResults.clear();
        }
        

    }

    private void searchCustomers(String query) {
        db.collection("customers")
                .orderBy("name")
                .startAt(query)
                .endAt(query + '\uf8ff')
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    searchResults.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Customer customer = doc.toObject(Customer.class);
                        if (customer != null) {
                            customer.setId(doc.getId());
                            searchResults.add(customer);
                        }
                    }
                    updateSearchUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "خطأ في البحث", Toast.LENGTH_SHORT).show();
                });
    }

    private void searchCustomersByPhone(String query) {
        db.collection("customers")
                .orderBy("phone")
                .startAt(query)
                .endAt(query + '\uf8ff')
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    searchResults.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Customer customer = doc.toObject(Customer.class);
                        if (customer != null) {
                            customer.setId(doc.getId());
                            searchResults.add(customer);
                        }
                    }
                    updateSearchUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "خطأ في البحث", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSearchUI() {
        if (searchResults.isEmpty()) {
            searchResultsRecyclerView.setVisibility(View.GONE);
            noResultsTextView.setVisibility(View.VISIBLE);
            addNewCustomerButton.setVisibility(View.VISIBLE);
            searchContactsButton.setVisibility(View.VISIBLE);
        } else {
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
            noResultsTextView.setVisibility(View.GONE);
            addNewCustomerButton.setVisibility(View.GONE);
            searchContactsButton.setVisibility(View.GONE);
            searchAdapter.notifyDataSetChanged();
        }
    }

    private void hideSearchResults() {
        searchResultsRecyclerView.setVisibility(View.GONE);
        noResultsTextView.setVisibility(View.GONE);
        addNewCustomerButton.setVisibility(View.GONE);
        searchContactsButton.setVisibility(View.GONE);
    }

    @Override
    public void onCustomerSelected(Customer customer) {
        selectedCustomer = customer;
        updateCustomerFields();
        hideSearchResults();
    }

    private void updateCustomerFields() {
        if (selectedCustomer != null && customerNameEditText != null && phoneNumberEditText != null) {
            String customerName = selectedCustomer.getName();
            String customerPhone = selectedCustomer.getPhone();
            
            customerNameEditText.setText(customerName);
            phoneNumberEditText.setText(customerPhone);
        }
    }

    private void showAddCustomerDialog() {
        AddCustomerDialog dialog = new AddCustomerDialog();
        dialog.setOnCustomerAddedListener(customer -> {
            selectedCustomer = customer;
            updateCustomerFields();
            hideSearchResults();
        });
        dialog.show(getChildFragmentManager(), "AddCustomerDialog");
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

        // تحديث المخزون لكل منتج في الفاتورة
        updateInventoryForInvoice(batch, invoiceRef.getId(), invoiceItems);

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
                    if (listener != null) {
                        listener.onInvoiceCompleted();
                    }
                    if (invoiceAddedListener != null) {
                        invoiceAddedListener.onInvoiceAdded();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل في حفظ الفاتورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateInventoryForInvoice(WriteBatch batch, String invoiceId, List<InvoiceItem> items) {
        // تحديث المخزون بشكل متزامن
        for (InvoiceItem item : items) {
            // جلب المنتج الحالي وتحديث الكمية
            DocumentReference productRef = db.collection("products").document(item.getProductId());
            
            // سنقوم بتحديث الكمية مباشرة دون انتظار النتيجة
            // في عملية منفصلة لتجنب مشاكل التزامن
            updateProductQuantity(item, invoiceId);
        }
    }

    private void updateProductQuantity(InvoiceItem item, String invoiceId) {
        DocumentReference productRef = db.collection("products").document(item.getProductId());
        
        productRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Product product = documentSnapshot.toObject(Product.class);
                if (product != null) {
                    int currentQuantity = product.getQuantity();
                    int newQuantity = currentQuantity - item.getQuantity();
                    
                    // تحديث كمية المنتج
                    productRef.update("quantity", newQuantity)
                        .addOnSuccessListener(aVoid -> {
                            // تسجيل حركة المخزون بعد نجاح التحديث
                            recordStockMovement(item, invoiceId, currentQuantity, newQuantity);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "فشل في تحديث المخزون للمنتج: " + item.getProductName(), Toast.LENGTH_SHORT).show();
                        });
                }
            }
        });
    }

    private void recordStockMovement(InvoiceItem item, String invoiceId, int quantityBefore, int quantityAfter) {
        DocumentReference stockMovementRef = db.collection("stock_movements").document();
        StockMovement stockMovement = new StockMovement(
            item.getProductId(),
            item.getProductName(),
            item.getQuantity(),
            StockMovement.MOVEMENT_OUT,
            StockMovement.REASON_SALE,
            invoiceId,
            "system", // يمكن تحديد المستخدم الحالي
            "بيع - فاتورة رقم: " + invoiceId,
            quantityBefore,
            quantityAfter
        );
        stockMovement.setId(stockMovementRef.getId());
        
        stockMovementRef.set(stockMovement)
            .addOnFailureListener(e -> {
                // في حالة فشل تسجيل حركة المخزون، يمكن إضافة رسالة تحذير
                // لكن لا نريد إيقاف العملية الأساسية
                android.util.Log.w("CheckoutDialog", "فشل في تسجيل حركة المخزون: " + e.getMessage());
            });
    }
}