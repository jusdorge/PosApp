package com.example.posapp;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.model.InvoiceItem;

public class EditInvoiceItemDialog extends DialogFragment {
    private InvoiceItem invoiceItem;
    private int position;
    private OnItemUpdatedListener listener;

    public interface OnItemUpdatedListener {
        void onItemUpdated(InvoiceItem item, int position);
    }

    public static EditInvoiceItemDialog newInstance(InvoiceItem item, int position) {
        EditInvoiceItemDialog dialog = new EditInvoiceItemDialog();
        Bundle args = new Bundle();
        args.putInt("position", position);
        // سنقوم بنقل قيم المنتج مباشرة لتجنب مشاكل Parcelable
        dialog.setArguments(args);
        dialog.invoiceItem = item;
        return dialog;
    }

    public void setOnItemUpdatedListener(OnItemUpdatedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_invoice_item, null);

        if (getArguments() != null) {
            position = getArguments().getInt("position", -1);
        }

        // ربط عناصر الواجهة
        TextView productNameText = view.findViewById(R.id.productNameText);
        EditText priceEditText = view.findViewById(R.id.priceEditText);
        EditText quantityEditText = view.findViewById(R.id.quantityEditText);
        TextView totalPriceText = view.findViewById(R.id.totalPriceText);
        Button decreaseButton = view.findViewById(R.id.decreaseButton);
        Button increaseButton = view.findViewById(R.id.increaseButton);

        // عرض بيانات المنتج الحالي
        productNameText.setText(invoiceItem.getProductName());
        priceEditText.setText(String.valueOf(invoiceItem.getPrice()));
        quantityEditText.setText(String.valueOf(invoiceItem.getQuantity()));
        updateTotalPrice(totalPriceText, invoiceItem.getPrice(), invoiceItem.getQuantity());
        
        // تنظيف التركيز على الحقول
        priceEditText.clearFocus();
        quantityEditText.clearFocus();

        // إضافة مستمعين لتغيير الكمية بالأزرار
        decreaseButton.setOnClickListener(v -> {
            int currentQty = Integer.parseInt(quantityEditText.getText().toString());
            if (currentQty > 1) {
                currentQty--;
                quantityEditText.setText(String.valueOf(currentQty));
            }
        });

        increaseButton.setOnClickListener(v -> {
            int currentQty = Integer.parseInt(quantityEditText.getText().toString());
            currentQty++;
            quantityEditText.setText(String.valueOf(currentQty));
        });

        // مستمع لتغيير السعر
        priceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double price = s.toString().isEmpty() ? 0 : Double.parseDouble(s.toString());
                    int quantity = Integer.parseInt(quantityEditText.getText().toString());
                    updateTotalPrice(totalPriceText, price, quantity);
                } catch (NumberFormatException e) {
                    totalPriceText.setText("0.00 ريال");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // مستمع لتغيير الكمية
        quantityEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double price = Double.parseDouble(priceEditText.getText().toString());
                    int quantity = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                    updateTotalPrice(totalPriceText, price, quantity);
                } catch (NumberFormatException e) {
                    totalPriceText.setText("0.00 ريال");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        builder.setView(view)
                .setTitle("تعديل المنتج")
                .setPositiveButton("حفظ", null) // سنقوم بتعيين المستمع لاحقًا
                .setNegativeButton("إلغاء", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view1 -> {
                try {
                    double price = Double.parseDouble(priceEditText.getText().toString());
                    int quantity = Integer.parseInt(quantityEditText.getText().toString());
                    
                    if (quantity <= 0) {
                        Toast.makeText(getContext(), "الكمية يجب أن تكون أكبر من صفر", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // تحديث المنتج
                    invoiceItem.setPrice(price);
                    invoiceItem.setQuantity(quantity);
                    
                    if (listener != null) {
                        listener.onItemUpdated(invoiceItem, position);
                    }
                    
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "الرجاء إدخال قيم صحيحة", Toast.LENGTH_SHORT).show();
                }
            });
        });

        return dialog;
    }

    private void updateTotalPrice(TextView totalPriceText, double price, int quantity) {
        double total = price * quantity;
        totalPriceText.setText(String.format("%.2f ريال", total));
    }
} 