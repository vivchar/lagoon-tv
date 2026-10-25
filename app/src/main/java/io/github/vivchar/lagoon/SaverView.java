package io.github.vivchar.lagoon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.View;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * The screensaver: photos from the iCloud album, one, two or three at a time, slowly drifting, with the clock,
 * the date and the weather at the bottom left and when the photo was taken at the bottom right. A vertical
 * photo alone stands in the middle on a blurred copy of itself. Photos are decoded off the UI thread, the next
 * slide always ready before the current one ends, and slides cross-fade.
 */
final class SaverView extends View {
    private static final long FADE_MS = 1200;

    /** What one screen shows: one to three photos and how they share it. */
    private static final class Slide {
        int kind;
        Bitmap[] photos;
        Bitmap blur;
        String when;
        int at;
    }

    private static final int LAND = 0, PORTRAIT = 1, TWO = 2, THREE = 3;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scrim = new Paint();
    private final Matrix m = new Matrix();
    private final Path clip = new Path();
    private final RectF r = new RectF();
    private final Weather weather;
    private final HandlerThread worker = new HandlerThread("saver");
    private Handler work;

    private List<File> files;
    private int next;
    private Slide cur, prev, ready;
    private long shownAt;
    private boolean preparing;

    SaverView(Context c) {
        super(c);
        weather = new Weather(c);
        setBackgroundColor(0xFF000000);
        worker.start();
        work = new Handler(worker.getLooper());
        files = Saver.photos(c);
        Saver.syncAsync(c, () -> post(() -> {
            files = Saver.photos(getContext());
            if (cur == null) prepare();
        }));
        prepare();
    }

    private float u(float design) {
        return design * getWidth() / 1280f;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        worker.quitSafely();
    }

    /** Builds the next slide on the worker thread. */
    private void prepare() {
        if (preparing || files.isEmpty()) return;
        preparing = true;
        List<File> list = files;
        int start = next;
        int layout = Saver.layout(getContext());
        int w = Math.max(1, getResources().getDisplayMetrics().widthPixels), h = Math.max(1, getResources().getDisplayMetrics().heightPixels);
        work.post(() -> {
            Slide s = new Slide();
            s.at = start % list.size();
            File first = list.get(s.at);
            boolean portrait = isPortrait(first);
            int kind;
            if (layout == Saver.ONE) kind = portrait ? PORTRAIT : LAND;
            else if (layout == Saver.TWO) kind = TWO;
            else if (layout == Saver.THREE) kind = THREE;
            else {
                // automatic: two verticals side by side, otherwise one photo, every third screen a trio
                File second = list.get((s.at + 1) % list.size());
                if (portrait && list.size() > 1 && isPortrait(second)) kind = TWO;
                else if (!portrait && list.size() > 2 && (s.at / 2) % 3 == 2) kind = THREE;
                else kind = portrait ? PORTRAIT : LAND;
            }
            int count = kind == TWO ? 2 : kind == THREE ? 3 : 1;
            count = Math.min(count, list.size());
            if (count < 2 && (kind == TWO || kind == THREE)) kind = portrait ? PORTRAIT : LAND;
            s.kind = kind;
            s.photos = new Bitmap[count];
            for (int i = 0; i < count; i++) s.photos[i] = decode(list.get((s.at + i) % list.size()), kind == LAND ? w : w / 2);
            if (kind == PORTRAIT && s.photos[0] != null) {
                // a blurred copy fills the sides: shrink hard, the bitmap filter smears it back up
                Bitmap b = s.photos[0];
                s.blur = Bitmap.createScaledBitmap(b, 24, Math.max(1, 24 * b.getHeight() / b.getWidth()), true);
            }
            int[] d = Saver.date(first);
            // the month's own (nominative) name from the chosen language, then the year
            java.util.Calendar month = java.util.Calendar.getInstance();
            month.set(d == null ? 2000 : d[0], d == null ? 0 : d[1] - 1, 1);
            s.when = d == null ? "" : Lang.date(getContext(), "LLLL", month.getTime()) + " " + d[0];
            int used = count;
            post(() -> {
                ready = s;
                next = (start + used) % Math.max(1, list.size());
                preparing = false;
                if (cur == null) advance();
                invalidate();
            });
        });
    }

