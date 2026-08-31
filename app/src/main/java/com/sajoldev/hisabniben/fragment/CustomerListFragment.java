package com.sajoldev.hisabniben.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.CustomerAdapter;
import com.sajoldev.hisabniben.dialog.AddCustomerDialog;
import com.sajoldev.hisabniben.dialog.AddSaleDialog;
import com.sajoldev.hisabniben.dialog.CustomerDetailsBottomSheet;
import com.sajoldev.hisabniben.dialog.CustomerPaymentDialog;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class CustomerListFragment extends Fragment implements CustomerAdapter.OnCustomerActionListener {

    private TextView tvSummaryTotalCustomers, tvSummaryTotalDue, tvSummaryDueCustomersCount;
    private MaterialButton btnAddCustomerHeader, btnEmptyAddCustomer;
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;
    private MaterialCardView btnFilterSort;
    private ChipGroup chipGroupCustomerStatus;
    private RecyclerView rvCustomers;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;
    private ExtendedFloatingActionButton fabAdd;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private CustomerAdapter adapter;

    private List<Customer> allCustomers = new ArrayList<>();
    private List<Customer> filteredCustomers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadCustomers();
    }

    private void initViews(View view) {
        tvSummaryTotalCustomers = view.findViewById(R.id.tvSummaryTotalCustomers);
        tvSummaryTotalDue = view.findViewById(R.id.tvSummaryTotalDue);
        tvSummaryDueCustomersCount = view.findViewById(R.id.tvSummaryDueCustomersCount);
        btnAddCustomerHeader = view.findViewById(R.id.btnAddCustomerHeader);

        tilSearch = view.findViewById(R.id.tilSearch);
        etSearch = view.findViewById(R.id.etSearch);
        btnFilterSort = view.findViewById(R.id.btnFilterSort);
        chipGroupCustomerStatus = view.findViewById(R.id.chipGroupCustomerStatus);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rvCustomers = view.findViewById(R.id.rvCustomers);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        btnEmptyAddCustomer = view.findViewById(R.id.btnEmptyAddCustomer);
        fabAdd = view.findViewById(R.id.fabAdd);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new CustomerAdapter(filteredCustomers, this);
        rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCustomers.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCustomers);

        btnAddCustomerHeader.setOnClickListener(v -> com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(requireContext(), this::showAddCustomerDialog));
        btnEmptyAddCustomer.setOnClickListener(v -> com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(requireContext(), this::showAddCustomerDialog));
        fabAdd.setOnClickListener(v -> com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(requireContext(), this::showAddCustomerDialog));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupCustomerStatus.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
    }

    private void loadCustomers() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        firestoreManager.getCustomersByUser(userId, new FirestoreManager.FirestoreListCallback<Customer>() {
            @Override
            public void onSuccess(List<Customer> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                allCustomers = result;

                updateSummaryHeader();
                applyFilters();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "ত্রুটি: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryHeader() {
        int totalCustomers = allCustomers.size();
        double totalDue = 0;
        int dueCustomersCount = 0;

        for (Customer c : allCustomers) {
            if (c.getBaki() > 0) {
                totalDue += c.getBaki();
                dueCustomersCount++;
            }
        }

        tvSummaryTotalCustomers.setText(String.valueOf(totalCustomers));
        tvSummaryTotalDue.setText(UnitConverterHelper.formatCurrency(totalDue));
        tvSummaryDueCustomersCount.setText(dueCustomersCount + " জন");
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        int checkedChipId = chipGroupCustomerStatus.getCheckedChipId();

        filteredCustomers.clear();

        for (Customer customer : allCustomers) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;

            if (!query.isEmpty()) {
                String name = customer.getName() != null ? customer.getName().toLowerCase() : "";
                String business = customer.getBusinessName() != null ? customer.getBusinessName().toLowerCase() : "";
                String phone = customer.getPhone() != null ? customer.getPhone().toLowerCase() : "";
                matchesSearch = name.contains(query) || business.contains(query) || phone.contains(query);
            }

            if (checkedChipId == R.id.chipFilterHasDue) {
                matchesStatus = customer.getBaki() > 0;
            } else if (checkedChipId == R.id.chipFilterNoDue) {
                matchesStatus = customer.getBaki() <= 0;
            } else if (checkedChipId == R.id.chipFilterOverdue) {
                // Overdue: baki > 0 and updatedAt older than 30 days
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                matchesStatus = customer.getBaki() > 0 && customer.getUpdatedAt() < thirtyDaysAgo;
            }

            if (matchesSearch && matchesStatus) {
                filteredCustomers.add(customer);
            }
        }

        adapter.updateData(filteredCustomers);
        layoutEmptyState.setVisibility(filteredCustomers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddCustomerDialog() {
        String userId = sessionManager.getUserId();
        AddCustomerDialog dialog = AddCustomerDialog.newInstance(userId);
        dialog.setOnCustomerAddedListener((id, name) -> loadCustomers());
        dialog.show(getChildFragmentManager(), "AddCustomerFromList");
    }

    @Override
    public void onCustomerClick(Customer customer) {
        CustomerDetailsBottomSheet bottomSheet = CustomerDetailsBottomSheet.newInstance(customer);
        bottomSheet.setOnCustomerActionListener(new CustomerDetailsBottomSheet.OnCustomerActionListener() {
            @Override public void onEditClick(Customer customer) { loadCustomers(); }
            @Override public void onDeleteClick(Customer customer) { loadCustomers(); }
            @Override public void onActivityClick(Customer customer) { loadCustomers(); }
        });
        bottomSheet.show(getChildFragmentManager(), "CustomerDetails");
    }

    @Override
    public void onSaleClick(Customer customer) {
        AddSaleDialog dialog = new AddSaleDialog();
        dialog.setOnSaleSavedListener(this::loadCustomers);
        dialog.show(getChildFragmentManager(), "AddSaleFromCustomerCard");
    }

    @Override
    public void onCollectionClick(Customer customer) {
        CustomerPaymentDialog dialog = CustomerPaymentDialog.newInstance(customer);
        dialog.setOnPaymentSavedListener(this::loadCustomers);
        dialog.show(getChildFragmentManager(), "PaymentFromCustomerCard");
    }

    @Override
    public void onSmsClick(Customer customer) {
        CustomerDetailsBottomSheet bottomSheet = CustomerDetailsBottomSheet.newInstance(customer);
        bottomSheet.show(getChildFragmentManager(), "CustomerDetailsForSms");
    }

    @Override
    public void onLedgerClick(Customer customer) {
        onCustomerClick(customer);
    }
}
