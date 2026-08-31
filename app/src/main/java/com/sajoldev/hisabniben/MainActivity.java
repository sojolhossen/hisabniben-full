package com.sajoldev.hisabniben;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.sajoldev.hisabniben.activity.BuySmsActivity;
import com.sajoldev.hisabniben.activity.CompleteProfileActivity;
import com.sajoldev.hisabniben.activity.LoginActivity;
import com.sajoldev.hisabniben.activity.NotificationActivity;
import com.sajoldev.hisabniben.activity.ReportsActivity;
import com.sajoldev.hisabniben.dialog.AddTransactionDialog;
import com.sajoldev.hisabniben.fragment.CustomerListFragment;
import com.sajoldev.hisabniben.fragment.DashboardFragment;
import com.sajoldev.hisabniben.fragment.SettingsFragment;
import com.sajoldev.hisabniben.fragment.StockFragment;
import com.sajoldev.hisabniben.fragment.SupplierListFragment;
import com.sajoldev.hisabniben.activity.TransactionHistoryActivity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sajoldev.hisabniben.model.User;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        com.sajoldev.hisabniben.util.ScreenSecurityHelper.allowScreenSharingAndRecording(this);

        if (!isInternetAvailable()) {
            showNoInternetDialog();
            return;
        }

        sessionManager = SessionManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        OneSignal.initWithContext(this);
        initPushNotifications();
        showMainContent();
    }

    private void initPushNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                if (sessionManager != null) {
                    sessionManager.setFcmToken(token);
                    String userId = sessionManager.getUserId();
                    if (userId != null && !userId.isEmpty()) {
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                                .update("fcmToken", token, "fcm_token", token);
                    }
                }
            }
        });
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.main);
        View fragmentContainer = findViewById(R.id.fragment_container);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, windowInsets) -> {
            int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottomInset = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            if (fragmentContainer != null) {
                fragmentContainer.setPadding(0, topInset, 0, 0);
            }

            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, bottomInset);
            }

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return networkCapabilities != null && (
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        }
        return false;
    }

    private void showNoInternetDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.dialog_no_internet);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.CENTER);
        }

        dialog.findViewById(R.id.btnRetry).setOnClickListener(v -> {
            dialog.dismiss();
            recreate();
        });

        dialog.findViewById(R.id.btnExit).setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
            System.exit(0);
        });

        dialog.show();
    }

    private void checkProfileInBackground() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            navigateToLogin();
            return;
        }

        firestoreManager.getUser(userId, new FirestoreManager.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    com.sajoldev.hisabniben.util.SubscriptionAccessManager.getInstance(MainActivity.this).setCurrentUser(user);
                    sessionManager.updatePremiumStatus(
                        user.isPremium(),
                        user.getSubscriptionExpiryDate() != null ? user.getSubscriptionExpiryDate() : 0
                    );
                    if (user.getSubscriptionPackageName() != null && !user.getSubscriptionPackageName().isEmpty()) {
                        sessionManager.setSubscriptionPackageName(user.getSubscriptionPackageName());
                    }
                    if (user.getStoreName() == null || user.getStoreName().isEmpty()) {
                        navigateToCompleteProfile();
                        return;
                    }
                    updateHeaderUserInfo(user);
                }
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    private void updateHeaderUserInfo(User user) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvStoreName = headerView.findViewById(R.id.tvNavHeaderStoreName);
            TextView tvUserPhone = headerView.findViewById(R.id.tvNavHeaderUserPhone);

            if (tvStoreName != null && user.getShopName() != null) {
                tvStoreName.setText(user.getShopName());
            }
            if (tvUserPhone != null && user.getPhone() != null) {
                tvUserPhone.setText("📱 " + user.getPhone());
            }
        }
    }

    private void showMainContent() {
        setContentView(R.layout.activity_main);
        initViews();
        setupWindowInsets();
        setupNavigation();
        loadFragment(new DashboardFragment());
        checkProfileInBackground();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_customers) {
                return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> loadFragment(new CustomerListFragment()));
            } else if (itemId == R.id.nav_stock) {
                return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> loadFragment(new StockFragment()));
            } else if (itemId == R.id.nav_suppliers) {
                return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> loadFragment(new SupplierListFragment()));
            } else if (itemId == R.id.nav_more) {
                openDrawer();
                return false;
            }
            return false;
        });
    }

    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public void navigateToTab(int itemId) {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(itemId);
        }
    }

    public void loadFragment(Fragment fragment) {
        currentFragment = fragment;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        closeDrawer();

        if (itemId == R.id.nav_drawer_dashboard) {
            navigateToTab(R.id.nav_home);
            return true;
        } else if (itemId == R.id.nav_drawer_sales) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> {
                Intent intent = new Intent(this, TransactionHistoryActivity.class);
                intent.putExtra(TransactionHistoryActivity.EXTRA_FILTER_TYPE, "SALE");
                startActivity(intent);
            });
        } else if (itemId == R.id.nav_drawer_purchases) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> {
                Intent intent = new Intent(this, TransactionHistoryActivity.class);
                intent.putExtra(TransactionHistoryActivity.EXTRA_FILTER_TYPE, "PURCHASE");
                startActivity(intent);
            });
        } else if (itemId == R.id.nav_drawer_stock) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> navigateToTab(R.id.nav_stock));
        } else if (itemId == R.id.nav_drawer_wallet) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> {
                startActivity(new Intent(this, com.sajoldev.hisabniben.activity.WalletDashboardActivity.class));
            });
        } else if (itemId == R.id.nav_drawer_history) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
        } else if (itemId == R.id.nav_drawer_money_receive) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> {
                AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_RECEIVE);
                dialog.show(getSupportFragmentManager(), "DrawerMoneyReceive");
            });
        } else if (itemId == R.id.nav_drawer_expenses) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> {
                AddTransactionDialog dialog = AddTransactionDialog.newInstance(AddTransactionDialog.MODE_EXPENSE);
                dialog.show(getSupportFragmentManager(), "DrawerExpense");
            });
        } else if (itemId == R.id.nav_drawer_customers) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> navigateToTab(R.id.nav_customers));
        } else if (itemId == R.id.nav_drawer_suppliers) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> navigateToTab(R.id.nav_suppliers));
        } else if (itemId == R.id.nav_drawer_reports) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> startActivity(new Intent(this, ReportsActivity.class)));
        } else if (itemId == R.id.nav_drawer_tutorials) {
            startActivity(new Intent(this, com.sajoldev.hisabniben.activity.TutorialListActivity.class));
            return true;
        } else if (itemId == R.id.nav_drawer_sms) {
            return com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(this, () -> startActivity(new Intent(this, BuySmsActivity.class)));
        } else if (itemId == R.id.nav_drawer_subscription) {
            startActivity(new Intent(this, com.sajoldev.hisabniben.activity.SubscriptionActivity.class));
            return true;
        } else if (itemId == R.id.nav_drawer_notifications) {
            startActivity(new Intent(this, NotificationActivity.class));
            return true;
        } else if (itemId == R.id.nav_drawer_settings) {
            loadFragment(new SettingsFragment());
            return true;
        } else if (itemId == R.id.nav_drawer_logout) {
            showLogoutDialog();
            return true;
        }

        return false;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("লগআউট করতে চান?")
                .setMessage("আপনি কি হিসাব নিবেন অ্যাকাউন্ট থেকে লগআউট করতে চান?")
                .setPositiveButton("লগআউট", (dialog, which) -> {
                    sessionManager.logout();
                    navigateToLogin();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCompleteProfile() {
        Intent intent = new Intent(MainActivity.this, CompleteProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && !sessionManager.isLoggedIn()) {
            navigateToLogin();
        }
    }
}
