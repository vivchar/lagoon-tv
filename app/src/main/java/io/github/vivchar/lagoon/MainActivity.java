package io.github.vivchar.lagoon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Home: the live bay behind a clock and the weather, Telegram notes on the right and a glass dock of apps.
 * Left/right walk the dock, Up goes to the notes, Down opens quick settings. Holding OK on an app opens a
 * small menu over it: pin it here, unpin it, or move it.
 */
public class MainActivity extends Activity implements Weather.Listener, Telegram.Listener {
    private static final Locale RU = Locale.forLanguageTag("ru");

    private SceneView scene;
    private View sceneShade;
    private SceneVideo video;
    private FrameLayout root, home;
    private TextView greeting, time, date, weatherLine, focusedName;
    private LinearLayout alert, messages, weatherBox, offline, today;
    private LinearLayout shelf;
    private android.widget.HorizontalScrollView shelfScroll;
    private View overlay;
    private Apps apps;
    private FocusRing ring;
    private Weather weather;
    private Telegram tg;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<Apps.App> order = new ArrayList<>();

    // moving an app
    private String movingPkg;
    private int movingFrom, movingIndex, movingMax;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            updatePhase();
            updateGlass();
            weather.refresh(false);
            updateOffline();
            renderToday();
            Saver.syncAsync(MainActivity.this, () -> { });
            Cal.refreshIfDue(MainActivity.this, MainActivity.this::renderToday);
            handler.postDelayed(this, 60_000 - System.currentTimeMillis() % 60_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apps = new Apps(this);
        weather = new Weather(this);
        tg = new Telegram(this);
        root = new FrameLayout(this);
        scene = new SceneView(this);
        root.addView(scene, new FrameLayout.LayoutParams(-1, -1));
        video = new SceneVideo(this, scene);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1));
        // the pictures and the clips are bright under the clock and the notes: a soft shade there
        sceneShade = new View(this) {
            @Override
            protected void onDraw(android.graphics.Canvas c) {
                c.scale(getWidth() / 1280f, getHeight() / 720f);
                SceneView.shade(c);
            }
        };
        root.addView(sceneShade, new FrameLayout.LayoutParams(-1, -1));

        // darker at the top for the clock and at the bottom for the dock, the bay clear in between
        View scrim = Backdrop.scrim(this);
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        home = new FrameLayout(this);
        home.setClipChildren(false);
        root.addView(home, new FrameLayout.LayoutParams(-1, -1));

        buildClock();
        buildMessages();
        buildShelf();
        setContentView(root);
        // one frame glides between the dock's tiles; anything else hides it
        ring = new FocusRing(this, root);
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((old, now) -> {
            boolean tile = now != null && now.getParent() == shelf && now instanceof FrameLayout;
            if (tile) ring.follow(now);
            else ring.hide();
        });

        weather.setListener(this);
        tg.setListener(this);
        tg.start();
        KeyWatchService.start(this);
        Dim.apply(getApplicationContext());
        applyScene();
        testing(getIntent());
    }

    /** Testing: --es phase sunset pins a moment of the day, --ef demo 120 runs the day 120× faster, --ez saver true opens the screensaver,
     * --es page scene|calendar|places|telegram opens that settings page, --es style and --ez bare compare the scenes. */
    private void testing(Intent intent) {
        // the activity is exported (HOME), so any app could send these: only a debug build listens
        if (!BuildConfig.DEBUG) return;
        try {
            testExtras(intent);
        } catch (RuntimeException e) {
            android.util.Log.w("Home", "testing extras: " + e);
        }
    }

    private void testExtras(Intent intent) {
        String phase = intent.getStringExtra("phase");
        if (phase != null) {
            int[] at = {7 * 60 + 10, 13 * 60 + 40, 18 * 60 + 5, 22 * 60 + 30};
            scene.setForcedMinute(at[Palette.Phase.valueOf(phase.toUpperCase(Locale.ROOT)).ordinal()]);
            video.update();
            updateGlass();
        }
        // --ez animated false shows the still pictures instead of the clips, --ez bare true hides everything over the bay
        if (intent.hasExtra("animated")) showAnimated(intent.getBooleanExtra("animated", true));
        if (intent.hasExtra("bare")) {
            boolean bare = intent.getBooleanExtra("bare", false);
            for (int i = 0; i < root.getChildCount(); i++) {
                View v = root.getChildAt(i);
                if (v != scene && v != video && v != sceneShade) v.setVisibility(bare ? View.INVISIBLE : View.VISIBLE);
            }
        }
        float demo = intent.getFloatExtra("demo", 0);
        if (demo > 0) scene.setDemoSpeed(demo);
        // --ez saver true shows the screensaver, as «Показать сейчас» does
        if (intent.getBooleanExtra("saver", false)) startActivity(new Intent(this, SaverActivity.class));
        // --ez quick true opens the quick panel, as holding Prime Video does (the emulator has no such button)
        if (intent.getBooleanExtra("quick", false)) QuickPanel.show(this);
        // --ei sleep 5 runs the sleep timer out in five seconds
        int sleepIn = intent.getIntExtra("sleep", 0);
        if (sleepIn > 0) Actions.sleepIn(this, sleepIn);
        String page = intent.getStringExtra("page");
        // after layout: pages slide by the screen's height, which is 0 until then
        if (page != null) root.post(() -> {
            openPanel();
            if ("scene".equals(page)) openScene();
            else if ("calendar".equals(page)) openCalendar();
            else if ("places".equals(page)) openPlaces();
            else if ("telegram".equals(page)) openTelegram();
            else if ("remote".equals(page)) openRemote();
            else if ("panel".equals(page)) openPanelSettings(null, null);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        scene.setRunning(true);
        video.setRunning(true);
        reloadShelf();
        renderMessages();
        // the first focus lands before the window is shown and fires no focus change: frame it by hand
        root.post(() -> {
            View f = getCurrentFocus();
            if (f != null && f.getParent() == shelf) ring.follow(f);
        });
        handler.removeCallbacks(tick);
        tick.run();
    }

    @Override
    protected void onPause() {
        scene.setRunning(false);
        video.setRunning(false);
        handler.removeCallbacks(tick);
        releaseMedia();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        testing(intent);
        // Home pressed while already home: close whatever is open and go back to the first app.
        if (overlay != null) closeOverlay();
        if (menu != null) closeTileMenu();
        if (movingPkg != null) cancelMove();
        shelfScroll.smoothScrollTo(0, 0);
        if (shelf.getChildCount() > 0) shelf.getChildAt(0).requestFocus();
    }

    // ---------------------------------------------------------------- clock, weather, phase

    /** Top left: a greeting, the thin clock, the date in spaced capitals, the weather, and a storm warning. */
    private void buildClock() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        greeting = Ui.text(this, "", 18, 500, Color.WHITE);
        greeting.setAlpha(0.82f);
        time = Ui.text(this, "", 112, 200, Color.WHITE);
        time.setSingleLine(true);
        time.setLetterSpacing(-0.045f);
        date = Ui.text(this, "", 15, 600, Color.WHITE);
        date.setLetterSpacing(0.23f);
        date.setAlpha(0.78f);
        weatherLine = Ui.text(this, "", 16, 400, Color.WHITE);
        weatherLine.setAlpha(0.9f);
        for (TextView t : new TextView[]{greeting, time, date, weatherLine}) Ui.shadow(t);
        col.addView(greeting);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
        tl.topMargin = Ui.px(this, 2);
        col.addView(time, tl);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-2, -2);
        dl.topMargin = Ui.px(this, 12);
        col.addView(date, dl);
        // the weather, the offline line and the storm warning are one block the cursor can land on: OK opens
        // the forecast. It pads out around its text so the ring has room, and the text keeps its place.
        weatherBox = new LinearLayout(this);
        weatherBox.setOrientation(LinearLayout.VERTICAL);
        int bp = Ui.px(this, 12);
        weatherBox.setPadding(bp, Ui.px(this, 8), bp, Ui.px(this, 8));
        StateListDrawable wb = new StateListDrawable();
        GradientDrawable wf = Ui.round(Ui.GLASS_FOCUS, 0, 18, this);
        wf.setStroke(Ui.px(this, 3), Color.WHITE);
        wb.addState(new int[]{android.R.attr.state_focused}, wf);
        wb.addState(new int[]{}, Ui.round(0x00FFFFFF, 0, 18, this));
        weatherBox.setBackground(wb);
        weatherBox.setFocusable(true);
        weatherBox.setClickable(true);
        weatherBox.setOnClickListener(v -> openWeather());
        weatherBox.addView(weatherLine);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(-2, -2);
        wl.topMargin = Ui.px(this, 8) - Ui.px(this, 8);
        wl.leftMargin = -bp;
        col.addView(weatherBox, wl);

        // no network: a quiet line saying how old the numbers are
        offline = new LinearLayout(this);
        offline.setGravity(Gravity.CENTER_VERTICAL);
        ImageView oi = new ImageView(this);
        oi.setImageResource(R.drawable.ic_wifi_off);
        offline.addView(oi, new LinearLayout.LayoutParams(Ui.px(this, 20), Ui.px(this, 20)));
        TextView ot = Ui.text(this, "", 16, 500, Color.WHITE);
        Ui.shadow(ot);
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(-2, -2);
        ol.leftMargin = Ui.px(this, 8);
        offline.addView(ot, ol);
        offline.setAlpha(0.75f);
        offline.setVisibility(View.GONE);
        LinearLayout.LayoutParams oll = new LinearLayout.LayoutParams(-2, -2);
        oll.topMargin = Ui.px(this, 8);
        weatherBox.addView(offline, oll);

        // a storm on its way: a warm line under the weather, no box around it
        alert = new LinearLayout(this);
        alert.setGravity(Gravity.CENTER_VERTICAL);
        ImageView ic = new ImageView(this);
        ic.setImageResource(R.drawable.ic_storm);
        ic.setColorFilter(Ui.WARN);
        alert.addView(ic, new LinearLayout.LayoutParams(Ui.px(this, 20), Ui.px(this, 20)));
        TextView at = Ui.text(this, "", 16, 500, Ui.WARN);
        Ui.shadow(at);
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(-2, -2);
        al.leftMargin = Ui.px(this, 8);
        alert.addView(at, al);
        alert.setVisibility(View.GONE);
        LinearLayout.LayoutParams all = new LinearLayout.LayoutParams(-2, -2);
        all.topMargin = Ui.px(this, 8);
        weatherBox.addView(alert, all);

        // the day's important events from the calendars, plain lines like the weather
        today = new LinearLayout(this);
        today.setOrientation(LinearLayout.VERTICAL);
        today.setVisibility(View.GONE);
        LinearLayout.LayoutParams tdl = new LinearLayout.LayoutParams(-2, -2);
        tdl.topMargin = Ui.px(this, 14);
        col.addView(today, tdl);

        // the variable font measures narrower than it draws, so the block gets room of its own
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(Ui.px(this, 640), -2);
        fl.leftMargin = Ui.px(this, 64);
        fl.topMargin = Ui.px(this, 48);
        home.addView(col, fl);
    }

    private void updateClock() {
        Date now = new Date();
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(h < 5 ? "Доброй ночи" : h < 12 ? "Доброе утро" : h < 17 ? "Добрый день" : h < 23 ? "Добрый вечер" : "Доброй ночи");
        time.setText(new SimpleDateFormat("HH:mm", RU).format(now));
        date.setText(new SimpleDateFormat("EEEE, d MMMM", RU).format(now).toUpperCase(RU));
    }

    /** Without a network the weather line stays, and a line under it says how old it is. */
    private void updateOffline() {
        boolean none = !online();
        offline.setVisibility(none ? View.VISIBLE : View.GONE);
        long at = weather.updatedAt();
        if (none) ((TextView) offline.getChildAt(1)).setText(at == 0 ? "Нет интернета" : "Нет интернета — погода и события от "
                + new SimpleDateFormat("HH:mm", RU).format(new Date(at)));
    }

    private boolean online() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return true;
        }
    }

    /** «СЕГОДНЯ»: up to three of the day's events still ahead, a dot in the calendar's colour, time, title. */
    private void renderToday() {
        List<Cal.Event> list = Cal.today(this);
        today.removeAllViews();
        today.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        if (list.isEmpty()) return;
        TextView cap = Ui.text(this, "СЕГОДНЯ", 15, 600, Color.WHITE);
        cap.setLetterSpacing(0.23f);
        cap.setAlpha(0.7f);
        Ui.shadow(cap);
        today.addView(cap);
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            Cal.Event e = list.get(i);
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(e.color);
            dot.setBackground(d);
            r.addView(dot, new LinearLayout.LayoutParams(Ui.px(this, 8), Ui.px(this, 8)));
            TextView t = Ui.text(this, e.allDay ? "весь день" : new SimpleDateFormat("HH:mm", RU).format(new Date(e.start)), 17, 600, Color.WHITE);
            t.setFontFeatureSettings("tnum");
            Ui.shadow(t);
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
            tl.leftMargin = Ui.px(this, 10);
            r.addView(t, tl);
            TextView n = Ui.text(this, e.title, 17, 400, 0xEBFFFFFF);
            n.setSingleLine(true);
            n.setEllipsize(TextUtils.TruncateAt.END);
            Ui.shadow(n);
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(Ui.px(this, 440), -2);
            nl.leftMargin = Ui.px(this, 10);
            r.addView(n, nl);
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
            rl.topMargin = Ui.px(this, 8);
            today.addView(r, rl);
        }
    }

    /** The scene follows the clock by itself; it only needs today's sunrise and sunset. */
    private void updatePhase() {
        if (weather.ready()) scene.setSun(weather.sunMinutes("sunrise"), weather.sunMinutes("sunset"));
        if (weather.ready()) weatherLine.setText(weather.placeName() + " · " + weather.line());
    }

    @Override
    public void onWeather(Weather w) {
        if (w.ready()) weatherLine.setText(w.placeName() + " · " + w.line());
        String a = w.ready() ? w.alert() : null;
        alert.setVisibility(a == null ? View.GONE : View.VISIBLE);
        if (a != null) ((TextView) alert.getChildAt(1)).setText(a);
        updatePhase();
    }

    // ---------------------------------------------------------------- Telegram notes

    /** Where the notes column ends: just above the line with the focused app's name. */
    private static final float NOTES_TOP = 40, NOTES_BOTTOM = 470;
    private View notesFrame;

    /**
     * Every unread note, newest on top, in a column on the right that ends above the dock. The cursor walks
     * down the notes and the column scrolls with it; at both ends the notes fade out instead of being cut.
     */
    private void buildMessages() {
        // the fades are the frame's (FadeFrame): the scroll view's own stop at its padding and leave a band
        ScrollView sc = new GlideScroll(this) {
            @Override
            protected float getTopFadingEdgeStrength() {
                return 0;
            }

            @Override
            protected float getBottomFadingEdgeStrength() {
                return 0;
            }
        };
        sc.setVerticalScrollBarEnabled(false);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sc.setClipToPadding(false);
        // a focused note grows past the list's width; only the outer frame may cut
        sc.setClipChildren(false);
        sc.setVerticalFadingEdgeEnabled(true);
        sc.setFadingEdgeLength(Ui.px(this, 44));
        sc.setPadding(Ui.px(this, 10), Ui.px(this, 12), Ui.px(this, 10), Ui.px(this, 12));
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        sc.addView(messages, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(Ui.px(this, 360), Ui.px(this, NOTES_BOTTOM - NOTES_TOP), Gravity.END | Gravity.TOP);
        fl.rightMargin = Ui.px(this, 54);
        fl.topMargin = Ui.px(this, NOTES_TOP);
        // Home lets things grow past their bounds, so the column needs a frame of its own that cuts it
        FadeFrame frame = new FadeFrame(this, sc, Ui.px(this, 44));
        notesFrame = frame;
        frame.setClipChildren(true);
        frame.addView(sc, new FrameLayout.LayoutParams(-1, -1));
        home.addView(frame, fl);
        notesScroll = sc;
        // how many notes are hidden past each end of the column
        for (int i = 0; i < 2; i++) {
            TextView t = Ui.text(this, "", 15, 600, Color.WHITE);
            t.setGravity(Gravity.CENTER);
            t.setAlpha(0.8f);
            Ui.shadow(t);
            t.setVisibility(View.INVISIBLE);
            FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(Ui.px(this, 340), -2, Gravity.END | Gravity.TOP);
            ml.rightMargin = Ui.px(this, 64);
            ml.topMargin = Ui.px(this, i == 0 ? NOTES_TOP - 18 : NOTES_BOTTOM - 4);
            home.addView(t, ml);
            more[i] = t;
        }
        sc.setOnScrollChangeListener((v, x, y, ox, oy) -> {
            updateMore();
            frame.invalidate();
        });
    }

    /**
     * The notes' frame: cuts the column and fades it out at an end with more notes past it. Drawn as a mask
     * over the whole frame, so a card fades as one piece, its border and the part in the list's padding too.
     */
    private static final class FadeFrame extends FrameLayout {
        private final ScrollView sc;
        private final int edge;
        private final Paint mask = new Paint();

        FadeFrame(Context c, ScrollView sc, int edge) {
            super(c);
            this.sc = sc;
            this.edge = edge;
            mask.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
            setClipChildren(true);
        }

        @Override
        protected void dispatchDraw(Canvas c) {
            View list = sc.getChildAt(0);
            int w = getWidth(), h = getHeight();
            float above = Math.min(1f, sc.getScrollY() / (float) edge);
            float below = list == null ? 0 : Math.min(1f, Math.max(0, list.getBottom() + sc.getPaddingBottom() - sc.getScrollY() - sc.getHeight()) / (float) edge);
            if (above == 0 && below == 0) {
                super.dispatchDraw(c);
                return;
            }
            int save = c.saveLayer(0, 0, w, h, null);
            super.dispatchDraw(c);
            if (above > 0) {
                mask.setShader(new android.graphics.LinearGradient(0, 0, 0, edge,
                        Color.argb(Math.round(255 * (1 - above)), 0, 0, 0), Color.BLACK, android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(0, 0, w, edge, mask);
            }
            if (below > 0) {
                mask.setShader(new android.graphics.LinearGradient(0, h - edge, 0, h,
                        Color.BLACK, Color.argb(Math.round(255 * (1 - below)), 0, 0, 0), android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(0, h - edge, w, h, mask);
            }
            c.restoreToCount(save);
        }
    }

    /** {@link FadeFrame} sideways, for the dock: masks the ends where tiles go on out of sight. */
    private static final class DockFade extends FrameLayout {
        private final android.widget.HorizontalScrollView sc;
        private final int edge;
        private final Paint mask = new Paint();

        DockFade(Context c, android.widget.HorizontalScrollView sc, int edge) {
            super(c);
            this.sc = sc;
            this.edge = edge;
            mask.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
            setClipChildren(false);
            sc.setOnScrollChangeListener((v, x, y, ox, oy) -> invalidate());
        }

        @Override
        protected void dispatchDraw(Canvas c) {
            View row = sc.getChildAt(0);
            int w = getWidth(), h = getHeight();
            float before = Math.min(1f, sc.getScrollX() / (float) edge);
            float after = row == null ? 0 : Math.min(1f, Math.max(0, row.getRight() + sc.getPaddingRight() - sc.getScrollX() - sc.getWidth()) / (float) edge);
            if (before == 0 && after == 0) {
                super.dispatchDraw(c);
                return;
            }
            int save = c.saveLayer(0, 0, w, h, null);
            super.dispatchDraw(c);
            if (before > 0) {
                mask.setShader(new android.graphics.LinearGradient(0, 0, edge, 0,
                        Color.argb(Math.round(255 * (1 - before)), 0, 0, 0), Color.BLACK, android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(0, 0, edge, h, mask);
            }
            if (after > 0) {
                mask.setShader(new android.graphics.LinearGradient(w - edge, 0, w, 0,
                        Color.BLACK, Color.argb(Math.round(255 * (1 - after)), 0, 0, 0), android.graphics.Shader.TileMode.CLAMP));
                c.drawRect(w - edge, 0, w, h, mask);
            }
            c.restoreToCount(save);
        }
    }

    private ScrollView notesScroll;
    private final TextView[] more = new TextView[2];

    /** «↑ ещё 2» over the column, «↓ ещё 3» under it: notes wholly out of sight at either end. */
    private void updateMore() {
        int top = notesScroll.getScrollY() + notesScroll.getPaddingTop(), bottom = notesScroll.getScrollY() + notesScroll.getHeight() - notesScroll.getPaddingBottom();
        int above = 0, below = 0;
        for (int i = 0; i < messages.getChildCount(); i++) {
            View c = messages.getChildAt(i);
            if (c.getBottom() <= top) above++;
            else if (c.getTop() >= bottom) below++;
        }
        more[0].setText("↑ ещё " + above);
        more[0].setVisibility(above > 0 ? View.VISIBLE : View.INVISIBLE);
        more[1].setText("↓ ещё " + below);
        more[1].setVisibility(below > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * A scroll view that brings the focused child into view smoothly and keeps its padding around it: the
     * stock one jumps in a single frame and stops flush with the edge, cutting a zoomed tile or card.
     */
    private static class GlideScroll extends ScrollView {
        GlideScroll(Context c) {
            super(c);
            // only what it holds takes the cursor: an empty column must not swallow it
            setFocusable(false);
        }

        @Override
        protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) {
            return 0;
        }

        @Override
        public void requestChildFocus(View child, View focused) {
            super.requestChildFocus(child, focused);
            android.graphics.Rect r = new android.graphics.Rect();
            focused.getDrawingRect(r);
            offsetDescendantRectToMyCoords(focused, r);
            // keep the neighbours peeking out of the fades: leave a fade's length free around the focused one
            int edge = getVerticalFadingEdgeLength() > 0 && isVerticalFadingEdgeEnabled() ? getVerticalFadingEdgeLength() : 0;
            int top = getScrollY() + getPaddingTop() + edge, bottom = getScrollY() + getHeight() - getPaddingBottom() - edge;
            int d = r.top < top ? r.top - top : r.bottom > bottom ? r.bottom - bottom : 0;
            if (d != 0) smoothScrollBy(0, d);
        }
    }

    @Override
    public void onChanged() {
        renderMessages();
        if (overlay != null && overlay.getTag() == "panel" && tg.people("pending").size() != panelAsks) {
            closeOverlay();
            openPanel();
        }
        if (overlay != null && overlay.getTag() == "telegram" && dialog == null) {
            View f = getCurrentFocus();
            Object tag = f == null ? null : f.getTag();
            closeOverlay();
            openTelegram();
            if (tag != null) {
                View again = overlay.findViewWithTag(tag);
                if (again != null) again.requestFocus();
            }
        }
    }

    private static final int NOTE_GLASS = Ui.DARK_GLASS, NOTE_LINE = 0x24FFFFFF;

    private void renderMessages() {
        View was = getCurrentFocus();
        boolean onNote = was != null && was.getParent() == messages;
        releaseMedia();
        messages.removeAllViews();
        List<Telegram.Msg> list = tg.messages();
        for (Telegram.Msg m : list) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(Ui.px(this, 18), Ui.px(this, 16), Ui.px(this, 18), Ui.px(this, 16));
            Ui.glassBackground(card, NOTE_GLASS, NOTE_LINE, 22);
            // a new note takes the cursor by itself; focused, it shows how to put it away, and OK does it
            card.setId(View.generateViewId());
            card.setFocusable(true);
            card.setClickable(true);
            long id = m.id, from = m.from;
            // what moves or sounds on the card; OK plays it first, if it can be played
            Bitmap b = m.photo == null ? null : decode(m.photo, Ui.px(this, 360));
            int mk = m.clip == null ? -1 : "gif".equals(m.kind) ? NoteMedia.GIF : "round".equals(m.kind) ? NoteMedia.ROUND
                    : "video".equals(m.kind) ? NoteMedia.VIDEO : "voice".equals(m.kind) || "audio".equals(m.kind) ? NoteMedia.SOUND : -1;
            TextView hint = Ui.text(this, "", 15, 400, 0x8C0B0F14);
            NoteMedia media = mk < 0 ? null : new NoteMedia(this, mk, m.clip, b, () -> hint.setText(hintFor(card)));
            card.setTag(R.id.media, media);
            card.setOnClickListener(v -> {
                if (media != null && media.click()) return;
                v.animate().alpha(0f).translationX(Ui.px(this, 40)).setDuration(220).withEndAction(() -> tg.dismiss(from, id)).start();
            });

            LinearLayout head = new LinearLayout(this);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.addView(avatar(m.from, m.name, 30));
            TextView name = Ui.text(this, m.name, 15, 600, Color.WHITE);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, -2, 1);
            nl.leftMargin = Ui.px(this, 10);
            head.addView(name, nl);
            TextView when = Ui.text(this, when(m.date), 15, 400, Ui.TEXT_DIM);
            head.addView(when);
            card.addView(head);

            if (media != null && media.view != null) {
                boolean round = mk == NoteMedia.ROUND;
                LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(round ? Ui.px(this, 170) : -1, Ui.px(this, round ? 170 : 150));
                il.topMargin = Ui.px(this, 12);
                card.addView(media.view, il);
            } else if (b != null && "sticker".equals(m.kind)) {
                // a sticker stands on its own: whole, transparent, not cropped into a frame
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(b);
                iv.setScaleType(ImageView.ScaleType.FIT_START);
                LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(Ui.px(this, 150), Ui.px(this, 150));
                il.topMargin = Ui.px(this, 12);
                card.addView(iv, il);
            } else if (b != null) {
                FrameLayout pic = new FrameLayout(this);
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(b);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                pic.addView(iv, new FrameLayout.LayoutParams(-1, -1));
                if ("video".equals(m.kind)) {
                    // a video arrives as its still: say so in the corner
                    TextView tag = Ui.text(this, "Видео", 14, 600, Color.WHITE);
                    tag.setPadding(Ui.px(this, 8), Ui.px(this, 3), Ui.px(this, 8), Ui.px(this, 3));
                    tag.setBackground(Ui.round(0x99000000, 0, 8, this));
                    FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.BOTTOM);
                    tl.leftMargin = tl.bottomMargin = Ui.px(this, 8);
                    pic.addView(tag, tl);
                }
                clipRound(pic, 14);
                LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(-1, Ui.px(this, 150));
                il.topMargin = Ui.px(this, 12);
                card.addView(pic, il);
            }
            int glyph = "voice".equals(m.kind) ? R.drawable.ic_mic : "audio".equals(m.kind) ? R.drawable.ic_music
                    : "place".equals(m.kind) ? R.drawable.ic_pin : 0;
            if (glyph != 0) {
                // nothing to look at: the line says what came, the icon says what kind of thing it is
                LinearLayout line = new LinearLayout(this);
                line.setGravity(Gravity.CENTER_VERTICAL);
                ImageView ic = new ImageView(this);
                ic.setImageResource(glyph);
                ic.setTag("glyph");
                line.addView(ic, new LinearLayout.LayoutParams(Ui.px(this, 22), Ui.px(this, 22)));
                TextView body = Ui.text(this, m.text, 17, 400, Color.WHITE);
                body.setLineSpacing(0, 1.25f);
                body.setMaxLines(3);
                body.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, -2, 1);
                bl.leftMargin = Ui.px(this, 10);
                line.addView(body, bl);
                LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-1, -2);
                ll.topMargin = Ui.px(this, 10);
                card.addView(line, ll);
                if (media != null) {
                    LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-1, Ui.px(this, 4));
                    pl.topMargin = Ui.px(this, 10);
                    card.addView(media.bar, pl);
                }
            } else if (m.text != null && !m.text.isEmpty()) {
                TextView body = Ui.text(this, m.text, 17, 400, Color.WHITE);
                body.setLineSpacing(0, 1.25f);
                body.setMaxLines(5);
                body.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
                bl.topMargin = Ui.px(this, 10);
                card.addView(body, bl);
            }
            if (media != null && media.view != null) {
                LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-1, Ui.px(this, 4));
                pl.topMargin = Ui.px(this, 10);
                card.addView(media.bar, pl);
            }
            // under the cursor the card says what the remote does with it
            hint.setText(hintFor(card));
            hint.setVisibility(View.GONE);
            hint.setTag("hint");
            LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
            hl.topMargin = Ui.px(this, 10);
            card.addView(hint, hl);
            card.setOnFocusChangeListener((v, has) -> {
                if (has) v.setBackground(Ui.round(0xF0FFFFFF, Color.WHITE, 22, this));
                else Ui.glassBackground(v, NOTE_GLASS, NOTE_LINE, 22);
                recolor((ViewGroup) v, has);
                v.findViewWithTag("hint").setVisibility(has ? View.VISIBLE : View.GONE);
                Ui.grow(v, has);
                if (media != null) media.focus(has);
                // the app's name over the dock belongs to the dock: it goes while the cursor is up here
                if (has) focusedName.animate().alpha(0f).setDuration(150).start();
            });
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, -2);
            cl.bottomMargin = Ui.px(this, 12);
            messages.addView(card, cl);
        }
        // A note that came in since the last look gets the cursor, so reading it is one press of OK; after
        // OK the next note takes over, and with none left the cursor goes back to the app it came from.
        messages.post(this::updateMore);
        long newest = list.isEmpty() ? 0 : list.get(0).date;
        boolean fresh = newest > seenMsg;
        seenMsg = Math.max(seenMsg, newest);
        if (overlay != null || movingPkg != null || menu != null || shelf == null) return;
        if (!list.isEmpty() && (onNote || fresh)) messages.getChildAt(0).requestFocus();
        else if (onNote || getCurrentFocus() == null) backToShelf();
    }

    /** A picture shrunk on decode to about {@code max} pixels wide: a sent image file can be 4000 px across. */
    private static Bitmap decode(String path, int max) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        if (o.outWidth <= 0) return null;
        o.inJustDecodeBounds = false;
        o.inSampleSize = 1;
        while (o.outWidth / (o.inSampleSize * 2) >= max) o.inSampleSize *= 2;
        return BitmapFactory.decodeFile(path, o);
    }

    /** A focused note turns white; its text turns dark to stay readable. */
    private static void recolor(ViewGroup g, boolean dark) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View ch = g.getChildAt(i);
            if (ch instanceof ImageView && "glyph".equals(ch.getTag())) {
                ((ImageView) ch).setImageTintList(android.content.res.ColorStateList.valueOf(dark ? Ui.INK : Color.WHITE));
            } else if (ch instanceof TextView && !"hint".equals(ch.getTag()) && !"avatar".equals(ch.getTag()) && !(ch.getParent() instanceof FrameLayout)) {
                TextView t = (TextView) ch;
                Object base = t.getTag(R.id.base_color);
                if (base == null) t.setTag(R.id.base_color, t.getCurrentTextColor());
                int c = (int) t.getTag(R.id.base_color);
                t.setTextColor(dark ? (Color.alpha(c) < 255 ? 0x990B0F14 : Ui.INK) : c);
            } else if (ch instanceof ViewGroup) {
                recolor((ViewGroup) ch, dark);
            }
        }
    }

    private static String hintFor(View card) {
        NoteMedia media = (NoteMedia) card.getTag(R.id.media);
        return media != null ? media.hint() : "OK — прочитано, ↓ — к приложениям";
    }

    /** Stops every player on the notes: before they are drawn again, and when Home goes to the background. */
    private void releaseMedia() {
        for (int i = 0; messages != null && i < messages.getChildCount(); i++) {
            NoteMedia media = (NoteMedia) messages.getChildAt(i).getTag(R.id.media);
            if (media != null) media.release();
        }
    }

    private static final int[] AVATAR = {0xFFE17076, 0xFFF4A15D, 0xFF7BC862, 0xFF65AADD, 0xFFA695E7, 0xFFEE7AAE, 0xFF6EC9CB};

    private View avatar(long user, String name, float size) {
        int s = Ui.px(this, size);
        File f = tg.avatar(user);
        if (f.exists()) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(BitmapFactory.decodeFile(f.getPath()));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline o) {
                    o.setOval(0, 0, v.getWidth(), v.getHeight());
                }
            });
            iv.setClipToOutline(true);
            iv.setLayoutParams(new LinearLayout.LayoutParams(s, s));
            return iv;
        }
        // no photo: the first letter on a colour of its own, picked by the person, as Telegram does
        TextView t = Ui.text(this, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(), size * 0.45f, 600, Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setTag("avatar");
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(AVATAR[(int) Math.floorMod(user, (long) AVATAR.length)]);
        t.setBackground(g);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return t;
    }

    private String when(long unix) {
        Calendar now = Calendar.getInstance(), c = Calendar.getInstance();
        c.setTimeInMillis(unix * 1000);
        String hm = new SimpleDateFormat("HH:mm", RU).format(c.getTime());
        if (now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == c.get(Calendar.YEAR)) return hm;
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)) return "вчера";
        return new SimpleDateFormat("d MMM", RU).format(c.getTime());
    }

    private void clipRound(View v, float r) {
        int rad = Ui.px(this, r);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), rad);
            }
        });
        v.setClipToOutline(true);
    }

    // ---------------------------------------------------------------- dock

    // dock geometry, design units: a glass band 152 high, 64 from the screen's sides and 40 from its bottom;
    // banners 190 × 107, 20 apart, starting 26 in; five and the edge of a sixth fit
    private static final float DOCK_SIDE = 64, DOCK_BOTTOM = 40, DOCK_H = 152, DOCK_IN = 26;
    private static final float TILE_W = 190, TILE_H = 107, GAP = 20;

    private View lastTile;
    private long seenMsg;
    private View dockDim;

    /**
     * One row of every app: those pinned by hand stand where they were put, the rest follow by how much they
     * are used, the most used on the left. The row scrolls sideways and fades out at the right edge.
     */
    private void buildShelf() {
        // the menu over a tile dims everything but the dock
        dockDim = new View(this);
        dockDim.setBackgroundColor(Ui.SCRIM);
        dockDim.setAlpha(0f);
        dockDim.setVisibility(View.GONE);
        home.addView(dockDim, new FrameLayout.LayoutParams(-1, -1));

        focusedName = Ui.text(this, "", 30, 600, Color.WHITE);
        focusedName.setLetterSpacing(-0.01f);
        Ui.shadow(focusedName);
        focusedName.setSingleLine(true);
        focusedName.setEllipsize(TextUtils.TruncateAt.END);
        // as wide as the name, so the pin of a pinned app sits right after it
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        nl.leftMargin = Ui.px(this, DOCK_SIDE + 22);
        nl.rightMargin = Ui.px(this, DOCK_SIDE);
        nl.bottomMargin = Ui.px(this, 206);
        home.addView(focusedName, nl);

        FrameLayout band = new FrameLayout(this);
        Ui.glassBackground(band, Ui.DARK_GLASS, Ui.GLASS_LINE, 34);
        shelfScroll = new HGlideScroll(this);
        shelfScroll.setHorizontalScrollBarEnabled(false);
        shelfScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        shelfScroll.setClipToPadding(false);
        // the band clips; the scroll view itself must not, or the zoomed end tiles lose an edge
        shelfScroll.setClipChildren(false);
        int padY = Math.round(Ui.px(this, (DOCK_H - TILE_H) / 2f));
        shelfScroll.setPadding(Ui.px(this, DOCK_IN), padY, Ui.px(this, DOCK_IN), padY);
        shelf = new LinearLayout(this);
        shelf.setClipChildren(false);
        shelfScroll.addView(shelf);
        // the dock fades out at either end where there are more tiles: the scroll view's own fading edge stops at
        // its padding, and a tile under the padding was cut off hard
        DockFade fade = new DockFade(this, shelfScroll, Ui.px(this, 120));
        fade.addView(shelfScroll, new FrameLayout.LayoutParams(-1, -1));
        band.addView(fade, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-1, Ui.px(this, DOCK_H), Gravity.BOTTOM);
        fl.leftMargin = fl.rightMargin = Ui.px(this, DOCK_SIDE);
        fl.bottomMargin = Ui.px(this, DOCK_BOTTOM);
        home.addView(band, fl);

        // the arrows beside a tile that is being moved; they ride with it while the dock scrolls
        for (int i = 0; i < 2; i++) {
            TextView a = Ui.text(this, i == 0 ? "‹" : "›", 22, 600, Ui.INK);
            a.setGravity(Gravity.CENTER);
            a.setBackground(Ui.round(Color.WHITE, 0, 15, this));
            a.setElevation(Ui.px(this, 14));
            a.setVisibility(View.GONE);
            home.addView(a, new FrameLayout.LayoutParams(Ui.px(this, 30), Ui.px(this, 30)));
            moveArrows[i] = a;
        }
        home.getViewTreeObserver().addOnPreDrawListener(() -> {
            if (movingPkg != null) placeArrows();
            return true;
        });
    }

    private final TextView[] moveArrows = new TextView[2];

    /** {@link GlideScroll}, sideways: glides to the focused tile and keeps the band's padding around it. */
    private static final class HGlideScroll extends android.widget.HorizontalScrollView {
        HGlideScroll(Context c) {
            super(c);
            setFocusable(false);
        }

        @Override
        protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) {
            return 0;
        }

        /** Only the right edge fades: the dock always starts with its first tile in plain view. */
        @Override
        protected float getLeftFadingEdgeStrength() {
            return 0f;
        }

        @Override
        public void requestChildFocus(View child, View focused) {
            super.requestChildFocus(child, focused);
            android.graphics.Rect r = new android.graphics.Rect();
            focused.getDrawingRect(r);
            offsetDescendantRectToMyCoords(focused, r);
            // the tile under the cursor stays clear of the fade on the right
            int left = getScrollX() + getPaddingLeft(), right = getScrollX() + getWidth() - getPaddingRight() - getHorizontalFadingEdgeLength() / 2;
            int d = r.left < left ? r.left - left : r.right > right ? r.right - right : 0;
            if (d != 0) smoothScrollBy(d, 0);
        }
    }

    private void reloadShelf() {
        if (movingPkg != null || menu != null) return;
        View f = getCurrentFocus();
        String keep = f != null && f.getParent() == shelf ? (String) f.getTag() : null;
        order = apps.ordered();
        renderShelf(keep);
    }

    /** Every app in one row. Focus goes to {@code focusPkg}. */
    private void renderShelf(String focusPkg) {
        boolean hadFocus = getCurrentFocus() == null || getCurrentFocus().getParent() == shelf;
        shelf.removeAllViews();
        View focus = null;
        int w = Ui.px(this, TILE_W), h = Ui.px(this, TILE_H);
        for (int i = 0; i < order.size(); i++) {
            Apps.App a = order.get(i);
            View t = appTile(a, i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.rightMargin = i == order.size() - 1 ? 0 : Ui.px(this, GAP);
            shelf.addView(t, lp);
            if (a.pkg.equals(focusPkg)) focus = t;
        }
        if (focus == null && shelf.getChildCount() > 0) focus = shelf.getChildAt(0);
        // under an open panel the shelf is hidden: it must not take the focus away from the panel
        if (focus != null && overlay == null && hadFocus) focus.requestFocus();
        lastTile = focus;
        dimOthers(movingPkg != null ? focus : null);
    }

    /** While a tile is being moved or has its menu open, the other tiles step back. */
    private void dimOthers(View keep) {
        for (int i = 0; i < shelf.getChildCount(); i++) {
            View t = shelf.getChildAt(i);
            t.animate().alpha(keep == null || t == keep ? 1f : 0.5f).setDuration(160).start();
        }
    }

    /** An app tile: its TV banner, or its icon and name when it has none. */
    private View appTile(Apps.App a, int index) {
        FrameLayout t = new FrameLayout(this);
        t.setTag(a.pkg);
        t.setFocusable(true);
        t.setClickable(true);
        t.setLongClickable(true);
        clipRound(t, 18);
        android.graphics.drawable.Drawable banner = apps.banner(a);
        if (banner != null) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(banner);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            t.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            t.setBackgroundColor(0xFF1F2A36);
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(apps.icon(a));
            inner.addView(iv, new LinearLayout.LayoutParams(Ui.px(this, 40), Ui.px(this, 40)));
            TextView l = Ui.text(this, a.label, 15, 500, Color.WHITE);
            l.setSingleLine(true);
            l.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-2, -2);
            ll.topMargin = Ui.px(this, 6);
            inner.addView(l, ll);
            t.addView(inner, new FrameLayout.LayoutParams(-1, -1));
        }
        t.setOnFocusChangeListener((v, has) -> {
            // the tile under the cursor lifts out of the dock; the ring follows it there
            v.animate().scaleX(has ? FocusRing.TILE_SCALE : 1f).scaleY(has ? FocusRing.TILE_SCALE : 1f)
                    .translationY(has ? -Ui.px(this, 8) : 0).translationZ(has ? 8 : 0)
                    .setInterpolator(FocusRing.EASE).setDuration(FocusRing.MS).start();
            if (has) {
                lastTile = v;
                if (movingPkg == null) {
                    focusedName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 30));
                    focusedName.setText(a.label);
                    pinMark(apps.pinned(a.pkg));
                    focusedName.setAlpha(0.2f);
                    focusedName.animate().alpha(1f).setDuration(180).start();
                }
            }
        });
        t.setOnClickListener(v -> {
            if (movingPkg != null) finishMove();
            else apps.open(a.pkg);
        });
        t.setOnLongClickListener(v -> {
            openTileMenu(v, a, index);
            return true;
        });
        return t;
    }

    // ---------------------------------------------------------------- the tile menu, moving apps

    private View menu;
    private View menuTile;

    /**
     * Holding OK on an app opens a small glass menu right above it: a line on how the app is placed (pinned
     * to its spot, or following how much it is used), then pin it here or unpin it, move it, cancel.
     */
    private void openTileMenu(View tile, Apps.App a, int index) {
        menuTile = tile;
        boolean pinned = apps.pinned(a.pkg);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.px(this, 10);
        card.setPadding(p, p, p, p);
        card.setBackground(Ui.round(Ui.SURFACE, 0x29FFFFFF, 28, this));
        card.setElevation(Ui.px(this, 24));
        TextView cap = Ui.text(this, pinned ? "Закреплено, стоит слева" : "Сейчас стоит по частоте запусков", 15, 400, Ui.TEXT_DIM);
        cap.setPadding(Ui.px(this, 14), Ui.px(this, 8), Ui.px(this, 14), Ui.px(this, 10));
        card.addView(cap);
        View first = menuRow(card, pinned ? R.drawable.ic_close : R.drawable.ic_pushpin, pinned ? "Открепить" : "Закрепить", v -> {
            // a pinned app joins the others at the left, after them
            if (pinned) apps.unpin(a.pkg);
            else apps.moveTo(a.pkg, apps.pinnedCount());
            closeTileMenu();
            reloadShelf();
        });
        menuRow(card, R.drawable.ic_move, "Переместить", v -> {
            closeTileMenu();
            startMove(a.pkg, index);
        });
        // the system's own page of the app (stop it, clear its data, permissions) and its own uninstall dialog
        menuRow(card, R.drawable.ic_settings, "О приложении", v -> {
            closeTileMenu();
            startSystem(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + a.pkg)));
        });
        menuRow(card, R.drawable.ic_trash, "Удалить", v -> {
            closeTileMenu();
            startSystem(new Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:" + a.pkg)));
        });
        menuRow(card, R.drawable.ic_close, "Отмена", v -> closeTileMenu());

        // centred over the tile, its bottom just above the dock
        int[] at = new int[2], h = new int[2];
        tile.getLocationInWindow(at);
        home.getLocationInWindow(h);
        int w = Ui.px(this, 300);
        int cx = at[0] - h[0] + Math.round(tile.getWidth() * tile.getScaleX() / 2f);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(w, -2, Gravity.BOTTOM | Gravity.START);
        fl.leftMargin = Math.max(Ui.px(this, DOCK_SIDE), Math.min(home.getWidth() - Ui.px(this, DOCK_SIDE) - w, cx - w / 2));
        fl.bottomMargin = Ui.px(this, DOCK_BOTTOM + DOCK_H + 14);
        home.addView(card, fl);
        menu = card;
        focusedName.setVisibility(View.INVISIBLE);
        dockDim.setVisibility(View.VISIBLE);
        dockDim.animate().alpha(1f).setDuration(160).start();
        // the dock and its tiles stay above the dim
        ((View) shelfScroll.getParent().getParent()).bringToFront();
        card.bringToFront();
        dimOthers(tile);
        card.setAlpha(0f);
        card.setTranslationY(Ui.px(this, 12));
        card.animate().alpha(1f).translationY(0).setInterpolator(FocusRing.EASE).setDuration(FocusRing.MS).start();
        first.requestFocus();
        // the cursor is in the menu now, but the tile it belongs to stays lifted out of the dock
        tile.animate().scaleX(FocusRing.TILE_SCALE).scaleY(FocusRing.TILE_SCALE).translationY(-Ui.px(this, 8)).setDuration(FocusRing.MS).start();
    }

    private void startSystem(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            android.util.Log.w("Home", "no screen for " + i, e);
        }
    }

        /** A menu line: icon and name; under the cursor it turns white, like the selected row of a list. */
    private View menuRow(LinearLayout card, int icon, String label, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.px(this, 14), 0, Ui.px(this, 14), 0);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, Ui.round(Color.WHITE, 0, 18, this));
        bg.addState(new int[]{}, Ui.round(0x00FFFFFF, 0, 18, this));
        r.setBackground(bg);
        android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{Ui.INK, Color.WHITE});
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setImageTintList(ink);
        iv.setDuplicateParentStateEnabled(true);
        r.addView(iv, new LinearLayout.LayoutParams(Ui.px(this, 20), Ui.px(this, 20)));
        TextView t = Ui.text(this, label, 17, 500, Color.WHITE);
        t.setTextColor(ink);
        t.setDuplicateParentStateEnabled(true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.leftMargin = Ui.px(this, 12);
        r.addView(t, tl);
        r.setFocusable(true);
        r.setClickable(true);
        r.setOnClickListener(click);
        card.addView(r, new LinearLayout.LayoutParams(-1, Ui.px(this, 52)));
        return r;
    }

    private void closeTileMenu() {
        if (menu == null) return;
        View m = menu;
        menu = null;
        m.animate().alpha(0f).setDuration(120).withEndAction(() -> home.removeView(m)).start();
        dockDim.animate().alpha(0f).setDuration(160).withEndAction(() -> dockDim.setVisibility(View.GONE)).start();
        focusedName.setVisibility(View.VISIBLE);
        dimOthers(null);
        if (menuTile != null && menuTile.isAttachedToWindow()) menuTile.requestFocus();
        else backToShelf();
        menuTile = null;
    }

    /** The pin beside the focused app's name, for an app pinned to the left. */
    private void pinMark(boolean on) {
        android.graphics.drawable.Drawable d = null;
        if (on) {
            d = getDrawable(R.drawable.ic_pushpin).mutate();
            d.setTint(Color.WHITE);
            int s = Ui.px(this, 22);
            d.setBounds(0, 0, s, s);
        }
        focusedName.setCompoundDrawablesRelative(null, null, d, null);
        focusedName.setCompoundDrawablePadding(Ui.px(this, 12));
    }

    private void startMove(String pkg, int index) {
        pinMark(false);
        movingPkg = pkg;
        // a moved app is pinned: it goes among the pinned ones at the left, not past them
        movingMax = Math.min(order.size() - 1, apps.pinned(pkg) ? apps.pinnedCount() - 1 : apps.pinnedCount());
        if (index > movingMax) {
            order.add(movingMax, order.remove(index));
            index = movingMax;
            renderShelf(pkg);
        }
        movingFrom = movingIndex = index;
        moveLabel();
        for (TextView a : moveArrows) a.setVisibility(View.VISIBLE);
        dimOthers(shelf.getChildAt(index));
    }

    private void moveLabel() {
        focusedName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 22));
        focusedName.setText(order.get(movingIndex).label + "    ← → двигать  ·  OK закрепить  ·  Back отмена");
    }

    /** The arrows sit on the moving tile's sides, half outside it. */
    private void placeArrows() {
        View t = shelf.getChildAt(movingIndex);
        if (t == null || t.getWidth() == 0) return;
        int[] at = new int[2], h = new int[2];
        t.getLocationInWindow(at);
        home.getLocationInWindow(h);
        float x = at[0] - h[0], y = at[1] - h[1];
        float w = t.getWidth() * t.getScaleX(), hh = t.getHeight() * t.getScaleY();
        int s = moveArrows[0].getWidth();
        moveArrows[0].setX(x - s * 0.6f);
        moveArrows[1].setX(x + w - s * 0.4f);
        for (TextView a : moveArrows) a.setY(y + hh / 2f - s / 2f);
    }

    private void step(int delta) {
        int to = Math.max(0, Math.min(movingMax, movingIndex + delta));
        if (to == movingIndex) return;
        Apps.App a = order.remove(movingIndex);
        order.add(to, a);
        movingIndex = to;
        renderShelf(movingPkg);
        moveLabel();
    }

    private void endMove() {
        for (TextView a : moveArrows) a.setVisibility(View.GONE);
        focusedName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 30));
    }

    private void finishMove() {
        String pkg = movingPkg;
        apps.moveTo(pkg, movingIndex);
        movingPkg = null;
        endMove();
        order = apps.ordered();
        renderShelf(pkg);
    }

    private void cancelMove() {
        String pkg = movingPkg;
        movingPkg = null;
        endMove();
        order = apps.ordered();
        renderShelf(pkg);
    }

    // ---------------------------------------------------------------- keys

    /** One page back: settings return to Home, the screens opened from them return to them. */
    private void back() {
        if (dialog != null) {
            closeDialog();
            return;
        }
        Object tag = overlay.getTag();
        goingBack = true;
        closeOverlay();
        if (tag == "places" || tag == "saver" || tag == "scene" || tag == "calendar") openPanel();
        else if (tag == "remote_pick") openRemote();
        else if (tag == "telegram" || tag == "remote") openPanel();
        else goingBack = false;
    }

    /** True when Up has nowhere to go on this page: on a page opened from Home it then turns the page back. */
    private boolean nothingAbove() {
        View f = getCurrentFocus();
        if (dialog != null) return false;
        if (f == null || !isInside(f, overlay)) return true;
        View next = android.view.FocusFinder.getInstance().findNextFocus((ViewGroup) overlay, f, View.FOCUS_UP);
        return next == null;
    }

    /** Moves focus to dock tile {@code i}; false when there is none. */
    private boolean tileAt(int i) {
        if (i < 0 || i >= shelf.getChildCount()) return false;
        shelf.getChildAt(i).requestFocus();
        return true;
    }

    private void backToShelf() {
        View t = lastTile != null && lastTile.getParent() == shelf ? lastTile : shelf.getChildAt(0);
        if (t != null) t.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            int k = e.getKeyCode();
            if (movingPkg != null) {
                if (k == KeyEvent.KEYCODE_DPAD_LEFT) { step(-1); return true; }
                if (k == KeyEvent.KEYCODE_DPAD_RIGHT) { step(1); return true; }
                if (k == KeyEvent.KEYCODE_BACK) { cancelMove(); return true; }
                if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) {
                    if (e.getRepeatCount() == 0) finishMove();
                    return true;
                }
                return true;
            }
            if (menu != null) {
                View f = getCurrentFocus();
                ViewGroup rows = (ViewGroup) menu;
                int i = f == null ? -1 : rows.indexOfChild(f);
                if (k == KeyEvent.KEYCODE_BACK) closeTileMenu();
                else if (k == KeyEvent.KEYCODE_DPAD_UP && i > 1) rows.getChildAt(i - 1).requestFocus();
                else if (k == KeyEvent.KEYCODE_DPAD_DOWN && i >= 1 && i < rows.getChildCount() - 1) rows.getChildAt(i + 1).requestFocus();
                else if (k == KeyEvent.KEYCODE_DPAD_DOWN) closeTileMenu();
                else if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) return super.dispatchKeyEvent(e);
                return true;
            }
            if (overlay == null) {
                View f = getCurrentFocus();
                if (f != null && f.getParent() == shelf) {
                    int i = shelf.indexOfChild(f);
                    switch (k) {
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            tileAt(i - 1);
                            return true;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            tileAt(i + 1);
                            return true;
                        case KeyEvent.KEYCODE_DPAD_UP: {
                            if (messages.getChildCount() > 0) messages.getChildAt(0).requestFocus();
                            else weatherBox.requestFocus();
                            return true;
                        }
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            openPanel();
                            return true;
                        case KeyEvent.KEYCODE_BACK:
                            return true;
                    }
                }
                if (f == weatherBox) {
                    if (k == KeyEvent.KEYCODE_DPAD_DOWN || k == KeyEvent.KEYCODE_BACK) backToShelf();
                    else if (k == KeyEvent.KEYCODE_DPAD_RIGHT && messages.getChildCount() > 0) messages.getChildAt(0).requestFocus();
                    else if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) return super.dispatchKeyEvent(e);
                    return true;
                }
                if (f != null && f.getParent() == messages && k == KeyEvent.KEYCODE_DPAD_LEFT) {
                    weatherBox.requestFocus();
                    return true;
                }
                if (f != null && f.getParent() == messages) {
                    NoteMedia media = (NoteMedia) f.getTag(R.id.media);
                    if (k == KeyEvent.KEYCODE_BACK && media != null && media.back()) return true;
                    if (k == KeyEvent.KEYCODE_BACK
                            || (k == KeyEvent.KEYCODE_DPAD_DOWN && messages.indexOfChild(f) == messages.getChildCount() - 1)) {
                        backToShelf();
                        return true;
                    }
                    if (k == KeyEvent.KEYCODE_DPAD_RIGHT || (k == KeyEvent.KEYCODE_DPAD_UP && messages.indexOfChild(f) == 0)) return true;
                }
                if (k == KeyEvent.KEYCODE_BACK) return true;
            } else if (k == KeyEvent.KEYCODE_BACK) {
                back();
                return true;
            } else if (k == KeyEvent.KEYCODE_DPAD_UP && nothingAbove()) {
                // Up past the top closes only the pages opened from Home; a page inside the menu stays put, so
                // scrolling up to its top cannot throw you out of it by accident: it is left with Back
                Object tag = overlay.getTag();
                if (tag == "panel" || tag == "weather") back();
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }


    // ---------------------------------------------------------------- overlays

    /**
     * Every screen is a sheet: it rises from the bottom while Home slides up and away, and on Back it sinks
     * while Home comes down again. One screen replacing another rises over it; a screen redrawn in place
     * (a setting changed) just swaps without moving.
     */
    private void show(View v, String tag) {
        View old = overlay != null ? overlay : closing;
        boolean redraw = old != null && tag.equals(old.getTag());
        if (overlay != null) {
            // a page opened straight from another one still slides away; a redrawn one is just swapped
            if (redraw) root.removeView(overlay);
            else {
                if (closing != null) root.removeView(closing);
                closing = overlay;
            }
        }
        overlay = v;
        dialog = null;
        v.setTag(tag);
        unclip(v);
        root.addView(v, new FrameLayout.LayoutParams(-1, -1));
        if (redraw) {
            if (closing != null) root.removeView(closing);
            closing = null;
        } else {
            // forward: the new page comes up from below and pushes the old one (or Home) up; back: the reverse
            int dir = goingBack ? -1 : 1, H = root.getHeight();
            View out = old != null ? old : home;
            v.setTranslationY(dir * H);
            v.animate().translationY(0).setInterpolator(FocusRing.EASE).setDuration(SLIDE_MS).withEndAction(() -> {
                if (closing != null) root.removeView(closing);
                closing = null;
            }).start();
            out.animate().alpha(1f).translationY(-dir * H).setInterpolator(FocusRing.EASE).setDuration(SLIDE_MS).start();
        }
        goingBack = false;
        // the hidden home must not keep the remote's focus, nor take it back
        home.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        View f = getCurrentFocus();
        if (f == null || !isInside(f, v)) {
            if (!v.requestFocus() && f != null) f.clearFocus();
        }
    }

    private static final int SLIDE_MS = 340;
    /** The next page arrives from above, as on Back. */
    private boolean goingBack;
    /** A screen just closed: it stays on screen until we know whether another one replaces it. */
    private View closing;

    private static boolean isInside(View child, View parent) {
        for (View p = child; p != null; p = p.getParent() instanceof View ? (View) p.getParent() : null) if (p == parent) return true;
        return false;
    }

    /** Focused controls grow a little; no container may cut their corners off. */
    private static void unclip(View v) {
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        // scrolling lists keep clipping, or their content slides out over the panel; they pad instead
        boolean scrolls = g instanceof ScrollView || g instanceof android.widget.HorizontalScrollView;
        g.setClipChildren(scrolls);
        g.setClipToPadding(false);
        for (int i = 0; i < g.getChildCount(); i++) unclip(g.getChildAt(i));
    }

    private void closeOverlay() {
        if (overlay == null) return;
        View old = overlay;
        overlay = null;
        dialog = null;
        if (closing != null) root.removeView(closing);
        closing = old;
        // usually another screen opens right after; only when none does, this one sinks and Home returns
        root.post(() -> {
            if (closing != old) return;
            if (overlay != null) return; // show() took over and removes it when the new screen is in
            closing = null;
            if (old instanceof ViewGroup) ((ViewGroup) old).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            home.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            View f = getCurrentFocus();
            if (f != null && isInside(f, old)) f.clearFocus();
            // the cursor comes back to the app it left from, not to whatever the system picks
            View now = getCurrentFocus();
            if (now == null || isInside(now, old) || now.getParent() != shelf) backToShelf();
            // the page and Home move as one, back the way they came
            old.animate().translationY(root.getHeight()).setInterpolator(FocusRing.EASE).setDuration(SLIDE_MS)
                    .withEndAction(() -> root.removeView(old)).start();
            home.animate().alpha(1f).translationY(0).setInterpolator(FocusRing.EASE).setDuration(SLIDE_MS).start();
            reloadShelf();
        });
    }

    /**
     * A page: every screen fills the whole screen over the darkened bay and stacks like a vertical pager
     * under Home. A bold title on the left (with a ‹ when it was opened from another page), a quiet status on
     * the right, and the page scrolls as a whole when it is taller than the screen.
     */
    private LinearLayout page(FrameLayout host, String title, String status, boolean sub) {
        // the page is the same frosted glass as the dock, a shade darker so its text reads on the bright day
        Ui.glassBackground(host, Ui.PAGE_GLASS, 0, 0);
        ScrollView sc = new GlideScroll(this);
        sc.setVerticalScrollBarEnabled(false);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sc.setClipToPadding(false);
        sc.setPadding(0, Ui.px(this, 56), 0, Ui.px(this, 48));
        host.addView(sc, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout s = new LinearLayout(this);
        s.setOrientation(LinearLayout.VERTICAL);
        // room under the last row for its cursor ring: the scroll view cuts at the content's end
        s.setPadding(Ui.px(this, 72), 0, Ui.px(this, 72), Ui.px(this, 16));
        sc.addView(s, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        if (sub) {
            ImageView b = new ImageView(this);
            b.setImageResource(R.drawable.ic_chevron_left);
            b.setAlpha(0.6f);
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(Ui.px(this, 28), Ui.px(this, 28));
            bl.rightMargin = Ui.px(this, 10);
            top.addView(b, bl);
        }
        TextView t = Ui.text(this, title, 32, 600, Color.WHITE);
        t.setLetterSpacing(-0.015f);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        if (status != null && !status.isEmpty()) {
            TextView st = Ui.text(this, status, 16, 500, 0xB3FFFFFF);
            top.addView(st);
        }
        s.addView(top);
        return s;
    }

    private FrameLayout host() {
        FrameLayout f = new FrameLayout(this);
        f.setClickable(true);
        return f;
    }

    private TextView label(String s) {
        return Ui.text(this, s, 15, 400, Ui.TEXT_DIM);
    }

    /** A section label with the standard rhythm: 32 above, 14 to what it names. */
    private void section(LinearLayout parent, String s) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = Ui.px(this, 32);
        lp.bottomMargin = Ui.px(this, 14);
        parent.addView(Ui.caps(this, s), lp);
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setClipChildren(false);
        r.setClipToPadding(false);
        return r;
    }

    private void addSpaced(LinearLayout parent, View child, float gap) {
        LinearLayout.LayoutParams lp = child.getLayoutParams() instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) child.getLayoutParams() : new LinearLayout.LayoutParams(-2, -2);
        if (parent.getChildCount() > 0) {
            if (parent.getOrientation() == LinearLayout.HORIZONTAL) lp.leftMargin = Ui.px(this, gap);
            else lp.topMargin = Ui.px(this, gap);
        }
        parent.addView(child, lp);
    }

    /** A pill button: glass, ringed under the cursor, white when it is the selected one of its group. */
    private TextView chip(String s, boolean on, View.OnClickListener click) {
        TextView t = Ui.text(this, s, 16, 600, Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.px(this, 22), 0, Ui.px(this, 22), 0);
        t.setTextColor(Ui.ink());
        t.setBackground(Ui.control(this, 24));
        t.setSelected(on);
        Ui.focusable(t);
        t.setOnClickListener(click);
        t.setLayoutParams(new LinearLayout.LayoutParams(-2, Ui.px(this, 48)));
        return t;
    }

    /**
     * A segmented switch: pills in one glass groove, the chosen one white. Returns the groove; the pill that
     * is on carries the tag {@code "on"}.
     */
    private LinearLayout segmented(String[] names, int on, float width, java.util.function.IntConsumer pick) {
        LinearLayout g = row();
        int p = Ui.px(this, 5);
        g.setPadding(p, p, p, p);
        g.setBackground(Ui.glass(this, 29));
        for (int i = 0; i < names.length; i++) {
            int n = i;
            TextView c = chip(names[i], i == on, v -> pick.accept(n));
            c.setPadding(Ui.px(this, 8), 0, Ui.px(this, 8), 0);
            if (i == on) c.setTag("on");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
            if (i > 0) lp.leftMargin = Ui.px(this, 8);
            g.addView(c, lp);
        }
        g.setLayoutParams(new LinearLayout.LayoutParams(Ui.px(this, width), Ui.px(this, 58)));
        return g;
    }

    // ---------------------------------------------------------------- dialogs

    private View dialog;
    private View dialogFocusBefore;

    /**
     * A dialog over the current page: the page dims to half, a glass card in the middle with an icon in a
     * circle, a title, a line or two and pill buttons across. Back closes it without doing anything. The
     * cursor starts on {@code focus}: for a risky action that is the harmless button.
     */
    private void dialog(int icon, String title, String text, String code, String[] buttons, Runnable[] actions, int focus) {
        if (!(overlay instanceof FrameLayout)) return;
        FrameLayout host = (FrameLayout) overlay;
        dialogFocusBefore = getCurrentFocus();
        FrameLayout layer = new FrameLayout(this);
        layer.setBackgroundColor(Ui.SCRIM);
        layer.setClickable(true);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.px(this, 32), Ui.px(this, 36), Ui.px(this, 32), Ui.px(this, 28));
        card.setBackground(Ui.round(Ui.SURFACE, 0x29FFFFFF, 28, this));
        card.setElevation(Ui.px(this, 24));
        card.setClipChildren(false);
        card.setClipToPadding(false);
        FrameLayout circle = new FrameLayout(this);
        GradientDrawable cg = new GradientDrawable();
        cg.setShape(GradientDrawable.OVAL);
        cg.setColor(0x1AFFFFFF);
        circle.setBackground(cg);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        circle.addView(iv, new FrameLayout.LayoutParams(Ui.px(this, 34), Ui.px(this, 34), Gravity.CENTER));
        card.addView(circle, new LinearLayout.LayoutParams(Ui.px(this, 80), Ui.px(this, 80)));
        TextView t = Ui.text(this, title, 24, 600, Color.WHITE);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = Ui.px(this, 22);
        card.addView(t, tl);
        if (text != null) {
            TextView b = Ui.text(this, text, 16, 400, Ui.TEXT_DIM);
            b.setGravity(Gravity.CENTER);
            b.setLineSpacing(0, 1.3f);
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2);
            bl.topMargin = Ui.px(this, 8);
            card.addView(b, bl);
        }
        if (code != null) {
            TextView c = Ui.text(this, code, 15, 400, 0xD9FFFFFF);
            c.setTypeface(android.graphics.Typeface.MONOSPACE);
            c.setPadding(Ui.px(this, 16), Ui.px(this, 12), Ui.px(this, 16), Ui.px(this, 12));
            c.setBackground(Ui.round(0x4D000000, 0, 14, this));
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, -2);
            cl.topMargin = Ui.px(this, 14);
            card.addView(c, cl);
        }
        LinearLayout btns = row();
        View first = null;
        for (int i = 0; i < buttons.length; i++) {
            Runnable a = actions[i];
            TextView btn = chip(buttons[i], false, v -> {
                closeDialog();
                if (a != null) a.run();
            });
            btn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 17));
            btn.setBackground(Ui.control(this, 26));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.px(this, 52), 1);
            if (i > 0) lp.leftMargin = Ui.px(this, 12);
            btns.addView(btn, lp);
            if (i == focus) first = btn;
        }
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
        bl.topMargin = Ui.px(this, 24);
        card.addView(btns, bl);
        layer.addView(card, new FrameLayout.LayoutParams(Ui.px(this, 520), -2, Gravity.CENTER));
        // the page under the dialog must not take the cursor
        for (int i = 0; i < host.getChildCount(); i++) {
            View ch = host.getChildAt(i);
            if (ch instanceof ViewGroup) ((ViewGroup) ch).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        host.addView(layer, new FrameLayout.LayoutParams(-1, -1));
        unclip(layer);
        dialog = layer;
        layer.setAlpha(0f);
        layer.animate().alpha(1f).setDuration(160).start();
        if (first != null) first.requestFocus();
    }

    private void closeDialog() {
        if (dialog == null || !(overlay instanceof FrameLayout)) {
            dialog = null;
            return;
        }
        FrameLayout host = (FrameLayout) overlay;
        host.removeView(dialog);
        dialog = null;
        for (int i = 0; i < host.getChildCount(); i++) {
            View ch = host.getChildAt(i);
            if (ch instanceof ViewGroup) ((ViewGroup) ch).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        }
        if (dialogFocusBefore != null && dialogFocusBefore.isAttachedToWindow()) dialogFocusBefore.requestFocus();
        dialogFocusBefore = null;
    }

    // ---- quick panel

    /** Home's second page, a whole screen below it. */
    private void openPanel() {
        FrameLayout h = host();
        String status = time.getText() + (weather.ready() ? " · " + weather.temp() + "° · закат в " + weather.sun("sunset") : "");
        LinearLayout s = page(h, "Настройки", status, false);

        LinearLayout r1 = row();
        LinearLayout.LayoutParams r1l = new LinearLayout.LayoutParams(-1, -2);
        r1l.topMargin = Ui.px(this, 44);
        s.addView(r1, r1l);

        // the light: one card with three choices, the current one white
        LinearLayout light = new LinearLayout(this);
        light.setOrientation(LinearLayout.VERTICAL);
        light.setPadding(Ui.px(this, 22), Ui.px(this, 20), Ui.px(this, 22), Ui.px(this, 20));
        light.setBackground(Ui.glass(this, 26));
        light.setClipChildren(false);
        float lvl = Dim.level(this);
        float[] levels = {0f, 0.4f, Dim.NIGHT};
        // the panel's slider can leave the level between the presets: the nearest one is lit, the title says how much
        int on = 0;
        for (int i = 1; i < levels.length; i++) if (Math.abs(levels[i] - lvl) < Math.abs(levels[on] - lvl)) on = i;
        boolean exact = Math.abs(levels[on] - lvl) < 0.01f;
        light.addView(Ui.text(this, exact ? "Свет" : "Свет · " + Dim.brightness(this) + "%", 17, 600, Color.WHITE));
        LinearLayout pills = segmented(new String[]{"100%", "60%", "Ночь"}, on, 246, i -> dim(levels[i]));
        View firstPill = pills.getChildAt(on);
        View push = new View(this);
        light.addView(push, new LinearLayout.LayoutParams(1, 0, 1));
        light.addView(pills, new LinearLayout.LayoutParams(-1, Ui.px(this, 58)));
        r1.addView(light, new LinearLayout.LayoutParams(Ui.px(this, 290), Ui.px(this, 140)));

        int timeout = saverMinutes();
        addSpaced(r1, Ui.tile(this, R.drawable.ic_image, "Заставка", timeout <= 0 ? "выключена" : "iCloud · " + timeout + " мин", 148, 140, v -> openSaver()), 20);
        addSpaced(r1, Ui.tile(this, R.drawable.ic_palm, "Сцена", SCENE_TIMES[scenePrefs().getInt("time", 0)].toLowerCase(RU), 148, 140, v -> openScene()), 20);
        int asks = tg.people("pending").size();
        panelAsks = asks;
        String tgSub = tg.token().isEmpty() ? "не настроен" : asks > 0 ? asks + " " + plural(asks, "запрос", "запроса", "запросов") : "сообщения";
        LinearLayout tgTile = Ui.tile(this, R.drawable.ic_send, "Telegram", tgSub, 148, 140, v -> openTelegram());
        if (asks > 0) badge(tgTile, asks);
        addSpaced(r1, tgTile, 20);
        int events = Cal.today(this).size();
        addSpaced(r1, Ui.tile(this, R.drawable.ic_calendar, "Календарь", Cal.sources(this).isEmpty() ? "не подключён" : events + " " + plural(events, "событие", "события", "событий"), 148, 140, v -> openCalendar()), 20);
        addSpaced(r1, Ui.tile(this, R.drawable.ic_pin, "Город", weather.placeName(), 148, 140, v -> openPlaces()), 20);

        section(s, "Система");
        LinearLayout r2 = row();
        boolean wifi = network(NetworkCapabilities.TRANSPORT_WIFI), vpn = network(NetworkCapabilities.TRANSPORT_VPN);
        r2.addView(Ui.round(this, R.drawable.ic_wifi, "Wi-Fi", wifi ? "подключён" : "нет сети", wifi, v -> settings(Settings.ACTION_WIFI_SETTINGS)));
        boolean bt = bluetooth();
        addSpaced(r2, Ui.round(this, R.drawable.ic_bluetooth, "Bluetooth", bt ? "включён" : "выключен", bt, v -> Actions.bluetooth(this)), 44);
        addSpaced(r2, Ui.round(this, R.drawable.ic_shield, "VPN", vpn ? "включён" : "AmneziaVPN", vpn, v -> apps.open("org.amnezia.vpn")), 44);
        addSpaced(r2, Ui.round(this, R.drawable.ic_volume, "Звук", null, false, v -> settings(Settings.ACTION_SOUND_SETTINGS)), 44);
        int keys = new Mappings(this).codes().size();
        addSpaced(r2, Ui.round(this, R.drawable.ic_remote, "Кнопки пульта", keys + " " + plural(keys, "кнопка", "кнопки", "кнопок"), false, v -> openRemote()), 44);
        addSpaced(r2, Ui.round(this, R.drawable.ic_monitor, "Настройки ТВ", null, false, v -> settings(Settings.ACTION_SETTINGS)), 44);
        s.addView(r2);

        TextView hint = Ui.text(this, "↑ или Back — на главный", 15, 400, Ui.TEXT_FAINT);
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hl.bottomMargin = Ui.px(this, 40);
        h.addView(hint, hl);
        show(h, "panel");
        if (firstPill != null) firstPill.requestFocus();
    }

    /** Whether the active network goes over {@code transport} (Wi-Fi, VPN). */
    private boolean network(int transport) {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && nc.hasTransport(transport);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean bluetooth() {
        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            return a != null && a.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /** A count in the tile's top-right corner, on the icon's line: someone waits to be let in. */
    private void badge(LinearLayout tile, int n) {
        View icon = tile.getChildAt(0);
        tile.removeViewAt(0);
        LinearLayout line = row();
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(icon, new LinearLayout.LayoutParams(0, icon.getLayoutParams().height, 1));
        ((ImageView) icon).setScaleType(ImageView.ScaleType.FIT_START);
        TextView b = Ui.text(this, String.valueOf(n), 15, 700, Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(Ui.px(this, 7), 0, Ui.px(this, 7), 0);
        b.setMinWidth(Ui.px(this, 24));
        b.setBackground(Ui.round(Ui.BADGE, 0, 12, this));
        line.addView(b, new LinearLayout.LayoutParams(-2, Ui.px(this, 24)));
        tile.addView(line, 0, new LinearLayout.LayoutParams(-1, -2));
    }

    private int panelAsks;

    private static String plural(int n, String one, String few, String many) {
        int m10 = n % 10, m100 = n % 100;
        if (m10 == 1 && m100 != 11) return one;
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return few;
        return many;
    }

    private void dim(float level) {
        Dim.set(this, level);
        closeOverlay();
        openPanel();
        if (level > 0 && !Dim.available(this)) {
            // the stick can't dim its backlight; the dark layer needs a permission granted once over ADB
            dialog(R.drawable.ic_sun_dim, "Затемнение не включилось",
                    "Телевизор сам не меняет подсветку, поэтому лончер кладёт тёмный слой поверх экрана. На это нужно разрешение, его дают один раз с компьютера:",
                    "adb shell appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow",
                    new String[]{"Понятно"}, new Runnable[]{null}, 0);
        }
    }

    private void startDream() {
        // the system silently falls back to Home when the chosen screensaver isn't installed: say so instead
        String chosen = Settings.Secure.getString(getContentResolver(), "screensaver_components");
        android.content.ComponentName cn = chosen == null || chosen.isEmpty() ? null : android.content.ComponentName.unflattenFromString(chosen.split(",")[0]);
        boolean installed = false;
        if (cn != null) {
            try {
                getPackageManager().getServiceInfo(cn, 0);
                installed = true;
            } catch (Exception ignored) {
            }
        }
        if (!installed) {
            dialog(R.drawable.ic_image, "Заставка не выбрана",
                    "Выбранной в системе заставки нет на телевизоре. Поставьте Photo Prints и выберите её в настройках ТВ: «Экран и звук» → «Заставка».",
                    null, new String[]{"Понятно"}, new Runnable[]{null}, 0);
            return;
        }
        closeOverlay();
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator"));
        } catch (Exception ignored) {
        }
    }

    private void settings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    // ---- the screensaver

    /** Minutes of an idle remote before the screensaver starts; 0 when it is off. */
    private int saverMinutes() {
        try {
            if (Settings.Secure.getInt(getContentResolver(), "screensaver_enabled", 1) == 0) return 0;
            int m = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 15 * 60_000) / 60_000;
            // a day or more is the system's way of saying never
            return m >= 24 * 60 ? 0 : m;
        } catch (Exception e) {
            return 15;
        }
    }

    private boolean saverIsOurs() {
        String chosen = Settings.Secure.getString(getContentResolver(), "screensaver_components");
        return chosen != null && chosen.startsWith(new android.content.ComponentName(this, HomeDream.class).flattenToString());
    }

    /** The two commands that let the launcher change the TV's screensaver settings, run once from a computer. */
    private void needSettingsAccess() {
        dialog(R.drawable.ic_image, "Нужен доступ к настройкам",
                "Заставку и её время меняет система. Разрешите это лончеру один раз с компьютера:",
                "adb shell pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS\nadb shell appops set " + getPackageName() + " WRITE_SETTINGS allow",
                new String[]{"Понятно"}, new Runnable[]{null}, 0);
    }

    private void setSaverMinutes(int minutes) {
        try {
            if (minutes > 0) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, minutes * 60_000);
                Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 1);
            } else {
                Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 0);
            }
            closeOverlay();
            openSaver();
        } catch (Exception e) {
            needSettingsAccess();
        }
    }

    /**
     * The screensaver: the iCloud album it shows (the link changes by sending a new one to the bot), how the
     * photos share the screen, how often they change, and after how long it starts.
     */
    private void openSaver() {
        FrameLayout h = host();
        LinearLayout s = page(h, "Заставка", "фото из iCloud, когда ТВ простаивает", true);
        LinearLayout top = row();
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
        tl.topMargin = Ui.px(this, 32);
        s.addView(top, tl);

        LinearLayout album = new LinearLayout(this);
        album.setOrientation(LinearLayout.VERTICAL);
        album.setPadding(Ui.px(this, 24), Ui.px(this, 20), Ui.px(this, 24), Ui.px(this, 20));
        album.setBackground(Ui.glass(this, 26));
        album.setClipChildren(false);
        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout ib = new FrameLayout(this);
        ib.setBackground(Ui.round(0x24FFFFFF, 0, 12, this));
        ImageView ii = new ImageView(this);
        ii.setImageResource(R.drawable.ic_image);
        ib.addView(ii, new FrameLayout.LayoutParams(Ui.px(this, 24), Ui.px(this, 24), Gravity.CENTER));
        head.addView(ib, new LinearLayout.LayoutParams(Ui.px(this, 44), Ui.px(this, 44)));
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        String name = Saver.albumName(this);
        ht.addView(Ui.text(this, name.isEmpty() ? "Общий альбом iCloud" : "Общий альбом «" + name + "»", 18, 600, Color.WHITE));
        int n = Saver.photos(this).size();
        long at = Saver.syncedAt(this);
        TextView hs = label(n + " " + plural(n, "фото", "фото", "фото") + (at == 0 ? " · ещё не загружался" : " · обновлено " + when(at / 1000)));
        LinearLayout.LayoutParams hsl = new LinearLayout.LayoutParams(-2, -2);
        hsl.topMargin = Ui.px(this, 3);
        ht.addView(hs, hsl);
        LinearLayout.LayoutParams htl = new LinearLayout.LayoutParams(-2, -2);
        htl.leftMargin = Ui.px(this, 14);
        head.addView(ht, htl);
        album.addView(head);
        LinearLayout act = row();
        TextView now = chip("Показать сейчас", false, v -> startActivity(new Intent(this, SaverActivity.class)));
        act.addView(now);
        if (!saverIsOurs()) {
            addSpaced(act, chip("Сделать заставкой ТВ", false, v -> {
                try {
                    Settings.Secure.putString(getContentResolver(), "screensaver_components", new android.content.ComponentName(this, HomeDream.class).flattenToString());
                    Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 1);
                    closeOverlay();
                    openSaver();
                } catch (Exception e) {
                    needSettingsAccess();
                }
            }), 12);
        }
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(-1, -2);
        al.topMargin = Ui.px(this, 18);
        album.addView(act, al);
        top.addView(album, new LinearLayout.LayoutParams(Ui.px(this, 620), Ui.px(this, 168)));

        LinearLayout how = new LinearLayout(this);
        how.setOrientation(LinearLayout.VERTICAL);
        how.setPadding(Ui.px(this, 24), Ui.px(this, 20), Ui.px(this, 24), Ui.px(this, 20));
        how.setBackground(Ui.glass(this, 26));
        how.addView(Ui.text(this, "Другой альбом", 15, 600, Color.WHITE));
        TextView ht2 = label("Фото на iPhone → общий альбом → «Пригласить» → «Общедоступный веб-сайт». Отправьте ссылку боту в Telegram — пультом ничего набирать не нужно.");
        ht2.setLineSpacing(0, 1.35f);
        LinearLayout.LayoutParams h2l = new LinearLayout.LayoutParams(-2, -2);
        h2l.topMargin = Ui.px(this, 8);
        how.addView(ht2, h2l);
        LinearLayout.LayoutParams howl = new LinearLayout.LayoutParams(0, Ui.px(this, 168), 1);
        howl.leftMargin = Ui.px(this, 24);
        top.addView(how, howl);

        section(s, "Раскладка");
        LinearLayout layouts = row();
        String[] ln = {"Одно фото", "Два рядом", "Три", "Меняется сама"};
        int cur = Saver.layout(this);
        View first = null;
        for (int i = 0; i < 4; i++) {
            int li = i;
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setClipChildren(false);
            FrameLayout box = new FrameLayout(this);
            box.setBackground(Ui.control(this, 18));
            box.addView(new LayoutThumb(this, i), new FrameLayout.LayoutParams(-1, -1));
            Ui.focusable(box);
            box.setOnClickListener(v -> {
                Saver.setLayout(this, li);
                closeOverlay();
                openSaver();
            });
            col.addView(box, new LinearLayout.LayoutParams(-1, Ui.px(this, 140)));
            LinearLayout cap = row();
            cap.setGravity(Gravity.CENTER_VERTICAL);
            cap.addView(Ui.text(this, ln[i], 15, 600, Color.WHITE));
            if (i == cur) {
                ImageView ck = new ImageView(this);
                ck.setImageResource(R.drawable.ic_check);
                LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(Ui.px(this, 18), Ui.px(this, 18));
                cl.leftMargin = Ui.px(this, 8);
                cap.addView(ck, cl);
                first = box;
            }
            LinearLayout.LayoutParams capl = new LinearLayout.LayoutParams(-2, -2);
            capl.topMargin = Ui.px(this, 10);
            col.addView(cap, capl);
            LinearLayout.LayoutParams coll = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) coll.leftMargin = Ui.px(this, 20);
            layouts.addView(col, coll);
        }
        s.addView(layouts);

        LinearLayout bottom = row();
        LinearLayout every = new LinearLayout(this);
        every.setOrientation(LinearLayout.VERTICAL);
        every.setClipChildren(false);
        every.addView(Ui.caps(this, "Смена фото"));
        int e = Saver.every(this), eOn = e <= 10 ? 0 : e <= 30 ? 1 : 2;
        LinearLayout es = segmented(new String[]{"10 секунд", "30 секунд", "1 минута"}, eOn, 540, i -> {
            Saver.setEvery(this, Saver.EVERY[i]);
            closeOverlay();
            openSaver();
        });
        LinearLayout.LayoutParams esl = new LinearLayout.LayoutParams(Ui.px(this, 540), Ui.px(this, 58));
        esl.topMargin = Ui.px(this, 14);
        every.addView(es, esl);
        bottom.addView(every);
        LinearLayout after = new LinearLayout(this);
        after.setOrientation(LinearLayout.VERTICAL);
        after.setClipChildren(false);
        after.addView(Ui.caps(this, "Включать, если пульт не трогали"));
        int m = saverMinutes(), mOn = m <= 0 ? 2 : m <= 5 ? 0 : 1;
        int[] mins = {5, 15, 0};
        LinearLayout as = segmented(new String[]{"5 минут", "15 минут", "Никогда"}, mOn, 540, i -> setSaverMinutes(mins[i]));
        LinearLayout.LayoutParams asl = new LinearLayout.LayoutParams(Ui.px(this, 540), Ui.px(this, 58));
        asl.topMargin = Ui.px(this, 14);
        after.addView(as, asl);
        LinearLayout.LayoutParams afl = new LinearLayout.LayoutParams(-2, -2);
        afl.leftMargin = Ui.px(this, 56);
        bottom.addView(after, afl);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
        bl.topMargin = Ui.px(this, 32);
        s.addView(bottom, bl);

        show(h, "saver");
        if (first != null) first.requestFocus();
        else now.requestFocus();
        // fetch the album now, not only when the screensaver first runs; redraw with the count when done
        Saver.syncAsync(this, () -> runOnUiThread(() -> {
            if (overlay != null && overlay.getTag() == "saver" && dialog == null) {
                View f = getCurrentFocus();
                closeOverlay();
                openSaver();
                if (f != null && f.getTag() != null) {
                    View again = overlay.findViewWithTag(f.getTag());
                    if (again != null) again.requestFocus();
                }
            }
        }));
    }

    /** A thumbnail of a screensaver layout: one to three photo-coloured blocks in a 16:9 frame. */
    private static final class LayoutThumb extends View {
        private final int kind;
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF r = new android.graphics.RectF();
        private static final int[] TINT = {0xFFE9A57C, 0xFF4E7896, 0xFF7FA36B};

        LayoutThumb(Context c, int kind) {
            super(c);
            this.kind = kind;
            setDuplicateParentStateEnabled(true);
        }

        @Override
        protected void onDraw(android.graphics.Canvas c) {
            float w = getWidth(), h = getHeight(), pad = w * 0.04f, g = w * 0.02f;
            float x0 = pad, y0 = pad, x1 = w - pad, y1 = h - pad, cw = x1 - x0, ch = y1 - y0;
            float[][] cells;
            switch (kind) {
                case Saver.ONE: cells = new float[][]{{0, 0, 1, 1}}; break;
                case Saver.TWO: cells = new float[][]{{0, 0, 0.495f, 1}, {0.505f, 0, 1, 1}}; break;
                case Saver.THREE: cells = new float[][]{{0, 0, 0.64f, 1}, {0.66f, 0, 1, 0.49f}, {0.66f, 0.51f, 1, 1}}; break;
                default: cells = new float[][]{{0, 0, 0.495f, 1}, {0.505f, 0, 1, 0.49f}, {0.505f, 0.51f, 1, 1}};
            }
            for (int i = 0; i < cells.length; i++) {
                float[] q = cells[i];
                r.set(x0 + q[0] * cw, y0 + q[1] * ch, x0 + q[2] * cw, y0 + q[3] * ch);
                p.setColor(TINT[(i + kind) % 3]);
                c.drawRoundRect(r, g * 3, g * 3, p);
            }
        }
    }

    // ---- the scene

    /** By the stored value of "time": morning came last, so the older values keep their meaning. */
    private static final String[] SCENE_TIMES = {"По солнцу", "День", "Закат", "Ночь", "Утро"};
    /** The order on the page: the stored value at each position. */
    private static final int[] SCENE_ORDER = {0, 4, 1, 2, 3};

    private android.content.SharedPreferences scenePrefs() {
        return getSharedPreferences("scene", MODE_PRIVATE);
    }

    /** Whether the clips loop. The bay drawn by code is gone: its old setting turns into the clips too. */
    private boolean sceneAnimated() {
        android.content.SharedPreferences p = scenePrefs();
        // "style" was 0 drawn, 1 pictures, 2 clips
        return p.getBoolean("animated", p.getInt("style", 2) != 1);
    }

    /** Puts the scene settings into the live bay: time of day, and the clips or the still pictures. */
    private void applyScene() {
        android.content.SharedPreferences p = scenePrefs();
        int t = p.getInt("time", 0);
        int[] at = {-1, 13 * 60 + 40, 18 * 60 + 5, 22 * 60 + 30, 7 * 60 + 10};
        scene.setForcedMinute(at[t]);
        showAnimated(sceneAnimated());
    }

    /** The clips have a surface of their own; the still pictures are drawn on the scene's. */
    private void showAnimated(boolean animated) {
        scene.setAnimated(animated);
        scene.setVisibility(animated ? View.GONE : View.VISIBLE);
        video.setVisibility(animated ? View.VISIBLE : View.GONE);
        video.update();
        updateGlass();
    }

    /**
     * The glass of the panels follows what is behind them: the clip with the most weight while the clips play
     * (they switch from one to the next), the crossfading blend while the pictures stand still.
     */
    private void updateGlass() {
        float[] w = scene.weights();
        if (scene.animated()) {
            int clip = scene.clipNow();
            w = new float[w.length];
            w[clip] = 1;
        }
        Backdrop.update(this, w);
    }

    /**
     * The bay behind Home: the page is only lightly dimmed so the scene itself is the preview, and every
     * change shows at once.
     */
    private void openScene() {
        FrameLayout h = host();
        LinearLayout s = page(h, "Сцена", "бухта Ката за главным экраном", true);
        // no glass here: the bay itself is what the page shows
        h.setBackgroundColor(0x8C0C1016);
        android.content.SharedPreferences p = scenePrefs();
        boolean animated = sceneAnimated();

        section(s, "Анимация");
        LinearLayout anim = segmented(new String[]{"Включена", "Выключена"}, animated ? 0 : 1, 420, i -> {
            p.edit().putBoolean("animated", i == 0).apply();
            applyScene();
            redrawScene();
        });
        s.addView(anim);
        section(s, "Время суток");
        String[] times = new String[SCENE_ORDER.length];
        int timeOn = 0;
        for (int i = 0; i < times.length; i++) {
            times[i] = SCENE_TIMES[SCENE_ORDER[i]];
            if (SCENE_ORDER[i] == p.getInt("time", 0)) timeOn = i;
        }
        LinearLayout time = segmented(times, timeOn, 640, i -> {
            p.edit().putInt("time", SCENE_ORDER[i]).apply();
            applyScene();
            redrawScene();
        });
        s.addView(time);
        TextView note = label(animated
                ? "Короткое видео по каждой картинке крутится по кругу и сменяется по времени суток. «По солнцу» — по восходу и закату в выбранном городе."
                : "Картинки стоят без движения и меняются по времени суток, плавно за полчаса. Так стик меньше занят. «По солнцу» — по восходу и закату в выбранном городе.");
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(Ui.px(this, 640), -2);
        nl.topMargin = Ui.px(this, 32);
        s.addView(note, nl);
        show(h, "scene");
        View f = anim.findViewWithTag("on");
        if (f != null) f.requestFocus();
    }

    /** Redraws the scene page in place, keeping the cursor where it was. */
    private void redrawScene() {
        View f = getCurrentFocus();
        String text = f instanceof TextView ? ((TextView) f).getText().toString() : null;
        openScene();
        if (text == null) return;
        java.util.ArrayList<View> found = new java.util.ArrayList<>();
        overlay.findViewsWithText(found, text, View.FIND_VIEWS_WITH_TEXT);
        if (!found.isEmpty()) found.get(0).requestFocus();
    }

    // ---- the calendar

    /**
     * Calendars feeding «СЕГОДНЯ» on Home: each added by sending its secret iCal address to the bot, shown or
     * hidden here, and whether Home shows the important events, all of today's, or none.
     */
    private void openCalendar() {
        FrameLayout h = host();
        List<Cal.Source> list = Cal.sources(this);
        int count = Cal.today(this).size();
        LinearLayout s = page(h, "Календарь", list.isEmpty() ? "" : "сегодня " + count + " " + plural(count, "событие", "события", "событий"), true);
        section(s, "Календари");
        View first = null;
        if (list.isEmpty()) {
            TextView e = label("Пока ни одного. Как подключить — справа.");
            s.addView(e);
        }
        for (Cal.Source src : list) {
            LinearLayout r = row();
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(Ui.px(this, 22), 0, Ui.px(this, 16), 0);
            r.setBackground(Ui.control(this, 22));
            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(src.color);
            dot.setBackground(d);
            r.addView(dot, new LinearLayout.LayoutParams(Ui.px(this, 12), Ui.px(this, 12)));
            TextView n = Ui.text(this, src.name, 17, 600, Color.WHITE);
            n.setSingleLine(true);
            n.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, -2, 1);
            nl.leftMargin = Ui.px(this, 14);
            r.addView(n, nl);
            TextView st = Ui.text(this, src.shown ? "показывать" : "скрыт", 15, 600, Color.WHITE);
            st.setPadding(Ui.px(this, 14), Ui.px(this, 8), Ui.px(this, 14), Ui.px(this, 8));
            st.setBackground(src.shown ? Ui.round(Ui.ON, 0, 18, this) : Ui.round(0x1AFFFFFF, 0, 18, this));
            if (!src.shown) st.setAlpha(0.7f);
            r.addView(st);
            Ui.focusable(r);
            r.setTag("cal_" + src.url);
            r.setOnClickListener(v -> {
                Cal.setShown(this, src.url, !src.shown);
                Cal.refreshIfDue(this, this::renderToday);
                closeOverlay();
                openCalendar();
                View again = overlay.findViewWithTag("cal_" + src.url);
                if (again != null) again.requestFocus();
            });
            r.setOnLongClickListener(v -> {
                dialog(R.drawable.ic_calendar, "Убрать «" + src.name + "»?", "Его события перестанут показываться на главном.", null,
                        new String[]{"Убрать", "Отмена"}, new Runnable[]{() -> {
                            Cal.remove(this, src.url);
                            closeOverlay();
                            openCalendar();
                        }, null}, 1);
                return true;
            });
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(Ui.px(this, 620), Ui.px(this, 68));
            if (s.getChildCount() > 2) rl.topMargin = Ui.px(this, 10);
            s.addView(r, rl);
            if (first == null) first = r;
        }
        if (!list.isEmpty()) {
            TextView hint = Ui.text(this, "OK — показать или скрыть, удержание OK — убрать", 15, 400, Ui.TEXT_FAINT);
            LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
            hl.topMargin = Ui.px(this, 10);
            s.addView(hint, hl);
        }
        section(s, "На главном");
        LinearLayout mode = segmented(new String[]{"Только важные", "Все на сегодня", "Не показывать"}, Cal.mode(this), 620, i -> {
            Cal.setMode(this, i);
            renderToday();
            closeOverlay();
            openCalendar();
        });
        s.addView(mode);
        TextView imp = label("Важные — события с напоминанием. До трёх под погодой, прошедшие исчезают.");
        LinearLayout.LayoutParams il = new LinearLayout.LayoutParams(Ui.px(this, 620), -2);
        il.topMargin = Ui.px(this, 12);
        s.addView(imp, il);

        String bot = tg.botName();
        qrBlock(h, bot.isEmpty() ? null : "https://t.me/" + bot, "Отправьте ссылку боту",
                "Google Календарь. В приложении ссылки нет: откройте calendar.google.com в Chrome или Safari, включите версию для ПК → Настройки → ваш календарь → «Интеграция календаря» → «Секретный адрес в формате iCal».\n\n"
                        + "Календарь iPhone. «Календари» → ⓘ у календаря iCloud → «Общедоступный календарь» → «Поделиться ссылкой».\n\n"
                        + "Скопируйте ссылку и пришлите боту" + (bot.isEmpty() ? "." : " @" + bot + "."));
        show(h, "calendar");
        if (first != null) first.requestFocus();
        else if (mode.findViewWithTag("on") != null) mode.findViewWithTag("on").requestFocus();
    }

    /**
     * The QR block shared by the pages that hand something over to a phone: a 180 px code on white at the top
     * right, a bold line and a short grey explanation under it. With no link, only the text.
     */
    private void qrBlock(FrameLayout host, String link, String title, String text) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.END);
        if (link != null && !link.isEmpty()) {
            FrameLayout q = new FrameLayout(this);
            q.setBackground(Ui.round(Color.WHITE, 0, 20, this));
            ImageView iv = new ImageView(this);
            Bitmap bm = Qr.bitmap(link, 8);
            iv.setImageBitmap(bm);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            q.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            q.setPadding(Ui.px(this, 6), Ui.px(this, 6), Ui.px(this, 6), Ui.px(this, 6));
            b.addView(q, new LinearLayout.LayoutParams(Ui.px(this, 180), Ui.px(this, 180)));
        }
        TextView t = Ui.text(this, title, 17, 600, Color.WHITE);
        t.setGravity(Gravity.END);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = Ui.px(this, 18);
        b.addView(t, tl);
        TextView d = Ui.text(this, text, 15, 400, Ui.TEXT_DIM);
        d.setGravity(Gravity.END);
        d.setLineSpacing(0, 1.4f);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-2, -2);
        dl.topMargin = Ui.px(this, 6);
        b.addView(d, dl);
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(Ui.px(this, 360), -2, Gravity.END | Gravity.TOP);
        fl.rightMargin = Ui.px(this, 72);
        fl.topMargin = Ui.px(this, 56 + 32 + 44);
        host.addView(b, fl);
    }

    // ---- weather

    /** The forecast, kept simple: now, the next hours, the next days. Opened by OK on the weather line. */
    private void openWeather() {
        if (!weather.ready()) return;
        FrameLayout h = host();
        LinearLayout s = page(h, weather.placeTitle(), "обновлено в " + new SimpleDateFormat("HH:mm", RU).format(new Date(weather.updatedAt())), false);
        LinearLayout now = row();
        now.setGravity(Gravity.CENTER_VERTICAL);
        TextView temp = Ui.text(this, weather.temp() + "°", 140, 200, Color.WHITE);
        temp.setLetterSpacing(-0.05f);
        now.addView(temp);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(Ui.text(this, cap(Weather.describe(weather.code())), 28, 600, Color.WHITE));
        Integer sea = weather.sea();
        String line = "ощущается " + weather.feels() + "°" + (sea != null ? " · море " + sea + "°" : "") + " · закат " + weather.sun("sunset");
        TextView ln = Ui.text(this, line, 17, 400, 0xB3FFFFFF);
        LinearLayout.LayoutParams lnl = new LinearLayout.LayoutParams(-2, -2);
        lnl.topMargin = Ui.px(this, 8);
        col.addView(ln, lnl);
        String a = weather.alert();
        if (a != null) {
            LinearLayout al = new LinearLayout(this);
            al.setGravity(Gravity.CENTER_VERTICAL);
            ImageView ic = new ImageView(this);
            ic.setImageResource(R.drawable.ic_storm);
            ic.setColorFilter(Ui.WARN);
            al.addView(ic, new LinearLayout.LayoutParams(Ui.px(this, 20), Ui.px(this, 20)));
            TextView at = Ui.text(this, a, 17, 500, Ui.WARN);
            LinearLayout.LayoutParams atl = new LinearLayout.LayoutParams(-2, -2);
            atl.leftMargin = Ui.px(this, 8);
            al.addView(at, atl);
            LinearLayout.LayoutParams all = new LinearLayout.LayoutParams(-2, -2);
            all.topMargin = Ui.px(this, 8);
            col.addView(al, all);
        }
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-2, -2);
        cl.leftMargin = Ui.px(this, 28);
        now.addView(col, cl);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-1, -2);
        nl.topMargin = Ui.px(this, 24);
        s.addView(now, nl);

        section(s, "Сегодня");
        LinearLayout hours = row();
        for (String[] hr : weather.hours(6, 2)) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.addView(Ui.text(this, hr[0], 15, 400, Ui.TEXT_DIM));
            TextView t = Ui.text(this, hr[1], 26, 500, Color.WHITE);
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
            tl.topMargin = Ui.px(this, 8);
            c.addView(t, tl);
            TextView r = Ui.text(this, hr[2].isEmpty() ? " " : hr[2], 15, 400, 0xFF8EC9FF);
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
            rl.topMargin = Ui.px(this, 8);
            c.addView(r, rl);
            hours.addView(c, new LinearLayout.LayoutParams(0, -2, 1));
        }
        s.addView(hours, new LinearLayout.LayoutParams(-1, -2));

        section(s, "Дальше");
        LinearLayout days = row();
        View first = null;
        for (String[] d : weather.days(4)) {
            LinearLayout c = row();
            c.setGravity(Gravity.CENTER_VERTICAL);
            c.setPadding(Ui.px(this, 22), 0, Ui.px(this, 22), 0);
            c.setBackground(Ui.control(this, 20));
            c.addView(Ui.text(this, d[0], 17, 600, Color.WHITE));
            TextView w = Ui.text(this, d[1], 15, 400, 0xA6FFFFFF);
            w.setSingleLine(true);
            w.setEllipsize(TextUtils.TruncateAt.END);
            w.setGravity(Gravity.CENTER);
            c.addView(w, new LinearLayout.LayoutParams(0, -2, 1));
            c.addView(Ui.text(this, d[2], 17, 500, Color.WHITE));
            Ui.focusable(c);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(0, Ui.px(this, 64), 1);
            if (days.getChildCount() > 0) dl.leftMargin = Ui.px(this, 16);
            days.addView(c, dl);
            if (first == null) first = c;
        }
        s.addView(days, new LinearLayout.LayoutParams(-1, -2));
        show(h, "weather");
        if (first != null) first.requestFocus();
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(RU) + s.substring(1);
    }

    // ---- places

    private void openPlaces() {
        FrameLayout h = host();
        LinearLayout s = page(h, "Город для погоды", "сейчас: " + weather.placeName(), true);

        LinearLayout sr = row();
        sr.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srl = new LinearLayout.LayoutParams(-1, -2);
        srl.topMargin = Ui.px(this, 32);
        s.addView(sr, srl);
        LinearLayout field = row();
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(Ui.px(this, 22), 0, Ui.px(this, 12), 0);
        field.setBackground(Ui.glass(this, 28));
        ImageView si = new ImageView(this);
        si.setImageResource(R.drawable.ic_search);
        field.addView(si, new LinearLayout.LayoutParams(Ui.px(this, 20), Ui.px(this, 20)));
        EditText q = new EditText(this);
        q.setTag("query");
        q.setSingleLine(true);
        q.setHint("Найти город");
        q.setHintTextColor(0x99FFFFFF);
        q.setTextColor(Color.WHITE);
        q.setTypeface(Ui.font(this, 500));
        q.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 18));
        q.setPadding(Ui.px(this, 12), 0, 0, 0);
        q.setBackground(null);
        q.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.addView(q, new LinearLayout.LayoutParams(0, -1, 1));
        // the field shows the cursor ring on its pill, not on the bare text
        q.setOnFocusChangeListener((v, has) -> {
            GradientDrawable g = Ui.round(has ? Ui.GLASS_FOCUS : Ui.GLASS, has ? 0 : Ui.GLASS_LINE, 28, this);
            if (has) g.setStroke(Ui.px(this, 3), Color.WHITE);
            field.setBackground(g);
            Ui.grow(field, has);
        });
        sr.addView(field, new LinearLayout.LayoutParams(Ui.px(this, 560), Ui.px(this, 56)));
        TextView go = chip("Найти", false, null);
        go.setLayoutParams(new LinearLayout.LayoutParams(-2, Ui.px(this, 56)));
        addSpaced(sr, go, 12);
        TextView status = label("");
        LinearLayout.LayoutParams stl = new LinearLayout.LayoutParams(-2, -2);
        stl.leftMargin = Ui.px(this, 18);
        sr.addView(status, stl);

        GridLayout results = new GridLayout(this);
        results.setColumnCount(3);
        s.addView(results);

        Runnable search = () -> {
            String text = q.getText().toString().trim();
            if (text.isEmpty()) return;
            status.setText("Ищу…");
            weather.search(text, found -> {
                results.removeAllViews();
                status.setText(found.isEmpty() ? "Ничего не нашлось" : "");
                for (String[] p : found) addPlace(results, p[0], p[3], p[1], p[2]);
                unclip(results);
                if (results.getChildCount() > 0) results.getChildAt(0).requestFocus();
            });
        };
        go.setOnClickListener(v -> search.run());
        q.setOnEditorActionListener((v, action, e) -> {
            search.run();
            return true;
        });

        // the place in use and the ones before it, then cities to pick without typing
        List<String[]> recent = new ArrayList<>();
        recent.add(new String[]{weather.placeName(), weather.placeRegion(), weather.placeLat(), weather.placeLon()});
        for (String[] p : weather.recent()) if (!p[0].equals(weather.placeName()) && recent.size() < 3) recent.add(p);
        View current = null;
        boolean inList = false;
        for (String[] p : Weather.CITIES) inList |= p[0].equals(weather.placeName());
        if (recent.size() > 1 || !inList) {
            section(s, "Недавние");
            GridLayout rg = new GridLayout(this);
            rg.setColumnCount(3);
            for (String[] p : recent) {
                View c = addPlace(rg, p[0], p[1], p[2], p[3]);
                if (current == null) current = c;
            }
            s.addView(rg);
        }
        section(s, "Города");
        GridLayout cities = new GridLayout(this);
        cities.setColumnCount(3);
        for (String[] p : Weather.CITIES) {
            View c = addPlace(cities, p[0], p[1], p[2], p[3]);
            if (current == null && p[0].equals(weather.placeName())) current = c;
        }
        s.addView(cities);
        show(h, "places");
        if (current != null) current.requestFocus();
        else q.requestFocus();
    }

    /** One place to pick: city on the first line, region and country below; the current one has a check. */
    private View addPlace(GridLayout grid, String name, String where, String lat, String lon) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(Ui.px(this, 24), 0, Ui.px(this, 22), 0);
        c.setBackground(Ui.control(this, 22));
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView n = Ui.text(this, name, 18, 600, Color.WHITE);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        txt.addView(n);
        TextView w = Ui.text(this, where, 15, 400, Ui.TEXT_DIM);
        w.setSingleLine(true);
        w.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(-2, -2);
        wl.topMargin = Ui.px(this, 3);
        txt.addView(w, wl);
        c.addView(txt, new LinearLayout.LayoutParams(0, -2, 1));
        if (name.equals(weather.placeName())) {
            ImageView ck = new ImageView(this);
            ck.setImageResource(R.drawable.ic_check);
            c.addView(ck, new LinearLayout.LayoutParams(Ui.px(this, 22), Ui.px(this, 22)));
        }
        Ui.focusable(c);
        c.setOnClickListener(v -> {
            weather.setPlace(name, where, lat, lon);
            goingBack = true;
            closeOverlay();
            openPanel();
        });
        int i = grid.getChildCount();
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = Math.round((getResources().getDisplayMetrics().widthPixels - Ui.px(this, 144) - Ui.px(this, 18) * 2) / 3f);
        lp.height = Ui.px(this, 72);
        lp.rightMargin = i % 3 == 2 ? 0 : Ui.px(this, 18);
        lp.topMargin = i < 3 ? 0 : Ui.px(this, 14);
        grid.addView(c, lp);
        return c;
    }

    // ---- remote buttons

    /** Which app the remote's app buttons open, on a press and on a hold. Replaces the Remote Keys app. */
    private void openRemote() {
        KeyWatchService.learner = null;
        Mappings mp = new Mappings(this);
        FrameLayout h = host();
        boolean logs = checkSelfPermission("android.permission.READ_LOGS") == android.content.pm.PackageManager.PERMISSION_GRANTED;
        LinearLayout s = page(h, "Кнопки пульта", logs ? "нажатие открывает одно, удержание — другое" : "нет доступа к журналу: выдайте READ_LOGS через ADB", true);
        View first = null;
        boolean top = true;
        for (int code : mp.codes()) {
            LinearLayout r = row();
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(Ui.px(this, 24), 0, Ui.px(this, 16), 0);
            r.setBackground(Ui.glass(this, 26));
            LinearLayout name = new LinearLayout(this);
            name.setOrientation(LinearLayout.VERTICAL);
            name.addView(Ui.text(this, Mappings.name(code), 18, 600, Color.WHITE));
            TextView sub = label(code == Mappings.NETFLIX || code == Mappings.PRIME ? "кнопка на пульте" : "код " + code);
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2);
            sl.topMargin = Ui.px(this, 4);
            name.addView(sub, sl);
            r.addView(name, new LinearLayout.LayoutParams(Ui.px(this, 300), -2));
            View tap = appChoice("Нажатие", mp.tap(code), v -> pickApp(code, false));
            tap.setTag("tap_" + code);
            addSpaced(r, tap, 16);
            View hold = appChoice("Удержание", mp.hold(code), v -> pickApp(code, true));
            hold.setTag("hold_" + code);
            addSpaced(r, hold, 16);
            r.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            if (code != Mappings.NETFLIX && code != Mappings.PRIME) {
                TextView rm = chip("Убрать", false, v -> dialog(R.drawable.ic_remote, "Убрать кнопку " + Mappings.name(code) + "?",
                        "Нажатие и удержание перестанут открывать приложения.", null,
                        new String[]{"Убрать", "Отмена"}, new Runnable[]{() -> {
                            mp.remove(code);
                            closeOverlay();
                            openRemote();
                        }, null}, 1));
                r.addView(rm);
            }
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, Ui.px(this, 96));
            rl.topMargin = Ui.px(this, top ? 32 : 14);
            s.addView(r, rl);
            top = false;
            if (first == null) first = tap;
        }

        // a new button: a dashed row; the press is caught by a dialog over the page
        LinearLayout add = row();
        add.setGravity(Gravity.CENTER_VERTICAL);
        add.setPadding(Ui.px(this, 24), 0, Ui.px(this, 24), 0);
        StateListDrawable ab = new StateListDrawable();
        GradientDrawable af = Ui.round(Ui.GLASS_FOCUS, 0, 26, this);
        af.setStroke(Ui.px(this, 3), Color.WHITE);
        ab.addState(new int[]{android.R.attr.state_focused}, af);
        GradientDrawable ad = Ui.round(0x00FFFFFF, 0, 26, this);
        ad.setStroke(Math.max(1, Ui.px(this, 1.5f)), 0x4DFFFFFF, Ui.px(this, 8), Ui.px(this, 6));
        ab.addState(new int[]{}, ad);
        add.setBackground(ab);
        ImageView plus = new ImageView(this);
        plus.setImageResource(R.drawable.ic_plus);
        add.addView(plus, new LinearLayout.LayoutParams(Ui.px(this, 26), Ui.px(this, 26)));
        LinearLayout at = new LinearLayout(this);
        at.setOrientation(LinearLayout.VERTICAL);
        at.addView(Ui.text(this, "Добавить кнопку", 17, 600, Color.WHITE));
        TextView as = label("потом нажмите её на пульте, Back — отмена");
        LinearLayout.LayoutParams asl = new LinearLayout.LayoutParams(-2, -2);
        asl.topMargin = Ui.px(this, 3);
        at.addView(as, asl);
        LinearLayout.LayoutParams atl = new LinearLayout.LayoutParams(-2, -2);
        atl.leftMargin = Ui.px(this, 14);
        add.addView(at, atl);
        add.setTag("learn");
        Ui.focusable(add);
        add.setOnClickListener(v -> {
            dialog(R.drawable.ic_remote, "Нажмите кнопку на пульте",
                    "Любую, кроме стрелок, OK, Back и Home. Потом выберете, что она открывает.", null,
                    new String[]{"Отмена"}, new Runnable[]{() -> KeyWatchService.learner = null}, 0);
            KeyWatchService.learner = c -> {
                // the keys that drive this screen can't be taken over
                if (c == KeyEvent.KEYCODE_BACK || c == KeyEvent.KEYCODE_DPAD_CENTER || c == KeyEvent.KEYCODE_ENTER
                        || (c >= KeyEvent.KEYCODE_DPAD_UP && c <= KeyEvent.KEYCODE_DPAD_RIGHT)) return;
                KeyWatchService.learner = null;
                mp.add(c);
                closeOverlay();
                openRemote();
            };
        });
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(-1, Ui.px(this, 76));
        al.topMargin = Ui.px(this, 14);
        s.addView(add, al);
        int items = 0;
        for (String id : Actions.panelOrder(this)) if (Actions.panelShows(this, id)) items++;
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(Ui.px(this, 300), Ui.px(this, 140));
        pl.topMargin = Ui.px(this, 14);
        LinearLayout panelTile = Ui.tile(this, R.drawable.ic_sliders, "Быстрая панель", "яркость и " + items + " " + plural(items, "пункт", "пункта", "пунктов"), 300, 140,
                v -> openPanelSettings(null, null));
        panelTile.setTag("panel");
        s.addView(panelTile, pl);
        show(h, "remote");
        if (first != null) first.requestFocus();
        else add.requestFocus();
    }

    /** A control showing which app a button opens: its icon, "Нажатие" and the app's name. */
    private View appChoice(String what, String pkg, View.OnClickListener click) {
        LinearLayout c = new LinearLayout(this);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(Ui.px(this, 14), 0, Ui.px(this, 16), 0);
        c.setBackground(Ui.control(this, 20));
        Apps.App app = null;
        if (pkg != null) for (Apps.App a : apps.installed()) if (a.pkg.equals(pkg)) app = a;
        boolean action = Actions.is(pkg);
        View ic;
        if (action) {
            // an action, not an app: the blue of "switched on" tells the two apart at a glance
            FrameLayout box = new FrameLayout(this);
            box.setBackground(Ui.round(Ui.ON, 0, 10, this));
            ImageView iv = new ImageView(this);
            iv.setImageResource(Actions.icon(pkg));
            box.addView(iv, new FrameLayout.LayoutParams(Ui.px(this, 22), Ui.px(this, 22), Gravity.CENTER));
            ic = box;
        } else if (app != null) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(apps.icon(app));
            clipRound(iv, 10);
            ic = iv;
        } else {
            TextView none = Ui.text(this, "–", 16, 600, Ui.TEXT_FAINT);
            none.setGravity(Gravity.CENTER);
            none.setBackground(Ui.round(0x1AFFFFFF, 0, 10, this));
            ic = none;
        }
        c.addView(ic, new LinearLayout.LayoutParams(Ui.px(this, 40), Ui.px(this, 40)));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        TextView w = Ui.text(this, what, 15, 400, Ui.TEXT_DIM);
        t.addView(w);
        TextView n = Ui.text(this, action ? Actions.name(pkg) : app != null ? app.label : pkg == null ? "ничего" : "не установлено", 16, 600, Color.WHITE);
        n.setSingleLine(true);
        n.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-2, -2);
        nl.topMargin = Ui.px(this, 2);
        t.addView(n, nl);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
        tl.leftMargin = Ui.px(this, 12);
        c.addView(t, tl);
        Ui.focusable(c);
        c.setOnClickListener(click);
        c.setLayoutParams(new LinearLayout.LayoutParams(Ui.px(this, 280), Ui.px(this, 68)));
        return c;
    }

    /** What a button does: the actions on top (there are few), then every app, "ничего" first. */
    private void pickApp(int code, boolean hold) {
        Mappings mp = new Mappings(this);
        String current = hold ? mp.hold(code) : mp.tap(code);
        FrameLayout h = host();
        LinearLayout s = page(h, Mappings.name(code) + (hold ? " · удержание" : " · нажатие"), "что делать по кнопке", true);
        View[] focus = new View[1];
        section(s, "Действия");
        s.addView(choiceGrid(java.util.Arrays.asList(Actions.ALL), code, hold, current, focus));
        section(s, "Приложения");
        List<Apps.App> all = apps.installed();
        java.util.Collections.sort(all, (a, b) -> a.label.compareToIgnoreCase(b.label));
        List<String> pkgs = new java.util.ArrayList<>();
        pkgs.add(null);
        for (Apps.App a : all) pkgs.add(a.pkg);
        s.addView(choiceGrid(pkgs, code, hold, current, focus));
        show(h, "remote_pick");
        if (focus[0] != null) focus[0].requestFocus();
    }

    /** Four to a row; {@code focus[0]} ends on the current choice, or on the first card when none of these is it. */
    private GridLayout choiceGrid(List<String> values, int code, boolean hold, String current, View[] focus) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setClipChildren(false);
        int w = Math.round((getResources().getDisplayMetrics().widthPixels - Ui.px(this, 144) - Ui.px(this, 16) * 3) / 4f);
        for (int i = 0; i < values.size(); i++) {
            String pkg = values.get(i);
            String verb = Actions.is(pkg) ? Actions.verb(pkg) : pkg == null ? "убрать" : "открыть";
            View c = appChoice(verb, pkg, v -> {
                new Mappings(this).set(code, hold, pkg);
                goingBack = true;
                closeOverlay();
                openRemote();
                View again = overlay.findViewWithTag((hold ? "hold_" : "tap_") + code);
                if (again != null) again.requestFocus();
            });
            boolean chosen = pkg == null ? current == null : pkg.equals(current);
            if (chosen) {
                ImageView ck = new ImageView(this);
                ck.setImageResource(R.drawable.ic_check);
                ((LinearLayout) c).addView(ck, new LinearLayout.LayoutParams(Ui.px(this, 20), Ui.px(this, 20)));
                ((LinearLayout.LayoutParams) ((LinearLayout) c).getChildAt(1).getLayoutParams()).width = 0;
                ((LinearLayout.LayoutParams) ((LinearLayout) c).getChildAt(1).getLayoutParams()).weight = 1;
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = w;
            lp.height = Ui.px(this, 72);
            lp.rightMargin = i % 4 == 3 ? 0 : Ui.px(this, 16);
            if (i >= 4) lp.topMargin = Ui.px(this, 16);
            c.setBackground(Ui.control(this, 22));
            grid.addView(c, lp);
            if (focus[0] == null || chosen) focus[0] = c;
        }
        return grid;
    }

    // ---- the quick panel's settings

    /**
     * Which items the quick panel shows and in what order, how soon it hides and how far one press moves the
     * brightness. OK shows or hides an item; a long OK picks it up, ↑/↓ carry it, OK or Back put it down.
     */
    private void openPanelSettings(String focus, String moving) {
        FrameLayout h = host();
        LinearLayout s = page(h, "Панель", "по удержанию Prime Video", true);
        section(s, "Пункты");
        LinearLayout fixed = panelRow("Яркость", "бегунок · всегда первая", true, false);
        fixed.setFocusable(false);
        fixed.setAlpha(0.7f);
        s.addView(fixed);
        List<String> order = Actions.panelOrder(this);
        View first = null, wanted = null;
        for (String id : order) {
            boolean on = Actions.panelShows(this, id);
            String sub = Actions.SLEEP.equals(id) ? "15 · 30 · 45 · 60 · 90 мин" : Actions.verb(id);
            LinearLayout r = panelRow(Actions.name(id), id.equals(moving) ? "↑ ↓ — двигать, OK — поставить" : sub, on, id.equals(moving));
            r.setTag("panel_" + id);
            Ui.focusable(r);
            r.setOnClickListener(v -> {
                if (moving != null) {
                    redrawPanelSettings(id, null);
                    return;
                }
                Actions.setPanelShows(this, id, !on);
                redrawPanelSettings(id, null);
            });
            r.setOnLongClickListener(v -> {
                redrawPanelSettings(id, id);
                return true;
            });
            if (id.equals(moving)) r.setOnKeyListener((v, k, e) -> {
                if (k == KeyEvent.KEYCODE_BACK) {
                    if (e.getAction() == KeyEvent.ACTION_UP) redrawPanelSettings(id, null);
                    return true;
                }
                if (k != KeyEvent.KEYCODE_DPAD_UP && k != KeyEvent.KEYCODE_DPAD_DOWN) return false;
                if (e.getAction() == KeyEvent.ACTION_DOWN) {
                    int i = order.indexOf(id), j = i + (k == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1);
                    if (j >= 0 && j < order.size()) {
                        java.util.Collections.swap(order, i, j);
                        Actions.setPanelOrder(this, order);
                        redrawPanelSettings(id, id);
                    }
                }
                return true;
            });
            LinearLayout.LayoutParams rl = (LinearLayout.LayoutParams) r.getLayoutParams();
            rl.topMargin = Ui.px(this, 10);
            s.addView(r);
            if (first == null) first = r;
            if (id.equals(focus)) wanted = r;
        }
        TextView tip = label("OK — показать или скрыть · долгий OK — взять и двигать стрелками");
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.topMargin = Ui.px(this, 16);
        s.addView(tip, tl);

        section(s, "Скрывать через");
        int[] secs = {3, 5, 10};
        int t = Actions.panelTimeout(this);
        LinearLayout hide = segmented(new String[]{"3 с", "5 с", "10 с"}, t == 3 ? 0 : t == 10 ? 2 : 1, 300, i -> {
            Actions.setPanelTimeout(this, secs[i]);
            redrawPanelSettings("timeout", null);
        });
        hide.setTag("timeout");
        s.addView(hide);
        section(s, "Шаг яркости");
        int[] steps = {5, 10};
        LinearLayout step = segmented(new String[]{"5%", "10%"}, Actions.brightnessStep(this) == 10 ? 1 : 0, 200, i -> {
            Actions.setBrightnessStep(this, steps[i]);
            redrawPanelSettings("step", null);
        });
        step.setTag("step");
        s.addView(step);
        show(h, "panel_settings");
        View f = wanted != null ? wanted : "timeout".equals(focus) ? hide.findViewWithTag("on")
                : "step".equals(focus) ? step.findViewWithTag("on") : first;
        if (f != null) f.requestFocus();
    }

    private void redrawPanelSettings(String focus, String moving) {
        openPanelSettings(focus, moving);
    }

    /** One item of the panel: its name and a line, a switch on the right; white while it is being moved. */
    private LinearLayout panelRow(String name, String sub, boolean on, boolean moving) {
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.px(this, 22), 0, Ui.px(this, 20), 0);
        r.setBackground(Ui.control(this, 24));
        r.setSelected(moving);
        TextView n = Ui.text(this, name, 16, 600, Color.WHITE);
        n.setTextColor(Ui.ink());
        r.addView(n, new LinearLayout.LayoutParams(0, -2, 1));
        TextView l = Ui.text(this, sub, 15, 400, Ui.TEXT_DIM);
        l.setTextColor(Ui.inkDim());
        r.addView(l);
        View sw = new View(this);
        sw.setBackground(Ui.round(on ? Ui.ON : 0x38FFFFFF, 0, 13, this));
        FrameLayout swb = new FrameLayout(this);
        swb.addView(sw, new FrameLayout.LayoutParams(-1, -1));
        View knob = new View(this);
        knob.setBackground(Ui.round(Color.WHITE, 0, 10, this));
        FrameLayout.LayoutParams kl = new FrameLayout.LayoutParams(Ui.px(this, 20), Ui.px(this, 20), Gravity.CENTER_VERTICAL | (on ? Gravity.END : Gravity.START));
        kl.leftMargin = kl.rightMargin = Ui.px(this, 3);
        swb.addView(knob, kl);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(Ui.px(this, 44), Ui.px(this, 26));
        sl.leftMargin = Ui.px(this, 18);
        r.addView(swb, sl);
        r.setLayoutParams(new LinearLayout.LayoutParams(Ui.px(this, 640), Ui.px(this, 56)));
        return r;
    }

    // ---- Telegram settings

    /**
     * Telegram, kept to what a household needs: who asks to write and who may, when read notes go away, and
     * the bot's name to write to. Connecting another bot hides behind one quiet line at the bottom.
     */
    private void openTelegram() {
        FrameLayout h = host();
        String bot = tg.botName();
        boolean ready = !tg.token().isEmpty();
        LinearLayout s = page(h, "Telegram", ready ? tg.status.toLowerCase(RU) : "бот не подключён", true);
        View first = null;

        if (!ready) {
            section(s, "Подключите бота — один раз");
            String[] steps = {"В Telegram откройте @BotFather и отправьте /newbot", "Придумайте имя — BotFather пришлёт токен", "Вставьте токен ниже и нажмите «Подключить»"};
            for (int i = 0; i < steps.length; i++) {
                LinearLayout st = row();
                st.setGravity(Gravity.CENTER_VERTICAL);
                st.setPadding(Ui.px(this, 16), 0, Ui.px(this, 22), 0);
                st.setBackground(Ui.glass(this, 22));
                TextView n = Ui.text(this, String.valueOf(i + 1), 15, 700, Color.WHITE);
                n.setGravity(Gravity.CENTER);
                n.setBackground(Ui.round(0x29FFFFFF, 0, 16, this));
                st.addView(n, new LinearLayout.LayoutParams(Ui.px(this, 32), Ui.px(this, 32)));
                TextView tx = Ui.text(this, steps[i], 17, 400, Color.WHITE);
                LinearLayout.LayoutParams txl = new LinearLayout.LayoutParams(-2, -2);
                txl.leftMargin = Ui.px(this, 16);
                st.addView(tx, txl);
                LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(Ui.px(this, 640), Ui.px(this, 64));
                if (i > 0) sl.topMargin = Ui.px(this, 10);
                s.addView(st, sl);
            }
            section(s, "Токен бота");
            View field = tokenRow(s);
            show(h, "telegram");
            field.requestFocus();
            return;
        }

        // someone found the bot and wrote: ask right at the top
        for (Telegram.Person p : tg.people("pending")) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(Ui.px(this, 24), Ui.px(this, 20), Ui.px(this, 24), Ui.px(this, 20));
            card.setBackground(Ui.glass(this, 26));
            card.addView(personLine(p, "хочет писать на телевизор"));
            LinearLayout btns = row();
            TextView yes = chip("Разрешить", false, v -> tg.approve(p.id, true));
            yes.setTag("yes_" + p.id);
            btns.addView(yes, new LinearLayout.LayoutParams(0, Ui.px(this, 50), 1));
            TextView no = chip("Нет", false, v -> tg.approve(p.id, false));
            no.setTag("no_" + p.id);
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, Ui.px(this, 50), 1);
            nl.leftMargin = Ui.px(this, 12);
            btns.addView(no, nl);
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, -2);
            bl.topMargin = Ui.px(this, 18);
            card.addView(btns, bl);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(Ui.px(this, 560), -2);
            cl.topMargin = Ui.px(this, 32);
            s.addView(card, cl);
            if (first == null) first = yes;
        }

        List<Telegram.Person> allowed = tg.people("allowed");
        section(s, "Кто может писать");
        if (allowed.isEmpty()) {
            s.addView(label("Пока никто. Напишите боту — здесь появится запрос"));
        } else {
            LinearLayout people = row();
            for (Telegram.Person p : allowed) {
                LinearLayout chip = personLine(p, null);
                chip.setPadding(Ui.px(this, 12), 0, Ui.px(this, 22), 0);
                chip.setBackground(Ui.control(this, 32));
                TextView rm = Ui.text(this, "убрать", 15, 400, Ui.TEXT_FAINT);
                LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
                rl.leftMargin = Ui.px(this, 12);
                chip.addView(rm, rl);
                chip.setTag("rm_" + p.id);
                Ui.focusable(chip);
                chip.setOnClickListener(v -> dialog(R.drawable.ic_send, "Убрать " + p.name + "?",
                        "Сообщения от " + p.name + " перестанут приходить на телевизор.", null,
                        new String[]{"Убрать", "Отмена"}, new Runnable[]{() -> tg.remove(p.id), null}, 1));
                chip.setLayoutParams(new LinearLayout.LayoutParams(-2, Ui.px(this, 64)));
                addSpaced(people, chip, 14);
            }
            s.addView(people);
        }

        section(s, "Сообщения исчезают");
        int[] hours = {0, 3, 24};
        int on = 0;
        for (int i = 0; i < hours.length; i++) if (tg.keepHours() == hours[i]) on = i;
        LinearLayout keep = segmented(new String[]{"Только по OK", "Через 3 часа", "Через сутки"}, on, 640, i -> tg.setKeepHours(hours[i]));
        for (int i = 0; i < hours.length; i++) keep.getChildAt(i).setTag("keep_" + hours[i]);
        s.addView(keep);
        if (first == null) first = keep.getChildAt(on);

        // the invitation: scanning the QR opens the bot with this TV's code, and that person may write at once
        qrBlock(h, tg.inviteLink(), "Наведите камеру — и пишите",
                "Telegram откроет бота и сам нажмёт «Старт» с кодом этого телевизора. Разрешать ничего не нужно. Код меняется каждые 10 минут."
                        + (bot.isEmpty() ? "" : " Кто найдёт @" + bot + " сам, без QR, попадёт сюда запросом."));

        // another bot: rarely needed, so one quiet line at the bottom that opens the field
        LinearLayout other = new LinearLayout(this);
        other.setOrientation(LinearLayout.VERTICAL);
        TextView more = Ui.text(this, "Подключить другого бота ›", 15, 500, Color.WHITE);
        more.setPadding(Ui.px(this, 14), Ui.px(this, 8), Ui.px(this, 14), Ui.px(this, 8));
        more.setBackground(Ui.control(this, 18));
        more.setAlpha(0.7f);
        more.setTag("other_bot");
        Ui.focusable(more);
        more.setOnClickListener(v -> {
            other.removeView(v);
            tokenRow(other).requestFocus();
        });
        other.addView(more);
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(-2, -2);
        ol.topMargin = Ui.px(this, 40);
        ol.leftMargin = -Ui.px(this, 14);
        s.addView(other, ol);

        show(h, "telegram");
        if (first != null) first.requestFocus();
    }

    /** An avatar and a name, with an optional quiet line under the name. */
    private LinearLayout personLine(Telegram.Person p, String sub) {
        LinearLayout pr = row();
        pr.setGravity(Gravity.CENTER_VERTICAL);
        pr.addView(avatar(p.id, p.name, 40));
        LinearLayout who = new LinearLayout(this);
        who.setOrientation(LinearLayout.VERTICAL);
        who.addView(Ui.text(this, p.name, 17, 600, Color.WHITE));
        if (sub != null) {
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2);
            sl.topMargin = Ui.px(this, 3);
            who.addView(label(sub), sl);
        }
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(-2, -2);
        wl.leftMargin = Ui.px(this, 12);
        pr.addView(who, wl);
        return pr;
    }

    /** The token field and its button, added to {@code parent}; returns the field. */
    private View tokenRow(LinearLayout parent) {
        LinearLayout tr = row();
        EditText token = new EditText(this);
        token.setTag("token");
        token.setSingleLine(true);
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        token.setImeOptions(EditorInfo.IME_ACTION_DONE);
        token.setHint("123456789:AAF…");
        token.setHintTextColor(Ui.TEXT_FAINT);
        token.setTextColor(Color.WHITE);
        token.setTypeface(Ui.font(this, 400));
        token.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.px(this, 17));
        token.setPadding(Ui.px(this, 22), 0, Ui.px(this, 22), 0);
        StateListDrawable tb = new StateListDrawable();
        GradientDrawable tf = Ui.round(0x38000000, 0, 29, this);
        tf.setStroke(Ui.px(this, 3), Color.WHITE);
        tb.addState(new int[]{android.R.attr.state_focused}, tf);
        tb.addState(new int[]{}, Ui.round(0x38000000, 0, 29, this));
        token.setBackground(tb);
        token.setOnFocusChangeListener((v, has) -> Ui.grow(v, has));
        tr.addView(token, new LinearLayout.LayoutParams(Ui.px(this, 520), Ui.px(this, 58)));
        TextView save = chip("Подключить", false, v -> {
            String t = token.getText().toString().trim();
            if (!t.isEmpty()) tg.setToken(t);
            closeOverlay();
            openTelegram();
        });
        save.setTag("save");
        save.setLayoutParams(new LinearLayout.LayoutParams(-2, Ui.px(this, 58)));
        addSpaced(tr, save, 12);
        parent.addView(tr);
        return token;
    }
}
