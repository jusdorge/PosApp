package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductManagementAdapter extends RecyclerView.Adapter<ProductManagementAdapter.ProductViewHolder> {
    private List<Product> productList;
    private List<Product> filteredList;
    private OnProductEditListener editListener;

    public interface OnProductEditListener {
        void onProductEdit(Product product);
    }

    public ProductManagementAdapter(List<Product> productList) {
        this.productList = productList;
        this.filteredList = new ArrayList<>(productList);
    }

    public void setOnProductEditListener(OnProductEditListener listener) {
        this.editListener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_management, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = filteredList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void updateProducts(List<Product> newProducts) {
        this.productList = newProducts;
        this.filteredList = new ArrayList<>(newProducts);
        notifyDataSetChanged();
    }

    public void filterProducts(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(productList);
        } else {

            for (Product product : productList) {
                if (product.getName().contains(query) ||
                    //product.getBarcode().contains(query) ||
                    (product.getCategory() != null && product.getCategory().contains(query))) {
                    filteredList.add(product);
                }
            }
        }
        notifyDataSetChanged();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productNameTextView;
        TextView productPriceTextView;
        TextView productQuantityTextView;
        TextView productBarcodeTextView;
        ImageButton editProductButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            productPriceTextView = itemView.findViewById(R.id.productPriceTextView);
            productQuantityTextView = itemView.findViewById(R.id.productQuantityTextView);
            productBarcodeTextView = itemView.findViewById(R.id.productBarcodeTextView);
            editProductButton = itemView.findViewById(R.id.editProductButton);
        }

        void bind(Product product) {
            productNameTextView.setText(product.getName());
            productPriceTextView.setText(String.format("%.2f دج", product.getCostPrice()));
            productQuantityTextView.setText(String.valueOf(product.getQuantity()));
            productBarcodeTextView.setText(product.getBarcode());

            editProductButton.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onProductEdit(product);
                }
            });

            itemView.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onProductEdit(product);
                }
            });
        }
    }
} 