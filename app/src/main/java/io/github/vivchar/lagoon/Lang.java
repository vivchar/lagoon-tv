package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.res.Configuration;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The interface language. Android 11 has no per-app locale, so the choice lives in SharedPreferences and every
 * context that builds text is wrapped with {@link #wrap}. Nothing chosen means Russian, whatever the TV's own
 * language is: the first owner's system is English and his screens must not change on update.
 */
final class Lang {
    /** The choice that follows the TV's language. */
    static final String SYSTEM = "system";
    static final String DEFAULT = "ru";
    /** Language codes in the order of the picker; each is named in itself. */
    static final String[] CODES = {"ru", "en", "th", "zh", "es", "de"};
    static final String[] NAMES = {"Русский", "English", "ไทย", "中文", "Español", "Deutsch"};

    private Lang() {
    }

    /** The stored choice: a code from {@link #CODES} or {@link #SYSTEM}. */
    static String get(Context c) {
        return c.getSharedPreferences("lang", Context.MODE_PRIVATE).getString("code", DEFAULT);
    }

    static void set(Context c, String code) {
        c.getSharedPreferences("lang", Context.MODE_PRIVATE).edit().putString("code", code).apply();
    }

    /** What the picker shows for the choice: the language in itself, or the system's word for "as in system". */
    static String label(Context c, String code) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return NAMES[i];
        return c.getString(R.string.lang_system);
    }

    /** The language in effect: the choice, or for {@link #SYSTEM} the TV's language if it is supported, else English. */
    static String effective(Context c) {
        String code = get(c);
        return SYSTEM.equals(code) ? ofSystem() : code;
    }

    /** What "as in system" gives: the TV's language when it is one of ours, English otherwise. */
    static String ofSystem() {
        String sys = android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
        for (String s : CODES) if (s.equals(sys)) return s;
        return "en";
    }

    static Locale locale(Context c) {
        return Locale.forLanguageTag("zh".equals(effective(c)) ? "zh-CN" : effective(c));
    }

    /**
     * A context whose resources are in the chosen language. Always wrap the base of an activity or service, and
     * the application context before it builds text; a wrapped context stays wrapped, so it is cheap to repeat.
     */
    static Context wrap(Context base) {
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        Locale l = locale(base);
        cfg.setLocale(l);
        cfg.setLayoutDirection(l);
        return base.createConfigurationContext(cfg);
    }

    /**
     * Letter-spacing for spaced capitals. Thai stacks vowel and tone marks over the letters and spacing tears
     * them apart; Chinese needs only a little.
     */
    static float tracking(Context c, float spacing) {
        String l = effective(c);
        return "th".equals(l) ? 0 : "zh".equals(l) ? spacing / 3 : spacing;
    }

    /** A date by a skeleton ("EEEEdMMMM"), in the order and punctuation of the language. */
    static String date(Context c, String skeleton, Date d) {
        Locale l = locale(c);
        return new SimpleDateFormat(DateFormat.getBestDateTimePattern(l, skeleton), l).format(d);
    }

    /** HH:mm in the chosen language's digits. */
    static String clock(Context c, Date d) {
        return new SimpleDateFormat(h12(c) ? "h:mm" : "HH:mm", locale(c)).format(d);
    }

    // ---- the clock page

    /** Whether the clocks run 12 hours; without AM/PM, as a wall clock: the bay already says whether it is night. */
    static boolean h12(Context c) {
        return c.getSharedPreferences("lang", Context.MODE_PRIVATE).getBoolean("h12", false);
    }

    static void setH12(Context c, boolean on) {
        c.getSharedPreferences("lang", Context.MODE_PRIVATE).edit().putBoolean("h12", on).apply();
    }

    /** Time zones the second clock offers: where the family and friends are. */
    static final String[] ZONES = {"Europe/Moscow", "Europe/London", "America/New_York"};

    /** The second clock's zone, or null when it is off. */
    static String secondZone(Context c) {
        return c.getSharedPreferences("lang", Context.MODE_PRIVATE).getString("zone2", null);
    }

    static void setSecondZone(Context c, String zone) {
        c.getSharedPreferences("lang", Context.MODE_PRIVATE).edit().putString("zone2", zone).apply();
    }

    static String zoneName(Context c, String zone) {
        switch (zone) {
            case "Europe/Moscow": return c.getString(R.string.zone_moscow);
            case "Europe/London": return c.getString(R.string.zone_london);
            default: return c.getString(R.string.zone_new_york);
        }
    }

    /** "МОСКВА 15:56" for the line under the date; null when the second clock is off. */
    static String second(Context c, Date d) {
        String zone = secondZone(c);
        if (zone == null) return null;
        SimpleDateFormat f = new SimpleDateFormat(h12(c) ? "h:mm" : "HH:mm", locale(c));
        f.setTimeZone(java.util.TimeZone.getTimeZone(zone));
        return zoneName(c, zone) + " " + f.format(d);
    }
}
