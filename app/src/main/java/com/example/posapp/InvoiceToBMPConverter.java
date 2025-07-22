package com.example.posapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.posapp.model.Invoice;
import com.example.posapp.model.InvoiceItem;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * فئة تحويل الفواتير إلى صور BMP مع الحفاظ على النصوص العربية
 */
public class InvoiceToBMPConverter {
    private static final String TAG = "InvoiceToBMPConverter";
    
    // إعدادات الصورة
    private static final int IMAGE_WIDTH = 800;   // عرض الصورة
    private static final int IMAGE_HEIGHT = 1200; // ارتفاع الصورة الافتراضي (سيتم التعديل حسب المحتوى)
    private static final int PADDING = 40;         // الحشو الجانبي
    private static final int LINE_SPACING = 10;   // المسافة بين الأسطر
    
    // إعدادات الخطوط
    private static final int TITLE_TEXT_SIZE = 32;      // حجم خط العنوان
    private static final int HEADER_TEXT_SIZE = 24;     // حجم خط العناوين الفرعية
    private static final int NORMAL_TEXT_SIZE = 20;     // حجم الخط العادي
    private static final int SMALL_TEXT_SIZE = 16;      // حجم الخط الصغير
    
    private Context context;
    
    public InvoiceToBMPConverter(Context context) {
        this.context = context;
    }
    
    /**
     * تحويل فاتورة إلى صورة BMP
     */
    public void convertInvoiceToBMP(Invoice invoice, ConvertCallback callback) {
        if (invoice == null) {
            callback.onError("الفاتورة غير موجودة");
            return;
        }
        
        try {
            // إنشاء الـ bitmap
            Bitmap invoiceBitmap = createInvoiceBitmap(invoice);
            
            // حفظ الصورة
            String fileName = "فاتورة_" + invoice.getId() + "_" + 
                            System.currentTimeMillis() + ".bmp";
            File savedFile = saveBitmapAsBMP(invoiceBitmap, fileName);
            
            if (savedFile != null) {
                callback.onSuccess(savedFile, invoiceBitmap);
            } else {
                callback.onError("فشل في حفظ الصورة");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting invoice to BMP", e);
            callback.onError("خطأ في التحويل: " + e.getMessage());
        }
    }
    
    /**
     * إنشاء bitmap للفاتورة
     */
    private Bitmap createInvoiceBitmap(Invoice invoice) {
        // حساب الارتفاع المطلوب
        int requiredHeight = calculateRequiredHeight(invoice);
        
        // إنشاء bitmap
        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, requiredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية بيضاء
        canvas.drawColor(Color.WHITE);
        
        // رسم الفاتورة
        int currentY = drawInvoiceContent(canvas, invoice);
        
        return bitmap;
    }
    
    /**
     * حساب الارتفاع المطلوب للفاتورة
     */
    private int calculateRequiredHeight(Invoice invoice) {
        int height = PADDING * 2; // الحشو العلوي والسفلي
        
        // العنوان الرئيسي
        height += TITLE_TEXT_SIZE + LINE_SPACING * 2;
        
        // معلومات الفاتورة الأساسية (6 أسطر تقريباً)
        height += (NORMAL_TEXT_SIZE + LINE_SPACING) * 7;
        
        // خط فاصل
        height += 20;
        
        // عنوان العناصر
        height += HEADER_TEXT_SIZE + LINE_SPACING;
        
        // العناصر
        if (invoice.getItems() != null) {
            height += invoice.getItems().size() * (NORMAL_TEXT_SIZE + SMALL_TEXT_SIZE + LINE_SPACING * 3);
        }
        
        // خط فاصل
        height += 20;
        
        // المجموع
        height += (HEADER_TEXT_SIZE + LINE_SPACING) * 2;
        
        // QR Code
        height += 200; // حجم QR Code + مسافة
        
        // النهاية
        height += NORMAL_TEXT_SIZE + LINE_SPACING * 2;
        
        return Math.max(height, IMAGE_HEIGHT);
    }
    
    /**
     * رسم محتوى الفاتورة على Canvas
     */
    private int drawInvoiceContent(Canvas canvas, Invoice invoice) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.RIGHT); // النص العربي من اليمين
        paint.setColor(Color.BLACK);
        
        int currentY = PADDING;
        int rightMargin = IMAGE_WIDTH - PADDING;
        int leftMargin = PADDING;
        
        // العنوان الرئيسي
        paint.setTextSize(TITLE_TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        currentY += TITLE_TEXT_SIZE;
        canvas.drawText("فاتورة مبيعات", IMAGE_WIDTH / 2, currentY, paint);
        currentY += LINE_SPACING * 2;
        
        // رسم خط فاصل
        paint.setStrokeWidth(2);
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint);
        currentY += 20;
        
        // العودة للنص العادي
        paint.setTextSize(NORMAL_TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.RIGHT);
        
