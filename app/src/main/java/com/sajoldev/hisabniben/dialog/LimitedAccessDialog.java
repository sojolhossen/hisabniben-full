package com.sajoldev.hisabniben.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.sajoldev.hisabniben.R;

public class LimitedAccessDialog extends Dialog {

    private OnUpgradeClickListener upgradeClickListener;

    public interface OnUpgradeClickListener {
        void onUpgradeClick();
    }

    public LimitedAccessDialog(@NonNull Context context, OnUpgradeClickListener listener) {
        super(context, R.style.FullScreenDialogTheme);
        this.upgradeClickListener = listener;
        setCancelable(true);
    }

    @Override
    public void onBackPressed() {
        dismiss();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_limited_access);
        
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setDimAmount(0.0f);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        com.sajoldev.hisabniben.util.ScreenSecurityHelper.allowDialogSharing(this);

        MaterialButton btnUpgrade = findViewById(R.id.btnUpgrade);
        btnUpgrade.setOnClickListener(v -> {
            dismiss();
            if (upgradeClickListener != null) {
                upgradeClickListener.onUpgradeClick();
            }
        });
    }
}