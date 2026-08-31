package com.sajoldev.hisabniben.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.WalletAccountAdapter;
import com.sajoldev.hisabniben.dialog.AddWalletAccountDialog;
import com.sajoldev.hisabniben.model.WalletAccount;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class WalletAccountsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvAccounts;
    private MaterialButton btnAddAccount;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private WalletAccountAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_wallet_accounts);

        sessionManager = SessionManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadAccountsData();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            int topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(view.getPaddingLeft(), topInsets, view.getPaddingRight(), view.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvAccounts = findViewById(R.id.rvAccounts);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("ক্যাশ ও ওয়ালেট অ্যাকাউন্টস (Cash Accounts)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new WalletAccountAdapter(this);
        adapter.setOnAccountClickListener(account -> {
            Toast.makeText(this, account.getAccountName() + ": " + UnitConverterHelper.formatCurrency(account.getCurrentBalance()), Toast.LENGTH_SHORT).show();
        });
        adapter.setOnAccountActionListener(new WalletAccountAdapter.OnAccountActionListener() {
            @Override
            public void onEditClick(WalletAccount account) {
                openEditAccountDialog(account);
            }

            @Override
            public void onDeleteClick(WalletAccount account) {
                confirmAndDeleteAccount(account);
            }
        });
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(adapter);
    }

    private void openEditAccountDialog(WalletAccount account) {
        AddWalletAccountDialog dialog = AddWalletAccountDialog.newInstance(account);
        dialog.setOnAccountSavedListener(this::loadAccountsData);
        dialog.show(getSupportFragmentManager(), "EditAccount");
    }

    private void confirmAndDeleteAccount(WalletAccount account) {
        if (account == null || account.getAccountId() == null) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("অ্যাকাউন্ট মুছে ফেলার নিশ্চিতকরণ")
            .setMessage("আপনি কি নিশ্চিত যে '" + account.getAccountName() + "' অ্যাকাউন্টটি মুছে ফেলতে চান?\n\n⚠️ সতর্কতা: এই অ্যাকাউন্টটি মুছে ফেললে এর সাথে সম্পর্কিত লেনদেনের তথ্য মুছে যাবে না, তবে ফিল্টারিং-এ প্রভাব পড়তে পারে।")
            .setPositiveButton("হ্যাঁ, মুছে ফেলুন", (dialog, which) -> {
                deleteAccountFromFirestore(account);
            })
            .setNegativeButton("বাতিল", null)
            .show();
    }

    private void deleteAccountFromFirestore(WalletAccount account) {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        firestoreManager.deleteWalletAccount(userId, account.getAccountId(), new FirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WalletAccountsActivity.this, "'" + account.getAccountName() + "' অ্যাকাউন্টটি সফলভাবে মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show();
                loadAccountsData();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(WalletAccountsActivity.this, "মুছে ফেলতে ব্যর্থ: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadAccountsData);

        btnAddAccount.setOnClickListener(v -> {
            AddWalletAccountDialog dialog = new AddWalletAccountDialog();
            dialog.setOnAccountSavedListener(this::loadAccountsData);
            dialog.show(getSupportFragmentManager(), "AddAccount");
        });
    }

    private void loadAccountsData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        firestoreManager.getWalletAccounts(userId, new FirestoreManager.FirestoreListCallback<WalletAccount>() {
            @Override
            public void onSuccess(List<WalletAccount> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                List<WalletAccount> list = result != null ? result : new ArrayList<>();
                adapter.setAccounts(list);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(WalletAccountsActivity.this, "অ্যাকাউন্ট লোড করতে ব্যর্থ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
