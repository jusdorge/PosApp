package com.example.posapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

        debtsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        invoicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        selectCustomerButton.setOnClickListener(v -> selectCustomerForInvoice());
        closeButton.setOnClickListener(v -> dismiss());
        addPaymentButton.setOnClickListener(v -> showAddPaymentDialog());
        btnShowLocationOnMap.setOnClickListener(v -> showLocationOnMap());

        builder.setView(view);

        loadCustomerDetails();

        return builder.create();
    }

    private void loadCustomerDetails() {
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
    }

    private void displayCustomerDetails(Customer customer) {
        customerNameTextView.setText(customer.getName());
        customerPhoneTextView.setText(customer.getPhone());
        totalDebtTextView.setText(String.format("%.2f دج", customer.getTotalDebt()));

        latitude = customer.getLatitude();
        longitude = customer.getLongitude();
        if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
            tvCustomerLocation.setText(String.format("%.4f, %.4f", latitude, longitude));
            btnShowLocationOnMap.setEnabled(true);
        } else {
            tvCustomerLocation.setText("غير محدد");
            btnShowLocationOnMap.setEnabled(false);
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
        Customer newCurrentCustomer = new Customer();
        newCurrentCustomer.setId(customerId);
        newCurrentCustomer.setName(customerNameTextView.getText().toString());
        newCurrentCustomer.setPhone(customerPhoneTextView.getText().toString());
        CounterFragment.setCustomer(newCurrentCustomer);
        CheckoutDialog.setCustomer(newCurrentCustomer);

        Toast.makeText(getContext(), "تم اختيار الزبون للفاتورة: " + customerNameTextView.getText(), Toast.LENGTH_SHORT).show();
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
}