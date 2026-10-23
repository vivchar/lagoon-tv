package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Today's events from Google calendars, read through their secret iCal addresses (Google Calendar > settings
 * of a calendar > "Secret address in iCal format"). No sign-in: a link is sent to the Telegram bot and the
 * calendar appears in the settings. Each calendar can be hidden; Home shows the important events (those with a
 * reminder), all of today's, or none. Read every 30 minutes; events that are over drop off.
 */
final class Cal {
    static final int IMPORTANT = 0, ALL = 1, OFF = 2;
    static final int[] COLORS = {0xFF8EC9FF, 0xFFFF9F7A, 0xFFC9A7FF, 0xFF7FD1AE, 0xFFFFD27A, 0xFFE57399};
    private static final Pattern LINK = Pattern.compile("(?:https?|webcal)://\\S+?(?:\\.ics|/basic\\.ics)(?:\\?\\S*)?(?=\\s|$)|https?://calendar\\.google\\.com/calendar/ical/\\S+|(?:https?|webcal)://[\\w.-]*icloud\\.com/published/\\S+");
    private static final Handler main = new Handler(Looper.getMainLooper());

    static final class Event {
        long start, end;
        boolean allDay, important;
        String title;
        int color;
    }

    static final class Source {
        String url, name;
        int color;
        boolean shown;
    }

