package com.example.posapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.posapp.model.Product;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditProductActivity extends AppCompatActivity {

    private TextInputEditText productNameEditText;
    private TextInputEditText barcodeEditText;
    private TextInputEditText costPriceEditText;
    private TextInputEditText sellingPriceEditText;
    private TextInputEditText quantityEditText;
    private TextInputEditText categoryEditText;
    private Button saveButton;
    private Button cancelButton;
    private Button deleteButton;

    private FirebaseFirestore db;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        // إعداد شريط الأدوات
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("تعديل المنتج");

        // ربط العناصر
        productNameEditText = findViewById(R.id.productNameEditText);
        barcodeEditText = findViewById(R.id.barcodeEditText);
        costPriceEditText = findViewById(R.id.costPriceEditText);
        sellingPriceEditText = findViewById(R.id.sellingPriceEditText);
        quantityEditText = findViewById(R.id.quantityEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        deleteButton = findViewById(R.id.deleteButton);

        // تهيئة Firestore
        db = FirebaseFirestore.getInstance();

        // الحصول على معرف المنتج من Intent
        productId = getIntent().getStringExtra("product_id");
        if (productId == null) {
            Toast.makeText(this, "خطأ: معرف المنتج غير متوفر", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // تحميل بيانات المنتج
        loadProductData();

        // إعداد مستمعي الأحداث
        setupListeners();
    }

    private void loadProductData() {
        DocumentReference productRef = db.collection("products").document(productId);
        productRef.get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    currentProduct = documentSnapshot.toObject(Product.class);
                    if (currentProduct != null) {
                        currentProduct.setId(documentSnapshot.getId());
                        populateFields();
                    }
                } else {
                    Toast.makeText(this, "لم يتم العثور على المنتج", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في تحميل بيانات المنتج: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void populateFields() {
        productNameEditText.setText(currentProduct.getName());
        barcodeEditText.setText(currentProduct.getBarcode());
        costPriceEditText.setText(String.valueOf(currentProduct.getCostPrice()));
        sellingPriceEditText.setText(String.valueOf(currentProduct.getSellingPrice()));
        quantityEditText.setText(String.valueOf(currentProduct.getQuantity()));
        
        if (currentProduct.getCategory() != null) {
            categoryEditText.setText(currentProduct.getCategory());
        }
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveProduct());
        cancelButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void saveProduct() {
        String name = productNameEditText.getText().toString().trim();
        String barcode = barcodeEditText.getText().toString().trim();
        String costPriceStr = costPriceEditText.getText().toString().trim();
        String sellingPriceStr = sellingPriceEditText.getText().toString().trim();
        String quantityStr = quantityEditText.getText().toString().trim();
        String category = categoryEditText.getText().toString().trim();

        // التحقق من الحقول المطلوبة
        if (TextUtils.isEmpty(name)) {
            productNameEditText.setError("الرجاء إدخال اسم المنتج");
            return;
        }

//        if (TextUtils.isEmpty(barcode)) {
//            barcodeEditText.setError("الرجاء إدخال باركود المنتج");
//            return;
//        }

        if (TextUtils.isEmpty(costPriceStr)) {
            costPriceEditText.setError("الرجاء إدخال سعر التكلفة");
            return;
        }

        if (TextUtils.isEmpty(sellingPriceStr)) {
            sellingPriceEditText.setError("الرجاء إدخال سعر البيع");
            return;
        }

        if (TextUtils.isEmpty(quantityStr)) {
            quantityEditText.setError("الرجاء إدخال الكمية");
            return;
        }

        // تحويل القيم إلى أنواع مناسبة
        double costPrice = Double.parseDouble(costPriceStr);
        double sellingPrice = Double.parseDouble(sellingPriceStr);
        int quantity = Integer.parseInt(quantityStr);

        // التحقق من صحة القيم
        if (costPrice < 0) {
            costPriceEditText.setError("يجب أن يكون سعر التكلفة قيمة موجبة");
            return;
        }

        if (sellingPrice < 0) {
            sellingPriceEditText.setError("يجب أن يكون سعر البيع قيمة موجبة");
            return;
        }

        if (quantity < 0) {
            quantityEditText.setError("يجب أن تكون الكمية قيمة موجبة");
            return;
        }

        // تحديث كائن المنتج بالقيم الجديدة
        currentProduct.setName(name);
        currentProduct.setBarcode(barcode);
        currentProduct.setCostPrice(costPrice);
        currentProduct.setSellingPrice(sellingPrice);
        currentProduct.setQuantity(quantity);
        currentProduct.setCategory(category);

        // حفظ التغييرات في Firestore
        db.collection("products").document(productId)
            .set(currentProduct)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "تم تحديث المنتج بنجاح", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في تحديث المنتج: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setTitle("حذف المنتج")
            .setMessage("هل أنت متأكد من رغبتك في حذف هذا المنتج؟")
            .setPositiveButton("حذف", (dialog, which) -> deleteProduct())
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void deleteProduct() {
        db.collection("products").document(productId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "تم حذف المنتج بنجاح", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في حذف المنتج: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 