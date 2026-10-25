package io.github.vivchar.lagoon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.Log;
import android.view.View;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * The frosted glass of the panels. The stick has no blur (that came with Android 12), and the bay plays on a
 * surface no view can read back, so the glass blurs the painted backdrops instead: each one is decoded tiny,
 * box-blurred once and mixed by the same weights as the bay behind it. Blurred this much, a still picture can't
 * be told from the moving clip. {@link Ui.Glass} draws the part of it under each panel.
 */
final class Backdrop {
    /** Home's scrim over the bay: darker at the top for the clock and at the bottom for the dock. */
    static final int[] SCRIM_COLORS = {0x61060A10, 0x00060A10, 0x00060A10, 0x6B060A10};

    /** A view that darkens the bay like Home does; the glass is made from the bay under it too. */
    static View scrim(Context c) {
        View v = new View(c);
        v.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, SCRIM_COLORS));
        return v;
    }

    /** Blurred backdrops are this wide; drawn back up to the screen, the bitmap filter smooths them out. */
    private static final int W = 96, H = 54;

    private static final Bitmap[] blurred = new Bitmap[SceneView.BACKDROPS.length];
    private static Bitmap mix;
    private static int mixKey = -1;
    private static final List<WeakReference<View>> glass = new ArrayList<>();
    /** The dark top and bottom of Home ({@link #scrim}), as one paint for the small mix. */
    private static final Paint SCRIM = new Paint();

    static {
        SCRIM.setShader(new LinearGradient(0, 0, 0, H, SCRIM_COLORS, null, Shader.TileMode.CLAMP));
    }

    private Backdrop() {
    }

    /** The blur of the bay as it is now; null until {@link #update} has run once. */
    static Bitmap current() {
        return mix;
    }

    /** A view whose background is the glass: it is drawn again when the bay behind it changes. */
    static void watch(View v) {
        for (WeakReference<View> r : glass) if (r.get() == v) return;
        glass.add(new WeakReference<>(v));
    }

    /**
     * Mixes the blurred backdrops by {@code w} (the bay's weights for this moment) and redraws the glass if that
     * changed. Main thread; the mix is 96×54, cheap enough to run each minute.
     */
    static void update(Context c, float[] w) {
        int key = 0;
        for (float x : w) key = key * 49 + Math.round(x * 48);
        if (key == mixKey && mix != null) return;
        if (mix == null) mix = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mix);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        float acc = 0;
        boolean any = false;
        for (int i = 0; i < w.length; i++) {
            if (w[i] <= 0.001f) continue;
            Bitmap b = blurred(c, i);
            if (b == null) continue;
            acc += w[i];
            paint.setAlpha(Math.round(255 * w[i] / acc));
            canvas.drawBitmap(b, 0, 0, paint);
            any = true;
        }
        if (!any) return;
        // what the eye sees under the glass is the bay with Home's shades over it: the same here, or the glass
        // would come out lighter than the bay around it
        canvas.save();
        canvas.scale(W / 1280f, H / 720f);
        SceneView.shade(canvas);
        canvas.restore();
        canvas.drawPaint(SCRIM);
        mixKey = key;
        for (int i = glass.size() - 1; i >= 0; i--) {
            View v = glass.get(i).get();
            if (v == null) glass.remove(i);
            else v.invalidate();
        }
    }

    private static Bitmap blurred(Context c, int i) {
        if (blurred[i] != null) return blurred[i];
        BitmapFactory.Options o = new BitmapFactory.Options();
        // 1920 → 120: the decoder does most of the shrinking, nothing big is ever in memory
        o.inSampleSize = 16;
        try (InputStream in = c.getAssets().open("scene/" + SceneView.BACKDROPS[i] + ".webp")) {
            Bitmap src = BitmapFactory.decodeStream(in, null, o);
            if (src == null) return null;
            Bitmap small = Bitmap.createScaledBitmap(src, W, H, true);
            if (small != src) src.recycle();
            blur(small, 3);
            blurred[i] = small;
        } catch (Exception e) {
            Log.w("LagoonScene", "glass " + SceneView.BACKDROPS[i], e);
        }
        return blurred[i];
    }

    /** Three box blurs in a row look like a Gaussian; at 96 px a radius of 3 is about 60 px on a 1080p screen. */
    private static void blur(Bitmap b, int r) {
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h], tmp = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            box(px, tmp, w, h, r, true);
            box(tmp, px, w, h, r, false);
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
    }

    /** One box blur along rows ({@code horizontal}) or columns, edges clamped. */
    private static void box(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int n = horizontal ? w : h, lines = horizontal ? h : w, div = 2 * r + 1;
        for (int line = 0; line < lines; line++) {
            int ra = 0, ga = 0, ba = 0;
            for (int k = -r; k <= r; k++) {
                int p = in[at(line, Math.max(0, Math.min(n - 1, k)), w, horizontal)];
                ra += (p >> 16) & 255;
                ga += (p >> 8) & 255;
                ba += p & 255;
            }
            for (int k = 0; k < n; k++) {
                out[at(line, k, w, horizontal)] = 0xFF000000 | (ra / div) << 16 | (ga / div) << 8 | (ba / div);
                int add = in[at(line, Math.min(n - 1, k + r + 1), w, horizontal)];
                int sub = in[at(line, Math.max(0, k - r), w, horizontal)];
                ra += ((add >> 16) & 255) - ((sub >> 16) & 255);
                ga += ((add >> 8) & 255) - ((sub >> 8) & 255);
                ba += (add & 255) - (sub & 255);
            }
        }
    }

    private static int at(int line, int k, int w, boolean horizontal) {
        return horizontal ? line * w + k : k * w + line;
    }
}
