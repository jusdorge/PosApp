package com.example.islamicquiz;

import android.os.Bundle;
import android.net.Uri;
import android.widget.ImageView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.islamicquiz.model.Product;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.StorageMetadata;
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
    private Button changeImageButton;
    private ImageView productImageView;

    private FirebaseFirestore db;
    private String productId;
    private Product currentProduct;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 201;

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
        changeImageButton = findViewById(R.id.changeImageButton);
        productImageView = findViewById(R.id.productImageView);

        // تهيئة Firestore
        db = FirebaseFirestore.getInstance();
        // تهيئة Firebase Storage
        try {
            String bucket = getString(getResources().getIdentifier("google_storage_bucket", "string", getPackageName()));
            if (bucket != null && !bucket.trim().isEmpty()) {
                storage = FirebaseStorage.getInstance("gs://" + bucket);
            } else {
                storage = FirebaseStorage.getInstance();
            }
        } catch (Exception e) {
            storage = FirebaseStorage.getInstance();
        }
        storageRef = storage.getReference();

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
        sellingPriceEditText.setText(String.valueOf(currentProduct.getDefaultPrice()));
        quantityEditText.setText(String.valueOf(currentProduct.getQuantity()));
        
        if (currentProduct.getCategory() != null) {
            categoryEditText.setText(currentProduct.getCategory());
        }
        if (currentProduct.getImageUrl() != null && !currentProduct.getImageUrl().isEmpty()) {
            try {
                // تحميل الصورة عبر Glide إن كانت موجودة، أو بديل بسيط
                new Thread(() -> {
                    try {
                        java.io.InputStream in = new java.net.URL(currentProduct.getImageUrl()).openStream();
                        final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
                        runOnUiThread(() -> productImageView.setImageBitmap(bmp));
                    } catch (Exception ignored) {}
                }).start();
            } catch (Exception ignored) {}
        }
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveProduct());
        cancelButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> confirmDelete());
        changeImageButton.setOnClickListener(v -> openImageChooser());
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);
            } catch (Exception ignored) {}
            Toast.makeText(this, getString(R.string.image_chosen), Toast.LENGTH_SHORT).show();
            productImageView.setImageURI(selectedImageUri);
        }
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
        currentProduct.setDefaultPrice(sellingPrice);
        currentProduct.setQuantity(quantity);
        currentProduct.setCategory(category);

        // إذا تم اختيار صورة جديدة، ارفعها ثم حدّث المنتج بالرابط
        if (selectedImageUri != null) {
            uploadImageAndSave(selectedImageUri);
        } else {
            // حفظ التغييرات في Firestore دون تغيير الصورة
            saveProductDocument();
        }
    }

    private void uploadImageAndSave(Uri imageUri) {
        Toast.makeText(this, getString(R.string.uploading_image), Toast.LENGTH_SHORT).show();
        StorageReference imageRef = storageRef.child("product_images/" + productId + ".jpg");
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
        UploadTask uploadTask = imageRef.putFile(imageUri, metadata);
        uploadTask.addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    currentProduct.setImageUrl(uri.toString());
                    saveProductDocument();
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_upload_image) + e.getMessage(), Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_upload_image) + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void saveProductDocument() {
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