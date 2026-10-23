package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

/**
 * "Свет 60%" and "Ночь": a black, untouchable layer over everything on screen. The stick cannot change the
 * TV's backlight, so dimming the picture is the closest thing. Needs the overlay app-op, granted over ADB.
 */
final class Dim {
    private static View view;

    private Dim() {
    }

    static float level(Context c) {
        return c.getSharedPreferences("dim", Context.MODE_PRIVATE).getFloat("level", 0f);
    }

    static boolean available(Context c) {
        return Settings.canDrawOverlays(c);
    }

    /** 0 = off, up to 0.8 = very dark. */
    static void set(Context c, float level) {
        Context app = c.getApplicationContext();
        SharedPreferences p = app.getSharedPreferences("dim", Context.MODE_PRIVATE);
        p.edit().putFloat("level", level).apply();
        apply(app);
    }

    static void apply(Context app) {
        if (!available(app)) return;
        WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        float level = level(app);
        if (level <= 0) {
            if (view != null) {
                wm.removeView(view);
                view = null;
            }
            return;
        }
        if (view == null) {
            view = new View(app);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            wm.addView(view, lp);
        }
        view.setBackgroundColor(Math.round(level * 255) << 24);
    }
}
