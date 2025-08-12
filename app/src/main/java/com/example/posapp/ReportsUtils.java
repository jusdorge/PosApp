package com.example.posapp;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ReportsUtils {

    public static String formatCurrency(double amount) {
        return CurrencyUtils.formatCurrencyForReports(amount);
    }

    public static String formatDate(Date date) {
        return ArabicNumberUtils.formatShortDateWithArabicNumbers(date);
    }

    public static String formatDateTime(Date date) {
        return ArabicNumberUtils.formatDateTimeWithArabicNumbers(date);
    }

    public static Date getStartOfDay(Calendar calendar) {
        Calendar start = (Calendar) calendar.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return start.getTime();
    }

    public static Date getEndOfDay(Calendar calendar) {
        Calendar end = (Calendar) calendar.clone();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        return end.getTime();
    }

    public static String getDayName(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        if (isSameDay(calendar, today)) {
            return ArabicNumberUtils.convertToArabicNumbers(new SimpleDateFormat("EEEE", Locale.getDefault()).format(today.getTime()));
        } else if (isSameDay(calendar, yesterday)) {
            Calendar tmp = (Calendar) yesterday.clone();
            return ArabicNumberUtils.convertToArabicNumbers(new SimpleDateFormat("EEEE", Locale.getDefault()).format(tmp.getTime()));
        } else {
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            String dayName = dayFormat.format(calendar.getTime());
            return ArabicNumberUtils.convertToArabicNumbers(dayName);
        }
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static View createSimpleBarChart(Context context, Map<String, Integer> data, int maxValue) {
        LinearLayout chartLayout = new LinearLayout(context);
        chartLayout.setOrientation(LinearLayout.VERTICAL);
        chartLayout.setPadding(16, 16, 16, 16);

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            LinearLayout barContainer = new LinearLayout(context);
            barContainer.setOrientation(LinearLayout.HORIZONTAL);
            barContainer.setPadding(0, 4, 0, 4);

            // Label
            TextView label = new TextView(context);
            label.setText(entry.getKey());
            label.setTextSize(12);
            label.setMinWidth(150);

            // Bar
            View bar = new View(context);
            int barWidth = (int) (200 * ((double) entry.getValue() / maxValue));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(barWidth, 20);
            bar.setLayoutParams(barParams);
            bar.setBackgroundColor(Color.parseColor("#1877F2"));

            // Value
            TextView value = new TextView(context);
            value.setText(String.valueOf(entry.getValue()));
            value.setTextSize(12);
            value.setPadding(8, 0, 0, 0);

            barContainer.addView(label);
            barContainer.addView(bar);
            barContainer.addView(value);

            chartLayout.addView(barContainer);
        }

        return chartLayout;
    }

    public static String getReportSummary(int totalInvoices, double totalSales, double averageSale) {
        StringBuilder summary = new StringBuilder();
        summary.append(AppGlobals.getContext().getString(R.string.report_summary_title)).append("\n");
        summary.append(AppGlobals.getContext().getString(R.string.report_summary_invoices_count, totalInvoices)).append("\n");
        summary.append(AppGlobals.getContext().getString(R.string.report_summary_total_sales, formatCurrency(totalSales))).append("\n");
        summary.append(AppGlobals.getContext().getString(R.string.report_summary_avg_invoice, formatCurrency(averageSale))).append("\n");

        if (totalInvoices > 0) {
            summary.append(AppGlobals.getContext().getString(R.string.report_summary_performance_prefix)).append(" ");
            if (averageSale > 100) {
                summary.append(AppGlobals.getContext().getString(R.string.report_performance_excellent));
            } else if (averageSale > 50) {
                summary.append(AppGlobals.getContext().getString(R.string.report_performance_good));
            } else {
                summary.append(AppGlobals.getContext().getString(R.string.report_performance_needs_improvement));
            }
        }

        return summary.toString();
    }

    public static double calculateProfitMargin(double totalSales, double totalCost) {
        if (totalSales == 0) return 0;
        return ((totalSales - totalCost) / totalSales) * 100;
    }

    public static String getPerformanceEmoji(double value, double threshold) {
        if (value >= threshold * 1.2) return "🔥"; // Excellent
        if (value >= threshold) return "✅"; // Good
        if (value >= threshold * 0.8) return "⚠️"; // Warning
        return "📉"; // Poor
    }

    public static String formatPercentage(double percentage) {
        return String.format(java.util.Locale.US, "%.1f%%", percentage);
    }

    public static class ReportStats {
        public int totalInvoices;
        public double totalSales;
        public double totalCash;
        public double totalCredit;
        public double averageSale;
        public String topProduct;
        public int topProductCount;
        public String bestCustomer;
        public double bestCustomerAmount;

        public ReportStats() {
            this.totalInvoices = 0;
            this.totalSales = 0.0;
            this.totalCash = 0.0;
            this.totalCredit = 0.0;
            this.averageSale = 0.0;
            this.topProduct = AppGlobals.getContext().getString(R.string.not_specified);
            this.topProductCount = 0;
            this.bestCustomer = AppGlobals.getContext().getString(R.string.not_specified);
            this.bestCustomerAmount = 0.0;
        }

        public void calculateAverageSale() {
            this.averageSale = totalInvoices > 0 ? totalSales / totalInvoices : 0;
        }

        public double getCashPercentage() {
            return totalSales > 0 ? (totalCash / totalSales) * 100 : 0;
        }

        public double getCreditPercentage() {
            return totalSales > 0 ? (totalCredit / totalSales) * 100 : 0;
        }
    }
}
