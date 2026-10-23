package io.github.vivchar.lagoon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Follows WindowManager's log for remote key presses and opens the mapped app.
 *
 * Xiaomi's PhoneWindowManager handles the Netflix and Prime Video buttons in interceptKeyBeforeQueueing and never
 * passes them on, so neither apps nor accessibility services see them. It does log every key:
 * "interceptKeyTq keycode=193 interactive=true keyguardActive=false policyFlags=22000001", where the low bit of
 * policyFlags is set on the press and clear on the release. That line is what this service reacts to.
 */
public class KeyWatchService extends Service {
    private static final String TAG = "RemoteKeys";
    private static final long HOLD_MS = 500;
    /**
     * Any app may write a WindowManager line to the log, so only the system's own count: "-v uid" puts the writer's
     * uid in the header (logd records it, an app cannot pick it), and 1000 is system_server's.
     */
    private static final Pattern LINE = Pattern.compile(
            "^[VDIWEF]/WindowManager\\(\\s*(?:1000|system):\\s*\\d+\\): interceptKeyTq keycode=(\\d+) .*policyFlags=([0-9a-fA-F]+)\\s*$");

    /** Set by SettingsActivity while it waits for a button press; gets the keycode instead of an app launch. */
    interface Learner { void onKey(int code); }
    static volatile Learner learner;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<Integer, Long> downAt = new HashMap<>();
    private final Map<Integer, Boolean> held = new HashMap<>();
    private volatile boolean running;
    private Thread reader;
    private Process logcat;
    private Mappings mappings;

    static void start(Context context) {
        context.startForegroundService(new Intent(context, KeyWatchService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mappings = new Mappings(this);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("watch", "Кнопки пульта", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "watch")
                .setContentTitle("Кнопки пульта")
                .setSmallIcon(R.drawable.ic_remote)
                .build();
        startForeground(1, n);
        running = true;
        reader = new Thread(this::readLoop, "logcat");
        reader.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (logcat != null) logcat.destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void readLoop() {
        while (running) {
            try {
                // -T 1: follow from the newest line on; that one line is history, so it is skipped.
                logcat = new ProcessBuilder("logcat", "-T", "1", "-v", "brief", "-v", "uid", "WindowManager:D", "*:S")
                        .redirectErrorStream(true).start();
                BufferedReader in = new BufferedReader(new InputStreamReader(logcat.getInputStream()));
                in.readLine();
                String line;
                while (running && (line = in.readLine()) != null) {
                    Matcher m = LINE.matcher(line);
                    if (!m.matches()) continue;
                    int code = Integer.parseInt(m.group(1));
                    boolean down = (Long.parseLong(m.group(2), 16) & 1) != 0;
                    main.post(() -> onKey(code, down));
                }
            } catch (Exception e) {
                Log.w(TAG, "logcat", e);
            }
            if (logcat != null) logcat.destroy();
            if (running) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void onKey(int code, boolean down) {
        Learner l = learner;
        if (l != null) {
            if (down) l.onKey(code);
            return;
        }
        if (!mappings.has(code)) return;
        if (down) {
            if (downAt.containsKey(code)) return;   // auto-repeat while held
            downAt.put(code, System.currentTimeMillis());
            held.put(code, false);
            String holdApp = mappings.hold(code);
            if (holdApp != null) {
                main.postDelayed(() -> {
                    if (downAt.containsKey(code) && !Boolean.TRUE.equals(held.get(code))) {
                        held.put(code, true);
                        open(holdApp);
                    }
                }, HOLD_MS);
            }
        } else {
            if (downAt.remove(code) == null) return;
            if (!Boolean.TRUE.equals(held.remove(code))) open(mappings.tap(code));
        }
    }

    private void open(String pkg) {
        if (pkg == null) return;
        Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent == null) return;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "open " + pkg, e);
        }
    }
}
