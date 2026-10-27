package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The installed TV apps and their order on the shelf. Pinned apps stand together at the left, in the order
 * they were put in; every other app follows them by how much it is used, the most used first.
 */
final class Apps {
    static final class App {
        final String pkg;
        final String label;
        final ActivityInfo info;

        App(String pkg, String label, ActivityInfo info) {
            this.pkg = pkg;
            this.label = label;
            this.info = info;
        }
    }

    // Banners are loaded only for tiles on screen and shrunk to tile size: other apps ship them as large
    // as 1280×720, and holding all of them at full size cost tens of megabytes on a 2 GB stick.
    private static final Map<String, Drawable> art = new HashMap<>();

    /** The app's TV banner shrunk to tile size, or null when it has none. Cached per package. */
    Drawable banner(App a) {
        String key = "b:" + a.pkg;
        if (art.containsKey(key)) return art.get(key);
        PackageManager pm = ctx.getPackageManager();
        Drawable d = a.info.loadBanner(pm);
        if (d == null) d = a.info.applicationInfo.loadBanner(pm);
        Drawable small = d == null ? null : shrink(d, 400, 225);
        art.put(key, small);
        return small;
    }

    Drawable icon(App a) {
        String key = "i:" + a.pkg;
        if (art.containsKey(key)) return art.get(key);
        Drawable small = shrink(a.info.loadIcon(ctx.getPackageManager()), 96, 96);
        art.put(key, small);
        return small;
    }

    private Drawable shrink(Drawable d, int w, int h) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return new BitmapDrawable(ctx.getResources(), b);
    }

    private final Context ctx;
    private final SharedPreferences prefs;

    Apps(Context ctx) {
        this.ctx = ctx;
        this.prefs = ctx.getSharedPreferences("apps", Context.MODE_PRIVATE);
    }

    List<App> installed() {
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> ri = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        List<App> out = new ArrayList<>();
        for (ResolveInfo r : ri) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName())) continue;
            out.add(new App(pkg, r.loadLabel(pm).toString(), r.activityInfo));
        }
        return out;
    }

    /** All apps in shelf order: the pinned ones first. */
    List<App> ordered() {
        List<App> all = installed();
        // the system's own record sees every way an app was opened (remote buttons, other apps), the last
        // 30 days; our counters cover only launches from here and fill in when access is missing
        Map<String, android.app.usage.UsageStats> use = usage();
        Comparator<App> cmp = (a, b) -> Long.compare(timeUsed(b, use), timeUsed(a, use));
        List<App> pins = new ArrayList<>(), rest = new ArrayList<>();
        for (App a : all) if (!hidden(a.pkg)) (pinned(a.pkg) ? pins : rest).add(a);
        pins.sort(Comparator.<App>comparingInt(a -> prefs.getInt("pos_" + a.pkg, 0)).thenComparing(a -> a.label.toLowerCase()));
        rest.sort(cmp.thenComparing(a -> a.label.toLowerCase()));
        pins.addAll(rest);
        return pins;
    }

    /** How many apps are pinned: they take the first places. */
    int pinnedCount() {
        int n = 0;
        for (App a : installed()) if (pinned(a.pkg)) n++;
        return n;
    }

    private Map<String, android.app.usage.UsageStats> usage() {
        try {
            android.app.usage.UsageStatsManager m = (android.app.usage.UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            return m.queryAndAggregateUsageStats(now - 30L * 86400_000L, now);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /** Minutes on screen in 30 days; without the system's record, one launch from here counts as a minute. */
    private long timeUsed(App a, Map<String, android.app.usage.UsageStats> use) {
        android.app.usage.UsageStats u = use.get(a.pkg);
        long ms = u == null ? 0 : u.getTotalTimeInForeground();
        return ms > 0 ? ms / 60_000 + 1 : prefs.getInt("count_" + a.pkg, 0);
    }

    /** Pins the app at place {@code pos} among the pinned ones (at most right after them); the others shift. */
    void moveTo(String pkg, int pos) {
        List<String> pins = new ArrayList<>();
        for (App a : ordered()) if (pinned(a.pkg) && !a.pkg.equals(pkg)) pins.add(a.pkg);
        pins.add(Math.max(0, Math.min(pins.size(), pos)), pkg);
        SharedPreferences.Editor e = prefs.edit();
        for (int i = 0; i < pins.size(); i++) e.putInt("pos_" + pins.get(i), i);
        e.apply();
    }

    /** Apps kept out of the dock, by name. */
    List<App> hiddenApps() {
        List<App> out = new ArrayList<>();
        for (App a : installed()) if (hidden(a.pkg)) out.add(a);
        out.sort(Comparator.comparing(a -> a.label.toLowerCase()));
        return out;
    }

    boolean hidden(String pkg) {
        return prefs.getBoolean("hide_" + pkg, false);
    }

    void setHidden(String pkg, boolean on) {
        if (on) prefs.edit().putBoolean("hide_" + pkg, true).apply();
        else prefs.edit().remove("hide_" + pkg).apply();
    }

    /** Forgets the launches counted here; the system's own record of use stays, it is not ours to clear. */
    void resetCounts() {
        SharedPreferences.Editor e = prefs.edit();
        for (String k : prefs.getAll().keySet()) if (k.startsWith("count_") || k.startsWith("last_")) e.remove(k);
        e.apply();
    }

    void unpin(String pkg) {
        prefs.edit().remove("pos_" + pkg).apply();
    }

    boolean pinned(String pkg) {
        return prefs.contains("pos_" + pkg);
    }

    void launched(String pkg) {
        prefs.edit().putInt("count_" + pkg, prefs.getInt("count_" + pkg, 0) + 1)
                .putLong("last_" + pkg, System.currentTimeMillis()).apply();
    }

    boolean open(String pkg) {
        PackageManager pm = ctx.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(i);
            launched(pkg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
