package com.sajoldev.hisabniben.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private static BillingManager instance;
    
    private final BillingClient billingClient;
    private final Context context;
    
    public interface BillingCallback {
        void onBillingReady();
        void onPurchaseSuccess();
        void onPurchaseError(String error);
        void onSubscriptionStatus(boolean isPremium, long expiryDate);
    }

    private BillingCallback callback;
    private List<ProductDetails> subscriptionProducts;
    private String selectedProductId;
    private int selectedDurationDays = 30;
    private List<String> productIdsToQuery = new ArrayList<>();
    private final SessionManager sessionManager;

    private BillingManager(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = SessionManager.getInstance(this.context);
        PendingPurchasesParams pendingPurchasesParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build();
        billingClient = BillingClient.newBuilder(this.context)
                .setListener(this)
                .enablePendingPurchases(pendingPurchasesParams)
                .build();
    }

    public static synchronized BillingManager getInstance(Context context) {
        if (instance == null) {
            instance = new BillingManager(context);
        }
        return instance;
    }

    public void setCallback(BillingCallback callback) {
        this.callback = callback;
    }

    public void startConnection() {
        startConnection(null);
    }

    public void startConnection(List<String> dynamicProductIds) {
        if (dynamicProductIds != null && !dynamicProductIds.isEmpty()) {
            this.productIdsToQuery = dynamicProductIds;
        } else if (this.productIdsToQuery.isEmpty()) {
            // Default IDs if none provided
            this.productIdsToQuery.add("hisabniben_premium_monthly");
            this.productIdsToQuery.add("hisabniben_premium_yearly");
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully");
                    querySubscriptions();
                    checkExistingPurchases();
                } else {
                    String errorMsg = "Billing setup failed: " + billingResult.getDebugMessage() + 
                                     " (Code: " + billingResult.getResponseCode() + ")";
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onPurchaseError(errorMsg);
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected");
            }
        });
    }

    private void querySubscriptions() {
        if (productIdsToQuery.isEmpty()) {
            Log.e(TAG, "No product IDs to query");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String productId : productIdsToQuery) {
            productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
            );
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (result, queryProductDetailsResult) -> {
            List<ProductDetails> productDetailsList = queryProductDetailsResult != null ? queryProductDetailsResult.getProductDetailsList() : null;
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                subscriptionProducts = productDetailsList;
                Log.d(TAG, "Found " + subscriptionProducts.size() + " subscription products");
                for (ProductDetails pd : subscriptionProducts) {
                    Log.d(TAG, "Product found: " + pd.getProductId() + " - " + pd.getName());
                }
                
                if (subscriptionProducts.isEmpty()) {
                    Log.w(TAG, "Google Play returned 0 products for requested IDs: " + productIdsToQuery);
                }
                
                if (callback != null) {
                    callback.onBillingReady();
                }
            } else {
                String errorMsg = "Failed to query subscriptions: " + result.getDebugMessage() + 
                                 " (Code: " + result.getResponseCode() + ")";
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onPurchaseError(errorMsg);
                }
            }
        });
    }

    private void checkExistingPurchases() {
        QueryPurchasesParams queryPurchasesParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build();
        billingClient.queryPurchasesAsync(
                queryPurchasesParams,
                (result, purchases) -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                handlePurchase(purchase);
                            }
                        }
                    }
                }
        );
    }

    public void launchSubscriptionFlow(Activity activity, String productId) {
        launchSubscriptionFlow(activity, productId, 30);
    }

    public void launchSubscriptionFlow(Activity activity, String productId, int durationDays) {
        if (subscriptionProducts == null || subscriptionProducts.isEmpty()) {
            if (callback != null) {
                callback.onPurchaseError("Subscription products not loaded");
            }
            return;
        }

        selectedProductId = productId;
        selectedDurationDays = durationDays > 0 ? durationDays : 30;
        ProductDetails selectedProduct = null;
        
        for (ProductDetails product : subscriptionProducts) {
            if (product.getProductId().equals(productId)) {
                selectedProduct = product;
                break;
            }
        }

        if (selectedProduct == null) {
            if (callback != null) {
                callback.onPurchaseError("Product not found");
            }
            return;
        }

        ProductDetails.SubscriptionOfferDetails offerDetails = selectedProduct.getSubscriptionOfferDetails() != null &&
                !selectedProduct.getSubscriptionOfferDetails().isEmpty() ?
                selectedProduct.getSubscriptionOfferDetails().get(0) : null;

        if (offerDetails == null) {
            if (callback != null) {
                callback.onPurchaseError("No offer details available");
            }
            return;
        }

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(selectedProduct)
                                .setOfferToken(offerDetails.getOfferToken())
                                .build()
                ))
                .build();

        billingClient.launchBillingFlow(activity, flowParams);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase cancelled by user");
            if (callback != null) {
                callback.onPurchaseError("Purchase cancelled");
            }
        } else {
            Log.e(TAG, "Purchase error: " + billingResult.getDebugMessage());
            if (callback != null) {
                callback.onPurchaseError(billingResult.getDebugMessage());
            }
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            processSubscription();
        }
    }

    private void processSubscription() {
        int days = selectedDurationDays > 0 ? selectedDurationDays : 30;
        long currentTime = System.currentTimeMillis();
        long currentExpiry = sessionManager != null ? sessionManager.getSubscriptionExpiry() : 0;
        
        long expiryDate;
        if (days >= 999) {
            expiryDate = currentTime + (9999L * 24L * 60L * 60L * 1000L);
        } else {
            long baseTime = (currentExpiry > currentTime && currentExpiry < currentTime + (900L * 24L * 60L * 60L * 1000L)) ? currentExpiry : currentTime;
            expiryDate = baseTime + (days * 24L * 60L * 60L * 1000L);
        }
        
        if (callback != null) {
            callback.onPurchaseSuccess();
            callback.onSubscriptionStatus(true, expiryDate);
        }
    }

    public List<ProductDetails> getSubscriptionProducts() {
        return subscriptionProducts;
    }

    public void endConnection() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    public boolean isReady() {
        return billingClient.isReady();
    }

    public boolean isBillingAvailable() {
        return billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).getResponseCode() 
               == BillingClient.BillingResponseCode.OK;
    }
}
