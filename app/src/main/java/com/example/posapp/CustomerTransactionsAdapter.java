package com.example.posapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.CustomerDebt;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerTransactionsAdapter extends RecyclerView.Adapter<CustomerTransactionsAdapter.TransactionViewHolder> {

    private List<CustomerDebt> transactions;
    private Context context;

    public CustomerTransactionsAdapter(Context context, List<CustomerDebt> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        CustomerDebt transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionTextView;
        TextView dateTextView;
        TextView amountTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            descriptionTextView = itemView.findViewById(R.id.transactionDescriptionTextView);
            dateTextView = itemView.findViewById(R.id.transactionDateTextView);
            amountTextView = itemView.findViewById(R.id.transactionAmountTextView);
        }

        void bind(CustomerDebt transaction) {
            descriptionTextView.setText(transaction.getDescription());
            
            // تنسيق التاريخ
            Timestamp timestamp = transaction.getDate();
            Date date = timestamp != null ? timestamp.toDate() : new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            dateTextView.setText(sdf.format(date));
            
            // تنسيق المبلغ والألوان
            if (transaction.isPayment()) {
                // المدفوعات باللون الأخضر
                amountTextView.setText(String.format("+ %.2f دج", transaction.getAmount()));
                amountTextView.setBackgroundResource(R.drawable.payment_background);
            } else {
                // الديون باللون الأحمر
                amountTextView.setText(String.format("- %.2f دج", transaction.getAmount()));
                amountTextView.setBackgroundResource(R.drawable.debt_background);
            }
        }
    }
} 