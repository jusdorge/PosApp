package com.example.posapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Invoice;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllInvoicesActivity extends AppCompatActivity {
    
    private RecyclerView invoicesRecyclerView;
    private AllInvoicesAdapter invoicesAdapter;
    private List<Invoice> invoicesList;
    
    private Button btnStartDate;
    private Button btnEndDate;
    private Button btnFilter;
    private Button btnClearFilter;
    private TextView tvTotalInvoices;
    private TextView tvTotalAmount;
    
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    
    private Date startDate;
    private Date endDate;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_invoices);
        
        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("جميع الفواتير");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // تهيئة المتغيرات
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        calendar = Calendar.getInstance();
        invoicesList = new ArrayList<>();
        
        // ربط عناصر الواجهة
        initViews();
        
        // إعداد RecyclerView
        setupRecyclerView();
        
        // إعداد الأزرار
        setupButtons();
        
        // تحميل جميع الفواتير
        loadAllInvoices();
    }
    
    private void initViews() {
        invoicesRecyclerView = findViewById(R.id.invoicesRecyclerView);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnFilter = findViewById(R.id.btnFilter);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        tvTotalInvoices = findViewById(R.id.tvTotalInvoices);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
    }
    
    private void setupRecyclerView() {
        invoicesAdapter = new AllInvoicesAdapter(invoicesList);
        invoicesAdapter.setOnInvoiceClickListener(this::openInvoicePrintActivity);
        invoicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        invoicesRecyclerView.setAdapter(invoicesAdapter);
    }
    
    private void setupButtons() {
        btnStartDate.setOnClickListener(v -> showStartDatePicker());
        btnEndDate.setOnClickListener(v -> showEndDatePicker());
        btnFilter.setOnClickListener(v -> filterInvoicesByDate());
        btnClearFilter.setOnClickListener(v -> clearFilters());
    }
    
    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 0, 0, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startDate = calendar.getTime();
                btnStartDate.setText("من: " + dateFormat.format(startDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 23, 59, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                endDate = calendar.getTime();
                btnEndDate.setText("إلى: " + dateFormat.format(endDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void filterInvoicesByDate() {
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "يرجى اختيار تاريخ البداية والنهاية", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (startDate.after(endDate)) {
            Toast.makeText(this, "تاريخ البداية يجب أن يكون قبل تاريخ النهاية", Toast.LENGTH_SHORT).show();
            return;
        }
        
        loadInvoicesByDateRange();
    }
    
    private void clearFilters() {
        startDate = null;
        endDate = null;
        btnStartDate.setText("اختر تاريخ البداية");
        btnEndDate.setText("اختر تاريخ النهاية");
        loadAllInvoices();
    }
    
    private void loadAllInvoices() {
        db.collection("invoices")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    invoicesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Invoice invoice = document.toObject(Invoice.class);
                        invoice.setId(document.getId());
                        invoicesList.add(invoice);
                    }
                    invoicesAdapter.notifyDataSetChanged();
                    updateSummary();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل الفواتير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadInvoicesByDateRange() {
        Timestamp startTimestamp = new Timestamp(startDate);
        Timestamp endTimestamp = new Timestamp(endDate);
        
        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThanOrEqualTo("date", endTimestamp)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    invoicesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Invoice invoice = document.toObject(Invoice.class);
                        invoice.setId(document.getId());
                        invoicesList.add(invoice);
                    }
                    invoicesAdapter.notifyDataSetChanged();
                    updateSummary();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل الفواتير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateSummary() {
        int totalInvoices = invoicesList.size();
        double totalAmount = 0.0;
        
        for (Invoice invoice : invoicesList) {
            totalAmount += invoice.getTotalAmount();
        }
        
        tvTotalInvoices.setText("عدد الفواتير: " + totalInvoices);
        tvTotalAmount.setText(String.format("المجموع الكلي: %.2f دج", totalAmount));
    }
    
    private void openInvoicePrintActivity(Invoice invoice) {
        Intent intent = InvoicePrintActivity.createIntent(this, invoice.getId());
        startActivity(intent);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 