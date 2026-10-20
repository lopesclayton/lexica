package com.serwylo.lexica;

import android.app.Activity;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** Keep existing screen layouts clear of enforced edge-to-edge system UI. */
public final class WigoWindowInsets {
    private WigoWindowInsets() {}

    public static void apply(Activity activity) {
        // Older Android versions already fit these non-fullscreen windows.
        if (Build.VERSION.SDK_INT < 35) return;
        View content = activity.findViewById(android.R.id.content);
        TypedValue background = new TypedValue();
        if (activity.getTheme().resolveAttribute(android.R.attr.colorBackground, background, true)) {
            content.setBackgroundColor(background.data);
            boolean light = ColorUtils.calculateLuminance(background.data) > 0.5;
            var controller = WindowCompat.getInsetsController(activity.getWindow(),
                activity.getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(light);
            controller.setAppearanceLightNavigationBars(light);
        }
        final int left = content.getPaddingLeft();
        final int top = content.getPaddingTop();
        final int right = content.getPaddingRight();
        final int bottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(left + safe.left, top + safe.top,
                right + safe.right, bottom + safe.bottom);
            // This container has accounted for all insets for its child layout.
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
