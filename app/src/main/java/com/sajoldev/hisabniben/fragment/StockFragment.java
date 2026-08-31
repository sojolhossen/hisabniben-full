package com.sajoldev.hisabniben.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.RiceProductAdapter;
import com.sajoldev.hisabniben.dialog.AddPurchaseDialog;
import com.sajoldev.hisabniben.dialog.AddRiceProductDialog;
import com.sajoldev.hisabniben.dialog.AddSaleDialog;
import com.sajoldev.hisabniben.dialog.RiceProductDetailsBottomSheet;
import com.sajoldev.hisabniben.model.RiceProduct;
import com.sajoldev.hisabniben.util.FirestoreManager;
import com.sajoldev.hisabniben.util.SessionManager;
import com.sajoldev.hisabniben.util.UnitConverterHelper;

import java.util.ArrayList;
import java.util.List;

public class StockFragment extends Fragment implements RiceProductAdapter.OnRiceProductActionListener {

    private SwipeRefreshLayout swipeRefresh;
    private ExtendedFloatingActionButton fabAdd;
    private TextView tvTotalVarieties, tvTotalStockKg, tvTotalInventoryValue;
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupStatus;
    private RecyclerView rvStock;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private ImageView ivSearchToggle;

    private RiceProductAdapter adapter;
    private List<RiceProduct> products = new ArrayList<>();
    private SessionManager sessionManager;
    private String currentStatusFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadRiceProducts();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        fabAdd = view.findViewById(R.id.fabAdd);
        tvTotalVarieties = view.findViewById(R.id.tvTotalVarieties);
        tvTotalStockKg = view.findViewById(R.id.tvTotalStockKg);
        tvTotalInventoryValue = view.findViewById(R.id.tvTotalInventoryValue);
        tilSearch = view.findViewById(R.id.tilSearch);
        etSearch = view.findViewById(R.id.etSearch);
        chipGroupStatus = view.findViewById(R.id.chipGroupStatus);
        rvStock = view.findViewById(R.id.rvStock);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        progressBar = view.findViewById(R.id.progressBar);
        ivSearchToggle = view.findViewById(R.id.ivSearchToggle);

        view.findViewById(R.id.btnEmptyAddRice).setOnClickListener(v -> com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(requireContext(), this::openAddRiceDialog));
    }

    private void setupRecyclerView() {
        adapter = new RiceProductAdapter(requireContext(), products, this);
        rvStock.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvStock.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadRiceProducts);
        fabAdd.setOnClickListener(v -> com.sajoldev.hisabniben.util.SubscriptionGuard.checkAccess(requireContext(), this::openAddRiceDialog));

        ivSearchToggle.setOnClickListener(v -> {
            if (tilSearch.getVisibility() == View.VISIBLE) {
                tilSearch.setVisibility(View.GONE);
                etSearch.setText("");
            } else {
                tilSearch.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipInStock) {
                currentStatusFilter = "IN_STOCK";
            } else if (checkedId == R.id.chipLowStock) {
                currentStatusFilter = "LOW_STOCK";
            } else if (checkedId == R.id.chipOutOfStock) {
                currentStatusFilter = "OUT_OF_STOCK";
            } else {
                currentStatusFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void openAddRiceDialog() {
        AddRiceProductDialog dialog = new AddRiceProductDialog();
        dialog.setOnProductSavedListener(this::loadRiceProducts);
        dialog.show(getChildFragmentManager(), "AddRiceProduct");
    }

    private void loadRiceProducts() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        FirestoreManager.getInstance().getRiceProductsByUser(userId, new FirestoreManager.FirestoreListCallback<RiceProduct>() {
            @Override
            public void onSuccess(List<RiceProduct> result) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                products = result;
                adapter.updateData(products);
                updateSummaryHeader();
                applyFilters();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "স্টক লোড করতে ব্যর্থ: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryHeader() {
        int totalVarieties = products.size();
        double totalStockKg = 0;
        double totalInventoryValue = 0;

        for (RiceProduct p : products) {
            totalStockKg += p.getCurrentStockKg();
            totalInventoryValue += p.getCurrentStockKg() * (p.getPurchaseRatePerKg() > 0 ? p.getPurchaseRatePerKg() : p.getSaleRatePerKg());
        }

        tvTotalVarieties.setText(totalVarieties + " ধরনের");
        tvTotalStockKg.setText(UnitConverterHelper.formatKg(totalStockKg));
        tvTotalInventoryValue.setText(UnitConverterHelper.formatCurrency(totalInventoryValue));
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        adapter.filter(query, currentStatusFilter);

        if (adapter.getDisplayItemCount() == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvStock.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvStock.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaleClick(RiceProduct product) {
        AddSaleDialog dialog = new AddSaleDialog();
        dialog.setOnSaleSavedListener(this::loadRiceProducts);
        dialog.show(getChildFragmentManager(), "AddSaleFromStock");
    }

    @Override
    public void onPurchaseClick(RiceProduct product) {
        AddPurchaseDialog dialog = new AddPurchaseDialog();
        dialog.setOnPurchaseSavedListener(this::loadRiceProducts);
        dialog.show(getChildFragmentManager(), "AddPurchaseFromStock");
    }

    @Override
    public void onStockClick(RiceProduct product) {
        RiceProductDetailsBottomSheet detailsSheet = RiceProductDetailsBottomSheet.newInstance(product);
        detailsSheet.setOnSaleClickListener(() -> onSaleClick(product));
        detailsSheet.setOnPurchaseClickListener(() -> onPurchaseClick(product));
        detailsSheet.show(getChildFragmentManager(), "RiceProductDetails");
    }

    @Override
    public void onMoreClick(RiceProduct product, View anchorView) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add("সম্পাদনা করুন (Edit)");
        popup.getMenu().add("নিষ্ক্রিয় করুন (Deactivate)");
        popup.setOnMenuItemClickListener(item -> {
            if ("সম্পাদনা করুন (Edit)".equals(item.getTitle())) {
                Toast.makeText(requireContext(), "সম্পাদনা অপশন শীঘ্রই আসছে", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "পণ্যটি নিষ্ক্রিয় করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }
}
