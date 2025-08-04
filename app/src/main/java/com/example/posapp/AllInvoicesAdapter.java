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

public class AllInvoicesAdapter extends RecyclerView.Adapter<AllInvoicesAdapter.InvoiceViewHolder> {
    
    private List<Invoice> invoicesList;
    private OnInvoiceClickListener onInvoiceClickListener;
    private OnInvoiceButtonClickListener buttonClickListener;
    private SimpleDateFormat dateFormat;
    
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
    
    public AllInvoicesAdapter(List<Invoice> invoicesList) {
        this.invoicesList = invoicesList;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }
    
    public void setOnInvoiceClickListener(OnInvoiceClickListener listener) {
        this.onInvoiceClickListener = listener;
    }
    
    public void setOnInvoiceButtonClickListener(OnInvoiceButtonClickListener listener) {
        this.buttonClickListener = listener;
    }
    
    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invoice_all, parent, false);
        return new InvoiceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        Invoice invoice = invoicesList.get(position);
        holder.bind(invoice);
    }
    
    @Override
    public int getItemCount() {
        return invoicesList.size();
    }
    
    class InvoiceViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvInvoiceNumber;
        private TextView tvCustomerName;
        private TextView tvInvoiceDate;
        private TextView tvTotalAmount;
        private TextView tvPaymentStatus;
        private TextView tvItemsCount;
        
        private Button printInvoiceButton;
        private Button loadInCounterButton;
        private Button addProductsButton;
        private Button customerLocationButton;
        private Button deleteInvoiceButton;
        
        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInvoiceNumber = itemView.findViewById(R.id.tvInvoiceNumber);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvInvoiceDate = itemView.findViewById(R.id.tvInvoiceDate);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvItemsCount = itemView.findViewById(R.id.tvItemsCount);
            
            printInvoiceButton = itemView.findViewById(R.id.printInvoiceButton);
            loadInCounterButton = itemView.findViewById(R.id.loadInCounterButton);
            addProductsButton = itemView.findViewById(R.id.addProductsButton);
            customerLocationButton = itemView.findViewById(R.id.customerLocationButton);
            deleteInvoiceButton = itemView.findViewById(R.id.deleteInvoiceButton);
        }
        
        public void bind(Invoice invoice) {
            tvInvoiceNumber.setText("فاتورة #" + invoice.getDisplayNumber());
            tvCustomerName.setText("العميل: " + (invoice.getCustomerName() != null ? invoice.getCustomerName() : "غير محدد"));
            
            if (invoice.getDate() != null) {
                tvInvoiceDate.setText("التاريخ: " + dateFormat.format(invoice.getDate().toDate()));
            } else {
                tvInvoiceDate.setText("التاريخ: غير محدد");
            }
            
            tvTotalAmount.setText("المبلغ: " + CurrencyUtils.formatCurrency(invoice.getTotalAmount()));
            
            if (invoice.isPaid()) {
                tvPaymentStatus.setText("نقدي");
                tvPaymentStatus.setBackgroundResource(R.drawable.payment_background);
            } else {
                tvPaymentStatus.setText("آجل");
                tvPaymentStatus.setBackgroundResource(R.drawable.debt_background);
            }
            
            int itemsCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
            tvItemsCount.setText(itemsCount + " عنصر");
            
            itemView.setOnClickListener(v -> {
                if (onInvoiceClickListener != null) {
                    onInvoiceClickListener.onInvoiceClick(invoice, getAdapterPosition());
                }
            });
            
            // مستمعي النقر على الأزرار
            if (buttonClickListener != null) {
                printInvoiceButton.setOnClickListener(v -> 
                    buttonClickListener.onPrintClick(invoice, getAdapterPosition()));
                
                loadInCounterButton.setOnClickListener(v -> 
                    buttonClickListener.onLoadInCounterClick(invoice, getAdapterPosition()));
                
                addProductsButton.setOnClickListener(v -> 
                    buttonClickListener.onAddProductsClick(invoice, getAdapterPosition()));
                
                customerLocationButton.setOnClickListener(v -> 
                    buttonClickListener.onCustomerLocationClick(invoice, getAdapterPosition()));
                
                deleteInvoiceButton.setOnClickListener(v -> 
                    buttonClickListener.onDeleteClick(invoice, getAdapterPosition()));
            }
        }
    }
} 