    private Cal() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("calendar", Context.MODE_PRIVATE);
    }

    /** The calendar address in a message, or null. */
    static String linkIn(String text) {
        Matcher m = LINK.matcher(text == null ? "" : text);
        return m.find() ? m.group().replaceFirst("^webcal://", "https://") : null;
    }

    static int mode(Context c) {
        return prefs(c).getInt("mode", IMPORTANT);
    }

    static void setMode(Context c, int m) {
        prefs(c).edit().putInt("mode", m).apply();
    }

    static List<Source> sources(Context c) {
        List<Source> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("sources", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Source s = new Source();
                s.url = o.getString("url");
                s.name = o.optString("name", "Календарь");
                s.color = o.optInt("color", COLORS[i % COLORS.length]);
                s.shown = o.optBoolean("shown", true);
                out.add(s);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void save(Context c, List<Source> list) {
        JSONArray a = new JSONArray();
        try {
            for (Source s : list) a.put(new JSONObject().put("url", s.url).put("name", s.name).put("color", s.color).put("shown", s.shown));
        } catch (Exception ignored) {
        }
        prefs(c).edit().putString("sources", a.toString()).remove("at").apply();
    }

    /** Adds a calendar by its address; {@code done} gets its name, or null when it could not be read. */
    static void add(Context c, String url, java.util.function.Consumer<String> done) {
        Context app = c.getApplicationContext();
        new Thread(() -> {
            String name = null;
            try {
                List<String> lines = fetch(url);
                name = "Календарь";
                for (String l : lines) if (l.startsWith("X-WR-CALNAME:")) name = l.substring(13).trim();
                List<Source> list = sources(app);
                list.removeIf(s -> s.url.equals(url));
                Source s = new Source();
                s.url = url;
                s.name = name;
                s.color = COLORS[list.size() % COLORS.length];
                s.shown = true;
                list.add(s);
                save(app, list);
                refresh(app, null);
            } catch (Exception e) {
                Log.w("Cal", "add", e);
            }
            String n = name;
            if (done != null) done.accept(n);
        }, "calendar").start();
    }

    static void setShown(Context c, String url, boolean shown) {
        List<Source> list = sources(c);
        for (Source s : list) if (s.url.equals(url)) s.shown = shown;
        save(c, list);
    }

    static void remove(Context c, String url) {
        List<Source> list = sources(c);
        list.removeIf(s -> s.url.equals(url));
        save(c, list);
    }

    /** Today's events still ahead (or going on), important ones only unless the mode says all. */
    static List<Event> today(Context c) {
        List<Event> out = new ArrayList<>();
        int mode = mode(c);
        if (mode == OFF) return out;
        long now = System.currentTimeMillis();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("today", "[]"));
            String day = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            if (!day.equals(prefs(c).getString("day", ""))) return out;
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Event e = new Event();
                e.start = o.getLong("start");
                e.end = o.getLong("end");
                e.allDay = o.getBoolean("allDay");
                e.important = o.getBoolean("important");
                e.title = o.getString("title");
                e.color = o.getInt("color");
                if (e.end <= now && !e.allDay) continue;
                if (mode == IMPORTANT && !e.important) continue;
                out.add(e);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** Reads every shown calendar again when the last read is 30 minutes old, or {@code force}. */
    static void refreshIfDue(Context c, Runnable done) {
        long at = prefs(c).getLong("at", 0);
        String day = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        if (System.currentTimeMillis() - at < 30 * 60_000L && day.equals(prefs(c).getString("day", ""))) return;
        Context app = c.getApplicationContext();
        new Thread(() -> refresh(app, done), "calendar").start();
    }

    private static void refresh(Context c, Runnable done) {
        List<Event> all = new ArrayList<>();
        for (Source s : sources(c)) {
            if (!s.shown) continue;
            try {
                all.addAll(parse(fetch(s.url), s.color));
            } catch (Exception e) {
                Log.w("Cal", "read " + s.name, e);
            }
        }
        all.sort((a, b) -> a.allDay != b.allDay ? (a.allDay ? -1 : 1) : Long.compare(a.start, b.start));
        JSONArray a = new JSONArray();
        try {
            for (Event e : all) a.put(new JSONObject().put("start", e.start).put("end", e.end).put("allDay", e.allDay)
                    .put("important", e.important).put("title", e.title).put("color", e.color));
        } catch (Exception ignored) {
        }
        prefs(c).edit().putString("today", a.toString()).putLong("at", System.currentTimeMillis())
                .putString("day", new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date())).apply();
        if (done != null) main.post(done);
    }

    private static List<String> fetch(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) {
                // folded lines continue with a space or a tab
                if ((l.startsWith(" ") || l.startsWith("\t")) && !lines.isEmpty()) lines.set(lines.size() - 1, lines.get(lines.size() - 1) + l.substring(1));
                else lines.add(l);
            }
        } finally {
            conn.disconnect();
        }
        return lines;
    }

    /** The events of an iCal file that happen today, recurring ones included (daily, weekly, monthly, yearly). */
    static List<Event> parse(List<String> lines, int color) {
        List<Event> out = new ArrayList<>();
        Calendar today = Calendar.getInstance();
        String key = new SimpleDateFormat("yyyyMMdd", Locale.US).format(today.getTime());
        String title = null, start = null, end = null, rrule = null, status = null;
        boolean alarm = false, in = false;
        for (String l : lines) {
            if (l.equals("BEGIN:VEVENT")) {
                in = true;
                title = start = end = rrule = status = null;
                alarm = false;
            } else if (l.equals("END:VEVENT") && in) {
                in = false;
                if ("CANCELLED".equals(status) || start == null) continue;
                Event e = occurrence(start, end, rrule, today, key);
                if (e == null) continue;
                e.title = title == null ? "Событие" : unescape(title);
                e.important = alarm;
                e.color = color;
                out.add(e);
            } else if (in) {
                if (l.startsWith("SUMMARY")) title = value(l);
                else if (l.startsWith("DTSTART")) start = l;
                else if (l.startsWith("DTEND")) end = l;
                else if (l.startsWith("RRULE:")) rrule = l.substring(6);
                else if (l.startsWith("STATUS:")) status = l.substring(7).trim();
                else if (l.equals("BEGIN:VALARM")) alarm = true;
            }
        }
        return out;
    }

    private static String value(String line) {
        int i = line.indexOf(':');
        return i < 0 ? "" : line.substring(i + 1);
    }

    private static String unescape(String s) {
        return s.replace("\\,", ",").replace("\\;", ";").replace("\\n", " ").replace("\\N", " ").replace("\\\\", "\\").trim();
    }

    /** {start ms, all-day} of a DTSTART/DTEND line, in its own time zone. */
    private static long[] time(String line) throws Exception {
        String v = value(line).trim();
        if (v.length() == 8) {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
            return new long[]{f.parse(v).getTime(), 1};
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
        if (v.endsWith("Z")) {
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            v = v.substring(0, v.length() - 1);
        } else {
            Matcher m = Pattern.compile("TZID=([^:;]+)").matcher(line);
            if (m.find()) f.setTimeZone(TimeZone.getTimeZone(m.group(1)));
        }
        return new long[]{f.parse(v).getTime(), 0};
    }

    /** Today's occurrence of the event, or null when it does not happen today. */
    private static Event occurrence(String startLine, String endLine, String rrule, Calendar today, String key) {
        try {
            long[] s = time(startLine);
            long[] e = endLine == null ? null : time(endLine);
            long length = e == null ? (s[1] == 1 ? 24 * 3600_000L : 3600_000L) : e[0] - s[0];
            Calendar st = Calendar.getInstance();
            st.setTimeInMillis(s[0]);
            SimpleDateFormat day = new SimpleDateFormat("yyyyMMdd", Locale.US);
            boolean match;
            if (rrule == null) {
                match = day.format(st.getTime()).equals(key);
            } else {
                if (day.format(st.getTime()).compareTo(key) > 0) return null;
                Matcher u = Pattern.compile("UNTIL=(\\d{8})").matcher(rrule);
                if (u.find() && u.group(1).compareTo(key) < 0) return null;
                Matcher f = Pattern.compile("FREQ=(\\w+)").matcher(rrule);
                String freq = f.find() ? f.group(1) : "";
                switch (freq) {
                    case "DAILY":
                        match = true;
                        break;
                    case "WEEKLY": {
                        Matcher by = Pattern.compile("BYDAY=([A-Z,]+)").matcher(rrule);
                        String[] names = {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
                        String todayName = names[today.get(Calendar.DAY_OF_WEEK) - 1];
                        match = by.find() ? by.group(1).contains(todayName) : st.get(Calendar.DAY_OF_WEEK) == today.get(Calendar.DAY_OF_WEEK);
                        break;
                    }
                    case "MONTHLY":
                        match = st.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
                        break;
                    case "YEARLY":
                        match = st.get(Calendar.MONTH) == today.get(Calendar.MONTH) && st.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
                        break;
                    default:
                        match = false;
                }
            }
            if (!match) return null;
            Event ev = new Event();
            ev.allDay = s[1] == 1;
            Calendar on = (Calendar) today.clone();
            if (ev.allDay) {
                on.set(Calendar.HOUR_OF_DAY, 0);
                on.set(Calendar.MINUTE, 0);
            } else {
                on.set(Calendar.HOUR_OF_DAY, st.get(Calendar.HOUR_OF_DAY));
                on.set(Calendar.MINUTE, st.get(Calendar.MINUTE));
            }
            on.set(Calendar.SECOND, 0);
            on.set(Calendar.MILLISECOND, 0);
            ev.start = on.getTimeInMillis();
            ev.end = ev.start + length;
            return ev;
        } catch (Exception ex) {
            return null;
        }
    }
}
