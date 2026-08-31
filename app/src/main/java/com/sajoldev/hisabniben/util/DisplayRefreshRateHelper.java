package com.sajoldev.hisabniben.util;

import android.app.Activity;
import android.os.Build;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public class DisplayRefreshRateHelper {

    public static void enableHighRefreshRate(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;

        try {
            Window window = activity.getWindow();
            
            // 1. Force Hardware Acceleration on Window
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );

            // 2. Request Highest Available Display Refresh Rate (90Hz / 120Hz / 144Hz)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Display display = activity.getDisplay();
                if (display != null) {
                    Display.Mode[] modes = display.getSupportedModes();
                    Display.Mode maxMode = null;
                    float maxRate = 60.0f;
                    for (Display.Mode mode : modes) {
                        if (mode.getRefreshRate() > maxRate) {
                            maxRate = mode.getRefreshRate();
                            maxMode = mode;
                        }
                    }
                    if (maxMode != null) {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.preferredDisplayModeId = maxMode.getModeId();
                        window.setAttributes(lp);
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Display display = activity.getWindowManager().getDefaultDisplay();
                if (display != null) {
                    Display.Mode[] modes = display.getSupportedModes();
                    Display.Mode maxMode = null;
                    float maxRate = 60.0f;
                    for (Display.Mode mode : modes) {
                        if (mode.getRefreshRate() > maxRate) {
                            maxRate = mode.getRefreshRate();
                            maxMode = mode;
                        }
                    }
                    if (maxMode != null) {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.preferredDisplayModeId = maxMode.getModeId();
                        window.setAttributes(lp);
                    }
                }
            }
        } catch (Throwable t) {
            // Safe fallback if display refresh rate override is restricted by OS
        }
    }
}
