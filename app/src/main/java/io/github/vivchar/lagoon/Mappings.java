package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Remote button (keycode) -> what a tap and a hold do: an app's package, or one of {@link Actions} ("@panel").
 * Stored in SharedPreferences.
 */
final class Mappings {
    static final int NETFLIX = 193;   // KEYCODE_BUTTON_6 on the Xiaomi RC
    static final int PRIME = 194;     // KEYCODE_BUTTON_7

    private static final String PREFS = "mappings";
    private static final String CODES = "codes";

    private final SharedPreferences prefs;

    Mappings(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(CODES)) {
            // First run: the button that used to open YouTube through TV Quick Actions.
            prefs.edit().putString(CODES, NETFLIX + "," + PRIME)
                    .putString("tap_" + NETFLIX, "com.google.android.youtube.tv")
                    .putString("tap_" + PRIME, "mobi.zona").apply();
        }
        if (!prefs.getBoolean("panel_hold", false)) {
            // once, for installs from before the quick panel: holding Prime opens it, unless that hold was taken
            SharedPreferences.Editor e = prefs.edit().putBoolean("panel_hold", true);
            if (prefs.getString("hold_" + PRIME, null) == null) e.putString("hold_" + PRIME, Actions.PANEL);
            e.apply();
        }
    }

    List<Integer> codes() {
        TreeSet<Integer> set = new TreeSet<>();
        for (String s : prefs.getString(CODES, "").split(",")) {
            if (!s.isEmpty()) set.add(Integer.parseInt(s));
        }
        return new ArrayList<>(set);
    }

    boolean has(int code) {
        return codes().contains(code);
    }

    String tap(int code) {
        return prefs.getString("tap_" + code, null);
    }

    String hold(int code) {
        return prefs.getString("hold_" + code, null);
    }

    void add(int code) {
        List<Integer> codes = codes();
        if (!codes.contains(code)) codes.add(code);
        save(codes);
    }

    void remove(int code) {
        List<Integer> codes = codes();
        codes.remove(Integer.valueOf(code));
        save(codes);
        prefs.edit().remove("tap_" + code).remove("hold_" + code).apply();
    }

    void set(int code, boolean hold, String pkg) {
        String key = (hold ? "hold_" : "tap_") + code;
        if (pkg == null) prefs.edit().remove(key).apply();
        else prefs.edit().putString(key, pkg).apply();
    }

    private void save(List<Integer> codes) {
        StringBuilder sb = new StringBuilder();
        for (int c : codes) {
            if (sb.length() > 0) sb.append(',');
            sb.append(c);
        }
        prefs.edit().putString(CODES, sb.toString()).apply();
    }

    static String name(int code) {
        if (code == NETFLIX) return "Netflix";
        if (code == PRIME) return "Prime Video";
        String name = android.view.KeyEvent.keyCodeToString(code);
        return name.startsWith("KEYCODE_") ? name.substring(8) : name;
    }
}
