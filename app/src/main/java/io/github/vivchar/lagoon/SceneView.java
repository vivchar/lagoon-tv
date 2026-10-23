package io.github.vivchar.lagoon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * The bay of Kata with Koh Pu, drawn live in a 1280×720 surface that the display hardware scales to the
 * screen. It follows the painted backdrops (assets/scene) shape for shape: the forested headlands on both
 * sides, Koh Pu in the middle, the beach running in from the bottom-left with its bushes and two palms, and
 * fronds hanging in from the top-right corner. Everything still is painted into bitmaps by a background
 * builder for the current light; every frame only moves things: the sun or moon, clouds, stars, the sea and
 * the surf, the longtail boat, birds and the swaying fronds. The light is rebuilt as the day goes on, so it
 * changes smoothly with the real time of day.
 * <p>
 * The other styles: {@link #PICTURES} shows the four backdrops themselves (morning, day, sunset, night) and
 * nothing else, no motion, only a half-hour crossfade into the next one as the day goes on; {@link #VIDEO}
 * loops a short clip made from each backdrop instead, in {@link SceneVideo}. Which one stays is still to be decided.
 */
public class SceneView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "LagoonScene";
    private static final float W = 1280f, H = 720f;
    /** The horizon, where it is in the backdrops. */
    private static final float SEA = 443;
    // still layers are cropped to where they paint: the hills start below 290, the sand below 470, and the
    // bushes with the two palm trunks on the left fit in FRONT_W × (720 - FRONT_TOP)
    private static final int LAND_TOP = 290, SAND_TOP = 470, FRONT_TOP = 280, FRONT_W = 260;
    private static final long FRAME_MS = 40, FRAME_IDLE_MS = 100, IDLE_MS = 60_000;
    private volatile long lastInput = SystemClock.uptimeMillis();

    // The land, traced from the backdrops as x, y pairs: the far hazy hill, the headlands left and right, Koh Pu.
    private static final float[] FAR = {795, 444, 820, 432, 850, 424, 880, 414, 910, 405, 940, 399, 970, 402, 1000, 412};
    private static final float[] RIGHT = {900, 444, 930, 421, 955, 396, 980, 381, 1005, 371, 1030, 359, 1055, 346, 1080, 337,
            1110, 331, 1140, 326, 1170, 327, 1200, 325, 1230, 329, 1260, 334, 1290, 339};
    private static final float[] LEFT = {-10, 312, 20, 314, 50, 318, 80, 326, 110, 338, 140, 350, 170, 357, 200, 362, 225, 368,
            250, 373, 270, 371, 295, 368, 320, 374, 345, 380, 370, 387, 395, 396, 420, 401, 445, 405, 470, 413, 490, 422,
            505, 432, 518, 444};
    private static final float[] ISLAND = {560, 444, 575, 437, 600, 423, 612, 406, 628, 393, 645, 386, 665, 381, 690, 380,
            710, 384, 730, 392, 750, 404, 770, 415, 790, 426, 812, 436, 832, 444};
    /** Where the dry sand starts, every 100 px from x = 0: the beach runs in from the bottom-left. */
    private static final float[] SHORE = {505, 525, 550, 576, 598, 615, 629, 642, 654, 664, 673, 683, 692, 700};
    /** The bushes at the bottom-left, as outlines. */
    private static final float[] BUSH = {0, 438, 30, 446, 60, 462, 95, 484, 130, 508, 165, 538, 200, 566, 228, 590, 205, 597,
            150, 589, 100, 585, 60, 591, 30, 600, 0, 602};
    private static final float[] SHRUB = {0, 574, 40, 584, 75, 600, 100, 624, 80, 640, 40, 643, 0, 641};
    /** Lights in the houses on the hills at night, and the bands of morning mist (centre x, y, radii). */
    private static final float[] LIGHTS = {1078, 425, 1092, 419, 1121, 428, 1180, 403, 1197, 399, 1214, 405, 1240, 411,
            1255, 401, 1263, 421, 172, 431, 181, 432, 76, 434, 300, 436};
    private static final float[][] MIST = {{260, 392, 280, 10}, {430, 416, 160, 7}, {1150, 370, 210, 10}, {960, 408, 170, 8}, {700, 404, 90, 6}};

    /** Heaps of cumulus: centre x, flat base y, width, height. */
    private static final float[][] CUMULUS = {{1160, 382, 300, 205}, {930, 386, 300, 145}, {760, 386, 160, 75}, {550, 386, 150, 50}};
    /** Thin streaks: centre x, y, width, height, and 1 for the wisp that stays even on a clear day. */
    private static final float[][] STREAKS = {{700, 300, 150, 10, 1}, {150, 170, 340, 50, 0}, {330, 262, 480, 46, 0},
            {470, 205, 220, 18, 0}, {880, 212, 220, 26, 0}, {760, 150, 160, 14, 0}, {60, 246, 200, 30, 0}};
    private static final int CLOUDS = CUMULUS.length + STREAKS.length;

    // palms on the left: base x, base y, crown x, crown y, trunk scale, crown scale, seed
    private static final float[][] PALMS = {
            {16, 600, 97, 302, 0.42f, 0.52f, 0},
            {-6, 470, 22, 366, 0.3f, 0.36f, 3}
    };

    // crown: angle, length, droop, dead
    private static final float[][] CROWN = {
            {2.25f, 0.62f, 2.1f, 1}, {0.95f, 0.55f, 2.2f, 1},
            {2.75f, 0.85f, 1.5f, 0}, {0.4f, 0.85f, 1.5f, 0},
            {3.1f, 1.0f, 1.15f, 0}, {0.05f, 1.0f, 1.15f, 0},
            {-2.85f, 1.0f, 0.85f, 0}, {-0.3f, 1.0f, 0.85f, 0},
            {-2.45f, 0.92f, 0.55f, 0}, {-0.75f, 0.92f, 0.55f, 0},
            {-2.05f, 0.72f, 0.3f, 0}, {-1.15f, 0.72f, 0.3f, 0},
            {-1.62f, 0.5f, 0.1f, 0}
    };
    /** A palm close to the camera, out of the frame at the top-right: only its long fronds hang in. */
    private static final float[] CORNER_AT = {1335, -45, 1.9f};
    private static final float[][] CORNER = {
            {2.98f, 0.95f, 0.2f}, {2.75f, 1.32f, 0.3f}, {2.52f, 1.2f, 0.4f}, {2.2f, 1.05f, 0.42f}, {1.92f, 0.9f, 0.38f}, {1.66f, 0.72f, 0.3f}
    };

    // fixed randomness, shared read-only by the builder and the renderer
    private final float[][] stars = new float[170][5];
    /** Each cloud drifts to and fro around where it is in the backdrops: period in seconds, phase, reach. */
    private final float[][] cloudDrift = new float[CLOUDS][3];
    private final float[][] sandDots = new float[500][3];
    private final float[][][] waveRows = new float[70][][];
    /** The moon's seas: centre x, y and radii, in moon radii. */
    private static final float[][] MARIA = {
            {-0.25f, -0.35f, 0.32f, 0.22f}, {0.15f, -0.3f, 0.22f, 0.18f}, {0.3f, 0.05f, 0.2f, 0.26f}, {-0.1f, 0.1f, 0.18f, 0.14f}, {-0.35f, 0.3f, 0.2f, 0.12f}
    };
    private final Bitmap grain;

    /** Everything painted for one light. Built off the render thread, then swapped in. */
    private static final class Layers {
        Palette p;
        Bitmap sky, land, sand, front, column;
        Bitmap[] clouds;
        /** Where each cloud's bitmap sits when it has not drifted: left, top. */
        float[][] cloudAt;
        float[] cloudAlpha;
        final List<Frond> fronds = new ArrayList<>();
        RadialGradient glow;
        Bitmap sun, moon;

        void recycle() {
            for (Bitmap b : new Bitmap[]{sky, land, sand, front, column, sun, moon}) if (b != null) b.recycle();
            if (clouds != null) for (Bitmap b : clouds) if (b != null) b.recycle();
            for (Frond f : fronds) f.bm.recycle();
        }
    }

    private static final class Frond {
        Bitmap bm;
        float cx, cy, ox, oy, w, h, sway = 1;
        int k;
    }

    private volatile Layers cur, next;
    private HandlerThread renderThread, buildThread;
    private Handler render, build;
    private volatile boolean hasSurface, running;
    private final long start = SystemClock.uptimeMillis();

    // time of day
    private volatile int rise = 6 * 60 + 18, set = 18 * 60 + 12;
    private volatile int forcedMinute = -1;
    private volatile float demoSpeed = 0;
    private volatile int builtMinute = -999;

    // renderer's own tools
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lace = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private long statFrom = SystemClock.uptimeMillis(), drawNs;
    private int frames;

    public SceneView(Context context) {
        super(context);
        getHolder().setFixedSize(1280, 720);
        getHolder().addCallback(this);
        Random r = new Random(11);
        for (float[] s : stars) {
            s[0] = r.nextFloat() * 1280; s[1] = r.nextFloat() * 380; s[2] = 0.4f + r.nextFloat() * 1.4f;
            s[3] = r.nextFloat() * 6.28f; s[4] = 0.6f + r.nextFloat() * 2;
        }
        for (float[] d : cloudDrift) {
            d[0] = 900 + r.nextFloat() * 700; d[1] = r.nextFloat() * 6.28f; d[2] = 30 + r.nextFloat() * 40;
        }
        for (float[] d : sandDots) { d[0] = r.nextFloat() * 1280; d[1] = 500 + r.nextFloat() * 220; d[2] = r.nextFloat(); }
        for (int i = 0; i < waveRows.length; i++) {
            float[][] row = new float[14 + i][2];
            for (float[] d : row) { d[0] = r.nextFloat() * 1400; d[1] = 0.5f + r.nextFloat(); }
            waveRows[i] = row;
        }
        int[] px = new int[160 * 160];
        for (int i = 0; i < px.length; i++) { int v = r.nextInt(256); px[i] = Color.rgb(v, v, v); }
        grain = Bitmap.createBitmap(px, 160, 160, Bitmap.Config.ARGB_8888);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        lace.setStyle(Paint.Style.STROKE);
        lace.setStrokeCap(Paint.Cap.ROUND);
        lace.setPathEffect(new DashPathEffect(new float[]{7, 5, 14, 6, 4, 7}, 0));
    }

    // ---------------------------------------------------------------- time of day

    /** Today's sunrise and sunset in minutes since midnight. */
    public void setSun(int riseMin, int setMin) {
        if (riseMin == rise && setMin == set) return;
        rise = riseMin;
        set = setMin;
        builtMinute = -999;
    }

    /** Pins the scene to a fixed moment (testing), or -1 to follow the clock. */
    public void setForcedMinute(int m) {
        forcedMinute = m;
        builtMinute = -999;
    }

    /** How alive the bay is: everything moves; it calms down when nobody touches the remote; it stands still. */
    static final int LIVELY = 0, CALM = 1, STILL = 2;
    private volatile int motion = CALM;
    private volatile boolean boat = true, birds = true, clouds = true, showStars = true;
    private final float stillT = 37f;

    void setMotion(int m) {
        motion = m;
        kick();
    }

    /** What else lives in the bay besides the sea and the palms. */
    void setShown(boolean boat, boolean birds, boolean clouds, boolean stars) {
        this.boat = boat;
        this.birds = birds;
        this.clouds = clouds;
        this.showStars = stars;
        kick();
    }

    /** The bay drawn by code, the painted backdrops, or the clips made from them. */
    static final int PAINTED = 0, PICTURES = 1, VIDEO = 2;
    private volatile int style = VIDEO;

    void setStyle(int s) {
        if (style == s) return;
        style = s;
        builtMinute = -999;
        builtMix = -1;
        kick();
    }

    /** Runs the day fast: speed × real time. 0 follows the clock. */
    public void setDemoSpeed(float speed) {
        demoSpeed = speed;
        builtMinute = -999;
    }

    private float minuteNow() {
        if (demoSpeed > 0) return ((SystemClock.uptimeMillis() - start) / 60000f * demoSpeed + 5 * 60) % 1440;
        if (forcedMinute >= 0) return forcedMinute;
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE) + c.get(Calendar.SECOND) / 60f;
    }

    public Palette.Phase phase() {
        Layers l = cur;
        return l == null ? Palette.at(Math.round(minuteNow()), rise, set).phase : l.p.phase;
    }

    // ---------------------------------------------------------------- lifecycle

    /** Called on every remote key: back to full frame rate. */
    public void poke() {
        lastInput = SystemClock.uptimeMillis();
    }

    public void setRunning(boolean run) {
        if (run) poke();
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
        builtMinute = -999;
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
        if (!hasSurface) return;
        long now = SystemClock.uptimeMillis();
        float minute = minuteNow();
        // the clips play on a surface of their own (SceneVideo): this one is hidden then
        if (style == VIDEO) return;
        if (style == PICTURES) {
            runPictures(now, minute);
            return;
        }
        dropPicture();
        int step = demoSpeed > 0 ? 6 : 5;
        if (Math.abs(minute - builtMinute) >= step && build != null) {
            builtMinute = Math.round(minute);
            int m = builtMinute;
            build.removeCallbacksAndMessages(null);
            build.post(() -> {
                long t0 = SystemClock.uptimeMillis();
                Layers built = buildLayers(Palette.at(m, rise, set), m);
                Log.i(TAG, "built light for minute " + m + " in " + (SystemClock.uptimeMillis() - t0) + " ms");
                Layers stale;
                synchronized (this) {
                    stale = next;
                    next = built;
                }
                // a build the renderer never picked up must still give its memory back
                if (stale != null) stale.recycle();
            });
        }
        Layers n;
        synchronized (this) {
            n = next;
            next = null;
        }
        if (n != null) {
            Layers old = cur;
            cur = n;
            if (old != null) old.recycle();
        }
        if (cur != null) {
            Canvas c = getHolder().lockHardwareCanvas();
            if (c != null) {
                try {
                    drawFrame(c, cur, minute);
                } finally {
                    getHolder().unlockCanvasAndPost(c);
                }
            }
        }
        // nobody touched the remote for a while: the bay keeps moving, at a calmer frame rate
        long frame = motion == STILL ? 30_000 : motion == LIVELY || now - lastInput <= IDLE_MS ? FRAME_MS : FRAME_IDLE_MS;
        if (running && hasSurface) render.postAtTime(this, now + frame);
    }

    // ---------------------------------------------------------------- building a light

    private Layers buildLayers(Palette p, int minute) {
        Builder b = new Builder(p);
        Layers l = new Layers();
        l.p = p;
        l.sky = Bitmap.createBitmap(1280, 446, Bitmap.Config.RGB_565);
        b.sky(new Canvas(l.sky));
        l.land = Bitmap.createBitmap(1280, 720 - LAND_TOP, Bitmap.Config.ARGB_8888);
        Canvas lc = new Canvas(l.land);
        lc.translate(0, -LAND_TOP);
        b.land(lc);
        l.sand = Bitmap.createBitmap(1280, 720 - SAND_TOP, Bitmap.Config.ARGB_8888);
        Canvas sc = new Canvas(l.sand);
        sc.translate(0, -SAND_TOP);
        b.sand(sc);
        l.front = Bitmap.createBitmap(FRONT_W, 720 - FRONT_TOP, Bitmap.Config.ARGB_8888);
        Canvas fc = new Canvas(l.front);
        fc.translate(0, -FRONT_TOP);
        b.front(fc);
        l.clouds = new Bitmap[CLOUDS];
        l.cloudAt = new float[CLOUDS][2];
        l.cloudAlpha = new float[CLOUDS];
        for (int i = 0; i < CUMULUS.length; i++) {
            l.clouds[i] = b.cumulus(CUMULUS[i], 40 + i, l.cloudAt[i]);
            l.cloudAlpha[i] = 1;
        }
        for (int i = 0; i < STREAKS.length; i++) {
            int k = CUMULUS.length + i;
            l.cloudAlpha[k] = Math.max(STREAKS[i][4], p.streaks);
            if (l.cloudAlpha[k] > 0.02f) l.clouds[k] = b.streak(STREAKS[i], 60 + i, l.cloudAt[k]);
        }
        for (float[] pm : PALMS) b.palm(l.fronds, pm[2], pm[3], pm[5], (int) pm[6]);
        for (int i = 0; i < CORNER.length; i++) {
            float[] f = CORNER[i];
            float s = CORNER_AT[2];
            Frond fr = b.frond(CORNER_AT[0], CORNER_AT[1], f[0], f[1] * 175 * s, f[2], 20 + i, s, false, 0.27f);
            fr.sway = 0.45f;
            l.fronds.add(fr);
        }
        float[] sun = sunAt(minute);
        float low = low(sun[1]);
        l.glow = new RadialGradient(0, 0, p.gr, new int[]{Palette.alpha(p.glow, 0.55f + 0.15f * low), Palette.alpha(p.glow, 0.22f + 0.1f * low),
                Palette.alpha(p.glow, 0.07f), Palette.alpha(p.glow, 0)}, new float[]{0, 0.18f, 0.5f, 1}, Shader.TileMode.CLAMP);
        l.sun = b.sun(low);
        l.moon = b.moon();
        l.column = b.column(sun[2] > 0 ? Palette.mix(p.glow, p.sun, 0.4f) : p.glint);
        return l;
    }

    /** Paints the still layers for one palette. Has its own paints, so it can run beside the renderer. */
    private final class Builder {
        final Palette p;
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final Paint vignette = new Paint();

        Builder(Palette p) {
            this.p = p;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            vignette.setShader(new RadialGradient(640, 360, 820, new int[]{0x00000000, 0x00000000, 0x26000000},
                    new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        }

        /**
         * The sun as light, not a sticker: a bloom that burns out the disc's edge and a white-hot core. Low over
         * the sea (low = 1) it turns warmer and a little flattened. The wide glare is drawn per frame from the glow.
         */
        Bitmap sun(float low) {
            float r = p.sr, h = (float) Math.ceil(r * 2.6f) + 2;
            Bitmap bm = Bitmap.createBitmap((int) (h * 2), (int) (h * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            c.translate(h, h);
            Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
            g.setShader(new RadialGradient(0, 0, r * 2.6f, new int[]{0xD9FFFDF0, 0xD9FFFDF0, Palette.alpha(p.sun, 0.35f), Palette.alpha(p.sun, 0)},
                    new float[]{0, 0.23f, 0.5f, 1}, Shader.TileMode.CLAMP));
            c.drawCircle(0, 0, r * 2.6f, g);
            c.scale(1, 1 - 0.07f * low);
            g.setShader(new RadialGradient(0, 0, r, new int[]{Color.WHITE, Palette.mix(0xFFFFF8, p.sun, low),
                    Palette.alpha(Palette.mix(0xFFF6D8, p.glow, low), 1 - 0.05f * low)}, new float[]{0, 0.7f, 1}, Shader.TileMode.CLAMP));
            g.setMaskFilter(new BlurMaskFilter(r * 0.06f, BlurMaskFilter.Blur.NORMAL));
            c.drawCircle(0, 0, r, g);
            return bm;
        }

        /** A waxing gibbous moon: a faint cool halo, a warm grey disc with its seas, the unlit edge fading into the sky. */
        Bitmap moon() {
            float r = p.sr, h = (float) Math.ceil(r * 5) + 2;
            Bitmap bm = Bitmap.createBitmap((int) (h * 2), (int) (h * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            c.translate(h, h);
            Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
            g.setShader(new RadialGradient(0, 0, r * 5, new int[]{0x33E1E8FF, 0x33E1E8FF, 0x0FC8D7FF, 0x00C8D7FF},
                    new float[]{0, 0.18f, 0.47f, 1}, Shader.TileMode.CLAMP));
            c.drawCircle(0, 0, r * 5, g);
            Path disc = new Path();
            disc.addCircle(0, 0, r, Path.Direction.CW);
            c.clipPath(disc);
            g.setShader(new RadialGradient(-r * 0.25f, -r * 0.3f, r, new int[]{0xFFF4F1E6, 0xFFE3DECF, 0xFFCFC9B8},
                    new float[]{0, 0.8f, 1}, Shader.TileMode.CLAMP));
            c.drawRect(-r, -r, r, r, g);
            // the seas: soft grey patches where the real ones are
            g.setShader(null);
            g.setColor(0x528C8A80);
            RectF o = new RectF();
            for (float[] m : MARIA) {
                c.save();
                c.rotate(23, m[0] * r, m[1] * r);
                o.set((m[0] - m[2]) * r, (m[1] - m[3]) * r, (m[0] + m[2]) * r, (m[1] + m[3]) * r);
                c.drawOval(o, g);
                c.restore();
            }
            // night side on the left, 80% lit: the terminator is half an ellipse, blurred so the edge is not cut
            g.setColor(0xED081028);
            g.setMaskFilter(new BlurMaskFilter(r * 0.08f, BlurMaskFilter.Blur.NORMAL));
            Path dark = new Path();
            o.set(-r * 1.05f, -r * 1.05f, r * 1.05f, r * 1.05f);
            dark.arcTo(o, -90, -180, true);
            o.set(-r * 0.6f, -r * 1.05f, r * 0.56f, r * 1.05f);
            dark.arcTo(o, 90, 180, false);
            dark.close();
            c.drawPath(dark, g);
            return bm;
        }

        /** The path of light on the water under a low sun or the moon: soft at the sides, fading towards the beach. */
        Bitmap column(int color) {
            Bitmap bm = Bitmap.createBitmap(160, 270, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            Paint g = new Paint();
            g.setShader(new LinearGradient(0, 0, 160, 0, new int[]{Palette.alpha(color, 0), Palette.alpha(color, 0.5f), Palette.alpha(color, 0.5f), Palette.alpha(color, 0)},
                    new float[]{0, 0.4f, 0.6f, 1}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 160, 270, g);
            g.setShader(new LinearGradient(0, 0, 0, 270, 0xFF000000, 0x00000000, Shader.TileMode.CLAMP));
            g.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            c.drawRect(0, 0, 160, 270, g);
            return bm;
        }

        void sky(Canvas c) {
            float[] at = new float[Palette.SKY_AT.length];
            for (int i = 0; i < at.length; i++) at[i] = Palette.SKY_AT[i] / SEA;
            fill.setShader(new LinearGradient(0, 0, 0, SEA, p.sky, at, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, 446, fill);
            // the sky is lighter and warmer low on the right, where the sun goes down
            RadialGradient warm = new RadialGradient(0, 0, 1, new int[]{Palette.alpha(p.skyWarm, 0.75f), Palette.alpha(p.skyWarm, 0.3f),
                    Palette.alpha(p.skyWarm, 0)}, new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP);
            Matrix m = new Matrix();
            m.setScale(760, 420);
            m.postTranslate(1000, 440);
            warm.setLocalMatrix(m);
            fill.setShader(warm);
            c.drawRect(0, 0, W, 446, fill);
            fill.setShader(null);
            grain(c, 0, 446);
            c.drawRect(0, 0, W, 446, vignette);
        }

        void land(Canvas c) {
            sea(c);
            forest(c, FAR, Palette.mix(p.far, p.hillDark, 0.25f), p.far, Palette.mix(p.far, p.hillLit, 0.35f), 3.5f, 1, 0.8f);
            forest(c, RIGHT, p.hillDark, p.hillMid, p.hillLit, 7, 2, 1);
            forest(c, LEFT, p.hillDark, p.hillMid, p.hillLit, 7, 3, 1);
            // the far beach under the left headland, with a few houses
            fill.setColor(Palette.mix(p.sand1, p.haze, 0.35f));
            path.reset();
            path.moveTo(28, SEA + 1);
            path.lineTo(40, SEA - 5);
            path.lineTo(268, SEA - 4);
            path.lineTo(280, SEA + 1);
            path.close();
            c.drawPath(path, fill);
            int wall = Palette.mix(0xFFF2EEE6, p.hillDark, 0.25f + p.night * 0.6f), roof = Palette.mix(0xFFB0584A, p.hillDark, 0.25f + p.night * 0.6f);
            for (float[] hs : new float[][]{{160, 424, 24, 8}, {70, 430, 12, 6}, {294, 432, 10, 5}}) {
                fill.setColor(wall);
                c.drawRect(hs[0], hs[1], hs[0] + hs[2], hs[1] + hs[3], fill);
                fill.setColor(roof);
                path.reset();
                path.moveTo(hs[0] - 2, hs[1] + 1);
                path.lineTo(hs[0] + hs[2] / 2, hs[1] - hs[3] * 0.7f);
                path.lineTo(hs[0] + hs[2] + 2, hs[1] + 1);
                path.close();
                c.drawPath(path, fill);
            }
            forest(c, ISLAND, p.islandDark, Palette.mix(p.islandDark, p.islandLit, 0.5f), p.islandLit, 7.5f, 4, 0.35f);
            rocks(c);
            if (p.mist > 0.01f) {
                fill.setMaskFilter(new BlurMaskFilter(12, BlurMaskFilter.Blur.NORMAL));
                fill.setColor(Palette.alpha(Palette.mix(p.haze, Color.WHITE, 0.3f), p.mist * 0.6f));
                RectF o = new RectF();
                for (float[] m : MIST) {
                    o.set(m[0] - m[2], m[1] - m[3], m[0] + m[2], m[1] + m[3]);
                    c.drawOval(o, fill);
                }
                fill.setMaskFilter(null);
            }
            if (p.night > 0.3f) {
                float a = (p.night - 0.3f) / 0.7f;
                for (int i = 0; i < LIGHTS.length; i += 2) {
                    fill.setColor(Palette.alpha(0xFFFFC46A, 0.3f * a));
                    c.drawCircle(LIGHTS[i], LIGHTS[i + 1], 4.5f, fill);
                    fill.setColor(Palette.alpha(0xFFFFE2A8, a));
                    c.drawCircle(LIGHTS[i], LIGHTS[i + 1], 1.4f, fill);
                }
            }
            grain(c, LAND_TOP, 720);
            c.drawRect(0, SEA, W, 720, vignette);
        }

        /** The sea under the hills: deep blue at the horizon, painted ripples, the turquoise shallows along the beach. */
        void sea(Canvas c) {
            fill.setShader(new LinearGradient(0, SEA, 0, 700, new int[]{p.sea[0], p.sea[1], p.sea[2]}, new float[]{0, 0.3f, 1}, Shader.TileMode.CLAMP));
            c.drawRect(0, SEA, W, H, fill);
            fill.setShader(new LinearGradient(0, SEA, 0, SEA + 14, Palette.alpha(Palette.mix(p.sea[0], Color.BLACK, 0.25f), 0.6f),
                    Palette.alpha(p.sea[0], 0), Shader.TileMode.CLAMP));
            c.drawRect(0, SEA, W, SEA + 14, fill);
            fill.setShader(null);
            Random r = new Random(5);
            int dark = Palette.mix(p.sea[0], Color.BLACK, 0.3f), light = Palette.mix(p.sea[1], Color.WHITE, 0.35f);
            for (int i = 0; i < 1100; i++) {
                float kk = (float) Math.pow(r.nextFloat(), 1.5);
                float y = SEA + 3 + kk * 260, x = r.nextFloat() * 1300 - 10, len = (5 + kk * 40) * (0.5f + r.nextFloat());
                boolean lit = r.nextBoolean();
                stroke.setStrokeWidth(0.8f + kk * 2.2f);
                stroke.setColor(lit ? Palette.alpha(light, 0.16f + 0.1f * kk) : Palette.alpha(dark, 0.22f));
                c.drawLine(x, y, x + len, y, stroke);
            }
            stroke.setMaskFilter(new BlurMaskFilter(18, BlurMaskFilter.Blur.NORMAL));
            stroke.setStrokeWidth(70);
            stroke.setColor(Palette.alpha(p.sea[3], 0.85f));
            c.drawPath(along(-30, 0), stroke);
            // the swell before it breaks: a darker trough with a light crest
            stroke.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));
            stroke.setStrokeWidth(7);
            stroke.setColor(Palette.alpha(Palette.mix(p.sea[2], Color.BLACK, 0.2f), 0.4f));
            c.drawPath(along(-58, 3), stroke);
            stroke.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.NORMAL));
            stroke.setStrokeWidth(2);
            stroke.setColor(Palette.alpha(p.foam, 0.3f));
            c.drawPath(along(-62, 3), stroke);
            stroke.setMaskFilter(null);
        }

        /** A line along the shore, dy from it, with a slow wobble. */
        Path along(float dy, float wobble) {
            Path a = new Path();
            for (int x = -20; x <= 1300; x += 10) {
                float y = shore(x) + dy + (float) Math.sin(x / 80.0) * wobble;
                if (x == -20) a.moveTo(x, y); else a.lineTo(x, y);
            }
            return a;
        }

        /**
         * A forested hill: its outline in shadow, then crowns of trees from the back to the front, each with a
         * darker side and a lit top, and a row of them along the ridge so the skyline is made of treetops. Haze
         * gathers towards the water.
         */
        void forest(Canvas c, float[] pts, int dark, int mid, int lit, float size, long seed, float haze) {
            float x0 = pts[0], x1 = pts[pts.length - 2], top = SEA;
            for (int i = 1; i < pts.length; i += 2) top = Math.min(top, pts[i]);
            path.reset();
            path.moveTo(x0, SEA + 1);
            for (int i = 0; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
            path.lineTo(x1, SEA + 1);
            path.close();
            c.saveLayer(x0 - size * 2, top - size * 2, x1 + size * 2, SEA + 1, null);
            c.clipRect(x0 - size * 2, 0, x1 + size * 2, SEA + 1);
            fill.setColor(dark);
            c.drawPath(path, fill);
            Random r = new Random(seed);
            List<float[]> crowns = new ArrayList<>();
            for (float x = x0 + size; x < x1 - size * 0.5f; x += size * 0.9f) {
                float rad = size * (0.8f + r.nextFloat() * 0.5f);
                crowns.add(new float[]{x, ridgeY(pts, x) + rad * 0.45f, rad, 0.75f + r.nextFloat() * 0.25f});
            }
            float area = 0;
            for (float x = x0; x < x1; x += 4) area += (SEA - ridgeY(pts, x)) * 4;
            int n = (int) (area / (size * size * 1.3f));
            for (int i = 0; i < n; i++) {
                float x = x0 + r.nextFloat() * (x1 - x0), ry = ridgeY(pts, x);
                float y = ry + size * 0.5f + r.nextFloat() * (SEA - ry);
                float depth = (y - ry) / (SEA - ry + 1);
                crowns.add(new float[]{x, y, size * (0.7f + r.nextFloat() * 0.6f), 0.75f - depth * 0.6f + (r.nextFloat() - 0.5f) * 0.5f});
            }
            crowns.sort((a, b) -> Float.compare(a[1], b[1]));
            for (float[] cr : crowns) {
                float x = cr[0], y = cr[1], rad = cr[2], light = Math.max(0, Math.min(1, cr[3]));
                int body = light < 0.5f ? Palette.mix(dark, mid, light * 2) : Palette.mix(mid, lit, (light - 0.5f) * 2);
                fill.setColor(Palette.alpha(dark, 0.55f));
                c.drawCircle(x + rad * 0.2f, y + rad * 0.3f, rad, fill);
                fill.setColor(body);
                c.drawCircle(x, y, rad, fill);
                fill.setColor(Palette.alpha(Palette.mix(body, lit, 0.55f), 0.85f));
                c.drawCircle(x - rad * 0.22f, y - rad * 0.3f, rad * 0.55f, fill);
            }
            fill.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            fill.setShader(new LinearGradient(0, top, 0, SEA, Palette.alpha(p.haze, p.hazeA * haze * 0.35f),
                    Palette.alpha(p.haze, p.hazeA * haze), Shader.TileMode.CLAMP));
            c.drawRect(x0 - size * 2, top - size * 2, x1 + size * 2, SEA + 1, fill);
            fill.setShader(null);
            fill.setXfermode(null);
            c.restore();
        }

        /** Boulders where the hills and Koh Pu meet the water, lit on top. */
        void rocks(Canvas c) {
            float[][] runs = {{290, 520, 1}, {562, 832, 1.3f}, {840, 905, 0.7f}, {1000, 1285, 1}};
            Random r = new Random(9);
            RectF o = new RectF();
            for (float[] run : runs) {
                float s = run[2];
                for (float x = run[0]; x < run[1]; x += (6 + r.nextFloat() * 14) * s) {
                    float w = (7 + r.nextFloat() * 10) * s, h = w * (0.45f + r.nextFloat() * 0.25f), y = SEA + 1;
                    fill.setColor(p.rock);
                    o.set(x - w / 2, y - h, x + w / 2, y + h * 0.3f);
                    c.drawOval(o, fill);
                    fill.setColor(Palette.alpha(p.rockLit, 0.85f));
                    o.set(x - w * 0.38f, y - h * 0.95f, x + w * 0.25f, y - h * 0.35f);
                    c.drawOval(o, fill);
                }
            }
        }

        void sand(Canvas c) {
            Path beach = new Path();
            beach.moveTo(0, 720);
            for (int x = 0; x <= 1280; x += 10) beach.lineTo(x, shore(x) + 8);
            beach.lineTo(1280, 720);
            beach.close();
            fill.setShader(new LinearGradient(0, 520, 0, 720, p.sand0, p.sand1, Shader.TileMode.CLAMP));
            c.drawPath(beach, fill);
            fill.setShader(null);
            c.save();
            c.clipPath(beach);
            // the shade of the bushes and of the palms' fronds
            fill.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));
            fill.setColor(Palette.alpha(p.shade, 0.55f));
            c.drawOval(new RectF(-20, 584, 240, 614), fill);
            fill.setMaskFilter(new BlurMaskFilter(2.5f, BlurMaskFilter.Blur.NORMAL));
            fill.setColor(Palette.alpha(p.shade, 0.5f));
            Random r = new Random(13);
            RectF o = new RectF();
            for (int i = 0; i < 90; i++) {
                float x = r.nextFloat() * 700 - 20, y = 600 + r.nextFloat() * 125;
                float rx = 8 + r.nextFloat() * 34, ry = 2 + r.nextFloat() * 5;
                c.save();
                c.rotate(-14 + r.nextFloat() * 18, x, y);
                o.set(x - rx, y - ry, x + rx, y + ry);
                c.drawOval(o, fill);
                c.restore();
            }
            fill.setMaskFilter(null);
            for (float[] d : sandDots) {
                if (d[1] < shore(d[0]) + 12) continue;
                fill.setColor(d[2] > 0.5f ? 0x1AFFFFFF : 0x12000000);
                c.drawRect(d[0], d[1], d[0] + 1.5f, d[1] + 1.5f, fill);
            }
            c.drawRect(0, SAND_TOP, W, H, vignette);
            c.restore();
        }

        /** What stands in front of the beach on the left: the two palm trunks and the bushes at their feet. */
        void front(Canvas c) {
            for (float[] pm : PALMS) trunk(c, pm[0], pm[1], pm[2], pm[3], pm[4], (int) pm[6]);
            bush(c, BUSH, 650, 31);
            bush(c, SHRUB, 220, 32);
        }

        /** A bush as a heap of leaves inside an outline: darker inside and below, lit on top. */
        void bush(Canvas c, float[] poly, int n, long seed) {
            Path outline = new Path();
            outline.moveTo(poly[0], poly[1]);
            for (int i = 2; i < poly.length; i += 2) outline.lineTo(poly[i], poly[i + 1]);
            outline.close();
            RectF b = new RectF();
            outline.computeBounds(b, true);
            Region reg = new Region();
            reg.setPath(outline, new Region(-10, 0, 1400, 800));
            int dk = Palette.mix(p.palm, Color.BLACK, 0.4f), lit = Palette.mix(p.palm, p.rim, 0.8f);
            fill.setMaskFilter(new BlurMaskFilter(4, BlurMaskFilter.Blur.NORMAL));
            fill.setColor(Palette.alpha(dk, 0.7f));
            c.drawPath(outline, fill);
            fill.setMaskFilter(null);
            Random r = new Random(seed);
            List<float[]> leaves = new ArrayList<>();
            for (int tries = 0; leaves.size() < n && tries < n * 20; tries++) {
                float x = b.left + r.nextFloat() * b.width(), y = b.top + r.nextFloat() * b.height();
                if (!reg.contains((int) x, (int) y)) continue;
                float depth = (y - b.top) / b.height();
                leaves.add(new float[]{x, y, -100 + r.nextFloat() * 200, 7 + r.nextFloat() * 6, 0.8f - depth * 0.5f + (r.nextFloat() - 0.5f) * 0.6f});
            }
            leaves.sort((a, bb) -> Float.compare(a[4], bb[4]));
            for (float[] lf : leaves) {
                float x = lf[0], y = lf[1], len = lf[3], light = Math.max(0, Math.min(1, lf[4]));
                fill.setColor(light < 0.5f ? Palette.mix(dk, p.palm, light * 2) : Palette.mix(p.palm, lit, (light - 0.5f) * 2));
                c.save();
                c.rotate(lf[2], x, y);
                path.reset();
                path.moveTo(x - len / 2, y);
                path.quadTo(x, y - len * 0.32f, x + len / 2, y);
                path.quadTo(x, y + len * 0.32f, x - len / 2, y);
                c.drawPath(path, fill);
                c.restore();
            }
        }

        /** A heap of cumulus with a flat base: puffs in columns, the shadowed body first, then the lit tops. */
        Bitmap cumulus(float[] d, long seed, float[] at) {
            float cx = d[0], base = d[1], w = d[2], h = d[3], pad = 16;
            Bitmap bm = Bitmap.createBitmap(Math.round(w + pad * 2), Math.round(h + pad * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            at[0] = cx - w / 2 - pad;
            at[1] = base - h - pad;
            c.translate(-at[0], -at[1]);
            Random r = new Random(seed);
            List<float[]> puffs = new ArrayList<>();
            int cols = Math.max(3, Math.round(w / 34));
            for (int j = 0; j < cols; j++) {
                float u = (j + 0.5f) / cols;
                float x = cx - w / 2 + w * u + (r.nextFloat() - 0.5f) * w / cols * 0.5f;
                float top = base - h * (0.3f + 0.7f * (float) Math.pow(Math.sin(Math.PI * u), 0.8)) * (0.8f + r.nextFloat() * 0.2f);
                float rad = Math.min(w / cols * 0.95f, (base - top) * 0.55f) * (0.85f + r.nextFloat() * 0.3f);
                for (float y = base - rad * 0.5f; ; y -= rad * 0.75f) {
                    float yy = Math.max(y, top + rad);
                    puffs.add(new float[]{x + (r.nextFloat() - 0.5f) * rad * 0.4f, yy, rad * (0.85f + r.nextFloat() * 0.3f)});
                    if (yy <= top + rad) break;
                }
            }
            c.save();
            c.clipRect(at[0], at[1], at[0] + w + pad * 2, base);
            fill.setMaskFilter(new BlurMaskFilter(2.5f, BlurMaskFilter.Blur.NORMAL));
            fill.setColor(p.cloudLo);
            for (float[] pf : puffs) c.drawCircle(pf[0], pf[1], pf[2], fill);
            for (float[] pf : puffs) {
                float k = Math.max(0, Math.min(1, 1 - (pf[1] - (base - h)) / h));
                fill.setColor(Palette.alpha(Palette.mix(p.cloudLo, p.cloudHi, 0.55f + 0.45f * k), 0.9f));
                c.drawCircle(pf[0] - pf[2] * 0.06f, pf[1] - pf[2] * 0.2f, pf[2] * 0.84f, fill);
            }
            fill.setMaskFilter(null);
            c.restore();
            // shading towards the flat base
            fill.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            fill.setShader(new LinearGradient(0, base - h * 0.45f, 0, base, Palette.alpha(p.cloudLo, 0), Palette.alpha(p.cloudLo, 0.65f), Shader.TileMode.CLAMP));
            c.drawRect(at[0], at[1], at[0] + w + pad * 2, base, fill);
            fill.setShader(null);
            fill.setXfermode(null);
            return bm;
        }

        /** A thin streak of cloud: a few soft overlapping ellipses, lit along the top. */
        Bitmap streak(float[] d, long seed, float[] at) {
            float cx = d[0], cy = d[1], w = d[2], h = d[3], pad = h * 1.2f + 8;
            Bitmap bm = Bitmap.createBitmap(Math.round(w + pad * 2), Math.round(h + pad * 2), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            at[0] = cx - w / 2 - pad;
            at[1] = cy - h / 2 - pad;
            c.translate(-at[0], -at[1]);
            Random r = new Random(seed);
            fill.setMaskFilter(new BlurMaskFilter(h * 0.35f + 1, BlurMaskFilter.Blur.NORMAL));
            RectF o = new RectF();
            float[][] parts = new float[5][4];
            for (float[] e : parts) {
                e[0] = cx + (r.nextFloat() - 0.5f) * w * 0.5f; e[1] = cy + (r.nextFloat() - 0.5f) * h * 0.6f;
                e[2] = w * (0.35f + r.nextFloat() * 0.4f); e[3] = h * (0.5f + r.nextFloat() * 0.5f);
                fill.setColor(Palette.alpha(Palette.mix(p.cloudLo, p.cloudHi, 0.35f), 0.75f));
                o.set(e[0] - e[2] / 2, e[1] - e[3] / 2, e[0] + e[2] / 2, e[1] + e[3] / 2);
                c.drawOval(o, fill);
            }
            for (float[] e : parts) {
                fill.setColor(Palette.alpha(p.cloudHi, 0.7f));
                o.set(e[0] - e[2] * 0.4f, e[1] - e[3] * 0.5f, e[0] + e[2] * 0.4f, e[1]);
                c.drawOval(o, fill);
            }
            fill.setMaskFilter(null);
            return bm;
        }

        void grain(Canvas c, float y0, float y1) {
            Paint g = new Paint();
            g.setShader(new BitmapShader(grain, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
            g.setAlpha(15);
            g.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
            c.drawRect(0, y0, W, y1, g);
        }

        void trunk(Canvas c, float bx, float by, float tx, float ty, float s, int k) {
            float lean = tx - bx;
            float c1x = bx - 26 * s + lean * 0.05f, c1y = by - (by - ty) * 0.38f;
            float c2x = bx + lean * 0.78f + 20 * s, c2y = by - (by - ty) * 0.78f;
            int n = 48;
            float[][] pts = new float[n + 1][3];
            for (int i = 0; i <= n; i++) {
                float u = i / (float) n, v = 1 - u;
                float x = v * v * v * bx + 3 * v * v * u * c1x + 3 * v * u * u * c2x + u * u * u * tx;
                float y = v * v * v * by + 3 * v * v * u * c1y + 3 * v * u * u * c2y + u * u * u * ty;
                x += (float) (Math.sin(u * 19 + k) * 0.9 * s + Math.sin(u * 47 + k * 3) * 0.4 * s);
                float flare = u < 0.07f ? (float) Math.pow((0.07 - u) / 0.07, 2) * 10 * s : 0;
                pts[i][0] = x; pts[i][1] = y; pts[i][2] = (float) ((12.5 - 5.5 * Math.pow(u, 0.8)) * s + flare);
            }
            fill.setColor(p.trunk);
            ribbon(c, pts, 1, -1);
            fill.setColor(Palette.alpha(p.rim, 0.32f));
            ribbon(c, pts, 1, 0.15f);
            float u = 0.04f;
            int j = 0;
            stroke.setStrokeCap(Paint.Cap.BUTT);
            while (u < 0.97f) {
                int i = Math.round(u * n);
                float[] q = pts[i], nm = norm(pts, i);
                stroke.setColor(j % 3 == 0 ? Palette.alpha(p.rim, 0.35f) : 0x47000000);
                stroke.setStrokeWidth((j % 3 == 0 ? 1.4f : 1f) * s);
                path.reset();
                path.moveTo(q[0] + nm[0] * q[2] * 0.95f, q[1] + nm[1] * q[2] * 0.95f);
                path.quadTo(q[0] - nm[1] * 3 * s, q[1] + nm[0] * 3 * s, q[0] - nm[0] * q[2] * 0.95f, q[1] - nm[1] * q[2] * 0.95f);
                c.drawPath(path, stroke);
                u += 0.018f + fract(Math.sin(j * 12.9898 + k) * 43758.5453) * 0.022f;
                j++;
            }
            stroke.setStrokeCap(Paint.Cap.ROUND);
            fill.setColor(p.trunk);
            float[][] nuts = {{-7, 9}, {6, 11}, {0, 16}, {10, 4}, {-11, 3}};
            for (float[] nu : nuts) c.drawCircle(tx + nu[0] * s, ty + nu[1] * s, 6.5f * s, fill);
            fill.setColor(Palette.alpha(p.rim, 0.25f));
            c.drawCircle(tx - 8 * s, ty + 7 * s, 2.5f * s, fill);
            c.drawCircle(tx + 5 * s, ty + 9 * s, 2.5f * s, fill);
        }

        void ribbon(Canvas c, float[][] pts, float a, float b) {
            path.reset();
            for (int i = 0; i < pts.length; i++) {
                float[] q = pts[i], nm = norm(pts, i);
                float x = q[0] + nm[0] * q[2] * a, y = q[1] + nm[1] * q[2] * a;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            for (int i = pts.length - 1; i >= 0; i--) {
                float[] q = pts[i], nm = norm(pts, i);
                path.lineTo(q[0] + nm[0] * q[2] * b, q[1] + nm[1] * q[2] * b);
            }
            path.close();
            c.drawPath(path, fill);
        }

        void palm(List<Frond> out, float cx, float cy, float s, int k0) {
            float base = 175 * s;
            for (int i = 0; i < CROWN.length; i++) {
                float[] f = CROWN[i];
                float ang = f[0] + (fract(Math.sin((k0 + 1) * 78.233 + i * 12.9898 + 1) * 43758.5453) - 0.5f) * 0.3f;
                float len = f[1] * base * (0.82f + fract(Math.sin((k0 + 1) * 78.233 + i * 12.9898 + 2) * 43758.5453) * 0.32f);
                float droop = f[2] * (0.8f + fract(Math.sin((k0 + 1) * 78.233 + i * 12.9898 + 3) * 43758.5453) * 0.45f);
                out.add(frond(cx, cy, ang, len, droop, k0 + i, s, f[3] > 0, 0.4f));
            }
        }

        /** One frond at rest, as a bitmap with the crown at offset (-ox, -oy). Leaf is the leaflets' length to the frond's. */
        Frond frond(float cx, float cy, float ang, float len, float droop, int kk, float s, boolean dead, float leaf) {
            float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
            float[] P1 = {dx * len * 0.4f, dy * len * 0.4f - len * 0.06f};
            float[] P2 = {dx * len * 0.78f, dy * len * 0.78f + len * 0.12f * droop};
            float[] P3 = {dx * len * 0.92f, dy * len * 0.85f + len * 0.42f * droop};
            int dark = dead ? Palette.mix(p.trunk, p.rim, 0.35f) : p.palm;
            int lit = dead ? Palette.mix(p.trunk, p.rim, 0.6f) : Palette.mix(p.palm, p.rim, 0.5f);
            // a leaflet every 9 px or so, however long the frond
            int m = dead ? 14 : Math.max(20, Math.round(len / 9));
            List<float[]> leaves = new ArrayList<>();
            float minX = 0, minY = 0, maxX = 0, maxY = 0;
            float[] prev = bez(P1, P2, P3, 0);
            for (int i = 2; i <= m; i++) {
                float u = i / (float) m;
                float[] q = bez(P1, P2, P3, u);
                float tx = q[0] - prev[0], ty = q[1] - prev[1];
                float tl = (float) Math.hypot(tx, ty);
                if (tl == 0) tl = 1;
                tx /= tl; ty /= tl;
                prev = q;
                float rnd = fract(Math.sin(i * 91.7 + kk * 13.1) * 43758.5453);
                if (!dead && rnd > 0.93f) continue;
                float lf = (float) (len * (dead ? 0.26 : leaf) * Math.pow(Math.sin(Math.PI * Math.min(1, 0.06 + u)), 0.7) * (1 - 0.4 * u) * (0.78 + rnd * 0.4));
                for (int side = 1; side >= -1; side -= 2) {
                    double ra = side * (0.8 + rnd * 0.35);
                    float lx = (float) (tx * Math.cos(ra) - ty * Math.sin(ra)), ly = (float) (tx * Math.sin(ra) + ty * Math.cos(ra));
                    float hang = dead ? 0.95f : 0.55f;
                    float ex = q[0] + lx * lf * 0.72f, ey = q[1] + ly * lf * 0.72f + lf * hang;
                    float c2x = q[0] + lx * lf * 0.55f, c2y = q[1] + ly * lf * 0.55f + lf * 0.05f;
                    float nx = -(ey - q[1]), ny = ex - q[0];
                    float nl = (float) Math.hypot(nx, ny);
                    if (nl == 0) nl = 1;
                    nx /= nl; ny /= nl;
                    float w = (dead ? 2.4f : 6.2f) * Math.min(s, 1.1f) * (1 - u * 0.3f);
                    leaves.add(new float[]{q[0], q[1], c2x + nx * w, c2y + ny * w, ex, ey, c2x - nx * w, c2y - ny * w, side > 0 ? dark : lit});
                    minX = Math.min(minX, Math.min(ex, c2x - w)); maxX = Math.max(maxX, Math.max(ex, c2x + w));
                    minY = Math.min(minY, Math.min(ey, c2y - w)); maxY = Math.max(maxY, Math.max(ey, c2y + w));
                }
            }
            for (float[] pt : new float[][]{P1, P2, P3}) {
                minX = Math.min(minX, pt[0]); maxX = Math.max(maxX, pt[0]); minY = Math.min(minY, pt[1]); maxY = Math.max(maxY, pt[1]);
            }
            float mg = 6;
            Frond fr = new Frond();
            fr.cx = cx; fr.cy = cy; fr.k = kk;
            fr.ox = minX - mg; fr.oy = minY - mg;
            fr.w = maxX - minX + 2 * mg; fr.h = maxY - minY + 2 * mg;
            fr.bm = Bitmap.createBitmap(Math.max(1, Math.round(fr.w)), Math.max(1, Math.round(fr.h)), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(fr.bm);
            c.translate(-fr.ox, -fr.oy);
            for (float[] l : leaves) {
                fill.setColor((int) l[8]);
                path.reset();
                path.moveTo(l[0], l[1]);
                path.quadTo(l[2], l[3], l[4], l[5]);
                path.quadTo(l[6], l[7], l[0], l[1]);
                c.drawPath(path, fill);
            }
            stroke.setColor(dark);
            stroke.setStrokeWidth(2.4f * Math.min(s, 1.1f));
            path.reset();
            path.moveTo(0, 0);
            path.cubicTo(P1[0], P1[1], P2[0], P2[1], P3[0], P3[1]);
            c.drawPath(path, stroke);
            return fr;
        }
    }

    /** Where the dry sand starts at x: a smooth curve through {@link #SHORE}. */
    private static float shore(float x) {
        float f = Math.max(0, Math.min(SHORE.length - 1.001f, x / 100f));
        int i = (int) f;
        float t = f - i;
        float p0 = SHORE[Math.max(0, i - 1)], p1 = SHORE[i], p2 = SHORE[Math.min(SHORE.length - 1, i + 1)], p3 = SHORE[Math.min(SHORE.length - 1, i + 2)];
        return 0.5f * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    /** The height of a traced outline at x. */
    private static float ridgeY(float[] pts, float x) {
        for (int i = 2; i < pts.length; i += 2) {
            if (x <= pts[i]) {
                float x0 = pts[i - 2], f = (x - x0) / Math.max(1, pts[i] - x0);
                return pts[i - 1] + (pts[i + 1] - pts[i - 1]) * Math.max(0, f);
            }
        }
        return pts[pts.length - 1];
    }

    private static float[] norm(float[][] pts, int i) {
        float[] a = pts[Math.max(0, i - 1)], b = pts[Math.min(pts.length - 1, i + 1)];
        float dx = b[0] - a[0], dy = b[1] - a[1];
        float l = (float) Math.hypot(dx, dy);
        if (l == 0) l = 1;
        return new float[]{-dy / l, dx / l};
    }

    private static float[] bez(float[] p1, float[] p2, float[] p3, float u) {
        float v = 1 - u;
        return new float[]{3 * v * v * u * p1[0] + 3 * v * u * u * p2[0] + u * u * u * p3[0],
                3 * v * v * u * p1[1] + 3 * v * u * u * p2[1] + u * u * u * p3[1]};
    }

    private static float fract(double x) {
        return (float) (x - Math.floor(x));
    }

    // ---------------------------------------------------------------- frame

    /**
     * Where the sun or the moon is at this minute. Kata looks west: in the morning the sun is behind the hills at
     * our back; after noon it comes down from the top of the frame and sets into the sea right of Koh Pu. The moon
     * crosses the night sky from the right.
     */
    private float[] sunAt(float m) {
        float u = (m - rise) / (float) (set - rise);
        if (u >= -0.04f && u <= 1.04f) {
            if (u < 0.5f) return new float[]{640, -600, 1};
            float x = 940 - (1 - u) * 420;
            float y = SEA - (1 - u) * 1300 + (u > 1 ? (u - 1) * 600 : 0);
            return new float[]{x, y, 1};
        }
        float nightLen = 1440 - (set - rise);
        float v = ((m - set + 1440) % 1440) / nightLen;
        float x = 1250 - v * 950;
        float y = (float) (446 - Math.sin(Math.PI * Math.max(0, Math.min(1, v))) * 320);
        return new float[]{x, y, 0};
    }

    /** 0 with the sun high, 1 when it sits on the sea. */
    private static float low(float y) {
        return Math.max(0, Math.min(1, (y - 200) / 200f));
    }

    private void drawFrame(Canvas c, Layers l, float minute) {
        long t0 = System.nanoTime();
        Palette p = l.p;
        float t = motion == STILL ? stillT : (SystemClock.uptimeMillis() - start) / 1000f;
        float[] sun = sunAt(minute);
        boolean isSun = sun[2] > 0;
        c.drawBitmap(l.sky, 0, 0, null);

        if (p.night > 0.05f && showStars) drawStars(c, p, t);
        // sun or moon with its glow
        if (isSun && sun[1] > -p.gr) {
            // low over the sea the glare spreads along the horizon
            c.save();
            c.translate(sun[0], sun[1]);
            c.scale(1 + 0.6f * low(sun[1]), 1);
            glowPaint.setShader(l.glow);
            c.drawCircle(0, 0, p.gr, glowPaint);
            c.restore();
            c.drawBitmap(l.sun, sun[0] - l.sun.getWidth() / 2f, sun[1] - l.sun.getHeight() / 2f, bmp);
        } else if (!isSun && p.night > 0.3f) {
            bmp.setAlpha(Math.round(255 * Math.min(1, p.night)));
            c.drawBitmap(l.moon, sun[0] - l.moon.getWidth() / 2f, sun[1] - l.moon.getHeight() / 2f, bmp);
            bmp.setAlpha(255);
        }
        for (int i = 0; clouds && i < CLOUDS; i++) {
            if (l.clouds[i] == null) continue;
            float[] d = cloudDrift[i];
            float x = l.cloudAt[i][0] + (float) Math.sin(t * 6.2832f / d[0] + d[1]) * d[2];
            bmp.setAlpha(Math.round(255 * Math.min(1, l.cloudAlpha[i])));
            c.drawBitmap(l.clouds[i], x, l.cloudAt[i][1], bmp);
        }
        bmp.setAlpha(255);
        c.drawBitmap(l.land, 0, LAND_TOP, null);

        // the path of light on the water under a low sun or the moon
        float shine = isSun ? low(sun[1]) : p.night * 0.8f;
        if (shine > 0.05f) {
            bmp.setAlpha(Math.round(255 * shine));
            c.drawBitmap(l.column, sun[0] - 80, SEA + 1, bmp);
            bmp.setAlpha(255);
        }
        drawWaves(c, p, t, sun[0], shine > 0.05f);
        if (boat) drawBoat(c, p, t);
        if (birds && (p.phase == Palette.Phase.MORNING || p.phase == Palette.Phase.SUNSET)) drawBirds(c, p, t);

        float wash = (float) (Math.sin(t * 0.55) * 0.5 + 0.5) * 16;
        drawSwell(c, p, t, wash);
        fill.setColor(p.wet);
        path.reset();
        path.moveTo(0, 720);
        for (int x = 0; x <= 1280; x += 16) path.lineTo(x, shore(x) - wash - 4);
        path.lineTo(1280, 720);
        path.close();
        c.drawPath(path, fill);
        c.drawBitmap(l.sand, 0, SAND_TOP, null);
        drawFoam(c, p, t, wash);
        c.drawBitmap(l.front, 0, FRONT_TOP, null);

        for (Frond fr : l.fronds) {
            float sway = fr.sway * (float) (Math.sin(t * 0.9 + fr.k * 1.3) * 0.045 + Math.sin(t * 2.1 + fr.k * 0.7) * 0.015);
            c.save();
            c.rotate((float) Math.toDegrees(sway), fr.cx, fr.cy);
            c.drawBitmap(fr.bm, fr.cx + fr.ox, fr.cy + fr.oy, bmp);
            c.restore();
        }

        drawNs += System.nanoTime() - t0;
        frames++;
        long now = SystemClock.uptimeMillis();
        if (now - statFrom >= 5000) {
            Log.i(TAG, String.format("fps=%.1f draw=%.2fms minute=%.0f", frames * 1000f / (now - statFrom), drawNs / 1e6f / Math.max(1, frames), minute));
            statFrom = now; frames = 0; drawNs = 0;
        }
    }

    // Waves and stars go out in a few batched calls: one per width/brightness band instead of one per dash
    // or star, which on the stick's CPU was most of the frame.
    private static final int BANDS = 6, GLINTS = 4;
    private final float[][] waveBuf = new float[BANDS + BANDS * GLINTS][4 * 700];
    private final int[] waveCnt = new int[BANDS + BANDS * GLINTS];
    private final float[][] starBuf = new float[3 * 5][2 * 170];
    private final int[] starCnt = new int[3 * 5];

    private void drawWaves(Canvas c, Palette p, float t, float sx, boolean lit) {
        java.util.Arrays.fill(waveCnt, 0);
        for (int i = 0; i < waveRows.length; i++) {
            float kk = i / (float) waveRows.length;
            int band = Math.min(BANDS - 1, (int) (kk * BANDS));
            float y = (float) (SEA + 4 + Math.pow(kk, 1.7) * 250);
            float sp = 6 + kk * 26;
            float near = lit ? 30 + kk * 90 : -1;
            for (float[] d : waveRows[i]) {
                float x = ((d[0] + t * sp) % 1400) - 60;
                if (y > shore(x) - 6) continue;
                float len = (6 + kk * 34) * d[1];
                int b = band;
                if (Math.abs(x - sx) < near) {
                    float a = (float) (0.5 + 0.5 * Math.sin(t * 3 + d[0]));
                    b = BANDS + band * GLINTS + Math.min(GLINTS - 1, (int) (a * GLINTS));
                }
                float[] buf = waveBuf[b];
                int n = waveCnt[b];
                if (n + 4 > buf.length) continue;
                buf[n] = x; buf[n + 1] = y; buf[n + 2] = x + len; buf[n + 3] = y;
                waveCnt[b] = n + 4;
            }
        }
        for (int b = 0; b < waveBuf.length; b++) {
            if (waveCnt[b] == 0) continue;
            int band = b < BANDS ? b : (b - BANDS) / GLINTS;
            float kk = (band + 0.5f) / BANDS;
            float alpha = b < BANDS ? 0.04f + 0.09f * kk : 0.3f + 0.5f * (((b - BANDS) % GLINTS) + 0.5f) / GLINTS;
            stroke.setStrokeWidth(0.8f + kk * 1.8f);
            stroke.setColor(Palette.alpha(p.glint, alpha));
            c.drawLines(waveBuf[b], 0, waveCnt[b], stroke);
        }
    }

    private void drawStars(Canvas c, Palette p, float t) {
        java.util.Arrays.fill(starCnt, 0);
        for (float[] s : stars) {
            float a = (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * s[4] + s[3]))) * (s[1] < 340 ? 1 : 0.4f);
            int size = Math.min(2, (int) ((s[2] - 0.4f) / 1.4f * 3));
            int b = size * 5 + Math.min(4, (int) (a * 5));
            float[] buf = starBuf[b];
            buf[starCnt[b]++] = s[0];
            buf[starCnt[b]++] = s[1];
        }
        stroke.setStrokeCap(Paint.Cap.ROUND);
        for (int b = 0; b < starBuf.length; b++) {
            if (starCnt[b] == 0) continue;
            stroke.setStrokeWidth(2 * (0.4f + (b / 5 + 0.5f) / 3f * 1.4f));
            stroke.setColor(Palette.alpha(Color.WHITE, ((b % 5) + 0.5f) / 5f * p.night * 0.8f));
            c.drawPoints(starBuf[b], 0, starCnt[b], stroke);
        }
    }

    /** Two waves rolling in through the shallows, one behind the other: they rise, run up the sand and fade. */
    private void drawSwell(Canvas c, Palette p, float t, float wash) {
        for (int k = 0; k < 2; k++) {
            float f = (t * 0.11f + k * 0.5f) % 1;
            float d = 12 + 70 * (1 - f), a = (float) Math.pow(Math.sin(Math.PI * f), 1.5);
            if (a < 0.02f) continue;
            path.reset();
            for (int x = -16; x <= 1296; x += 16) {
                float y = (float) (shore(x) - d - wash * f * 0.5f + Math.sin(x / 70.0 + k * 2 + t * 0.5) * 2.5);
                if (x == -16) path.moveTo(x, y); else path.lineTo(x, y);
            }
            stroke.setStrokeWidth(6);
            stroke.setColor(Palette.alpha(Palette.mix(p.sea[2], p.sea[0], 0.4f), 0.3f * a));
            c.drawPath(path, stroke);
            c.save();
            c.translate(0, -3);
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(Palette.alpha(p.foam, 0.75f * a));
            c.drawPath(path, stroke);
            c.restore();
        }
    }

    private void drawFoam(Canvas c, Palette p, float t, float wash) {
        path.reset();
        for (int x = 0; x <= 1280; x += 8) {
            float y = (float) (shore(x) - wash + Math.sin(x / 21.0 + t * 2.2) * 1.6 + Math.sin(x / 57.0 - t) * 2);
            if (x == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        stroke.setColor(Palette.alpha(p.foam, 0.28f));
        stroke.setStrokeWidth(12);
        c.drawPath(path, stroke);
        stroke.setColor(Palette.alpha(p.foam, 0.9f));
        stroke.setStrokeWidth(2.6f);
        if (p.night > 0.5f) stroke.setShadowLayer(10, 0, 0, Palette.alpha(p.foam, 1));
        c.drawPath(path, stroke);
        stroke.clearShadowLayer();
        // lace left behind on the wet sand
        path.reset();
        for (int x = 0; x <= 1280; x += 10) {
            float y = (float) (shore(x) - 12 - wash * 0.6 + Math.sin(x / 33.0 + t * 1.6) * 1.4);
            if (x == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        lace.setColor(Palette.alpha(p.foam, 0.55f));
        lace.setStrokeWidth(2);
        c.drawPath(path, lace);
        path.reset();
        for (int x = 0; x <= 1280; x += 10) {
            float y = (float) (shore(x) - 26 - wash * 0.6 + Math.sin(x / 33.0 + t * 1.6) * 1.4);
            if (x == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        stroke.setColor(Palette.alpha(p.foam, 0.35f));
        stroke.setStrokeWidth(1.6f);
        c.drawPath(path, stroke);
    }

    private void drawBirds(Canvas c, Palette p, float t) {
        stroke.setColor(Palette.alpha(p.palm, 0.8f));
        stroke.setStrokeWidth(1.6f);
        for (int i = 0; i < 4; i++) {
            float bx = ((300 + t * 14 + i * 46) % 1500) - 100;
            float by = (float) (210 + i * 14 + Math.sin(t * 0.7 + i) * 6);
            float f = (float) (Math.sin(t * 6 + i) * 3);
            path.reset();
            path.moveTo(bx - 8, by - f);
            path.quadTo(bx - 3, by - 3, bx, by);
            path.quadTo(bx + 3, by - 3, bx + 8, by - f);
            c.drawPath(path, stroke);
        }
    }

    /** A Thai longtail crossing the bay right to left with its wake; it leaves the frame and comes back. */
    private void drawBoat(Canvas c, Palette p, float t) {
        float x = 1500 - ((t * 24) % 1900);
        float y = (float) (490 + Math.sin(t * 1.7) * 0.8);
        if (x < -260 || x > 1500) return;
        boolean night = p.night > 0.5f;
        int wood = night ? 0xFF0A0F22 : Palette.mix(p.hillDark, 0xFF3A2414, 0.55f);
        int rail = night ? 0xFF1A2340 : Palette.mix(p.sand1, Color.WHITE, 0.2f);
        stroke.setStrokeWidth(1.4f);
        for (int i = 0; i < 14; i++) {
            float d = i * 14;
            stroke.setColor(Palette.alpha(p.foam, 0.32f * (1 - i / 14f)));
            c.drawLine(x + 70 + d, y + 3 + d * 0.12f, x + 78 + d, y + 3 + d * 0.13f, stroke);
            c.drawLine(x + 70 + d, y + 7 + d * 0.14f, x + 78 + d, y + 7 + d * 0.17f, stroke);
        }
        fill.setColor(Palette.alpha(p.foam, 0.55f));
        for (int i = 0; i < 6; i++) {
            float cx = x + 92 + i * 5, cy = (float) (y + 5 + Math.sin(t * 9 + i) * 1.2);
            rect.set(cx - (4 - i * 0.5f), cy - 1.4f, cx + (4 - i * 0.5f), cy + 1.4f);
            c.drawOval(rect, fill);
        }
        stroke.setColor(night ? 0xFF0A0F22 : 0xFF2B2B2B);
        stroke.setStrokeWidth(1.6f);
        c.drawLine(x + 64, y - 6, x + 96, y + 4, stroke);
        fill.setColor(night ? 0xFF0A0F22 : 0xFF3A3A3A);
        c.drawRect(x + 58, y - 11, x + 67, y - 5, fill);
        fill.setColor(wood);
        path.reset();
        path.moveTo(x + 66, y - 4);
        path.quadTo(x + 20, y + 6, x - 30, y + 2);
        path.quadTo(x - 48, y - 2, x - 58, y - 16);
        path.quadTo(x - 62, y - 26, x - 58, y - 32);
        path.lineTo(x - 54, y - 30);
        path.quadTo(x - 50, y - 18, x - 36, y - 9);
        path.quadTo(x + 10, y - 7, x + 66, y - 9);
        path.close();
        c.drawPath(path, fill);
        stroke.setColor(rail);
        stroke.setStrokeWidth(1.2f);
        path.reset();
        path.moveTo(x - 52, y - 25);
        path.quadTo(x - 48, y - 15, x - 36, y - 9);
        path.quadTo(x + 10, y - 7, x + 66, y - 9);
        c.drawPath(path, stroke);
        if (!night) {
            int[] ribbons = {0xFFE8473C, 0xFFF5C542, 0xFF3FA9F5};
            stroke.setStrokeWidth(1.6f);
            for (int i = 0; i < 3; i++) {
                stroke.setColor(ribbons[i]);
                path.reset();
                path.moveTo(x - 57 + i, y - 30 + i * 2);
                path.quadTo(x - 62 + i, (float) (y - 24 + i * 2 + Math.sin(t * 4 + i)), x - 58 + i * 2, y - 18 + i * 2);
                c.drawPath(path, stroke);
            }
        }
        fill.setColor(night ? 0xFF121A36 : Palette.mix(p.sea[2], 0xFF2C6FB0, 0.6f));
        path.reset();
        path.moveTo(x - 18, y - 22);
        path.lineTo(x + 40, y - 23);
        path.lineTo(x + 42, y - 19);
        path.lineTo(x - 20, y - 18);
        path.close();
        c.drawPath(path, fill);
        stroke.setColor(wood);
        stroke.setStrokeWidth(1.2f);
        for (int dx : new int[]{-14, 12, 36}) c.drawLine(x + dx, y - 19, x + dx, y - 8, stroke);
        if (night) {
            fill.setColor(0xFFFFD98A);
            fill.setShadowLayer(10, 0, 0, 0xFFFFD98A);
            c.drawCircle(x + 12, y - 25, 2, fill);
            fill.clearShadowLayer();
        }
        fill.setColor(Palette.alpha(night ? Color.BLACK : p.sea[2], 0.25f));
        rect.set(x + 10 - 62, y + 3, x + 10 + 62, y + 9);
        c.drawOval(rect, fill);
    }

    // ---------------------------------------------------------------- the painted backdrops

    private static final String[] BACKDROPS = {"morning", "day", "sunset", "night"};
    /** A still picture is drawn again only when the blend changes; the clock is looked at this often. */
    private static final long PICTURE_CHECK_MS = 20_000;
    /** The backdrops at the surface's size, decoded once on the build thread. */
    private final Bitmap[] backdrop = new Bitmap[BACKDROPS.length];
    /** The current blend of the backdrops, and the one being swapped in. */
    private Bitmap picture;
    private volatile Bitmap nextPicture;
    private volatile int builtMix = -1;
    private int mixShown = -1;
    private boolean pictureShown;
    private static final Paint SHADE_LEFT = shade(150, 190, 640, 0x70000000), SHADE_RIGHT = shade(1120, 250, 520, 0x4D000000);

    /**
     * A soft shade under the clock and under the notes, in 1280×720 units: the backdrops and the clips are bright
     * where the drawn bay has a darker sky. Drawn by the screen above the scene, over the pictures and the video.
     */
    static void shade(Canvas c) {
        c.drawRect(0, 0, 1280, 720, SHADE_LEFT);
        c.drawRect(0, 0, 1280, 720, SHADE_RIGHT);
    }

    /** Whether this style needs {@link #shade}. */
    boolean shaded() {
        return style != PAINTED;
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

    private void runPictures(long now, float minute) {
        if (cur != null) {
            // the drawn bay's layers are not needed while the pictures show
            cur.recycle();
            cur = null;
            builtMinute = -999;
        }
        float[] w = Palette.backdrops(minute, rise, set);
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
        if (running && hasSurface && style == PICTURES) render.postAtTime(this, now + frame);
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

    /** Back to the drawn bay: the pictures give their memory back. */
    private void dropPicture() {
        if (picture == null) return;
        picture.recycle();
        picture = null;
        builtMix = -1;
        if (build != null) build.post(() -> {
            for (int i = 0; i < backdrop.length; i++) {
                if (backdrop[i] != null) backdrop[i].recycle();
                backdrop[i] = null;
            }
        });
    }

    // ---------------------------------------------------------------- the clips

    /** Which backdrop's clip belongs to this moment: the one with the most weight. Played by {@link SceneVideo}. */
    int clipNow() {
        float[] w = Palette.backdrops(minuteNow(), rise, set);
        int best = 0;
        for (int i = 1; i < w.length; i++) if (w[i] > w[best]) best = i;
        return best;
    }

    static String clipName(int i) {
        return "scene/" + BACKDROPS[i] + ".mp4";
    }
}
