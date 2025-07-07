package com.example.posapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private BottomNavigationView bottomNavigationView;

    // للحفاظ على الفragments وتجنب إعادة إنشائها
    private Fragment counterFragment;
    private Fragment itemsFragment;
    private Fragment todayFragment;
    private Fragment reportsFragment;
    private Fragment moreFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // تهيئة Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // إعداد Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // إعداد DrawerLayout و NavigationView
        setupDrawer(toolbar);

        // إعداد Bottom Navigation
        setupBottomNavigation();

        // تهيئة الFragments
        initializeFragments();
    }

    private void setupDrawer(Toolbar toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            boolean handled = false;

            if (id == R.id.action_home) {
                switchFragment(counterFragment);
                bottomNavigationView.setSelectedItemId(R.id.nav_counter);
                handled = true;
            } else if (id == R.id.action_manage_customers) {
                navigateToCustomersManagement();
                handled = true;
            } else if (id == R.id.action_manage_products) {
                navigateToProductsManagement();
                handled = true;
            } else if (id == R.id.action_logout) {
                logoutUser();
                handled = true;
            }

            drawerLayout.closeDrawers();
            return handled;
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_reports:
                    switchFragment(reportsFragment);
                    return true;
                case R.id.nav_today:
                    switchFragment(todayFragment);
                    return true;
                case R.id.nav_counter:
                    switchFragment(counterFragment);
                    return true;
                case R.id.nav_items:
                    switchFragment(itemsFragment);
                    return true;
                case R.id.nav_more:
                    switchFragment(moreFragment);
                    return true;
            }
            return false;
        });
    }

    private void initializeFragments() {
        // إنشاء الFragments مرة واحدة فقط
        counterFragment = new CounterFragment();
        itemsFragment = new ItemsFragment();
        todayFragment = new TodayFragment();
        reportsFragment = new ReportsFragment();
        moreFragment = new MoreFragment();

        // إضافة جميع الFragments مع إخفائها
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, moreFragment, "more").hide(moreFragment)
                .add(R.id.fragment_container, reportsFragment, "reports").hide(reportsFragment)
                .add(R.id.fragment_container, todayFragment, "today").hide(todayFragment)
                .add(R.id.fragment_container, itemsFragment, "items").hide(itemsFragment)
                .add(R.id.fragment_container, counterFragment, "counter")
                .commit();

        activeFragment = counterFragment;
    }

    private void switchFragment(Fragment fragment) {
        if (fragment != activeFragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(fragment)
                    .commit();
            activeFragment = fragment;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // فتح شاشة الإعدادات
            return true;
        } else if (id == R.id.action_manage_customers) {
            navigateToCustomersManagement();
            return true;
        } else if (id == R.id.action_manage_products) {
            navigateToProductsManagement();
            return true;
        } else if (id == R.id.action_logout) {
            logoutUser();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        mAuth.signOut();
        GoogleSignIn.getClient(this, new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCustomersManagement() {
        // استخدام replace بدلاً من add لتجنب تكديس الFragments
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.fragment_container, new CustomersFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToProductsManagement() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.fragment_container, new ProductsManagementFragment())
                .addToBackStack(null)
                .commit();
    }

    public void switchToCounterFragment() {
        bottomNavigationView.setSelectedItemId(R.id.nav_counter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onBackPressed() {
        // التعامل مع زر الرجوع بشكل أفضل
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else if (activeFragment != counterFragment) {
            switchToCounterFragment();
        } else {
            super.onBackPressed();
        }
    }
}