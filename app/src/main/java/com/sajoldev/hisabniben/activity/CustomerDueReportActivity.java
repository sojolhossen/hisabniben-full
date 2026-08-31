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
import com.sajoldev.hisabniben.adapter.CustomerAdapter;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class CustomerDueReportActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalCustomerDue, tvBakiCustomerCount, tvEmptyCustomerDue;
    private TextInputEditText etSearchCustomer;
    private RecyclerView rvCustomerDueReport;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private CustomerAdapter adapter;

    private List<Customer> allCustomers = new ArrayList<>();
    private List<Customer> filteredCustomers = new ArrayList<>();
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_customer_due_report);

        sessionManager = SessionManager.getInstance(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupWindowInsets();
        setupRecyclerView();
        setupListeners();
        loadCustomerDueData();
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
        tvTotalCustomerDue = findViewById(R.id.tvTotalCustomerDue);
        tvBakiCustomerCount = findViewById(R.id.tvBakiCustomerCount);
        tvEmptyCustomerDue = findViewById(R.id.tvEmptyCustomerDue);
        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        rvCustomerDueReport = findViewById(R.id.rvCustomerDueReport);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("কাস্টমার বাকি রিপোর্ট (Customer Due)");
        }
        toolbar.setTitleTextColor(getResources().getColor(R.color.text_primary));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new CustomerAdapter(new ArrayList<>(), (CustomerAdapter.OnCustomerClickListener) null);
        rvCustomerDueReport.setLayoutManager(new LinearLayoutManager(this));
        rvCustomerDueReport.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCustomerDueData);

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim().toLowerCase() : "";
                applySearchFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCustomerDueData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        db.collection("customers")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(snap -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                allCustomers.clear();
                double totalDue = 0;
                int countWithDue = 0;

                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Customer c = doc.toObject(Customer.class);
                    if (c != null) {
                        if (c.getId() == null) c.setId(doc.getId());
                        allCustomers.add(c);
                        if (c.getBaki() > 0) {
                            totalDue += c.getBaki();
                            countWithDue++;
                        }
                    }
                }
                allCustomers.sort((c1, c2) -> Double.compare(c2.getBaki(), c1.getBaki()));

                tvTotalCustomerDue.setText(UnitConverterHelper.formatCurrency(totalDue));
                tvBakiCustomerCount.setText(countWithDue + " জন কাস্টমারের কাছে বাকি রয়েছে");

                applySearchFilter();
            }).addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CustomerDueReportActivity.this, "কাস্টমার লোড করতে ব্যর্থ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void applySearchFilter() {
        filteredCustomers.clear();
        for (Customer c : allCustomers) {
            if (currentQuery.isEmpty()) {
                filteredCustomers.add(c);
            } else {
                boolean matchName = c.getName() != null && c.getName().toLowerCase().contains(currentQuery);
                boolean matchPhone = c.getPhone() != null && c.getPhone().contains(currentQuery);

                if (matchName || matchPhone) {
                    filteredCustomers.add(c);
                }
            }
        }

        adapter.updateData(filteredCustomers);

        if (filteredCustomers.isEmpty()) {
            tvEmptyCustomerDue.setVisibility(View.VISIBLE);
            rvCustomerDueReport.setVisibility(View.GONE);
        } else {
            tvEmptyCustomerDue.setVisibility(View.GONE);
            rvCustomerDueReport.setVisibility(View.VISIBLE);
        }
    }
}
