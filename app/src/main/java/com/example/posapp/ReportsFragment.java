package com.example.posapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.posapp.model.Product;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportsFragment extends Fragment {
    private static final int STORAGE_PERMISSION_CODE = 1001;

    private FirebaseFirestore db;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    // Date selection controls
    private Button datePickerButton;
    private Button todayButton;
    private Button yesterdayButton;
    private TextView selectedDateTextView;

    // Report data TextViews
    private TextView categoryTitleTextView;
    private TextView agrodivValueTextView;
    private TextView agrodivDescTextView;
    private TextView receiptsCountTitleTextView;
    private TextView receiptsCountValueTextView;
    private TextView taxTitleTextView;
    private TextView taxValueTextView;
    private TextView discountTitleTextView;
    private TextView discountValueTextView;
    private TextView avgSalesTitleTextView;
    private TextView avgSalesValueTextView;
    private TextView bestCustomerTitleTextView;
    private TextView bestCustomerValueTextView;
    private TextView bestCustomerDescTextView;
    private TextView paymentMethodTitleTextView;
    private TextView cashValueTextView;
    private TextView sellerTitleTextView;
    private TextView sellerNameTextView;
    private TextView sellerDescTextView;
    private TextView totalSalesTextView;
    private TextView totalProfitTextView;
    private TextView performanceIndicatorTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports_enhanced, container, false);

        // Initialize Firestore and date
        db = FirebaseFirestore.getInstance();
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy/MM/dd", new Locale("ar"));

        // Initialize all views
        initializeViews(view);
        setupDateControls();

        // Load initial data for today
        loadReportsData();

        return view;
    }

    private void initializeViews(View view) {
        // Date controls
        datePickerButton = view.findViewById(R.id.datePickerButton);
        todayButton = view.findViewById(R.id.todayButton);
        yesterdayButton = view.findViewById(R.id.yesterdayButton);
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView);

        // Report data views
        categoryTitleTextView = view.findViewById(R.id.categoryTitleTextView);
        agrodivValueTextView = view.findViewById(R.id.agrodivValueTextView);
        agrodivDescTextView = view.findViewById(R.id.agrodivDescTextView);
        receiptsCountTitleTextView = view.findViewById(R.id.receiptsCountTitleTextView);
        receiptsCountValueTextView = view.findViewById(R.id.receiptsCountValueTextView);
        taxTitleTextView = view.findViewById(R.id.taxTitleTextView);
        taxValueTextView = view.findViewById(R.id.taxValueTextView);
        discountTitleTextView = view.findViewById(R.id.discountTitleTextView);
        discountValueTextView = view.findViewById(R.id.discountValueTextView);
        avgSalesTitleTextView = view.findViewById(R.id.avgSalesTitleTextView);
        avgSalesValueTextView = view.findViewById(R.id.avgSalesValueTextView);
        bestCustomerTitleTextView = view.findViewById(R.id.bestCustomerTitleTextView);
        bestCustomerValueTextView = view.findViewById(R.id.bestCustomerValueTextView);
        bestCustomerDescTextView = view.findViewById(R.id.bestCustomerDescTextView);
        paymentMethodTitleTextView = view.findViewById(R.id.paymentMethodTitleTextView);
        cashValueTextView = view.findViewById(R.id.cashValueTextView);
        sellerTitleTextView = view.findViewById(R.id.sellerTitleTextView);
        sellerNameTextView = view.findViewById(R.id.sellerNameTextView);
        sellerDescTextView = view.findViewById(R.id.sellerDescTextView);
        totalSalesTextView = view.findViewById(R.id.totalSalesTextView);
        totalProfitTextView = view.findViewById(R.id.totalProfitTextView);
        performanceIndicatorTextView = view.findViewById(R.id.performanceIndicatorTextView);
    }

    private void setupDateControls() {
        updateSelectedDateDisplay();

        datePickerButton.setOnClickListener(v -> showDatePicker());

        todayButton.setOnClickListener(v -> {
            selectedDate = Calendar.getInstance();
            updateSelectedDateDisplay();
            loadReportsData();
        });

        yesterdayButton.setOnClickListener(v -> {
            selectedDate = Calendar.getInstance();
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            updateSelectedDateDisplay();
            loadReportsData();
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateSelectedDateDisplay();
                    loadReportsData();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateSelectedDateDisplay() {
        String dateString = dateFormat.format(selectedDate.getTime());
        selectedDateTextView.setText("تقرير ليوم: " + dateString);
    }

    private void loadReportsData() {
        // Set static titles
        categoryTitleTextView.setText("الفئة الأكثر مبيعاً");
        receiptsCountTitleTextView.setText("إجمالي عدد الفواتير");
        taxTitleTextView.setText("الضرائب");
        discountTitleTextView.setText("الخصومات");
        avgSalesTitleTextView.setText("متوسط قيمة المبيعات");
        bestCustomerTitleTextView.setText("أفضل عميل");
        paymentMethodTitleTextView.setText("طرق الدفع");
        sellerTitleTextView.setText("البائع");

        // Load dynamic data
        loadTopCategory();
        loadTotalReceipts();
        loadTaxInfo();
        loadDiscountInfo();
        loadAverageSales();
        loadBestCustomer();
        loadPaymentMethods();
        loadSellerInfo();
        loadTotalSalesAndProfit();
    }

    private Timestamp[] getDateRange() {
        Calendar startCal = (Calendar) selectedDate.clone();
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = (Calendar) selectedDate.clone();
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        return new Timestamp[]{
                new Timestamp(startCal.getTime()),
                new Timestamp(endCal.getTime())
        };
    }

    private void loadTopCategory() {
        Timestamp[] dateRange = getDateRange();

        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> productCounts = new HashMap<>();
                    String topProductName = "غير محدد";
                    int topCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Object itemsObj = document.get("items");
                        if (itemsObj instanceof java.util.List) {
                            java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) itemsObj;
                            for (Map<String, Object> item : items) {
                                String productName = (String) item.get("productName");
                                Object quantityObj = item.get("quantity");

                                if (productName != null && quantityObj != null) {
                                    int quantity = 0;
                                    if (quantityObj instanceof Long) {
                                        quantity = ((Long) quantityObj).intValue();
                                    } else if (quantityObj instanceof Integer) {
                                        quantity = (Integer) quantityObj;
                                    }

                                    productCounts.put(productName,
                                            productCounts.getOrDefault(productName, 0) + quantity);

                                    if (productCounts.get(productName) > topCount) {
                                        topCount = productCounts.get(productName);
                                        topProductName = productName;
                                    }
                                }
                            }
                        }
                    }

                    if (!topProductName.equals("غير محدد")) {
                        agrodivValueTextView.setText(topProductName + " : " + topCount);
                        agrodivDescTextView.setText((productCounts.size() - 1) + " منتجات أخرى");
                    } else {
                        agrodivValueTextView.setText("لا توجد مبيعات");
                        agrodivDescTextView.setText("0 منتجات");
                    }
                })
                .addOnFailureListener(e -> {
                    agrodivValueTextView.setText("خطأ في التحميل");
                    agrodivDescTextView.setText("--");
                });
    }

    private void loadTotalReceipts() {
        Timestamp[] dateRange = getDateRange();

        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    receiptsCountValueTextView.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    receiptsCountValueTextView.setText("0");
                });
    }

    private void loadTaxInfo() {
        taxValueTextView.setText("DA0.00");
    }

    private void loadDiscountInfo() {
        discountValueTextView.setText("DA0.00");
    }

    private void loadAverageSales() {
        Timestamp[] dateRange = getDateRange();

        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        avgSalesValueTextView.setText("DA0.00");
                        return;
                    }

                    double totalAmount = 0;
                    int count = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double amount = document.getDouble("totalAmount");
                        if (amount != null) {
                            totalAmount += amount;
                            count++;
                        }
                    }

                    double average = count > 0 ? totalAmount / count : 0;
                    avgSalesValueTextView.setText(String.format("DA%.2f", average));
                })
                .addOnFailureListener(e -> {
                    avgSalesValueTextView.setText("DA0.00");
                });
    }

    private void loadBestCustomer() {
        Timestamp[] dateRange = getDateRange();

        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .orderBy("totalAmount", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot topInvoice = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String customerName = topInvoice.getString("customerName");
                        String customerPhone = topInvoice.getString("customerPhone");

                        if (customerName != null && !customerName.isEmpty()) {
                            bestCustomerValueTextView.setText(customerName);
                        } else if (customerPhone != null) {
                            bestCustomerValueTextView.setText(customerPhone);
                        } else {
                            bestCustomerValueTextView.setText("غير محدد");
                        }
                        bestCustomerDescTextView.setText("أفضل عميل اليوم");
                    } else {
                        bestCustomerValueTextView.setText("لا يوجد زبائن");
                        bestCustomerDescTextView.setText("0 زبون");
                    }
                })
                .addOnFailureListener(e -> {
                    bestCustomerValueTextView.setText("خطأ في التحميل");
                    bestCustomerDescTextView.setText("--");
                });
    }

    private void loadPaymentMethods() {
        Timestamp[] dateRange = getDateRange();

        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .whereEqualTo("isPaid", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalCash = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double amount = document.getDouble("totalAmount");
                        if (amount != null) {
                            totalCash += amount;
                        }
                    }

                    cashValueTextView.setText(String.format("نقدي : DA%.2f", totalCash));
                })
                .addOnFailureListener(e -> {
                    cashValueTextView.setText("نقدي : DA0.00");
                });
    }

    private void loadSellerInfo() {
        sellerNameTextView.setText("akram taf");
        sellerDescTextView.setText("1 بائع فقط!");
    }

    private void loadTotalSalesAndProfit() {
        Timestamp[] dateRange = getDateRange();

        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalSales = 0;
                    double totalProfit = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double amount = document.getDouble("totalAmount");
                        if (amount != null) {
                            totalSales += amount;
                            totalProfit += amount * 0.3; // 30% profit margin
                        }
                    }

                    totalSalesTextView.setText(String.format("إجمالي المبيعات: DA%.2f", totalSales));
                    totalProfitTextView.setText(String.format("إجمالي الربح: DA%.2f", totalProfit));
                })
                .addOnFailureListener(e -> {
                    totalSalesTextView.setText("إجمالي المبيعات: DA0.00");
                    totalProfitTextView.setText("إجمالي الربح: DA0.00");
                });
    }
}