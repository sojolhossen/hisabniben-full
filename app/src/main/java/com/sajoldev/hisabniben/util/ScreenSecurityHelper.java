package com.sajoldev.hisabniben.util;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

public class ScreenSecurityHelper {

    /**
     * Completely removes FLAG_SECURE & Android sensitive content hiding
     * to allow screen sharing (Meet, Zoom, WhatsApp, TeamViewer), screen recording,
     * and screenshots across all Activities and Dialogs without performance penalty.
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
        } catch (Throwable ignored) {}
    }

    public static void allowWindowSharing(Window window) {
        if (window == null) return;
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
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
}
