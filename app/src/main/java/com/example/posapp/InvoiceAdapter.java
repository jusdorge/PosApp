package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.InvoiceItem;

import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder> {
    private List<InvoiceItem> invoiceItems;
    private OnInvoiceItemDeleteListener deleteListener;
    private OnInvoiceItemClickListener editListener;
    private boolean isDeletionEnabled;

    public interface OnInvoiceItemDeleteListener {
        void onInvoiceItemDelete(int position);
    }

    public interface OnInvoiceItemClickListener {
        void onInvoiceItemClick(InvoiceItem item, int position);
    }

    public InvoiceAdapter(List<InvoiceItem> invoiceItems, OnInvoiceItemDeleteListener deleteListener) {
        this.invoiceItems = invoiceItems;
        this.deleteListener = deleteListener;
        this.isDeletionEnabled = true; // افتراضياً يكون الحذف مفعل
    }
    
    public InvoiceAdapter(List<InvoiceItem> invoiceItems, OnInvoiceItemDeleteListener deleteListener, boolean isDeletionEnabled) {
        this.invoiceItems = invoiceItems;
        this.deleteListener = deleteListener;
        this.isDeletionEnabled = isDeletionEnabled;
    }

    public void setOnInvoiceItemClickListener(OnInvoiceItemClickListener listener) {
        this.editListener = listener;
    }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice, parent, false);
        return new InvoiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        InvoiceItem item = invoiceItems.get(position);
        holder.bindInvoiceItem(item, position);
    }

    @Override
    public int getItemCount() {
        return invoiceItems.size();
    }

    class InvoiceViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameText, itemPriceText, itemQuantityText, itemTotalText;
        ImageButton deleteButton;
        View itemView;

        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            itemNameText = itemView.findViewById(R.id.itemNameText);
            itemPriceText = itemView.findViewById(R.id.itemPriceText);
            itemQuantityText = itemView.findViewById(R.id.itemQuantityText);
            itemTotalText = itemView.findViewById(R.id.itemTotalText);
            deleteButton = itemView.findViewById(R.id.deleteItemButton);
        }

        void bindInvoiceItem(InvoiceItem item, int position) {
            itemNameText.setText(item.getProductName());
            itemPriceText.setText(CurrencyUtils.formatCurrency(item.getPrice()));
            itemQuantityText.setText(String.valueOf(item.getQuantity()));
            itemTotalText.setText(CurrencyUtils.formatCurrency(item.getTotal()));

            // إظهار أو إخفاء زر الحذف بناءً على الإعدادات
            if (isDeletionEnabled && deleteListener != null) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    deleteListener.onInvoiceItemDelete(position);
                });
            } else {
                deleteButton.setVisibility(View.GONE);
                deleteButton.setOnClickListener(null);
            }

            itemView.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onInvoiceItemClick(item, position);
                }
            });
        }
    }
} 