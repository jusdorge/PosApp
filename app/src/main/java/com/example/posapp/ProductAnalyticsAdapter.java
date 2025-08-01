package com.example.posapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAnalyticsAdapter extends RecyclerView.Adapter<ProductAnalyticsAdapter.ViewHolder> {
    private List<ProductsAnalyticsActivity.ProductAnalyticsData> dataList;
    
    public ProductAnalyticsAdapter(List<ProductsAnalyticsActivity.ProductAnalyticsData> dataList) {
        this.dataList = dataList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_analytics, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductsAnalyticsActivity.ProductAnalyticsData data = dataList.get(position);
        
        // Calculate total quantity for percentage
        int totalQuantity = 0;
        for (ProductsAnalyticsActivity.ProductAnalyticsData item : dataList) {
            totalQuantity += item.getQuantitySold();
        }
        
        // Set product data
        holder.productNameTextView.setText(data.getProductName());
        
        if (data.getQuantitySold() > 0) {
            holder.quantityTextView.setText("الكمية: " + data.getQuantitySold());
            holder.revenueTextView.setText("الإيرادات: " + CurrencyUtils.formatCurrency(data.getTotalRevenue()));
            
            // Calculate and set percentage
            double percentage = data.getPercentage(totalQuantity);
            holder.percentageTextView.setText(String.format("%.1f%%", percentage));
            
            // Set progress bar
            holder.progressBar.setProgress((int) percentage);
        } else {
            // Product not sold today
            holder.quantityTextView.setText("لم يُباع اليوم");
            holder.revenueTextView.setText("الإيرادات: " + CurrencyUtils.formatCurrency(0.0));
            holder.percentageTextView.setText("0.0%");
            holder.progressBar.setProgress(0);
            
            // Gray out the text for unsold products
            holder.quantityTextView.setTextColor(android.graphics.Color.GRAY);
            holder.revenueTextView.setTextColor(android.graphics.Color.GRAY);
        }
        
        // Set rank indicator
        holder.rankTextView.setText("#" + (position + 1));
        
        // Set colors based on rank and sales status
        int color;
        int rankColor = Color.WHITE;
        
        if (data.getQuantitySold() > 0) {
            // Products with sales - use ranking colors
            switch (position) {
                case 0: // Gold - أول منتج مباع
                    color = Color.rgb(255, 215, 0);
                    break;
                case 1: // Silver - ثاني منتج مباع
                    color = Color.rgb(192, 192, 192);
                    break;
                case 2: // Bronze - ثالث منتج مباع
                    color = Color.rgb(205, 127, 50);
                    break;
                default:
                    color = Color.rgb(76, 175, 80); // Green for sold products
                    break;
            }
            rankColor = Color.WHITE;
        } else {
            // Products not sold - use gray
            color = Color.rgb(158, 158, 158);
            rankColor = Color.DKGRAY;
            holder.rankTextView.setText("⭕");
        }
        
        holder.rankTextView.setTextColor(rankColor);
        holder.progressBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        
        // Set rank background color
        if (holder.rankTextView.getBackground() != null) {
            holder.rankTextView.getBackground().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }
    
    @Override
    public int getItemCount() {
        return dataList.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView productNameTextView;
        public TextView quantityTextView;
        public TextView revenueTextView;
        public TextView percentageTextView;
        public TextView rankTextView;
        public ProgressBar progressBar;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            revenueTextView = itemView.findViewById(R.id.revenueTextView);
            percentageTextView = itemView.findViewById(R.id.percentageTextView);
            rankTextView = itemView.findViewById(R.id.rankTextView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}