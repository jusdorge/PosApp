package com.example.posapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.posapp.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddProductActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private String imageUrl; // رابط الصورة (download URL)
    private Uri selectedImageUri; // URI المحلي للصورة المختارة
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText productNameInput, productCategoryInput, productBarcodeInput,
                     productSellingPriceInput, productCostPriceInput,
                     productQuantityInput, productMinQuantityInput;
    private Button addProductButton, uploadImageButton, cancelButton;
    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_product);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // إعداد شريط التنقل
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("إضافة منتج جديد");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // تهيئة Firestore
        db = FirebaseFirestore.getInstance();
        // تهيئة Firebase Storage مع التأكد من استخدام الحاوية الصحيحة
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
        Log.d("AddProduct", "Using storage bucket: " + storageRef.toString());
        // تهيئة المصادقة
        auth = FirebaseAuth.getInstance();

        // ربط العناصر الجديدة
        productNameInput = findViewById(R.id.productNameInput);
        productCategoryInput = findViewById(R.id.productCategoryInput);
        productBarcodeInput = findViewById(R.id.productBarcodeInput);
        productSellingPriceInput = findViewById(R.id.productSellingPriceInput);
        productCostPriceInput = findViewById(R.id.productCostPriceInput);
        productQuantityInput = findViewById(R.id.productQuantityInput);
        productMinQuantityInput = findViewById(R.id.productMinQuantityInput);
        addProductButton = findViewById(R.id.addProductButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        cancelButton = findViewById(R.id.cancelButton);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);

        productList = new ArrayList<>();
        productAdapter = new ProductAdapter(productList);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(productAdapter);

        addProductButton.setOnClickListener(v -> addProduct());
        uploadImageButton.setOnClickListener(v -> openImageChooser());
        cancelButton.setOnClickListener(v -> confirmExit());

        // تنظيف الحقول عند فتح الشاشة
        clearAllFields();
        
        loadProducts(); // Load products from Firestore
    }

    private void loadProducts() {
        db.collection("products").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                productList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Product product = document.toObject(Product.class);
                    productList.add(product);
                }
                productAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(AddProductActivity.this, "فشل في تحميل المنتجات", Toast.LENGTH_SHORT).show();
            }
        });
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
            Toast.makeText(this, "تم اختيار الصورة", Toast.LENGTH_SHORT).show();
        }
    }

    private void addProduct() {
        try {
            String name = productNameInput.getText().toString();
            String category = productCategoryInput.getText().toString();
            String barcode = productBarcodeInput.getText().toString();
            double sellingPrice = Double.parseDouble(productSellingPriceInput.getText().toString());
            double costPrice = Double.parseDouble(productCostPriceInput.getText().toString());
            int quantity = Integer.parseInt(productQuantityInput.getText().toString());
            int minQuantity = Integer.parseInt(productMinQuantityInput.getText().toString());

            if (name.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "يرجى ملء جميع الحقول المطلوبة", Toast.LENGTH_SHORT).show();
                return;
            }

            String productId = UUID.randomUUID().toString();

            if (selectedImageUri != null) {
                ensureAuthAndUpload(productId, name, category, sellingPrice, costPrice, quantity, minQuantity, barcode, selectedImageUri);
            } else {
                // بدون صورة
                saveProductToFirestore(new Product(
                    productId,
                    name,
                    category,
                    sellingPrice,
                    costPrice,
                    quantity,
                    minQuantity,
                    barcode,
                    ""
                ));
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "يرجى إدخال قيم صحيحة للأرقام", Toast.LENGTH_SHORT).show();
        }
    }

    private void ensureAuthAndUpload(String productId,
                                     String name,
                                     String category,
                                     double sellingPrice,
                                     double costPrice,
                                     int quantity,
                                     int minQuantity,
                                     String barcode,
                                     Uri imageUri) {
        if (auth.getCurrentUser() != null) {
            uploadImageAndSaveProduct(productId, name, category, sellingPrice, costPrice, quantity, minQuantity, barcode, imageUri);
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener(result -> uploadImageAndSaveProduct(productId, name, category, sellingPrice, costPrice, quantity, minQuantity, barcode, imageUri))
                .addOnFailureListener(e -> Toast.makeText(this, "فشل تسجيل الدخول للمصادقة: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void uploadImageAndSaveProduct(String productId,
                                           String name,
                                           String category,
                                           double sellingPrice,
                                           double costPrice,
                                           int quantity,
                                           int minQuantity,
                                           String barcode,
                                           Uri imageUri) {
        Toast.makeText(this, "جارٍ رفع الصورة...", Toast.LENGTH_SHORT).show();
        StorageReference imageRef = storageRef.child("product_images/" + productId + ".jpg");
        StorageMetadata metadata = new StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build();
        UploadTask uploadTask = imageRef.putFile(imageUri, metadata);
        uploadTask
            .addOnSuccessListener(taskSnapshot -> {
                Log.d("AddProduct", "Upload success to: " + imageRef.getPath());
                fetchDownloadUrlAndSave(imageRef, productId, name, category, sellingPrice, costPrice, quantity, minQuantity, barcode, 1);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل رفع الصورة: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("AddProduct", "Upload failed at: " + imageRef.getPath(), e);
            });
    }

    private void fetchDownloadUrlAndSave(StorageReference imageRef,
                                         String productId,
                                         String name,
                                         String category,
                                         double sellingPrice,
                                         double costPrice,
                                         int quantity,
                                         int minQuantity,
                                         String barcode,
                                         int attempt) {
        imageRef.getDownloadUrl()
            .addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Product product = new Product(
                    productId,
                    name,
                    category,
                    sellingPrice,
                    costPrice,
                    quantity,
                    minQuantity,
                    barcode,
                    downloadUrl
                );
                saveProductToFirestore(product);
            })
            .addOnFailureListener(e -> {
                if (e instanceof StorageException) {
                    int errorCode = ((StorageException) e).getErrorCode();
                    if (errorCode == StorageException.ERROR_OBJECT_NOT_FOUND && attempt <= 2) {
                        // أعد المحاولة بعد تأخير بسيط
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                            fetchDownloadUrlAndSave(imageRef, productId, name, category, sellingPrice, costPrice, quantity, minQuantity, barcode, attempt + 1),
                            800
                        );
                        return;
                    } else if (errorCode == StorageException.ERROR_NOT_AUTHORIZED || errorCode == StorageException.ERROR_NOT_AUTHENTICATED) {
                        Toast.makeText(this, "قواعد التخزين تمنع الوصول لرابط الصورة. فعّل القراءة للمستخدم المصادق.", Toast.LENGTH_LONG).show();
                        Log.e("AddProduct", "Storage rules blocking read for path: " + imageRef.getPath(), e);
                        return;
                    }
                }
                Toast.makeText(this, "فشل الحصول على رابط الصورة: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("AddProduct", "getDownloadUrl failed for: " + imageRef.getPath(), e);
            });
    }

    private void saveProductToFirestore(Product product) {
        db.collection("products")
            .document(product.getId())
            .set(product)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "تمت إضافة المنتج بنجاح", Toast.LENGTH_SHORT).show();
                clearInputs();
                loadProducts(); // Reload products after adding
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في إضافة المنتج: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void clearInputs() {
        productNameInput.setText("");
        productCategoryInput.setText("");
        productBarcodeInput.setText("");
        productSellingPriceInput.setText("");
        productCostPriceInput.setText("");
        productQuantityInput.setText("");
        productMinQuantityInput.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // التعامل مع الإعدادات
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        confirmExit();
        return true;
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    private void confirmExit() {
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("تأكيد الخروج")
                    .setMessage("هل تريد الخروج بدون حفظ التغييرات؟")
                    .setPositiveButton("خروج", (dialog, which) -> {
                        super.onBackPressed();
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        return !productNameInput.getText().toString().trim().isEmpty() ||
               !productCategoryInput.getText().toString().trim().isEmpty() ||
               !productBarcodeInput.getText().toString().trim().isEmpty() ||
               !productSellingPriceInput.getText().toString().trim().isEmpty() ||
               !productCostPriceInput.getText().toString().trim().isEmpty() ||
               !productQuantityInput.getText().toString().trim().isEmpty() ||
               !productMinQuantityInput.getText().toString().trim().isEmpty();
    }

    private void clearAllFields() {
        productNameInput.setText("");
        productCategoryInput.setText("");
        productBarcodeInput.setText("");
        productSellingPriceInput.setText("");
        productCostPriceInput.setText("");
        productQuantityInput.setText("");
        productMinQuantityInput.setText("");
        imageUrl = "";
        selectedImageUri = null;
    }
} 