package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.InvoiceItem;

import java.util.List;

public class InvoicePrintItemAdapter extends RecyclerView.Adapter<InvoicePrintItemAdapter.InvoiceItemViewHolder> {
    private List<InvoiceItem> invoiceItems;

    public InvoicePrintItemAdapter(List<InvoiceItem> invoiceItems) {
        this.invoiceItems = invoiceItems;
    }

    @NonNull
    @Override
    public InvoiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_print, parent, false);
        return new InvoiceItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceItemViewHolder holder, int position) {
        if (invoiceItems != null && position < invoiceItems.size()) {
            InvoiceItem item = invoiceItems.get(position);
            if (item != null) {
                holder.bind(item);
            }
        }
    }

    @Override
    public int getItemCount() {
        return invoiceItems != null ? invoiceItems.size() : 0;
    }
    
    public void updateData(List<InvoiceItem> newItems) {
        if (newItems != null) {
            this.invoiceItems = newItems;
            notifyDataSetChanged();
        }
    }

    static class InvoiceItemViewHolder extends RecyclerView.ViewHolder {
        private TextView productNameTextView;
        private TextView quantityTextView;
        private TextView priceTextView;
        private TextView totalTextView;

        public InvoiceItemViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(R.id.tv_product_name);
            quantityTextView = itemView.findViewById(R.id.tv_quantity);
            priceTextView = itemView.findViewById(R.id.tv_price);
            totalTextView = itemView.findViewById(R.id.tv_total);
        }

        public void bind(InvoiceItem item) {
            productNameTextView.setText(item.getProductName() != null ? item.getProductName() : "منتج غير محدد");
            
            // التأكد من أن القيم ليست null قبل التنسيق
            int quantity = item.getQuantity();
            double price = item.getPrice();
            double total = quantity * price;
            
            quantityTextView.setText(String.valueOf(quantity));
            priceTextView.setText(String.format("%.2f", price));
            totalTextView.setText(String.format("%.2f", total));
        }
    }
} 