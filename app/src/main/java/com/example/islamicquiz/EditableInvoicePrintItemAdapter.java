package com.example.islamicquiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.islamicquiz.model.InvoiceItem;

import java.util.List;

public class EditableInvoicePrintItemAdapter extends RecyclerView.Adapter<EditableInvoicePrintItemAdapter.EditableInvoiceItemViewHolder> {
    private List<InvoiceItem> invoiceItems;
    private OnItemEditListener onItemEditListener;
    private OnItemDeleteListener onItemDeleteListener;

    public interface OnItemEditListener {
        void onItemEdit(InvoiceItem item, int position);
    }
    
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    public EditableInvoicePrintItemAdapter(List<InvoiceItem> invoiceItems) {
        this.invoiceItems = invoiceItems;
    }

    public void setOnItemEditListener(OnItemEditListener listener) {
        this.onItemEditListener = listener;
    }
    
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onItemDeleteListener = listener;
    }

    @NonNull
    @Override
    public EditableInvoiceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_print_editable, parent, false);
        return new EditableInvoiceItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EditableInvoiceItemViewHolder holder, int position) {
        if (invoiceItems != null && position < invoiceItems.size()) {
            InvoiceItem item = invoiceItems.get(position);
            if (item != null) {
                holder.bind(item, position);
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

    class EditableInvoiceItemViewHolder extends RecyclerView.ViewHolder {
        private TextView productNameTextView;
        private TextView quantityTextView;
        private TextView priceTextView;
        private TextView totalTextView;
        private TextView editHintTextView;
        private ImageButton deleteButton;

        public EditableInvoiceItemViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(R.id.tv_product_name);
            quantityTextView = itemView.findViewById(R.id.tv_quantity);
            priceTextView = itemView.findViewById(R.id.tv_price);
            totalTextView = itemView.findViewById(R.id.tv_total);
            editHintTextView = itemView.findViewById(R.id.tv_edit_hint);
            deleteButton = itemView.findViewById(R.id.btn_delete_item);
        }

        public void bind(InvoiceItem item, int position) {
            productNameTextView.setText(item.getProductName() != null ? item.getProductName() : "منتج غير محدد");
            
            // التأكد من أن القيم ليست null قبل التنسيق
            int quantity = item.getQuantity();
            double price = item.getPrice();
            double total = quantity * price;
            
            quantityTextView.setText(String.valueOf(quantity));
            priceTextView.setText(String.format(java.util.Locale.US, "%.2f", price));
            totalTextView.setText(String.format(java.util.Locale.US, "%.2f", total));
            
            // إظهار تلميح التعديل
            editHintTextView.setVisibility(View.VISIBLE);
            editHintTextView.setText(itemView.getContext().getString(R.string.click_to_edit));

            // إضافة مستمع النقر للتعديل
            itemView.setOnClickListener(v -> {
                if (onItemEditListener != null) {
                    onItemEditListener.onItemEdit(item, position);
                }
            });
            
            // إضافة مستمع النقر لزر الحذف
            deleteButton.setOnClickListener(v -> {
                if (onItemDeleteListener != null) {
                    onItemDeleteListener.onItemDelete(position);
                }
            });

            // تمييز البند كقابل للنقر
            itemView.setBackground(itemView.getContext().getDrawable(R.drawable.search_background));
            itemView.setClickable(true);
            itemView.setFocusable(true);
        }
    }
} 