package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.widget.Toast;

/**
 * The settings items as the quick panel sees them: one round button each, the same as on the settings page.
 * The panel lives in an overlay window over other apps, so it can't call the settings page's own code; what a page
 * of Lagoon opens goes through {@link #OPEN} to Home, the rest runs right here.
 */
final class Items {
    /** Intent extra for {@link MainActivity}: the settings item to open, as if OK was pressed on it. */
    static final String OPEN = "open";

    /**
     * Settings items the panel can take besides its actions. Bluetooth, «Ночь», the sleep timer and the screensaver are
     * actions already; two buttons that do the same in one panel would only confuse.
     */
    static final String[] SETTINGS = {"wifi", "vpn", "sound", "remote", "language", "tv", "refresh", "reboot", "display", "cache",
            "scene", "telegram", "calendar", "city", "storage", "apps", "weather", "clock", "about"};

    private Items() {
    }

    static int icon(String id) {
        switch (id) {
            case "wifi": return R.drawable.ic_wifi;
            case "vpn": return R.drawable.ic_shield;
            case "sound": return R.drawable.ic_volume;
            case "remote": return R.drawable.ic_remote;
            case "language": return R.drawable.ic_languages;
            case "refresh": return R.drawable.ic_cloud;
            case "reboot": return R.drawable.ic_power;
            case "display": return R.drawable.ic_sun_dim;
            case "cache": return R.drawable.ic_trash;
            case "scene": return R.drawable.ic_palm;
            case "telegram": return R.drawable.ic_send;
            case "calendar": return R.drawable.ic_calendar;
            case "city": return R.drawable.ic_pin;
            case "storage": return R.drawable.ic_grid;
            case "apps": return R.drawable.ic_pushpin;
            case "weather": return R.drawable.ic_sun;
            case "clock": return R.drawable.ic_timer;
            default: return R.drawable.ic_monitor;
        }
    }

    static String name(Context c, String id) {
        switch (id) {
            case "wifi": return "Wi-Fi";
            case "bluetooth": return "Bluetooth";
            case "vpn": return "VPN";
            case "sound": return c.getString(R.string.sound);
            case "remote": return c.getString(R.string.remote_buttons);
            case "language": return c.getString(R.string.language);
            case "refresh": return c.getString(R.string.refresh);
            case "reboot": return c.getString(R.string.reboot);
            case "night": return c.getString(R.string.night);
            case "sleep": return c.getString(R.string.act_sleep);
            case "display": return c.getString(R.string.display);
            case "cache": return c.getString(R.string.cache);
            case "saver": return c.getString(R.string.saver_tile);
            case "scene": return c.getString(R.string.scene);
            case "telegram": return "Telegram";
            case "calendar": return c.getString(R.string.calendar);
            case "city": return c.getString(R.string.city);
            case "storage": return c.getString(R.string.storage);
            case "apps": return c.getString(R.string.apps);
            case "weather": return c.getString(R.string.weather);
            case "clock": return c.getString(R.string.clock);
            case "about": return c.getString(R.string.about);
            default: return c.getString(R.string.tv_settings);
        }
    }

    /** The quiet line under the button in the panel: a state worth seeing at a glance, or null. */
    static String state(Context c, String id) {
        switch (id) {
            case "wifi": return c.getString(on(c, id) ? R.string.wifi_on : R.string.wifi_off);
            case "vpn": return on(c, id) ? c.getString(R.string.state_on) : null;
            case "language": return Lang.label(c, Lang.get(c));
            case "clock": return Lang.clock(c, new java.util.Date());
            default: return null;
        }
    }

    /** Switched on: the button is brighter glass and says so in its line. */
    static boolean on(Context c, String id) {
        if ("wifi".equals(id)) return network(c, NetworkCapabilities.TRANSPORT_WIFI);
        if ("vpn".equals(id)) return network(c, NetworkCapabilities.TRANSPORT_VPN);
        return false;
    }

    /** Whether the item makes sense on this TV: VPN only with a VPN app installed. */
    static boolean available(Context c, String id) {
        if (!"vpn".equals(id)) return true;
        for (android.content.pm.ResolveInfo r : c.getPackageManager().queryIntentServices(new Intent("android.net.VpnService"), 0))
            if (!r.serviceInfo.packageName.equals(c.getPackageName())) return true;
        return false;
    }

    /** OK on the item in the quick panel. */
    static void run(Context c, String id) {
        switch (id) {
            case "wifi":
                start(c, new Intent(Settings.ACTION_WIFI_SETTINGS));
                break;
            case "sound":
                start(c, new Intent(Settings.ACTION_SOUND_SETTINGS));
                break;
            case "tv":
                start(c, new Intent(Settings.ACTION_SETTINGS));
                break;
            case "reboot":
                start(c, new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
                break;
            case "display":
                // the stick has one «Display & Sound» page, reached only by the sound action
                if (!start(c, new Intent(Settings.ACTION_DISPLAY_SETTINGS))) start(c, new Intent(Settings.ACTION_SOUND_SETTINGS));
                break;
            case "refresh": {
                // over another app there is no Home to show it: read everything again and say so
                new Weather(c).refresh(true);
                Cal.refreshNow(c, null);
                Saver.syncAsync(c, () -> { });
                Toast.makeText(c, c.getString(R.string.refreshed), Toast.LENGTH_SHORT).show();
                break;
            }
            case "cache": {
                android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
                new Thread(() -> {
                    long freed = Stick.clearCache(c);
                    main.post(() -> Toast.makeText(c, c.getString(R.string.cache_cleared_text, Stick.bytes(c, freed)), Toast.LENGTH_SHORT).show());
                }, "cache").start();
                break;
            }
            default:
                // a page of Lagoon: Home opens it, as OK on the settings page would
                start(c, new Intent(c, MainActivity.class).putExtra(OPEN, id));
        }
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

    private static boolean network(Context c, int transport) {
        try {
            ConnectivityManager cm = c.getSystemService(ConnectivityManager.class);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && nc.hasTransport(transport);
        } catch (Exception e) {
            return false;
        }
    }
}
