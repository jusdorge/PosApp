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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddProductActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private String imageUrl; // رابط الصورة
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
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "اختر صورة"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            imageUrl = imageUri.toString(); // حفظ رابط الصورة
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

            Product product = new Product(
                UUID.randomUUID().toString(),
                name,
                category,
                sellingPrice,
                costPrice,
                quantity,
                minQuantity,
                barcode,
                imageUrl
            );

            db.collection("products")
                .document(product.getId())
                .set(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تمت إضافة المنتج بنجاح", Toast.LENGTH_SHORT).show();
                    clearInputs();
                    loadProducts(); // Reload products after adding
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في إضافة المنتج", Toast.LENGTH_SHORT).show();
                });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "يرجى إدخال قيم صحيحة للأرقام", Toast.LENGTH_SHORT).show();
        }
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
                    .setPositiveButton("خروج", (dialog, which) -> finish())
                    .setNegativeButton("إلغاء", null)
                    .show();
        } else {
            finish();
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
} 