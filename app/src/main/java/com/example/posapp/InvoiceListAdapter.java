package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public interface OnInvoiceClickListener {
        void onInvoiceClick(Invoice invoice, int position);
    }

    public InvoiceListAdapter(List<Invoice> invoices) {
        this.invoices = invoices;
    }

    public void setOnInvoiceClickListener(OnInvoiceClickListener listener) {
        this.clickListener = listener;
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

        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            invoiceTimeTextView = itemView.findViewById(R.id.invoiceTimeTextView);
            invoiceIdTextView = itemView.findViewById(R.id.invoiceIdTextView);
            invoiceStatusTextView = itemView.findViewById(R.id.invoiceStatusTextView);
            customerNameTextView = itemView.findViewById(R.id.customerNameTextView);
            customerPhoneTextView = itemView.findViewById(R.id.customerPhoneTextView);
            invoiceTotalTextView = itemView.findViewById(R.id.invoiceTotalTextView);
            invoiceItemsCountTextView = itemView.findViewById(R.id.invoiceItemsCountTextView);
        }

        void bind(Invoice invoice, int position) {
            // تنسيق الوقت
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String timeString = timeFormat.format(invoice.getDate().toDate());
            invoiceTimeTextView.setText(timeString);
            
            // رقم الفاتورة
            String invoiceId = invoice.getId().substring(0, 6).toUpperCase(); // اختصار لرقم الفاتورة
            invoiceIdTextView.setText("#" + invoiceId);
            
            // حالة الفاتورة
            if (invoice.isPaid()) {
                invoiceStatusTextView.setText("مدفوعة");
                invoiceStatusTextView.setTextColor(itemView.getContext().getResources().getColor(R.color.colorPrimary));
            } else {
                invoiceStatusTextView.setText("دين");
                invoiceStatusTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            }
            
            // بيانات العميل
            customerNameTextView.setText(invoice.getCustomerName());
            customerPhoneTextView.setText(invoice.getCustomerPhone());
            
            // المجموع وعدد المنتجات
            invoiceTotalTextView.setText(String.format("%.2f ريال", invoice.getTotalAmount()));
            
            int itemsCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
            invoiceItemsCountTextView.setText(itemsCount + " منتجات");
            
            // مستمع النقر
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onInvoiceClick(invoice, position);
                }
            });
        }
    }
} 