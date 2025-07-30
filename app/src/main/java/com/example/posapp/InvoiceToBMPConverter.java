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
    
    // إعدادات الصورة - مقاسات أصغر للطابعات الحرارية
    private static final int IMAGE_WIDTH = 384;   // عرض مناسب للطابعات الحرارية (48mm * 8 dpi)
    private static final int IMAGE_HEIGHT = 800;  // ارتفاع أقل لتوفير الذاكرة
    private static final int PADDING = 20;         // حشو أقل لتوفير المساحة
    private static final int LINE_SPACING = 6;     // مسافات مضغوطة
    
    // إعدادات الخطوط - أحجام مناسبة للطابعات الصغيرة
    private static final int TITLE_TEXT_SIZE = 20;      // حجم خط العنوان
    private static final int HEADER_TEXT_SIZE = 16;     // حجم خط العناوين الفرعية
    private static final int NORMAL_TEXT_SIZE = 14;     // حجم الخط العادي
    private static final int SMALL_TEXT_SIZE = 12;      // حجم الخط الصغير
    
    private Context context;
    
    public InvoiceToBMPConverter(Context context) {
        this.context = context;
    }
    
    /**
     * تحويل فاتورة إلى صورة BMP (مع خيار المضغوطة)
     */
    public void convertInvoiceToBMP(Invoice invoice, ConvertCallback callback) {
        convertInvoiceToBMP(invoice, false, callback); // افتراضياً مع QR Code
    }
    
    /**
     * تحويل فاتورة إلى صورة BMP مع خيار ضغط
     */
    public void convertInvoiceToBMP(Invoice invoice, boolean compactMode, ConvertCallback callback) {
        if (invoice == null) {
            callback.onError("الفاتورة غير موجودة");
            return;
        }
        
        try {
            // إنشاء الـ bitmap
            Bitmap invoiceBitmap = createInvoiceBitmap(invoice, compactMode);
            
            // حفظ الصورة
            String fileName = "فاتورة_" + invoice.getId() + "_" + 
                            (compactMode ? "مضغوطة_" : "") +
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
     * إنشاء bitmap للفاتورة مع خيار الضغط
     */
    private Bitmap createInvoiceBitmap(Invoice invoice, boolean compactMode) {
        // حساب الارتفاع المطلوب
        int requiredHeight = calculateRequiredHeight(invoice, compactMode);
        
        // إنشاء bitmap
        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, requiredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // خلفية بيضاء
        canvas.drawColor(Color.WHITE);
        
        // رسم الفاتورة
        int currentY = drawInvoiceContent(canvas, invoice, compactMode);
        
        return bitmap;
    }
    
    /**
     * حساب الارتفاع المطلوب للفاتورة مع خيار الضغط
     */
    private int calculateRequiredHeight(Invoice invoice, boolean compactMode) {
        int height = PADDING * 2; // الحشو العلوي والسفلي
        
        // العنوان الرئيسي
        height += TITLE_TEXT_SIZE + LINE_SPACING * 2;
        
        // معلومات الفاتورة الأساسية (5 أسطر)
        height += (NORMAL_TEXT_SIZE + LINE_SPACING) * 6;
        
        // خط فاصل
        height += 25;
        
        // عنوان العناصر
        height += HEADER_TEXT_SIZE + LINE_SPACING;
        
        // العناصر (مضغوطة إذا كان في الوضع المضغوط)
        if (invoice.getItems() != null) {
            int maxItems = compactMode ? Math.min(invoice.getItems().size(), 8) : invoice.getItems().size();
            height += maxItems * (NORMAL_TEXT_SIZE + SMALL_TEXT_SIZE + LINE_SPACING * 2);
        }
        
        // خط فاصل
        height += 25;
        
        // المجموع
        height += (HEADER_TEXT_SIZE + LINE_SPACING) * 2;
        
        // QR Code (فقط في الوضع العادي)
        if (!compactMode) {
            height += 120; // حجم QR Code + مسافة
        }
        
        // النهاية
        height += NORMAL_TEXT_SIZE + LINE_SPACING * 2;
        
        return Math.max(height, compactMode ? 600 : IMAGE_HEIGHT);
    }
    
    /**
     * رسم محتوى الفاتورة على Canvas مع دعم الوضع المضغوط
     */
    private int drawInvoiceContent(Canvas canvas, Invoice invoice, boolean compactMode) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setSubpixelText(true); // تحسين جودة النص
        paint.setFilterBitmap(true); // تحسين جودة الرسم
        paint.setDither(true); // تحسين الألوان
        
        // إعداد الخط العربي
        try {
            Typeface arabicTypeface = Typeface.create("serif", Typeface.NORMAL);
            paint.setTypeface(arabicTypeface);
        } catch (Exception e) {
            // استخدام الخط الافتراضي إذا فشل تحميل الخط العربي
            paint.setTypeface(Typeface.DEFAULT);
        }
        
        paint.setTextAlign(Paint.Align.RIGHT); // النص العربي من اليمين
        paint.setColor(Color.BLACK);
        
        int currentY = PADDING;
        int rightMargin = IMAGE_WIDTH - PADDING;
        int leftMargin = PADDING;
        
        // العنوان الرئيسي
        paint.setTextSize(TITLE_TEXT_SIZE);
        paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        currentY += TITLE_TEXT_SIZE;
        
        // رسم العنوان مع تحسين للعربية
        String title = compactMode ? "فاتورة مبيعات - مضغوطة" : "فاتورة مبيعات";
        drawArabicText(canvas, title, IMAGE_WIDTH / 2, currentY, paint);
        currentY += LINE_SPACING * 2;
        
        // رسم خط فاصل
        paint.setStrokeWidth(3);
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint);
        currentY += 25;
        
        // العودة للنص العادي
        paint.setTextSize(NORMAL_TEXT_SIZE);
        paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.RIGHT);
        
        // معلومات الفاتورة
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("ar", "SA"));
        
        currentY += NORMAL_TEXT_SIZE;
        String invoiceId = invoice.getId() != null ? invoice.getId() : "غير محدد";
        drawArabicText(canvas, "رقم الفاتورة: " + invoiceId, rightMargin, currentY, paint);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        if (invoice.getDate() != null) {
            String dateStr = dateFormat.format(invoice.getDate().toDate());
            drawArabicText(canvas, "التاريخ: " + dateStr, rightMargin, currentY, paint);
        }
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        String customerName = invoice.getCustomerName() != null ? invoice.getCustomerName() : "غير محدد";
        drawArabicText(canvas, "اسم العميل: " + customerName, rightMargin, currentY, paint);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        String customerPhone = invoice.getCustomerPhone() != null ? invoice.getCustomerPhone() : "غير محدد";
        drawArabicText(canvas, "هاتف العميل: " + customerPhone, rightMargin, currentY, paint);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        String paymentMethod = invoice.isPaid() ? "نقدي" : "آجل";
        drawArabicText(canvas, "طريقة الدفع: " + paymentMethod, rightMargin, currentY, paint);
        
        currentY += LINE_SPACING * 2;
        
        // رسم خط فاصل
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint);
        currentY += 25;
        
        // عنوان العناصر
        paint.setTextSize(HEADER_TEXT_SIZE);
        paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.BOLD));
        currentY += HEADER_TEXT_SIZE;
        String itemsTitle = compactMode ? "العناصر (أهم 8)" : "عناصر الفاتورة";
        drawArabicText(canvas, itemsTitle, rightMargin, currentY, paint);
        currentY += LINE_SPACING;
        
        // رسم العناصر
        paint.setTextSize(NORMAL_TEXT_SIZE);
        paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.NORMAL));
        
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            int itemNumber = 1;
            int maxItemsToDraw = compactMode ? Math.min(invoice.getItems().size(), 8) : invoice.getItems().size();
            for (int i = 0; i < maxItemsToDraw; i++) {
                InvoiceItem item = invoice.getItems().get(i);
                currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
                
                // اسم المنتج مع الرقم التسلسلي
                String productName = item.getProductName() != null ? item.getProductName() : "منتج غير محدد";
                String productLine = itemNumber + ". " + productName;
                drawArabicText(canvas, productLine, rightMargin, currentY, paint);
                
                // تفاصيل السعر والكمية
                currentY += SMALL_TEXT_SIZE + LINE_SPACING;
                paint.setTextSize(SMALL_TEXT_SIZE);
                paint.setColor(Color.GRAY);
                
                String details = String.format(new Locale("ar", "SA"), 
                    "الكمية: %d × %.2f = %.2f دج", 
                    item.getQuantity(), item.getPrice(), item.getQuantity() * item.getPrice());
                drawArabicText(canvas, details, rightMargin, currentY, paint);
                
                // العودة للإعدادات العادية
                paint.setTextSize(NORMAL_TEXT_SIZE);
                paint.setColor(Color.BLACK);
                
                currentY += LINE_SPACING;
                itemNumber++;
            }
            
            // إضافة رسالة إذا كان هناك المزيد من العناصر في الوضع المضغوط
            if (compactMode && invoice.getItems().size() > 8) {
                currentY += SMALL_TEXT_SIZE + LINE_SPACING;
                paint.setTextSize(SMALL_TEXT_SIZE);
                paint.setColor(Color.GRAY);
                drawArabicText(canvas, "... و " + (invoice.getItems().size() - 8) + " عناصر أخرى", 
                              rightMargin, currentY, paint);
                paint.setTextSize(NORMAL_TEXT_SIZE);
                paint.setColor(Color.BLACK);
            }
        } else {
            currentY += NORMAL_TEXT_SIZE;
            paint.setColor(Color.GRAY);
            drawArabicText(canvas, "لا توجد عناصر في هذه الفاتورة", rightMargin, currentY, paint);
            paint.setColor(Color.BLACK);
        }
        
        currentY += LINE_SPACING * 2;
        
        // رسم خط فاصل
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint);
        currentY += 25;
        
        // المجموع الكلي
        paint.setTextSize(HEADER_TEXT_SIZE);
        paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.BOLD));
        paint.setColor(Color.parseColor("#2196F3")); // اللون الأزرق
        
        currentY += HEADER_TEXT_SIZE;
        String totalText = String.format(new Locale("ar", "SA"), "المجموع الكلي: %.2f دج", invoice.getTotalAmount());
        drawArabicText(canvas, totalText, rightMargin, currentY, paint);
        
        currentY += LINE_SPACING * 2;
        
        // إضافة QR Code (فقط في الوضع العادي)
        if (!compactMode) {
            currentY = drawQRCode(canvas, invoice, currentY);
        }
        
        // رسالة الشكر
        paint.setTextSize(NORMAL_TEXT_SIZE);
        paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.NORMAL));
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);
        
        currentY += NORMAL_TEXT_SIZE + LINE_SPACING;
        String thankYouMessage = compactMode ? "شكراً لكم" : "شكراً لتعاملكم معنا";
        drawArabicText(canvas, thankYouMessage, IMAGE_WIDTH / 2, currentY, paint);
        
        return currentY;
    }
    
    /**
     * رسم QR Code على الفاتورة (حجم أصغر)
     */
    private int drawQRCode(Canvas canvas, Invoice invoice, int startY) {
        try {
            // إنشاء محتوى QR Code مبسط
            String qrData = generateSimpleQRData(invoice);
            
            // إنشاء QR Code بحجم أصغر
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 0); // بدون هوامش
            hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L); // أقل تصحيح للأخطاء
            
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 80, 80, hints); // حجم أصغر 80x80
            
            // تحويل إلى Bitmap
            Bitmap qrBitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.RGB_565);
            for (int x = 0; x < 80; x++) {
                for (int y = 0; y < 80; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            // رسم QR Code في المنتصف
            int qrX = (IMAGE_WIDTH - 80) / 2;
            int qrY = startY + 15;
            canvas.drawBitmap(qrBitmap, qrX, qrY, null);
            
            // إضافة نص تحت QR Code
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextSize(SMALL_TEXT_SIZE);
            paint.setColor(Color.GRAY);
            paint.setTextAlign(Paint.Align.CENTER);
            
            int textY = qrY + 80 + SMALL_TEXT_SIZE + 8;
            drawArabicText(canvas, "QR للتفاصيل", IMAGE_WIDTH / 2, textY, paint);
            
            return textY + LINE_SPACING;
            
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR Code", e);
            return startY + 20;
        }
    }
    
    /**
     * إنشاء محتوى QR Code مبسط لتوفير المساحة
     */
    private String generateSimpleQRData(Invoice invoice) {
        StringBuilder qrData = new StringBuilder();
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("ar", "SA"));
        
        qrData.append("فاتورة: ").append(invoice.getId() != null ? invoice.getId() : "N/A").append("\n");
        
        if (invoice.getDate() != null) {
            qrData.append("تاريخ: ").append(dateFormat.format(invoice.getDate().toDate())).append("\n");
        }
        
        qrData.append("عميل: ").append(invoice.getCustomerName() != null ? invoice.getCustomerName() : "N/A").append("\n");
        qrData.append("مجموع: ").append(String.format(new Locale("ar", "SA"), "%.2f دج", invoice.getTotalAmount())).append("\n");
        qrData.append("حالة: ").append(invoice.isPaid() ? "مدفوع" : "آجل");
        
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
     * رسم النص العربي مع تحسينات خاصة
     */
    private void drawArabicText(Canvas canvas, String text, float x, float y, Paint paint) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        try {
            // حفظ إعدادات الرسم الحالية
            Paint.Align originalAlign = paint.getTextAlign();
            
            // تحسين إعدادات الرسم للنصوص العربية
            paint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
            paint.setHinting(Paint.HINTING_ON);
            
            // تحسين جودة النص العربي
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            
            // التحقق من اتجاه النص
            if (containsArabic(text)) {
                // للنصوص العربية، تأكد من الاتجاه الصحيح
                paint.setTextScaleX(1.0f); // تأكد من عدم تشويه النص
                
                // رسم النص مع تحسين المسافات
                canvas.drawText(text, x, y, paint);
            } else {
                // للنصوص الإنجليزية أو الأرقام
                canvas.drawText(text, x, y, paint);
            }
            
            // استعادة الإعدادات الأصلية
            paint.setTextAlign(originalAlign);
            
        } catch (Exception e) {
            Log.e(TAG, "Error drawing Arabic text: " + text, e);
            // رسم النص بطريقة بديلة
            try {
                canvas.drawText(text, x, y, paint);
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback drawing also failed", fallbackError);
            }
        }
    }
    
    /**
     * تحقق مما إذا كان النص يحتوي على أحرف عربية
     */
    private boolean containsArabic(String text) {
        if (text == null) return false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // نطاق الأحرف العربية في Unicode
            if ((c >= 0x0600 && c <= 0x06FF) || // Arabic
                (c >= 0x0750 && c <= 0x077F) || // Arabic Supplement  
                (c >= 0x08A0 && c <= 0x08FF) || // Arabic Extended-A
                (c >= 0xFB50 && c <= 0xFDFF) || // Arabic Presentation Forms-A
                (c >= 0xFE70 && c <= 0xFEFF)) {  // Arabic Presentation Forms-B
                return true;
            }
        }
        return false;
    }
    
    /**
     * واجهة callback للنتائج
     */
    public interface ConvertCallback {
        void onSuccess(File bmpFile, Bitmap bitmap);
        void onError(String error);
    }
} 