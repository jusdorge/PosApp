package com.example.posapp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.model.Invoice;
import com.example.posapp.model.PaymentMethod;

public class EditPaymentMethodDialog extends DialogFragment {
    
    public interface OnPaymentMethodChangedListener {
        void onPaymentMethodChanged(PaymentMethod newPaymentMethod);
    }
    
    private OnPaymentMethodChangedListener listener;
    private Invoice invoice;
    private RadioGroup paymentMethodRadioGroup;
    
    public static EditPaymentMethodDialog newInstance(Invoice invoice) {
        EditPaymentMethodDialog dialog = new EditPaymentMethodDialog();
        Bundle args = new Bundle();
        args.putSerializable("invoice", invoice);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnPaymentMethodChangedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPaymentMethodChangedListener");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_payment_method, null);
        
        // استخراج الفاتورة من Arguments
        if (getArguments() != null) {
            invoice = (Invoice) getArguments().getSerializable("invoice");
        }
        
        // تهيئة العناصر
        initializeViews(view);
        
        builder.setView(view)
                .setTitle("تعديل طريقة الدفع")
                .setPositiveButton("حفظ", null) // سنضع الListener بعد إنشاء Dialog
                .setNegativeButton("إلغاء", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        
        // تخصيص زر الحفظ لمنع إغلاق Dialog عند وجود خطأ
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (savePaymentMethod()) {
                    dialog.dismiss();
                }
            });
        });
        
        return dialog;
    }
    
    private void initializeViews(View view) {
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView currentMethodTextView = view.findViewById(R.id.currentMethodTextView);
        paymentMethodRadioGroup = view.findViewById(R.id.paymentMethodRadioGroup);
        
        // عرض طريقة الدفع الحالية
        if (invoice != null) {
            PaymentMethod currentMethod = invoice.getPaymentMethod();
            currentMethodTextView.setText("الطريقة الحالية: " + currentMethod.getDisplayWithIcon());
            
            // اختيار الطريقة الحالية
            selectCurrentPaymentMethod(currentMethod);
        }
        
        // إضافة طرق الدفع إلى RadioGroup
        addPaymentMethodOptions();
    }
    
    private void addPaymentMethodOptions() {
        PaymentMethod[] methods = PaymentMethod.values();
        
        for (PaymentMethod method : methods) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(method.getDisplayWithIcon());
            radioButton.setTextSize(16);
            radioButton.setPadding(16, 12, 16, 12);
            radioButton.setTag(method);
            
            // تحديد اللون حسب نوع الدفع
            if (method == PaymentMethod.CREDIT) {
                radioButton.setTextColor(getResources().getColor(R.color.reportWarning));
            } else {
                radioButton.setTextColor(getResources().getColor(R.color.reportSuccess));
            }
            
            paymentMethodRadioGroup.addView(radioButton);
        }
    }
    
    private void selectCurrentPaymentMethod(PaymentMethod currentMethod) {
        // سيتم اختيار الطريقة الحالية بعد إضافة الخيارات
        paymentMethodRadioGroup.post(() -> {
            for (int i = 0; i < paymentMethodRadioGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) paymentMethodRadioGroup.getChildAt(i);
                PaymentMethod method = (PaymentMethod) radioButton.getTag();
                if (method == currentMethod) {
                    radioButton.setChecked(true);
                    break;
                }
            }
        });
    }
    
    private boolean savePaymentMethod() {
        int selectedId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        
        if (selectedId == -1) {
            // لا يوجد اختيار
            return false;
        }
        
        RadioButton selectedRadioButton = paymentMethodRadioGroup.findViewById(selectedId);
        PaymentMethod selectedMethod = (PaymentMethod) selectedRadioButton.getTag();
        
        // إشعار المستمع بالتغيير
        if (listener != null) {
            listener.onPaymentMethodChanged(selectedMethod);
        }
        
        return true;
    }
} 