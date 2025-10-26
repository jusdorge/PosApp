package com.example.islamicquiz;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.islamicquiz.model.Customer;
import com.example.islamicquiz.model.Invoice;
import com.example.islamicquiz.model.Product;
import com.example.islamicquiz.model.StockMovement;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.CSVParserBuilder;
import com.opencsv.ICSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Helper class للتعامل مع تصدير واستيراد البيانات بصيغة CSV
 * 
 * ملاحظات هامة:
 * - يستخدم ترميز UTF-8 مع BOM لضمان عرض النصوص العربية بشكل صحيح
 * - يستخدم الفاصلة المنقوطة (;) كفاصل للحقول لأن:
 *   1. Excel في النسخ العربية يتوقع الفاصلة المنقوطة كفاصل افتراضي
 *   2. الفاصلة العادية (,) قد تتداخل مع الفاصلة العشرية في الأرقام العربية
 *   3. يضمن فصل الحقول بشكل صحيح في جميع التطبيقات
 * 
 * التقنيات المستخدمة:
 * - CSVWriterBuilder مع withSeparator(';') للتصدير (يرجع ICSVWriter)
 * - CSVReaderBuilder مع CSVParserBuilder للاستيراد
 * - FileWriter مع StandardCharsets.UTF_8 للترميز الصحيح
 */
public class CSVExportImportHelper {
    private static final String TAG = "CSVHelper";
    private Context context;
    private FirebaseFirestore db;
    
    public interface ExportListener {
        void onExportSuccess(File file);
        void onExportFailure(String error);
    }
    
    public interface ImportListener {
        void onImportSuccess();
        void onImportFailure(String error);
    }
    
