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
import androidx.fragment.app.FragmentManager;

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

    // للحفاظ على الFragments وتجنب إعادة إنشائها
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private Fragment counterFragment = new CounterFragment();
    private Fragment itemsFragment = new ItemsFragment();
    private Fragment todayFragment = new TodayFragment();
    private Fragment reportsFragment = new ReportsFragment();
    private Fragment moreFragment = new MoreFragment();
    private Fragment activeFragment = counterFragment;

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
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // مستمع لعناصر القائمة الجانبية
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_home) {
                    switchToFragment(counterFragment);
                    BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                    bottomNav.setSelectedItemId(R.id.nav_counter);
                } else if (id == R.id.action_manage_customers) {
                    navigateToCustomersManagement();
                } else if (id == R.id.action_manage_products) {
                    navigateToProductsManagement();
                } else if (id == R.id.action_logout) {
                    logoutUser();
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });

        // إعداد BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.nav_reports:
                    selectedFragment = reportsFragment;
                    break;
                case R.id.nav_today:
                    selectedFragment = todayFragment;
                    break;
                case R.id.nav_counter:
                    selectedFragment = counterFragment;
                    break;
                case R.id.nav_items:
                    selectedFragment = itemsFragment;
                    break;
                case R.id.nav_more:
                    selectedFragment = moreFragment;
                    break;
            }

            if (selectedFragment != null) {
                switchToFragment(selectedFragment);
            }

            return true;
        });

        // إضافة جميع الFragments مع إخفائها (ما عدا CounterFragment)
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, moreFragment, "5").hide(moreFragment)
                .add(R.id.fragment_container, itemsFragment, "4").hide(itemsFragment)
                .add(R.id.fragment_container, todayFragment, "3").hide(todayFragment)
                .add(R.id.fragment_container, reportsFragment, "2").hide(reportsFragment)
                .add(R.id.fragment_container, counterFragment, "1")
                .commit();
    }

    private void switchToFragment(Fragment fragment) {
        if (activeFragment != fragment) {
            fragmentManager.beginTransaction()
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onBackPressed() {
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