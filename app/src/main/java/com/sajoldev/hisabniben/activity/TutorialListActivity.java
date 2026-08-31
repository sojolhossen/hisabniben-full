package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.TutorialAdapter;
import com.sajoldev.hisabniben.model.TutorialVideo;
import com.sajoldev.hisabniben.util.FirestoreManager;

import java.util.ArrayList;
import java.util.List;

public class TutorialListActivity extends AppCompatActivity {

    public static final String EXTRA_TUTORIAL_VIDEO = "extra_tutorial_video";

    private MaterialToolbar toolbar;
    private EditText etSearch;
    private ChipGroup chipGroupCategory;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty, layoutError;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;

    private TutorialAdapter adapter;
    private final List<TutorialVideo> masterList = new ArrayList<>();
    private String selectedCategory = TutorialVideo.CAT_ALL;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_tutorial_list);

        initViews();
        setupWindowInsets();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        loadTutorials();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutError = findViewById(R.id.layoutError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetry = findViewById(R.id.btnRetry);
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, windowInsets) -> {
                int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int bottomInset = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                v.setPadding(0, topInset, 0, bottomInset);
                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("🎥 ভিডিও টিউটোরিয়াল");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new TutorialAdapter(this, new ArrayList<>(), video -> {
            Intent intent = new Intent(TutorialListActivity.this, TutorialVideoActivity.class);
            intent.putExtra(EXTRA_TUTORIAL_VIDEO, video);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadTutorials);
        btnRetry.setOnClickListener(v -> loadTutorials());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTutorials();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = TutorialVideo.CAT_ALL;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipCatGettingStarted) selectedCategory = TutorialVideo.CAT_GETTING_STARTED;
                else if (checkedId == R.id.chipCatSales) selectedCategory = TutorialVideo.CAT_SALES;
                else if (checkedId == R.id.chipCatPurchase) selectedCategory = TutorialVideo.CAT_PURCHASE;
                else if (checkedId == R.id.chipCatStock) selectedCategory = TutorialVideo.CAT_STOCK;
                else if (checkedId == R.id.chipCatCustomer) selectedCategory = TutorialVideo.CAT_CUSTOMER;
                else if (checkedId == R.id.chipCatSupplier) selectedCategory = TutorialVideo.CAT_SUPPLIER;
                else if (checkedId == R.id.chipCatWallet) selectedCategory = TutorialVideo.CAT_WALLET;
                else if (checkedId == R.id.chipCatExpense) selectedCategory = TutorialVideo.CAT_EXPENSE;
                else if (checkedId == R.id.chipCatReports) selectedCategory = TutorialVideo.CAT_REPORTS;
                else if (checkedId == R.id.chipCatSms) selectedCategory = TutorialVideo.CAT_SMS;
                else if (checkedId == R.id.chipCatSubscription) selectedCategory = TutorialVideo.CAT_SUBSCRIPTION;
                else if (checkedId == R.id.chipCatSettings) selectedCategory = TutorialVideo.CAT_SETTINGS;
                else if (checkedId == R.id.chipCatOther) selectedCategory = TutorialVideo.CAT_OTHER;
                else selectedCategory = TutorialVideo.CAT_ALL;
            }
            filterTutorials();
        });
    }

    private void loadTutorials() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        FirestoreManager.getInstance().getPublishedTutorialVideos(new FirestoreManager.FirestoreListCallback<TutorialVideo>() {
            @Override
            public void onSuccess(List<TutorialVideo> list) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                masterList.clear();
                if (list != null) {
                    masterList.addAll(list);
                }

                filterTutorials();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (masterList.isEmpty()) {
                    layoutError.setVisibility(View.VISIBLE);
                    tvErrorMessage.setText("ভিডিও তালিকা লোড করা যাচ্ছে না (" + error + ")");
                }
            }
        });
    }

    private void filterTutorials() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";

        List<TutorialVideo> filtered = new ArrayList<>();
        for (TutorialVideo video : masterList) {
            boolean matchesCategory = TutorialVideo.CAT_ALL.equals(selectedCategory) ||
                    (video.getCategory() != null && video.getCategory().equalsIgnoreCase(selectedCategory));

            boolean matchesSearch = query.isEmpty() ||
                    (video.getTitle() != null && video.getTitle().toLowerCase().contains(query)) ||
                    (video.getDescription() != null && video.getDescription().toLowerCase().contains(query)) ||
                    (video.getCategoryLabelBangla() != null && video.getCategoryLabelBangla().toLowerCase().contains(query));

            if (matchesCategory && matchesSearch) {
                filtered.add(video);
            }
        }

        adapter.updateList(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
