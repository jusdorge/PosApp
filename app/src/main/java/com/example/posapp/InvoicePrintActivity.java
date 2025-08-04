package com.example.posapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Invoice;
import com.example.posapp.model.InvoiceItem;
import com.example.posapp.model.OperationLog;
import com.example.posapp.model.PaymentMethod;
import com.example.posapp.service.OperationLogService;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InvoicePrintActivity extends AppCompatActivity implements EditPaymentMethodDialog.OnPaymentMethodChangedListener {
    private static final String TAG = "InvoicePrintActivity";
    private static final String ARG_INVOICE_ID = "invoice_id";
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1001;
    
    private TextView invoiceNumberTextView;
    private TextView invoiceDateTextView;
    private TextView customerNameTextView;
    private TextView customerPhoneTextView;
    private TextView paymentMethodTextView;
    private RecyclerView itemsRecyclerView;
    private TextView totalAmountTextView;
    private Button printButton;
    private Button printImageButton;
    private Button editButton;
    private Button shareButton;
    private Button saveAsBMPButton;
    private Button closeButton;
    private Button editPaymentMethodButton;  // زر تعديل طريقة الدفع الجديد
    
    private FirebaseFirestore db;
    private String invoiceId;
    private Invoice currentInvoice;
    private InvoicePrintItemAdapter itemsAdapter;
    private List<InvoiceItem> invoiceItems = new ArrayList<>();
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_print);

        // الحصول على معرف الفاتورة
        invoiceId = getIntent().getStringExtra(ARG_INVOICE_ID);
        if (invoiceId == null) {
            Toast.makeText(this, "خطأ: معرف الفاتورة غير موجود", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("طباعة الفاتورة");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // ربط عناصر الواجهة
        initViews();
        
        // إعداد قاعدة البيانات
        db = FirebaseFirestore.getInstance();
        
        // إعداد البلوتوث
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // تحميل بيانات الفاتورة
        loadInvoiceData();
        
        // إعداد الأزرار
        setupButtons();
    }

    private void initViews() {
        invoiceNumberTextView = findViewById(R.id.invoiceNumberTextView);
        invoiceDateTextView = findViewById(R.id.invoiceDateTextView);
        customerNameTextView = findViewById(R.id.customerNameTextView);
        customerPhoneTextView = findViewById(R.id.customerPhoneTextView);
        paymentMethodTextView = findViewById(R.id.paymentMethodTextView);
        itemsRecyclerView = findViewById(R.id.itemsRecyclerView);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);
        printButton = findViewById(R.id.printButton);
        printImageButton = findViewById(R.id.printImageButton);
        editButton = findViewById(R.id.editButton);
        shareButton = findViewById(R.id.shareButton);
        saveAsBMPButton = findViewById(R.id.saveAsBMPButton);
        closeButton = findViewById(R.id.closeButton);
        editPaymentMethodButton = findViewById(R.id.editPaymentMethodButton); // ربط الزر
        
        // إعداد RecyclerView
        itemsAdapter = new InvoicePrintItemAdapter(invoiceItems);
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemsRecyclerView.setAdapter(itemsAdapter);
    }

    private void setupButtons() {
        printButton.setOnClickListener(v -> {
            try {
                showPrintingOptions();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح خيارات الطباعة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        printImageButton.setOnClickListener(v -> {
            try {
                showImagePrintingInfo();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في طباعة الصورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        editButton.setOnClickListener(v -> {
            try {
                showEditInvoiceDialog();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح تعديل الفاتورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // زر تعديل طريقة الدفع
        editPaymentMethodButton.setOnClickListener(v -> {
            try {
                showEditPaymentMethodDialog();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح تعديل طريقة الدفع: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        saveAsBMPButton.setOnClickListener(v -> {
            try {
                convertInvoiceToBMP();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في تحويل الفاتورة لصورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        shareButton.setOnClickListener(v -> {
            try {
                shareInvoice();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في مشاركة الفاتورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        closeButton.setOnClickListener(v -> finish());
    }
    
    /**
     * عرض خيارات الطباعة مع الشرح
     */
    private void showPrintingOptions() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("خيارات الطباعة")
            .setMessage("اختر نوع الطباعة المناسب:\n\n" +
                       "📄 الطباعة النصية:\n" +
                       "• سريعة ومتوافقة مع جميع الطابعات\n" +
                       "• تحويل النصوص العربية لأحرف لاتينية\n" +
                       "• مناسبة للاستخدام اليومي\n\n" +
                       "🖼️ طباعة الصورة:\n" +
                       "• الحفاظ على النصوص العربية الأصلية\n" +
                       "• جودة عالية ووضوح تام\n" +
                       "• قد تستغرق وقتاً أطول")
            .setPositiveButton("طباعة نصية", (dialog, which) -> {
                showPrinterSelectionDialog();
            })
            .setNeutralButton("طباعة الصورة", (dialog, which) -> {
                printInvoiceAsImage();
            })
            .setNegativeButton("إلغاء", null)
            .setIcon(android.R.drawable.ic_menu_info_details)
            .show();
    }
    
    /**
     * عرض معلومات طباعة الصورة مع خيار التأكيد
     */
    private void showImagePrintingInfo() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("طباعة الصورة العربية")
            .setMessage("اختر نوع الطباعة:\n\n" +
                       "📄 **طباعة عادية:**\n" +
                       "• نصوص عربية كاملة\n" +
                       "• QR Code مع تفاصيل شاملة\n" +
                       "• جودة عالية (وقت أطول)\n\n" +
                       "⚡ **طباعة مضغوطة (موصى بها):**\n" +
                       "• نصوص عربية محسنة\n" +
                       "• حجم أصغر وسرعة أكبر\n" +
                       "• مناسبة للطابعات الحرارية\n" +
                       "• أقل استهلاكاً للذاكرة\n\n" +
                       "⏱️ الطباعة المضغوطة أسرع وأكثر استقراراً")
            .setPositiveButton("طباعة مضغوطة ⚡", (dialog, which) -> {
                printInvoiceAsImageCompact();
            })
            .setNeutralButton("طباعة عادية 📄", (dialog, which) -> {
                printInvoiceAsImage();
            })
            .setNegativeButton("إلغاء", null)
            .setIcon(android.R.drawable.ic_menu_camera)
            .show();
    }
    
    /**
     * طباعة الفاتورة كصورة مع النصوص العربية الكاملة
     */
    private void printInvoiceAsImage() {
        if (currentInvoice == null) {
            Toast.makeText(this, "لا يمكن طباعة الفاتورة، البيانات غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // عرض progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("جاري إنشاء الصورة للطباعة...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // إنشاء الصورة في background thread
        new Thread(() -> {
            try {
                InvoiceToBMPConverter converter = new InvoiceToBMPConverter(this);
                
                converter.convertInvoiceToBMP(currentInvoice, new InvoiceToBMPConverter.ConvertCallback() {
                    @Override
                    public void onSuccess(java.io.File bmpFile, android.graphics.Bitmap bitmap) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            // الآن طباعة الصورة
                            printBitmapImage(bitmap);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(InvoicePrintActivity.this, 
                                         "فشل في إنشاء صورة الفاتورة: " + error, 
                                         Toast.LENGTH_LONG).show();
                        });
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(InvoicePrintActivity.this, 
                                 "خطأ في إنشاء الصورة: " + e.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * طباعة الفاتورة كصورة مضغوطة (أسرع وأكثر استقراراً)
     */
    private void printInvoiceAsImageCompact() {
        if (currentInvoice == null) {
            Toast.makeText(this, "لا يمكن طباعة الفاتورة، البيانات غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // عرض progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("جاري إنشاء صورة مضغوطة للطباعة...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // إنشاء الصورة في background thread
        new Thread(() -> {
            try {
                InvoiceToBMPConverter converter = new InvoiceToBMPConverter(this);
                
                // إنشاء صورة مضغوطة
                converter.convertInvoiceToBMP(currentInvoice, true, new InvoiceToBMPConverter.ConvertCallback() {
                    @Override
                    public void onSuccess(java.io.File bmpFile, android.graphics.Bitmap bitmap) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            // الآن طباعة الصورة
                            printBitmapImage(bitmap);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(InvoicePrintActivity.this, 
                                         "فشل في إنشاء الصورة المضغوطة: " + error, 
                                         Toast.LENGTH_LONG).show();
                        });
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(InvoicePrintActivity.this, 
                                 "خطأ في إنشاء الصورة: " + e.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * طباعة صورة البيتماب على الطابعة
     */
    private void printBitmapImage(android.graphics.Bitmap bitmap) {
        BMPPrinter bmpPrinter = new BMPPrinter(this);
        
        bmpPrinter.printBitmap(bitmap, new BMPPrinter.PrintCallback() {
            @Override
            public void onPrintStart() {
                runOnUiThread(() -> {
                    Toast.makeText(InvoicePrintActivity.this, "بدء الطباعة...", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onPrintProgress(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(InvoicePrintActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onPrintSuccess() {
                runOnUiThread(() -> {
                    new android.app.AlertDialog.Builder(InvoicePrintActivity.this)
                        .setTitle("تمت الطباعة بنجاح!")
                        .setMessage("تم طباعة فاتورة الصورة بالنصوص العربية الكاملة على الطابعة")
                        .setPositiveButton("حسناً", null)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
                });
            }
            
            @Override
            public void onPrintError(String error) {
                runOnUiThread(() -> {
                    new android.app.AlertDialog.Builder(InvoicePrintActivity.this)
                        .setTitle("فشل في الطباعة")
                        .setMessage("حدث خطأ أثناء طباعة الصورة:\n\n" + error)
                        .setPositiveButton("حسناً", null)
                        .setNegativeButton("إعادة المحاولة", (dialog, which) -> printBitmapImage(bitmap))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                });
            }
        });
    }
    
    /**
     * تحويل الفاتورة إلى صورة BMP مع الحفاظ على النصوص العربية
     */
    private void convertInvoiceToBMP() {
        if (currentInvoice == null) {
            Toast.makeText(this, "لا يمكن تحويل الفاتورة، البيانات غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // عرض progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("جاري تحويل الفاتورة إلى صورة...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // تشغيل التحويل في background thread
        new Thread(() -> {
            try {
                InvoiceToBMPConverter converter = new InvoiceToBMPConverter(this);
                
                converter.convertInvoiceToBMP(currentInvoice, new InvoiceToBMPConverter.ConvertCallback() {
                    @Override
                    public void onSuccess(java.io.File bmpFile, android.graphics.Bitmap bitmap) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            showBMPSaveSuccess(bmpFile, bitmap);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(InvoicePrintActivity.this, 
                                         "فشل في تحويل الفاتورة: " + error, 
                                         Toast.LENGTH_LONG).show();
                        });
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(InvoicePrintActivity.this, 
                                 "خطأ في التحويل: " + e.getMessage(), 
                                 Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    /**
     * عرض نجاح حفظ الصورة مع خيارات المشاركة والعرض
     */
    private void showBMPSaveSuccess(java.io.File bmpFile, android.graphics.Bitmap bitmap) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("تم الحفظ بنجاح!")
                .setMessage("تم حفظ الفاتورة كصورة BMP بالنصوص العربية الكاملة\n\n" +
                           "المسار: " + bmpFile.getAbsolutePath() + "\n\n" +
                           "ماذا تريد أن تفعل؟")
                .setPositiveButton("مشاركة الصورة", (dialog, which) -> {
                    InvoiceToBMPConverter converter = new InvoiceToBMPConverter(this);
                    converter.shareBMPFile(bmpFile);
                })
                .setNeutralButton("عرض الملف", (dialog, which) -> {
                    try {
                        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                        android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                            this, getPackageName() + ".fileprovider", bmpFile);
                        viewIntent.setDataAndType(fileUri, "image/bmp");
                        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(viewIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "لا يوجد تطبيق لعرض الصورة: " + e.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("حسناً", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void loadInvoiceData() {
        db.collection("invoices").document(invoiceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentInvoice = documentSnapshot.toObject(Invoice.class);
                        if (currentInvoice != null) {
                            currentInvoice.setId(documentSnapshot.getId());
                            displayInvoiceData();
                        }
                    } else {
                        Toast.makeText(this, "الفاتورة غير موجودة", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل الفاتورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayInvoiceData() {
        // عرض معلومات الفاتورة
        invoiceNumberTextView.setText("رقم الفاتورة: " + currentInvoice.getDisplayNumber());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        invoiceDateTextView.setText("التاريخ: " + dateFormat.format(currentInvoice.getDate().toDate()));
        
        customerNameTextView.setText("العميل: " + (currentInvoice.getCustomerName() != null ? currentInvoice.getCustomerName() : "غير محدد"));
        customerPhoneTextView.setText("الهاتف: " + (currentInvoice.getCustomerPhone() != null ? currentInvoice.getCustomerPhone() : "غير محدد"));
        
        // عرض طريقة الدفع باستخدام النظام الجديد
        updatePaymentMethodDisplay();
        
        totalAmountTextView.setText("المجموع: " + CurrencyUtils.formatCurrency(currentInvoice.getTotalAmount()));
        
        // عرض عناصر الفاتورة
        if (currentInvoice.getItems() != null) {
            invoiceItems.clear();
            invoiceItems.addAll(currentInvoice.getItems());
            if (itemsAdapter != null) {
                itemsAdapter.updateData(invoiceItems);
            }
        }
        
        // تسجيل عملية عرض/طباعة الفاتورة
        OperationLogService operationLogService = OperationLogService.getInstance(this);
        String description = "عرض فاتورة رقم " + currentInvoice.getDisplayNumber() + 
                           " للعميل " + currentInvoice.getCustomerName() + 
                           " بقيمة " + CurrencyUtils.formatCurrency(currentInvoice.getTotalAmount());
        
        operationLogService.logView(
            OperationLog.EntityType.INVOICE,
            currentInvoice.getId(),
            description
        ).thenRun(() -> {
            android.util.Log.d("InvoicePrintActivity", "Invoice view logged successfully");
        }).exceptionally(throwable -> {
            android.util.Log.e("InvoicePrintActivity", "Failed to log invoice view", throwable);
            return null;
        });
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
            // Android 12 وما فوق
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 11 وما دون
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestBluetoothPermissions() {
        // عرض رسالة توضيحية للمستخدم
        new AlertDialog.Builder(this)
                .setTitle("صلاحيات البلوتوث")
                .setMessage("يحتاج التطبيق إلى صلاحيات البلوتوث للاتصال بالطابعة وطباعة الفواتير")
                .setPositiveButton("موافق", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Android 12 وما فوق
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        }, BLUETOOTH_PERMISSION_REQUEST_CODE);
                    } else {
                        // Android 11 وما دون
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
                // طلب أذونات البلوتوث المطلوبة
                requestBluetoothPermissions();
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

        // إنشاء قائمة بأسماء الأجهزة
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
        if (!checkBluetoothPermissions()) {
            Toast.makeText(this, "لا توجد أذونات للاتصال بالطابعة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // تشغيل الطباعة في thread منفصل لتجنب تجميد الواجهة
        new Thread(() -> {
            try {
                runOnUiThread(() -> {
                    Toast.makeText(this, "جاري الاتصال بالطابعة...", Toast.LENGTH_SHORT).show();
                });
                
                // إنشاء الاتصال
                bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
                bluetoothSocket.connect();
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "تم الاتصال، جاري الطباعة...", Toast.LENGTH_SHORT).show();
                });
                
                // طباعة الفاتورة
                printInvoice();
                
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "فشل في الاتصال بالطابعة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                
                // محاولة إغلاق الاتصال في حالة الفشل
                try {
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                    }
                } catch (IOException closeException) {
                    // تجاهل أخطاء الإغلاق
                }
            } catch (InterruptedException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "تم مقاطعة عملية الطباعة", Toast.LENGTH_SHORT).show();
                });
                Thread.currentThread().interrupt(); // استعادة حالة المقاطعة
            } catch (SecurityException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "ليس لديك صلاحية للاتصال بالطابعة", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String convertArabicToEnglish(String text) {
        if (text == null) return "N/A";
        
        // تحويل الأرقام العربية إلى إنجليزية
        text = text.replace("٠", "0").replace("١", "1").replace("٢", "2")
                  .replace("٣", "3").replace("٤", "4").replace("٥", "5")
                  .replace("٦", "6").replace("٧", "7").replace("٨", "8")
                  .replace("٩", "9");
        
        // تحويل بعض الكلمات العربية الشائعة
        text = text.replace("كيلو", "Kg").replace("جرام", "g")
                  .replace("لتر", "L").replace("متر", "m")
                  .replace("قطعة", "Pcs").replace("علبة", "Box")
                  .replace("كيس", "Bag").replace("زجاجة", "Bottle")
                  .replace("حليب", "Milk").replace("خبز", "Bread")
                  .replace("ماء", "Water").replace("عصير", "Juice")
                  .replace("شاي", "Tea").replace("قهوة", "Coffee")
                  .replace("سكر", "Sugar").replace("ملح", "Salt")
                  .replace("أرز", "Rice").replace("دقيق", "Flour")
                  .replace("زيت", "Oil").replace("صابون", "Soap")
                  .replace("شامبو", "Shampoo").replace("معجون", "Paste")
                  .replace("بسكويت", "Biscuit").replace("شوكولاتة", "Chocolate");
        
        // إزالة الأحرف العربية واستبدالها بمسافات
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || c == ' ' || c == '.' || 
                c == '-' || c == '_' || c == '(' || c == ')' || 
                c == '[' || c == ']' || c == '+' || c == '=' || 
                c == ',' || c == ':' || c == ';' || c == '!' || 
                c == '?' || c == '/' || c == '\\' || c == '*' || 
                c == '@' || c == '#' || c == '$' || c == '%' || 
                c == '&' || c == '^' || c == '~' || c == '`' || 
                c == '{' || c == '}' || c == '|' || c == '<' || 
                c == '>') {
                result.append(c);
            } else if (c >= '\u0600' && c <= '\u06FF') {
                // إذا كان حرف عربي، استبدله بـ transliteration
                result.append(getTransliteration(c));
            } else {
                result.append(' ');
            }
        }
        
        // تنظيف المسافات الزائدة
        return result.toString().replaceAll("\\s+", " ").trim();
    }
    
    private String getTransliteration(char arabicChar) {
        switch (arabicChar) {
            case 'ا': case 'أ': case 'إ': case 'آ': return "a";
            case 'ب': return "b";
            case 'ت': return "t";
            case 'ث': return "th";
            case 'ج': return "j";
            case 'ح': return "h";
            case 'خ': return "kh";
            case 'د': return "d";
            case 'ذ': return "dh";
            case 'ر': return "r";
            case 'ز': return "z";
            case 'س': return "s";
            case 'ش': return "sh";
            case 'ص': return "s";
            case 'ض': return "d";
            case 'ط': return "t";
            case 'ظ': return "z";
            case 'ع': return "a";
            case 'غ': return "gh";
            case 'ف': return "f";
            case 'ق': return "q";
            case 'ك': return "k";
            case 'ل': return "l";
            case 'م': return "m";
            case 'ن': return "n";
            case 'ه': return "h";
            case 'و': return "w";
            case 'ي': case 'ى': return "y";
            case 'ة': return "h";
            case 'ء': return "'";
            default: return "";
        }
    }

    private String generateCustomerQRData() {
        StringBuilder qrData = new StringBuilder();
        
        // معلومات العميل
        qrData.append("CUSTOMER_INFO\n");
        qrData.append("Name: ").append(currentInvoice.getCustomerName() != null ? currentInvoice.getCustomerName() : "N/A").append("\n");
        qrData.append("Phone: ").append(currentInvoice.getCustomerPhone() != null ? currentInvoice.getCustomerPhone() : "N/A").append("\n");
        
        // معلومات الفاتورة
        qrData.append("INVOICE_INFO\n");
        qrData.append("Invoice: ").append(currentInvoice.getDisplayNumber()).append("\n");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        qrData.append("Date: ").append(dateFormat.format(currentInvoice.getDate().toDate())).append("\n");
        qrData.append("Total: ").append(String.format("%.2f DZD", currentInvoice.getTotalAmount())).append("\n");
        qrData.append("Payment: ").append(currentInvoice.isPaid() ? "Cash" : "Credit");
        
        return qrData.toString();
    }

    private Bitmap generateQRCode(String data) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200, hints);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void printQRCode(OutputStream outputStream, Bitmap qrBitmap) throws IOException {
        if (qrBitmap == null) return;
        
        // توسيط QR Code
        outputStream.write(new byte[]{0x1B, 0x61, 0x01});
        
        // تحويل الـ bitmap إلى بيانات للطباعة
        int width = qrBitmap.getWidth();
        int height = qrBitmap.getHeight();
        
        // تصغير حجم QR Code للطباعة الحرارية (48x48 pixels)
        int printWidth = 48;
        int printHeight = 48;
        
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
        
        // العودة للمحاذاة اليسرى
        outputStream.write(new byte[]{0x1B, 0x61, 0x00});
    }

    private void printInvoice() throws InterruptedException {
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            
            // إعداد الطابعة
            outputStream.write(new byte[]{0x1B, 0x40}); // تهيئة الطابعة
            outputStream.write(new byte[]{0x1B, 0x74, 0x06}); // تعيين جدول الأحرف العربية
            outputStream.write(new byte[]{0x1B, 0x61, 0x01}); // توسيط النص
            
            // طباعة رأس الفاتورة بالأحرف الإنجليزية
            String encoding = "ISO-8859-1"; // ترميز أساسي يدعمه معظم الطابعات
            outputStream.write("================================\n".getBytes(encoding));
            outputStream.write("       INVOICE / FACTURE        \n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            
            // معلومات الفاتورة
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            outputStream.write(("Invoice No: " + currentInvoice.getDisplayNumber() + "\n").getBytes(encoding));
            outputStream.write(("Date: " + dateFormat.format(currentInvoice.getDate().toDate()) + "\n").getBytes(encoding));
            String customerName = currentInvoice.getCustomerName() != null ? 
                convertArabicToEnglish(currentInvoice.getCustomerName()) : "N/A";
            outputStream.write(("Customer: " + customerName + "\n").getBytes(encoding));
            outputStream.write(("Phone: " + (currentInvoice.getCustomerPhone() != null ? currentInvoice.getCustomerPhone() : "N/A") + "\n").getBytes(encoding));
            
            String paymentMethod = currentInvoice.isPaid() ? "Cash" : "Credit";
            outputStream.write(("Payment: " + paymentMethod + "\n").getBytes(encoding));
            
            outputStream.write("--------------------------------\n".getBytes(encoding));
            
            // عرض عدد العناصر
            outputStream.write(("Items: " + invoiceItems.size() + "\n").getBytes(encoding));
            outputStream.write("--------------------------------\n".getBytes(encoding));
            
            // محاذاة لليسار للعناصر
            outputStream.write(new byte[]{0x1B, 0x61, 0x00});
            
            // عناصر الفاتورة
            int itemNumber = 1;
            for (InvoiceItem item : invoiceItems) {
                // اسم المنتج مع التحويل للإنجليزية ورقم تسلسلي
                String productName = convertArabicToEnglish(item.getProductName());
                outputStream.write((itemNumber + ". " + productName + "\n").getBytes(encoding));
                
                // تفاصيل الكمية والسعر
                String itemDetails = String.format("   Qty: %d x %.2f = %.2f DZD\n", 
                        item.getQuantity(), item.getPrice(), item.getQuantity() * item.getPrice());
                outputStream.write(itemDetails.getBytes(encoding));
                outputStream.write("--------------------------------\n".getBytes(encoding));
                
                itemNumber++;
                
                // إضافة تأخير صغير بين العناصر
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep interrupted during item printing", e);
                    Thread.currentThread().interrupt();
                    break; // الخروج من الحلقة في حالة المقاطعة
                }
            }
            
            // توسيط للمجموع
            outputStream.write(new byte[]{0x1B, 0x61, 0x01});
            outputStream.write(new byte[]{0x1B, 0x45, 0x01}); // خط عريض
            
            try {
                double totalAmount = currentInvoice.getTotalAmount();
                String totalText = String.format("TOTAL: %.2f DZD\n", totalAmount);
                outputStream.write(totalText.getBytes(encoding));
            } catch (Exception e) {
                outputStream.write("TOTAL: N/A\n".getBytes(encoding));
            }
            
            outputStream.write(new byte[]{0x1B, 0x45, 0x00}); // إلغاء الخط العريض
            
            outputStream.write("================================\n".getBytes(encoding));
            outputStream.write("     THANK YOU / MERCI         \n".getBytes(encoding));
            outputStream.write("================================\n".getBytes(encoding));
            
            // إضافة QR Code للعميل
            outputStream.write("\n".getBytes(encoding));
            outputStream.write(new byte[]{0x1B, 0x61, 0x01}); // توسيط
            outputStream.write("Customer QR Code:\n".getBytes(encoding));
            
            String qrData = generateCustomerQRData();
            Bitmap qrBitmap = generateQRCode(qrData);
            
            if (qrBitmap != null) {
                printQRCode(outputStream, qrBitmap);
                outputStream.write("\n".getBytes(encoding));
            } else {
                outputStream.write("QR Code generation failed\n".getBytes(encoding));
            }
            
            // إضافة أسطر فارغة
            outputStream.write("\n\n".getBytes(encoding));
            
            // قطع الورق
            outputStream.write(new byte[]{0x1D, 0x56, 0x41, 0x10});
            
            // التأكد من إرسال جميع البيانات
            outputStream.flush();
            
            // إضافة تأخير قبل إغلاق الاتصال
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Sleep interrupted before closing connection", e);
                Thread.currentThread().interrupt();
            }
            
            // إغلاق الاتصال
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                bluetoothSocket.close();
            }
            
            runOnUiThread(() -> {
                Toast.makeText(this, "تم طباعة الفاتورة بنجاح", Toast.LENGTH_SHORT).show();
                
                // تسجيل عملية الطباعة في الأرشيف
                OperationLogService operationLogService = OperationLogService.getInstance(this);
                String description = "طباعة فاتورة رقم " + currentInvoice.getDisplayNumber() + 
                                   " للعميل " + currentInvoice.getCustomerName() + 
                                   " بقيمة " + CurrencyUtils.formatCurrency(currentInvoice.getTotalAmount());
                
                operationLogService.logPrint(
                    OperationLog.EntityType.INVOICE,
                    currentInvoice.getId(),
                    description
                ).thenRun(() -> {
                    android.util.Log.d("InvoicePrintActivity", "Invoice print logged successfully");
                }).exceptionally(throwable -> {
                    android.util.Log.e("InvoicePrintActivity", "Failed to log invoice print", throwable);
                    return null;
                });
            });
            
        } catch (IOException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "فشل في طباعة الفاتورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "خطأ في الطباعة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void shareInvoice() {
        if (currentInvoice == null) {
            Toast.makeText(this, "لا يمكن مشاركة الفاتورة، البيانات غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            StringBuilder invoiceText = new StringBuilder();
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            
            invoiceText.append("=== فاتورة مبيعات ===\n\n");
            invoiceText.append("رقم الفاتورة: ").append(currentInvoice.getDisplayNumber()).append("\n");
            
            if (currentInvoice.getDate() != null) {
                try {
                    invoiceText.append("التاريخ: ").append(dateFormat.format(currentInvoice.getDate().toDate())).append("\n");
                } catch (Exception e) {
                    invoiceText.append("التاريخ: غير متوفر\n");
                }
            }
            
            invoiceText.append("العميل: ").append(currentInvoice.getCustomerName() != null ? currentInvoice.getCustomerName() : "غير محدد").append("\n");
            invoiceText.append("الهاتف: ").append(currentInvoice.getCustomerPhone() != null ? currentInvoice.getCustomerPhone() : "غير محدد").append("\n");
            
            // استخدام النظام الجديد لطريقة الدفع
            PaymentMethod paymentMethod = currentInvoice.getPaymentMethod();
            invoiceText.append("طريقة الدفع: ").append(paymentMethod.getDisplayWithIcon()).append("\n\n");
            
            invoiceText.append("=== العناصر ===\n");
            if (invoiceItems != null && !invoiceItems.isEmpty()) {
                for (InvoiceItem item : invoiceItems) {
                    if (item != null) {
                        try {
                            invoiceText.append(item.getProductName() != null ? item.getProductName() : "منتج غير محدد").append("\n");
                            
                            // التأكد من أن القيم ليست null قبل التنسيق
                            int quantity = item.getQuantity();
                            double price = item.getPrice();
                            double total = quantity * price;
                            
                            invoiceText.append(String.format("الكمية: %d × %s = %s\n\n", 
                                    quantity, 
                                    CurrencyUtils.formatCurrency(price),
                                    CurrencyUtils.formatCurrency(total)));
                        } catch (Exception e) {
                            invoiceText.append("عنصر غير صالح\n\n");
                        }
                    }
                }
            } else {
                invoiceText.append("لا توجد عناصر في الفاتورة\n\n");
            }
            
            // التأكد من أن المبلغ الإجمالي ليس null
            try {
                double totalAmount = currentInvoice.getTotalAmount();
                invoiceText.append("المجموع الكلي: " + CurrencyUtils.formatCurrency(totalAmount));
            } catch (Exception e) {
                invoiceText.append("المجموع الكلي: غير متوفر");
            }
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, invoiceText.toString());
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "فاتورة رقم " + currentInvoice.getDisplayNumber());
            
            try {
                startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة"));
            } catch (Exception e) {
                Toast.makeText(this, "فشل في فتح تطبيق المشاركة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ أثناء إعداد المشاركة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showEditInvoiceDialog() {
        if (currentInvoice == null || invoiceItems == null || invoiceItems.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_items_to_edit), Toast.LENGTH_SHORT).show();
            return;
        }

        // تبديل المحول لدعم التعديل
        enableEditMode();
    }

    private void enableEditMode() {
        // إنشاء محول جديد مع إمكانية التعديل
        EditableInvoicePrintItemAdapter editableAdapter = new EditableInvoicePrintItemAdapter(invoiceItems);
        
        // إعداد مستمع التعديل
        editableAdapter.setOnItemEditListener((item, position) -> {
            EditInvoiceItemDialog dialog = EditInvoiceItemDialog.newInstance(item, position);
            dialog.setOnItemUpdatedListener((updatedItem, itemPosition) -> {
                // تحديث البند في القائمة
                invoiceItems.set(itemPosition, updatedItem);
                editableAdapter.notifyItemChanged(itemPosition);
                
                // إعادة حساب المجموع
                recalculateTotal();
                
                // إظهار خيارات الحفظ
                showSaveChangesDialog();
            });
            dialog.show(getSupportFragmentManager(), "EditInvoiceItemDialog");
        });
        
        // إعداد مستمع الحذف
        editableAdapter.setOnItemDeleteListener(position -> {
            if (position >= 0 && position < invoiceItems.size()) {
                // إظهار حوار التأكيد قبل الحذف
                InvoiceItem itemToDelete = invoiceItems.get(position);
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("حذف منتج")
                    .setMessage("هل أنت متأكد من حذف \"" + itemToDelete.getProductName() + "\" من الفاتورة؟")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("حذف", (dialog, which) -> {
                        // حذف المنتج من القائمة
                        invoiceItems.remove(position);
                        editableAdapter.notifyItemRemoved(position);
                        editableAdapter.notifyItemRangeChanged(position, invoiceItems.size());
                        
                        // إعادة حساب المجموع
                        recalculateTotal();
                        
                        // إظهار خيارات الحفظ
                        showSaveChangesDialog();
                        
                        Toast.makeText(this, "✅ تم حذف " + itemToDelete.getProductName(), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss())
                    .show();
            }
        });

        itemsRecyclerView.setAdapter(editableAdapter);
        
        // تغيير نص الزر
        editButton.setText(getString(R.string.finish_editing));
        editButton.setOnClickListener(v -> disableEditMode());
        
        Toast.makeText(this, getString(R.string.edit_mode_enabled) + "\n💡 يمكنك الآن تعديل أو حذف المنتجات", Toast.LENGTH_LONG).show();
    }

    private void disableEditMode() {
        // العودة للمحول العادي
        itemsAdapter = new InvoicePrintItemAdapter(invoiceItems);
        itemsRecyclerView.setAdapter(itemsAdapter);
        
        // إعادة تعيين نص الزر ووظيفته
        editButton.setText(getString(R.string.edit_invoice));
        editButton.setOnClickListener(v -> {
            try {
                showEditInvoiceDialog();
            } catch (Exception e) {
                Toast.makeText(this, "خطأ في فتح تعديل الفاتورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        Toast.makeText(this, getString(R.string.edit_mode_disabled), Toast.LENGTH_SHORT).show();
    }

    private void recalculateTotal() {
        double newTotal = 0;
        for (InvoiceItem item : invoiceItems) {
            newTotal += item.getPrice() * item.getQuantity();
        }
        
        // تحديث المجموع في واجهة المستخدم
        totalAmountTextView.setText("المجموع: " + CurrencyUtils.formatCurrency(newTotal));
        
        // تحديث المجموع في كائن الفاتورة
        if (currentInvoice != null) {
            currentInvoice.setTotalAmount(newTotal);
        }
    }

    private void showSaveChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.save_changes))
                .setMessage(getString(R.string.save_changes_question))
                .setPositiveButton("حفظ", (dialog, which) -> saveInvoiceChanges())
                .setNegativeButton("لاحقاً", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void saveInvoiceChanges() {
        if (currentInvoice == null) {
            Toast.makeText(this, "خطأ: بيانات الفاتورة غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }

        // إظهار progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.saving_changes));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // تحديث بنود الفاتورة
        currentInvoice.setItems(invoiceItems);
        
        // حفظ في قاعدة البيانات
        db.collection("invoices").document(currentInvoice.getId())
                .set(currentInvoice)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.changes_saved), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.save_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    public static Intent createIntent(android.content.Context context, String invoiceId) {
        Intent intent = new Intent(context, InvoicePrintActivity.class);
        intent.putExtra(ARG_INVOICE_ID, invoiceId);
        return intent;
    }

    @Override
    public void onPaymentMethodChanged(PaymentMethod newPaymentMethod) {
        if (currentInvoice != null) {
            // تحديث طريقة الدفع في الفاتورة
            currentInvoice.setPaymentMethod(newPaymentMethod);
            
            // تحديث العرض
            updatePaymentMethodDisplay();
            
            // عرض حوار لحفظ التغييرات
            showSavePaymentMethodChangesDialog(newPaymentMethod);
        }
    }
    
    /**
     * عرض حوار تعديل طريقة الدفع
     */
    private void showEditPaymentMethodDialog() {
        if (currentInvoice == null) {
            Toast.makeText(this, "خطأ: بيانات الفاتورة غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        EditPaymentMethodDialog dialog = EditPaymentMethodDialog.newInstance(currentInvoice);
        dialog.show(getSupportFragmentManager(), "EditPaymentMethodDialog");
    }
    
    /**
     * تحديث عرض طريقة الدفع
     */
    private void updatePaymentMethodDisplay() {
        if (currentInvoice != null && paymentMethodTextView != null) {
            PaymentMethod method = currentInvoice.getPaymentMethod();
            paymentMethodTextView.setText("طريقة الدفع: " + method.getDisplayWithIcon());
            
            // تغيير لون النص حسب نوع الدفع
            if (method == PaymentMethod.CREDIT) {
                paymentMethodTextView.setTextColor(getResources().getColor(R.color.reportWarning));
            } else {
                paymentMethodTextView.setTextColor(getResources().getColor(R.color.reportSuccess));
            }
        }
    }
    
    /**
     * عرض حوار لحفظ تغييرات طريقة الدفع
     */
    private void showSavePaymentMethodChangesDialog(PaymentMethod newMethod) {
        new AlertDialog.Builder(this)
                .setTitle("حفظ التغييرات")
                .setMessage("هل تريد حفظ تغيير طريقة الدفع إلى: " + newMethod.getDisplayWithIcon() + "؟")
                .setPositiveButton("حفظ", (dialog, which) -> {
                    savePaymentMethodChanges(newMethod);
                })
                .setNegativeButton("إلغاء", (dialog, which) -> {
                    // الرجوع للطريقة السابقة
                    loadInvoiceData();
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * حفظ تغييرات طريقة الدفع في قاعدة البيانات
     */
    private void savePaymentMethodChanges(PaymentMethod newMethod) {
        if (currentInvoice == null || invoiceId == null) {
            Toast.makeText(this, "خطأ: بيانات الفاتورة غير متوفرة", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // إظهار رسالة التحميل
        Toast.makeText(this, "جاري حفظ التغييرات...", Toast.LENGTH_SHORT).show();
        
        // تحديث الفاتورة في قاعدة البيانات
        db.collection("invoices").document(invoiceId)
                .update("paymentMethod", newMethod.name(), 
                       "isPaid", newMethod.isLegacyPaid())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ تم حفظ طريقة الدفع بنجاح", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Payment method updated successfully: " + newMethod.getDisplayName());
                    
                    // تحديث العرض النهائي
                    updatePaymentMethodDisplay();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ فشل في حفظ التغييرات: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update payment method", e);
                    
                    // الرجوع للحالة السابقة
                    loadInvoiceData();
                });
    }

} 