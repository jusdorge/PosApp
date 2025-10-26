package com.example.islamicquiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.islamicquiz.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {
    private List<Customer> customerList;
    private List<Customer> filteredList;
    private OnCustomerClickListener clickListener;
    private OnQRCodeClickListener qrCodeClickListener;
    private OnEditCustomerClickListener editClickListener;
    private OnDeleteCustomerClickListener deleteClickListener;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer, int position);
    }

    public interface OnQRCodeClickListener {
        void onQRCodeClick(Customer customer);
    }
    
    public interface OnEditCustomerClickListener {
        void onEditCustomerClick(Customer customer, int position);
    }
    
    public interface OnDeleteCustomerClickListener {
        void onDeleteCustomerClick(Customer customer, int position);
    }

    public CustomerAdapter(List<Customer> customerList) {
        this.customerList = customerList;
        this.filteredList = new ArrayList<>(customerList);
    }

    public void setOnCustomerClickListener(OnCustomerClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnQRCodeClickListener(OnQRCodeClickListener listener) {
        this.qrCodeClickListener = listener;
    }
    
    public void setOnEditCustomerClickListener(OnEditCustomerClickListener listener) {
        this.editClickListener = listener;
    }
    
    public void setOnDeleteCustomerClickListener(OnDeleteCustomerClickListener listener) {
        this.deleteClickListener = listener;
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
        TextView customerEmailTextView;
        TextView customerAddressTextView;
        TextView customerDebtTextView;
        Button qrCodeButton;
        Button editCustomerButton;
        Button deleteCustomerButton;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            customerNameTextView = itemView.findViewById(R.id.customerNameTextView);
            customerPhoneTextView = itemView.findViewById(R.id.customerPhoneTextView);
            customerEmailTextView = itemView.findViewById(R.id.customerEmailTextView);
            customerAddressTextView = itemView.findViewById(R.id.customerAddressTextView);
            customerDebtTextView = itemView.findViewById(R.id.customerDebtTextView);
            qrCodeButton = itemView.findViewById(R.id.qrCodeButton);
            editCustomerButton = itemView.findViewById(R.id.editCustomerButton);
            deleteCustomerButton = itemView.findViewById(R.id.deleteCustomerButton);
        }

        void bind(Customer customer, int position) {
            customerNameTextView.setText(customer.getName());
            customerPhoneTextView.setText(customer.getPhone());
            
            // عرض الإيميل إن وجد
            if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
                customerEmailTextView.setVisibility(View.VISIBLE);
                customerEmailTextView.setText("📧 " + customer.getEmail());
            } else {
                customerEmailTextView.setVisibility(View.GONE);
            }
            
            // عرض العنوان إن وجد
            if (customer.getAddress() != null && !customer.getAddress().trim().isEmpty()) {
                customerAddressTextView.setVisibility(View.VISIBLE);
                customerAddressTextView.setText("📍 " + customer.getAddress());
            } else {
                customerAddressTextView.setVisibility(View.GONE);
            }
            
            // عرض الدين
            double totalDebt = customer.getTotalDebt();
            if (totalDebt > 0) {
                customerDebtTextView.setVisibility(View.VISIBLE);
                customerDebtTextView.setText(CurrencyUtils.formatCurrency(totalDebt));
            } else {
                customerDebtTextView.setVisibility(View.GONE);
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onCustomerClick(customer, position);
                }
            });

            qrCodeButton.setOnClickListener(v -> {
                if (qrCodeClickListener != null) {
                    qrCodeClickListener.onQRCodeClick(customer);
                }
            });
            
            editCustomerButton.setOnClickListener(v -> {
                if (editClickListener != null) {
                    editClickListener.onEditCustomerClick(customer, position);
                }
            });
            
            deleteCustomerButton.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteCustomerClick(customer, position);
                }
            });
        }
    }
} 