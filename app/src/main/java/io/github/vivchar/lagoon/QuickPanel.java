package io.github.vivchar.lagoon;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The quick panel: a card at the bottom of the screen over whatever is playing, opened by a remote button
 * (by default holding Prime Video). A brightness slider on top, the chosen actions under it. It is an overlay
 * window that takes the remote while it is up; Back, the button again or a few idle seconds close it.
 */
final class QuickPanel {
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final Runnable hideLater = QuickPanel::hide;
    private static View view;

    private QuickPanel() {
    }

    static boolean shown() {
        return view != null;
    }

    static void toggle(Context c) {
        if (shown()) hide();
        else show(c);
    }

    static void show(Context c) {
        Context app = c.getApplicationContext();
        if (!Dim.available(app)) {
            Toast.makeText(app, "Панели нужно разрешение поверх других окон: adb shell appops set "
                    + app.getPackageName() + " SYSTEM_ALERT_WINDOW allow", Toast.LENGTH_LONG).show();
            return;
        }
        if (view != null) return;
        // the dark layer goes up first, so a darker setting lies under the panel, not over it
        Dim.apply(app, true);
        view = build(app);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        wm(app).addView(view, lp);
        view.findViewWithTag("brightness").requestFocus();
        poke(app);
    }

    static void hide() {
        main.removeCallbacks(hideLater);
        if (view == null) return;
        Context app = view.getContext();
        wm(app).removeView(view);
        view = null;
        Dim.apply(app);
    }

    /** A key was pressed: the panel stays up for another few seconds. */
    private static void poke(Context c) {
        main.removeCallbacks(hideLater);
        main.postDelayed(hideLater, Actions.panelTimeout(c) * 1000L);
    }

    private static View build(Context c) {
        FrameLayout root = new FrameLayout(c) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                poke(c);
                if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    if (e.getAction() == KeyEvent.ACTION_UP) hide();
                    return true;
                }
                return super.dispatchKeyEvent(e);
            }
        };
        root.setClipChildren(false);
        root.setClipToPadding(false);
        // room above and around the card for the cursor's growth
        root.setPadding(Ui.px(c, 72), Ui.px(c, 24), Ui.px(c, 72), Ui.px(c, 48));

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(Ui.px(c, 30), Ui.px(c, 26), Ui.px(c, 30), Ui.px(c, 24));
        card.setBackground(Ui.round(Ui.SURFACE, Ui.GLASS_LINE, 28, c));
        root.addView(card, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.text(c, "Быстрые настройки", 24, 600, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(Ui.text(c, new SimpleDateFormat("HH:mm", Locale.ROOT).format(new Date()), 16, 500, Ui.TEXT_DIM));
        card.addView(head);

        View bright = brightness(c);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, Ui.px(c, 72));
        bl.topMargin = Ui.px(c, 22);
        card.addView(bright, bl);

        LinearLayout pills = new LinearLayout(c);
        pills.setClipChildren(false);
        pills.setClipToPadding(false);
        for (String id : Actions.panelOrder(c)) {
            if (!Actions.panelShows(c, id)) continue;
            View p = item(c, id);
            p.setTag(id);
            LinearLayout.LayoutParams pl = (LinearLayout.LayoutParams) p.getLayoutParams();
            if (pills.getChildCount() > 0) pl.leftMargin = Ui.px(c, 14);
            pills.addView(p, pl);
        }
        if (pills.getChildCount() > 0) {
            // ↓ from the slider starts the row at its first item, not under wherever the knob happens to be
            View firstPill = pills.getChildAt(0);
            firstPill.setId(View.generateViewId());
            bright.setNextFocusDownId(firstPill.getId());
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-1, -2);
            pl.topMargin = Ui.px(c, 22);
            card.addView(pills, pl);
        }

        TextView hint = Ui.text(c, "← → яркость · Back — закрыть", 14, 400, Ui.TEXT_FAINT);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = Ui.px(c, 18);
        card.addView(hint, hl);
        return root;
    }

    /** The brightness row: one control, ←/→ move the slider by the step set in «Панель». */
    private static View brightness(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.px(c, 22), 0, Ui.px(c, 22), 0);
        r.setBackground(Ui.control(c, 24));
        r.addView(Ui.icon(c, R.drawable.ic_sun, 26));
        TextView name = Ui.text(c, "Яркость", 17, 600, Color.WHITE);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(Ui.px(c, 110), -2);
        nl.leftMargin = Ui.px(c, 16);
        r.addView(name, nl);
        Ui.Slider slider = new Ui.Slider(c);
        r.addView(slider, new LinearLayout.LayoutParams(0, Ui.px(c, 32), 1));
        TextView pct = Ui.text(c, "", 20, 600, Color.WHITE);
        pct.setGravity(Gravity.END);
        r.addView(pct, new LinearLayout.LayoutParams(Ui.px(c, 72), -2));
        Runnable show = () -> {
            int b = Dim.brightness(c);
            slider.set((b - Dim.MIN_BRIGHTNESS) / (float) (100 - Dim.MIN_BRIGHTNESS));
            pct.setText(b + "%");
        };
        show.run();
        Ui.focusable(r);
        r.setTag("brightness");
        r.setOnKeyListener((v, code, e) -> {
            if (code != KeyEvent.KEYCODE_DPAD_LEFT && code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                int step = Actions.brightnessStep(c) * (code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                // keep to the step's grid, so 60% and the other presets are always reachable
                int b = Math.round((Dim.brightness(c) + step) / (float) Math.abs(step)) * Math.abs(step);
                Dim.setBrightness(c, b);
                show.run();
            }
            return true;
        });
        return r;
    }

    private static View item(Context c, String id) {
        boolean on = Actions.NIGHT.equals(id) ? Dim.night(c) : Actions.SLEEP.equals(id) && Actions.sleepAt(c) != 0;
        String label = Actions.SLEEP.equals(id) ? Actions.sleepLabel(c) : Actions.name(id);
        return Ui.pill(c, Actions.icon(id), label, on, v -> {
            switch (id) {
                case Actions.NIGHT:
                    Dim.toggleNight(c);
                    rebuild(c, id);
                    break;
                case Actions.SLEEP:
                    Actions.nextSleep(c);
                    rebuild(c, id);
                    break;
                default:
                    hide();
                    Actions.run(c, id);
            }
        });
    }

    /**
     * Swaps the card for a fresh one in the same window, so the pills show the new state, and puts the cursor
     * back on {@code focus}.
     */
    private static void rebuild(Context c, String focus) {
        if (view == null) return;
        FrameLayout old = (FrameLayout) view;
        FrameLayout fresh = (FrameLayout) build(c);
        View card = fresh.getChildAt(0);
        fresh.removeView(card);
        old.removeAllViews();
        old.addView(card);
        View f = old.findViewWithTag(focus);
        if (f != null) f.requestFocus();
    }

    private static WindowManager wm(Context c) {
        return (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
    }
}
