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
import com.example.posapp.model.OperationLog;
import com.example.posapp.service.OperationLogService;
import com.example.posapp.model.Product;
import com.example.posapp.model.PaymentMethod;
import com.example.posapp.model.StockMovement;
import com.example.posapp.model.User;
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
    private RadioGroup paymentMethodRadioGroup;
    private TextView totalAmountTextView;
    private Button cancelButton;
    private Button confirmButton;

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
        paymentMethodRadioGroup = view.findViewById(R.id.paymentMethodRadioGroup);
        totalAmountTextView = view.findViewById(R.id.totalAmountTextView);
        cancelButton = view.findViewById(R.id.cancelButton);
        confirmButton = view.findViewById(R.id.confirmButton);

        // إضافة طرق الدفع برمجياً
        addPaymentMethodOptions(paymentMethodRadioGroup);

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
        totalAmountTextView.setText(CurrencyUtils.formatCurrency(totalAmount));

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
            if (selectedId == -1) {
                Toast.makeText(getContext(), "الرجاء اختيار طريقة الدفع", Toast.LENGTH_SHORT).show();
                return;
            }
            
            PaymentMethod selectedPaymentMethod = getSelectedPaymentMethod(selectedId);

            // حفظ الفاتورة
            saveInvoice(customerName, phoneNumber, selectedPaymentMethod);
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

    private void saveInvoice(String customerName, String phoneNumber, PaymentMethod selectedPaymentMethod) {
        // الحصول على المستخدم الحالي
        UserSession userSession = UserSession.getInstance(getContext());
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(getContext(), "خطأ: لا يمكن تحديد المستخدم الحالي", Toast.LENGTH_SHORT).show();
            return;
        }

        // فحص ما إذا كان يتم تعديل فاتورة موجودة
        String loadedInvoiceId = CounterFragment.getLoadedInvoiceId();
        boolean isEditingExisting = CounterFragment.isEditingExistingInvoice();
        
        if (isEditingExisting && loadedInvoiceId != null) {
            // تحديث الفاتورة الموجودة
            updateExistingInvoice(loadedInvoiceId, customerName, phoneNumber, selectedPaymentMethod);
        } else {
            // إنشاء فاتورة جديدة
            createNewInvoice(customerName, phoneNumber, selectedPaymentMethod, currentUser);
        }
    }
    
    private void createNewInvoice(String customerName, String phoneNumber, PaymentMethod selectedPaymentMethod, User currentUser) {
        // إنشاء رقم الفاتورة المخصص
        InvoiceNumberGenerator numberGenerator = new InvoiceNumberGenerator(getContext());
        numberGenerator.generateInvoiceNumber(currentUser)
            .thenAccept(customInvoiceNumber -> {
                // إنشاء كائن الفاتورة مع الرقم المخصص
                Timestamp now = new Timestamp(new Date());
                Invoice invoice = new Invoice(customerName, phoneNumber, selectedPaymentMethod, 
                    totalAmount, now, invoiceItems, customInvoiceNumber, currentUser.getId());

                // البدء بعملية كتابة مجمعة (batch) لضمان اتساق البيانات
                WriteBatch batch = db.batch();

                // إضافة الفاتورة
                DocumentReference invoiceRef = db.collection("invoices").document();
                invoice.setId(invoiceRef.getId());
                batch.set(invoiceRef, invoice);
                
                // متابعة عملية الحفظ
                continueInvoiceSaving(batch, invoice, selectedPaymentMethod, phoneNumber, customerName);
            })
            .exceptionally(throwable -> {
                android.util.Log.e("CheckoutDialog", "Error generating invoice number", throwable);
                Toast.makeText(getContext(), "خطأ في إنشاء رقم الفاتورة: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            });
    }
    
    private void updateExistingInvoice(String invoiceId, String customerName, String phoneNumber, PaymentMethod selectedPaymentMethod) {
        // تحميل الفاتورة الموجودة أولاً
        db.collection("invoices").document(invoiceId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Invoice existingInvoice = documentSnapshot.toObject(Invoice.class);
                    if (existingInvoice != null) {
                        // تحديث بيانات الفاتورة
                        existingInvoice.setCustomerName(customerName);
                        existingInvoice.setCustomerPhone(phoneNumber);
                        existingInvoice.setPaymentMethod(selectedPaymentMethod);
                        existingInvoice.setTotalAmount(totalAmount);
                        existingInvoice.setItems(invoiceItems);
                        existingInvoice.setDate(new Timestamp(new Date())); // تحديث تاريخ التعديل
                        
                        // بدء عملية التحديث
                        WriteBatch batch = db.batch();
                        DocumentReference invoiceRef = db.collection("invoices").document(invoiceId);
                        batch.set(invoiceRef, existingInvoice);
                        
                        // متابعة عملية الحفظ
                        continueInvoiceSaving(batch, existingInvoice, selectedPaymentMethod, phoneNumber, customerName);
                        
                        Toast.makeText(getContext(), "🔄 جاري تحديث الفاتورة الموجودة...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "❌ الفاتورة غير موجودة، سيتم إنشاء فاتورة جديدة", Toast.LENGTH_SHORT).show();
                    // إنشاء فاتورة جديدة إذا لم توجد الأصلية
                    UserSession userSession = UserSession.getInstance(getContext());
                    createNewInvoice(customerName, phoneNumber, selectedPaymentMethod, userSession.getCurrentUser());
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "❌ خطأ في تحميل الفاتورة: " + e.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("CheckoutDialog", "Error loading existing invoice", e);
            });
    }
    
    private void continueInvoiceSaving(WriteBatch batch, Invoice invoice, PaymentMethod selectedPaymentMethod, 
                                     String phoneNumber, String customerName) {

        // تحديث المخزون لكل منتج في الفاتورة
        updateInventoryForInvoice(batch, invoice.getId(), invoiceItems);

        // التحقق من وجود العميل وإضافته تلقائياً إذا لم يكن موجوداً (في جميع الحالات)
        if (isValidCustomerInfo(customerName, phoneNumber)) {
            // تطهير البيانات قبل البحث
            String cleanName = customerName.trim();
            String cleanPhone = cleanPhoneNumber(phoneNumber.trim());
            
            checkAndAddCustomer(batch, invoice, selectedPaymentMethod, cleanPhone, cleanName);
        } else {
            // إذا لم يتم توفير معلومات العميل صحيحة، احفظ الفاتورة فقط
            android.util.Log.d("CheckoutDialog", "معلومات العميل غير مكتملة، سيتم حفظ الفاتورة فقط");
            commitBatchAndFinish(batch);
        }
    }
    
    /**
     * التحقق من وجود العميل وإضافته تلقائياً إذا لم يكن موجوداً
     */
    private void checkAndAddCustomer(WriteBatch batch, Invoice invoice, PaymentMethod selectedPaymentMethod,
                                   String phoneNumber, String customerName) {
        // البحث عن العميل بناءً على رقم الهاتف
        android.util.Log.d("CheckoutDialog", "البحث عن العميل: " + customerName + " - " + phoneNumber);
        
        db.collection("customers")
                .whereEqualTo("phone", phoneNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // تحديث عملية الكتابة المجمعة
                    WriteBatch newBatch = db.batch();
                    DocumentReference invoiceRef = db.collection("invoices").document(invoice.getId());
                    newBatch.set(invoiceRef, invoice);

                    if (queryDocumentSnapshots.isEmpty()) {
                        // العميل غير موجود - إنشاء عميل جديد
                        android.util.Log.d("CheckoutDialog", "إنشاء عميل جديد: " + customerName);
                        Customer newCustomer = new Customer(customerName, phoneNumber);
                        
                        // إضافة الدين فقط إذا كانت الفاتورة دين
                        if (selectedPaymentMethod.isDebt()) {
                            newCustomer.addDebt(invoice.getId(), invoice.getTotalAmount(), invoice.getDate());
                            android.util.Log.d("CheckoutDialog", "تم إضافة دين بقيمة: " + invoice.getTotalAmount());
                        }

                        DocumentReference customerRef = db.collection("customers").document();
                        newCustomer.setId(customerRef.getId());
                        newBatch.set(customerRef, newCustomer);
                        
                        Toast.makeText(getContext(), "✅ تم إضافة العميل الجديد: " + customerName, Toast.LENGTH_SHORT).show();
                    } else {
                        // العميل موجود - تحديث العميل إذا كان هناك دين
                        android.util.Log.d("CheckoutDialog", "عميل موجود: " + customerName);
                        
                        if (selectedPaymentMethod.isDebt()) {
                            DocumentReference customerRef = queryDocumentSnapshots.getDocuments().get(0).getReference();

                            // إضافة الدين الجديد
                            CustomerDebt newDebt = new CustomerDebt(invoice.getId(), invoice.getTotalAmount(), invoice.getDate());

                            // تحميل العميل بالكامل وتحديث ديونه
                            Customer existingCustomer = queryDocumentSnapshots.getDocuments().get(0).toObject(Customer.class);
                            if (existingCustomer != null) {
                                // إضافة الدين الجديد إلى قائمة الديون
                                if (existingCustomer.getDebts() == null) {
                                    existingCustomer.setDebts(new ArrayList<>());
                                }
                                existingCustomer.getDebts().add(newDebt);
                                existingCustomer.setTotalDebt(existingCustomer.getTotalDebt() + invoice.getTotalAmount());

                                // تحديث العميل بالكامل
                                newBatch.set(customerRef, existingCustomer);
                                android.util.Log.d("CheckoutDialog", "تم تحديث دين العميل: " + existingCustomer.getTotalDebt());
                            } else {
                                // في حالة حدوث خطأ في تحويل البيانات
                                Double currentDebt = queryDocumentSnapshots.getDocuments().get(0).getDouble("totalDebt");
                                double newTotalDebt = (currentDebt != null ? currentDebt : 0.0) + invoice.getTotalAmount();
                                newBatch.update(customerRef, "totalDebt", newTotalDebt);
                            }
                            
                            Toast.makeText(getContext(), "✅ تم تحديث دين العميل: " + customerName, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "ℹ️ العميل موجود مسبقاً: " + customerName, Toast.LENGTH_SHORT).show();
                        }
                    }

                    // تحديث المخزون
                    updateInventoryForInvoice(newBatch, invoice.getId(), invoiceItems);

                    // حفظ جميع التغييرات
                    commitBatchAndFinish(newBatch);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CheckoutDialog", "خطأ في البحث عن العميل", e);
                    Toast.makeText(getContext(), "❌ خطأ في البحث عن العميل: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // محاولة حفظ الفاتورة على الأقل
                    commitBatchAndFinish(batch);
                });
    }
    
    /**
     * التحقق من صحة معلومات العميل
     */
    private boolean isValidCustomerInfo(String customerName, String phoneNumber) {
        return customerName != null && !customerName.trim().isEmpty() && 
               phoneNumber != null && !phoneNumber.trim().isEmpty() && 
               phoneNumber.trim().length() >= 8; // رقم هاتف لا يقل عن 8 أرقام
    }
    
    /**
     * تطهير رقم الهاتف من المسافات والرموز غير المرغوبة
     */
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        
        // إزالة المسافات والرموز الخاصة وترك الأرقام والعلامة +
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        
        return cleaned;
    }

    private void commitBatchAndFinish(WriteBatch batch) {
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "تم حفظ الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                    
                    // تسجيل عملية الفاتورة في الأرشيف
                    OperationLogService operationLogService = OperationLogService.getInstance(getContext());
                    
                    // التحقق من نوع العملية (إنشاء أم تحديث)
                    boolean isUpdate = CounterFragment.isEditingExistingInvoice();
                    String loadedInvoiceId = CounterFragment.getLoadedInvoiceId();
                    
                    java.util.Map<String, Object> invoiceData = new java.util.HashMap<>();
                    invoiceData.put("totalAmount", totalAmount);
                    invoiceData.put("itemsCount", invoiceItems.size());
                    
                    if (isUpdate && loadedInvoiceId != null) {
                        // تسجيل عملية تحديث
                        String description = "تحديث فاتورة بقيمة " + CurrencyUtils.formatCurrency(totalAmount) + 
                                           " تحتوي على " + invoiceItems.size() + " منتجات";
                        
                        operationLogService.logUpdate(
                            OperationLog.EntityType.INVOICE,
                            loadedInvoiceId,
                            description,
                            null, // البيانات القديمة
                            invoiceData
                        );
                    } else {
                        // تسجيل عملية إنشاء
                        String description = "إنشاء فاتورة جديدة بقيمة " + CurrencyUtils.formatCurrency(totalAmount) + 
                                           " تحتوي على " + invoiceItems.size() + " منتجات";
                        
                        operationLogService.logCreate(
                            OperationLog.EntityType.INVOICE,
                            "new_invoice", // سيتم تحديثه بالمعرف الحقيقي لاحقاً
                            description,
                            invoiceData
                        );
                    }
                    
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

    private void addPaymentMethodOptions(RadioGroup paymentMethodRadioGroup) {
        PaymentMethod[] methods = PaymentMethod.values();
        
        for (int i = 0; i < methods.length; i++) {
            PaymentMethod method = methods[i];
            RadioButton radioButton = new RadioButton(requireContext());
            
            // تعيين ID فريد لكل RadioButton
            radioButton.setId(View.generateViewId());
            radioButton.setText(method.getDisplayWithIcon());
            radioButton.setTextSize(16);
            radioButton.setPadding(16, 12, 16, 12);
            radioButton.setTag(method); // حفظ نوع الدفع في tag
            
            // تلوين النص حسب نوع الدفع
            if (method.isDebt()) {
                radioButton.setTextColor(getResources().getColor(R.color.reportWarning));
            } else {
                radioButton.setTextColor(getResources().getColor(R.color.reportSuccess));
            }
            
            paymentMethodRadioGroup.addView(radioButton);
            
            // تعيين الطريقة الافتراضية (نقداً)
            if (method == PaymentMethod.CASH) {
                radioButton.setChecked(true);
            }
        }
    }
    
    private PaymentMethod getSelectedPaymentMethod(int selectedId) {
        RadioButton selectedRadioButton = paymentMethodRadioGroup.findViewById(selectedId);
        if (selectedRadioButton != null && selectedRadioButton.getTag() instanceof PaymentMethod) {
            return (PaymentMethod) selectedRadioButton.getTag();
        }
        return PaymentMethod.CASH; // Default to cash if no match
    }
}