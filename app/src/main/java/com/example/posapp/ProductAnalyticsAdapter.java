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
        
        // Set product data (all products in the list are sold products)
        holder.productNameTextView.setText(data.getProductName());
        holder.quantityTextView.setText(holder.itemView.getContext().getString(R.string.quantity_sold_label, data.getQuantitySold()));
        holder.revenueTextView.setText(holder.itemView.getContext().getString(R.string.revenue_label, CurrencyUtils.formatCurrency(data.getTotalRevenue())));
        
        // Calculate and set percentage
        double percentage = data.getPercentage(totalQuantity);
        holder.percentageTextView.setText(String.format("%.1f%%", percentage));
        
        // Set progress bar
        holder.progressBar.setProgress((int) percentage);
        
        // Set rank indicator
        holder.rankTextView.setText("#" + (position + 1));
        
        // Set colors based on ranking (only sold products are shown)
        int color;
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
                color = Color.rgb(76, 175, 80); // Green for other sold products
                break;
        }
        
        holder.rankTextView.setTextColor(Color.WHITE);
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