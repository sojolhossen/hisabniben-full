package com.sajoldev.hisabniben.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.sajoldev.hisabniben.R;

public class SubscriptionRequiredActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription_required);

        Button btnViewPackages = findViewById(R.id.btnViewPackages);
        Button btnGoBack = findViewById(R.id.btnGoBack);

        btnViewPackages.setOnClickListener(v -> {
            Intent intent = new Intent(SubscriptionRequiredActivity.this, SubscriptionActivity.class);
            startActivity(intent);
            finish();
        });

        btnGoBack.setOnClickListener(v -> finish());
    }
}
