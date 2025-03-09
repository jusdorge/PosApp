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

import com.example.posapp.model.Invoice;
import com.example.posapp.model.InvoiceItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Date;

public class ReportsFragment extends Fragment {
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        // ربط TextView لعرض الأرباح
        TextView profitDisplayTextView = view.findViewById(R.id.profitDisplayTextView);

        // تهيئة Firestore
        db = FirebaseFirestore.getInstance();

        // عرض الأرباح اليومية عند تحميل الصفحة
        showDailyProfits(profitDisplayTextView);

        return view;
    }

    private void showDailyProfits(TextView profitDisplayTextView) {
        // الحصول على تاريخ اليوم
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date todayStart = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date todayEnd = calendar.getTime();

        // جلب الفواتير من Firestore
        db.collection("invoices")
            .whereGreaterThanOrEqualTo("date", todayStart)
            .whereLessThan("date", todayEnd)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                double totalProfit = 0.0;
                for (Invoice invoice : queryDocumentSnapshots.toObjects(Invoice.class)) {
                    double profit = invoice.getTotalAmount() - calculateCost(invoice);
                    totalProfit += profit;
                }
                profitDisplayTextView.setText(String.format("الأرباح اليومية: %.2f دج", totalProfit));
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في جلب البيانات: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private double calculateCost(Invoice invoice) {
        // حساب التكلفة الإجمالية للفاتورة
        double totalCost = 0.0;
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                // نفترض أن لديك طريقة للحصول على تكلفة المنتج
                double costPrice = getProductCostPrice(item.getProductId());
                totalCost += costPrice * item.getQuantity();
            }
        }
        return totalCost;
    }

    private double getProductCostPrice(String productId) {
        // جلب تكلفة المنتج من قاعدة البيانات
        final double[] costPrice = {0.0}; // استخدام مصفوفة لتخزين القيمة بسبب القيود على المتغيرات النهائية في Java

        db.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Double cost = documentSnapshot.getDouble("costPrice");
                    if (cost != null) {
                        costPrice[0] = cost;
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في جلب تكلفة المنتج: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        return costPrice[0];
    }
}
