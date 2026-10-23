package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Weather, sea and air for one place, from Open-Meteo (no key). Refreshed every 30 minutes on a background
 * thread; the last good answer is kept in preferences so Home has numbers right after a reboot.
 */
final class Weather {
    interface Listener { void onWeather(Weather w); }

    /**
     * Cities to pick without typing: {name, "region, country", lat, lon}. Phuket's point is on the west coast,
     * where the sea temperature is the one that matters.
     */
    static final String[][] CITIES = {
            {"Пхукет", "Таиланд", "7.880", "98.300"}, {"Бангкок", "Таиланд", "13.756", "100.502"},
            {"Паттайя", "Таиланд", "12.927", "100.877"}, {"Самуи", "Таиланд", "9.512", "100.014"},
            {"Чиангмай", "Таиланд", "18.788", "98.985"}, {"Москва", "Россия", "55.756", "37.617"},
            {"Санкт-Петербург", "Россия", "59.939", "30.316"}, {"Дубай", "ОАЭ", "25.205", "55.271"},
            {"Бали", "Индонезия", "-8.650", "115.216"}
    };

    private final Context ctx;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Listener listener;

    JSONObject forecast, marine, air;

    Weather(Context ctx) {
        this.ctx = ctx;
        prefs = ctx.getSharedPreferences("weather", Context.MODE_PRIVATE);
        try {
            if (prefs.contains("forecast")) forecast = new JSONObject(prefs.getString("forecast", "{}"));
            if (prefs.contains("marine")) marine = new JSONObject(prefs.getString("marine", "{}"));
            if (prefs.contains("air")) air = new JSONObject(prefs.getString("air", "{}"));
        } catch (Exception ignored) {
        }
    }

    void setListener(Listener l) {
        listener = l;
        if (forecast != null) l.onWeather(this);
    }

    /** The chosen place: a city from the list or found by search, Phuket by default. */
    String placeName() {
        return prefs.getString("place_name", CITIES[0][0]);
    }

    String placeLat() {
        return prefs.getString("place_lat", CITIES[0][2]);
    }

    String placeLon() {
        return prefs.getString("place_lon", CITIES[0][3]);
    }

    /** Where the place is, "region, country", as the search or {@link #CITIES} gave it. */
    String placeRegion() {
        return prefs.getString("place_region", CITIES[0][1]);
    }

    /** "Ката, Пхукет": the place and its region, the region left out when it is the place itself. */
    String placeTitle() {
        String name = placeName(), region = placeRegion().split(",")[0].trim();
        return region.isEmpty() || region.equalsIgnoreCase(name) ? name : name + ", " + region;
    }

    void setPlace(String name, String region, String lat, String lon) {
        // the one being left goes first in «Недавние»
        remember(placeName(), placeRegion(), placeLat(), placeLon());
        prefs.edit().putString("place_name", name).putString("place_region", region)
                .putString("place_lat", lat).putString("place_lon", lon).remove("at").apply();
        refresh(true);
    }

