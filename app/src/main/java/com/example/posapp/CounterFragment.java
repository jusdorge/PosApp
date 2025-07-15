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
            CheckoutDialog dialog = CheckoutDialog.newInstance(new ArrayList<>(invoiceItems), totalPrice);
            dialog.setOnInvoiceCompletedListener(() -> {
                // مسح الفاتورة بعد الدفع
                invoiceItems.clear();
                invoiceAdapter.notifyDataSetChanged();
                updateTotalPrice();
            });
            dialog.show(getChildFragmentManager(), "CheckoutDialog");
        });

        // تحديث إجمالي السعر
        updateTotalPrice();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activeInstance == this) {
            activeInstance = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // تحديث قائمة الفاتورة وإجمالي السعر عند العودة إلى الشاشة
        if (invoiceAdapter != null) {
            invoiceAdapter.notifyDataSetChanged();
            updateTotalPrice();
        }
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
        if (customer != null) {
            currentCustomer = customer;
        }
    }

    private void updateTotalPrice() {
        totalPrice = 0.0;
        for (InvoiceItem item : invoiceItems) {
            totalPrice += item.getQuantity()*item.getPrice();
        }
        totalPriceTextView.setText(String.format("%.2f ريال", totalPrice));
    }
} 