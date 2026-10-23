package io.github.vivchar.lagoon;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * One white frame that glides from app tile to app tile instead of each tile lighting up on its own.
 * It is drawn, never laid out, so a move costs a redraw and not a layout pass of the whole screen, and it
 * reads the tile's place on every frame: when the shelf or the grid scrolls the tile into view, the frame
 * rides along instead of landing where the tile was a moment ago.
 */
final class FocusRing {
    static final float TILE_SCALE = 1.07f;
    /** Material's "emphasized decelerate": quick start, long soft landing, no bounce. */
    static final android.view.animation.Interpolator EASE = new android.view.animation.PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    static final int MS = 280;

    private final FrameLayout host;
    private final Frame ring;
    private final ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
    private final int[] a = new int[2], b = new int[2];
    private View target;
    private boolean shown;
    /** Where the frame was when the current move began, and how far along the move is. */
    private float x0, y0, w0, h0, f = 1f;

    FocusRing(Context c, FrameLayout host) {
        this.host = host;
        ring = new Frame(c);
        ring.setElevation(Ui.px(c, 12));
        ring.setAlpha(0f);
        host.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        anim.setDuration(MS);
        anim.setInterpolator(EASE);
        anim.addUpdateListener(v -> {
            f = (float) v.getAnimatedValue();
            track();
        });
        // scrolls and the tile's own zoom move it without telling us: look again before every frame
        host.getViewTreeObserver().addOnPreDrawListener(() -> {
            if (target != null) track();
            return true;
        });
    }

    void follow(View tile) {
        if (tile == target && shown) return;
        target = tile;
        if (host.getChildAt(host.getChildCount() - 1) != ring) ring.bringToFront();
        if (!shown || ring.w == 0) {
            // first appearance: no glide from nowhere, just fade in on the tile
            f = 1f;
            shown = true;
            ring.animate().alpha(1f).setDuration(160).start();
            track();
            return;
        }
        x0 = ring.x; y0 = ring.y; w0 = ring.w; h0 = ring.h;
        f = 0f;
        ring.animate().alpha(1f).setDuration(120).start();
        anim.start();
    }

    void hide() {
        shown = false;
        target = null;
        anim.cancel();
        ring.animate().alpha(0f).setDuration(120).start();
    }

    private void track() {
        View t = target;
        if (t == null || !t.isAttachedToWindow() || t.getWidth() == 0) return;
        t.getLocationInWindow(a);
        host.getLocationInWindow(b);
        // the tile zooms around its centre; frame the size it is zooming to
        float tw = t.getWidth() * TILE_SCALE, th = t.getHeight() * TILE_SCALE;
        float tx = a[0] - b[0] - (tw - t.getWidth() * t.getScaleX()) / 2f;
        float ty = a[1] - b[1] - (th - t.getHeight() * t.getScaleY()) / 2f;
        if (f < 1f) {
            tx = x0 + (tx - x0) * f;
            ty = y0 + (ty - y0) * f;
            tw = w0 + (tw - w0) * f;
            th = h0 + (th - h0) * f;
        }
        ring.place(tx, ty, tw, th);
    }

    private static final class Frame extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius, stroke;
        float x, y, w, h;

        Frame(Context c) {
            super(c);
            stroke = Ui.px(c, 3);
            radius = Ui.px(c, 18 * TILE_SCALE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setColor(Color.WHITE);
            // a thin dark line just outside the white one: the ring stays visible on white banners
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(Ui.px(c, 1.5f));
            edge.setColor(0x660B0F14);
        }

        void place(float nx, float ny, float nw, float nh) {
            if (Math.abs(nx - x) < 0.25f && Math.abs(ny - y) < 0.25f && Math.abs(nw - w) < 0.25f && Math.abs(nh - h) < 0.25f) return;
            x = nx; y = ny; w = nw; h = nh;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            if (w == 0) return;
            // the ring sits just outside the tile, so it never covers the banner's edge
            float s = stroke / 2f, e = stroke + edge.getStrokeWidth() / 2f;
            c.drawRoundRect(x - s, y - s, x + w + s, y + h + s, radius + s, radius + s, paint);
            c.drawRoundRect(x - e, y - e, x + w + e, y + h + e, radius + e, radius + e, edge);
        }
    }
}
