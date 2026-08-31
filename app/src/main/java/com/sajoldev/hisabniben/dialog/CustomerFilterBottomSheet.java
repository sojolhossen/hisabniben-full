package com.sajoldev.hisabniben.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.adapter.CustomerAdapter;
import com.sajoldev.hisabniben.model.Customer;

import java.util.List;

public class CustomerFilterBottomSheet extends BottomSheetDialogFragment implements CustomerAdapter.OnCustomerClickListener {
    @Override
    public void onCustomerLongClick(Customer customer) {
        // Not used in filter
    }
    private List<Customer> customers;
    private Customer selectedCustomer;
    private OnCustomerSelectedListener listener;
    private RecyclerView rvCustomers;
    private CustomerAdapter adapter;

    public interface OnCustomerSelectedListener {
        void onCustomerSelected(Customer customer);
    }

    public static CustomerFilterBottomSheet newInstance(List<Customer> customers, Customer selectedCustomer) {
        CustomerFilterBottomSheet sheet = new CustomerFilterBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("customers", new java.util.ArrayList<>(customers));
        if (selectedCustomer != null) {
            args.putString("selectedCustomerId", selectedCustomer.getId());
        }
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customers = (List<Customer>) getArguments().getSerializable("customers");
            String selectedId = getArguments().getString("selectedCustomerId");
            if (selectedId != null && customers != null) {
                for (Customer c : customers) {
                    if (c.getId().equals(selectedId)) {
                        selectedCustomer = c;
                        break;
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_customer_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvAllCustomers = view.findViewById(R.id.tvAllCustomers);
        rvCustomers = view.findViewById(R.id.rvCustomers);

        tvTitle.setText("Filter by Customer");

        tvAllCustomers.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onCustomerSelected(null);
            }
        });

        if (selectedCustomer == null) {
            tvAllCustomers.setTextColor(requireContext().getResources().getColor(R.color.primary, null));
        }

        adapter = new CustomerAdapter(customers, this, selectedCustomer);
        rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCustomers.setAdapter(adapter);
    }

    public void setOnCustomerSelectedListener(OnCustomerSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCustomerClick(Customer customer) {
        dismiss();
        if (listener != null) {
            listener.onCustomerSelected(customer);
        }
    }
}
