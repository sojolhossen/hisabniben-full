package com.sajoldev.hisabniben.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sajoldev.hisabniben.R;
import com.sajoldev.hisabniben.model.PaymentMethod;

import java.util.List;

public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.ViewHolder> {
    private List<PaymentMethod> methods;
    private int selectedPosition = -1;
    private OnMethodClickListener listener;

    public interface OnMethodClickListener {
        void onMethodClick(PaymentMethod method);
    }

    public PaymentMethodAdapter(List<PaymentMethod> methods, OnMethodClickListener listener) {
        this.methods = methods;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentMethod method = methods.get(position);
        holder.tvPaymentName.setText(method.getName());
        holder.ivSelected.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);
        
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onMethodClick(method);
        });
    }

    @Override
    public int getItemCount() {
        return methods.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPaymentName;
        ImageView ivSelected;

        ViewHolder(View itemView) {
            super(itemView);
            tvPaymentName = itemView.findViewById(R.id.tvPaymentName);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}