package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The look shared by every screen (the "Дизайн-система" page of the mockups): Inter, frosted glass for
 * everything that can be chosen, one white ring for the remote's cursor, white fill for what is selected in a
 * group and blue for what is switched on. Sizes are in the mockups' 1280×720 units, converted to pixels.
 */
final class Ui {
    static final int INK = 0xFF0B0F14;
    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_DIM = 0xBFFFFFFF;
    static final int TEXT_FAINT = 0xA6FFFFFF;
    static final int GLASS = 0x1FFFFFFF;
    static final int GLASS_FOCUS = 0x2EFFFFFF;
    static final int GLASS_LINE = 0x24FFFFFF;
    /** The rim of glass: lit at the top edge, nearly gone at the bottom. */
    static final int RIM_TOP = 0x52FFFFFF, RIM_BOTTOM = 0x0FFFFFFF;
    /** Glass of something switched on: brighter, not coloured. */
    static final int GLASS_ON = 0x47FFFFFF;
    /** The chosen one of a group: brighter glass, text stays white; only the cursor is white. */
    static final int SELECTED = 0x52FFFFFF;
    /** Quiet text on white. */
    static final int INK_DIM = 0xA60B0F14;
    /**
     * The tint of every glass panel in the launcher: the dock, notes, menus, dialogs, pages, the quick panel over
     * Home. One tint everywhere, so nothing looks brighter or richer than the rest.
     */
    static final int DARK_GLASS = 0x5C12161C;
    /** Corners of the floating panels over Home: the dock, its tile menu, the quick panel. One radius for all. */
    static final float RADIUS_PANEL = 34;
    /** Corners of dialogs. */
    static final float RADIUS_DIALOG = 28;
    /**
     * A capsule: the radius is always half the height, whatever the height. Buttons, chips, segments and fields
     * are capsules; rows, cards, tiles and panels are rounded rectangles with their own radius.
     */
    static final float PILL = 999;
    /** The quick panel over another app: there is nothing to blur there, so a plain fill, light enough to see the film. */
    static final int PANEL_OVER_APP = 0xD91E2228;
    /** Dialogs and menus. Nearly opaque: without the mockups' blur, anything behind would show through the text. */
    static final int SURFACE = 0xF01E2228;
    static final int SCRIM = 0x80040810;
    static final int ON = 0xFF0A84FF;
    static final int WARN = 0xFFFFD27A;
    static final int BADGE = 0xFFFF453A;
    /** The same growth as a dock tile, so every control answers the cursor alike. */
    static final float FOCUS_SCALE = 1.07f;

    private static Typeface[] faces;

    private Ui() {
    }

    /** Design units (the mockups' 1280-wide frame) to pixels. */
    static int px(Context c, float design) {
        return Math.round(design * c.getResources().getDisplayMetrics().widthPixels / 1280f);
    }

    static Typeface font(Context c, int weight) {
        if (faces == null) faces = new Typeface[10];
        int i = Math.max(1, Math.min(9, weight / 100));
        if (faces[i] == null) {
            faces[i] = new Typeface.Builder(c.getResources().getAssets(), "fonts/inter.ttf")
                    .setFontVariationSettings("'wght' " + (i * 100)).build();
        }
        return faces[i];
    }

    static TextView text(Context c, String s, float size, int weight, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(c, size));
        t.setTypeface(font(c, weight));
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    /** A section label: small capitals, spaced, half-transparent ("СИСТЕМА"). */
    static TextView caps(Context c, String s) {
        TextView t = text(c, s.toUpperCase(Lang.locale(c)), 14, 600, TEXT_FAINT);
        t.setLetterSpacing(Lang.tracking(c, 0.15f));
        return t;
    }

    static GradientDrawable round(int fill, int line, float radius, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(px(c, radius));
        if (line != 0) g.setStroke(Math.max(1, px(c, 1)), line);
        return g;
    }

