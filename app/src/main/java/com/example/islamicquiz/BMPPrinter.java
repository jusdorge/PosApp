package com.example.islamicquiz;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.Manifest;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * فئة طباعة صور BMP على الطابعة الحرارية
 */
public class BMPPrinter {
    private static final String TAG = "BMPPrinter";
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    
    public interface PrintCallback {
        void onPrintStart();
        void onPrintProgress(String message);
        void onPrintSuccess();
        void onPrintError(String error);
    }
    
    public BMPPrinter(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    /**
     * عرض قائمة الطابعات المتاحة وطباعة الصورة
     */
    public void printBitmap(Bitmap bitmap, PrintCallback callback) {
        if (bitmap == null) {
            callback.onPrintError("الصورة غير متوفرة");
            return;
        }
        
        if (bluetoothAdapter == null) {
            callback.onPrintError("البلوتوث غير مدعوم على هذا الجهاز");
            return;
        }
        
        // التحقق من الأذونات
        if (!checkBluetoothPermissions()) {
            callback.onPrintError("لا توجد أذونات البلوتوث المطلوبة");
            return;
        }
        
        // عرض قائمة الطابعات
        showPrinterSelectionDialog(bitmap, callback);
    }
    
    /**
     * التحقق من أذونات البلوتوث
     */
    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                   == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) 
                   == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * عرض قائمة الطابعات المتاحة
     */
    private void showPrinterSelectionDialog(Bitmap bitmap, PrintCallback callback) {
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            
            if (pairedDevices.size() == 0) {
                callback.onPrintError("لا توجد طابعات مقترنة");
                return;
            }
            
            BluetoothDevice[] devices = new BluetoothDevice[pairedDevices.size()];
            String[] deviceNames = new String[pairedDevices.size()];
            
            int i = 0;
            for (BluetoothDevice device : pairedDevices) {
                try {
                    deviceNames[i] = device.getName() + "\n" + device.getAddress();
                    devices[i] = device;
                    i++;
                } catch (SecurityException e) {
                    callback.onPrintError("ليس لديك صلاحية للوصول لمعلومات الجهاز");
                    return;
                }
            }
            
            new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("اختر الطابعة")
                .setItems(deviceNames, (dialog, which) -> {
                    connectAndPrintBitmap(devices[which], bitmap, callback);
                })
                .setNegativeButton("إلغاء", null)
                .show();
                
        } catch (SecurityException e) {
            callback.onPrintError("ليس لديك صلاحية للوصول لأجهزة البلوتوث");
        }
    }
    
    /**
     * الاتصال بالطابعة وطباعة الصورة
     */
    private void connectAndPrintBitmap(BluetoothDevice device, Bitmap bitmap, PrintCallback callback) {
        if (!checkBluetoothPermissions()) {
            callback.onPrintError("لا توجد أذونات للاتصال بالطابعة");
            return;
        }
        
        // تشغيل الطباعة في thread منفصل
        new Thread(() -> {
            try {
                callback.onPrintStart();
                callback.onPrintProgress("جاري الاتصال بالطابعة...");
                
                // إنشاء الاتصال
                bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
                bluetoothSocket.connect();
                
                callback.onPrintProgress("تم الاتصال، جاري تحضير الصورة...");
                Thread.sleep(500); // وقت للتحضير
                
                callback.onPrintProgress("بدء طباعة الصورة العربية... يرجى الانتظار");
                
                // طباعة الصورة مع callback للتقدم
                printBitmapToThermalPrinter(bitmap, callback);
                
                callback.onPrintSuccess();
                
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to printer", e);
                callback.onPrintError("فشل في الاتصال بالطابعة: " + e.getMessage());
                
                // محاولة إغلاق الاتصال في حالة الفشل
                try {
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                    }
                } catch (IOException closeException) {
                    // تجاهل أخطاء الإغلاق
                }
            } catch (SecurityException e) {
                callback.onPrintError("ليس لديك صلاحية للاتصال بالطابعة");
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during printing", e);
                callback.onPrintError("خطأ غير متوقع: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * طباعة الصورة على الطابعة الحرارية مع تقرير التقدم
     */
    private void printBitmapToThermalPrinter(Bitmap bitmap, PrintCallback callback) throws IOException {
        OutputStream outputStream = bluetoothSocket.getOutputStream();
        
        try {
            callback.onPrintProgress("إعداد الطابعة...");
            
            // إعداد الطابعة
            outputStream.write(new byte[]{0x1B, 0x40}); // تهيئة الطابعة
            outputStream.write(new byte[]{0x1B, 0x61, 0x01}); // توسيط المحتوى
            outputStream.write(new byte[]{0x1B, 0x21, 0x00}); // إعداد الخط العادي
            
            // عنوان الطباعة مع تحسين التنسيق
            String encoding = "ISO-8859-1";
            outputStream.write("================================\n".getBytes(encoding));
            outputStream.write("     HIGH QUALITY INVOICE      \n".getBytes(encoding));
            outputStream.write("       ARABIC TEXT SUPPORT     \n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            outputStream.write("\n".getBytes(encoding));
            
            outputStream.flush();
            Thread.sleep(500);
            
            callback.onPrintProgress("جاري طباعة الفاتورة... 0%");
            
            // طباعة الصورة مع تقرير التقدم
            printBitmapDataWithProgress(outputStream, bitmap, callback);
            
            callback.onPrintProgress("إنهاء الطباعة...");
            
            // خاتمة الطباعة
            outputStream.write("\n\n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            outputStream.write("   PRINTED WITH ARABIC FONTS   \n".getBytes(encoding));
            outputStream.write("     THERMAL PRINTER OUTPUT    \n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            
            // إضافة أسطر فارغة
            outputStream.write("\n\n\n".getBytes(encoding));
            
            // قطع الورق
            outputStream.write(new byte[]{0x1D, 0x56, 0x41, 0x10});
            
            // التأكد من إرسال جميع البيانات
            outputStream.flush();
            
            callback.onPrintProgress("طباعة مكتملة! جاري قطع الورق...");
            
            // إضافة تأخير قبل إغلاق الاتصال
            Thread.sleep(5000); // تأخير أطول للطابعة لإنهاء العمل
            
        } catch (InterruptedException e) {
            Log.w(TAG, "Sleep interrupted during printing", e);
            Thread.currentThread().interrupt(); // استعادة حالة المقاطعة
        }
        
        // إغلاق الاتصال
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            bluetoothSocket.close();
        }
    }
    
    /**
     * طباعة بيانات الصورة مع تقرير التقدم
     */
    private void printBitmapDataWithProgress(OutputStream outputStream, Bitmap bitmap, PrintCallback callback) throws IOException {
        // تحديد أبعاد الطباعة للطابعة الحرارية (حجم محدود لضمان التوافق)
        int maxWidth = 320; // عرض أقل لضمان عمل جميع الطابعات (40mm * 8 dots/mm)
        
        // تغيير حجم الصورة إذا لزم الأمر
        Bitmap scaledBitmap = scaleBitmapForPrinting(bitmap, maxWidth);
        
        int width = scaledBitmap.getWidth();
        int height = scaledBitmap.getHeight();
        
        Log.i(TAG, "Printing bitmap: " + width + "x" + height);
        
        // تحويل الصورة إلى أبيض وأسود مع تحسين الوضوح
        Bitmap bwBitmap = convertToBlackAndWhite(scaledBitmap);
        
        // طباعة الصورة سطراً بسطر مع تحكم بطيء للاستقرار
        int linesPerChunk = 4; // chunks أصغر لتجنب buffer overflow
        
        for (int y = 0; y < height; y += linesPerChunk) {
            int actualLines = Math.min(linesPerChunk, height - y);
            
            // إرسال أمر لكل سطر منفصل (أبطأ لكن أكثر استقراراً)
            for (int line = 0; line < actualLines; line++) {
                int currentY = y + line;
                if (currentY >= height) break;
                
                // أمر طباعة سطر واحد
                outputStream.write(new byte[]{0x1B, 0x2A, 0x00}); // ESC * 0
                
                // عرض السطر (low byte, high byte)
                outputStream.write(new byte[]{(byte)(width % 256), (byte)(width / 256)});
                
                // بيانات السطر
                for (int x = 0; x < width; x++) {
                    byte pixelByte = 0;
                    
                    // كل byte يحتوي على 8 pixels عموديين
                    for (int bit = 0; bit < 8; bit++) {
                        int pixelY = currentY + bit;
                        if (pixelY < height) {
                            int pixel = bwBitmap.getPixel(x, pixelY);
                            if (pixel == Color.BLACK) {
                                pixelByte |= (1 << (7 - bit));
                            }
                        }
                    }
                    
                    outputStream.write(pixelByte);
                }
                
                outputStream.write('\n'); // نهاية السطر
                outputStream.flush(); // إرسال فوري
                
                // تأخير بين كل سطر
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep interrupted", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            // تأخير أطول بين كل chunk
            try {
                Thread.sleep(200); // تأخير 200ms بين chunks
            } catch (InterruptedException e) {
                Log.w(TAG, "Sleep interrupted during bitmap data printing", e);
                Thread.currentThread().interrupt();
                break;
            }
            
            // إرسال البيانات والتأكد من الاستلام
            outputStream.flush();
            
            // تقرير التقدم
            int progress = (y * 100) / height;
            callback.onPrintProgress("جاري الطباعة... " + progress + "%");
            Log.i(TAG, "Printing progress: " + progress + "%");
        }
        
        Log.i(TAG, "Bitmap printing completed successfully");
    }
    
    /**
     * تغيير حجم الصورة للطباعة
     */
    private Bitmap scaleBitmapForPrinting(Bitmap bitmap, int maxWidth) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        if (width <= maxWidth) {
            return bitmap; // لا حاجة لتغيير الحجم
        }
        
        // حساب النسبة للحفاظ على الأبعاد
        float ratio = (float) maxWidth / width;
        int newHeight = Math.round(height * ratio);
        
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true);
    }
    
    /**
     * تحويل الصورة إلى أبيض وأسود مع تحسين الوضوح
     */
    private Bitmap convertToBlackAndWhite(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        Bitmap bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = bitmap.getPixel(x, y);
                
                // استخراج قيم RGB
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                
                // حساب اللون الرمادي باستخدام weighted average
                int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                
                // تحسين threshold للنصوص العربية
                int threshold = 160; // رفع threshold لوضوح أفضل للنصوص الدقيقة
                int bwColor = gray < threshold ? Color.BLACK : Color.WHITE;
                
                bwBitmap.setPixel(x, y, bwColor);
            }
        }
        
        return bwBitmap;
    }
    
    /**
     * إلغاء أي اتصال نشط بالطابعة
     */
    public void disconnect() {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing bluetooth socket", e);
        }
    }
} 