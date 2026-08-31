package com.sajoldev.hisabniben.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class FirestoreJobIntentService extends JobIntentService {
    private static final int JOB_ID = 1001;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, FirestoreJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        
    }
}