    /** Places chosen before, newest first, {name, region, lat, lon}, at most three. */
    List<String[]> recent() {
        List<String[]> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString("recent", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONArray p = a.getJSONArray(i);
                out.add(new String[]{p.getString(0), p.getString(1), p.getString(2), p.getString(3)});
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void remember(String name, String region, String lat, String lon) {
        JSONArray a = new JSONArray().put(new JSONArray().put(name).put(region).put(lat).put(lon));
        for (String[] p : recent())
            if (!p[0].equals(name) && a.length() < 3) a.put(new JSONArray().put(p[0]).put(p[1]).put(p[2]).put(p[3]));
        prefs.edit().putString("recent", a.toString()).apply();
    }

    interface Found { void onFound(List<String[]> places); }

    /** City search through Open-Meteo's geocoder: {name, lat, lon, "region, country"} per result. */
    void search(String query, Found done) {
        new Thread(() -> {
            List<String[]> out = new ArrayList<>();
            try {
                JSONObject r = get("https://geocoding-api.open-meteo.com/v1/search?count=10&language=ru&format=json&name="
                        + java.net.URLEncoder.encode(query.trim(), "UTF-8"));
                JSONArray a = r.optJSONArray("results");
                for (int i = 0; a != null && i < a.length(); i++) {
                    JSONObject p = a.getJSONObject(i);
                    String where = p.optString("admin1", "");
                    String country = p.optString("country", "");
                    where = where.isEmpty() ? country : country.isEmpty() ? where : where + ", " + country;
                    out.add(new String[]{p.optString("name"), String.valueOf(p.optDouble("latitude")),
                            String.valueOf(p.optDouble("longitude")), where});
                }
            } catch (Exception e) {
                Log.w("Weather", "search", e);
            }
            main.post(() -> done.onFound(out));
        }, "geocode").start();
    }

    void refresh(boolean force) {
        if (!force && System.currentTimeMillis() - prefs.getLong("at", 0) < 30 * 60_000L) return;
        String lat = placeLat(), lon = placeLon();
        new Thread(() -> {
            try {
                String q = "latitude=" + lat + "&longitude=" + lon + "&timezone=auto";
                JSONObject f = get("https://api.open-meteo.com/v1/forecast?" + q
                        + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,uv_index"
                        + "&hourly=temperature_2m,precipitation_probability,weather_code"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max&forecast_days=6");
                JSONObject m = null, a = null;
                try {
                    m = get("https://marine-api.open-meteo.com/v1/marine?" + q + "&current=sea_surface_temperature,wave_height");
                } catch (Exception e) {
                    Log.w("Weather", "marine", e);
                }
                try {
                    a = get("https://air-quality-api.open-meteo.com/v1/air-quality?" + q + "&current=pm2_5,us_aqi");
                } catch (Exception e) {
                    Log.w("Weather", "air", e);
                }
                JSONObject fm = m, fa = a;
                main.post(() -> {
                    forecast = f;
                    if (fm != null) marine = fm;
                    if (fa != null) air = fa;
                    SharedPreferences.Editor e = prefs.edit().putString("forecast", f.toString()).putLong("at", System.currentTimeMillis());
                    if (fm != null) e.putString("marine", fm.toString());
                    if (fa != null) e.putString("air", fa.toString());
                    e.apply();
                    if (listener != null) listener.onWeather(this);
                });
            } catch (Exception e) {
                Log.w("Weather", "forecast", e);
            }
        }, "weather").start();
    }

    private static JSONObject get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            c.disconnect();
        }
    }

    // ---------------------------------------------------------------- readings

    boolean ready() {
        return forecast != null && forecast.has("current");
    }

    int temp() {
        return (int) Math.round(forecast.optJSONObject("current").optDouble("temperature_2m"));
    }

    int feels() {
        return (int) Math.round(forecast.optJSONObject("current").optDouble("apparent_temperature"));
    }

    int code() {
        return forecast.optJSONObject("current").optInt("weather_code");
    }

    int humidity() {
        return forecast.optJSONObject("current").optInt("relative_humidity_2m");
    }

    String wind() {
        JSONObject c = forecast.optJSONObject("current");
        String[] dirs = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};
        int d = (int) Math.round(c.optDouble("wind_direction_10m") / 45) % 8;
        return dirs[d] + " " + Math.round(c.optDouble("wind_speed_10m") / 3.6) + " м/с";
    }

    double uv() {
        return forecast.optJSONObject("current").optDouble("uv_index");
    }

    Integer sea() {
        if (marine == null || marine.optJSONObject("current") == null) return null;
        double v = marine.optJSONObject("current").optDouble("sea_surface_temperature", Double.NaN);
        return Double.isNaN(v) ? null : (int) Math.round(v);
    }

    String wave() {
        if (marine == null || marine.optJSONObject("current") == null) return null;
        double v = marine.optJSONObject("current").optDouble("wave_height", Double.NaN);
        return Double.isNaN(v) ? null : String.format(Locale.forLanguageTag("ru"), "%.1f м", v);
    }

    Integer pm25() {
        if (air == null || air.optJSONObject("current") == null) return null;
        double v = air.optJSONObject("current").optDouble("pm2_5", Double.NaN);
        return Double.isNaN(v) ? null : (int) Math.round(v);
    }

    /** "HH:mm" of today's sunrise (i = "sunrise") or sunset. */
    String sun(String which) {
        JSONArray a = forecast.optJSONObject("daily").optJSONArray(which);
        String s = a.optString(0);
        return s.length() >= 16 ? s.substring(11, 16) : "";
    }

