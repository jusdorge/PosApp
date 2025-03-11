package com.example.posapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfitDetailsActivity extends AppCompatActivity {
    private Calendar currentCalendar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profit_details);

        Button dateButton = findViewById(R.id.dateButton);
        Button previousDayButton = findViewById(R.id.previousDayButton);

        currentCalendar = Calendar.getInstance();
        updateDateButton(dateButton);

        previousDayButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDateButton(dateButton);
            // هنا يمكنك إضافة منطق لتحديث التقارير بناءً على التاريخ الجديد
        });
    }

    private void updateDateButton(Button dateButton) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateButton.setText(sdf.format(currentCalendar.getTime()));
    }
} 