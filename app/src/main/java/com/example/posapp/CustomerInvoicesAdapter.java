package com.example.posapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Invoice;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerInvoicesAdapter extends RecyclerView.Adapter<CustomerInvoicesAdapter.InvoiceViewHolder> {

    private List<Invoice> invoices;
    private Context context;
    private OnInvoiceClickListener clickListener;

    public interface OnInvoiceClickListener {
        void onInvoiceClick(Invoice invoice);
    }

    public CustomerInvoicesAdapter(Context context, List<Invoice> invoices) {
        this.context = context;
        this.invoices = invoices;
    }

    public void setOnInvoiceClickListener(OnInvoiceClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer_invoice, parent, false);
        return new InvoiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        Invoice invoice = invoices.get(position);
        holder.bind(invoice);
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    class InvoiceViewHolder extends RecyclerView.ViewHolder {
        TextView invoiceIdTextView;
        TextView invoiceDateTextView;
        TextView invoiceItemsCountTextView;
        TextView invoiceAmountTextView;
        TextView invoiceStatusTextView;

        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            invoiceIdTextView = itemView.findViewById(R.id.invoiceIdTextView);
            invoiceDateTextView = itemView.findViewById(R.id.invoiceDateTextView);
            invoiceItemsCountTextView = itemView.findViewById(R.id.invoiceItemsCountTextView);
            invoiceAmountTextView = itemView.findViewById(R.id.invoiceAmountTextView);
            invoiceStatusTextView = itemView.findViewById(R.id.invoiceStatusTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onInvoiceClick(invoices.get(position));
                }
            });
        }

        void bind(Invoice invoice) {
            // تنسيق رقم الفاتورة
            invoiceIdTextView.setText(String.format("فاتورة #%s", invoice.getId().substring(0, 8)));
            
            // تنسيق التاريخ
            Timestamp timestamp = invoice.getDate();
            Date date = timestamp != null ? timestamp.toDate() : new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            invoiceDateTextView.setText(sdf.format(date));
            
            // عدد العناصر
            int itemsCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
            invoiceItemsCountTextView.setText(String.format("%d منتجات", itemsCount));
            
            // المبلغ الإجمالي
            invoiceAmountTextView.setText(String.format("%.2f دج", invoice.getTotalAmount()));
            
            // حالة الدفع
            if (invoice.isPaid()) {
                invoiceStatusTextView.setText("مدفوعة");
                invoiceStatusTextView.setBackgroundResource(R.drawable.payment_background);
            } else {
                invoiceStatusTextView.setText("غير مدفوعة");
                invoiceStatusTextView.setBackgroundResource(R.drawable.debt_background);
            }
        }
    }
} 