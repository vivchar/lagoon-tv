package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The screensaver's settings and photos. The photos come from a public iCloud shared album (Photos > shared
 * album > People > Public Website) mirrored into the app's own folder: new photos are downloaded, removed ones
 * deleted, at most every SYNC_MS. Uses the same unauthenticated endpoints as icloud.com/sharedalbum.
 * The album link can be changed by sending it to the Telegram bot.
 */
final class Saver {
    static final int ONE = 0, TWO = 1, THREE = 2, AUTO = 3;
    static final int[] EVERY = {10, 30, 60};
    /** The owner's album, the one Photo Prints showed. */
    private static final String DEFAULT_ALBUM = "D25v3vQb_o7-J1zsaZdUnUlap0gCAEQARogV1Btv6FZ414IjGMKbcKUKRhR2gr5yGIWqP0DfiqHNnA";
    private static final String TAG = "Saver";
    private static final long SYNC_MS = 10 * 60 * 1000;
    private static final int MAX_SIDE = 2048;
    private static final Pattern LINK = Pattern.compile("icloud\\.com/sharedalbum/[^#\\s]*#([A-Za-z0-9_-]{10,})");
    private static final Pattern NAME_DATE = Pattern.compile("-(\\d{4})(\\d{2})(\\d{2})?\\.[A-Za-z]+$");
    private static long lastSync;
    private static boolean syncing;

    private Saver() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("saver", Context.MODE_PRIVATE);
    }

    static String album(Context c) {
        return prefs(c).getString("album", DEFAULT_ALBUM);
    }

    /** The shared album's name, once the first sync has read it. */
    static String albumName(Context c) {
        return prefs(c).getString("album_name", "");
    }

    static long syncedAt(Context c) {
        return prefs(c).getLong("synced", 0);
    }

    /** The token in a shared album link, or null when {@code text} holds none. */
    static String tokenIn(String text) {
        Matcher m = LINK.matcher(text == null ? "" : text);
        return m.find() ? m.group(1) : null;
    }

    static void setAlbum(Context c, String token) {
        prefs(c).edit().putString("album", token).remove("album_name").remove("synced").apply();
        lastSync = 0;
        syncAsync(c, () -> { });
    }

    static int layout(Context c) {
        return prefs(c).getInt("layout", AUTO);
    }

    static void setLayout(Context c, int l) {
        prefs(c).edit().putInt("layout", l).apply();
    }

    /** Seconds between photo changes. */
    static int every(Context c) {
        return prefs(c).getInt("every", 30);
    }

    static void setEvery(Context c, int s) {
        prefs(c).edit().putInt("every", s).apply();
    }

    static File folder(Context c) {
        return new File(c.getFilesDir(), "album");
    }

    /** The mirrored photos, in the album's order. */
    static List<File> photos(Context c) {
        List<File> out = new ArrayList<>();
        File[] files = folder(c).listFiles();
        if (files == null) return out;
        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        for (File f : files) if (f.isFile() && !f.getName().startsWith(".")) out.add(f);
        return out;
    }

    /** {year, month} from the file name the sync gave, or null. */
    static int[] date(File f) {
        Matcher m = NAME_DATE.matcher(f.getName());
        if (!m.find()) return null;
        int year = Integer.parseInt(m.group(1)), month = Integer.parseInt(m.group(2));
        return year >= 1900 && month >= 1 && month <= 12 ? new int[]{year, month} : null;
    }

    // ---------------------------------------------------------------- the iCloud mirror

    static synchronized void syncAsync(Context context, Runnable done) {
        String token = album(context).trim();
        if (token.isEmpty() || syncing || System.currentTimeMillis() - lastSync < SYNC_MS) return;
        syncing = true;
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                sync(app, token);
                lastSync = System.currentTimeMillis();
                prefs(app).edit().putLong("synced", lastSync).apply();
            } catch (Exception e) {
                Log.w(TAG, "iCloud sync", e);
            } finally {
                syncing = false;
                done.run();
            }
        }, "icloud").start();
    }

    private static void sync(Context c, String token) throws Exception {
        String host = "p23-sharedstreams.icloud.com";
        JSONObject stream;
        try {
            stream = post(host, token, "webstream", "{\"streamCtag\":null}");
        } catch (Redirect r) {
            host = r.host;
            stream = post(host, token, "webstream", "{\"streamCtag\":null}");
        }
        String name = stream.optString("streamName", "");
        if (!name.isEmpty()) prefs(c).edit().putString("album_name", name).apply();
        JSONArray photos = stream.getJSONArray("photos");
        JSONArray guids = new JSONArray();
        for (int i = 0; i < photos.length(); i++) guids.put(photos.getJSONObject(i).getString("photoGuid"));
        JSONObject assets = post(host, token, "webasseturls", new JSONObject().put("photoGuids", guids).toString());
        JSONObject items = assets.getJSONObject("items");
        JSONObject locations = assets.getJSONObject("locations");

        File dir = folder(c);
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        Set<String> keep = new HashSet<>();
        for (int i = 0; i < photos.length(); i++) {
            JSONObject p = photos.getJSONObject(i);
            if (!"image".equals(p.optString("mediaAssetType", "image"))) continue;   // videos are skipped
            String guid = p.getString("photoGuid");
            String date = p.optString("dateCreated", "").replaceAll("[^0-9]", "");
            String file = guid + (date.length() >= 8 ? "-" + date.substring(0, 8) : "") + ".jpg";
            keep.add(file);
            File f = new File(dir, file);
            if (f.isFile()) continue;
            JSONObject best = best(p.getJSONObject("derivatives"));
            if (best == null) continue;
            JSONObject item = items.optJSONObject(best.getString("checksum"));
            if (item == null) continue;
            JSONObject loc = locations.getJSONObject(item.getString("url_location"));
            String url = "https://" + loc.getJSONArray("hosts").getString(0) + item.getString("url_path");
            File tmp = new File(dir, "." + file);   // dot files are skipped, so a half-written photo never shows
            download(url, tmp);
            if (!tmp.renameTo(f)) tmp.delete();
        }
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) if (f.isFile() && !keep.contains(f.getName())) f.delete();
    }

    /** The biggest derivative no wider or taller than MAX_SIDE, else the smallest one. */
    private static JSONObject best(JSONObject derivatives) throws Exception {
        JSONObject best = null, smallest = null;
        for (Iterator<String> it = derivatives.keys(); it.hasNext(); ) {
            JSONObject d = derivatives.getJSONObject(it.next());
            int side = Math.max(d.optInt("width"), d.optInt("height"));
            long size = d.optLong("fileSize");
            if (side <= MAX_SIDE && (best == null || size > best.optLong("fileSize"))) best = d;
            if (smallest == null || size < smallest.optLong("fileSize")) smallest = d;
        }
        return best != null ? best : smallest;
    }

    private static final class Redirect extends Exception {
        final String host;

        Redirect(String host) {
            this.host = host;
        }
    }

    private static JSONObject post(String host, String token, String path, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://" + host + "/" + token + "/sharedstreams/" + path).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/plain");
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        InputStream in = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String text = in == null ? "{}" : new String(readAll(in), StandardCharsets.UTF_8);
        conn.disconnect();
        JSONObject json = new JSONObject(text.isEmpty() ? "{}" : text);
        if (code == 330) throw new Redirect(json.getString("X-Apple-MMe-Host"));
        if (code != 200) throw new Exception(path + ": HTTP " + code);
        return json;
    }

    private static void download(String url, File to) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            conn.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }
}
