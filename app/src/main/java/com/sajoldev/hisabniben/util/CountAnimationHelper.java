package com.sajoldev.hisabniben.util;

import android.animation.ValueAnimator;
import android.widget.TextView;

public class CountAnimationHelper {

    public static void animateCount(TextView textView, long startValue, long endValue) {
        animateCount(textView, startValue, endValue, 1500);
    }

    public static void animateCount(TextView textView, long startValue, long endValue, long duration) {
        ValueAnimator animator = ValueAnimator.ofInt((int) startValue, (int) endValue);
        animator.setDuration(duration);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(value));
        });
        
        animator.start();
    }

    public static void animateCurrency(TextView textView, double startValue, double endValue, String currencySymbol) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) startValue, (float) endValue);
        animator.setDuration(1500);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            textView.setText(currencySymbol + String.format("%.0f", value));
        });
        
        animator.start();
    }

    public static void animateCurrency(TextView textView, long startValue, long endValue, String currencySymbol) {
        ValueAnimator animator = ValueAnimator.ofInt((int) startValue, (int) endValue);
        animator.setDuration(1500);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(currencySymbol + String.valueOf(value));
        });
        
        animator.start();
    }
}
