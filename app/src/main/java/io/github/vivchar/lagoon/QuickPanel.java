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

import java.util.Date;

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
        // the panel's texts come from the chosen language, whichever context asked for it
        Context app = Lang.wrap(c.getApplicationContext());
        if (!Dim.available(app)) {
            Toast.makeText(app, app.getString(R.string.overlay_permission, app.getPackageName()), Toast.LENGTH_LONG).show();
            return;
        }
        if (view != null) return;
        view = build(app);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        wm(app).addView(view, lp);
        // it rises from the bottom edge instead of appearing at once
        view.setAlpha(0f);
        view.setTranslationY(Ui.px(app, 40));
        view.animate().alpha(1f).translationY(0).setInterpolator(FocusRing.EASE).setDuration(240).start();
        view.findViewWithTag("brightness").requestFocus();
        // the panel is dimmed like everything else, so it has the dock's tone and the slider shows its effect on
        // the panel itself: the dark layer goes back on top of it
        Dim.raise(app);
        poke(app);
    }

    static void hide() {
        main.removeCallbacks(hideLater);
        if (view == null) return;
        View gone = view;
        Context app = gone.getContext();
        view = null;
        // and sinks back out of sight; the window goes once it is down
        gone.animate().alpha(0f).translationY(Ui.px(app, 40)).setDuration(180).withEndAction(() -> {
            if (gone.isAttachedToWindow()) wm(app).removeView(gone);
            Dim.apply(app);
        }).start();
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
        // the dock's own insets (64 at the sides, 40 below): over Home the card lies exactly on the dock and
        // hides it, no strip of it showing around; the room above is for the cursor's growth
        root.setPadding(Ui.px(c, 64), Ui.px(c, 24), Ui.px(c, 64), Ui.px(c, 40));

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(Ui.px(c, 30), Ui.px(c, 26), Ui.px(c, 30), Ui.px(c, 24));
        // over Home the bay behind can be blurred like every panel; over another app there is nothing to read back
        if (MainActivity.front && Backdrop.current() != null) Ui.glassBackground(card, Ui.DARK_GLASS, Ui.GLASS_LINE, Ui.RADIUS_PANEL);
        else card.setBackground(new Ui.Rim(c, Ui.PANEL_OVER_APP, Ui.RADIUS_PANEL));
        root.addView(card, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.text(c, c.getString(R.string.quick_settings), 24, 600, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(Ui.text(c, Lang.clock(c, new Date()), 16, 500, Ui.TEXT_DIM));
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

        // 16 units is the 23 pt floor of TV text at 1080p: smaller can't be read from the couch
        TextView hint = Ui.text(c, c.getString(R.string.quick_hint), 16, 400, Ui.TEXT_FAINT);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = Ui.px(c, 18);
        card.addView(hint, hl);
        return root;
    }

    /**
     * The brightness row: one control, ←/→ move the slider by the step set in «Панель». Shared with the settings
     * page, so the two never drift apart.
     */
    static View brightness(Context c) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.px(c, 22), 0, Ui.px(c, 22), 0);
        r.setBackground(Ui.control(c, 24));
        TextView name = Ui.text(c, c.getString(R.string.brightness), 17, 600, Color.WHITE);
        r.addView(name, new LinearLayout.LayoutParams(Ui.px(c, 110), -2));
        // a dim sun at the dark end, a full one at the bright end: which way is brighter needs no reading
        r.addView(Ui.icon(c, R.drawable.ic_sun_dim, 22));
        Ui.Slider slider = new Ui.Slider(c);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(0, Ui.px(c, 32), 1);
        sl.leftMargin = sl.rightMargin = Ui.px(c, 12);
        r.addView(slider, sl);
        r.addView(Ui.icon(c, R.drawable.ic_sun, 28));
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
        // switched on says so in words too, not only in blue
        String label = Actions.SLEEP.equals(id) ? Actions.sleepLabel(c) : Actions.NIGHT.equals(id) && on ? c.getString(R.string.night_on) : Actions.name(c, id);
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
