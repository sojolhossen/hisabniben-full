package com.sajoldev.hisabniben.adapter;

import android.view.View;
import android.view.animation.OvershootInterpolator;
import androidx.recyclerview.widget.DefaultItemAnimator;

public class SlideInItemAnimator extends DefaultItemAnimator {
    private final OvershootInterpolator interpolator = new OvershootInterpolator(1.5f);
    
    @Override
    public boolean animateAdd(androidx.recyclerview.widget.RecyclerView.ViewHolder holder) {
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationX(holder.itemView.getWidth() / 2);
        
        holder.itemView.animate()
                .alpha(1f)
                .translationX(0)
                .setDuration(getAddDuration())
                .setInterpolator(interpolator)
                .withEndAction(() -> clearAnimatedValues(holder.itemView))
                .start();
        
        return true;
    }
    
    private void clearAnimatedValues(View view) {
        view.setAlpha(1f);
        view.setTranslationX(0);
    }
}