package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

/**
 * "Свет" and "Ночь": a black, untouchable layer over everything on screen. The stick cannot change the
 * TV's backlight, so dimming the picture is the closest thing. Needs the overlay app-op, granted over ADB.
 */
final class Dim {
    /** The darkest the slider goes: below this the picture is hard to make out. */
    static final int MIN_BRIGHTNESS = 20;
    static final float NIGHT = 0.7f;

    private static View view;
    /** The sleep timer ran out: the screen is black until a key is pressed, whatever the level says. */
    private static boolean blackout;

    private Dim() {
    }

    static float level(Context c) {
        return prefs(c).getFloat("level", 0f);
    }

    /** The level as the slider shows it: 100% is no layer at all. */
    static int brightness(Context c) {
        return Math.round((1 - level(c)) * 100);
    }

    static void setBrightness(Context c, int percent) {
        set(c, 1 - Math.max(MIN_BRIGHTNESS, Math.min(100, percent)) / 100f);
    }

    static boolean night(Context c) {
        return level(c) >= NIGHT;
    }

    /** "Ночь" on, or back to the level from before it. */
    static void toggleNight(Context c) {
        SharedPreferences p = prefs(c);
        if (night(c)) {
            set(c, p.getFloat("day", 0f));
        } else {
            p.edit().putFloat("day", level(c)).apply();
            set(c, NIGHT);
        }
    }

    static boolean available(Context c) {
        return Settings.canDrawOverlays(c);
    }

    /** 0 = off, up to 0.8 = very dark. */
    static void set(Context c, float level) {
        Context app = c.getApplicationContext();
        prefs(app).edit().putFloat("level", level).apply();
        apply(app);
    }

    static void setBlackout(Context c, boolean on) {
        blackout = on;
        apply(c.getApplicationContext());
    }

    static boolean blackout() {
        return blackout;
    }

    static void apply(Context app) {
        apply(app, false);
    }

    /**
     * Puts the layer up even when it is clear: the quick panel calls this before it shows itself, so a layer
     * that the slider brings up later lies under the panel instead of over it.
     */
    static void apply(Context app, boolean keep) {
        if (!available(app)) return;
        WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        float level = blackout ? 1f : level(app);
        if (level <= 0 && !keep) {
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

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("dim", Context.MODE_PRIVATE);
    }
}
