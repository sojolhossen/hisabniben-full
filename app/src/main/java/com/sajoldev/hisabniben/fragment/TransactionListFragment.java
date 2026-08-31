package com.sajoldev.hisabniben.fragment;

import android.content.Intent;
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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.TransactionAdapter;
import com.sajoldev.hisabniben.dialog.AddTransactionDialog;
import com.sajoldev.hisabniben.dialog.CustomerFilterBottomSheet;
import com.sajoldev.hisabniben.dialog.TransactionDetailsBottomSheet;
import com.sajoldev.hisabniben.model.Customer;
import com.sajoldev.hisabniben.model.Transaction;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class TransactionListFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener, CustomerFilterBottomSheet.OnCustomerSelectedListener {
    private TextInputEditText etSearch;
    private RecyclerView rvTransactions;
    private SwipeRefreshLayout swipeRefresh;
    private ExtendedFloatingActionButton fabAdd;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private MaterialCardView cardFilter;
    private TextView tvFilterCustomer;
    private TabLayout tabLayout;
    
    private int currentTab = 0;

    private SessionManager sessionManager;
    private FirestoreManager firestoreManager;
    private TransactionAdapter adapter;
    private List<Transaction> transactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private Customer selectedCustomer = null;
    private boolean isDataLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        initFirebase();
        setupRecyclerView();
        setupSearch();
        setupClickListeners();
        
        if (!isDataLoaded) {
            loadData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        rvTransactions = view.findViewById(R.id.rvTransactions);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        fabAdd = view.findViewById(R.id.fabAdd);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        progressBar = view.findViewById(R.id.progressBar);
        cardFilter = view.findViewById(R.id.cardFilter);
        tvFilterCustomer = view.findViewById(R.id.tvFilterCustomer);
        tabLayout = view.findViewById(R.id.tabLayout);
    }

    private void initFirebase() {
        sessionManager = SessionManager.getInstance(requireContext());
        firestoreManager = FirestoreManager.getInstance();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(filteredTransactions, this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> {
            if (customers.isEmpty()) {
                Toast.makeText(requireContext(), "Add a customer first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sessionManager.isLimited()) {
                int limit = sessionManager.getTransactionLimit();
                if (transactions.size() >= limit) {
                    Toast.makeText(requireContext(), "Limited feature! Upgrade to add more transactions.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            AddTransactionDialog dialog = AddTransactionDialog.newInstance(customers);
            dialog.setOnTransactionSavedListener(this::loadData);
            dialog.show(getChildFragmentManager(), "AddTransaction");
        });

        swipeRefresh.setOnRefreshListener(this::loadData);

        cardFilter.setOnClickListener(v -> {
            CustomerFilterBottomSheet bottomSheet = CustomerFilterBottomSheet.newInstance(customers, selectedCustomer);
            bottomSheet.setOnCustomerSelectedListener(this);
            bottomSheet.show(getChildFragmentManager(), "CustomerFilter");
        });
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                applyFilters();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public void onCustomerSelected(Customer customer) {
        selectedCustomer = customer;
        applyFilters();
    }

    private void applyFilters() {
        String searchQuery = etSearch.getText() != null ? etSearch.getText().toString() : "";
        
        filteredTransactions.clear();
        
        for (Transaction transaction : transactions) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                (transaction.getCustomerName() != null && transaction.getCustomerName().toLowerCase().contains(searchQuery.toLowerCase()));
            
            boolean matchesCustomer = selectedCustomer == null || 
                (transaction.getCustomerId() != null && transaction.getCustomerId().equals(selectedCustomer.getId()));
            
            boolean matchesTab = true;
            if (currentTab == 1) {
                matchesTab = Transaction.TYPE_PAYMENT.equals(transaction.getType());
            } else if (currentTab == 2) {
                matchesTab = Transaction.TYPE_BAKI.equals(transaction.getType());
            }
            
            if (matchesSearch && matchesCustomer && matchesTab) {
                filteredTransactions.add(transaction);
            }
        }
        
        adapter.updateData(filteredTransactions, customers);
        updateFilterUI();
        updateEmptyState();
    }
    
    private void updateFilterUI() {
        if (selectedCustomer != null) {
            tvFilterCustomer.setVisibility(View.VISIBLE);
            tvFilterCustomer.setText("Filter: " + selectedCustomer.getName());
            cardFilter.setCardBackgroundColor(requireContext().getResources().getColor(R.color.primary_light, null));
        } else {
            tvFilterCustomer.setVisibility(View.GONE);
            cardFilter.setCardBackgroundColor(requireContext().getResources().getColor(R.color.white, null));
        }
    }

    private void loadData() {
        showLoading(true);
        String userId = sessionManager.getUserId();

        firestoreManager.getCustomersByUser(userId, new FirestoreManager.FirestoreListCallback<Customer>() {
            @Override
            public void onSuccess(List<Customer> result) {
                customers = result;
                loadTransactions();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                loadTransactions();
            }
        });
    }

    private void loadTransactions() {
        String userId = sessionManager.getUserId();

        firestoreManager.getTransactionsByUser(userId, new FirestoreManager.FirestoreListCallback<Transaction>() {
            @Override
            public void onSuccess(List<Transaction> result) {
                transactions = result;
                filteredTransactions = new ArrayList<>(transactions);
                adapter.updateData(filteredTransactions, customers);
                updateEmptyState();
                swipeRefresh.setRefreshing(false);
                showLoading(false);
                isDataLoaded = true;
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
                showLoading(false);
            }
        });
    }

    private void filterTransactions(String query) {
        applyFilters();
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(filteredTransactions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        TransactionDetailsBottomSheet bottomSheet = TransactionDetailsBottomSheet.newInstance(transaction);
        bottomSheet.show(getChildFragmentManager(), "TransactionDetails");
    }

    private Customer getCustomerById(String customerId) {
        for (Customer customer : customers) {
            if (customer.getId().equals(customerId)) {
                return customer;
            }
        }
        return null;
    }
}
