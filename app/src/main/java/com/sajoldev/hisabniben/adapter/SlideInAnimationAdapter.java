package com.sajoldev.hisabniben.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import androidx.recyclerview.widget.RecyclerView;

public class SlideInAnimationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;
    private int duration = 500;
    private OvershootInterpolator interpolator;

    public SlideInAnimationAdapter(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        this.adapter = adapter;
        this.interpolator = new OvershootInterpolator(1.5f);
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setInterpolator(OvershootInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return adapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        adapter.onBindViewHolder(holder, position);
        
        holder.itemView.post(() -> {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationX(holder.itemView.getWidth() / 2);
            
            holder.itemView.animate()
                    .alpha(1f)
                    .translationX(0)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start();
        });
    }

    @Override
    public int getItemCount() {
        return adapter.getItemCount();
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override
    public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.registerAdapterDataObserver(observer);
        adapter.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
        adapter.unregisterAdapterDataObserver(observer);
    }
}