package com.example.posapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.Customer;
import com.example.posapp.model.InvoiceItem;

import java.util.ArrayList;
import java.util.List;

public class CounterFragment extends Fragment implements InvoiceAdapter.OnInvoiceItemDeleteListener, EditInvoiceItemDialog.OnItemUpdatedListener {
    private static TextView invoiceCustomerTextView;
    private RecyclerView invoiceItemsRecyclerView;
    private TextView totalPriceTextView;
    private Button checkoutButton;
    private InvoiceAdapter invoiceAdapter;
    private static List<InvoiceItem> invoiceItems = new ArrayList<>();
    private double totalPrice = 0.0;

    private static Customer currentCustomer;

    public static CounterFragment activeInstance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_counter, container, false);

        // Debug log
        android.util.Log.d("CounterFragment", "onCreateView called");
        
        activeInstance = this;
        // ربط العناصر من واجهة المستخدم
        invoiceItemsRecyclerView = view.findViewById(R.id.invoiceItemsRecyclerView);
        totalPriceTextView = view.findViewById(R.id.totalPriceTextView);
        checkoutButton = view.findViewById(R.id.checkoutButton);
        invoiceCustomerTextView = view.findViewById(R.id.invoiceCustomerTextView);
        if (currentCustomer != null) {
            invoiceCustomerTextView.setText("الزبون: " + currentCustomer.getName());
        }

        // إعداد محول الفاتورة
        invoiceAdapter = new InvoiceAdapter(invoiceItems, this);
        
        // تعيين مستمع للنقر على عناصر الفاتورة لتعديلها
        invoiceAdapter.setOnInvoiceItemClickListener((item, position) -> {
            EditInvoiceItemDialog dialog = EditInvoiceItemDialog.newInstance(item, position);
            dialog.setOnItemUpdatedListener(this);
            dialog.show(getChildFragmentManager(), "EditInvoiceItemDialog");
        });
        
        invoiceItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        invoiceItemsRecyclerView.setAdapter(invoiceAdapter);

        // إعداد مستمع زر الدفع
        checkoutButton.setOnClickListener(v -> {
            if (invoiceItems.isEmpty()) {
                Toast.makeText(getContext(), "الفاتورة فارغة", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // إظهار نافذة الدفع
            CheckoutDialog dialog;
            
            // Debug log
            android.util.Log.d("CounterFragment", "Creating CheckoutDialog with customer: " + 
                (currentCustomer != null ? currentCustomer.getName() : "null"));
            
            if (currentCustomer != null) {
                dialog = CheckoutDialog.newInstance(new ArrayList<>(invoiceItems), totalPrice, currentCustomer);
            } else {
                dialog = CheckoutDialog.newInstance(new ArrayList<>(invoiceItems), totalPrice);
            }
            
            // Debug log
            android.util.Log.d("CounterFragment", "CheckoutDialog created");
            dialog.setOnInvoiceCompletedListener(() -> {
                // مسح الفاتورة بعد الدفع
                invoiceItems.clear();
                invoiceAdapter.notifyDataSetChanged();
                updateTotalPrice();
            });
            dialog.show(getChildFragmentManager(), "CheckoutDialog");
            
            // Debug log
            android.util.Log.d("CounterFragment", "CheckoutDialog shown");
        });

        // تحديث إجمالي السعر
        updateTotalPrice();

        // Debug log
        android.util.Log.d("CounterFragment", "onCreateView finished");
        
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Debug log
        android.util.Log.d("CounterFragment", "onDestroy called");
        
        if (activeInstance == this) {
            activeInstance = null;
        }
        
        // Debug log
        android.util.Log.d("CounterFragment", "onDestroy finished");
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Debug log
        android.util.Log.d("CounterFragment", "onResume called");
        
        // تحديث قائمة الفاتورة وإجمالي السعر عند العودة إلى الشاشة
        if (invoiceAdapter != null) {
            invoiceAdapter.notifyDataSetChanged();
            updateTotalPrice();
        }
        
        // تحديث عرض العميل
        if (invoiceCustomerTextView != null) {
            if (currentCustomer != null) {
                invoiceCustomerTextView.setText("الزبون: " + currentCustomer.getName());
            } else {
                invoiceCustomerTextView.setText("الزبون: مجهول");
            }
        }
        
        // Debug log
        android.util.Log.d("CounterFragment", "onResume finished");
    }

    @Override
    public void onInvoiceItemDelete(int position) {
        if (position >= 0 && position < invoiceItems.size()) {
            invoiceItems.remove(position);
            invoiceAdapter.notifyDataSetChanged();
            updateTotalPrice();
        }
    }

    @Override
    public void onItemUpdated(InvoiceItem item, int position) {
        if (position >= 0 && position < invoiceItems.size()) {
            // تحديث عنصر الفاتورة بالقيم الجديدة
            InvoiceItem currentItem = invoiceItems.get(position);
            currentItem.setPrice(item.getPrice());
            currentItem.setQuantity(item.getQuantity());
            
            invoiceAdapter.notifyItemChanged(position);
            updateTotalPrice();
            
            Toast.makeText(getContext(), "تم تحديث المنتج بنجاح", Toast.LENGTH_SHORT).show();
        }
    }

    // طريقة عامة لإضافة عنصر إلى الفاتورة
    public static void addToInvoice(InvoiceItem item) {
        // التحقق مما إذا كان المنتج موجودًا بالفعل في الفاتورة
        boolean found = false;
        for (InvoiceItem invoiceItem : invoiceItems) {
            if (invoiceItem.getProductId().equals(item.getProductId())) {
                // زيادة الكمية إذا كان المنتج موجوداً بالفعل
                invoiceItem.setQuantity(invoiceItem.getQuantity() + item.getQuantity());
                found = true;
                break;
            }
        }
        
        // إذا لم يكن المنتج موجودًا، أضفه
        if (!found) {
            invoiceItems.add(item);
        }
        // تحديث واجهة المستخدم إذا كان الـ Fragment نشطًا
        if (activeInstance != null) {
            activeInstance.invoiceAdapter.notifyDataSetChanged();
            activeInstance.updateTotalPrice();
        }
    }
    public static void setCustomer(Customer customer) {
        currentCustomer = customer;
        
        // Debug log
        android.util.Log.d("CounterFragment", "Setting customer: " + 
            (customer != null ? customer.getName() : "null"));
        
        // تحديث TextView إذا كان Fragment نشطًا
        if (activeInstance != null && invoiceCustomerTextView != null) {
            if (customer != null) {
                invoiceCustomerTextView.setText("الزبون: " + customer.getName());
            } else {
                invoiceCustomerTextView.setText("الزبون: مجهول");
            }
        }
        
        // Debug log
        android.util.Log.d("CounterFragment", "setCustomer finished");
    }

    public static Customer getCurrentCustomer() {
        // Debug log
        android.util.Log.d("CounterFragment", "getCurrentCustomer called, returning: " + 
            (currentCustomer != null ? currentCustomer.getName() : "null"));
        
        // Debug log
        android.util.Log.d("CounterFragment", "getCurrentCustomer finished");
        
        return currentCustomer;
    }

    public static void clearInvoice() {
        // Debug log
        android.util.Log.d("CounterFragment", "clearInvoice called");
        
        invoiceItems.clear();
        if (activeInstance != null && activeInstance.invoiceAdapter != null) {
            activeInstance.invoiceAdapter.notifyDataSetChanged();
            activeInstance.updateTotalPrice();
        }
        
        // Debug log
        android.util.Log.d("CounterFragment", "clearInvoice finished");
    }

    public static void clearCustomer() {
        // Debug log
        android.util.Log.d("CounterFragment", "clearCustomer called");
        
        currentCustomer = null;
        if (activeInstance != null && invoiceCustomerTextView != null) {
            invoiceCustomerTextView.setText("الزبون: مجهول");
        }
        
        // Debug log
        android.util.Log.d("CounterFragment", "clearCustomer finished");
    }

    private void updateTotalPrice() {
        // Debug log
        android.util.Log.d("CounterFragment", "updateTotalPrice called");
        
        totalPrice = 0.0;
        for (InvoiceItem item : invoiceItems) {
            totalPrice += item.getQuantity()*item.getPrice();
        }
        totalPriceTextView.setText(String.format("%.2f ريال", totalPrice));
        
        // Debug log
        android.util.Log.d("CounterFragment", "updateTotalPrice finished - total: " + totalPrice);
    }
} 