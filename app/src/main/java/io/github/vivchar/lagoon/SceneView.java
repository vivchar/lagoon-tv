package io.github.vivchar.lagoon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.InputStream;
import java.util.Calendar;

/**
 * The bay of Kata with Koh Pu behind Home, and the scene's clock. Animated, the clip of the time of day loops in
 * {@link SceneVideo} and this surface is hidden; still, it shows the four painted backdrops (morning, day,
 * sunset, night) themselves, with a half-hour crossfade into the next one as the day goes on. Either way it is
 * the one place that knows which backdrop belongs to this moment: the clips and the blurred glass of the panels
 * ({@link Backdrop}) ask it.
 */
public class SceneView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "LagoonScene";
    static final String[] BACKDROPS = {"morning", "day", "sunset", "night"};
    /** A still picture is drawn again only when the blend changes; the clock is looked at this often. */
    private static final long PICTURE_CHECK_MS = 20_000;

    private volatile boolean running, hasSurface;
    private volatile boolean animated = true;
    private volatile int rise = 6 * 60 + 18, set = 18 * 60 + 12;
    private volatile int forcedMinute = -1;
    private volatile float demoSpeed;
    private final long start = SystemClock.uptimeMillis();
    private HandlerThread renderThread, buildThread;
    private Handler render, build;

    /** The backdrops at the surface's size, decoded once on the build thread. */
    private final Bitmap[] backdrop = new Bitmap[BACKDROPS.length];
    /** The current blend of the backdrops, and the one being swapped in. */
    private Bitmap picture;
    private volatile Bitmap nextPicture;
    private volatile int builtMix = -1;
    private int mixShown = -1;
    private boolean pictureShown;
    private static final Paint SHADE_LEFT = shade(150, 190, 640, 0x70000000), SHADE_RIGHT = shade(1120, 250, 520, 0x4D000000);

    public SceneView(Context context) {
        super(context);
        getHolder().setFixedSize(1280, 720);
        getHolder().addCallback(this);
    }

    // ---------------------------------------------------------------- time of day

    /** Today's sunrise and sunset in minutes since midnight. */
    public void setSun(int riseMin, int setMin) {
        rise = riseMin;
        set = setMin;
        kick();
    }

    /** Pins the scene to a fixed moment (the «Время суток» setting, testing), or -1 to follow the clock. */
    public void setForcedMinute(int m) {
        forcedMinute = m;
        kick();
    }

    /** Runs the day fast: speed × real time. 0 follows the clock. */
    public void setDemoSpeed(float speed) {
        demoSpeed = speed;
        kick();
    }

    /** The clip looping (true), or the backdrops standing still. */
    void setAnimated(boolean a) {
        if (animated == a) return;
        animated = a;
        builtMix = -1;
        if (a) dropPicture();
        kick();
    }

    boolean animated() {
        return animated;
    }

    private float minuteNow() {
        if (demoSpeed > 0) return ((SystemClock.uptimeMillis() - start) / 60000f * demoSpeed + 5 * 60) % 1440;
        if (forcedMinute >= 0) return forcedMinute;
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE) + c.get(Calendar.SECOND) / 60f;
    }

    /** How much of each backdrop belongs to this moment: what the still bay shows and the glass is blurred from. */
    float[] weights() {
        return Palette.backdrops(minuteNow(), rise, set);
    }

    // ---------------------------------------------------------------- lifecycle

    public void setRunning(boolean run) {
        running = run;
        kick();
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        renderThread = new HandlerThread("scene");
        renderThread.start();
        render = new Handler(renderThread.getLooper());
        buildThread = new HandlerThread("scene-build", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        buildThread.start();
        build = new Handler(buildThread.getLooper());
        hasSurface = true;
        pictureShown = false;
        kick();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int format, int w, int hh) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        hasSurface = false;
        for (HandlerThread t : new HandlerThread[]{renderThread, buildThread}) {
            if (t == null) continue;
            t.quitSafely();
            try {
                t.join(800);
            } catch (InterruptedException ignored) {
            }
        }
        renderThread = buildThread = null;
    }

    private void kick() {
        if (render != null && hasSurface) {
            render.removeCallbacks(this);
            render.post(this);
        }
    }

    @Override
    public void run() {
        // animated, the clips play on a surface of their own (SceneVideo): this one is hidden then
        if (!hasSurface || !running || animated) return;
        long now = SystemClock.uptimeMillis();
        float[] w = weights();
        // a new blend each 1/48 of a crossfade: the half hour goes by in steps too small to see
        int mix = 0;
        for (float x : w) mix = mix * 49 + Math.round(x * 48);
        if (mix != builtMix && build != null) {
            builtMix = mix;
            build.removeCallbacksAndMessages(null);
            build.post(() -> {
                Bitmap b = blend(w);
                if (b == null) return;
                Bitmap stale;
                synchronized (this) {
                    stale = nextPicture;
                    nextPicture = b;
                }
                if (stale != null) stale.recycle();
            });
        }
        Bitmap n;
        synchronized (this) {
            n = nextPicture;
            nextPicture = null;
        }
        if (n != null) {
            if (picture != null) picture.recycle();
            picture = n;
            pictureShown = false;
        }
        // a new surface starts empty: draw what there is again after kick()
        if (picture != null && !pictureShown) {
            Canvas c = getHolder().lockHardwareCanvas();
            if (c != null) {
                pictureShown = true;
                try {
                    c.drawBitmap(picture, 0, 0, null);
                } finally {
                    getHolder().unlockCanvasAndPost(c);
                }
            }
        }
        // until the blend is ready, look again soon; after that only when the time of day may have moved it
        long frame = picture == null || nextPicture != null || builtMix != mixShown ? 200 : PICTURE_CHECK_MS;
        mixShown = builtMix;
        render.postAtTime(this, now + frame);
    }

    // ---------------------------------------------------------------- the painted backdrops

    /**
     * A soft shade under the clock and under the notes, in 1280×720 units: the backdrops and the clips are bright
     * there. Drawn by the screen above the scene, over the pictures and the video.
     */
    static void shade(Canvas c) {
        c.drawRect(0, 0, 1280, 720, SHADE_LEFT);
        c.drawRect(0, 0, 1280, 720, SHADE_RIGHT);
    }

    private static Paint shade(float x, float y, float r, int color) {
        Paint p = new Paint();
        RadialGradient g = new RadialGradient(0, 0, r, new int[]{color, color & 0x80FFFFFF | ((color >>> 25) << 24), 0},
                new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP);
        Matrix m = new Matrix();
        m.setScale(1.35f, 1);
        m.postTranslate(x, y);
        g.setLocalMatrix(m);
        p.setShader(g);
        return p;
    }

    /** The backdrops mixed by their weights into one bitmap: each drawn at its share of what is under it. */
    private Bitmap blend(float[] w) {
        Bitmap out = null;
        Canvas c = null;
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        float acc = 0;
        for (int i = 0; i < BACKDROPS.length; i++) {
            if (w[i] <= 0.001f) continue;
            Bitmap b = backdrop(i);
            if (b == null) continue;
            if (out == null) {
                out = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
                c = new Canvas(out);
            }
            acc += w[i];
            paint.setAlpha(Math.round(255 * w[i] / acc));
            c.drawBitmap(b, 0, 0, paint);
        }
        return out;
    }

    private Bitmap backdrop(int i) {
        if (backdrop[i] != null) return backdrop[i];
        try (InputStream in = getContext().getAssets().open("scene/" + BACKDROPS[i] + ".webp")) {
            Bitmap src = BitmapFactory.decodeStream(in);
            if (src == null) return null;
            backdrop[i] = src.getWidth() == 1280 ? src : Bitmap.createScaledBitmap(src, 1280, 720, true);
            if (backdrop[i] != src) src.recycle();
        } catch (Exception e) {
            Log.w(TAG, "backdrop " + BACKDROPS[i], e);
        }
        return backdrop[i];
    }

    /** Back to the clips: the pictures give their memory back. */
    private void dropPicture() {
        Handler b = build;
        Runnable drop = () -> {
            synchronized (this) {
                if (nextPicture != null) nextPicture.recycle();
                nextPicture = null;
            }
            for (int i = 0; i < backdrop.length; i++) {
                if (backdrop[i] != null) backdrop[i].recycle();
                backdrop[i] = null;
            }
        };
        if (b != null) b.post(drop);
        Handler r = render;
        // the shown picture belongs to the render thread
        if (r != null) r.post(() -> {
            if (picture != null) picture.recycle();
            picture = null;
            pictureShown = false;
        });
    }

    // ---------------------------------------------------------------- the clips

    /** Which backdrop's clip belongs to this moment: the one with the most weight. Played by {@link SceneVideo}. */
    int clipNow() {
        float[] w = weights();
        int best = 0;
        for (int i = 1; i < w.length; i++) if (w[i] > w[best]) best = i;
        return best;
    }

    static String clipName(int i) {
        return "scene/" + BACKDROPS[i] + ".mp4";
    }
}
