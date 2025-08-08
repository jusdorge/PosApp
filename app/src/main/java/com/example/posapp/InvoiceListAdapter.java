package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Invoice;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class InvoiceListAdapter extends RecyclerView.Adapter<InvoiceListAdapter.InvoiceViewHolder> {
    private List<Invoice> invoices;
    private OnInvoiceClickListener clickListener;
    private OnInvoiceButtonClickListener buttonClickListener;

    public interface OnInvoiceClickListener {
        void onInvoiceClick(Invoice invoice, int position);
    }
    
    public interface OnInvoiceButtonClickListener {
        void onPrintClick(Invoice invoice, int position);
        void onLoadInCounterClick(Invoice invoice, int position);
        void onAddProductsClick(Invoice invoice, int position);
        void onCustomerLocationClick(Invoice invoice, int position);
        void onDeleteClick(Invoice invoice, int position);
    }

    public InvoiceListAdapter(List<Invoice> invoices) {
        this.invoices = invoices;
    }

    public void setOnInvoiceClickListener(OnInvoiceClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setOnInvoiceButtonClickListener(OnInvoiceButtonClickListener listener) {
        this.buttonClickListener = listener;
    }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_summary, parent, false);
        return new InvoiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        Invoice invoice = invoices.get(position);
        holder.bind(invoice, position);
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    public void updateInvoices(List<Invoice> newInvoices) {
        this.invoices = newInvoices;
        notifyDataSetChanged();
    }

    class InvoiceViewHolder extends RecyclerView.ViewHolder {
        TextView invoiceTimeTextView;
        TextView invoiceIdTextView;
        TextView invoiceStatusTextView;
        TextView customerNameTextView;
        TextView customerPhoneTextView;
        TextView invoiceTotalTextView;
        TextView invoiceItemsCountTextView;
        
        Button printInvoiceButton;
        Button loadInCounterButton;
        Button addProductsButton;
        Button customerLocationButton;
        Button deleteInvoiceButton;

        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            invoiceTimeTextView = itemView.findViewById(R.id.invoiceTimeTextView);
            invoiceIdTextView = itemView.findViewById(R.id.invoiceIdTextView);
            invoiceStatusTextView = itemView.findViewById(R.id.invoiceStatusTextView);
            customerNameTextView = itemView.findViewById(R.id.customerNameTextView);
            customerPhoneTextView = itemView.findViewById(R.id.customerPhoneTextView);
            invoiceTotalTextView = itemView.findViewById(R.id.invoiceTotalTextView);
            invoiceItemsCountTextView = itemView.findViewById(R.id.invoiceItemsCountTextView);
            
            printInvoiceButton = itemView.findViewById(R.id.printInvoiceButton);
            loadInCounterButton = itemView.findViewById(R.id.loadInCounterButton);
            addProductsButton = itemView.findViewById(R.id.addProductsButton);
            customerLocationButton = itemView.findViewById(R.id.customerLocationButton);
            deleteInvoiceButton = itemView.findViewById(R.id.deleteInvoiceButton);
        }

        void bind(Invoice invoice, int position) {
            // تنسيق الوقت
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String timeString = timeFormat.format(invoice.getDate().toDate());
            invoiceTimeTextView.setText(timeString);
            
            // رقم الفاتورة
            invoiceIdTextView.setText("#" + invoice.getDisplayNumber());
            
            // حالة الفاتورة
            if (invoice.isPaid()) {
                invoiceStatusTextView.setText(context.getString(R.string.invoice_status_paid));
                invoiceStatusTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.colorPrimary));
            } else {
                invoiceStatusTextView.setText(context.getString(R.string.invoice_status_debt));
                invoiceStatusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            }
            
            // بيانات العميل
            customerNameTextView.setText(invoice.getCustomerName());
            customerPhoneTextView.setText(invoice.getCustomerPhone());
            
            // المجموع وعدد المنتجات
            invoiceTotalTextView.setText(CurrencyUtils.formatCurrency(invoice.getTotalAmount()));
            
            int itemsCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
            invoiceItemsCountTextView.setText(itemsCount + " منتجات");
            
            // مستمع النقر على الكارد (للتوافق مع الإصدار السابق)
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onInvoiceClick(invoice, position);
                }
            });
            
            // مستمعي النقر على الأزرار
            if (buttonClickListener != null) {
                printInvoiceButton.setOnClickListener(v -> 
                    buttonClickListener.onPrintClick(invoice, position));
                
                loadInCounterButton.setOnClickListener(v -> 
                    buttonClickListener.onLoadInCounterClick(invoice, position));
                
                addProductsButton.setOnClickListener(v -> 
                    buttonClickListener.onAddProductsClick(invoice, position));
                
                customerLocationButton.setOnClickListener(v -> 
                    buttonClickListener.onCustomerLocationClick(invoice, position));
                
                deleteInvoiceButton.setOnClickListener(v -> 
                    buttonClickListener.onDeleteClick(invoice, position));
            }
        }
    }
} 