    private void advance() {
        if (ready == null) return;
        prev = cur;
        cur = ready;
        ready = null;
        shownAt = SystemClock.uptimeMillis();
        prepare();
    }

    private static boolean isPortrait(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        boolean rotated = false;
        try {
            int ori = new ExifInterface(f.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            rotated = ori == 6 || ori == 8;
        } catch (Exception ignored) {
        }
        return rotated ? o.outWidth > o.outHeight : o.outHeight > o.outWidth;
    }

    /** The photo shrunk on decode to about {@code max} pixels wide and turned upright. */
    private static Bitmap decode(File f, int max) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        if (o.outWidth <= 0) return null;
        o.inJustDecodeBounds = false;
        o.inSampleSize = 1;
        while (Math.min(o.outWidth, o.outHeight) / (o.inSampleSize * 2) >= max * 0.6f) o.inSampleSize *= 2;
        Bitmap b = BitmapFactory.decodeFile(f.getPath(), o);
        if (b == null) return null;
        try {
            int ori = new ExifInterface(f.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            int deg = ori == 6 ? 90 : ori == 3 ? 180 : ori == 8 ? 270 : 0;
            if (deg != 0) {
                Matrix rot = new Matrix();
                rot.postRotate(deg);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), rot, true);
            }
        } catch (Exception ignored) {
        }
        return b;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        long every = Saver.every(getContext()) * 1000L;
        if (cur != null && ready != null && now - shownAt > every) advance();
        if (cur == null) {
            drawEmpty(c);
        } else {
            float f = Math.min(1f, (now - shownAt) / (float) FADE_MS);
            if (prev != null && f < 1f) drawSlide(c, prev, 1f, every + FADE_MS);
            drawSlide(c, cur, prev == null ? 1f : f, now - shownAt);
            if (f >= 1f && prev != null) prev = null;
        }
        drawOverlay(c);
        postInvalidateOnAnimation();
    }

    private void drawEmpty(Canvas c) {
        text.setTypeface(Ui.font(getContext(), 400));
        text.setTextSize(u(18));
        text.setColor(0x99FFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(files.isEmpty() ? getContext().getString(R.string.photos_loading) : "", getWidth() / 2f, getHeight() / 2f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSlide(Canvas c, Slide s, float alpha, long age) {
        float drift = Math.min(1f, age / (Saver.every(getContext()) * 1000f + FADE_MS));
        int a = Math.round(alpha * 255);
        float w = getWidth(), h = getHeight(), gap = u(8);
        switch (s.kind) {
            case PORTRAIT:
                if (s.blur != null) {
                    paint.setAlpha(Math.round(a * 0.6f));
                    cover(c, s.blur, -u(40), -u(40), w + u(80), h + u(80), 0);
                }
                float pw = u(388), ph = u(640);
                r.set((w - pw) / 2f, u(40), (w + pw) / 2f, u(40) + ph);
                c.save();
                clip.reset();
                clip.addRoundRect(r, u(20), u(20), Path.Direction.CW);
                c.clipPath(clip);
                paint.setAlpha(a);
                cover(c, s.photos[0], r.left, r.top, r.width(), r.height(), drift);
                c.restore();
                break;
            case TWO:
                paint.setAlpha(a);
                float half = (w - gap) / 2f;
                cover(c, s.photos[0], 0, 0, half, h, drift);
                if (s.photos.length > 1) cover(c, s.photos[1], half + gap, 0, half, h, drift);
                break;
            case THREE:
                paint.setAlpha(a);
                float big = w * 852f / 1280f;
                cover(c, s.photos[0], 0, 0, big, h, drift);
                float side = w - big - gap, sh = (h - gap) / 2f;
                if (s.photos.length > 1) cover(c, s.photos[1], big + gap, 0, side, sh, drift);
                if (s.photos.length > 2) cover(c, s.photos[2], big + gap, sh + gap, side, sh, drift);
                break;
            default:
                paint.setAlpha(a);
                cover(c, s.photos[0], 0, 0, w, h, drift);
        }
    }

    /** Draws {@code b} cropped to fill the box, zoomed in a little more as {@code drift} goes from 0 to 1. */
    private void cover(Canvas c, Bitmap b, float x, float y, float bw, float bh, float drift) {
        if (b == null) return;
        float s = Math.max(bw / b.getWidth(), bh / b.getHeight()) * (1f + 0.06f * drift);
        float dw = b.getWidth() * s, dh = b.getHeight() * s;
        c.save();
        c.clipRect(x, y, x + bw, y + bh);
        m.setScale(s, s);
        m.postTranslate(x + (bw - dw) / 2f - u(12) * drift, y + (bh - dh) / 2f);
        c.drawBitmap(b, m, paint);
        c.restore();
    }

    private void drawOverlay(Canvas c) {
        float w = getWidth(), h = getHeight();
        scrim.setShader(new LinearGradient(0, h - u(300), 0, h, 0x00000000, 0x8C000000, Shader.TileMode.CLAMP));
        c.drawRect(0, h - u(300), w, h, scrim);

        float x = u(64);
        text.setTypeface(Ui.font(getContext(), 200));
        text.setTextSize(u(88));
        text.setLetterSpacing(-0.045f);
        text.setColor(0xFFFFFFFF);
        text.setShadowLayer(u(16), 0, u(1), 0x59000000);
        float clockY = h - u(52) - u(16) - u(6) - u(13) - u(12);
        c.drawText(Lang.clock(getContext(), new Date()), x, clockY, text);
        text.setTypeface(Ui.font(getContext(), 600));
        text.setTextSize(u(13));
        text.setLetterSpacing(Lang.tracking(getContext(), 0.23f));
        text.setColor(0xCCFFFFFF);
        c.drawText(Lang.date(getContext(), "EEEEdMMMM", new Date()).toUpperCase(Lang.locale(getContext())), x, clockY + u(12) + u(13), text);
        text.setTypeface(Ui.font(getContext(), 400));
        text.setTextSize(u(16));
        text.setLetterSpacing(0);
        text.setColor(0xE6FFFFFF);
        if (weather.ready()) c.drawText(weather.line(), x, clockY + u(12) + u(13) + u(6) + u(16), text);

        if (cur != null) {
            float right = w - u(64);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setTypeface(Ui.font(getContext(), 600));
            text.setTextSize(u(20));
            text.setColor(0xFFFFFFFF);
            String album = Saver.albumName(getContext());
            c.drawText(cur.when.isEmpty() ? album : cur.when, right, h - u(56) - u(20) - u(15) - u(14), text);
            text.setTypeface(Ui.font(getContext(), 400));
            text.setTextSize(u(15));
            text.setColor(0xBFFFFFFF);
            if (!cur.when.isEmpty() && !album.isEmpty()) c.drawText(getContext().getString(R.string.album_label, album), right, h - u(56) - u(20), text);
            text.setTextAlign(Paint.Align.LEFT);
            // five dots, the long one walks along with the photos
            int n = Math.max(1, files.size()), at = cur.at * 5 / n;
            float dx = right, dy = h - u(56) - u(3);
            for (int i = 4; i >= 0; i--) {
                float dw = i == at ? u(18) : u(6);
                paint.setAlpha(255);
                text.setColor(i == at ? 0xF2FFFFFF : 0x66FFFFFF);
                r.set(dx - dw, dy, dx, dy + u(6));
                c.drawRoundRect(r, u(3), u(3), text);
                dx -= dw + u(6);
            }
        }
        text.clearShadowLayer();
    }
}
