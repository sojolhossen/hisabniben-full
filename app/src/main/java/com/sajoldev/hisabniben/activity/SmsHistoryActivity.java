package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.fragment.CustomSmsHistoryFragment;
import com.sajoldev.hisabniben.fragment.TransactionSmsHistoryFragment;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class SmsHistoryActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvCurrentSms;
    private MaterialButton btnBuySms;
    private EditText etSearch;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private FirebaseFirestore db;
    private String userId;
    private SmsPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_sms_history);

        db = FirebaseFirestore.getInstance();
        userId = SessionManager.getInstance(this).getUserId();

        initViews();
        setupWindowInsets();
        setupClickListeners();
        setupViewPagerAndTabs();
        setupSearchListener();

        loadCurrentSmsBalance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentSmsBalance();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            int topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            View headerView = findViewById(R.id.headerLayout);
            if (headerView != null) {
                headerView.setPadding(headerView.getPaddingLeft(), topInsets, headerView.getPaddingRight(), headerView.getPaddingBottom());
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCurrentSms = findViewById(R.id.tvCurrentSms);
        btnBuySms = findViewById(R.id.btnBuySms);
        etSearch = findViewById(R.id.etSearch);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnBuySms.setOnClickListener(v -> {
            startActivity(new Intent(this, BuySmsActivity.class));
        });
    }

    private void setupViewPagerAndTabs() {
        pagerAdapter = new SmsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setUserInputEnabled(true);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Custom SMS" : "Transaction SMS");
        }).attach();
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                if (pagerAdapter != null) {
                    pagerAdapter.filterAll(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCurrentSmsBalance() {
        if (userId == null) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long smsLimit = documentSnapshot.getLong("smsLimit");
                        int remaining = smsLimit != null ? smsLimit.intValue() : 10;
                        tvCurrentSms.setText(remaining + " SMS বাকি");
                    }
                });
    }

    private static class SmsPagerAdapter extends FragmentStateAdapter {
        private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

        public SmsPagerAdapter(FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = position == 0
                    ? CustomSmsHistoryFragment.newInstance()
                    : TransactionSmsHistoryFragment.newInstance();
            fragmentMap.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        public void filterAll(String query) {
            for (Fragment fragment : fragmentMap.values()) {
                if (fragment instanceof CustomSmsHistoryFragment) {
                    ((CustomSmsHistoryFragment) fragment).filter(query);
                } else if (fragment instanceof TransactionSmsHistoryFragment) {
                    ((TransactionSmsHistoryFragment) fragment).filter(query);
                }
            }
        }
    }
}