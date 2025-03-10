package com.example.posapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        // ربط TextViews لعرض البيانات
        TextView lowStockTextView = view.findViewById(R.id.lowStockTextView);
        TextView remainingStockTextView = view.findViewById(R.id.remainingStockTextView);
        TextView totalSalesTextView = view.findViewById(R.id.totalSalesTextView);
        TextView dailyProfitTextView = view.findViewById(R.id.dailyProfitTextView);
//        TextView topStocksTextView = view.findViewById(R.id.topStocksTextView);
//        TextView topCategoryTextView = view.findViewById(R.id.topCategoryTextView);
//        TextView totalReceiptCountTextView = view.findViewById(R.id.totalReceiptCountTextView);
//        TextView topCustomerTextView = view.findViewById(R.id.topCustomerTextView);
//        ImageView profitDetailsIcon = view.findViewById(R.id.profitDetailsIcon);

        // تهيئة Firestore
        db = FirebaseFirestore.getInstance();

//        // إعداد مستمع للأيقونة للتنقل إلى صفحة تفاصيل الأرباح
//        profitDetailsIcon.setOnClickListener(v -> {
//            Intent intent = new Intent(getContext(), ProfitDetailsActivity.class);
//            startActivity(intent);
//        });

        // جلب البيانات وعرضها
        fetchLowStockInventory(lowStockTextView);
        fetchRemainingStock(remainingStockTextView);
        fetchTotalSales(totalSalesTextView);
        fetchDailyProfit(dailyProfitTextView);
//        fetchTopStocks(topStocksTextView);
//        fetchTopCategory(topCategoryTextView);
//        fetchTotalReceiptCount(totalReceiptCountTextView);
//        fetchTopCustomer(topCustomerTextView);

        return view;
    }

    private void fetchLowStockInventory(TextView textView) {
        // منطق جلب وعرض المخزون المنخفض
        textView.setText("Low Stock Inventory: ...");
    }

    private void fetchRemainingStock(TextView textView) {
        // منطق جلب وعرض المخزون المتبقي
        textView.setText("Remaining Stock: ...");
    }

    private void fetchTotalSales(TextView textView) {
        // منطق جلب وعرض مجموع المبيعات
        textView.setText("Total Sales: ...");
    }

    private void fetchDailyProfit(TextView textView) {
        // منطق جلب وعرض الأرباح اليومية
        textView.setText("Daily Profit: ...");
    }

    private void fetchTopStocks(TextView textView) {
        // منطق جلب وعرض أفضل المنتجات
        textView.setText("Top Stocks: ...");
    }

    private void fetchTopCategory(TextView textView) {
        // منطق جلب وعرض أفضل فئة
        textView.setText("Top Category: ...");
    }

    private void fetchTotalReceiptCount(TextView textView) {
        // منطق جلب وعرض عدد الفواتير الكلي
        textView.setText("Total Receipt Count: ...");
    }

    private void fetchTopCustomer(TextView textView) {
        // منطق جلب وعرض أفضل زبون
        textView.setText("Top Customer: ...");
    }
}
