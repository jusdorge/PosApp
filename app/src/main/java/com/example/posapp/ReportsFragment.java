package com.example.posapp;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.example.posapp.model.User;
import com.example.posapp.UserSession;
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

import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import androidx.cardview.widget.CardView;

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
    
    // Export buttons
    private Button exportCSVButton;
    private Button exportTextButton;
    private Button shareReportButton;
    
    // Performance indicator card
    private CardView performanceCard;
    
    // Cash collection views
    private TextView cashSalesAmountTextView;
    private TextView debtPaymentsAmountTextView;
    private TextView totalCashCollectedTextView;
    
    // Cached data for reports
    private Map<String, Object> reportData;

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
        setupExportButtons();

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
        
        // إضافة مستمع النقر لأفضل منتج
        View topProductCard = agrodivValueTextView.getParent().getParent() instanceof View ?
                (View) agrodivValueTextView.getParent().getParent() : null;
        if (topProductCard != null) {
            topProductCard.setOnClickListener(v -> {
                // إضافة تأثير بصري عند النقر
                v.setAlpha(0.7f);
                v.animate().alpha(1.0f).setDuration(200);
                
                openProductsAnalyticsReport();
            });
            
            // جعل الكارد يبدو قابل للنقر
            topProductCard.setClickable(true);
            topProductCard.setFocusable(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                topProductCard.setForeground(getResources().getDrawable(R.drawable.card_ripple_effect, null));
            }
        }
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
        
        // Export buttons
        exportCSVButton = view.findViewById(R.id.exportCSVButton);
        exportTextButton = view.findViewById(R.id.exportTextButton);
        shareReportButton = view.findViewById(R.id.shareReportButton);
        
        // Performance card
        performanceCard = view.findViewById(R.id.performanceIndicatorTextView).getParent().getParent() instanceof CardView ?
                (CardView) view.findViewById(R.id.performanceIndicatorTextView).getParent().getParent() : null;
        
        // Cash collection views
        cashSalesAmountTextView = view.findViewById(R.id.cashSalesAmountTextView);
        debtPaymentsAmountTextView = view.findViewById(R.id.debtPaymentsAmountTextView);
        totalCashCollectedTextView = view.findViewById(R.id.totalCashCollectedTextView);
        
        // Initialize report data cache
        reportData = new HashMap<>();
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
        selectedDateTextView.setText(getString(R.string.report_for_day, dateString));
    }

    private void loadReportsData() {
        // إظهار مؤشرات التحميل
        showLoadingIndicators();
        
        // Set static titles
        categoryTitleTextView.setText(getString(R.string.most_sold_category_title));
        receiptsCountTitleTextView.setText(getString(R.string.total_receipts_title));
        taxTitleTextView.setText(getString(R.string.tax_title));
        discountTitleTextView.setText(getString(R.string.discounts_title));
        avgSalesTitleTextView.setText(getString(R.string.avg_sales_title));
        bestCustomerTitleTextView.setText(getString(R.string.best_customer_title));
        paymentMethodTitleTextView.setText(getString(R.string.payment_methods_title));
        sellerTitleTextView.setText(getString(R.string.seller_title));

        // مسح البيانات السابقة
        reportData.clear();

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
        loadCashCollectionData();
        
        // إخفاء مؤشرات التحميل بعد 3 ثواني
        if (getView() != null) {
            getView().postDelayed(this::hideLoadingIndicators, 3000);
        }
    }
    
    private void showLoadingIndicators() {
        // تعطيل الأزرار أثناء التحميل
        if (exportCSVButton != null) exportCSVButton.setEnabled(false);
        if (exportTextButton != null) exportTextButton.setEnabled(false);
        if (shareReportButton != null) shareReportButton.setEnabled(false);
        
        // إظهار رسائل التحميل
        agrodivValueTextView.setText(getString(R.string.loading_dots));
        receiptsCountValueTextView.setText(getString(R.string.loading_dots));
        avgSalesValueTextView.setText(getString(R.string.calculating_dots));
        bestCustomerValueTextView.setText(getString(R.string.searching_dots));
        cashValueTextView.setText(getString(R.string.calculating_cash_prefix));
        if (creditValueTextView != null) {
            creditValueTextView.setText(getString(R.string.calculating_credit_prefix));
        }
        totalSalesTextView.setText(getString(R.string.total_sales_calculating));
        totalProfitTextView.setText(getString(R.string.total_profit_calculating));
        performanceIndicatorTextView.setText(getString(R.string.evaluating_performance));
        
        // إظهار رسائل تحميل النقود المحصلة
        if (cashSalesAmountTextView != null) {
            cashSalesAmountTextView.setText(getString(R.string.calculating_cash_collected));
        }
        if (debtPaymentsAmountTextView != null) {
            debtPaymentsAmountTextView.setText(getString(R.string.calculating_debt_payments));
        }
        if (totalCashCollectedTextView != null) {
            totalCashCollectedTextView.setText(getString(R.string.calculating_cash_collected));
        }
    }
    
    private void hideLoadingIndicators() {
        // إعادة تفعيل الأزرار
        if (exportCSVButton != null) exportCSVButton.setEnabled(true);
        if (exportTextButton != null) exportTextButton.setEnabled(true);
        if (shareReportButton != null) shareReportButton.setEnabled(true);
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
                    String topProductName = getString(R.string.not_specified);
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

                    if (!topProductName.equals(getString(R.string.not_specified))) {
                        agrodivValueTextView.setText(getString(R.string.top_product_with_count, topProductName, topCount));
                        agrodivDescTextView.setText(getString(R.string.other_products_with_hint, productCounts.size() - 1));
                        reportData.put("topProduct", topProductName + " : " + topCount);
                    } else {
                        agrodivValueTextView.setText(getString(R.string.no_sales));
                        agrodivDescTextView.setText(getString(R.string.zero_products_with_hint));
                        reportData.put("topProduct", getString(R.string.no_sales));
                    }
                })
                .addOnFailureListener(e -> {
                    agrodivValueTextView.setText(getString(R.string.load_error));
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
                    reportData.put("receiptsCount", String.valueOf(count));
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
                    reportData.put("avgSales", CurrencyUtils.formatCurrencyForReports(average));
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

                        String bestCustomer;
                        if (customerName != null && !customerName.isEmpty()) {
                            bestCustomer = customerName;
                            bestCustomerValueTextView.setText(customerName);
                        } else if (customerPhone != null) {
                            bestCustomer = customerPhone;
                            bestCustomerValueTextView.setText(customerPhone);
                        } else {
                            bestCustomer = getString(R.string.not_specified);
                            bestCustomerValueTextView.setText(getString(R.string.not_specified));
                        }
                        bestCustomerDescTextView.setText(getString(R.string.best_customer_today));
                        reportData.put("bestCustomer", bestCustomer);
                    } else {
                        bestCustomerValueTextView.setText(getString(R.string.no_customers));
                        bestCustomerDescTextView.setText(getString(R.string.zero_customers));
                    }
                })
                .addOnFailureListener(e -> {
                    bestCustomerValueTextView.setText(getString(R.string.load_error));
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

                    String cashText = getString(R.string.cash_sales_format, CurrencyUtils.formatCurrencyForReports(totalCash));
                    String creditText = getString(R.string.debt_payments_format, CurrencyUtils.formatCurrencyForReports(totalCredit));
                    
                    cashValueTextView.setText(cashText);
                    if (creditValueTextView != null) {
                        creditValueTextView.setText(creditText);
                    }
                    
                    reportData.put("cashSales", CurrencyUtils.formatCurrencyForReports(totalCash));
                    reportData.put("creditSales", CurrencyUtils.formatCurrencyForReports(totalCredit));
                })
                .addOnFailureListener(e -> {
                    cashValueTextView.setText(getString(R.string.cash_sales_format, CurrencyUtils.formatCurrencyForReports(0.0)));
                    if (creditValueTextView != null) {
                        creditValueTextView.setText(getString(R.string.debt_payments_format, CurrencyUtils.formatCurrencyForReports(0.0)));
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
        // الحصول على معلومات المستخدم الحالي
        UserSession userSession = UserSession.getInstance(getContext());
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser != null) {
            String displayName = currentUser.getFullName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = currentUser.getEmail();
                if (displayName != null && displayName.contains("@")) {
                    displayName = displayName.substring(0, displayName.indexOf("@"));
                }
            }
            
            // إنشاء متغير final للاستخدام في lambda
            final String finalDisplayName = displayName != null ? displayName : getString(R.string.not_specified);
            
            sellerNameTextView.setText(finalDisplayName);
            
            // حساب عدد البائعين النشطين
            db.collection("users")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int activeUsersCount = queryDocumentSnapshots.size();
                        String desc = activeUsersCount == 1 ? getString(R.string.only_one_seller) :
                                 getString(R.string.active_sellers_count, activeUsersCount);
                    sellerDescTextView.setText(desc);
                    
                    // حفظ في cache
                    reportData.put("sellerName", finalDisplayName);
                    reportData.put("activeUsers", activeUsersCount);
                })
                .addOnFailureListener(e -> {
                    sellerDescTextView.setText(getString(R.string.not_specified));
                    reportData.put("activeUsers", 1);
                    reportData.put("sellerName", finalDisplayName);
                });
        } else {
            sellerNameTextView.setText(getString(R.string.not_logged_in));
            sellerDescTextView.setText(getString(R.string.please_login));
        }
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
                        totalSalesTextView.setText(getString(R.string.total_sales_label, CurrencyUtils.formatCurrencyForReports(0.0)));
                        totalProfitTextView.setText(getString(R.string.total_profit_label, CurrencyUtils.formatCurrencyForReports(0.0)));
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
                                        totalSalesTextView.setText(getString(R.string.total_sales_label, CurrencyUtils.formatCurrencyForReports(finalTotalSales[0])));
                                        totalProfitTextView.setText(getString(R.string.total_profit_label, CurrencyUtils.formatCurrencyForReports(finalTotalProfit[0])));
                                        
                                        // حفظ البيانات في cache
                                        reportData.put("totalSales", CurrencyUtils.formatCurrencyForReports(finalTotalSales[0]));
                                        reportData.put("totalProfit", CurrencyUtils.formatCurrencyForReports(finalTotalProfit[0]));
                                        
                                        // تحديث مؤشر الأداء
                                        updatePerformanceIndicator(finalTotalSales[0], finalTotalProfit[0], totalInvoices);
                                    }
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    totalSalesTextView.setText(getString(R.string.total_sales_label, CurrencyUtils.formatCurrencyForReports(0.0)));
                    totalProfitTextView.setText(getString(R.string.total_profit_label, CurrencyUtils.formatCurrencyForReports(0.0)));
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
    
    /**
     * تحميل بيانات النقود المحصلة (المبيعات النقدية + المدفوعات من الديون)
     */
    private void loadCashCollectionData() {
        Timestamp[] dateRange = getDateRange();
        
        // أولاً: حساب المبيعات النقدية من الفواتير
        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .get()
                .addOnSuccessListener(invoicesSnapshot -> {
                    double cashSales = 0;
                    
                    for (QueryDocumentSnapshot document : invoicesSnapshot) {
                        Double amount = document.getDouble("totalAmount");
                        if (amount != null) {
                            String paymentMethodString = document.getString("paymentMethod");
                            Boolean isPaid = document.getBoolean("isPaid");
                            
                            if (isInvoiceCash(paymentMethodString, isPaid)) {
                                cashSales += amount;
                            }
                        }
                    }
                    
                    final double finalCashSales = cashSales;
                    
                    // ثانياً: حساب المدفوعات من الديون
                    loadDebtPayments(dateRange, finalCashSales);
                })
                .addOnFailureListener(e -> {
                    // في حالة الفشل، عرض قيم افتراضية
                    updateCashCollectionUI(0.0, 0.0);
                });
    }
    
    /**
     * حساب المدفوعات من ديون العملاء في النطاق الزمني المحدد
     */
    private void loadDebtPayments(Timestamp[] dateRange, double cashSales) {
        db.collection("customers")
                .get()
                .addOnSuccessListener(customersSnapshot -> {
                    double totalDebtPayments = 0;
                    
                    for (QueryDocumentSnapshot customerDoc : customersSnapshot) {
                        Object debtsObj = customerDoc.get("debts");
                        if (debtsObj instanceof java.util.List) {
                            java.util.List<Map<String, Object>> debts = (java.util.List<Map<String, Object>>) debtsObj;
                            
                            for (Map<String, Object> debtMap : debts) {
                                Boolean isPayment = (Boolean) debtMap.get("isPayment");
                                Object dateObj = debtMap.get("date");
                                Object amountObj = debtMap.get("amount");
                                
                                if (isPayment != null && isPayment && dateObj instanceof com.google.firebase.Timestamp && amountObj != null) {
                                    com.google.firebase.Timestamp paymentDate = (com.google.firebase.Timestamp) dateObj;
                                    
                                    // التحقق من أن المدفوعة في النطاق الزمني المحدد
                                    if (paymentDate.compareTo(dateRange[0]) >= 0 && paymentDate.compareTo(dateRange[1]) <= 0) {
                                        double amount = 0;
                                        if (amountObj instanceof Double) {
                                            amount = (Double) amountObj;
                                        } else if (amountObj instanceof Long) {
                                            amount = ((Long) amountObj).doubleValue();
                                        }
                                        totalDebtPayments += amount;
                                    }
                                }
                            }
                        }
                    }
                    
                    // تحديث واجهة المستخدم بالنتائج النهائية
                    updateCashCollectionUI(cashSales, totalDebtPayments);
                })
                .addOnFailureListener(e -> {
                    // في حالة فشل تحميل المدفوعات، عرض المبيعات النقدية فقط
                    updateCashCollectionUI(cashSales, 0.0);
                });
    }
    
    /**
     * تحديث واجهة المستخدم ببيانات النقود المحصلة
     */
    @SuppressLint("StringFormatInvalid")
    private void updateCashCollectionUI(double cashSales, double debtPayments) {
        double totalCashCollected = cashSales + debtPayments;
        
        if (cashSalesAmountTextView != null) {
            cashSalesAmountTextView.setText(
                getString(R.string.cash_sales_format, CurrencyUtils.formatCurrencyForReports(cashSales))
            );
        }
        
        if (debtPaymentsAmountTextView != null) {
            debtPaymentsAmountTextView.setText(
                getString(R.string.debt_payments_format, CurrencyUtils.formatCurrencyForReports(debtPayments))
            );
        }
        
        if (totalCashCollectedTextView != null) {
            totalCashCollectedTextView.setText(
                getString(R.string.total_cash_collected_format, CurrencyUtils.formatCurrencyForReports(totalCashCollected))
            );
        }
        
        // حفظ البيانات في التقرير للتصدير
        reportData.put("cashSalesAmount", CurrencyUtils.formatCurrencyForReports(cashSales));
        reportData.put("debtPaymentsAmount", CurrencyUtils.formatCurrencyForReports(debtPayments));
        reportData.put("totalCashCollected", CurrencyUtils.formatCurrencyForReports(totalCashCollected));
    }
    
    private void setupExportButtons() {
        exportCSVButton.setOnClickListener(v -> {
            // إضافة تأثير بصري للنقر
            v.setEnabled(false);
            exportToCSV();
            v.postDelayed(() -> v.setEnabled(true), 2000);
        });
        
        exportTextButton.setOnClickListener(v -> {
            v.setEnabled(false);
            exportToText();
            v.postDelayed(() -> v.setEnabled(true), 2000);
        });
        
        shareReportButton.setOnClickListener(v -> {
            v.setEnabled(false);
            shareReport();
            v.postDelayed(() -> v.setEnabled(true), 2000);
        });
    }
    
    private void exportToCSV() {
        // فحص الأذونات أولاً
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), 
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                    STORAGE_PERMISSION_CODE);
            return;
        }
        
        try {
            File exportDir = new File(getContext().getExternalFilesDir(null), "reports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            String dateStr = ArabicNumberUtils.formatShortDateWithArabicNumbers(selectedDate.getTime());
            File csvFile = new File(exportDir, getString(R.string.report_file_prefix) + "_" + dateStr.replace("/", "_") + ".csv");
            
            FileWriter writer = new FileWriter(csvFile);
            
            // كتابة رأس الملف
            writer.append(getString(R.string.report_csv_header)).append("\n");
            writer.append(getString(R.string.report_date_label)).append(",").append(dateStr).append("\n");
            writer.append(getString(R.string.report_total_sales_label)).append(",")
                    .append(String.valueOf(reportData.get("totalSales"))).append("\n");
            writer.append(getString(R.string.report_total_profit_label)).append(",")
                    .append(String.valueOf(reportData.get("totalProfit"))).append("\n");
            writer.append(getString(R.string.report_invoices_count_label)).append(",")
                    .append(String.valueOf(reportData.get("receiptsCount"))).append("\n");
            writer.append(getString(R.string.report_avg_invoice_label)).append(",")
                    .append(String.valueOf(reportData.get("avgSales"))).append("\n");
            writer.append(getString(R.string.report_top_product_label)).append(",")
                    .append(String.valueOf(reportData.get("topProduct"))).append("\n");
            writer.append(getString(R.string.report_best_customer_label)).append(",")
                    .append(String.valueOf(reportData.get("bestCustomer"))).append("\n");
            writer.append(getString(R.string.report_cash_sales_label)).append(",")
                    .append(String.valueOf(reportData.get("cashSales"))).append("\n");
            writer.append(getString(R.string.report_credit_sales_label)).append(",")
                    .append(String.valueOf(reportData.get("creditSales"))).append("\n");
            writer.append(getString(R.string.report_debt_payments_label)).append(",")
                    .append(String.valueOf(reportData.get("debtPaymentsAmount"))).append("\n");
            writer.append(getString(R.string.total_cash_collected)).append(",")
                    .append(String.valueOf(reportData.get("totalCashCollected"))).append("\n");
            
            writer.close();
            
            Toast.makeText(getContext(), getString(R.string.export_success), Toast.LENGTH_SHORT).show();
            
            // فتح الملف
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(csvFile);
            intent.setDataAndType(uri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(intent);
            }
            
        } catch (IOException e) {
            Toast.makeText(getContext(), getString(R.string.export_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @SuppressLint("StringFormatInvalid")
    private void exportToText() {
        // فحص الأذونات أولاً
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), 
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                    STORAGE_PERMISSION_CODE);
            return;
        }
        
        try {
            File exportDir = new File(getContext().getExternalFilesDir(null), "reports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            String dateStr = ArabicNumberUtils.formatShortDateWithArabicNumbers(selectedDate.getTime());
            File textFile = new File(exportDir, getString(R.string.report_file_prefix) + "_" + dateStr.replace("/", "_") + ".txt");
            
            FileWriter writer = new FileWriter(textFile);
            
            // كتابة التقرير النصي
            writer.append("====== ").append(getString(R.string.report_title)).append(" ======\n");
            writer.append(getString(R.string.report_date_label)).append(": ").append(dateStr).append("\n");
            writer.append("================================\n\n");
            
            writer.append(getString(R.string.report_stats_section_header)).append("\n");
            writer.append(getString(R.string.report_total_sales_line, String.valueOf(reportData.get("totalSales")))).append("\n");
            writer.append(getString(R.string.report_total_profit_line, String.valueOf(reportData.get("totalProfit")))).append("\n");
            writer.append(getString(R.string.report_invoices_count_line, (int)(reportData.get("receiptsCount") != null ? Integer.parseInt(String.valueOf(reportData.get("receiptsCount"))) : 0))).append("\n");
            writer.append(getString(R.string.report_avg_invoice_line, String.valueOf(reportData.get("avgSales")))).append("\n\n");
            
            writer.append(getString(R.string.report_best_performance_header)).append("\n");
            writer.append("• ").append(getString(R.string.report_top_product_label)).append(": ")
                  .append(String.valueOf(reportData.get("topProduct"))).append("\n");
            writer.append("• ").append(getString(R.string.report_best_customer_label)).append(": ")
                  .append(String.valueOf(reportData.get("bestCustomer"))).append("\n\n");
            
            writer.append(getString(R.string.report_payment_methods_header)).append("\n");
            writer.append("• ").append(getString(R.string.report_cash_sales_label)).append(": ")
                  .append(String.valueOf(reportData.get("cashSales"))).append("\n");
            writer.append("• ").append(getString(R.string.report_credit_sales_label)).append(": ")
                  .append(String.valueOf(reportData.get("creditSales"))).append("\n\n");
            
            writer.append(getString(R.string.cash_collection_summary)).append("\n");
            writer.append("• ").append(getString(R.string.cash_sales)).append(": ")
                  .append(String.valueOf(reportData.get("cashSalesAmount"))).append("\n");
            writer.append("• ").append(getString(R.string.debt_payments)).append(": ")
                  .append(String.valueOf(reportData.get("debtPaymentsAmount"))).append("\n");
            writer.append("• ").append(getString(R.string.total_cash_collected)).append(": ")
                  .append(String.valueOf(reportData.get("totalCashCollected"))).append("\n\n");
            
            writer.append("================================\n");
            writer.append(getString(R.string.report_generated_by_app)).append("\n");
            writer.append(getString(R.string.report_created_at_label)).append(": ")
                  .append(ArabicNumberUtils.formatDateTimeWithArabicNumbers(new Date())).append("\n");
            
            writer.close();
            
            Toast.makeText(getContext(), getString(R.string.export_success), Toast.LENGTH_SHORT).show();
            
            // فتح الملف
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(textFile);
            intent.setDataAndType(uri, "text/plain");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                startActivity(intent);
            }
            
        } catch (IOException e) {
            Toast.makeText(getContext(), getString(R.string.export_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void shareReport() {
        String dateStr = ArabicNumberUtils.formatShortDateWithArabicNumbers(selectedDate.getTime());
        StringBuilder shareText = new StringBuilder();
        shareText.append(getString(R.string.report_title)).append(" - ").append(dateStr).append("\n");
        shareText.append("═══════════════════════════════\n\n");
        shareText.append(getString(R.string.report_total_sales_line, String.valueOf(reportData.get("totalSales")))).append("\n");
        shareText.append(getString(R.string.report_total_profit_line, String.valueOf(reportData.get("totalProfit")))).append("\n");
        shareText.append(getString(R.string.report_invoices_count_line, (int)(reportData.get("receiptsCount") != null ? Integer.parseInt(String.valueOf(reportData.get("receiptsCount"))) : 0))).append("\n");
        shareText.append(getString(R.string.report_avg_invoice_line, String.valueOf(reportData.get("avgSales")))).append("\n\n");
        shareText.append(getString(R.string.report_top_product_label)).append(": ")
                .append(String.valueOf(reportData.get("topProduct"))).append("\n");
        shareText.append(getString(R.string.report_best_customer_label)).append(": ")
                .append(String.valueOf(reportData.get("bestCustomer"))).append("\n\n");
        shareText.append(getString(R.string.report_cash_sales_label)).append(": ")
                .append(String.valueOf(reportData.get("cashSales"))).append("\n");
        shareText.append(getString(R.string.report_credit_sales_label)).append(": ")
                .append(String.valueOf(reportData.get("creditSales"))).append("\n\n");
        shareText.append(getString(R.string.cash_collection_summary)).append("\n");
        shareText.append("• ").append(getString(R.string.cash_sales)).append(": ")
                .append(String.valueOf(reportData.get("cashSalesAmount"))).append("\n");
        shareText.append("• ").append(getString(R.string.debt_payments)).append(": ")
                .append(String.valueOf(reportData.get("debtPaymentsAmount"))).append("\n");
        shareText.append("• ").append(getString(R.string.total_cash_collected)).append(": ")
                .append(String.valueOf(reportData.get("totalCashCollected"))).append("\n\n");
        shareText.append(getString(R.string.report_share_hashtags));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_share_subject_with_name, dateStr));
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_report)));
    }
    
    private void updatePerformanceIndicator(double totalSales, double totalProfit, int receiptsCount) {
        String performanceText;
        int cardColor;
        
        // حساب مؤشر الأداء بناءً على عدة معايير
        if (totalSales >= 10000 && receiptsCount >= 10 && totalProfit > 0) {
            performanceText = getString(R.string.performance_excellent);
            cardColor = R.color.performanceExcellent;
        } else if (totalSales >= 5000 && receiptsCount >= 5 && totalProfit > 0) {
            performanceText = getString(R.string.performance_very_good);
            cardColor = R.color.performanceGood;
        } else if (totalSales >= 1000 && receiptsCount >= 2) {
            performanceText = getString(R.string.performance_good);
            cardColor = R.color.performanceAverage;
        } else if (totalSales > 0 || receiptsCount > 0) {
            performanceText = getString(R.string.performance_starting);
            cardColor = R.color.performanceLow;
        } else {
            performanceText = getString(R.string.performance_no_sales);
            cardColor = R.color.performancePoor;
        }
        
        performanceIndicatorTextView.setText(performanceText);
        
        if (performanceCard != null) {
            performanceCard.setCardBackgroundColor(ContextCompat.getColor(getContext(), cardColor));
        }
        
        // حفظ في cache
        reportData.put("performance", performanceText);
    }
    
    /**
     * إعادة تحديث البيانات يدوياً
     */
    public void refreshReportsData() {
        if (getView() != null) {
            loadReportsData();
            Toast.makeText(getContext(), getString(R.string.report_refreshed), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // تحديث البيانات عند العودة للشاشة
        if (reportData.isEmpty()) {
            loadReportsData();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), getString(R.string.storage_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), getString(R.string.storage_permission_denied_cannot_export), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * فتح تقرير تحليل المنتجات مع المخطط الدائري
     */
    private void openProductsAnalyticsReport() {
        Intent intent = new Intent(getContext(), ProductsAnalyticsActivity.class);
        startActivity(intent);
        
        // إضافة تأثير بصري للانتقال
        if (getActivity() != null) {
            getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }
    }
}