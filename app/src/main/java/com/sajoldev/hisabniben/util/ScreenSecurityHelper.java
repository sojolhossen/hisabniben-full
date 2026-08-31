package com.sajoldev.hisabniben.util;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import java.lang.reflect.Method;

public class ScreenSecurityHelper {

    /**
     * Completely removes FLAG_SECURE & Android 14/15/16 sensitive content hiding
     * to allow screen sharing (Meet, Zoom, WhatsApp, TeamViewer), screen recording,
     * and screenshots across all Activities, Layouts, Password fields and Dialogs.
     */
    public static void allowScreenSharingAndRecording(Activity activity) {
        if (activity == null) return;
        try {
            allowWindowSharing(activity.getWindow());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    activity.setRecentsScreenshotEnabled(true);
                } catch (Throwable ignored) {}
            }

            if (activity.getWindow() != null) {
                View decorView = activity.getWindow().getDecorView();
                if (decorView != null) {
                    allowWindowSharing(activity.getWindow());
                    makeViewNotSensitive(decorView);

                    decorView.post(() -> {
                        allowWindowSharing(activity.getWindow());
                        makeViewNotSensitive(decorView);
                    });

                    decorView.postDelayed(() -> {
                        allowWindowSharing(activity.getWindow());
                        makeViewNotSensitive(decorView);
                    }, 300);

                    decorView.postDelayed(() -> {
                        allowWindowSharing(activity.getWindow());
                        makeViewNotSensitive(decorView);
                    }, 800);

                    decorView.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
                        allowWindowSharing(activity.getWindow());
                        makeViewNotSensitive(decorView);
                    });

                    decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                        allowWindowSharing(activity.getWindow());
                        makeViewNotSensitive(decorView);
                    });
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void allowWindowSharing(Window window) {
        if (window == null) return;
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            
            WindowManager.LayoutParams lp = window.getAttributes();
            if (lp != null) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
                window.setAttributes(lp);
            }

            if (window.getDecorView() != null) {
                makeViewNotSensitive(window.getDecorView());
            }
        } catch (Throwable ignored) {}
    }

    public static void allowDialogSharing(Dialog dialog) {
        if (dialog == null) return;
        try {
            if (dialog.getWindow() != null) {
                allowWindowSharing(dialog.getWindow());
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Traverses view hierarchy and marks all views (including password fields) as NOT SENSITIVE
     * to prevent Android 14/15/16 Screen Sharing sensitive content masking ("App content hidden for security").
     */
    public static void makeViewNotSensitive(View view) {
        if (view == null) return;
        try {
            view.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    view.setImportantForContentCapture(View.IMPORTANT_FOR_CONTENT_CAPTURE_NO);
                } catch (Throwable ignored) {}
            }

            // Android 14, 15 & 16 (API 34, 35, 36+)
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    Method setSensitivityMethod = View.class.getMethod("setContentSensitivity", int.class);
                    // View.CONTENT_SENSITIVITY_NOT_SENSITIVE = 2
                    setSensitivityMethod.invoke(view, 2);
                } catch (Throwable e) {
                    try {
                        view.setContentSensitivity(2);
                    } catch (Throwable ignored) {}
                }
            }

            // Remove native password inputType flags for Android 16 Screen Share Redaction
            if (view instanceof EditText) {
                EditText editText = (EditText) view;
                int inputType = editText.getInputType();
                if ((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    (inputType & InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                    editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }

        } catch (Throwable ignored) {}

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                makeViewNotSensitive(group.getChildAt(i));
            }
        }
    }
}
