package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What a remote button or a quick-panel item can do besides opening an app. In {@link Mappings} an action is
 * stored where a package name would be, with a leading "@" that no package name has.
 */
final class Actions {
    static final String PANEL = "@panel";
    static final String NIGHT = "@night";
    static final String SAVER = "@saver";
    static final String SLEEP = "@sleep";
    static final String BLUETOOTH = "@bluetooth";
    static final String HOME = "@home";

    /** What a remote button can be given, in the picker's order. */
    static final String[] ALL = {PANEL, NIGHT, SAVER, SLEEP, BLUETOOTH, HOME};
    /** The quick panel's items under the slider; the slider itself is always there. */
    static final String[] PANEL_ITEMS = {NIGHT, SLEEP, SAVER, BLUETOOTH, HOME};
    /** Sleep timer steps in minutes; one more press after the last turns it off. */
    static final int[] SLEEP_STEPS = {15, 30, 45, 60, 90};

    private static final String TAG = "Actions";

    private Actions() {
    }

    static boolean is(String value) {
        return value != null && value.startsWith("@");
    }

    static String name(String id) {
        switch (id) {
            case PANEL: return "Панель";
            case NIGHT: return "Ночь";
            case SAVER: return "Заставка";
            case SLEEP: return "Таймер сна";
            case BLUETOOTH: return "Bluetooth";
            case HOME: return "На главный";
            default: return id;
        }
    }

    /** The quiet line over the name in the picker: what the action does. */
    static String verb(String id) {
        switch (id) {
            case NIGHT: return "вкл / выкл";
            case SLEEP: return "запустить";
            case BLUETOOTH: return "открыть";
            case HOME: return "выйти";
            default: return "показать";
        }
    }

    static int icon(String id) {
        switch (id) {
            case PANEL: return R.drawable.ic_sliders;
            case NIGHT: return R.drawable.ic_moon;
            case SAVER: return R.drawable.ic_image;
            case SLEEP: return R.drawable.ic_timer;
            case BLUETOOTH: return R.drawable.ic_bluetooth;
            default: return R.drawable.ic_home;
        }
    }

    static void run(Context c, String id) {
        Context app = c.getApplicationContext();
        switch (id) {
            case PANEL:
                QuickPanel.toggle(app);
                break;
            case NIGHT:
                Dim.toggleNight(app);
                break;
            case SAVER:
                start(app, new Intent(app, SaverActivity.class));
                break;
            case SLEEP:
                // from a button there is no panel to show the time on
                Toast.makeText(app, sleepLabel(app, nextSleep(app)), Toast.LENGTH_SHORT).show();
                break;
            case BLUETOOTH:
                bluetooth(app);
                break;
            case HOME:
                start(app, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(app.getPackageName()));
                break;
            default:
                Log.w(TAG, "unknown action " + id);
        }
    }

    /**
     * Android TV settings has no page for BLUETOOTH_SETTINGS; its closest one is the accessory search, which lists
     * nearby devices to pair. Some firmwares do have the phone page, so that one goes first.
     */
    static void bluetooth(Context c) {
        if (start(c, new Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) return;
        if (start(c, new Intent("com.google.android.intent.action.CONNECT_INPUT"))) return;
        start(c, new Intent(Settings.ACTION_SETTINGS));
    }

    private static boolean start(Context c, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- sleep timer

    /** When the screen goes black, in epoch ms; 0 when no timer runs. */
    static long sleepAt(Context c) {
        long at = prefs(c).getLong("sleep_at", 0);
        return at > System.currentTimeMillis() ? at : 0;
    }

    /** The next step after the running one (15 → 30 → … → 90 → off). Returns the minutes set, 0 for off. */
    static int nextSleep(Context c) {
        // steps by what was set, not by what is left: 30 minutes into a 45-minute timer, the next press is 60
        int step = sleepAt(c) == 0 ? 0 : prefs(c).getInt("sleep_step", -1) + 1;
        int next = step < SLEEP_STEPS.length ? SLEEP_STEPS[step] : 0;
        prefs(c).edit().putInt("sleep_step", step)
                .putLong("sleep_at", next == 0 ? 0 : System.currentTimeMillis() + next * 60000L).apply();
        KeyWatchService.armSleep(c);
        return next;
    }

    static String sleepLabel(Context c, int minutes) {
        return minutes == 0 ? "Таймер сна" : "Сон через " + minutes + " мин";
    }

    /** The label for the running timer, rounded up to a whole minute. */
    static String sleepLabel(Context c) {
        long at = sleepAt(c);
        return sleepLabel(c, at == 0 ? 0 : (int) Math.ceil((at - System.currentTimeMillis()) / 60000.0));
    }

    /** Testing: the timer runs out in {@code seconds} instead of whole minutes. */
    static void sleepIn(Context c, int seconds) {
        prefs(c).edit().putInt("sleep_step", 0).putLong("sleep_at", System.currentTimeMillis() + seconds * 1000L).apply();
        KeyWatchService.armSleep(c);
    }

    static void clearSleep(Context c) {
        prefs(c).edit().remove("sleep_at").apply();
    }

    // ---- what the quick panel shows

    /** Every item in the panel's order, the hidden ones too. */
    static List<String> panelOrder(Context c) {
        String saved = prefs(c).getString("panel_order", null);
        List<String> out = new ArrayList<>();
        if (saved != null) {
            for (String s : saved.split(",")) if (Arrays.asList(PANEL_ITEMS).contains(s) && !out.contains(s)) out.add(s);
        }
        // items added in a later version go to the end
        for (String s : PANEL_ITEMS) if (!out.contains(s)) out.add(s);
        return out;
    }

    static void setPanelOrder(Context c, List<String> order) {
        prefs(c).edit().putString("panel_order", String.join(",", order)).apply();
    }

    static boolean panelShows(Context c, String id) {
        return !prefs(c).getBoolean("panel_off_" + id, false);
    }

    static void setPanelShows(Context c, String id, boolean on) {
        prefs(c).edit().putBoolean("panel_off_" + id, !on).apply();
    }

    /** Seconds without a key before the panel hides itself. */
    static int panelTimeout(Context c) {
        return prefs(c).getInt("panel_timeout", 5);
    }

    static void setPanelTimeout(Context c, int seconds) {
        prefs(c).edit().putInt("panel_timeout", seconds).apply();
    }

    /** How far one press of ←/→ moves the brightness, in percent. */
    static int brightnessStep(Context c) {
        return prefs(c).getInt("panel_step", 5);
    }

    static void setBrightnessStep(Context c, int percent) {
        prefs(c).edit().putInt("panel_step", percent).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("actions", Context.MODE_PRIVATE);
    }
}
