package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Product;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {
    private List<Product> inventoryList;
    private OnStockAdjustmentListener adjustmentListener;

    public interface OnStockAdjustmentListener {
        void onStockAdjustment(Product product);
    }

    public InventoryAdapter(List<Product> inventoryList) {
        this.inventoryList = inventoryList;
    }

    public void setOnStockAdjustmentListener(OnStockAdjustmentListener listener) {
        this.adjustmentListener = listener;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Product product = inventoryList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return inventoryList.size();
    }

    class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView productNameTextView;
        TextView categoryTextView;
        TextView quantityTextView;
        TextView minQuantityTextView;
        TextView costPriceTextView;
        TextView stockValueTextView;
        ImageView stockStatusImageView;
        Button adjustStockButton;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            minQuantityTextView = itemView.findViewById(R.id.minQuantityTextView);
            costPriceTextView = itemView.findViewById(R.id.costPriceTextView);
            stockValueTextView = itemView.findViewById(R.id.stockValueTextView);
            stockStatusImageView = itemView.findViewById(R.id.stockStatusImageView);
            adjustStockButton = itemView.findViewById(R.id.adjustStockButton);
        }

        void bind(Product product) {
            productNameTextView.setText(product.getName());
            categoryTextView.setText(product.getCategory());
            quantityTextView.setText("الكمية: " + product.getQuantity());
            minQuantityTextView.setText("الحد الأدنى: " + product.getMinQuantity());
            costPriceTextView.setText("سعر التكلفة: " + product.getCostPrice() + " دج");
            
            // حساب قيمة المخزون
            double stockValue = product.getQuantity() * product.getCostPrice();
            stockValueTextView.setText("قيمة المخزون: " + String.format("%.2f", stockValue) + " دج");

            // تعيين حالة المخزون
            if (product.isLowStock()) {
                stockStatusImageView.setImageResource(R.drawable.ic_menu_remove);
                stockStatusImageView.setColorFilter(android.graphics.Color.RED);
                quantityTextView.setTextColor(android.graphics.Color.RED);
            } else {
                stockStatusImageView.setImageResource(R.drawable.ic_menu_add);
                stockStatusImageView.setColorFilter(android.graphics.Color.GREEN);
                quantityTextView.setTextColor(android.graphics.Color.BLACK);
            }

            // إعداد مستمع زر تعديل المخزون
            adjustStockButton.setOnClickListener(v -> {
                if (adjustmentListener != null) {
                    adjustmentListener.onStockAdjustment(product);
                }
            });
        }
    }
} 