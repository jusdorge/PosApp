package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Customer;

import java.util.List;

public class CustomerSearchAdapter extends RecyclerView.Adapter<CustomerSearchAdapter.CustomerViewHolder> {
    private List<Customer> customers;
    private OnCustomerSelectListener selectListener;

    public interface OnCustomerSelectListener {
        void onCustomerSelected(Customer customer);
    }

    public CustomerSearchAdapter(List<Customer> customers) {
        this.customers = customers;
    }

    public void setOnCustomerSelectListener(OnCustomerSelectListener listener) {
        this.selectListener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_search, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customers.get(position);
        holder.bind(customer, position);
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView customerNameTextView;
        TextView customerPhoneTextView;
        TextView customerDebtTextView;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            customerNameTextView = itemView.findViewById(R.id.customerNameTextView);
            customerPhoneTextView = itemView.findViewById(R.id.customerPhoneTextView);
            customerDebtTextView = itemView.findViewById(R.id.customerDebtTextView);
        }

        void bind(Customer customer, int position) {
            customerNameTextView.setText(customer.getName());
            customerPhoneTextView.setText(customer.getPhone());

            double totalDebt = customer.getTotalDebt();
            if (totalDebt > 0) {
                customerDebtTextView.setVisibility(View.VISIBLE);
                customerDebtTextView.setText(String.format("دين: %.2f دج", totalDebt));
            } else {
                customerDebtTextView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (selectListener != null) {
                    selectListener.onCustomerSelected(customer);
                }
            });
        }
    }
}