    public CSVExportImportHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }
    
    // تصدير جميع البيانات
    public void exportAllData(ExportListener listener) {
        try {
            // إنشاء مجلد للتطبيق
            File appDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "POS_App");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            
            // تصدير كل نوع في ملف منفصل
            exportCustomersToFile(new File(appDir, "customers_" + timestamp + ".csv"));
            exportProductsToFile(new File(appDir, "products_" + timestamp + ".csv"));
            exportInvoicesToFile(new File(appDir, "invoices_" + timestamp + ".csv"));
            exportStockMovementsToFile(new File(appDir, "stock_movements_" + timestamp + ".csv"));
            
            // إرجاع مجلد البيانات
            listener.onExportSuccess(appDir);
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting all data", e);
            listener.onExportFailure("خطأ في تصدير البيانات: " + e.getMessage());
        }
    }
    
    // تصدير العملاء فقط
    public void exportCustomers(ExportListener listener) {
        try {
            File appDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "POS_App");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            File file = new File(appDir, "customers_" + timestamp + ".csv");
            
            exportCustomersToFile(file);
            listener.onExportSuccess(file);
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting customers", e);
            listener.onExportFailure("خطأ في تصدير العملاء: " + e.getMessage());
        }
    }
    
    // تصدير المنتجات فقط
    public void exportProducts(ExportListener listener) {
        try {
            File appDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "POS_App");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            File file = new File(appDir, "products_" + timestamp + ".csv");
            
            exportProductsToFile(file);
            listener.onExportSuccess(file);
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting products", e);
            listener.onExportFailure("خطأ في تصدير المنتجات: " + e.getMessage());
        }
    }
    
    // تصدير الفواتير فقط
    public void exportInvoices(ExportListener listener) {
        try {
            File appDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "POS_App");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            File file = new File(appDir, "invoices_" + timestamp + ".csv");
            
            exportInvoicesToFile(file);
            listener.onExportSuccess(file);
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting invoices", e);
            listener.onExportFailure("خطأ في تصدير الفواتير: " + e.getMessage());
        }
    }
    
    private void exportCustomersToFile(File file) {
        db.collection("customers").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                try {
                    // إنشاء FileWriter مع ترميز UTF-8
                    FileWriter fileWriter = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8);
                    
                    // إضافة BOM للتعرف على UTF-8 في Excel
                    fileWriter.write('\ufeff');
                    
                    // استخدام الفاصلة المنقوطة للنسخ العربية من Excel
                    ICSVWriter writer = new CSVWriterBuilder(fileWriter)
                        .withSeparator(';')
                        .build();
                    
                    // كتابة العناوين
                    String[] headers = {"معرف العميل", "الاسم", "رقم الهاتف", "إجمالي الدين", "خط الطول", "خط العرض"};
                    writer.writeNext(headers);
                    
                    // كتابة البيانات
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Customer customer = document.toObject(Customer.class);
                        customer.setId(document.getId());
                        
                        String[] data = {
                            customer.getId() != null ? customer.getId() : "",
                            customer.getName() != null ? customer.getName() : "",
                            customer.getPhone() != null ? customer.getPhone() : "",
                            String.valueOf(customer.getTotalDebt()),
                            String.valueOf(customer.getLongitude()),
                            String.valueOf(customer.getLatitude())
                        };
                        writer.writeNext(data);
                    }
                    
                    writer.close();
                    fileWriter.close();
                    
                    Log.i(TAG, "Customers exported to: " + file.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error writing customers CSV", e);
                }
            } else {
                Log.e(TAG, "Error getting customers", task.getException());
            }
        });
    }
    
    private void exportProductsToFile(File file) {
        db.collection("products").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                try {
                    // إنشاء FileWriter مع ترميز UTF-8
                    FileWriter fileWriter = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8);
                    
                    // إضافة BOM للتعرف على UTF-8 في Excel
                    fileWriter.write('\ufeff');
                    
                    // استخدام الفاصلة المنقوطة للنسخ العربية من Excel
                    ICSVWriter writer = new CSVWriterBuilder(fileWriter)
                        .withSeparator(';')
                        .build();
                    
                    // كتابة العناوين
                    String[] headers = {"معرف المنتج", "اسم المنتج", "الفئة", "سعر البيع", "سعر الشراء", "الكمية", "الحد الأدنى", "الباركود"};
                    writer.writeNext(headers);
                    
                    // كتابة البيانات
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Product product = document.toObject(Product.class);
                        product.setId(document.getId());
                        
                        String[] data = {
                            product.getId() != null ? product.getId() : "",
                            product.getName() != null ? product.getName() : "",
                            product.getCategory() != null ? product.getCategory() : "",
                            String.valueOf(product.getDefaultPrice()),
                            String.valueOf(product.getCostPrice()),
                            String.valueOf(product.getQuantity()),
                            String.valueOf(product.getMinQuantity()),
                            product.getBarcode() != null ? product.getBarcode() : ""
                        };
                        writer.writeNext(data);
                    }
                    
                    writer.close();
                    fileWriter.close();
                    
                    Log.i(TAG, "Products exported to: " + file.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error writing products CSV", e);
                }
            } else {
                Log.e(TAG, "Error getting products", task.getException());
            }
        });
    }
    
    private void exportInvoicesToFile(File file) {
        db.collection("invoices").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                try {
                    // إنشاء FileWriter مع ترميز UTF-8
                    FileWriter fileWriter = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8);
                    
                    // إضافة BOM للتعرف على UTF-8 في Excel
                    fileWriter.write('\ufeff');
                    
                    // استخدام الفاصلة المنقوطة للنسخ العربية من Excel
                    ICSVWriter writer = new CSVWriterBuilder(fileWriter)
                        .withSeparator(';')
                        .build();
                    
                    // كتابة العناوين
                    String[] headers = {"معرف الفاتورة", "اسم العميل", "رقم هاتف العميل", "مدفوعة", "إجمالي المبلغ", "التاريخ"};
                    writer.writeNext(headers);
                    
                    // كتابة البيانات
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Invoice invoice = document.toObject(Invoice.class);
                        invoice.setId(document.getId());
                        
                        String dateStr = "";
                        if (invoice.getDate() != null) {
                            dateStr = dateFormat.format(invoice.getDate().toDate());
                        }
                        
                        String[] data = {
                            invoice.getDisplayNumber(),
                            invoice.getCustomerName() != null ? invoice.getCustomerName() : "",
                            invoice.getCustomerPhone() != null ? invoice.getCustomerPhone() : "",
                            invoice.isPaid() ? "نعم" : "لا",
                            String.valueOf(invoice.getTotalAmount()),
                            dateStr
                        };
                        writer.writeNext(data);
                    }
                    
                    writer.close();
                    fileWriter.close();
                    
                    Log.i(TAG, "Invoices exported to: " + file.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error writing invoices CSV", e);
                }
            } else {
                Log.e(TAG, "Error getting invoices", task.getException());
            }
        });
    }
    
    private void exportStockMovementsToFile(File file) {
        db.collection("stock_movements").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                try {
                    // إنشاء FileWriter مع ترميز UTF-8
                    FileWriter fileWriter = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8);
                    
                    // إضافة BOM للتعرف على UTF-8 في Excel
                    fileWriter.write('\ufeff');
                    
                    // استخدام الفاصلة المنقوطة للنسخ العربية من Excel
                    ICSVWriter writer = new CSVWriterBuilder(fileWriter)
                        .withSeparator(';')
                        .build();
                    
                    // كتابة العناوين
                    String[] headers = {"معرف الحركة", "معرف المنتج", "اسم المنتج", "الكمية", "نوع الحركة", "السبب", "التاريخ"};
                    writer.writeNext(headers);
                    
                    // كتابة البيانات
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        StockMovement movement = document.toObject(StockMovement.class);
                        movement.setId(document.getId());
                        
                        String dateStr = "";
                        if (movement.getTimestamp() != null) {
                            dateStr = dateFormat.format(movement.getTimestamp().toDate());
                        }
                        
                        String[] data = {
                            movement.getId() != null ? movement.getId() : "",
                            movement.getProductId() != null ? movement.getProductId() : "",
                            movement.getProductName() != null ? movement.getProductName() : "",
                            String.valueOf(movement.getQuantity()),
                            movement.getMovementType() != null ? movement.getMovementType() : "",
                            movement.getReason() != null ? movement.getReason() : "",
                            dateStr
                        };
                        writer.writeNext(data);
                    }
                    
                    writer.close();
                    fileWriter.close();
                    
                    Log.i(TAG, "Stock movements exported to: " + file.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error writing stock movements CSV", e);
                }
            } else {
                Log.e(TAG, "Error getting stock movements", task.getException());
            }
        });
    }
    
    // مشاركة الملف
    public void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            Intent chooser = Intent.createChooser(shareIntent, "مشاركة ملف البيانات");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing file", e);
            Toast.makeText(context, "خطأ في مشاركة الملف", Toast.LENGTH_SHORT).show();
        }
    }
    
    // استيراد العملاء من ملف CSV
    public void importCustomersFromFile(Uri fileUri, ImportListener listener) {
        try {
            // قراءة الملف بترميز UTF-8
            InputStreamReader isr = new InputStreamReader(
                context.getContentResolver().openInputStream(fileUri), 
                java.nio.charset.StandardCharsets.UTF_8
            );
            // استخدام الفاصلة المنقوطة كفاصل للحقول
            CSVReader reader = new CSVReaderBuilder(isr)
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build();
            
            List<String[]> allData = reader.readAll();
            reader.close();
            isr.close();
            
            if (allData.size() <= 1) {
                listener.onImportFailure("الملف فارغ أو يحتوي على العناوين فقط");
                return;
            }
            
            List<Customer> customers = new ArrayList<>();
            
            // تخطي الصف الأول (العناوين) والبدء من الصف الثاني
            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);
                
                if (row.length >= 3) { // على الأقل ID, Name, Phone
                    try {
                        Customer customer = new Customer();
                        
                        // تخطي معرف العميل (العمود 0) - سيتم إنشاؤه تلقائياً
                        if (row.length > 1 && row[1] != null && !row[1].trim().isEmpty()) {
                            customer.setName(row[1].trim());
                        }
                        if (row.length > 2 && row[2] != null && !row[2].trim().isEmpty()) {
                            customer.setPhone(row[2].trim());
                        }
                        if (row.length > 3 && row[3] != null && !row[3].trim().isEmpty()) {
                            try {
                                customer.setTotalDebt(Double.parseDouble(row[3].trim()));
                            } catch (NumberFormatException e) {
                                customer.setTotalDebt(0.0);
                            }
                        }
                        if (row.length > 4 && row[4] != null && !row[4].trim().isEmpty()) {
                            try {
                                customer.setLongitude(Double.parseDouble(row[4].trim()));
                            } catch (NumberFormatException e) {
                                customer.setLongitude(0.0);
                            }
                        }
                        if (row.length > 5 && row[5] != null && !row[5].trim().isEmpty()) {
                            try {
                                customer.setLatitude(Double.parseDouble(row[5].trim()));
                            } catch (NumberFormatException e) {
                                customer.setLatitude(0.0);
                            }
                        }
                        
                        // التحقق من وجود البيانات الأساسية
                        if (customer.getName() != null && !customer.getName().isEmpty()) {
                            customers.add(customer);
                        }
                        
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading customer row " + i, e);
                        // تخطي هذا الصف والمتابعة
                    }
                }
            }
            
            if (customers.isEmpty()) {
                listener.onImportFailure("لم يتم العثور على بيانات صحيحة للاستيراد");
                return;
            }
            
            // رفع البيانات إلى Firebase
            importCustomersToFirebase(customers, listener);
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing customers", e);
            listener.onImportFailure("خطأ في استيراد العملاء: " + e.getMessage());
        }
    }
    
    // استيراد المنتجات من ملف CSV
    public void importProductsFromFile(Uri fileUri, ImportListener listener) {
        try {
            // قراءة الملف بترميز UTF-8
            InputStreamReader isr = new InputStreamReader(
                context.getContentResolver().openInputStream(fileUri), 
                java.nio.charset.StandardCharsets.UTF_8
            );
            // استخدام الفاصلة المنقوطة كفاصل للحقول
            CSVReader reader = new CSVReaderBuilder(isr)
                .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                .build();
            
            List<String[]> allData = reader.readAll();
            reader.close();
            isr.close();
            
            if (allData.size() <= 1) {
                listener.onImportFailure("الملف فارغ أو يحتوي على العناوين فقط");
                return;
            }
            
            List<Product> products = new ArrayList<>();
            
            // تخطي الصف الأول (العناوين) والبدء من الصف الثاني
            for (int i = 1; i < allData.size(); i++) {
                String[] row = allData.get(i);
                
                if (row.length >= 7) { // على الأقل حتى العمود السابع
                    try {
                        Product product = new Product();
                        
                        // تخطي معرف المنتج (العمود 0) - سيتم إنشاؤه تلقائياً
                        if (row.length > 1 && row[1] != null && !row[1].trim().isEmpty()) {
                            product.setName(row[1].trim());
                        }
                        if (row.length > 2 && row[2] != null && !row[2].trim().isEmpty()) {
                            product.setCategory(row[2].trim());
                        }
                        if (row.length > 3 && row[3] != null && !row[3].trim().isEmpty()) {
                            try {
                                product.setDefaultPrice(Double.parseDouble(row[3].trim()));
                            } catch (NumberFormatException e) {
                                product.setDefaultPrice(0.0);
                            }
                        }
                        if (row.length > 4 && row[4] != null && !row[4].trim().isEmpty()) {
                            try {
                                product.setCostPrice(Double.parseDouble(row[4].trim()));
                            } catch (NumberFormatException e) {
                                product.setCostPrice(0.0);
                            }
                        }
                        if (row.length > 5 && row[5] != null && !row[5].trim().isEmpty()) {
                            try {
                                product.setQuantity(Integer.parseInt(row[5].trim()));
                            } catch (NumberFormatException e) {
                                product.setQuantity(0);
                            }
                        }
                        if (row.length > 6 && row[6] != null && !row[6].trim().isEmpty()) {
                            try {
                                product.setMinQuantity(Integer.parseInt(row[6].trim()));
                            } catch (NumberFormatException e) {
                                product.setMinQuantity(0);
                            }
                        }
                        if (row.length > 7 && row[7] != null && !row[7].trim().isEmpty()) {
                            product.setBarcode(row[7].trim());
                        }
                        
                        // التحقق من وجود البيانات الأساسية
                        if (product.getName() != null && !product.getName().isEmpty() &&
                            product.getCategory() != null && !product.getCategory().isEmpty()) {
                            products.add(product);
                        }
                        
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading product row " + i, e);
                        // تخطي هذا الصف والمتابعة
                    }
                }
            }
            
            if (products.isEmpty()) {
                listener.onImportFailure("لم يتم العثور على بيانات صحيحة للاستيراد");
                return;
            }
            
            // رفع البيانات إلى Firebase
            importProductsToFirebase(products, listener);
            
        } catch (Exception e) {
            Log.e(TAG, "Error importing products", e);
            listener.onImportFailure("خطأ في استيراد المنتجات: " + e.getMessage());
        }
    }
    
    private void importCustomersToFirebase(List<Customer> customers, ImportListener listener) {
        int totalCustomers = customers.size();
        final int[] successCount = {0};
        final int[] failCount = {0};
        
        for (Customer customer : customers) {
            db.collection("customers")
                    .add(customer)
                    .addOnSuccessListener(documentReference -> {
                        successCount[0]++;
                        Log.d(TAG, "Customer imported: " + documentReference.getId());
                        
                        if (successCount[0] + failCount[0] == totalCustomers) {
                            if (successCount[0] > 0) {
                                listener.onImportSuccess();
                            } else {
                                listener.onImportFailure("فشل في استيراد جميع العملاء");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        failCount[0]++;
                        Log.e(TAG, "Error importing customer", e);
                        
                        if (successCount[0] + failCount[0] == totalCustomers) {
                            if (successCount[0] > 0) {
                                listener.onImportSuccess();
                            } else {
                                listener.onImportFailure("فشل في استيراد جميع العملاء");
                            }
                        }
                    });
        }
    }
    
    private void importProductsToFirebase(List<Product> products, ImportListener listener) {
        int totalProducts = products.size();
        final int[] successCount = {0};
        final int[] failCount = {0};
        
        for (Product product : products) {
            db.collection("products")
                    .add(product)
                    .addOnSuccessListener(documentReference -> {
                        successCount[0]++;
                        Log.d(TAG, "Product imported: " + documentReference.getId());
                        
                        if (successCount[0] + failCount[0] == totalProducts) {
                            if (successCount[0] > 0) {
                                listener.onImportSuccess();
                            } else {
                                listener.onImportFailure("فشل في استيراد جميع المنتجات");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        failCount[0]++;
                        Log.e(TAG, "Error importing product", e);
                        
                        if (successCount[0] + failCount[0] == totalProducts) {
                            if (successCount[0] > 0) {
                                listener.onImportSuccess();
                            } else {
                                listener.onImportFailure("فشل في استيراد جميع المنتجات");
                            }
                        }
                    });
        }
    }
    
    // إنشاء ملف قالب للاستيراد
    public void createImportTemplate(String templateType, ExportListener listener) {
        try {
            File appDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "POS_App");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            File file = new File(appDir, templateType + "_template_" + timestamp + ".csv");
            
            if ("customers".equals(templateType)) {
                createCustomersTemplate(file);
            } else if ("products".equals(templateType)) {
                createProductsTemplate(file);
            } else {
                listener.onExportFailure("نوع القالب غير مدعوم");
                return;
            }
            
            listener.onExportSuccess(file);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating template", e);
            listener.onExportFailure("خطأ في إنشاء القالب: " + e.getMessage());
        }
    }
    
    private void createCustomersTemplate(File file) throws IOException {
        // إنشاء FileWriter مع ترميز UTF-8
        FileWriter fileWriter = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8);
        
        // إضافة BOM للتعرف على UTF-8 في Excel
        fileWriter.write('\ufeff');
        
        // استخدام الفاصلة المنقوطة للنسخ العربية من Excel
        ICSVWriter writer = new CSVWriterBuilder(fileWriter)
            .withSeparator(';')
            .build();
        
        // كتابة العناوين
        String[] headers = {"معرف العميل (اتركه فارغاً للعملاء الجدد)", "الاسم*", "رقم الهاتف*", "إجمالي الدين (اختياري)", "خط الطول (اختياري)", "خط العرض (اختياري)"};
        writer.writeNext(headers);
        
        // إضافة مثال
        String[] example = {"", "أحمد محمد", "0123456789", "0", "31.2357", "29.9792"};
        writer.writeNext(example);
        
        writer.close();
        fileWriter.close();
    }
    
    private void createProductsTemplate(File file) throws IOException {
        // إنشاء FileWriter مع ترميز UTF-8
        FileWriter fileWriter = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8);
        
        // إضافة BOM للتعرف على UTF-8 في Excel
        fileWriter.write('\ufeff');
        
        // استخدام الفاصلة المنقوطة للنسخ العربية من Excel
        ICSVWriter writer = new CSVWriterBuilder(fileWriter)
            .withSeparator(';')
            .build();
        
        // كتابة العناوين
        String[] headers = {"معرف المنتج (اتركه فارغاً للمنتجات الجديدة)", "اسم المنتج*", "الفئة*", "سعر البيع*", "سعر الشراء*", "الكمية*", "الحد الأدنى*", "الباركود (اختياري)"};
        writer.writeNext(headers);
        
        // إضافة مثال
        String[] example = {"", "عصير برتقال", "مشروبات", "15.50", "10.00", "100", "10", "1234567890123"};
        writer.writeNext(example);
        
        writer.close();
        fileWriter.close();
    }
} 