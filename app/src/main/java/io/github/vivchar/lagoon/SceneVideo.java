package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The «Видео» look: the clip of the time of day loops on a surface of its own. A video decoder cannot take over
 * a surface the drawn bay has painted on, so the bay's {@link SceneView} is hidden while this one shows. The
 * clock is looked at now and then; when the time of day changes, the next clip takes over.
 */
final class SceneVideo extends SurfaceView implements SurfaceHolder.Callback {
    private static final long CHECK_MS = 20_000;
    private final SceneView scene;
    private final Handler main = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private int playing = -1;
    private boolean hasSurface, running;

    SceneVideo(Context c, SceneView scene) {
        super(c);
        this.scene = scene;
        getHolder().addCallback(this);
    }

    private final Runnable check = new Runnable() {
        @Override
        public void run() {
            update();
            if (running) main.postDelayed(this, CHECK_MS);
        }
    };

    void setRunning(boolean run) {
        running = run;
        main.removeCallbacks(check);
        if (run) main.post(check);
        else stop();
    }

    /** Plays the clip of this moment, if this look is shown; stops otherwise. */
    void update() {
        if (!hasSurface || !running || getVisibility() != VISIBLE) {
            stop();
            return;
        }
        int i = scene.clipNow();
        if (i != playing || player == null) play(i);
    }

    @Override
    public void surfaceCreated(SurfaceHolder h) {
        hasSurface = true;
        update();
    }

    @Override
    public void surfaceChanged(SurfaceHolder h, int format, int w, int hh) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) {
        hasSurface = false;
        stop();
    }

    private void play(int i) {
        stop();
        try (AssetFileDescriptor fd = getContext().getAssets().openFd(SceneView.clipName(i))) {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mp.setDisplay(getHolder());
            mp.setLooping(true);
            mp.setVolume(0, 0);
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnErrorListener((p, what, extra) -> {
                Log.w("LagoonScene", "clip " + i + " " + what + "/" + extra);
                return true;
            });
            mp.prepareAsync();
            player = mp;
            playing = i;
        } catch (Exception e) {
            Log.w("LagoonScene", "clip " + i, e);
        }
    }

    private void stop() {
        if (player == null) return;
        player.release();
        player = null;
        playing = -1;
    }
}
