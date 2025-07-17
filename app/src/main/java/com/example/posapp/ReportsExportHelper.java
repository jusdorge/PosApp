package com.example.posapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsExportHelper {

    public static void exportReportToCSV(Context context, ReportsUtils.ReportStats stats, Date reportDate) {
        try {
            // Create filename with date
            SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String fileName = "report_" + fileFormat.format(reportDate) + ".csv";

            // Get external storage directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File reportFile = new File(downloadsDir, fileName);

            // Create CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("تقرير نقطة البيع\n");
            csvContent.append("التاريخ,").append(ReportsUtils.formatDate(reportDate)).append("\n");
            csvContent.append("وقت الإنشاء,").append(ReportsUtils.formatDateTime(new Date())).append("\n\n");

            // Add statistics
            csvContent.append("الإحصائيات\n");
            csvContent.append("عدد الفواتير,").append(stats.totalInvoices).append("\n");
            csvContent.append("إجمالي المبيعات,").append(ReportsUtils.formatCurrency(stats.totalSales)).append("\n");
            csvContent.append("المبيعات النقدية,").append(ReportsUtils.formatCurrency(stats.totalCash)).append("\n");
            csvContent.append("المبيعات الآجلة,").append(ReportsUtils.formatCurrency(stats.totalCredit)).append("\n");
            csvContent.append("متوسط الفاتورة,").append(ReportsUtils.formatCurrency(stats.averageSale)).append("\n");
            csvContent.append("أفضل منتج,").append(stats.topProduct).append("\n");
            csvContent.append("كمية أفضل منتج,").append(stats.topProductCount).append("\n");
            csvContent.append("أفضل عميل,").append(stats.bestCustomer).append("\n");
            csvContent.append("مشتريات أفضل عميل,").append(ReportsUtils.formatCurrency(stats.bestCustomerAmount)).append("\n");

            // Write to file
            FileWriter writer = new FileWriter(reportFile);
            writer.write(csvContent.toString());
            writer.close();

            Toast.makeText(context, "تم حفظ التقرير في: " + reportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Share file
            shareFile(context, reportFile);

        } catch (IOException e) {
            Toast.makeText(context, "فشل في حفظ التقرير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportReportToText(Context context, ReportsUtils.ReportStats stats, Date reportDate) {
        try {
            // Create filename with date
            SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String fileName = "report_" + fileFormat.format(reportDate) + ".txt";

            // Get external storage directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File reportFile = new File(downloadsDir, fileName);

            // Create text content
            StringBuilder textContent = new StringBuilder();
            textContent.append("===============================\n");
            textContent.append("        تقرير نقطة البيع        \n");
            textContent.append("===============================\n\n");

            textContent.append("التاريخ: ").append(ReportsUtils.formatDate(reportDate)).append("\n");
            textContent.append("وقت الإنشاء: ").append(ReportsUtils.formatDateTime(new Date())).append("\n\n");

            textContent.append("--- الإحصائيات العامة ---\n");
            textContent.append("عدد الفواتير: ").append(stats.totalInvoices).append("\n");
            textContent.append("إجمالي المبيعات: ").append(ReportsUtils.formatCurrency(stats.totalSales)).append("\n");
            textContent.append("متوسط قيمة الفاتورة: ").append(ReportsUtils.formatCurrency(stats.averageSale)).append("\n\n");

            textContent.append("--- طرق الدفع ---\n");
            textContent.append("المبيعات النقدية: ").append(ReportsUtils.formatCurrency(stats.totalCash));
            textContent.append(" (").append(ReportsUtils.formatPercentage(stats.getCashPercentage())).append(")\n");
            textContent.append("المبيعات الآجلة: ").append(ReportsUtils.formatCurrency(stats.totalCredit));
            textContent.append(" (").append(ReportsUtils.formatPercentage(stats.getCreditPercentage())).append(")\n\n");

            textContent.append("--- أفضل الأداء ---\n");
            textContent.append("أفضل منتج: ").append(stats.topProduct).append(" (").append(stats.topProductCount).append(" وحدة)\n");
            textContent.append("أفضل عميل: ").append(stats.bestCustomer).append(" (").append(ReportsUtils.formatCurrency(stats.bestCustomerAmount)).append(")\n\n");

            textContent.append("--- تقييم الأداء ---\n");
            textContent.append(ReportsUtils.getReportSummary(stats.totalInvoices, stats.totalSales, stats.averageSale));

            textContent.append("\n===============================\n");
            textContent.append("تم إنشاء هذا التقرير بواسطة تطبيق نقطة البيع\n");
            textContent.append("===============================");

            // Write to file
            FileWriter writer = new FileWriter(reportFile);
            writer.write(textContent.toString());
            writer.close();

            Toast.makeText(context, "تم حفظ التقرير في: " + reportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Share file
            shareFile(context, reportFile);

        } catch (IOException e) {
            Toast.makeText(context, "فشل في حفظ التقرير: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void shareFile(Context context, File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "تقرير نقطة البيع - " + file.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, "تقرير مبيعات من تطبيق نقطة البيع");

        try {
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير"));
        } catch (Exception e) {
            Toast.makeText(context, "لا يمكن مشاركة الملف", Toast.LENGTH_SHORT).show();
        }
    }

    public static String generateHTMLReport(ReportsUtils.ReportStats stats, Date reportDate) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html dir='rtl' lang='ar'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>تقرير نقطة البيع</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background-color: #f5f5f5; }");
        html.append(".container { max-width: 800px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 20px rgba(0,0,0,0.1); }");
        html.append(".header { text-align: center; border-bottom: 3px solid #1877F2; padding-bottom: 20px; margin-bottom: 30px; }");
        html.append(".header h1 { color: #1877F2; margin: 0; font-size: 2.5em; }");
        html.append(".date-info { background-color: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px; }");
        html.append(".stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }");
        html.append(".stat-card { background-color: #1877F2; color: white; padding: 20px; border-radius: 8px; text-align: center; }");
        html.append(".stat-card h3 { margin: 0 0 10px 0; font-size: 1.2em; }");
        html.append(".stat-card .value { font-size: 2em; font-weight: bold; }");
        html.append(".section { margin-bottom: 30px; }");
        html.append(".section h2 { color: #1877F2; border-bottom: 2px solid #1877F2; padding-bottom: 10px; }");
        html.append(".info-table { width: 100%; border-collapse: collapse; }");
        html.append(".info-table th, .info-table td { padding: 12px; text-align: right; border-bottom: 1px solid #ddd; }");
        html.append(".info-table th { background-color: #f8f9fa; font-weight: bold; }");
        html.append(".footer { text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>تقرير نقطة البيع</h1>");
        html.append("</div>");

        html.append("<div class='date-info'>");
        html.append("<strong>تاريخ التقرير:</strong> ").append(ReportsUtils.formatDate(reportDate));
        html.append("<br><strong>تاريخ الإنشاء:</strong> ").append(ReportsUtils.formatDateTime(new Date()));
        html.append("</div>");

        html.append("<div class='stats-grid'>");
        html.append("<div class='stat-card'>");
        html.append("<h3>عدد الفواتير</h3>");
        html.append("<div class='value'>").append(stats.totalInvoices).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card'>");
        html.append("<h3>إجمالي المبيعات</h3>");
        html.append("<div class='value'>").append(ReportsUtils.formatCurrency(stats.totalSales)).append("</div>");
        html.append("</div>");

        html.append("<div class='stat-card'>");
        html.append("<h3>متوسط الفاتورة</h3>");
        html.append("<div class='value'>").append(ReportsUtils.formatCurrency(stats.averageSale)).append("</div>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class='section'>");
        html.append("<h2>تفاصيل المبيعات</h2>");
        html.append("<table class='info-table'>");
        html.append("<tr><th>البيان</th><th>القيمة</th><th>النسبة</th></tr>");
        html.append("<tr><td>المبيعات النقدية</td><td>").append(ReportsUtils.formatCurrency(stats.totalCash)).append("</td><td>").append(ReportsUtils.formatPercentage(stats.getCashPercentage())).append("</td></tr>");
        html.append("<tr><td>المبيعات الآجلة</td><td>").append(ReportsUtils.formatCurrency(stats.totalCredit)).append("</td><td>").append(ReportsUtils.formatPercentage(stats.getCreditPercentage())).append("</td></tr>");
        html.append("</table>");
        html.append("</div>");

        html.append("<div class='section'>");
        html.append("<h2>أفضل الأداء</h2>");
        html.append("<table class='info-table'>");
        html.append("<tr><th>البيان</th><th>القيمة</th></tr>");
        html.append("<tr><td>أفضل منتج</td><td>").append(stats.topProduct).append(" (").append(stats.topProductCount).append(" وحدة)</td></tr>");
        html.append("<tr><td>أفضل عميل</td><td>").append(stats.bestCustomer).append(" (").append(ReportsUtils.formatCurrency(stats.bestCustomerAmount)).append(")</td></tr>");
        html.append("</table>");
        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>تم إنشاء هذا التقرير بواسطة تطبيق نقطة البيع</p>");
        html.append("<p>© 2025 جميع الحقوق محفوظة</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }
}