package io.github.vivchar.lagoon;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.os.Build;
import android.os.StatFs;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.Inet4Address;

/** What the «О стике» and «Хранилище» pages read about the device. Everything here touches files: call off the main thread. */
final class Stick {
    private Stick() {
    }

    /** Bytes under {@code f}, files and folders alike. */
    static long size(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long n = 0;
        File[] all = f.listFiles();
        if (all != null) for (File x : all) n += size(x);
        return n;
    }

    /** Removes what is inside {@code dir}, keeps the folder. Returns the bytes freed. */
    static long clear(File dir) {
        long freed = 0;
        File[] all = dir == null ? null : dir.listFiles();
        if (all == null) return 0;
        for (File x : all) {
            if (x.isDirectory()) {
                freed += clear(x);
                if (x.delete()) continue;
            } else {
                long n = x.length();
                if (x.delete()) freed += n;
            }
        }
        return freed;
    }

    /** What «Очистить кеш» removes: thumbnails and temporary files, all of which come back by themselves. */
    static long cache(Context c) {
        return size(c.getCacheDir());
    }

    static long clearCache(Context c) {
        return clear(c.getCacheDir());
    }

    /** {free, total} bytes of the partition apps live on. */
    static long[] disk(Context c) {
        StatFs s = new StatFs(c.getFilesDir().getAbsolutePath());
        return new long[]{s.getAvailableBytes(), s.getTotalBytes()};
    }

    /** The stick's IPv4 address in the local network, or null without one. */
    static String ip(Context c) {
        try {
            ConnectivityManager cm = c.getSystemService(ConnectivityManager.class);
            LinkProperties lp = cm.getLinkProperties(cm.getActiveNetwork());
            if (lp == null) return null;
            for (LinkAddress a : lp.getLinkAddresses()) if (a.getAddress() instanceof Inet4Address) return a.getAddress().getHostAddress();
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Minutes since the stick started. */
    static long uptimeMinutes() {
        return SystemClock.elapsedRealtime() / 60_000;
    }

    /** The SoC temperature in °C, or null where the firmware hides it. */
    static Integer temperature() {
        for (int i = 0; i < 4; i++) {
            try (BufferedReader r = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone" + i + "/temp"))) {
                int v = Integer.parseInt(r.readLine().trim());
                // most kernels report millidegrees, a few whole degrees
                int t = v > 1000 ? v / 1000 : v;
                if (t > 0 && t < 150) return t;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    static String model() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    static String version(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** "1,2 ГБ": the short size Android uses in its own settings, in the interface's language. */
    static String bytes(Context c, long n) {
        return android.text.format.Formatter.formatShortFileSize(c, n);
    }
}
