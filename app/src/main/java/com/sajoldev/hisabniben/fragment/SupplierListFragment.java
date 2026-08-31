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
import com.sajoldev.hisabniben.adapter.SupplierAdapter;
import com.sajoldev.hisabniben.dialog.AddPurchaseDialog;
import com.sajoldev.hisabniben.dialog.AddSupplierDialog;
import com.sajoldev.hisabniben.dialog.SupplierPaymentDialog;
import com.sajoldev.hisabniben.model.Supplier;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class SupplierListFragment extends Fragment implements SupplierAdapter.OnSupplierActionListener {

    private TextView tvSummaryTotalSuppliers, tvSummaryTotalPayable, tvSummaryPayableSuppliersCount;
    private MaterialButton btnAddSupplierHeader, btnEmptyAddSupplier;
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;
    private MaterialCardView btnFilterSort;
    private ChipGroup chipGroupSupplierStatus;
    private RecyclerView rvSuppliers;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;
    private ExtendedFloatingActionButton fabAdd;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private SupplierAdapter adapter;

    private List<Supplier> allSuppliers = new ArrayList<>();
    private List<Supplier> filteredSuppliers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_supplier_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadSuppliers();
    }

    private void initViews(View view) {
        tvSummaryTotalSuppliers = view.findViewById(R.id.tvSummaryTotalSuppliers);
        tvSummaryTotalPayable = view.findViewById(R.id.tvSummaryTotalPayable);
        tvSummaryPayableSuppliersCount = view.findViewById(R.id.tvSummaryPayableSuppliersCount);
        btnAddSupplierHeader = view.findViewById(R.id.btnAddSupplierHeader);

        tilSearch = view.findViewById(R.id.tilSearch);
        etSearch = view.findViewById(R.id.etSearch);
        btnFilterSort = view.findViewById(R.id.btnFilterSort);
        chipGroupSupplierStatus = view.findViewById(R.id.chipGroupSupplierStatus);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        rvSuppliers = view.findViewById(R.id.rvSuppliers);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        btnEmptyAddSupplier = view.findViewById(R.id.btnEmptyAddSupplier);
        fabAdd = view.findViewById(R.id.fabAdd);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new SupplierAdapter(filteredSuppliers, this);
        rvSuppliers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSuppliers.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadSuppliers);

        btnAddSupplierHeader.setOnClickListener(v -> showAddSupplierDialog());
        btnEmptyAddSupplier.setOnClickListener(v -> showAddSupplierDialog());
        fabAdd.setOnClickListener(v -> showAddSupplierDialog());

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

        chipGroupSupplierStatus.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
    }

    private void loadSuppliers() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        firestoreManager.getSuppliersByUser(userId, new FirestoreManager.FirestoreListCallback<Supplier>() {
            @Override
            public void onSuccess(List<Supplier> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                allSuppliers = result;

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
        int totalSuppliers = allSuppliers.size();
        double totalPayable = 0;
        int payableSuppliersCount = 0;

        for (Supplier s : allSuppliers) {
            if (s.getCurrentPayable() > 0) {
                totalPayable += s.getCurrentPayable();
                payableSuppliersCount++;
            }
        }

        tvSummaryTotalSuppliers.setText(totalSuppliers + " জন");
        tvSummaryTotalPayable.setText(UnitConverterHelper.formatCurrency(totalPayable));
        tvSummaryPayableSuppliersCount.setText(payableSuppliersCount + " জন");
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        int checkedChipId = chipGroupSupplierStatus.getCheckedChipId();

        filteredSuppliers.clear();

        for (Supplier supplier : allSuppliers) {
            boolean matchesSearch = true;
            boolean matchesStatus = true;

            if (!query.isEmpty()) {
                String name = supplier.getName() != null ? supplier.getName().toLowerCase() : "";
                String business = supplier.getBusinessName() != null ? supplier.getBusinessName().toLowerCase() : "";
                String phone = supplier.getPhone() != null ? supplier.getPhone().toLowerCase() : "";
                matchesSearch = name.contains(query) || business.contains(query) || phone.contains(query);
            }

            if (checkedChipId == R.id.chipFilterHasPayable) {
                matchesStatus = supplier.getCurrentPayable() > 0;
            } else if (checkedChipId == R.id.chipFilterSettled) {
                matchesStatus = supplier.getCurrentPayable() <= 0;
            } else if (checkedChipId == R.id.chipFilterOverdue) {
                // Overdue: payable > 0 and updatedAt older than 30 days
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                matchesStatus = supplier.getCurrentPayable() > 0 && supplier.getUpdatedAt() < thirtyDaysAgo;
            }

            if (matchesSearch && matchesStatus) {
                filteredSuppliers.add(supplier);
            }
        }

        adapter.updateData(filteredSuppliers);
        layoutEmptyState.setVisibility(filteredSuppliers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAddSupplierDialog() {
        String userId = sessionManager.getUserId();
        AddSupplierDialog dialog = AddSupplierDialog.newInstance(userId);
        dialog.setOnSupplierSavedListener(this::loadSuppliers);
        dialog.show(getChildFragmentManager(), "AddSupplierFromList");
    }

    @Override
    public void onSupplierClick(Supplier supplier) {
        Toast.makeText(requireContext(), supplier.getName() + " — পাওনা: " + UnitConverterHelper.formatCurrency(supplier.getCurrentPayable()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPurchaseClick(Supplier supplier) {
        AddPurchaseDialog dialog = new AddPurchaseDialog();
        dialog.setOnPurchaseSavedListener(this::loadSuppliers);
        dialog.show(getChildFragmentManager(), "AddPurchaseFromSupplierCard");
    }

    @Override
    public void onPaymentClick(Supplier supplier) {
        SupplierPaymentDialog dialog = SupplierPaymentDialog.newInstance(supplier);
        dialog.setOnPaymentSavedListener(this::loadSuppliers);
        dialog.show(getChildFragmentManager(), "PaymentFromSupplierCard");
    }

    @Override
    public void onLedgerClick(Supplier supplier) {
        onSupplierClick(supplier);
    }
}