    int sunMinutes(String which) {
        String s = sun(which);
        if (s.length() != 5) return which.equals("sunrise") ? 6 * 60 + 18 : 18 * 60 + 12;
        return Integer.parseInt(s.substring(0, 2)) * 60 + Integer.parseInt(s.substring(3));
    }

    /** A rain or storm notice for the next six hours, or null. */
    String alert() {
        JSONObject h = forecast.optJSONObject("hourly");
        if (h == null) return null;
        JSONArray time = h.optJSONArray("time"), prob = h.optJSONArray("precipitation_probability"), code = h.optJSONArray("weather_code");
        int now = hourIndex();
        for (int i = now + 1; i < Math.min(time.length(), now + 7); i++) {
            int c = code.optInt(i);
            if (prob.optInt(i) >= 60 && c >= 51) {
                String hh = time.optString(i).substring(11, 16);
                return (c >= 95 ? "Гроза около " : "Дождь около ") + hh;
            }
        }
        return null;
    }

    int hourIndex() {
        JSONArray time = forecast.optJSONObject("hourly").optJSONArray("time");
        Calendar cal = Calendar.getInstance();
        String key = String.format(Locale.US, "%04d-%02d-%02dT%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY));
        for (int i = 0; i < time.length(); i++) if (time.optString(i).startsWith(key)) return i;
        return 0;
    }

    static String describe(int code) {
        if (code == 0) return "ясно";
        if (code <= 2) return "переменная облачность";
        if (code == 3) return "пасмурно";
        if (code <= 48) return "туман";
        if (code <= 57) return "морось";
        if (code <= 67) return "дождь";
        if (code <= 77) return "снег";
        if (code <= 82) return "ливни";
        if (code <= 86) return "снегопад";
        return "гроза";
    }

    /** When the numbers on screen were fetched, ms; 0 when never. */
    long updatedAt() {
        return prefs.getLong("at", 0);
    }

    /**
     * The next hours for the weather screen, {time, temperature, what falls}: now and then every {@code step}
     * hours. The third field is "гроза" or "дождь" only where one is likely, else empty.
     */
    List<String[]> hours(int n, int step) {
        List<String[]> out = new ArrayList<>();
        JSONObject h = forecast == null ? null : forecast.optJSONObject("hourly");
        if (h == null) return out;
        JSONArray time = h.optJSONArray("time"), temp = h.optJSONArray("temperature_2m"),
                prob = h.optJSONArray("precipitation_probability"), code = h.optJSONArray("weather_code");
        int now = hourIndex();
        for (int k = 0; k < n; k++) {
            int i = now + k * step;
            if (i >= time.length()) break;
            int c = code.optInt(i);
            String falls = c >= 95 ? "гроза" : prob.optInt(i) >= 60 && c >= 51 ? "дождь" : "";
            out.add(new String[]{k == 0 ? "сейчас" : time.optString(i).substring(11, 16),
                    Math.round(temp.optDouble(i)) + "°", falls});
        }
        return out;
    }

    /** The coming days, {day, sky, "max° / min°"}, starting tomorrow. */
    List<String[]> days(int n) {
        List<String[]> out = new ArrayList<>();
        JSONObject d = forecast == null ? null : forecast.optJSONObject("daily");
        if (d == null) return out;
        JSONArray time = d.optJSONArray("time"), code = d.optJSONArray("weather_code"),
                max = d.optJSONArray("temperature_2m_max"), min = d.optJSONArray("temperature_2m_min");
        String[] wd = {"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
        for (int i = 1; i <= n && i < time.length(); i++) {
            String day = i == 1 ? "Завтра" : "";
            if (day.isEmpty()) {
                try {
                    java.util.Date dt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(time.optString(i));
                    Calendar c = Calendar.getInstance();
                    c.setTime(dt);
                    day = wd[c.get(Calendar.DAY_OF_WEEK) - 1];
                } catch (Exception e) {
                    day = time.optString(i);
                }
            }
            out.add(new String[]{day, describe(code.optInt(i)), Math.round(max.optDouble(i)) + "° / " + Math.round(min.optDouble(i)) + "°"});
        }
        return out;
    }

    /** The line under the clock. */
    String line() {
        if (!ready()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(temp()).append("° ").append(describe(code()));
        Integer s = sea();
        if (s != null) sb.append(" · море ").append(s).append('°');
        double uv = uv();
        if (uv >= 6) sb.append(" · UV ").append(Math.round(uv));
        return sb.toString();
    }
}
