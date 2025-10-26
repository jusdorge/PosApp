package com.example.islamicquiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanOptions;

public class QRScannerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("مسح QR Code");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // إعداد خيارات المسح
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("امسح QR Code الخاص بالعميل");
        options.setCameraId(0);  // استخدام الكاميرا الخلفية
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);

        // بدء المسح
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.setAction("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        intent.putExtra("PROMPT_MESSAGE", "امسح QR Code الخاص بالعميل");
        
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100) {
            if (resultCode == RESULT_OK && data != null) {
                String qrData = data.getStringExtra("SCAN_RESULT");
                
                if (qrData != null) {
                    // تحليل البيانات المقروءة
                    if (isCustomerQR(qrData)) {
                        String customerId = extractCustomerId(qrData);
                        if (customerId != null && !customerId.equals("N/A")) {
                            // إرسال النتيجة إلى Activity السابق
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("customer_id", customerId);
                            resultIntent.putExtra("qr_data", qrData);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            Toast.makeText(this, "QR Code غير صالح: لا يحتوي على معرف عميل", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "هذا ليس QR Code خاص بعميل", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(this, "فشل في قراءة QR Code", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                // المستخدم ألغى المسح
                finish();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * التحقق من أن QR Code يحتوي على معلومات عميل
     */
    private boolean isCustomerQR(String qrData) {
        return qrData != null && qrData.contains("CUSTOMER_INFO") && qrData.contains("Customer_ID:");
    }

    /**
     * استخراج معرف العميل من بيانات QR Code
     */
    private String extractCustomerId(String qrData) {
        try {
            String[] lines = qrData.split("\n");
            for (String line : lines) {
                if (line.startsWith("Customer_ID:")) {
                    return line.substring("Customer_ID:".length()).trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
} 