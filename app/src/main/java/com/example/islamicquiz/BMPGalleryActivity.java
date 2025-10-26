package com.example.islamicquiz;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * معرض الفواتير المحفوظة كصور BMP
 */
public class BMPGalleryActivity extends AppCompatActivity {
    private GridView gridView;
    private List<File> bmpFiles = new ArrayList<>();
    private BMPAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmp_gallery);
        
        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("معرض الفواتير");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        gridView = findViewById(R.id.gridView);
        adapter = new BMPAdapter();
        gridView.setAdapter(adapter);
        
        // تحميل الملفات
        loadBMPFiles();
        
        // إعداد النقر على العناصر
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = bmpFiles.get(position);
            openBMPFile(selectedFile);
        });
        
        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            File selectedFile = bmpFiles.get(position);
            showFileOptions(selectedFile);
            return true;
        });
    }
    
    /**
     * تحميل ملفات BMP من مجلد الحفظ
     */
    private void loadBMPFiles() {
        File documentsDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS), "POS_Invoices");
        
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }
        
        bmpFiles.clear();
        File[] files = documentsDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".bmp"));
        
        if (files != null) {
            // ترتيب حسب تاريخ التعديل (الأحدث أولاً)
            Arrays.sort(files, (f1, f2) -> 
                Long.compare(f2.lastModified(), f1.lastModified()));
            bmpFiles.addAll(Arrays.asList(files));
        }
        
        adapter.notifyDataSetChanged();
        
        if (bmpFiles.isEmpty()) {
            Toast.makeText(this, "لا توجد فواتير محفوظة كصور", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * فتح ملف BMP في تطبيق العرض
     */
    private void openBMPFile(File bmpFile) {
        try {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", bmpFile);
            viewIntent.setDataAndType(fileUri, "image/bmp");
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(viewIntent);
        } catch (Exception e) {
            Toast.makeText(this, "لا يوجد تطبيق لعرض الصورة", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * عرض خيارات الملف
     */
    private void showFileOptions(File bmpFile) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("خيارات الملف")
            .setMessage("الملف: " + bmpFile.getName())
            .setPositiveButton("طباعة", (dialog, which) -> printBMPFile(bmpFile))
            .setNeutralButton("مشاركة", (dialog, which) -> shareBMPFile(bmpFile))
            .setNegativeButton("حذف", (dialog, which) -> deleteBMPFile(bmpFile))
            .show();
    }
    
    /**
     * طباعة ملف BMP
     */
    private void printBMPFile(File bmpFile) {
        try {
            // قراءة الصورة من الملف
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), options);
            
            if (bitmap == null) {
                Toast.makeText(this, "فشل في قراءة الصورة", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // إنشاء printer وطباعة الصورة
            BMPPrinter bmpPrinter = new BMPPrinter(this);
            
            bmpPrinter.printBitmap(bitmap, new BMPPrinter.PrintCallback() {
                @Override
                public void onPrintStart() {
                    runOnUiThread(() -> {
                        Toast.makeText(BMPGalleryActivity.this, "بدء الطباعة...", Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onPrintProgress(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(BMPGalleryActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
                
                @Override
                public void onPrintSuccess() {
                    runOnUiThread(() -> {
                        new androidx.appcompat.app.AlertDialog.Builder(BMPGalleryActivity.this)
                            .setTitle("تمت الطباعة بنجاح!")
                            .setMessage("تم طباعة الصورة بنجاح على الطابعة")
                            .setPositiveButton("حسناً", null)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                    });
                }
                
                @Override
                public void onPrintError(String error) {
                    runOnUiThread(() -> {
                        new androidx.appcompat.app.AlertDialog.Builder(BMPGalleryActivity.this)
                            .setTitle("فشل في الطباعة")
                            .setMessage("حدث خطأ أثناء الطباعة:\n\n" + error)
                            .setPositiveButton("حسناً", null)
                            .setNegativeButton("إعادة المحاولة", (d, w) -> printBMPFile(bmpFile))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    });
                }
            });
            
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في تحضير الطباعة: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * مشاركة ملف BMP
     */
    private void shareBMPFile(File bmpFile) {
        InvoiceToBMPConverter converter = new InvoiceToBMPConverter(this);
        converter.shareBMPFile(bmpFile);
    }
    
    /**
     * حذف ملف BMP
     */
    private void deleteBMPFile(File bmpFile) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت متأكد من حذف هذا الملف؟\n\n" + bmpFile.getName())
            .setPositiveButton("نعم، احذف", (dialog, which) -> {
                if (bmpFile.delete()) {
                    Toast.makeText(this, "تم حذف الملف", Toast.LENGTH_SHORT).show();
                    loadBMPFiles(); // إعادة تحميل القائمة
                } else {
                    Toast.makeText(this, "فشل في حذف الملف", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadBMPFiles(); // إعادة تحميل عند العودة للنشاط
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    /**
     * محول البيانات للـ GridView
     */
    private class BMPAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return bmpFiles.size();
        }
        
        @Override
        public Object getItem(int position) {
            return bmpFiles.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            
            if (convertView == null) {
                convertView = LayoutInflater.from(BMPGalleryActivity.this)
                    .inflate(R.layout.item_bmp_file, parent, false);
                holder = new ViewHolder();
                holder.imageView = convertView.findViewById(R.id.imageView);
                holder.titleText = convertView.findViewById(R.id.titleText);
                holder.dateText = convertView.findViewById(R.id.dateText);
                holder.sizeText = convertView.findViewById(R.id.sizeText);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            File bmpFile = bmpFiles.get(position);
            
            // عرض معاينة الصورة
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 8; // تصغير الصورة للمعاينة
                Bitmap bitmap = BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), options);
                holder.imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            // معلومات الملف
            holder.titleText.setText(bmpFile.getName());
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.dateText.setText(dateFormat.format(new Date(bmpFile.lastModified())));
            
            long sizeInKB = bmpFile.length() / 1024;
            holder.sizeText.setText(sizeInKB + " KB");
            
            return convertView;
        }
        
        private class ViewHolder {
            ImageView imageView;
            TextView titleText;
            TextView dateText;
            TextView sizeText;
        }
    }
} 