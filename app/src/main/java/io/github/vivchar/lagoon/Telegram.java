package io.github.vivchar.lagoon;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Messages to the TV through a Telegram bot. The TV long-polls the Bot API itself, so no server is needed.
 * Only people approved on the TV can post: a stranger's first message becomes a request in the settings.
 * Notes, photos and /clear are understood; profile photos are fetched once and kept as avatars.
 */
final class Telegram {
    interface Listener { void onChanged(); }

    static final class Msg {
        long id, from, date;
        String name, text, photo;
        /**
         * What it is: null for a photo or text, "sticker", "gif" or "video" (the last two show a still), or
         * "voice", "audio" and "place", which have no picture and show as a line with an icon.
         */
        String kind;
        /** A GIF's own clip (Telegram sends GIFs as silent MP4), played on the card. */
        String clip;
    }

    static final class Person {
        long id;
        String name;
    }

    private final Context ctx;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Listener listener;
    private volatile boolean polling;
    private Thread thread;
    volatile String status = "";

    Telegram(Context ctx) {
        this.ctx = ctx;
        prefs = ctx.getSharedPreferences("telegram", Context.MODE_PRIVATE);
    }

    void setListener(Listener l) {
        listener = l;
    }

    // ---------------------------------------------------------------- settings

    String token() {
        return prefs.getString("token", "");
    }

    void setToken(String t) {
        prefs.edit().putString("token", t.trim()).putLong("offset", 0).putString("bot", "").apply();
        stop();
        start();
    }

    String botName() {
        return prefs.getString("bot", "");
    }

    /**
     * The code in the invitation QR: whoever opens the bot through it is let in at once. It changes every ten
     * minutes, so a photo of the screen stops working soon; the previous code still counts, for a slow scan.
     */
    String inviteCode() {
        return code(System.currentTimeMillis() / 600_000L);
    }

    /** The link in the invitation QR, or empty while the bot's name is unknown. */
    String inviteLink() {
        return botName().isEmpty() ? "" : "https://t.me/" + botName() + "?start=" + inviteCode();
    }

