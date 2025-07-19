package com.example.posapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.StockMovement;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class StockMovementsAdapter extends RecyclerView.Adapter<StockMovementsAdapter.MovementViewHolder> {
    private List<StockMovement> movementsList;
    private SimpleDateFormat dateTimeFormat;

    public StockMovementsAdapter(List<StockMovement> movementsList) {
        this.movementsList = movementsList;
        this.dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public MovementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_movement, parent, false);
        return new MovementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovementViewHolder holder, int position) {
        StockMovement movement = movementsList.get(position);
        holder.bind(movement);
    }

    @Override
    public int getItemCount() {
        return movementsList.size();
    }

    class MovementViewHolder extends RecyclerView.ViewHolder {
        TextView productNameTextView;
        TextView movementTypeTextView;
        TextView quantityTextView;
        TextView reasonTextView;
        TextView timestampTextView;
        TextView quantityChangeTextView;
        TextView notesTextView;
        ImageView movementTypeIcon;

        public MovementViewHolder(@NonNull View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            movementTypeTextView = itemView.findViewById(R.id.movementTypeTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            reasonTextView = itemView.findViewById(R.id.reasonTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            quantityChangeTextView = itemView.findViewById(R.id.quantityChangeTextView);
            notesTextView = itemView.findViewById(R.id.notesTextView);
            movementTypeIcon = itemView.findViewById(R.id.movementTypeIcon);
        }

        void bind(StockMovement movement) {
            productNameTextView.setText(movement.getProductName());
            reasonTextView.setText(movement.getReason());
            
            // Format timestamp
            if (movement.getTimestamp() != null) {
                timestampTextView.setText(dateTimeFormat.format(movement.getTimestamp().toDate()));
            }
            
            // Set movement type and icon
            if (movement.isIncoming()) {
                movementTypeTextView.setText("دخول");
                movementTypeTextView.setTextColor(android.graphics.Color.GREEN);
                movementTypeIcon.setImageResource(R.drawable.ic_menu_add);
                movementTypeIcon.setColorFilter(android.graphics.Color.GREEN);
                quantityTextView.setText("+" + movement.getQuantity());
                quantityTextView.setTextColor(android.graphics.Color.GREEN);
            } else if (movement.isOutgoing()) {
                movementTypeTextView.setText("خروج");
                movementTypeTextView.setTextColor(android.graphics.Color.RED);
                movementTypeIcon.setImageResource(R.drawable.ic_menu_remove);
                movementTypeIcon.setColorFilter(android.graphics.Color.RED);
                quantityTextView.setText("-" + movement.getQuantity());
                quantityTextView.setTextColor(android.graphics.Color.RED);
            } else if (movement.isAdjustment()) {
                movementTypeTextView.setText("تعديل");
                movementTypeTextView.setTextColor(android.graphics.Color.BLUE);
                movementTypeIcon.setImageResource(R.drawable.ic_settings);
                movementTypeIcon.setColorFilter(android.graphics.Color.BLUE);
                quantityTextView.setText("±" + movement.getQuantity());
                quantityTextView.setTextColor(android.graphics.Color.BLUE);
            }
            
            // Show quantity change
            String quantityChange = movement.getQuantityBefore() + " → " + movement.getQuantityAfter();
            quantityChangeTextView.setText(quantityChange);
            
            // Show notes if available
            if (movement.getNotes() != null && !movement.getNotes().isEmpty()) {
                notesTextView.setText(movement.getNotes());
                notesTextView.setVisibility(View.VISIBLE);
            } else {
                notesTextView.setVisibility(View.GONE);
            }
        }
    }
} 