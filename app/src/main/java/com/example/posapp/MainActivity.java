package com.example.posapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // إعداد Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // إخفاء العنوان الافتراضي

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            switch (item.getItemId()) {
                case R.id.nav_reports:
                    selectedFragment = new ReportsFragment();
                    break;
                case R.id.nav_today:
                    selectedFragment = new TodayFragment();
                    break;
                case R.id.nav_counter:
                    selectedFragment = new CounterFragment();
                    break;
                case R.id.nav_items:
                    selectedFragment = new ItemsFragment();
                    break;
                case R.id.nav_more:
                    selectedFragment = new MoreFragment();
                    break;
            }
            
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            }
            
            return true;
        });

        // Load the default fragment (CounterFragment)
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new CounterFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_manage_customers) {
            navigateToCustomersManagement();
            return true;
        } else if (id == R.id.action_manage_products) {
            navigateToProductsManagement();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void navigateToCustomersManagement() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new CustomersFragment())
            .addToBackStack(null)
            .commit();
    }

    private void navigateToProductsManagement() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new ProductsManagementFragment())
            .addToBackStack(null)
            .commit();
    }

    public void switchToCounterFragment() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_counter);
    }
}