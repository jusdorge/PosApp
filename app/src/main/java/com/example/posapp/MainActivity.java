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
import android.widget.TextView;
import com.example.posapp.UserSession;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle drawerToggle;
    private BottomNavigationView bottomNavigationView;

    // للحفاظ على الFragments وتجنب إعادة إنشائها
    private FragmentManager fragmentManager;
    private Fragment counterFragment;
    private Fragment itemsFragment;
    private Fragment todayFragment;
    private Fragment reportsFragment;
    private Fragment moreFragment;
    private Fragment customersFragment = null; // سيتم إنشاؤها عند الحاجة
    private Fragment productsManagementFragment = null; // سيتم إنشاؤها عند الحاجة
    private Fragment inventoryManagementFragment = null; // سيتم إنشاؤها عند الحاجة
    private Fragment activeFragment;

    // متغير لتتبع ما إذا كنا في شاشة من القائمة الجانبية
    private boolean isInDrawerFragment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Debug log
        android.util.Log.d("MainActivity", "✓ onCreate called");
        
        try {
            setContentView(R.layout.activity_main);
            android.util.Log.d("MainActivity", "✓ Layout set");
            
            // تهيئة FragmentManager والـ Fragments
            fragmentManager = getSupportFragmentManager();
            counterFragment = new CounterFragment();
            itemsFragment = new ItemsFragment();
            todayFragment = new TodayFragment();
            reportsFragment = new ReportsFragment();
            moreFragment = new MoreFragment();
            activeFragment = counterFragment;
            android.util.Log.d("MainActivity", "✓ Fragments initialized");
            
            // فحص حالة تسجيل الدخول
            android.util.Log.d("MainActivity", "Checking user session...");
            UserSession userSession = UserSession.getInstance(this);
            android.util.Log.d("MainActivity", "✓ UserSession obtained");
            
            if (!userSession.isLoggedIn()) {
                android.util.Log.d("MainActivity", "User not logged in - redirecting to login");
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            
            if (!userSession.validateSession()) {
                android.util.Log.d("MainActivity", "Session invalid - redirecting to login");
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            
            android.util.Log.d("MainActivity", "✓ User session valid");
            
            // تهيئة Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            android.util.Log.d("MainActivity", "✓ Firebase Auth initialized");

            // إعداد UI
            setupUI();
            android.util.Log.d("MainActivity", "✓ UI setup completed");
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error in onCreate", e);
            Toast.makeText(this, "خطأ في تهيئة التطبيق: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupUI() {
        // إعداد Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        android.util.Log.d("MainActivity", "✓ Toolbar setup");

        // إعداد DrawerLayout و NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        android.util.Log.d("MainActivity", "✓ Drawer setup");

        // الحصول على مرجع لـ BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        android.util.Log.d("MainActivity", "✓ Bottom navigation setup");

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
                } else if (id == R.id.action_inventory_management) {
                    navigateToInventoryManagement();
                } else if (id == R.id.action_all_invoices) {
                    openAllInvoicesActivity();
                } else if (id == R.id.action_select_customer_location) {
                    showSelectCustomersLocationPage();
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
        android.util.Log.d("MainActivity", "✓ Navigation listeners setup");

        try {
            // إضافة جميع الFragments الأساسية مع إخفائها (ما عدا CounterFragment)
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, moreFragment, "5").hide(moreFragment)
                    .add(R.id.fragment_container, itemsFragment, "4").hide(itemsFragment)
                    .add(R.id.fragment_container, todayFragment, "3").hide(todayFragment)
                    .add(R.id.fragment_container, reportsFragment, "2").hide(reportsFragment)
                    .add(R.id.fragment_container, counterFragment, "1")
                    .commit();
            android.util.Log.d("MainActivity", "✓ Fragments added to container");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error adding fragments", e);
        }
        
        // تحديث header النافذة الجانبية
        updateNavigationHeader();
        android.util.Log.d("MainActivity", "✓ Navigation header updated");
    }

    private void showSelectCustomersLocationPage() {
        Intent intent = new Intent(this, CustomersMapActivity.class);
        startActivity(intent);
    }

    private void openAllInvoicesActivity() {
        Intent intent = new Intent(this, AllInvoicesActivity.class);
        startActivity(intent);
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
        } else if (id == R.id.action_all_invoices) {
            openAllInvoicesActivity();
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
        // Debug log
        android.util.Log.d("MainActivity", "showAddCustomerDialog called");
        
        SelectCustomerDialog dialog = new SelectCustomerDialog();
        dialog.setOnCustomerSelectedListener(customer -> {
            // Debug log
            android.util.Log.d("MainActivity", "Customer selected: " + customer.getName());
            
            // تعيين العميل في CounterFragment
            CounterFragment.setCustomer(customer);
            
            // تحديث اسم العميل في واجهة CounterFragment إذا كانت نشطة
            if (CounterFragment.activeInstance != null) {
                TextView customerTextView = findViewById(R.id.invoiceCustomerTextView);
                if (customerTextView != null) {
                    customerTextView.setText("الزبون: " + customer.getName());
                }
            }
            
            Toast.makeText(this, "تم اختيار العميل: " + customer.getName(), Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "SelectCustomerDialog");
        
        // Debug log
        android.util.Log.d("MainActivity", "showAddCustomerDialog finished");
    }

    // صفحة اختيار الموقع الجغرافي للعميل
    private void showSelectCustomerLocationPage() {
        Intent intent = new Intent(this, SelectCustomerLocationActivity.class);
        startActivity(intent);
    }

    public void logoutUser() {
        // عرض حوار تأكيد تسجيل الخروج
        new android.app.AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage("هل أنت متأكد من تسجيل الخروج؟")
                .setPositiveButton("نعم", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }
    
    /**
     * تنفيذ عملية تسجيل الخروج الفعلية
     */
    private void performLogout() {
        try {
            // تنظيف جلسة المستخدم أولاً
            UserSession userSession = UserSession.getInstance(this);
            userSession.logoutUser();
            
            // تسجيل الخروج من Firebase Auth
            mAuth.signOut();
            
            // تسجيل الخروج من Google Sign In
            GoogleSignIn.getClient(this, new GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut();

            // الانتقال إلى شاشة تسجيل الدخول
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error during logout", e);
            android.widget.Toast.makeText(this, "خطأ في تسجيل الخروج", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * تحديث header النافذة الجانبية مع معلومات المستخدم الحالي
     */
    private void updateNavigationHeader() {
        try {
            UserSession userSession = UserSession.getInstance(this);
            if (userSession.isLoggedIn()) {
                android.view.View headerView = navigationView.getHeaderView(0);
                
                TextView userNameTextView = headerView.findViewById(R.id.user_name);
                TextView userEmailTextView = headerView.findViewById(R.id.user_email);
                
                com.example.posapp.model.User currentUser = userSession.getCurrentUser();
                
                if (userNameTextView != null) {
                    String displayName = currentUser.getFullName();
                    if (userSession.isGuestUser()) {
                        displayName += " (ضيف)";
                    }
                    userNameTextView.setText(displayName);
                }
                
                if (userEmailTextView != null) {
                    String statusText = currentUser.getEmail() + "\n" + userSession.getUserStatusMessage();
                    userEmailTextView.setText(statusText);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error updating navigation header", e);
        }
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

    private void navigateToInventoryManagement() {
        // إنشاء Fragment إدارة المخزون إذا لم يكن موجوداً
        if (inventoryManagementFragment == null) {
            inventoryManagementFragment = new InventoryManagementFragment();
        }

        isInDrawerFragment = true;

        // استخدام نفس آلية show/hide
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // إخفاء جميع الـ Fragments
        transaction.hide(activeFragment);

        // إضافة أو إظهار Fragment إدارة المخزون
        if (!inventoryManagementFragment.isAdded()) {
            transaction.add(R.id.fragment_container, inventoryManagementFragment, "inventory");
        } else {
            transaction.show(inventoryManagementFragment);
        }

        transaction.addToBackStack(null);
        transaction.commit();

        activeFragment = inventoryManagementFragment;

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
    protected void onResume() {
        super.onResume();
        
        // فحص حالة تسجيل الدخول عند العودة للتطبيق
        UserSession userSession = UserSession.getInstance(this);
        if (!userSession.isLoggedIn() || !userSession.validateSession()) {
            // إذا انتهت الجلسة، ارجع إلى شاشة تسجيل الدخول
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // تحديث header النافذة الجانبية
        updateNavigationHeader();
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