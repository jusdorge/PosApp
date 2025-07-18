package com.example.posapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.posapp.model.StockMovement;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockMovementsFragment extends Fragment {
    private RecyclerView stockMovementsRecyclerView;
    private EditText searchMovementsEditText;
    private Button btnStartDate;
    private Button btnEndDate;
    private Button btnFilter;
    private Button btnClearFilter;
    private TextView tvTotalMovements;
    private TextView tvIncomingMovements;
    private TextView tvOutgoingMovements;

    private StockMovementsAdapter movementsAdapter;
    private List<StockMovement> movementsList;
    private List<StockMovement> filteredMovementsList;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    
    private Date startDate;
    private Date endDate;
    private Calendar calendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stock_movements, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        calendar = Calendar.getInstance();
        movementsList = new ArrayList<>();
        filteredMovementsList = new ArrayList<>();

        // Initialize UI components
        initViews(view);
        setupRecyclerView();
        setupSearchListener();
        setupButtons();

        // Load stock movements data
        loadStockMovements();

        return view;
    }

    private void initViews(View view) {
        stockMovementsRecyclerView = view.findViewById(R.id.stockMovementsRecyclerView);
        searchMovementsEditText = view.findViewById(R.id.searchMovementsEditText);
        btnStartDate = view.findViewById(R.id.btnStartDate);
        btnEndDate = view.findViewById(R.id.btnEndDate);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnClearFilter = view.findViewById(R.id.btnClearFilter);
        tvTotalMovements = view.findViewById(R.id.tvTotalMovements);
        tvIncomingMovements = view.findViewById(R.id.tvIncomingMovements);
        tvOutgoingMovements = view.findViewById(R.id.tvOutgoingMovements);
    }

    private void setupRecyclerView() {
        movementsAdapter = new StockMovementsAdapter(filteredMovementsList);
        stockMovementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        stockMovementsRecyclerView.setAdapter(movementsAdapter);
    }

    private void setupSearchListener() {
        searchMovementsEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMovements(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        btnStartDate.setOnClickListener(v -> showStartDatePicker());
        btnEndDate.setOnClickListener(v -> showEndDatePicker());
        btnFilter.setOnClickListener(v -> filterMovementsByDate());
        btnClearFilter.setOnClickListener(v -> clearFilters());
    }

    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(),
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 0, 0, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                startDate = calendar.getTime();
                btnStartDate.setText("من: " + dateFormat.format(startDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(),
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 23, 59, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                endDate = calendar.getTime();
                btnEndDate.setText("إلى: " + dateFormat.format(endDate));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void filterMovementsByDate() {
        if (startDate == null || endDate == null) {
            Toast.makeText(getContext(), "يرجى اختيار تاريخ البداية والنهاية", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate.after(endDate)) {
            Toast.makeText(getContext(), "تاريخ البداية يجب أن يكون قبل تاريخ النهاية", Toast.LENGTH_SHORT).show();
            return;
        }

        loadMovementsByDateRange();
    }

    private void clearFilters() {
        startDate = null;
        endDate = null;
        btnStartDate.setText("اختر تاريخ البداية");
        btnEndDate.setText("اختر تاريخ النهاية");
        searchMovementsEditText.setText("");
        loadStockMovements();
    }

    private void loadStockMovements() {
        db.collection("stock_movements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                movementsList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    StockMovement movement = document.toObject(StockMovement.class);
                    movement.setId(document.getId());
                    movementsList.add(movement);
                }
                
                filteredMovementsList.clear();
                filteredMovementsList.addAll(movementsList);
                movementsAdapter.notifyDataSetChanged();
                
                updateSummary();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في تحميل حركات المخزون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadMovementsByDateRange() {
        Timestamp startTimestamp = new Timestamp(startDate);
        Timestamp endTimestamp = new Timestamp(endDate);

        db.collection("stock_movements")
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .whereLessThanOrEqualTo("timestamp", endTimestamp)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                movementsList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    StockMovement movement = document.toObject(StockMovement.class);
                    movement.setId(document.getId());
                    movementsList.add(movement);
                }
                
                filteredMovementsList.clear();
                filteredMovementsList.addAll(movementsList);
                movementsAdapter.notifyDataSetChanged();
                
                updateSummary();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "فشل في تحميل حركات المخزون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void filterMovements(String query) {
        filteredMovementsList.clear();
        if (query.isEmpty()) {
            filteredMovementsList.addAll(movementsList);
        } else {
            for (StockMovement movement : movementsList) {
                if (movement.getProductName().toLowerCase().contains(query.toLowerCase()) ||
                    movement.getReason().toLowerCase().contains(query.toLowerCase()) ||
                    movement.getMovementType().toLowerCase().contains(query.toLowerCase())) {
                    filteredMovementsList.add(movement);
                }
            }
        }
        movementsAdapter.notifyDataSetChanged();
        updateSummary();
    }

    private void updateSummary() {
        int totalMovements = filteredMovementsList.size();
        int incomingMovements = 0;
        int outgoingMovements = 0;

        for (StockMovement movement : filteredMovementsList) {
            if (movement.isIncoming()) {
                incomingMovements++;
            } else if (movement.isOutgoing()) {
                outgoingMovements++;
            }
        }

        tvTotalMovements.setText("إجمالي الحركات: " + totalMovements);
        tvIncomingMovements.setText("حركات الدخول: " + incomingMovements);
        tvOutgoingMovements.setText("حركات الخروج: " + outgoingMovements);
    }

    @Override
    public void onResume() {
        super.onResume();
        // تنظيف حقل البحث والمرشحات عند العودة للشاشة
        if (searchMovementsEditText != null) {
            searchMovementsEditText.setText("");
        }
        clearFilters();
        loadStockMovements();
    }
} 