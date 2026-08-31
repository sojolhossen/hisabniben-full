package com.sajoldev.hisabniben.util;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.sajoldev.hisabniben.activity.SubscriptionRequiredActivity;

public class SubscriptionGuard {

    public interface Action {
        void execute();
    }

    /**
     * Protect an activity action or navigation.
     * If user has active subscription or trial, executes action.
     * Otherwise redirects to SubscriptionRequiredActivity.
     */
    public static boolean checkAccess(Context context, Runnable onGranted) {
        SubscriptionAccessManager manager = SubscriptionAccessManager.getInstance(context);
        if (manager.hasActiveAccess()) {
            if (onGranted != null) {
                onGranted.run();
            }
            return true;
        } else {
            Intent intent = new Intent(context, SubscriptionRequiredActivity.class);
            context.startActivity(intent);
            return false;
        }
    }

    /**
     * Check feature-specific access
     */
    public static boolean checkFeatureAccess(Context context, String featureKey, Runnable onGranted) {
        SubscriptionAccessManager manager = SubscriptionAccessManager.getInstance(context);
        if (manager.hasFeature(featureKey)) {
            if (onGranted != null) {
                onGranted.run();
            }
            return true;
        } else {
            Toast.makeText(context, "এই ফিচারটি ব্যবহার করতে সাবস্ক্রিপশন প্রয়োজন", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, SubscriptionRequiredActivity.class);
            context.startActivity(intent);
            return false;
        }
    }
}
