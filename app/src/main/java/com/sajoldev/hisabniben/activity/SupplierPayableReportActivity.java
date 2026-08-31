package com.sajoldev.hisabniben.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.SupplierAdapter;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class SupplierPayableReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalSupplierPayable, tvPayableSupplierCount, tvEmptySupplierPayable;
    private TextInputEditText etSearchSupplier;
    private RecyclerView rvSupplierPayableReport;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private SupplierAdapter adapter;

    private List<Supplier> allSuppliers = new ArrayList<>();
    private List<Supplier> filteredSuppliers = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_supplier_payable_report);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadSupplierPayableData();
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
        tvTotalSupplierPayable = findViewById(R.id.tvTotalSupplierPayable);
        tvPayableSupplierCount = findViewById(R.id.tvPayableSupplierCount);
        tvEmptySupplierPayable = findViewById(R.id.tvEmptySupplierPayable);
        etSearchSupplier = findViewById(R.id.etSearchSupplier);
        rvSupplierPayableReport = findViewById(R.id.rvSupplierPayableReport);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("মহাজন পাওনা রিপোর্ট (Supplier Payable)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new SupplierAdapter(new ArrayList<>(), (SupplierAdapter.OnSupplierClickListener) null);
        rvSupplierPayableReport.setLayoutManager(new LinearLayoutManager(this));
        rvSupplierPayableReport.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadSupplierPayableData);

        etSearchSupplier.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applySearchFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSupplierPayableData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("suppliers")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(snap -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                allSuppliers.clear();
                double totalPayable = 0;
                int countWithPayable = 0;

                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Supplier s = doc.toObject(Supplier.class);
                    if (s != null) {
                        if (s.getId() == null) s.setId(doc.getId());
                        allSuppliers.add(s);
                        if (s.getCurrentPayable() > 0) {
                            totalPayable += s.getCurrentPayable();
                            countWithPayable++;
                        }
                    }
                }
                allSuppliers.sort((s1, s2) -> Double.compare(s2.getCurrentPayable(), s1.getCurrentPayable()));

                tvTotalSupplierPayable.setText(UnitConverterHelper.formatCurrency(totalPayable));
                tvPayableSupplierCount.setText(countWithPayable + " জন মহাজনের পাওনা রয়েছে");

                applySearchFilter();
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(SupplierPayableReportActivity.this, "মহাজন লোড করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void applySearchFilter() {
        filteredSuppliers.clear();
        for (Supplier s : allSuppliers) {
            if (currentQuery.isEmpty()) {
                filteredSuppliers.add(s);
            } else {
                boolean matchName = s.getName() != null && s.getName().toLowerCase().contains(currentQuery);
                boolean matchBiz = s.getBusinessName() != null && s.getBusinessName().toLowerCase().contains(currentQuery);
                boolean matchPhone = s.getPhone() != null && s.getPhone().contains(currentQuery);

                if (matchName || matchBiz || matchPhone) {
                    filteredSuppliers.add(s);
                }
            }
        }

        adapter.updateData(filteredSuppliers);

        if (filteredSuppliers.isEmpty()) {
            tvEmptySupplierPayable.setVisibility(View.VISIBLE);
            rvSupplierPayableReport.setVisibility(View.GONE);
        } else {
            tvEmptySupplierPayable.setVisibility(View.GONE);
            rvSupplierPayableReport.setVisibility(View.VISIBLE);
        }
    }
}
