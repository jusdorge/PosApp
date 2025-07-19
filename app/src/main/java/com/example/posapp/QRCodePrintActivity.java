package com.example.posapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class QRCodePrintActivity extends AppCompatActivity {
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001;
    
    private ImageView qrImageView;
    private TextView customerNameTextView;
    private TextView customerPhoneTextView;
    private TextView customerDebtTextView;
    private Button printButton;
    private Button shareButton;
    private Button closeButton;
    
    private Bitmap qrBitmap;
    private String customerName;
    private String customerPhone;
    private double customerDebt;
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_print);
        
        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("طباعة QR Code");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // ربط عناصر الواجهة
        initViews();
        
        // الحصول على البيانات من Intent
        getDataFromIntent();
        
        // إعداد البلوتوث
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // عرض البيانات
        displayData();
        
        // إعداد الأزرار
        setupButtons();
    }
    
    private void initViews() {
        qrImageView = findViewById(R.id.qrImageView);
        customerNameTextView = findViewById(R.id.customerNameTextView);
        customerPhoneTextView = findViewById(R.id.customerPhoneTextView);
        customerDebtTextView = findViewById(R.id.customerDebtTextView);
        printButton = findViewById(R.id.printButton);
        shareButton = findViewById(R.id.shareButton);
        closeButton = findViewById(R.id.closeButton);
    }
    
    private void getDataFromIntent() {
        Intent intent = getIntent();
        
        // الحصول على QR Code bitmap
        byte[] byteArray = intent.getByteArrayExtra("qr_bitmap");
        if (byteArray != null) {
            qrBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        }
        
        // الحصول على معلومات العميل
        customerName = intent.getStringExtra("customer_name");
        customerPhone = intent.getStringExtra("customer_phone");
        customerDebt = intent.getDoubleExtra("customer_debt", 0.0);
    }
    
    private void displayData() {
        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap);
        }
        
        customerNameTextView.setText("الاسم: " + (customerName != null ? customerName : "غير محدد"));
        customerPhoneTextView.setText("الهاتف: " + (customerPhone != null ? customerPhone : "غير محدد"));
        customerDebtTextView.setText(String.format("إجمالي الدين: %.2f دج", customerDebt));
    }
    
    private void setupButtons() {
        printButton.setOnClickListener(v -> {
            try {
                showPrinterSelectionDialog();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح قائمة الطابعات: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        shareButton.setOnClickListener(v -> {
            try {
                shareQRCode();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في مشاركة QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        closeButton.setOnClickListener(v -> finish());
    }
    
    private void showPrinterSelectionDialog() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "البلوتوث غير مدعوم على هذا الجهاز", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // التحقق من الأذونات
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
        
        showPrinterSelection();
    }
    
    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestBluetoothPermissions() {
        new AlertDialog.Builder(this)
                .setTitle("صلاحيات البلوتوث")
                .setMessage("يحتاج التطبيق إلى صلاحيات البلوتوث للاتصال بالطابعة")
                .setPositiveButton("موافق", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        }, BLUETOOTH_PERMISSION_REQUEST_CODE);
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN
                        }, BLUETOOTH_PERMISSION_REQUEST_CODE);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
    
    private void showPrinterSelection() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "يرجى تفعيل البلوتوث أولاً", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, 1);
            return;
        }

        Set<BluetoothDevice> pairedDevices;
        try {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException e) {
            Toast.makeText(this, "ليس لديك صلاحية للوصول لأجهزة البلوتوث", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "لا توجد أجهزة بلوتوث مقترنة", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] deviceNames = new String[pairedDevices.size()];
        BluetoothDevice[] devices = new BluetoothDevice[pairedDevices.size()];
        
        int i = 0;
        for (BluetoothDevice device : pairedDevices) {
            try {
                deviceNames[i] = device.getName() + "\n" + device.getAddress();
                devices[i] = device;
                i++;
            } catch (SecurityException e) {
                Toast.makeText(this, "ليس لديك صلاحية للوصول لمعلومات الجهاز", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("اختر الطابعة")
                .setItems(deviceNames, (dialog, which) -> {
                    connectAndPrint(devices[which]);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
    
    private void connectAndPrint(BluetoothDevice device) {
        new Thread(() -> {
            try {
                runOnUiThread(() -> {
                    Toast.makeText(this, "جاري الاتصال بالطابعة...", Toast.LENGTH_SHORT).show();
                });
                
                bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
                bluetoothSocket.connect();
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "تم الاتصال، جاري الطباعة...", Toast.LENGTH_SHORT).show();
                });
                
                printQRCode();
                
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "فشل في الاتصال بالطابعة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                
                try {
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                    }
                } catch (IOException closeException) {
                    // تجاهل أخطاء الإغلاق
                }
            } catch (SecurityException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "ليس لديك صلاحية للاتصال بالطابعة", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void printQRCode() {
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            String encoding = "ISO-8859-1";
            
            // إعداد الطابعة
            outputStream.write(new byte[]{0x1B, 0x40}); // تهيئة الطابعة
            outputStream.write(new byte[]{0x1B, 0x61, 0x01}); // توسيط النص
            
            // طباعة رأس
            outputStream.write("================================\n".getBytes(encoding));
            outputStream.write("       CUSTOMER QR CODE        \n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            
            // معلومات العميل
            outputStream.write(("Name: " + (customerName != null ? customerName : "N/A") + "\n").getBytes(encoding));
            outputStream.write(("Phone: " + (customerPhone != null ? customerPhone : "N/A") + "\n").getBytes(encoding));
            outputStream.write((String.format("Debt: %.2f DZD\n", customerDebt)).getBytes(encoding));
            
            outputStream.write("--------------------------------\n".getBytes(encoding));
            outputStream.write("QR Code:\n".getBytes(encoding));
            
            // طباعة QR Code
            if (qrBitmap != null) {
                printQRCodeBitmap(outputStream, qrBitmap);
            }
            
            outputStream.write("\n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            outputStream.write("     SCAN TO VIEW INFO         \n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            
            // إضافة أسطر فارغة
            outputStream.write("\n\n".getBytes(encoding));
            
            // قطع الورق
            outputStream.write(new byte[]{0x1D, 0x56, 0x41, 0x10});
            
            outputStream.flush();
            Thread.sleep(1000);
            
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                bluetoothSocket.close();
            }
            
            runOnUiThread(() -> {
                Toast.makeText(this, "تم طباعة QR Code بنجاح", Toast.LENGTH_SHORT).show();
            });
            
        } catch (IOException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "فشل في طباعة QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } catch (InterruptedException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "تم مقاطعة عملية الطباعة", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "خطأ في الطباعة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void printQRCodeBitmap(OutputStream outputStream, Bitmap qrBitmap) throws IOException {
        // تصغير حجم QR Code للطباعة الحرارية
        int printWidth = 100;
        int printHeight = 100;
        
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(qrBitmap, printWidth, printHeight, false);
        
        // تحويل إلى بيانات ESC/POS
        for (int y = 0; y < printHeight; y += 8) {
            // أمر طباعة الصورة
            outputStream.write(new byte[]{0x1B, 0x2A, 0x00, (byte)(printWidth % 256), (byte)(printWidth / 256)});
            
            for (int x = 0; x < printWidth; x++) {
                byte pixelByte = 0;
                for (int bit = 0; bit < 8; bit++) {
                    if (y + bit < printHeight) {
                        int pixel = scaledBitmap.getPixel(x, y + bit);
                        if (pixel == Color.BLACK) {
                            pixelByte |= (1 << (7 - bit));
                        }
                    }
                }
                outputStream.write(pixelByte);
            }
            outputStream.write('\n');
        }
    }
    
    private void shareQRCode() {
        try {
            if (qrBitmap == null) {
                Toast.makeText(this, "QR Code غير متوفر", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // إنشاء مجلد الصور في الذاكرة المؤقتة
            java.io.File cachePath = new java.io.File(getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            
            // حفظ QR Code في الذاكرة المؤقتة
            String fileName = "customer_qr_" + System.currentTimeMillis() + ".png";
            java.io.File file = new java.io.File(cachePath, fileName);
            
            java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            
            // التحقق من وجود الملف
            if (!file.exists()) {
                Toast.makeText(this, "فشل في حفظ QR Code", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                // إنشاء URI للملف
                android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                        this, 
                        "com.example.posapp.fileprovider", 
                        file);
                
                // إنشاء Intent للمشاركة
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Code للعميل: " + customerName);
                shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "QR Code للعميل: " + (customerName != null ? customerName : "غير محدد") + "\n" +
                    "الهاتف: " + (customerPhone != null ? customerPhone : "غير محدد") + "\n" +
                    "إجمالي الدين: " + String.format("%.2f دج", customerDebt));
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                // التحقق من وجود تطبيقات للمشاركة
                if (shareIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(shareIntent, "مشاركة QR Code"));
                } else {
                    Toast.makeText(this, "لا توجد تطبيقات للمشاركة", Toast.LENGTH_SHORT).show();
                }
                
            } catch (Exception fileProviderException) {
                // طريقة بديلة: مشاركة النص فقط
                shareQRCodeAsText();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "فشل في مشاركة QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (allPermissionsGranted) {
                showPrinterSelection();
            } else {
                Toast.makeText(this, "يحتاج التطبيق لأذونات البلوتوث للطباعة", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void shareQRCodeAsText() {
        try {
            String qrData = "CUSTOMER_INFO\n" +
                    "Name: " + (customerName != null ? customerName : "N/A") + "\n" +
                    "Phone: " + (customerPhone != null ? customerPhone : "N/A") + "\n" +
                    "Total_Debt: " + String.format("%.2f DZD", customerDebt);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "معلومات العميل - QR Code");
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "معلومات العميل:\n" +
                "الاسم: " + (customerName != null ? customerName : "غير محدد") + "\n" +
                "الهاتف: " + (customerPhone != null ? customerPhone : "غير محدد") + "\n" +
                "إجمالي الدين: " + String.format("%.2f دج", customerDebt) + "\n\n" +
                "QR Code Data:\n" + qrData);
            
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(shareIntent, "مشاركة معلومات العميل"));
            } else {
                Toast.makeText(this, "لا توجد تطبيقات للمشاركة", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "فشل في مشاركة معلومات العميل", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 