    /** Glass for a control: a light fill and a rim that catches the light at the top, as tvOS draws it. */
    static Drawable glass(Context c, float radius) {
        return new Rim(c, GLASS, radius);
    }

    /**
     * A control background, the tvOS way: no ring. Under the cursor it turns white (and grows and lifts, see
     * {@link #grow}). The one chosen in its group is brighter glass with white text: the cursor and the choice
     * never look alike, so it is always clear what OK would do and what is set.
     */
    static Drawable control(Context c, float radius) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, round(Color.WHITE, 0, radius, c));
        s.addState(new int[]{android.R.attr.state_selected}, new Rim(c, SELECTED, radius));
        s.addState(new int[]{}, glass(c, radius));
        return s;
    }

    /**
     * One segment of a segmented switch: no glass of its own, the groove is the glass. Only the chosen segment
     * (brighter) and the one under the cursor (white) are filled.
     */
    static Drawable segment(Context c, float radius) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, round(Color.WHITE, 0, radius, c));
        s.addState(new int[]{android.R.attr.state_selected}, round(SELECTED, 0, radius, c));
        s.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        return s;
    }

    /**
     * Like {@link #control}, for something that is switched on or off: on, its glass is brighter. The state is
     * also said in words next to it ("включён"), never by the fill alone.
     */
    static Drawable toggle(Context c, float radius, boolean on) {
        if (!on) return control(c, radius);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, round(Color.WHITE, 0, radius, c));
        s.addState(new int[]{}, new Rim(c, GLASS_ON, radius));
        return s;
    }

    /** Text on a control: dark on the cursor's white, white otherwise (the chosen one included). */
    static ColorStateList ink() {
        return new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{INK, TEXT});
    }

    static ColorStateList inkDim() {
        return new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{INK_DIM, TEXT_DIM});
    }

    /** The colours text and icons had before the cursor turned their control white. */
    private static final java.util.WeakHashMap<View, Integer> plain = new java.util.WeakHashMap<>();

    /** Makes a focusable control: white, larger and lifted under the cursor; what is on it turns dark. */
    static void focusable(View v) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnFocusChangeListener((view, has) -> {
            grow(view, has);
            if (view instanceof ViewGroup) refresh((ViewGroup) view);
            inkOn(view, has);
        });
        if (v instanceof ViewGroup) refresh((ViewGroup) v);
    }

    /**
     * Turns the text and line icons on a control dark while it is white under the cursor, and back after. Ones
     * that already follow the state ({@link #ink}) are left alone, and so are pictures (app icons, photos).
     */
    static void inkOn(View v, boolean dark) {
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            if (t.getTextColors().isStateful() && !plain.containsKey(t)) return;
            if (dark) {
                if (!plain.containsKey(t)) plain.put(t, t.getCurrentTextColor());
                int was = plain.get(t);
                t.setTextColor(Color.alpha(was) >= 0xF0 ? INK : INK_DIM);
            } else if (plain.containsKey(t)) {
                t.setTextColor(plain.remove(t));
            }
        } else if (v instanceof ImageView) {
            ImageView iv = (ImageView) v;
            if (!(iv.getDrawable() instanceof android.graphics.drawable.VectorDrawable)) return;
            if (iv.getImageTintList() != null && !plain.containsKey(iv)) return;
            if (dark) {
                plain.put(iv, 0);
                iv.setImageTintList(ColorStateList.valueOf(INK));
            } else if (plain.remove(iv) != null) {
                iv.setImageTintList(null);
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) inkOn(g.getChildAt(i), dark);
        }
    }

    /** A rounded fill with a rim lit from above: bright at the top edge, fading towards the bottom. */
    static final class Rim extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF r = new android.graphics.RectF();
        private final float radius;

        Rim(Context c, int fillColor, float radius) {
            this.radius = px(c, radius);
            fill.setColor(fillColor);
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(Math.max(1, px(c, 1)));
        }

        @Override
        protected void onBoundsChange(android.graphics.Rect b) {
            rim.setShader(new android.graphics.LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{RIM_TOP, RIM_BOTTOM}, null, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas c) {
            r.set(getBounds());
            c.drawRoundRect(r, radius, radius, fill);
            float h = rim.getStrokeWidth() / 2;
            r.inset(h, h);
            c.drawRoundRect(r, radius, radius, rim);
        }

        @Override
        public void getOutline(android.graphics.Outline o) {
            // a capsule's radius is wider than half the height: the outline (and its shadow) needs it clamped
            o.setRoundRect(getBounds(), Math.min(radius, getBounds().height() / 2f));
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter f) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * The press under OK, tvOS's "highlighted" state: the white control dips a little while OK is down and springs
     * back to its focused size on release, so a click is felt even when nothing else moves.
     */
    static void press(View v, android.view.KeyEvent e) {
        if (v == null || !v.isClickable() || e.getRepeatCount() > 0) return;
        float full = v.getScaleX() > 1f ? v.getScaleX() : 1f;
        if (e.getAction() == android.view.KeyEvent.ACTION_DOWN) {
            if (v.getTag(R.id.press_scale) == null) v.setTag(R.id.press_scale, full);
            float s = (float) v.getTag(R.id.press_scale);
            v.animate().scaleX(s * 0.95f).scaleY(s * 0.95f).setDuration(90).start();
        } else if (e.getAction() == android.view.KeyEvent.ACTION_UP && v.getTag(R.id.press_scale) != null) {
            float s = (float) v.getTag(R.id.press_scale);
            v.setTag(R.id.press_scale, null);
            v.animate().scaleX(s).scaleY(s).setInterpolator(FocusRing.EASE).setDuration(180).start();
        }
    }

    /** Controls that live inside something tight (a segment in its groove): how far they may grow, in px a side. */
    private static final java.util.WeakHashMap<View, Integer> roomOf = new java.util.WeakHashMap<>();

    /**
     * Keeps a control's growth inside {@code units} on every side and its lift low: a segment at the end of a
     * groove would otherwise grow over the groove's rim.
     */
    static void growWithin(View v, float units) {
        roomOf.put(v, px(v.getContext(), units));
    }

    /** The cursor's growth, for a control that handles its own focus (a text field grows its whole pill). */
    static void grow(View view, boolean has) {
        // a view focused before its first layout (a note card that has just arrived) has no width yet: grown now,
        // it would take a dock tile's full growth and lose its sides; it grows once it is laid out
        if (has && view.getWidth() == 0) {
            view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                    if (r - l == 0) return;
                    v.removeOnLayoutChangeListener(this);
                    grow(v, v.hasFocus());
                }
            });
            return;
        }
        // small controls grow like a dock tile; wide ones (a whole row) by at most 10 px a side, or they run off the screen
        float s = has ? Math.min(FOCUS_SCALE, 1f + 2f * px(view.getContext(), 10) / Math.max(1, view.getWidth())) : 1f;
        Integer room = roomOf.get(view);
        float lift = px(view.getContext(), 12);
        if (room != null) {
            if (has) s = Math.min(s, 1f + 2f * room / Math.max(1, Math.max(view.getWidth(), view.getHeight())));
            lift = px(view.getContext(), 4);
        }
        // the shadow belongs to the white, opaque state only: fading out under translucent glass it shows through
        // as a dark square, so it goes at once when the cursor leaves
        if (!has) view.setTranslationZ(0);
        view.animate().scaleX(s).scaleY(s).translationZ(has ? lift : 0)
                .setInterpolator(FocusRing.EASE).setDuration(FocusRing.MS).start();
    }

    private static void refresh(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View ch = g.getChildAt(i);
            ch.setDuplicateParentStateEnabled(true);
            ch.refreshDrawableState();
            if (ch instanceof ViewGroup) refresh((ViewGroup) ch);
        }
    }

    static ImageView icon(Context c, int res, float size) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setImageTintList(ink());
        iv.setDuplicateParentStateEnabled(true);
        iv.setLayoutParams(new LinearLayout.LayoutParams(px(c, size), px(c, size)));
        return iv;
    }

    /** A quick-settings card: icon at the top, name and a quiet line at the bottom. */
    static LinearLayout tile(Context c, int iconRes, String label, String sub, float w, float h, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(px(c, 18), px(c, 18), px(c, 16), px(c, 18));
        t.setBackground(control(c, 26));
        if (iconRes != 0) t.addView(icon(c, iconRes, 26));
        View push = new View(c);
        t.addView(push, new LinearLayout.LayoutParams(1, 0, 1));
        TextView l = text(c, label, 17, 600, TEXT);
        l.setTextColor(ink());
        l.setDuplicateParentStateEnabled(true);
        l.setSingleLine(true);
        l.setEllipsize(TextUtils.TruncateAt.END);
        t.addView(l);
        if (sub != null) {
            TextView s = text(c, sub, 15, 400, TEXT_DIM);
            s.setTextColor(inkDim());
            s.setDuplicateParentStateEnabled(true);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = px(c, 4);
            t.addView(s, sp);
        }
        focusable(t);
        t.setOnClickListener(click);
        t.setLayoutParams(new LinearLayout.LayoutParams(px(c, w), px(c, h)));
        return t;
    }

    /**
     * A round system button (Wi-Fi, VPN, sound…): the circle takes the cursor, the name and a quiet line sit
     * under it. Switched on, the circle is blue.
     */
    static LinearLayout round(Context c, int iconRes, String label, String sub, boolean on, View.OnClickListener click) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setClipChildren(false);
        android.widget.FrameLayout circle = new android.widget.FrameLayout(c);
        circle.setBackground(toggle(c, 48, on));
        ImageView iv = new ImageView(c);
        iv.setImageResource(iconRes);
        circle.addView(iv, new android.widget.FrameLayout.LayoutParams(px(c, 30), px(c, 30), Gravity.CENTER));
        focusable(circle);
        circle.setOnClickListener(click);
        col.addView(circle, new LinearLayout.LayoutParams(px(c, 96), px(c, 96)));
        TextView l = text(c, label, 15, 600, TEXT);
        l.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = px(c, 12);
        col.addView(l, lp);
        if (sub != null) {
            TextView s = text(c, sub, 15, 400, TEXT_DIM);
            s.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.topMargin = px(c, 4);
            col.addView(s, sp);
        }
        // 28 wider than the circle: "Fernbedienung" and "TV-Einstellungen" fit on one line
        col.setLayoutParams(new LinearLayout.LayoutParams(px(c, 140), -2));
        return col;
    }

    static void shadow(TextView t) {
        t.setShadowLayer(20, 0, 1, 0x4D000000);
    }

    /**
     * Frosted glass: the blurred bay ({@link Backdrop}) that lies under the panel, tinted. The blur is placed by
     * where the panel sits in the layout, not where an animation has moved it, so a page sliding in carries its
     * glass along and it settles right. {@code radius} 0 is a whole page.
     */
    static final class Glass extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Matrix m = new android.graphics.Matrix();
        private final android.graphics.RectF r = new android.graphics.RectF();
        private final int[] at = new int[2];
        private final float radius;
        private Bitmap shaderOf;

        Glass(Context c, int tintColor, int lineColor, float radius) {
            this.radius = px(c, radius);
            tint.setColor(tintColor);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(Math.max(1, px(c, 1)));
            line.setColor(lineColor);
        }

        @Override
        public void draw(Canvas c) {
            r.set(getBounds());
            Bitmap blur = Backdrop.current();
            Object cb = getCallback();
            if (blur != null && cb instanceof View) {
                View v = (View) cb;
                if (shaderOf != blur) {
                    paint.setShader(new BitmapShader(blur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                    shaderOf = blur;
                }
                // the panel's place on screen by layout: lefts and tops up to the root, scrolls taken off
                float x = 0, y = 0;
                View p = v;
                while (p != null) {
                    x += p.getLeft() - (p == v ? 0 : p.getScrollX());
                    y += p.getTop() - (p == v ? 0 : p.getScrollY());
                    p = p.getParent() instanceof View ? (View) p.getParent() : null;
                }
                // a window of its own (the quick panel) does not start at the screen's corner: add where it is
                View root = v.getRootView();
                root.getLocationOnScreen(at);
                android.util.DisplayMetrics dm = v.getResources().getDisplayMetrics();
                m.setScale(dm.widthPixels / (float) blur.getWidth(), dm.heightPixels / (float) blur.getHeight());
                m.postTranslate(-x - at[0], -y - at[1]);
                paint.getShader().setLocalMatrix(m);
                c.drawRoundRect(r, radius, radius, paint);
            }
            c.drawRoundRect(r, radius, radius, tint);
            if (line.getColor() != 0) {
                float h = line.getStrokeWidth() / 2;
                r.inset(h, h);
                c.drawRoundRect(r, radius, radius, line);
            }
        }

        @Override
        public void getOutline(android.graphics.Outline o) {
            // the shadow of a lifted card (a menu, a dialog) follows the rounded glass
            o.setRoundRect(getBounds(), radius);
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter f) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** Sets {@link Glass} as the background of {@code v} and keeps it in step with the bay. */
    static void glassBackground(View v, int tintColor, int lineColor, float radius) {
        v.setBackground(new Glass(v.getContext(), tintColor, lineColor, radius));
        Backdrop.watch(v);
    }

    /**
     * A pill with an icon in front, 52 high: one item of the quick panel. Blue while switched on (night, a sleep
     * timer running).
     */
    static LinearLayout pill(Context c, int iconRes, String label, boolean on, View.OnClickListener click) {
        LinearLayout p = new LinearLayout(c);
        p.setGravity(Gravity.CENTER_VERTICAL);
        p.setPadding(px(c, 18), 0, px(c, 22), 0);
        p.setBackground(toggle(c, PILL, on));
        ImageView iv = new ImageView(c);
        iv.setImageResource(iconRes);
        p.addView(iv, new LinearLayout.LayoutParams(px(c, 20), px(c, 20)));
        TextView t = text(c, label, 16, 600, TEXT);
        t.setSingleLine(true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.leftMargin = px(c, 10);
        p.addView(t, tl);
        focusable(p);
        p.setOnClickListener(click);
        p.setLayoutParams(new LinearLayout.LayoutParams(-2, px(c, 52)));
        return p;
    }

    /**
     * A slider's track, knob and fill; it takes no focus itself. The row around it is what the cursor lands on,
     * and its ←/→ move the value, so the whole row answers the remote like one control.
     */
    static final class Slider extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private float fraction;

        Slider(Context c) {
            super(c);
        }

        @Override
        protected void drawableStateChanged() {
            // its row took or lost the cursor (the state is the row's, duplicated): colours change with it
            super.drawableStateChanged();
            invalidate();
        }

        /** 0 at the left end, 1 at the right. */
        void set(float f) {
            fraction = Math.max(0, Math.min(1, f));
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            float knob = px(getContext(), 14), track = px(getContext(), 5), cy = getHeight() / 2f;
            float left = knob, right = getWidth() - knob, x = left + (right - left) * fraction;
            // on the white row under the cursor the slider is drawn dark, like the text next to it
            View row = (View) getParent();
            boolean dark = row != null && row.isFocused();
            paint.setColor(dark ? 0x290B0F14 : GLASS_LINE);
            canvas.drawRoundRect(left, cy - track, right, cy + track, track, track, paint);
            paint.setColor(dark ? INK : Color.WHITE);
            canvas.drawRoundRect(left, cy - track, x, cy + track, track, track, paint);
            paint.setShadowLayer(px(getContext(), 4), 0, px(getContext(), 1), dark ? 0x40000000 : 0x66000000);
            canvas.drawCircle(x, cy, knob, paint);
            paint.clearShadowLayer();
        }
    }
}
