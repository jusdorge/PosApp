package com.example.posapp;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.model.Product;
import com.example.posapp.model.StockMovement;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class StockAdjustmentDialog extends DialogFragment {
    private static final String ARG_PRODUCT = "product";
    
    private Product product;
    private FirebaseFirestore db;
    
    private TextView productNameTextView;
    private TextView currentQuantityTextView;
    private EditText adjustmentQuantityEditText;
    private RadioGroup adjustmentTypeRadioGroup;
    private RadioButton addRadioButton;
    private RadioButton subtractRadioButton;
    private EditText reasonEditText;
    private Button saveButton;
    private Button cancelButton;
    
    private OnStockAdjustedListener listener;
    
    public interface OnStockAdjustedListener {
        void onStockAdjusted();
    }
    
    public static StockAdjustmentDialog newInstance(Product product) {
        StockAdjustmentDialog dialog = new StockAdjustmentDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PRODUCT, product.getId());
        dialog.setArguments(args);
        return dialog;
    }
    
    public void setOnStockAdjustedListener(OnStockAdjustedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            product = (Product) getArguments().getSerializable(ARG_PRODUCT);
        }
        db = FirebaseFirestore.getInstance();
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_stock_adjustment, null);
        
        initViews(view);
        setupUI();
        setupListeners();
        
        builder.setView(view)
                .setTitle("تعديل المخزون");
        
        return builder.create();
    }
    
    private void initViews(View view) {
        productNameTextView = view.findViewById(R.id.productNameTextView);
        currentQuantityTextView = view.findViewById(R.id.currentQuantityTextView);
        adjustmentQuantityEditText = view.findViewById(R.id.adjustmentQuantityEditText);
        adjustmentTypeRadioGroup = view.findViewById(R.id.adjustmentTypeRadioGroup);
        addRadioButton = view.findViewById(R.id.addRadioButton);
        subtractRadioButton = view.findViewById(R.id.subtractRadioButton);
        reasonEditText = view.findViewById(R.id.reasonEditText);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
    }
    
    private void setupUI() {
        productNameTextView.setText(product.getName());
        currentQuantityTextView.setText("الكمية الحالية: " + product.getQuantity());
        addRadioButton.setChecked(true); // Default to adding
    }
    
    private void setupListeners() {
        saveButton.setOnClickListener(v -> performStockAdjustment());
        cancelButton.setOnClickListener(v -> dismiss());
    }
    
    private void performStockAdjustment() {
        String quantityText = adjustmentQuantityEditText.getText().toString().trim();
        String reason = reasonEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(quantityText)) {
            Toast.makeText(getContext(), "يرجى إدخال الكمية", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(reason)) {
            Toast.makeText(getContext(), "يرجى إدخال السبب", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int adjustmentQuantity = Integer.parseInt(quantityText);
            boolean isAdd = addRadioButton.isChecked();
            
            int currentQuantity = product.getQuantity();
            int newQuantity;
            String movementType;
            
            if (isAdd) {
                newQuantity = currentQuantity + adjustmentQuantity;
                movementType = StockMovement.MOVEMENT_IN;
            } else {
                newQuantity = currentQuantity - adjustmentQuantity;
                movementType = StockMovement.MOVEMENT_OUT;
                
                if (newQuantity < 0) {
                    Toast.makeText(getContext(), "لا يمكن أن تكون الكمية سالبة", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Update product quantity
            updateProductQuantity(newQuantity, currentQuantity, adjustmentQuantity, movementType, reason);
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "يرجى إدخال رقم صحيح", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateProductQuantity(int newQuantity, int currentQuantity, int adjustmentQuantity, String movementType, String reason) {
        DocumentReference productRef = db.collection("products").document(product.getId());
        
        productRef.update("quantity", newQuantity)
            .addOnSuccessListener(aVoid -> {
                // Record stock movement
                recordStockMovement(currentQuantity, newQuantity, adjustmentQuantity, movementType, reason);
                
                Toast.makeText(getContext(), "تم تحديث المخزون بنجاح", Toast.LENGTH_SHORT).show();
                
                if (listener != null) {
                    listener.onStockAdjusted();
                }
                
                dismiss();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في تحديث المخزون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void recordStockMovement(int quantityBefore, int quantityAfter, int adjustmentQuantity, String movementType, String reason) {
        DocumentReference stockMovementRef = db.collection("stock_movements").document();
        StockMovement stockMovement = new StockMovement(
            product.getId(),
            product.getName(),
            adjustmentQuantity,
            movementType,
            StockMovement.REASON_ADJUSTMENT,
            null, // No reference ID for manual adjustments
            "user", // You can get the actual user ID here
            reason,
            quantityBefore,
            quantityAfter
        );
        stockMovement.setId(stockMovementRef.getId());
        
        stockMovementRef.set(stockMovement)
            .addOnFailureListener(e -> {
                android.util.Log.w("StockAdjustmentDialog", "فشل في تسجيل حركة المخزون: " + e.getMessage());
            });
    }
} 