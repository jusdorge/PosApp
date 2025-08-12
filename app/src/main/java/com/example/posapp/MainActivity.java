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
import com.example.posapp.utils.LanguageManager;

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
    
    // Counter badge management
    private static MainActivity instance;
    
    // Network status management
    private NetworkStatusManager networkStatusManager;
    private TextView networkStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تطبيق اللغة المختارة قبل إعداد UI
        LanguageManager languageManager = LanguageManager.getInstance(this);
        languageManager.applyLanguage(this);
        android.util.Log.d("MainActivity", "✓ Language applied: " + languageManager.getLanguage());
        
        // Set instance for static access
        instance = this;
        
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
            
            // إعداد مراقب حالة الشبكة
            setupNetworkStatusManager();
            android.util.Log.d("MainActivity", "✓ Network status manager setup completed");
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error in onCreate", e);
            Toast.makeText(this, getString(R.string.app_init_error, e.getMessage()), Toast.LENGTH_LONG).show();
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
            android.util.Log.d("MainActivity", "📱 Bottom navigation item selected: " + 
                getResources().getResourceEntryName(item.getItemId()));
                
            // إذا كنا في شاشة من القائمة الجانبية، نعود أولاً إلى الـ Fragments الأساسية
            if (isInDrawerFragment) {
                android.util.Log.d("MainActivity", "🔄 Returning from drawer fragment to main fragments");
                isInDrawerFragment = false;
            }

            Fragment selectedFragment = null;
            String fragmentName = "";

            switch (item.getItemId()) {
                case R.id.nav_reports:
                    selectedFragment = reportsFragment;
                    fragmentName = "Reports";
                    break;
                case R.id.nav_today:
                    selectedFragment = todayFragment;
                    fragmentName = "Today";
                    break;
                case R.id.nav_counter:
                    selectedFragment = counterFragment;
                    fragmentName = "Counter";
                    break;
                case R.id.nav_items:
                    selectedFragment = itemsFragment;
                    fragmentName = "Items";
                    break;
                case R.id.nav_more:
                    selectedFragment = moreFragment;
                    fragmentName = "More";
                    break;
                default:
                    android.util.Log.w("MainActivity", "⚠️ Unknown navigation item selected: " + item.getItemId());
                    break;
            }

            if (selectedFragment != null) {
                android.util.Log.d("MainActivity", "🎯 Switching to " + fragmentName + " fragment");
                switchToFragment(selectedFragment);
            } else {
                android.util.Log.e("MainActivity", "❌ Selected fragment is null for " + fragmentName);
                return false;
            }

            return true;
        });
        android.util.Log.d("MainActivity", "✓ Navigation listeners setup");

        try {
            // مسح الـ container أولاً لتجنب التداخل
            fragmentManager.beginTransaction()
                    .disallowAddToBackStack()
                    .commit();
            
            // إضافة جميع الFragments الأساسية مع إخفائها (ما عدا CounterFragment)
            FragmentTransaction initialTransaction = fragmentManager.beginTransaction();
            
            // إضافة الـ fragments مع tags واضحة
            initialTransaction.add(R.id.fragment_container, counterFragment, "counter");
            initialTransaction.add(R.id.fragment_container, reportsFragment, "reports").hide(reportsFragment);
            initialTransaction.add(R.id.fragment_container, todayFragment, "today").hide(todayFragment);
            initialTransaction.add(R.id.fragment_container, itemsFragment, "items").hide(itemsFragment);
            initialTransaction.add(R.id.fragment_container, moreFragment, "more").hide(moreFragment);
            
            // تعيين انتقال سلس
            initialTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            initialTransaction.commitNow();
            
            android.util.Log.d("MainActivity", "✓ All fragments added successfully");
            android.util.Log.d("MainActivity", "✓ Active fragment: " + activeFragment.getClass().getSimpleName());
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error adding fragments", e);
            // إعادة المحاولة مع طريقة أبسط
            resetFragments();
        }
        
        // تحديث header النافذة الجانبية
        updateNavigationHeader();
        android.util.Log.d("MainActivity", "✓ Navigation header updated");
        
        // فحص ما إذا كان يجب التبديل إلى الكاونتر
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("switch_to_counter", false)) {
            android.util.Log.d("MainActivity", "Switching to counter fragment as requested");
            bottomNavigationView.setSelectedItemId(R.id.nav_counter);
        }
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
            android.util.Log.d("MainActivity", "Switching from " + 
                activeFragment.getClass().getSimpleName() + " to " + 
                fragment.getClass().getSimpleName());
                
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // إخفاء الـ Fragment النشط الحالي بشكل صريح
            if (activeFragment != null && activeFragment.isAdded()) {
                transaction.hide(activeFragment);
                android.util.Log.d("MainActivity", "Hiding " + activeFragment.getClass().getSimpleName());
            }

            // إذا كان الـ Fragment المطلوب غير مضاف بعد، أضفه
            if (!fragment.isAdded()) {
                transaction.add(R.id.fragment_container, fragment);
                android.util.Log.d("MainActivity", "Adding " + fragment.getClass().getSimpleName());
            }

            // إظهار الـ Fragment المطلوب
            transaction.show(fragment);
            android.util.Log.d("MainActivity", "Showing " + fragment.getClass().getSimpleName());

            // استخدام setTransition لضمان انتقال سلس
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            
            try {
                transaction.commitNow(); // استخدام commitNow بدلاً من commit لضمان التنفيذ الفوري
                activeFragment = fragment;
                android.util.Log.d("MainActivity", "✅ Fragment switch completed successfully");
                
                // فحص وإصلاح أي تداخل محتمل بعد التنقل
                checkAndFixFragmentOverlap();
                
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "❌ Error switching fragments", e);
                // fallback إلى commit العادي
                transaction.commit();
                activeFragment = fragment;
                
                // فحص التداخل حتى في حالة الخطأ
                checkAndFixFragmentOverlap();
            }
        } else {
            android.util.Log.d("MainActivity", "Fragment " + fragment.getClass().getSimpleName() + " is already active");
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
            showLanguageSelectionDialog();
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
                    customerTextView.setText(getString(R.string.customer_label, customer.getName()));
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

    /**
     * عرض حوار تغيير اللغة وتطبيقها
     */
    private void showLanguageSelectionDialog() {
        LanguageSelectionDialog dialog = LanguageSelectionDialog.newInstance();
        dialog.setOnLanguageSelectedListener(selectedLanguageCode -> {
            // تم حفظ اللغة داخل الحوار بالفعل عبر LanguageManager
            // أعد إنشاء النشاط لتطبيق موارد اللغة الجديدة
            recreate();
        });
        dialog.show(getSupportFragmentManager(), "LanguageSelectionDialog");
    }

    public void logoutUser() {
        // عرض حوار تأكيد تسجيل الخروج
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_confirm_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton(getString(R.string.cancel), null)
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
        
        // فحص وإصلاح أي تداخل في الـ fragments عند العودة للتطبيق
        if (fragmentManager != null && activeFragment != null) {
            checkAndFixFragmentOverlap();
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
    
    /**
     * Update counter badge with item count
     */
    public void updateCounterBadge(int itemCount) {
        if (bottomNavigationView != null) {
            if (itemCount > 0) {
                com.google.android.material.badge.BadgeDrawable badge = 
                    bottomNavigationView.getOrCreateBadge(R.id.nav_counter);
                badge.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                badge.setNumber(itemCount);
                badge.setVisible(true);
            } else {
                bottomNavigationView.removeBadge(R.id.nav_counter);
            }
        }
    }
    
    /**
     * Get instance for static access
     */
    public static MainActivity getInstance() {
        return instance;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        
        // إيقاف مراقبة الشبكة
        if (networkStatusManager != null) {
            networkStatusManager.stopNetworkMonitoring();
        }
    }
    
    /**
     * إعداد مراقب حالة الشبكة
     */
    private void setupNetworkStatusManager() {
        try {
            networkStatusManager = NetworkStatusManager.getInstance(this);
            
            // إضافة مستمع لتغييرات حالة الشبكة
            networkStatusManager.addNetworkStatusListener(new NetworkStatusManager.NetworkStatusListener() {
                @Override
                public void onNetworkAvailable() {
                    runOnUiThread(() -> {
                        android.util.Log.d("MainActivity", "🌐 Network is now available - Data will sync");
                        showNetworkStatusMessage(getString(R.string.network_reconnected), false);
                    });
                }
                
                @Override
                public void onNetworkLost() {
                    runOnUiThread(() -> {
                        android.util.Log.d("MainActivity", "🚫 Network lost - Working in offline mode");
                        showNetworkStatusMessage(getString(R.string.network_lost), true);
                    });
                }
            });
            
            // عرض الحالة الأولية
            boolean isOnline = networkStatusManager.isOnline();
            String message = isOnline ? 
                getString(R.string.network_online) : 
                getString(R.string.working_offline);
            showNetworkStatusMessage(message, !isOnline);
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error setting up network status manager", e);
        }
    }
    
    /**
     * عرض رسالة حالة الشبكة
     */
    private void showNetworkStatusMessage(String message, boolean isOffline) {
        Toast.makeText(this, message, isOffline ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
    
    /**
     * إعادة تعيين الـ fragments في حالة حدوث مشكلة في التداخل
     */
    private void resetFragments() {
        android.util.Log.d("MainActivity", "🔄 Resetting fragments to fix overlap issue...");
        
        try {
            // إزالة جميع الـ fragments الموجودة
            FragmentTransaction clearTransaction = fragmentManager.beginTransaction();
            
            if (counterFragment.isAdded()) clearTransaction.remove(counterFragment);
            if (todayFragment.isAdded()) clearTransaction.remove(todayFragment);
            if (itemsFragment.isAdded()) clearTransaction.remove(itemsFragment);
            if (reportsFragment.isAdded()) clearTransaction.remove(reportsFragment);
            if (moreFragment.isAdded()) clearTransaction.remove(moreFragment);
            
            clearTransaction.commitNow();
            
            // إعادة إنشاء الـ fragments
            counterFragment = new CounterFragment();
            todayFragment = new TodayFragment();
            itemsFragment = new ItemsFragment();
            reportsFragment = new ReportsFragment();
            moreFragment = new MoreFragment();
            activeFragment = counterFragment;
            
            // إضافتهم مرة أخرى
            FragmentTransaction addTransaction = fragmentManager.beginTransaction();
            addTransaction.add(R.id.fragment_container, counterFragment, "counter");
            addTransaction.add(R.id.fragment_container, todayFragment, "today").hide(todayFragment);
            addTransaction.add(R.id.fragment_container, itemsFragment, "items").hide(itemsFragment);
            addTransaction.add(R.id.fragment_container, reportsFragment, "reports").hide(reportsFragment);
            addTransaction.add(R.id.fragment_container, moreFragment, "more").hide(moreFragment);
            addTransaction.commitNow();
            
            android.util.Log.d("MainActivity", "✅ Fragments reset successfully");
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error resetting fragments", e);
            Toast.makeText(this, getString(R.string.error_reloading_screens), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * فحص حالة الـ fragments وإصلاح أي تداخل
     */
    private void checkAndFixFragmentOverlap() {
        android.util.Log.d("MainActivity", "🔍 Checking for fragment overlap...");
        
        int visibleCount = 0;
        String visibleFragments = "";
        
        if (counterFragment != null && counterFragment.isAdded() && counterFragment.isVisible()) {
            visibleCount++;
            visibleFragments += "Counter ";
        }
        if (todayFragment != null && todayFragment.isAdded() && todayFragment.isVisible()) {
            visibleCount++;
            visibleFragments += "Today ";
        }
        if (itemsFragment != null && itemsFragment.isAdded() && itemsFragment.isVisible()) {
            visibleCount++;
            visibleFragments += "Items ";
        }
        if (reportsFragment != null && reportsFragment.isAdded() && reportsFragment.isVisible()) {
            visibleCount++;
            visibleFragments += "Reports ";
        }
        if (moreFragment != null && moreFragment.isAdded() && moreFragment.isVisible()) {
            visibleCount++;
            visibleFragments += "More ";
        }
        
        if (visibleCount > 1) {
            android.util.Log.w("MainActivity", "⚠️ Fragment overlap detected! Visible: " + visibleFragments);
            android.util.Log.w("MainActivity", "🔧 Attempting to fix overlap...");
            
            // إخفاء جميع الـ fragments ما عدا الـ active
            FragmentTransaction fixTransaction = fragmentManager.beginTransaction();
            
            if (counterFragment != activeFragment && counterFragment.isVisible()) {
                fixTransaction.hide(counterFragment);
            }
            if (todayFragment != activeFragment && todayFragment.isVisible()) {
                fixTransaction.hide(todayFragment);
            }
            if (itemsFragment != activeFragment && itemsFragment.isVisible()) {
                fixTransaction.hide(itemsFragment);
            }
            if (reportsFragment != activeFragment && reportsFragment.isVisible()) {
                fixTransaction.hide(reportsFragment);
            }
            if (moreFragment != activeFragment && moreFragment.isVisible()) {
                fixTransaction.hide(moreFragment);
            }
            
            // التأكد من أن الـ active fragment مرئي
            if (activeFragment != null && !activeFragment.isVisible()) {
                fixTransaction.show(activeFragment);
            }
            
            fixTransaction.commitNow();
            android.util.Log.d("MainActivity", "✅ Fragment overlap fixed");
            
        } else {
            android.util.Log.d("MainActivity", "✅ No fragment overlap detected");
        }
    }
}