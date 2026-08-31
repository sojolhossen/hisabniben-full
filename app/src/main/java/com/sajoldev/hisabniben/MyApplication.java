package com.sajoldev.hisabniben;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.onesignal.OneSignal;
import com.sajoldev.hisabniben.util.DisplayRefreshRateHelper;
import com.sajoldev.hisabniben.util.ScreenSecurityHelper;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            db.setFirestoreSettings(settings);
        } catch (Exception e) {
            // Firestore settings already initialized
        }
        
        OneSignal.initWithContext(this, "b632ec59-9dfd-496f-ae50-5331bb53e91d");

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                ScreenSecurityHelper.allowScreenSharingAndRecording(activity);
                DisplayRefreshRateHelper.enableHighRefreshRate(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