    private String code(long window) {
        String secret = prefs.getString("invite_secret", "");
        if (secret.isEmpty()) {
            secret = Long.toHexString(new java.security.SecureRandom().nextLong());
            prefs.edit().putString("invite_secret", secret).apply();
        }
        try {
            byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest((secret + ":" + window).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (Exception e) {
            return secret;
        }
    }

    private boolean invited(String text) {
        if (!text.startsWith("/start ")) return false;
        String arg = text.substring(7).trim();
        long w = System.currentTimeMillis() / 600_000L;
        return arg.equals(code(w)) || arg.equals(code(w - 1));
    }

    /** Hours a message stays on Home; 0 = until cleared. */
    int keepHours() {
        return prefs.getInt("keep", 24);
    }

    void setKeepHours(int h) {
        prefs.edit().putInt("keep", h).apply();
        changed();
    }

    boolean confirm() {
        return prefs.getBoolean("confirm", true);
    }

    void setConfirm(boolean on) {
        prefs.edit().putBoolean("confirm", on).apply();
        changed();
    }

    List<Person> people(String key) {
        List<Person> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) {
                Person p = new Person();
                p.id = a.getJSONObject(i).getLong("id");
                p.name = a.getJSONObject(i).getString("name");
                out.add(p);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void savePeople(String key, List<Person> list) {
        JSONArray a = new JSONArray();
        try {
            for (Person p : list) a.put(new JSONObject().put("id", p.id).put("name", p.name));
        } catch (Exception ignored) {
        }
        prefs.edit().putString(key, a.toString()).apply();
    }

    boolean allowed(long id) {
        for (Person p : people("allowed")) if (p.id == id) return true;
        return false;
    }

    void approve(long id, boolean yes) {
        List<Person> pending = people("pending");
        List<Person> allowed = people("allowed");
        Person who = null;
        for (Person p : pending) if (p.id == id) who = p;
        if (who == null) return;
        pending.remove(who);
        if (yes) allowed.add(who);
        savePeople("pending", pending);
        savePeople("allowed", allowed);
        release(id, yes);
        if (yes) send(id, "Готово, теперь ваши сообщения появляются на телевизоре.");
        changed();
    }

    void remove(long id) {
        List<Person> allowed = people("allowed");
        allowed.removeIf(p -> p.id == id);
        savePeople("allowed", allowed);
        changed();
    }

    // ---------------------------------------------------------------- messages

    List<Msg> messages() {
        List<Msg> out = new ArrayList<>();
        long cutoff = keepHours() == 0 ? 0 : System.currentTimeMillis() / 1000 - keepHours() * 3600L;
        try {
            JSONArray a = new JSONArray(prefs.getString("messages", "[]"));
            for (int i = a.length() - 1; i >= 0; i--) {
                JSONObject o = a.getJSONObject(i);
                if (o.getLong("date") < cutoff) continue;
                Msg m = new Msg();
                m.id = o.getLong("id");
                m.from = o.getLong("from");
                m.date = o.getLong("date");
                m.name = o.optString("name");
                m.text = o.optString("text");
                m.photo = o.optString("photo", null);
                m.kind = o.optString("kind", null);
                m.clip = o.optString("clip", null);
                out.add(m);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    void clear() {
        try {
            JSONArray a = new JSONArray(prefs.getString("messages", "[]"));
            for (int i = 0; i < a.length(); i++) forget(a.getJSONObject(i));
        } catch (Exception ignored) {
        }
        prefs.edit().putString("messages", "[]").apply();
        changed();
    }

    /** The biggest file the Bot API lets a bot download. */
    private static final long MAX_FILE = 20_000_000;

    /** Deletes a note's picture and clip: a video can take 20 MB on a stick with little room. */
    private static void forget(Object note) {
        if (!(note instanceof JSONObject)) return;
        for (String k : new String[]{"photo", "clip"}) {
            String path = ((JSONObject) note).optString(k, "");
            if (!path.isEmpty()) new File(path).delete();
        }
    }

    void dismiss(long id) {
        try {
            JSONArray a = new JSONArray(prefs.getString("messages", "[]"));
            JSONArray b = new JSONArray();
            for (int i = 0; i < a.length(); i++) {
                if (a.getJSONObject(i).getLong("id") != id) b.put(a.get(i));
                else forget(a.getJSONObject(i));
            }
            prefs.edit().putString("messages", b.toString()).apply();
        } catch (Exception ignored) {
        }
        changed();
    }

    private void add(Msg m) {
        try {
            JSONArray a = new JSONArray(prefs.getString("messages", "[]"));
            a.put(json(m));
            while (a.length() > 30) forget(a.remove(0));
            prefs.edit().putString("messages", a.toString()).apply();
        } catch (Exception ignored) {
        }
        changed();
    }

    private static JSONObject json(Msg m) throws Exception {
        return new JSONObject().put("id", m.id).put("from", m.from).put("date", m.date).put("name", m.name)
                .put("text", m.text == null ? "" : m.text).put("photo", m.photo).put("kind", m.kind).put("clip", m.clip);
    }

    /** Keeps a message from someone not yet let in, until the TV answers their request. */
    private void hold(Msg m) {
        try {
            JSONArray a = new JSONArray(prefs.getString("held", "[]"));
            a.put(json(m));
            while (a.length() > 30) forget(a.remove(0));
            prefs.edit().putString("held", a.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /** Lets a person's held messages through (yes) or drops them (no). */
    private void release(long user, boolean show) {
        try {
            JSONArray held = new JSONArray(prefs.getString("held", "[]")), keep = new JSONArray();
            JSONArray shown = new JSONArray(prefs.getString("messages", "[]"));
            for (int i = 0; i < held.length(); i++) {
                JSONObject o = held.getJSONObject(i);
                if (o.getLong("from") != user) keep.put(o);
                else if (show) shown.put(o);
            }
            while (shown.length() > 30) forget(shown.remove(0));
            prefs.edit().putString("held", keep.toString()).putString("messages", shown.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    File avatar(long user) {
        return new File(ctx.getFilesDir(), "avatars/" + user + ".jpg");
    }

    Bitmap image(String path) {
        return path == null ? null : BitmapFactory.decodeFile(path);
    }

    private void changed() {
        main.post(() -> {
            if (listener != null) listener.onChanged();
        });
    }

    // ---------------------------------------------------------------- polling

    void start() {
        if (polling || token().isEmpty()) {
            if (token().isEmpty()) status = "Нет токена бота";
            return;
        }
        polling = true;
        thread = new Thread(this::loop, "telegram");
        thread.start();
    }

    void stop() {
        polling = false;
        if (thread != null) thread.interrupt();
    }

    private void loop() {
        while (polling) {
            try {
                if (botName().isEmpty()) {
                    JSONObject me = api("getMe", "").getJSONObject("result");
                    prefs.edit().putString("bot", me.optString("username")).apply();
                }
                if (!"Подключён".equals(status)) { status = "Подключён"; changed(); }
                long offset = prefs.getLong("offset", 0);
                JSONArray ups = api("getUpdates", "timeout=50&offset=" + offset).getJSONArray("result");
                for (int i = 0; i < ups.length(); i++) {
                    JSONObject u = ups.getJSONObject(i);
                    prefs.edit().putLong("offset", u.getLong("update_id") + 1).apply();
                    JSONObject m = u.optJSONObject("message");
                    if (m != null) handle(m);
                }
                changed();
            } catch (Exception e) {
                status = "Нет связи с Telegram";
                Log.w("Telegram", "poll", e);
                changed();
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    private void handle(JSONObject m) throws Exception {
        JSONObject from = m.getJSONObject("from");
        long id = from.getLong("id");
        String name = from.optString("first_name", "") + (from.has("last_name") ? " " + from.optString("last_name") : "");
        String text = m.optString("text", m.optString("caption", ""));
        // came through the QR on the TV: let in without asking
        if (!allowed(id) && invited(text)) {
            List<Person> pending = people("pending");
            pending.removeIf(p -> p.id == id);
            savePeople("pending", pending);
            List<Person> allowed = people("allowed");
            Person p = new Person();
            p.id = id;
            p.name = name.trim();
            allowed.add(p);
            savePeople("allowed", allowed);
            release(id, true);
            fetchAvatar(id);
            send(id, "Готово, пишите сюда — сообщения появятся на главном экране телевизора.");
            changed();
            return;
        }
        if (!allowed(id)) {
            List<Person> pending = people("pending");
            boolean known = false;
            for (Person p : pending) if (p.id == id) known = true;
            if (!known) {
                Person p = new Person();
                p.id = id;
                p.name = name.trim();
                pending.add(p);
                savePeople("pending", pending);
                send(id, "Запрос отправлен. Подтвердите его на телевизоре: панель снизу, Telegram. Это сообщение появится, как только доступ разрешат.");
            }
            // what they wrote waits for the answer instead of being lost
            if (text.startsWith("/")) return;
        }
        boolean held = !allowed(id);
        if (text.trim().equals("/clear")) {
            clear();
            send(id, "Сообщения на телевизоре очищены.");
            return;
        }
        // links that set the TV up rather than notes to show
        String album = Saver.tokenIn(text);
        if (album != null && !held) {
            Saver.setAlbum(ctx, album);
            send(id, "Альбом для заставки подключён. Фото загрузятся в ближайшие минуты.");
            changed();
            return;
        }
        String cal = Cal.linkIn(text);
        if (cal != null && !held) {
            Cal.add(ctx, cal, n -> {
                send(id, n == null ? "Не получилось прочитать этот календарь. Нужна «Секретная ссылка в формате iCal» из настроек Google Календаря." : "Календарь «" + n + "» подключён.");
                changed();
            });
            return;
        }
        if (text.startsWith("/start")) {
            send(id, "Пишите сюда, и сообщение появится на главном экране телевизора. /clear — убрать все.");
            return;
        }
        fetchAvatar(id);
        Msg msg = new Msg();
        msg.id = m.getLong("message_id");
        msg.from = id;
        msg.date = m.getLong("date");
        msg.name = name.trim();
        msg.text = text;
        JSONArray photos = m.optJSONArray("photo");
        File f = new File(ctx.getFilesDir(), "photos/" + msg.id);
        JSONObject sticker = m.optJSONObject("sticker"), doc = m.optJSONObject("document");
        JSONObject moving = m.has("animation") ? m.optJSONObject("animation") : m.has("video") ? m.optJSONObject("video") : m.optJSONObject("video_note");
        if (photos != null && photos.length() > 0) {
            String fileId = photos.getJSONObject(Math.max(0, photos.length() - 2)).getString("file_id");
            if (download(fileId, f)) msg.photo = f.getPath();
        } else if (sticker != null) {
            // plain stickers are WebP pictures; animated (Lottie) and video ones only get their still preview
            boolean still = !sticker.optBoolean("is_animated") && !sticker.optBoolean("is_video");
            String fileId = still ? sticker.optString("file_id", null) : thumb(sticker);
            if (fileId != null && download(fileId, f)) {
                msg.photo = f.getPath();
                msg.kind = "sticker";
            } else if (msg.text.isEmpty()) msg.text = sticker.optString("emoji", "Стикер");
        } else if (m.has("animation")) {
            // a GIF plays on the card; one that cannot be fetched is not shown at all rather than as a still
            JSONObject gif = m.getJSONObject("animation");
            File clip = new File(ctx.getFilesDir(), "photos/" + msg.id + ".mp4");
            if (gif.optLong("file_size") > 15_000_000 || !download(gif.getString("file_id"), clip)) return;
            msg.clip = clip.getPath();
            msg.kind = "gif";
            String still = thumb(gif);
            if (still != null && download(still, f)) msg.photo = f.getPath();
        } else if (moving != null) {
            // the still first; the clip itself, when the Bot API hands it out (up to 20 MB), plays on OK
            boolean round = m.has("video_note");
            String fileId = thumb(moving);
            if (fileId != null && download(fileId, f)) msg.photo = f.getPath();
            File clip = new File(ctx.getFilesDir(), "photos/" + msg.id + ".mp4");
            if (moving.optLong("file_size") <= MAX_FILE && download(moving.getString("file_id"), clip)) msg.clip = clip.getPath();
            if (msg.photo != null || msg.clip != null) msg.kind = round ? "round" : "video";
            else if (msg.text.isEmpty()) msg.text = round ? "Видеосообщение" : "Видео";
        } else if (doc != null && doc.optString("mime_type").startsWith("image/") && doc.optLong("file_size") < 8_000_000) {
            if (download(doc.getString("file_id"), f)) msg.photo = f.getPath();
        } else if (m.has("voice") && msg.text.isEmpty()) {
            int sec = m.getJSONObject("voice").optInt("duration");
            msg.text = String.format(java.util.Locale.US, "Голосовое · %d:%02d", sec / 60, sec % 60);
            msg.kind = "voice";
            File clip = new File(ctx.getFilesDir(), "photos/" + msg.id + ".ogg");
            if (download(m.getJSONObject("voice").getString("file_id"), clip)) msg.clip = clip.getPath();
        } else if (m.has("audio") && msg.text.isEmpty()) {
            JSONObject a = m.getJSONObject("audio");
            String title = (a.has("title") ? a.optString("performer", "") + " — " + a.optString("title") : a.optString("file_name", "")).trim();
            msg.text = title.startsWith("—") ? title.substring(1).trim() : title;
            if (msg.text.isEmpty()) msg.text = "Аудио";
            msg.kind = "audio";
            File clip = new File(ctx.getFilesDir(), "photos/" + msg.id + ".audio");
            if (a.optLong("file_size") <= MAX_FILE && download(a.getString("file_id"), clip)) msg.clip = clip.getPath();
        } else if (m.has("location") && msg.text.isEmpty()) {
            // a venue is a place picked from the map: its name says more than the coordinates
            JSONObject v = m.optJSONObject("venue");
            msg.text = v != null ? (v.optString("title") + (v.optString("address").isEmpty() ? "" : "\n" + v.optString("address"))).trim() : "Геопозиция";
            msg.kind = "place";
        }
        if (msg.text.isEmpty() && msg.photo == null && msg.clip == null) return;
        if (held) {
            hold(msg);
            return;
        }
        add(msg);
        if (confirm()) send(id, "Показано на телевизоре.");
    }

    /** The file id of a media's preview still, or null. */
    private static String thumb(JSONObject media) {
        JSONObject t = media.has("thumbnail") ? media.optJSONObject("thumbnail") : media.optJSONObject("thumb");
        return t == null ? null : t.optString("file_id", null);
    }

    private void fetchAvatar(long user) {
        File f = avatar(user);
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < 7 * 86400_000L) return;
        try {
            JSONObject r = api("getUserProfilePhotos", "user_id=" + user + "&limit=1").getJSONObject("result");
            JSONArray photos = r.getJSONArray("photos");
            if (photos.length() == 0) return;
            JSONArray sizes = photos.getJSONArray(0);
            download(sizes.getJSONObject(0).getString("file_id"), f);
        } catch (Exception e) {
            Log.w("Telegram", "avatar", e);
        }
    }

    private boolean download(String fileId, File to) {
        try {
            String path = api("getFile", "file_id=" + URLEncoder.encode(fileId, "UTF-8")).getJSONObject("result").getString("file_path");
            to.getParentFile().mkdirs();
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.telegram.org/file/bot" + token() + "/" + path).openConnection();
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(to)) {
                byte[] b = new byte[16384];
                int n;
                while ((n = in.read(b)) > 0) out.write(b, 0, n);
            } finally {
                c.disconnect();
            }
            return true;
        } catch (Exception e) {
            Log.w("Telegram", "download", e);
            return false;
        }
    }

    private void send(long chat, String text) {
        new Thread(() -> {
            try {
                api("sendMessage", "chat_id=" + chat + "&text=" + URLEncoder.encode(text, "UTF-8"));
            } catch (Exception e) {
                Log.w("Telegram", "send", e);
            }
        }).start();
    }

    private JSONObject api(String method, String query) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.telegram.org/bot" + token() + "/" + method + (query.isEmpty() ? "" : "?" + query)).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(65000);
        try (InputStream in = c.getResponseCode() < 400 ? c.getInputStream() : c.getErrorStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            JSONObject o = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            if (!o.optBoolean("ok")) throw new Exception(method + ": " + o.optString("description"));
            return o;
        } finally {
            c.disconnect();
        }
    }
}
