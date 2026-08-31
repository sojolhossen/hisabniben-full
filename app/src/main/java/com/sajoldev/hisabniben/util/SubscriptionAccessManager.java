package com.sajoldev.hisabniben.util;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sajoldev.hisabniben.model.User;

public class SubscriptionAccessManager {
    private static final String TAG = "SubscriptionAccess";
    
    public static final String STATUS_TRIAL = "TRIAL";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ADMIN_GRANTED = "ADMIN_GRANTED";
    public static final String STATUS_GRACE_PERIOD = "GRACE_PERIOD";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String PAYMENT_PENDING = "PENDING";
    public static final String PAYMENT_APPROVED = "APPROVED";
    public static final String PAYMENT_REJECTED = "REJECTED";

    private static SubscriptionAccessManager instance;
    private final Context context;
    private User currentUser;

    private SubscriptionAccessManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SubscriptionAccessManager getInstance(Context context) {
        if (instance == null) {
            instance = new SubscriptionAccessManager(context);
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Get effective subscription status considering state-specific expiration
     */
    public String getEffectiveStatus() {
        if (currentUser == null) {
            return STATUS_EXPIRED;
        }

        String rawStatus = currentUser.getSubscriptionStatus();
        long now = System.currentTimeMillis();

        // 1. Admin Granted
        if (STATUS_ADMIN_GRANTED.equalsIgnoreCase(rawStatus)) {
            if (currentUser.getSubscriptionExpiryDate() != null && currentUser.getSubscriptionExpiryDate() > 0) {
                return now < currentUser.getSubscriptionExpiryDate() ? STATUS_ADMIN_GRANTED : STATUS_EXPIRED;
            }
            return STATUS_ADMIN_GRANTED;
        }

        // 2. Active Subscription
        if (STATUS_ACTIVE.equalsIgnoreCase(rawStatus) || currentUser.isPremium()) {
            Long expiry = currentUser.getSubscriptionExpiryDate();
            if (expiry != null && expiry > 0) {
                return now < expiry ? STATUS_ACTIVE : STATUS_EXPIRED;
            }
            return STATUS_ACTIVE;
        }

        // 3. Free Trial
        if (STATUS_TRIAL.equalsIgnoreCase(rawStatus) || (rawStatus == null && !currentUser.isPremium())) {
            long trialEnd = currentUser.getTrialEnd();
            if (trialEnd > 0) {
                return now < trialEnd ? STATUS_TRIAL : STATUS_EXPIRED;
            }
        }

        return STATUS_EXPIRED;
    }

    /**
     * Main access check for protected features
     */
    public boolean hasActiveAccess() {
        String status = getEffectiveStatus();
        return STATUS_TRIAL.equals(status) ||
               STATUS_ACTIVE.equals(status) ||
               STATUS_ADMIN_GRANTED.equals(status) ||
               STATUS_GRACE_PERIOD.equals(status);
    }

    /**
     * Feature-specific permission check
     */
    public boolean hasFeature(String featureKey) {
        if (!hasActiveAccess()) {
            return false;
        }
        // Additional granular package feature checks can be placed here if needed
        return true;
    }

    /**
     * Get remaining days based on current active state target expiry
     */
    public long getRemainingDays() {
        if (currentUser == null) return 0;

        String status = getEffectiveStatus();
        long now = System.currentTimeMillis();
        long targetExpiry = 0;

        if (STATUS_TRIAL.equals(status)) {
            targetExpiry = currentUser.getTrialEnd();
        } else if (STATUS_ACTIVE.equals(status) || STATUS_ADMIN_GRANTED.equals(status)) {
            Long expiry = currentUser.getSubscriptionExpiryDate();
            if (expiry != null) {
                targetExpiry = expiry;
            }
        }

        if (targetExpiry <= now) return 0;
        long diff = targetExpiry - now;
        return Math.max(0, diff / (1000 * 60 * 60 * 24));
    }

    /**
     * Refresh user subscription data asynchronously from Firestore
     */
    public interface SubscriptionCallback {
        void onResult(boolean hasAccess, String status);
    }

    public void refreshFromFirestore(SubscriptionCallback callback) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null) {
            if (callback != null) callback.onResult(false, STATUS_EXPIRED);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(fUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        this.currentUser = documentSnapshot.toObject(User.class);
                        if (this.currentUser != null) {
                            this.currentUser.setUid(documentSnapshot.getId());
                        }
                    }
                    boolean access = hasActiveAccess();
                    String status = getEffectiveStatus();
                    if (callback != null) callback.onResult(access, status);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error refreshing user subscription:", e);
                    boolean access = hasActiveAccess();
                    String status = getEffectiveStatus();
                    if (callback != null) callback.onResult(access, status);
                });
    }
}
