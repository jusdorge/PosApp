package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Product;

import java.util.List;

public class InvoiceItemAdapter extends RecyclerView.Adapter<InvoiceItemAdapter.ProductViewHolder> {
    private List<Product> products;
    private OnProductAddListener productAddListener;

    public interface OnProductAddListener {
        void onProductAdd(Product product, int quantity);
    }

    public InvoiceItemAdapter(List<Product> products, OnProductAddListener listener) {
        this.products = products;
        this.productAddListener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bindProduct(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productNameText, productPriceText, productCategoryText;
        EditText quantityEdit;
        ImageButton decreaseQuantityButton, increaseQuantityButton;
        Button addToInvoiceButton;
        int quantity = 1;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameText = itemView.findViewById(R.id.productNameText);
            productPriceText = itemView.findViewById(R.id.productPriceText);
            productCategoryText = itemView.findViewById(R.id.productCategoryText);
            quantityEdit = itemView.findViewById(R.id.quantityEdit);
            decreaseQuantityButton = itemView.findViewById(R.id.decreaseQuantityButton);
            increaseQuantityButton = itemView.findViewById(R.id.increaseQuantityButton);
            addToInvoiceButton = itemView.findViewById(R.id.addToInvoiceButton);
        }

        void bindProduct(Product product) {
            productNameText.setText(product.getName());
            productPriceText.setText("السعر: " + product.getDefaultPrice() + " ريال");
            productCategoryText.setText(product.getCategory());
            
            // تنظيف الحقول وإعادة تعيين القيم الافتراضية
            quantity = 1;
            quantityEdit.setText("1");
            
            decreaseQuantityButton.setOnClickListener(v -> {
                if (quantity > 1) {
                    quantity--;
                    quantityEdit.setText(String.valueOf(quantity));
                }
            });

            increaseQuantityButton.setOnClickListener(v -> {
                if (quantity < product.getQuantity()) {
                    quantity++;
                    quantityEdit.setText(String.valueOf(quantity));
                } else {
                    Toast.makeText(itemView.getContext(), "لا توجد كمية كافية في المخزون", Toast.LENGTH_SHORT).show();
                }
            });

            quantityEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        int newQuantity = Integer.parseInt(quantityEdit.getText().toString());
                        if (newQuantity > 0 && newQuantity <= product.getQuantity()) {
                            quantity = newQuantity;
                        } else if (newQuantity > product.getQuantity()) {
                            quantity = product.getQuantity();
                            quantityEdit.setText(String.valueOf(quantity));
                            Toast.makeText(itemView.getContext(), "تم تعديل الكمية للحد الأقصى المتاح", Toast.LENGTH_SHORT).show();
                        } else {
                            quantity = 1;
                            quantityEdit.setText("1");
                        }
                    } catch (NumberFormatException e) {
                        quantity = 1;
                        quantityEdit.setText("1");
                    }
                }
            });

            addToInvoiceButton.setOnClickListener(v -> {
                if (productAddListener != null) {
                    try {
                        int newQuantity = Integer.parseInt(quantityEdit.getText().toString());
                        if (newQuantity > 0 && newQuantity <= product.getQuantity()) {
                            productAddListener.onProductAdd(product, newQuantity);
                        } else {
                            Toast.makeText(itemView.getContext(), "الكمية غير صالحة", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(itemView.getContext(), "الرجاء إدخال كمية صحيحة", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
} 