package io.github.vivchar.lagoon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * What moves or sounds on a note card. A GIF and a round video loop silently while their card is under the
 * cursor and stand still otherwise, so the home screen does not redraw for a card nobody looks at. A voice
 * message, a sound file, a video or a round video play with sound on OK, with a bar for how far they are; OK
 * or Back stops them, and once one has played to the end, the next OK puts the note away as usual.
 */
final class NoteMedia {
    static final int GIF = 0, VIDEO = 1, ROUND = 2, SOUND = 3;

    private final int kind;
    private final String clip;
    private final Runnable changed;
    private final Handler main = new Handler(Looper.getMainLooper());
    /** The picture: null for a sound. */
    final View view;
    /** The progress bar, shown while playing with sound. */
    final View bar;
    private final View fill;
    private TextureView tv;
    private ImageView poster;
    private Surface surface;
    private MediaPlayer mp;
    private boolean focused, loud, played, wantLoud;
    private final Context ctx;

    NoteMedia(Context c, int kind, String clip, Bitmap still, Runnable changed) {
        this.ctx = c;
        this.kind = kind;
        this.clip = clip;
        this.changed = changed;
        if (kind == SOUND) view = null;
        else {
            FrameLayout pic = new FrameLayout(c);
            tv = new TextureView(c);
            pic.addView(tv, new FrameLayout.LayoutParams(-1, -1));
            poster = new ImageView(c);
            poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (still != null) poster.setImageBitmap(still);
            pic.addView(poster, new FrameLayout.LayoutParams(-1, -1));
            if (kind == VIDEO) {
                // a video stands still until OK: say what it is in the corner
                android.widget.TextView tag = Ui.text(c, c.getString(R.string.video), 14, 600, 0xFFFFFFFF);
                tag.setPadding(Ui.px(c, 8), Ui.px(c, 3), Ui.px(c, 8), Ui.px(c, 3));
                tag.setBackground(Ui.round(0x99000000, 0, 8, c));
                FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.BOTTOM);
                tl.leftMargin = tl.bottomMargin = Ui.px(c, 8);
                pic.addView(tag, tl);
            }
            float r = kind == ROUND ? -1 : Ui.px(c, 14);
            pic.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline o) {
                    if (r < 0) o.setOval(0, 0, v.getWidth(), v.getHeight());
                    else o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
                }
            });
            pic.setClipToOutline(true);
            view = pic;
            tv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                    surface = new Surface(st);
                    if (wantLoud) play(true);
                    else if (focused && loops()) play(false);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                    release();
                    surface = null;
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture st) {
                }
            });
        }
        // a thin track with a blue fill: blue reads on the dark card and on the white one under the cursor
        FrameLayout track = new FrameLayout(c);
        track.setBackground(Ui.round(0x40808080, 0, 2, c));
        fill = new View(c);
        fill.setBackground(Ui.round(Ui.ON, 0, 2, c));
        fill.setPivotX(0);
        fill.setScaleX(0);
        track.addView(fill, new FrameLayout.LayoutParams(-1, -1));
        track.setVisibility(View.GONE);
        bar = track;
    }

    /** Whether OK plays it rather than putting the note away. */
    boolean playable() {
        return kind != GIF && !played;
    }

    boolean playing() {
        return loud;
    }

    /** The line under a focused card: what OK does now. */
    String hint() {
        if (loud) return ctx.getString(R.string.hint_stop);
        if (playable()) return ctx.getString(kind == SOUND ? R.string.hint_listen : R.string.hint_watch);
        return ctx.getString(R.string.hint_read);
    }

    void focus(boolean has) {
        focused = has;
        if (!has) stop();
        else if (loops() && surface != null) play(false);
    }

    /** OK on the card: true if it was taken (play or stop), false to put the note away. */
    boolean click() {
        if (loud) {
            stop();
            if (focused && loops()) play(false);
            return true;
        }
        if (!playable()) return false;
        if (view == null || surface != null) play(true);
        else wantLoud = true;
        return true;
    }

    /** Back on the card: stops playing; false when there was nothing to stop. */
    boolean back() {
        if (!loud) return false;
        click();
        return true;
    }

    void release() {
        main.removeCallbacks(tick);
        if (mp != null) mp.release();
        mp = null;
        if (loud) {
            loud = false;
            bar.setVisibility(View.GONE);
            changed.run();
        }
    }

    private boolean loops() {
        return kind == GIF || kind == ROUND;
    }

    private void stop() {
        release();
        if (poster != null) poster.animate().alpha(1f).setDuration(150).start();
    }

    private void play(boolean withSound) {
        release();
        wantLoud = false;
        try {
            mp = new MediaPlayer();
            if (surface != null) mp.setSurface(surface);
            mp.setDataSource(clip);
            mp.setLooping(!withSound);
            float v = withSound ? 1f : 0f;
            mp.setVolume(v, v);
            if (tv != null) mp.setOnVideoSizeChangedListener((p, vw, vh) -> crop(tv, vw, vh));
            mp.setOnInfoListener((p, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && poster != null) poster.animate().alpha(0f).setDuration(150).start();
                return false;
            });
            mp.setOnPreparedListener(p -> {
                p.start();
                if (withSound) main.post(tick);
            });
            mp.setOnCompletionListener(p -> {
                if (!withSound) return;
                played = true;
                fill.setScaleX(1);
                stop();
                if (focused && loops()) play(false);
            });
            mp.setOnErrorListener((p, what, extra) -> {
                Log.w("NoteMedia", "play " + what + "/" + extra);
                stop();
                return true;
            });
            mp.prepareAsync();
            if (withSound) {
                loud = true;
                fill.setScaleX(0);
                bar.setVisibility(View.VISIBLE);
                changed.run();
            }
        } catch (Exception e) {
            Log.w("NoteMedia", "play", e);
            stop();
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (mp == null || !loud) return;
            try {
                int d = mp.getDuration();
                if (d > 0) fill.setScaleX(Math.min(1f, mp.getCurrentPosition() / (float) d));
            } catch (IllegalStateException ignored) {
            }
            main.postDelayed(this, 200);
        }
    };

    /** Scales the clip to cover the view, cut evenly at the sides or top and bottom. */
    private static void crop(TextureView tv, int vw, int vh) {
        float w = tv.getWidth(), h = tv.getHeight();
        if (w == 0 || h == 0 || vw == 0 || vh == 0) return;
        float s = Math.max(w / vw, h / vh);
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.setScale(vw * s / w, vh * s / h, w / 2f, h / 2f);
        tv.setTransform(m);
    }
}
