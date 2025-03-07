package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {
    private List<Customer> customerList;
    private List<Customer> filteredList;
    private OnCustomerClickListener clickListener;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer, int position);
    }

    public CustomerAdapter(List<Customer> customerList) {
        this.customerList = customerList;
        this.filteredList = new ArrayList<>(customerList);
    }

    public void setOnCustomerClickListener(OnCustomerClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = filteredList.get(position);
        holder.bind(customer, position);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void updateCustomers(List<Customer> newCustomers) {
        this.customerList = newCustomers;
        this.filteredList = new ArrayList<>(newCustomers);
        notifyDataSetChanged();
    }

    public void filterCustomers(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(customerList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Customer customer : customerList) {
                if (customer.getName().toLowerCase().contains(lowerCaseQuery) ||
                    customer.getPhone().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(customer);
                }
            }
        }
        notifyDataSetChanged();
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
                customerDebtTextView.setText(String.format("%.2f دج", totalDebt));
            } else {
                customerDebtTextView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCustomerClick(customer, position);
                }
            });
        }
    }
} 