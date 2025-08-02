package com.example.posapp;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductsAnalyticsActivity extends AppCompatActivity {
    private static final String TAG = "ProductsAnalytics";
    
    private FirebaseFirestore db;
    private PieChart pieChart;
    private RecyclerView recyclerView;
    private TextView totalProductsTextView;
    private TextView dateRangeTextView;
    private TextView totalQuantityTextView;
    private TextView topRevenueTextView;
    private TextView averageQuantityTextView;
    private TextView availableProductsTextView;
    private TextView notSoldProductsTextView;
    
    // Date navigation controls
    private Button previousDayButton;
    private Button nextDayButton;
    private Button todayButton;
    private Button datePickerButton;
    
    private ProductAnalyticsAdapter adapter;
    private List<ProductAnalyticsData> analyticsDataList;
    private List<ProductAnalyticsData> allProductsList;
    private Calendar selectedDate;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_analytics);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        
        // Initialize selected date to today
        selectedDate = Calendar.getInstance();
        
        // Setup toolbar
        setupToolbar();
        
        // Initialize views
        initializeViews();
        
        // Setup date navigation
        setupDateNavigation();
        
        // Load analytics data
        loadProductsAnalytics();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("تحليل المنتجات");
        }
    }
    
    private void initializeViews() {
        pieChart = findViewById(R.id.pieChart);
        recyclerView = findViewById(R.id.recyclerView);
        totalProductsTextView = findViewById(R.id.totalProductsTextView);
        dateRangeTextView = findViewById(R.id.dateRangeTextView);
        totalQuantityTextView = findViewById(R.id.totalQuantityTextView);
        topRevenueTextView = findViewById(R.id.topRevenueTextView);
        averageQuantityTextView = findViewById(R.id.averageQuantityTextView);
        availableProductsTextView = findViewById(R.id.availableProductsTextView);
        notSoldProductsTextView = findViewById(R.id.notSoldProductsTextView);
        
        // Date navigation buttons
        previousDayButton = findViewById(R.id.previousDayButton);
        nextDayButton = findViewById(R.id.nextDayButton);
        todayButton = findViewById(R.id.todayButton);
        datePickerButton = findViewById(R.id.datePickerButton);
        
        // Setup pie chart
        setupPieChart();
        
        // Setup RecyclerView
        analyticsDataList = new ArrayList<>();
        allProductsList = new ArrayList<>();
        adapter = new ProductAnalyticsAdapter(analyticsDataList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Set initial date display
        updateDateDisplay();
    }
    
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("📊\nتوزيع المبيعات");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.DKGRAY);
        
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        
        // Add animation
        pieChart.animateY(1400);
        
        // Legend
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextSize(12f);
        pieChart.getLegend().setFormSize(14f);
        pieChart.getLegend().setXEntrySpace(12f);
        pieChart.getLegend().setYEntrySpace(8f);
    }
    
    private void setupDateNavigation() {
        previousDayButton.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
            loadProductsAnalytics();
        });
        
        nextDayButton.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
            loadProductsAnalytics();
        });
        
        todayButton.setOnClickListener(v -> {
            selectedDate = Calendar.getInstance();
            updateDateDisplay();
            loadProductsAnalytics();
        });
        
        datePickerButton.setOnClickListener(v -> showDatePicker());
    }
    
    private void updateDateDisplay() {
        String dateStr = ArabicNumberUtils.formatLongDateWithArabicNumbers(selectedDate.getTime());
        dateRangeTextView.setText("📅 " + dateStr);
        
        // Update button states
        Calendar today = Calendar.getInstance();
        boolean isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                         selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        
        todayButton.setEnabled(!isToday);
        nextDayButton.setEnabled(!isToday);
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateDisplay();
                    loadProductsAnalytics();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void loadProductsAnalytics() {
        // First, load all products from database
        loadAllProducts();
    }
    
    private void loadAllProducts() {
        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allProductsList.clear();
                    
                    // Add all products with zero sales initially
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String productName = document.getString("name");
                        if (productName != null) {
                            allProductsList.add(new ProductAnalyticsData(productName, 0, 0.0));
                        }
                    }
                    
                    // Now load sales data for the selected date
                    loadSalesData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ خطأ في تحميل المنتجات: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    
    private void loadSalesData() {
        // Get selected date range
        Timestamp[] dateRange = getDateRange();
        
        db.collection("invoices")
                .whereGreaterThanOrEqualTo("date", dateRange[0])
                .whereLessThanOrEqualTo("date", dateRange[1])
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, ProductAnalyticsData> productSales = new HashMap<>();
                    int totalQuantitySold = 0;
                    
                    // Process all invoices
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Object itemsObj = document.get("items");
                        if (itemsObj instanceof List) {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                            for (Map<String, Object> item : items) {
                                String productName = (String) item.get("productName");
                                Object quantityObj = item.get("quantity");
                                Object priceObj = item.get("price");
                                
                                if (productName != null && quantityObj != null && priceObj != null) {
                                    int quantity = 0;
                                    double price = 0.0;
                                    
                                    // Handle quantity
                                    if (quantityObj instanceof Long) {
                                        quantity = ((Long) quantityObj).intValue();
                                    } else if (quantityObj instanceof Integer) {
                                        quantity = (Integer) quantityObj;
                                    }
                                    
                                    // Handle price
                                    if (priceObj instanceof Double) {
                                        price = (Double) priceObj;
                                    } else if (priceObj instanceof Long) {
                                        price = ((Long) priceObj).doubleValue();
                                    }
                                    
                                    totalQuantitySold += quantity;
                                    
                                    // Update or create product analytics data
                                    ProductAnalyticsData data = productSales.get(productName);
                                    if (data == null) {
                                        data = new ProductAnalyticsData(productName, quantity, price * quantity);
                                        productSales.put(productName, data);
                                    } else {
                                        data.addSale(quantity, price * quantity);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Merge sales data with all products list
                    for (ProductAnalyticsData productData : allProductsList) {
                        ProductAnalyticsData salesData = productSales.get(productData.getProductName());
                        if (salesData != null) {
                            productData.addSale(salesData.getQuantitySold(), salesData.getTotalRevenue());
                        }
                    }
                    
                    // Update UI with comprehensive data
                    updateAnalyticsUI(allProductsList, totalQuantitySold);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ خطأ في تحميل البيانات: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    
    private void updateAnalyticsUI(List<ProductAnalyticsData> allProducts, int totalQuantitySold) {
        // Filter to show only sold products and sort by quantity (descending)
        analyticsDataList.clear();
        for (ProductAnalyticsData product : allProducts) {
            if (product.getQuantitySold() > 0) {
                analyticsDataList.add(product);
            }
        }
        Collections.sort(analyticsDataList, (a, b) -> Integer.compare(b.getQuantitySold(), a.getQuantitySold()));
        
        // Calculate statistics for sold products only
        double topRevenue = 0.0;
        double totalRevenue = 0.0;
        int totalProductsInDB = allProducts.size();
        int soldProductsCount = analyticsDataList.size(); // Only sold products are in the list now
        int notSoldProductsCount = totalProductsInDB - soldProductsCount;
        
        for (ProductAnalyticsData data : analyticsDataList) {
            totalRevenue += data.getTotalRevenue();
            if (topRevenue < data.getTotalRevenue()) {
                topRevenue = data.getTotalRevenue();
            }
        }
        
        double averageQuantity = soldProductsCount > 0 ? (double) totalQuantitySold / soldProductsCount : 0.0;
        
        // Update summary TextViews - show both sold and total products info
        totalProductsTextView.setText("📦 " + soldProductsCount + " من " + totalProductsInDB + " منتج");
        totalQuantityTextView.setText("📊 " + totalQuantitySold + " قطعة مباعة");
        topRevenueTextView.setText("💰 أعلى إيراد: " + CurrencyUtils.formatCurrency(topRevenue));
        averageQuantityTextView.setText("📈 متوسط الكمية: " + String.format("%.1f", averageQuantity));
        availableProductsTextView.setText("✅ منتجات مباعة: " + soldProductsCount);
        notSoldProductsTextView.setText("⭕ منتجات لم تُباع: " + notSoldProductsCount);
        
        // Update RecyclerView
        adapter.notifyDataSetChanged();
        
        // Update pie chart (show top 8 products + others)
        updatePieChart(analyticsDataList, totalQuantitySold);
    }
    
    private void updatePieChart(List<ProductAnalyticsData> dataList, int totalQuantity) {
        List<PieEntry> entries = new ArrayList<>();
        
        // Show top 7 products individually
        int othersQuantity = 0;
        int maxItems = Math.min(7, dataList.size());
        
        for (int i = 0; i < maxItems; i++) {
            ProductAnalyticsData data = dataList.get(i);
            entries.add(new PieEntry(data.getQuantitySold(), data.getProductName()));
        }
        
        // Group remaining products as "أخرى"
        for (int i = maxItems; i < dataList.size(); i++) {
            othersQuantity += dataList.get(i).getQuantitySold();
        }
        
        if (othersQuantity > 0) {
            entries.add(new PieEntry(othersQuantity, "منتجات أخرى"));
        }
        
        // Create dataset
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        
        // Set colors
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(64, 89, 128));   // أزرق داكن
        colors.add(Color.rgb(149, 165, 124)); // أخضر
        colors.add(Color.rgb(217, 184, 162)); // بيج
        colors.add(Color.rgb(191, 134, 134)); // وردي
        colors.add(Color.rgb(179, 48, 80));   // أحمر
        colors.add(Color.rgb(193, 37, 82));   // أحمر فاتح
        colors.add(Color.rgb(255, 102, 0));   // برتقالي
        colors.add(Color.rgb(245, 199, 0));   // أصفر
        
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        
        // Create pie data
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieData.setDrawValues(true);
        
        // Set data to chart
        pieChart.setData(pieData);
        pieChart.invalidate();
    }
    
    private Timestamp[] getDateRange() {
        // Get selected date range
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
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Data class for product analytics
    public static class ProductAnalyticsData {
        private String productName;
        private int quantitySold;
        private double totalRevenue;
        
        public ProductAnalyticsData(String productName, int quantitySold, double totalRevenue) {
            this.productName = productName;
            this.quantitySold = quantitySold;
            this.totalRevenue = totalRevenue;
        }
        
        public void addSale(int quantity, double revenue) {
            this.quantitySold += quantity;
            this.totalRevenue += revenue;
        }
        
        // Getters
        public String getProductName() { return productName; }
        public int getQuantitySold() { return quantitySold; }
        public double getTotalRevenue() { return totalRevenue; }
        public double getPercentage(int totalQuantity) {
            return totalQuantity > 0 ? (quantitySold * 100.0) / totalQuantity : 0.0;
        }
    }
}