        // معلومات الفاتورة
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("ar"));
        
        currentY += NORMAL_TEXT_SIZE;
        canvas.drawText("رقم الفاتورة: " + (invoice.getId() != null ? invoice.getId() : "غير محدد"), 
                       rightMargin, currentY, paint);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        if (invoice.getDate() != null) {
            canvas.drawText("التاريخ: " + dateFormat.format(invoice.getDate().toDate()), 
                           rightMargin, currentY, paint);
        }
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        canvas.drawText("اسم العميل: " + (invoice.getCustomerName() != null ? invoice.getCustomerName() : "غير محدد"), 
                       rightMargin, currentY, paint);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        canvas.drawText("هاتف العميل: " + (invoice.getCustomerPhone() != null ? invoice.getCustomerPhone() : "غير محدد"), 
                       rightMargin, currentY, paint);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        String paymentMethod = invoice.isPaid() ? "نقدي" : "آجل";
        canvas.drawText("طريقة الدفع: " + paymentMethod, rightMargin, currentY, paint);
        
        currentY += LINE_SPACING * 2;
        
        // رسم خط فاصل
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint);
        currentY += 20;
        
        // عنوان العناصر
        paint.setTextSize(HEADER_TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        currentY += HEADER_TEXT_SIZE;
        canvas.drawText("عناصر الفاتورة", rightMargin, currentY, paint);
        currentY += LINE_SPACING;
        
        // رسم العناصر
        paint.setTextSize(NORMAL_TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT);
        
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            int itemNumber = 1;
            for (InvoiceItem item : invoice.getItems()) {
                currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
                
                // اسم المنتج مع الرقم التسلسلي
                String productLine = itemNumber + ". " + (item.getProductName() != null ? item.getProductName() : "منتج غير محدد");
                canvas.drawText(productLine, rightMargin, currentY, paint);
                
                // تفاصيل السعر والكمية
                currentY += SMALL_TEXT_SIZE + LINE_SPACING;
                paint.setTextSize(SMALL_TEXT_SIZE);
                paint.setColor(Color.GRAY);
                
                String details = String.format(new Locale("ar"), 
                    "الكمية: %d × %.2f = %.2f دج", 
                    item.getQuantity(), item.getPrice(), item.getQuantity() * item.getPrice());
                canvas.drawText(details, rightMargin, currentY, paint);
                
                // العودة للإعدادات العادية
                paint.setTextSize(NORMAL_TEXT_SIZE);
                paint.setColor(Color.BLACK);
                
                currentY += LINE_SPACING;
                itemNumber++;
            }
        } else {
            currentY += NORMAL_TEXT_SIZE;
            paint.setColor(Color.GRAY);
            canvas.drawText("لا توجد عناصر في هذه الفاتورة", rightMargin, currentY, paint);
            paint.setColor(Color.BLACK);
        }
        
        currentY += LINE_SPACING * 2;
        
        // رسم خط فاصل
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint);
        currentY += 20;
        
        // المجموع الكلي
        paint.setTextSize(HEADER_TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(Color.parseColor("#2196F3")); // اللون الأزرق
        
        currentY += HEADER_TEXT_SIZE;
        String totalText = String.format(new Locale("ar"), "المجموع الكلي: %.2f دج", invoice.getTotalAmount());
        canvas.drawText(totalText, rightMargin, currentY, paint);
        
        currentY += LINE_SPACING * 2;
        
        // إضافة QR Code
        currentY = drawQRCode(canvas, invoice, currentY);
        
        // رسالة الشكر
        paint.setTextSize(NORMAL_TEXT_SIZE);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        canvas.drawText("شكراً لتعاملكم معنا", IMAGE_WIDTH / 2, currentY, paint);
        
        return currentY;
    }
    
    /**
     * رسم QR Code على الفاتورة
     */
    private int drawQRCode(Canvas canvas, Invoice invoice, int startY) {
        try {
            // إنشاء محتوى QR Code
            String qrData = generateQRData(invoice);
            
            // إنشاء QR Code
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 150, 150, hints);
            
            // تحويل إلى Bitmap
            Bitmap qrBitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.RGB_565);
            for (int x = 0; x < 150; x++) {
                for (int y = 0; y < 150; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            // رسم QR Code في المنتصف
            int qrX = (IMAGE_WIDTH - 150) / 2;
            int qrY = startY + 20;
            canvas.drawBitmap(qrBitmap, qrX, qrY, null);
            
            // إضافة نص تحت QR Code
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextSize(SMALL_TEXT_SIZE);
            paint.setColor(Color.GRAY);
            paint.setTextAlign(Paint.Align.CENTER);
            
            int textY = qrY + 150 + SMALL_TEXT_SIZE + 10;
            canvas.drawText("امسح الكود للحصول على تفاصيل الفاتورة", IMAGE_WIDTH / 2, textY, paint);
            
            return textY + LINE_SPACING;
            
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR Code", e);
            return startY + 20;
        }
    }
    
    /**
     * إنشاء محتوى QR Code
     */
    private String generateQRData(Invoice invoice) {
        StringBuilder qrData = new StringBuilder();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("ar"));
        
        qrData.append("=== فاتورة مبيعات ===\n");
        qrData.append("رقم الفاتورة: ").append(invoice.getId() != null ? invoice.getId() : "غير محدد").append("\n");
        
        if (invoice.getDate() != null) {
            qrData.append("التاريخ: ").append(dateFormat.format(invoice.getDate().toDate())).append("\n");
        }
        
        qrData.append("العميل: ").append(invoice.getCustomerName() != null ? invoice.getCustomerName() : "غير محدد").append("\n");
        qrData.append("الهاتف: ").append(invoice.getCustomerPhone() != null ? invoice.getCustomerPhone() : "غير محدد").append("\n");
        qrData.append("الدفع: ").append(invoice.isPaid() ? "نقدي" : "آجل").append("\n");
        qrData.append("المجموع: ").append(String.format(new Locale("ar"), "%.2f دج", invoice.getTotalAmount())).append("\n");
        
        if (invoice.getItems() != null) {
            qrData.append("\nالعناصر:\n");
            int itemNumber = 1;
            for (InvoiceItem item : invoice.getItems()) {
                qrData.append(itemNumber).append(". ").append(item.getProductName()).append("\n");
                qrData.append("   ").append(item.getQuantity()).append(" × ").append(item.getPrice()).append(" = ")
                      .append(item.getQuantity() * item.getPrice()).append(" دج\n");
                itemNumber++;
            }
        }
        
        return qrData.toString();
    }
    
    /**
     * حفظ Bitmap كملف BMP
     */
    private File saveBitmapAsBMP(Bitmap bitmap, String fileName) {
        try {
            // إنشاء مجلد الحفظ
            File documentsDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "POS_Invoices");
            
            if (!documentsDir.exists()) {
                documentsDir.mkdirs();
            }
            
            File file = new File(documentsDir, fileName);
            
            FileOutputStream fos = new FileOutputStream(file);
            
            // تحويل إلى تنسيق BMP
            writeBMPHeader(fos, bitmap.getWidth(), bitmap.getHeight());
            writeBMPData(fos, bitmap);
            
            fos.flush();
            fos.close();
            
            Log.i(TAG, "Bitmap saved as BMP: " + file.getAbsolutePath());
            return file;
            
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap as BMP", e);
            return null;
        }
    }
    
    /**
     * كتابة BMP Header
     */
    private void writeBMPHeader(FileOutputStream fos, int width, int height) throws IOException {
        int imageSize = width * height * 3; // 24 bits per pixel
        int fileSize = 54 + imageSize; // Header size + image size
        
        // BMP File Header (14 bytes)
        fos.write('B'); fos.write('M'); // Signature
        writeInt(fos, fileSize);        // File size
        writeInt(fos, 0);              // Reserved
        writeInt(fos, 54);             // Data offset
        
        // BMP Info Header (40 bytes)
        writeInt(fos, 40);             // Header size
        writeInt(fos, width);          // Width
        writeInt(fos, height);         // Height
        writeShort(fos, 1);            // Planes
        writeShort(fos, 24);           // Bits per pixel
        writeInt(fos, 0);              // Compression
        writeInt(fos, imageSize);      // Image size
        writeInt(fos, 2835);           // X pixels per meter
        writeInt(fos, 2835);           // Y pixels per meter
        writeInt(fos, 0);              // Colors used
        writeInt(fos, 0);              // Important colors
    }
    
    /**
     * كتابة بيانات الصورة بتنسيق BMP
     */
    private void writeBMPData(FileOutputStream fos, Bitmap bitmap) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // BMP يحفظ الصورة من الأسفل للأعلى
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                
                // استخراج قيم BGR (BMP يستخدم BGR بدلاً من RGB)
                int blue = (pixel) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                
                fos.write(blue);
                fos.write(green);
                fos.write(red);
            }
            
            // إضافة padding إذا لزم الأمر (كل سطر يجب أن يكون مضاعف 4 bytes)
            int padding = (4 - (width * 3) % 4) % 4;
            for (int p = 0; p < padding; p++) {
                fos.write(0);
            }
        }
    }
    
    /**
     * كتابة integer بتنسيق little-endian
     */
    private void writeInt(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 24) & 0xFF);
    }
    
    /**
     * كتابة short بتنسيق little-endian
     */
    private void writeShort(FileOutputStream fos, int value) throws IOException {
        fos.write(value & 0xFF);
        fos.write((value >> 8) & 0xFF);
    }
    
    /**
     * مشاركة ملف BMP
     */
    public void shareBMPFile(File bmpFile) {
        try {
            Uri fileUri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", bmpFile);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/bmp");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "فاتورة مبيعات");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharing BMP file", e);
            Toast.makeText(context, "فشل في مشاركة الملف: " + e.getMessage(), 
                         Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * واجهة callback للنتائج
     */
    public interface ConvertCallback {
        void onSuccess(File bmpFile, Bitmap bitmap);
        void onError(String error);
    }
} 