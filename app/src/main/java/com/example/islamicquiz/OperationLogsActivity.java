package com.example.islamicquiz;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.islamicquiz.model.OperationLog;
import com.example.islamicquiz.service.OperationLogService;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * نشاط عرض أرشيف العمليات
 */
public class OperationLogsActivity extends AppCompatActivity implements OperationLogAdapter.OnOperationLogClickListener {
    
    private RecyclerView operationLogsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView summaryTextView;
    private View emptyStateLayout;
    
    private Button filterAllButton;
    private Button filterCriticalButton;
    private Button filterInvoicesButton;
    private Button filterTodayButton;
    private Button exportLogsButton;
    private Button cleanOldLogsButton;
    
    private OperationLogAdapter adapter;
    private List<OperationLog> allOperationLogs;
    private List<OperationLog> filteredOperationLogs;
    
    private OperationLogService operationLogService;
    private FilterType currentFilter = FilterType.ALL;
    
    private enum FilterType {
        ALL, CRITICAL, INVOICES, TODAY
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operation_logs);
        
        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.operation_logs_title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        initViews();
        setupButtons();
        setupRecyclerView();
        
        // تهيئة خدمة العمليات
        operationLogService = OperationLogService.getInstance(this);
        
        // تحميل البيانات
        loadOperationLogs();
    }
    
    private void initViews() {
        operationLogsRecyclerView = findViewById(R.id.operationLogsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        summaryTextView = findViewById(R.id.summaryTextView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        
        filterAllButton = findViewById(R.id.filterAllButton);
        filterCriticalButton = findViewById(R.id.filterCriticalButton);
        filterInvoicesButton = findViewById(R.id.filterInvoicesButton);
        filterTodayButton = findViewById(R.id.filterTodayButton);
        exportLogsButton = findViewById(R.id.exportLogsButton);
        cleanOldLogsButton = findViewById(R.id.cleanOldLogsButton);
    }
    
    private void setupButtons() {
        // أزرار الفلترة
        filterAllButton.setOnClickListener(v -> setFilter(FilterType.ALL));
        filterCriticalButton.setOnClickListener(v -> setFilter(FilterType.CRITICAL));
        filterInvoicesButton.setOnClickListener(v -> setFilter(FilterType.INVOICES));
        filterTodayButton.setOnClickListener(v -> setFilter(FilterType.TODAY));
        
        // أزرار الإجراءات
        exportLogsButton.setOnClickListener(v -> exportLogs());
        cleanOldLogsButton.setOnClickListener(v -> showCleanOldLogsDialog());
        
        // تحديث السحب
        swipeRefreshLayout.setOnRefreshListener(this::loadOperationLogs);
        
        // تحديد الفلتر الافتراضي
        updateFilterButtons();
    }
    
    private void setupRecyclerView() {
        allOperationLogs = new ArrayList<>();
        filteredOperationLogs = new ArrayList<>();
        
        adapter = new OperationLogAdapter(this, filteredOperationLogs);
        adapter.setOnOperationLogClickListener(this);
        
        operationLogsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        operationLogsRecyclerView.setAdapter(adapter);
    }
    
    private void loadOperationLogs() {
        swipeRefreshLayout.setRefreshing(true);
        
        // تحميل سجلات المستخدم الحالي (أو كل السجلات للمديرين)
        operationLogService.getUserOperationLogs(500)
                .thenAccept(logs -> {
                    runOnUiThread(() -> {
                        allOperationLogs.clear();
                        allOperationLogs.addAll(logs);
                        applyCurrentFilter();
                        updateSummary();
                        swipeRefreshLayout.setRefreshing(false);
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        android.util.Log.e("OperationLogsActivity", getString(R.string.failed_load_operation_logs), throwable);
                        
                        // تحسين رسالة الخطأ للمستخدم
                        String errorMessage;
                        if (throwable.getMessage() != null) {
                            if (throwable.getMessage().contains("PERMISSION_DENIED")) {
                                errorMessage = getString(R.string.no_permission_view_operation_logs);
                            } else if (throwable.getMessage().contains("network")) {
                                errorMessage = getString(R.string.check_internet_try_again);
                            } else if (throwable.getMessage().contains("index")) {
                                errorMessage = getString(R.string.database_being_prepared_try_later);
                            } else {
                                errorMessage = getString(R.string.failed_load_operation_logs_with_error, throwable.getMessage());
                            }
                        } else {
                            errorMessage = getString(R.string.failed_load_operation_logs_try_again);
                        }
                        
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        swipeRefreshLayout.setRefreshing(false);
                        showEmptyState(true);
                    });
                    return null;
                });
    }
    
    private void setFilter(FilterType filterType) {
        currentFilter = filterType;
        updateFilterButtons();
        applyCurrentFilter();
    }
    
    private void updateFilterButtons() {
        // إعادة تعيين جميع الأزرار
        resetButtonStyle(filterAllButton);
        resetButtonStyle(filterCriticalButton);
        resetButtonStyle(filterInvoicesButton);
        resetButtonStyle(filterTodayButton);
        
        // تمييز الزر النشط
        Button activeButton = null;
        switch (currentFilter) {
            case ALL:
                activeButton = filterAllButton;
                break;
            case CRITICAL:
                activeButton = filterCriticalButton;
                break;
            case INVOICES:
                activeButton = filterInvoicesButton;
                break;
            case TODAY:
                activeButton = filterTodayButton;
                break;
        }
        
        if (activeButton != null) {
            activeButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
    }
    
    private void resetButtonStyle(Button button) {
        button.setBackgroundColor(getResources().getColor(R.color.colorAccent));
    }
    
    private void applyCurrentFilter() {
        filteredOperationLogs.clear();
        
        switch (currentFilter) {
            case ALL:
                filteredOperationLogs.addAll(allOperationLogs);
                break;
            case CRITICAL:
                for (OperationLog log : allOperationLogs) {
                    if (log.isCriticalOperation()) {
                        filteredOperationLogs.add(log);
                    }
                }
                break;
            case INVOICES:
                for (OperationLog log : allOperationLogs) {
                    if (log.getEntityType() == OperationLog.EntityType.INVOICE) {
                        filteredOperationLogs.add(log);
                    }
                }
                break;
            case TODAY:
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                Timestamp todayStart = new Timestamp(today.getTime());
                
                for (OperationLog log : allOperationLogs) {
                    if (log.getTimestamp() != null && log.getTimestamp().compareTo(todayStart) >= 0) {
                        filteredOperationLogs.add(log);
                    }
                }
                break;
        }
        
        adapter.updateOperationLogs(filteredOperationLogs);
        showEmptyState(filteredOperationLogs.isEmpty());
    }
    
    private void updateSummary() {
        int totalLogs = allOperationLogs.size();
        int criticalLogs = 0;
        int todayLogs = 0;
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Timestamp todayStart = new Timestamp(today.getTime());
        
        for (OperationLog log : allOperationLogs) {
            if (log.isCriticalOperation()) {
                criticalLogs++;
            }
            if (log.getTimestamp() != null && log.getTimestamp().compareTo(todayStart) >= 0) {
                todayLogs++;
            }
        }
        
        String summaryText = getString(R.string.operation_logs_summary_format, totalLogs, criticalLogs, todayLogs, filteredOperationLogs.size());
        
        summaryTextView.setText(summaryText);
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            operationLogsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            operationLogsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void exportLogs() {
        // TODO: تطبيق تصدير السجلات
        Toast.makeText(this, getString(R.string.export_feature_in_development), Toast.LENGTH_SHORT).show();
    }
    
    private void showCleanOldLogsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.clean_old_logs_title))
                .setMessage(getString(R.string.clean_old_logs_message))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(getString(R.string.clean_label), (dialog, which) -> cleanOldLogs())
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    private void cleanOldLogs() {
        Toast.makeText(this, getString(R.string.cleaning_old_logs), Toast.LENGTH_SHORT).show();
        
        operationLogService.cleanOldLogs()
                .thenAccept(deletedCount -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.deleted_old_logs_success, deletedCount),
                                Toast.LENGTH_LONG).show();
                        loadOperationLogs(); // إعادة تحميل البيانات
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.failed_clean_old_logs, throwable.getMessage()),
                                Toast.LENGTH_LONG).show();
                    });
                    return null;
                });
    }
    
    @Override
    public void onOperationLogClick(OperationLog operationLog) {
        // عرض تفاصيل العملية
        showOperationLogDetails(operationLog);
    }
    
    private void showOperationLogDetails(OperationLog operationLog) {
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        
        StringBuilder details = new StringBuilder();
        details.append(getString(R.string.operation_details_header)).append("\n\n");
        details.append(getString(R.string.operation_type_label)).append(operationLog.getOperationType().getArabicName()).append("\n");
        details.append(getString(R.string.entity_label)).append(operationLog.getEntityType().getArabicName()).append("\n");
        details.append(getString(R.string.id_label)).append(operationLog.getEntityId()).append("\n");
        details.append(getString(R.string.user_label)).append(operationLog.getUserName()).append("\n");
        
        if (operationLog.getTimestamp() != null) {
            details.append(getString(R.string.timestamp_label)).append(fullDateFormat.format(operationLog.getTimestamp().toDate())).append("\n");
        }
        
        details.append(getString(R.string.description_label)).append(operationLog.getDescription()).append("\n");
        
        if (operationLog.getDeviceInfo() != null) {
            details.append(getString(R.string.device_label)).append(operationLog.getDeviceInfo()).append("\n");
        }
        
        if (operationLog.getAppVersion() != null) {
            details.append(getString(R.string.app_version_label)).append(operationLog.getAppVersion()).append("\n");
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.operation_details_title))
                .setMessage(details.toString())
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(getString(R.string.close), (dialog, which) -> dialog.dismiss())
                .show();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}