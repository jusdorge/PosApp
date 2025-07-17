package com.example.posapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Invoice;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.Intent;

public class TodayFragment extends Fragment implements InvoiceListAdapter.OnInvoiceClickListener {
    private RecyclerView invoicesRecyclerView;
    private TextView emptyInvoicesTextView;
    private TextView totalSalesTextView;
    private TextView invoiceCountTextView;
    private TextView todayDateTextView;
    
    private InvoiceListAdapter adapter;
    private List<Invoice> invoiceList;
    
    private FirebaseFirestore db;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today, container, false);
        
        // ربط عناصر الواجهة
        invoicesRecyclerView = view.findViewById(R.id.invoicesRecyclerView);
        emptyInvoicesTextView = view.findViewById(R.id.emptyInvoicesTextView);
        totalSalesTextView = view.findViewById(R.id.totalSalesTextView);
        invoiceCountTextView = view.findViewById(R.id.invoiceCountTextView);
        todayDateTextView = view.findViewById(R.id.todayDateTextView);
        
        // إعداد قائمة الفواتير
        invoiceList = new ArrayList<>();
        adapter = new InvoiceListAdapter(invoiceList);
        adapter.setOnInvoiceClickListener(this);
        
        invoicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        invoicesRecyclerView.setAdapter(adapter);
        
        // إعداد Firestore
        db = FirebaseFirestore.getInstance();
        
        // عرض تاريخ اليوم
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("ar"));
        String todayDate = dateFormat.format(new Date());
        todayDateTextView.setText("فواتير " + todayDate);
        
        // تحميل فواتير اليوم
        loadTodayInvoices();
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // تسجيل مستمع إضافة الفواتير
        CheckoutDialog.setOnInvoiceAddedListener(() -> {
            // إعادة تحميل فواتير اليوم عند إضافة فاتورة جديدة
            loadTodayInvoices();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // إلغاء تسجيل المستمع
        CheckoutDialog.setOnInvoiceAddedListener(null);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // إعادة تحميل البيانات عند العودة للواجهة
        loadTodayInvoices();
    }
    
    private void loadTodayInvoices() {
        // الحصول على بداية ونهاية اليوم الحالي
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();
        
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfDay = calendar.getTime();
        
        Timestamp startTimestamp = new Timestamp(startOfDay);
        Timestamp endTimestamp = new Timestamp(endOfDay);
        
        // استعلام لجلب فواتير اليوم
        db.collection("invoices")
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .orderBy("date", Query.Direction.DESCENDING) // أحدث الفواتير أولاً
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                invoiceList.clear();
                
                if (queryDocumentSnapshots.isEmpty()) {
                    // عرض رسالة إذا لم تكن هناك فواتير
                    showEmptyView(true);
                    updateSummary(0, 0.0);
                    return;
                }
                
                double totalSales = 0.0;
                
                for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                    Invoice invoice = queryDocumentSnapshots.getDocuments().get(i).toObject(Invoice.class);
                    if (invoice != null) {
                        invoice.setId(queryDocumentSnapshots.getDocuments().get(i).getId());
                        invoiceList.add(invoice);
                        totalSales += invoice.getTotalAmount();
                    }
                }
                
                adapter.notifyDataSetChanged();
                showEmptyView(false);
                updateSummary(invoiceList.size(), totalSales);
            })
            .addOnFailureListener(e -> {
                showEmptyView(true);
                Toast.makeText(getContext(), "حدث خطأ أثناء تحميل الفواتير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showEmptyView(boolean isEmpty) {
        if (isEmpty) {
            invoicesRecyclerView.setVisibility(View.GONE);
            emptyInvoicesTextView.setVisibility(View.VISIBLE);
        } else {
            invoicesRecyclerView.setVisibility(View.VISIBLE);
            emptyInvoicesTextView.setVisibility(View.GONE);
        }
    }
    
    private void updateSummary(int count, double total) {
        invoiceCountTextView.setText("عدد الفواتير: " + count);
        totalSalesTextView.setText(String.format("إجمالي المبيعات: %.2f ريال", total));
    }
    
    @Override
    public void onInvoiceClick(Invoice invoice, int position) {
        // فتح صفحة طباعة الفاتورة
        Intent intent = InvoicePrintActivity.createIntent(getContext(), invoice.getId());
        startActivity(intent);
    }
    
    private void showInvoiceDetails(Invoice invoice) {
        // هنا يمكن إضافة كود لعرض تفاصيل الفاتورة في نافذة منبثقة
        // سنقوم بتنفيذها لاحقاً إذا طلب المستخدم
    }
}
