package com.example.posapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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

    // للحفاظ على الFragments وتجنب إعادة إنشائها
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private Fragment counterFragment = new CounterFragment();
    private Fragment itemsFragment = new ItemsFragment();
    private Fragment todayFragment = new TodayFragment();
    private Fragment reportsFragment = new ReportsFragment();
    private Fragment moreFragment = new MoreFragment();
    private Fragment customersFragment = null; // سيتم إنشاؤها عند الحاجة
    private Fragment productsManagementFragment = null; // سيتم إنشاؤها عند الحاجة
    private Fragment activeFragment = counterFragment;

    // متغير لتتبع ما إذا كنا في شاشة من القائمة الجانبية
    private boolean isInDrawerFragment = false;

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

        // الحصول على مرجع لـ BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // مستمع لعناصر القائمة الجانبية
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_home) {
                    // العودة إلى الشاشة الرئيسية
                    isInDrawerFragment = false;
                    switchToFragment(counterFragment);
                    bottomNavigationView.setSelectedItemId(R.id.nav_counter);
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
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            // إذا كنا في شاشة من القائمة الجانبية، نعود أولاً إلى الـ Fragments الأساسية
            if (isInDrawerFragment) {
                isInDrawerFragment = false;
            }

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

        // إضافة جميع الFragments الأساسية مع إخفائها (ما عدا CounterFragment)
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
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // إخفاء الـ Fragment النشط الحالي
            transaction.hide(activeFragment);

            // إذا كان الـ Fragment المطلوب غير مضاف بعد، أضفه
            if (!fragment.isAdded()) {
                transaction.add(R.id.fragment_container, fragment);
            }

            // إظهار الـ Fragment المطلوب
            transaction.show(fragment);

            transaction.commit();
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
        if (id == R.id.action_add_customer) {
            showAddCustomerDialog();
            return true;
        } else if (id == R.id.action_settings) {
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
        } else if (id == R.id.action_select_customer_location) {
            showSelectCustomerLocationPage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // عرض dialog لاختيار عميل للفاتورة
    private void showAddCustomerDialog() {
        SelectCustomerDialog dialog = new SelectCustomerDialog();
        dialog.setOnCustomerSelectedListener(customer -> {
            // يمكنك إضافة منطق هنا عند اختيار العميل
            // مثلاً: حفظ العميل المختار للفاتورة الحالية
            Toast.makeText(this, "تم اختيار العميل: " + customer.getName(), Toast.LENGTH_SHORT).show();
            // TODO: إضافة منطق ربط العميل بالفاتورة الحالية
        });
        dialog.show(getSupportFragmentManager(), "SelectCustomerDialog");
    }

    // صفحة اختيار الموقع الجغرافي للعميل
    private void showSelectCustomerLocationPage() {
        Intent intent = new Intent(this, SelectCustomerLocationActivity.class);
        startActivity(intent);
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
        // إنشاء Fragment إدارة الزبائن إذا لم يكن موجوداً
        if (customersFragment == null) {
            customersFragment = new CustomersFragment();
        }

        isInDrawerFragment = true;

        // استخدام نفس آلية show/hide
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // إخفاء جميع الـ Fragments
        transaction.hide(activeFragment);

        // إضافة أو إظهار Fragment إدارة الزبائن
        if (!customersFragment.isAdded()) {
            transaction.add(R.id.fragment_container, customersFragment, "customers");
        } else {
            transaction.show(customersFragment);
        }

        transaction.addToBackStack(null);
        transaction.commit();

        activeFragment = customersFragment;

        // إلغاء تحديد عناصر Bottom Navigation
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
    }

    private void navigateToProductsManagement() {
        // إنشاء Fragment إدارة المنتجات إذا لم يكن موجوداً
        if (productsManagementFragment == null) {
            productsManagementFragment = new ProductsManagementFragment();
        }

        isInDrawerFragment = true;

        // استخدام نفس آلية show/hide
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // إخفاء جميع الـ Fragments
        transaction.hide(activeFragment);

        // إضافة أو إظهار Fragment إدارة المنتجات
        if (!productsManagementFragment.isAdded()) {
            transaction.add(R.id.fragment_container, productsManagementFragment, "products");
        } else {
            transaction.show(productsManagementFragment);
        }

        transaction.addToBackStack(null);
        transaction.commit();

        activeFragment = productsManagementFragment;

        // إلغاء تحديد عناصر Bottom Navigation
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
    }

    public void switchToCounterFragment() {
        isInDrawerFragment = false;
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

            // إذا كنا نعود من شاشة القائمة الجانبية
            if (isInDrawerFragment) {
                isInDrawerFragment = false;
                // إعادة تحديد العنصر المناسب في Bottom Navigation
                if (activeFragment == counterFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_counter);
                } else if (activeFragment == itemsFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_items);
                } else if (activeFragment == todayFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_today);
                } else if (activeFragment == reportsFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_reports);
                } else if (activeFragment == moreFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_more);
                }
            }
        } else if (activeFragment != counterFragment) {
            switchToCounterFragment();
        } else {
            super.onBackPressed();
        }
    }
}