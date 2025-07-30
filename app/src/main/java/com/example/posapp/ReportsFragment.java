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
import com.example.posapp.model.Invoice;
import com.example.posapp.model.InvoiceItem;
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
    private TextView creditValueTextView;
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
        creditValueTextView = view.findViewById(R.id.creditValueTextView);
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
        String dateString = ArabicNumberUtils.formatShortDateWithArabicNumbers(selectedDate.getTime());
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
        // Static tax value for now
        taxValueTextView.setText(CurrencyUtils.formatCurrencyForReports(0.0));
    }

    private void loadDiscountInfo() {
        discountValueTextView.setText(CurrencyUtils.formatCurrencyForReports(0.0));
    }

    private void loadAverageSales() {
        Timestamp[] dateRange = getDateRange();

        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        avgSalesValueTextView.setText(CurrencyUtils.formatCurrencyForReports(0.0));
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
                    avgSalesValueTextView.setText(CurrencyUtils.formatCurrencyForReports(average));
                })
                .addOnFailureListener(e -> {
                    avgSalesValueTextView.setText(CurrencyUtils.formatCurrencyForReports(0.0));
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
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalCash = 0;
                    double totalCredit = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double amount = document.getDouble("totalAmount");
                        if (amount != null) {
                            // فحص طريقة الدفع
                            String paymentMethodString = document.getString("paymentMethod");
                            Boolean isPaid = document.getBoolean("isPaid");
                            
                            if (isInvoiceCash(paymentMethodString, isPaid)) {
                                totalCash += amount;
                            } else {
                                totalCredit += amount;
                            }
                        }
                    }

                    cashValueTextView.setText("💵 نقدي: " + CurrencyUtils.formatCurrencyForReports(totalCash));
                    if (creditValueTextView != null) {
                        creditValueTextView.setText("📝 دين: " + CurrencyUtils.formatCurrencyForReports(totalCredit));
                    }
                })
                .addOnFailureListener(e -> {
                    cashValueTextView.setText("💵 نقدي: " + CurrencyUtils.formatCurrencyForReports(0.0));
                    if (creditValueTextView != null) {
                        creditValueTextView.setText("📝 دين: " + CurrencyUtils.formatCurrencyForReports(0.0));
                    }
                });
    }
    
    /**
     * تحديد ما إذا كانت الفاتورة نقدية أم دين
     */
    private boolean isInvoiceCash(String paymentMethodString, Boolean isPaid) {
        // فحص طريقة الدفع الجديدة أولاً
        if (paymentMethodString != null && !paymentMethodString.isEmpty()) {
            try {
                com.example.posapp.model.PaymentMethod paymentMethod = 
                    com.example.posapp.model.PaymentMethod.valueOf(paymentMethodString);
                return paymentMethod.isLegacyPaid();
            } catch (IllegalArgumentException e) {
                // إذا فشل في التحويل، نستخدم الطريقة القديمة
            }
        }
        
        // الرجوع للطريقة القديمة
        return isPaid != null && isPaid;
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
                    int totalInvoices = queryDocumentSnapshots.size();
                    
                    if (totalInvoices == 0) {
                        totalSalesTextView.setText("إجمالي المبيعات: " + CurrencyUtils.formatCurrencyForReports(0.0));
                        totalProfitTextView.setText("إجمالي الربح: " + CurrencyUtils.formatCurrencyForReports(0.0));
                        return;
                    }

                    // مصفوفة لتتبع المعاملة غير المتزامنة
                    final int[] processedInvoices = {0};
                    final double[] finalTotalSales = {0.0};
                    final double[] finalTotalProfit = {0.0};

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double amount = document.getDouble("totalAmount");
                        if (amount != null) {
                            finalTotalSales[0] += amount;
                        }

                        // حساب الربح الفعلي من عناصر الفاتورة
                        calculateInvoiceProfit(document, new ProfitCalculationCallback() {
                            @Override
                            public void onProfitCalculated(double invoiceProfit) {
                                synchronized (processedInvoices) {
                                    finalTotalProfit[0] += invoiceProfit;
                                    processedInvoices[0]++;
                                    
                                    // إذا انتهينا من معالجة جميع الفواتير
                                    if (processedInvoices[0] == totalInvoices) {
                                        totalSalesTextView.setText("إجمالي المبيعات: " + CurrencyUtils.formatCurrencyForReports(finalTotalSales[0]));
                                        totalProfitTextView.setText("إجمالي الربح: " + CurrencyUtils.formatCurrencyForReports(finalTotalProfit[0]));
                                    }
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    totalSalesTextView.setText("إجمالي المبيعات: " + CurrencyUtils.formatCurrencyForReports(0.0));
                    totalProfitTextView.setText("إجمالي الربح: " + CurrencyUtils.formatCurrencyForReports(0.0));
                });
    }
    
    /**
     * حساب الربح الفعلي لفاتورة واحدة بناءً على تكلفة المنتجات
     */
    private void calculateInvoiceProfit(QueryDocumentSnapshot invoiceDoc, ProfitCalculationCallback callback) {
        try {
            Invoice invoice = invoiceDoc.toObject(Invoice.class);
            if (invoice == null || invoice.getItems() == null || invoice.getItems().isEmpty()) {
                callback.onProfitCalculated(0.0);
                return;
            }

            final double[] invoiceProfit = {0.0};
            final int[] processedItems = {0};
            final int totalItems = invoice.getItems().size();

            for (InvoiceItem item : invoice.getItems()) {
                // الحصول على معلومات المنتج من قاعدة البيانات
                db.collection("products")
                    .document(item.getProductId())
                    .get()
                    .addOnSuccessListener(productDoc -> {
                        synchronized (processedItems) {
                            if (productDoc.exists()) {
                                Double costPrice = productDoc.getDouble("costPrice");
                                if (costPrice != null) {
                                    // حساب الربح = (سعر البيع - سعر التكلفة) × الكمية
                                    double itemProfit = (item.getPrice() - costPrice) * item.getQuantity();
                                    invoiceProfit[0] += itemProfit;
                                }
                            }
                            
                            processedItems[0]++;
                            if (processedItems[0] == totalItems) {
                                callback.onProfitCalculated(invoiceProfit[0]);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        synchronized (processedItems) {
                            processedItems[0]++;
                            if (processedItems[0] == totalItems) {
                                callback.onProfitCalculated(invoiceProfit[0]);
                            }
                        }
                    });
            }
        } catch (Exception e) {
            android.util.Log.e("ReportsFragment", "Error calculating invoice profit", e);
            callback.onProfitCalculated(0.0);
        }
    }
    
    /**
     * واجهة للتعامل مع العمليات غير المتزامنة لحساب الربح
     */
    private interface ProfitCalculationCallback {
        void onProfitCalculated(double profit);
    }
}