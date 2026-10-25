package io.github.vivchar.lagoon;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Long strings typed on the phone instead of the remote: the QR on a page opens a small form served by the TV
 * itself over the home Wi-Fi ({@code http://<tv>:8080/<kind>?c=<code>}), and what is pasted there lands on the
 * TV at once. Nothing goes through the internet on the way. The server runs only while a page with the QR is
 * open (at most ten minutes), takes only the one-time code in the link and only callers from the local network.
 */
final class Handoff {
    static final String TOKEN = "token", ALBUM = "album", CALENDAR = "calendar";

    /**
     * Something arrived; main thread. {@code kind} is one of the constants above; {@code value} is the bot token
     * (checked with Telegram, for the owner of the bot to set), or empty for what Handoff sets up itself.
     */
    interface Listener {
        void received(String kind, String value);
    }

    private static final String TAG = "Handoff";
    private static final long LIFE_MS = 10 * 60_000L;
    /** Wrong codes before the server gives up: nobody guesses six letters in this many tries. */
    private static final int TRIES = 20;
    private static final Pattern BOT_TOKEN = Pattern.compile("\\d{5,12}:[A-Za-z0-9_-]{30,50}");
    private static final String CODE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static ServerSocket server;
    /** The code in the QR on screen and the one thing it may hand over: a code seen on one page can't set another. */
    private static volatile String code, codeKind;
    private static volatile long until;
    private static Listener listener;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Handoff() {
    }

    /**
     * The link for the QR that hands {@code kind} over, starting the server if it is not up; null when the TV is
     * on no local network the phone could reach.
     */
    static synchronized String link(Context c, String kind, Listener l) {
        String ip = address(c);
        if (ip == null) return null;
        listener = l;
        until = System.currentTimeMillis() + LIFE_MS;
        if (server == null && !start(c.getApplicationContext())) return null;
        if (!kind.equals(codeKind)) newCode(kind);
        return "http://" + ip + ":" + server.getLocalPort() + "/" + kind + "?c=" + code;
    }

    static synchronized void stop() {
        listener = null;
        code = null;
        codeKind = null;
        if (server == null) return;
        try {
            server.close();
        } catch (Exception ignored) {
        }
        server = null;
    }

    private static boolean start(Context app) {
        try {
            ServerSocket s = new ServerSocket();
            s.setReuseAddress(true);
            try {
                s.bind(new java.net.InetSocketAddress(8080));
            } catch (Exception busy) {
                s.bind(new java.net.InetSocketAddress(0));
            }
            s.setSoTimeout(1000);
            server = s;
        } catch (Exception e) {
            Log.w(TAG, "listen: " + e.getMessage());
            return false;
        }
        ServerSocket mine = server;
        new Thread(() -> serve(app, mine), "handoff").start();
        return true;
    }

    /** A fresh code for {@code kind}; the old one stops working. */
    private static void newCode(String kind) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(CODE_LETTERS.charAt(r.nextInt(CODE_LETTERS.length())));
        code = sb.toString();
        codeKind = kind;
    }

    private static void serve(Context app, ServerSocket s) {
        int wrong = 0;
        while (!s.isClosed() && System.currentTimeMillis() < until && wrong < TRIES) {
            try (Socket sock = s.accept()) {
                sock.setSoTimeout(10_000);
                if (!local(sock.getInetAddress())) continue;
                if (!answer(Lang.wrap(app), sock)) wrong++;
            } catch (SocketTimeoutException tick) {
                // check the time and go on waiting
            } catch (Exception e) {
                if (!s.isClosed()) Log.w(TAG, "serve: " + e.getClass().getSimpleName());
            }
        }
        synchronized (Handoff.class) {
            if (server == s) stop();
        }
    }

    /** One request: the form, or what was sent in it. False when the code in it was wrong. */
    private static boolean answer(Context c, Socket sock) throws Exception {
        InputStream in = sock.getInputStream();
        String head = readHead(in);
        if (head == null) return true;
        String[] first = head.substring(0, Math.max(0, head.indexOf('\r'))).split(" ");
        if (first.length < 2) return true;
        String method = first[0], target = first[1];
        int q = target.indexOf('?');
        String kind = (q < 0 ? target : target.substring(0, q)).replace("/", "");
        String given = q < 0 ? "" : param(target.substring(q + 1), "c");
        OutputStream out = sock.getOutputStream();
        if (!kind.equals(TOKEN) && !kind.equals(ALBUM) && !kind.equals(CALENDAR)) {
            // the browser asks for /favicon.ico and the like: not a guess at the code
            reply(out, 404, "");
            return true;
        }
        if (!given.equals(code) || !kind.equals(codeKind)) {
            reply(out, 403, page(c, c.getString(R.string.web_expired_title), c.getString(R.string.web_expired), null, null));
            return false;
        }
        if (!method.equals("POST")) {
            reply(out, 200, form(c, kind, null));
            return true;
        }
        int len = 0;
        for (String line : head.split("\r\n"))
            if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")) len = Integer.parseInt(line.substring(15).trim());
        // a token or a link: anything longer is not what the form is for
        if (len <= 0 || len > 8192) {
            reply(out, 413, form(c, kind, c.getString(R.string.web_too_long)));
            return true;
        }
        byte[] body = new byte[len];
        int got = 0;
        while (got < len) {
            int n = in.read(body, got, len - got);
            if (n < 0) break;
            got += n;
        }
        String value = param(new String(body, 0, got, StandardCharsets.UTF_8), "v").trim();
        String[] token = new String[1];
        String done = take(c, kind, value, token);
        if (done == null) {
            reply(out, 200, form(c, kind, error(c, kind)));
        } else {
            // used once: the page on the TV redraws with a new code
            synchronized (Handoff.class) {
                newCode(kind);
            }
            reply(out, 200, page(c, c.getString(R.string.web_done), done, null, null));
            String v = token[0] == null ? "" : token[0];
            MAIN.post(() -> {
                if (listener != null) listener.received(kind, v);
            });
        }
        return true;
    }

    /** Sets up what came in and says so in a line for the phone, or null when it was not what was asked for. */
    private static String take(Context c, String kind, String value, String[] token) throws InterruptedException {
        switch (kind) {
            case TOKEN: {
                java.util.regex.Matcher m = BOT_TOKEN.matcher(value);
                if (!m.find()) return null;
                String bot = botName(m.group());
                if (bot == null) return null;
                token[0] = m.group();
                return c.getString(R.string.web_token_ok, bot);
            }
            case ALBUM: {
                String album = Saver.tokenIn(value);
                if (album == null) return null;
                MAIN.post(() -> Saver.setAlbum(c, album));
                return c.getString(R.string.web_album_ok);
            }
            default: {
                String url = Cal.linkIn(value);
                if (url == null) return null;
                String[] name = new String[1];
                CountDownLatch read = new CountDownLatch(1);
                Cal.add(c, url, n -> {
                    name[0] = n;
                    read.countDown();
                });
                read.await(30, TimeUnit.SECONDS);
                return name[0] == null ? null : c.getString(R.string.web_cal_ok, name[0]);
            }
        }
    }

    private static String error(Context c, String kind) {
        switch (kind) {
            case TOKEN:
                return c.getString(R.string.web_bad_token);
            case ALBUM:
                return c.getString(R.string.web_bad_album);
            default:
                return c.getString(R.string.web_bad_cal);
        }
    }

    /** The bot's @name if Telegram knows this token; the token itself never reaches the log. */
    private static String botName(String token) {
        try {
            HttpURLConnection h = (HttpURLConnection) new URL("https://api.telegram.org/bot" + token + "/getMe").openConnection();
            h.setConnectTimeout(15000);
            h.setReadTimeout(15000);
            try (InputStream in = h.getResponseCode() < 400 ? h.getInputStream() : h.getErrorStream()) {
                JSONObject o = new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
                return o.optBoolean("ok") ? o.getJSONObject("result").optString("username") : null;
            } finally {
                h.disconnect();
            }
        } catch (Exception e) {
            Log.w(TAG, "getMe: " + e.getClass().getSimpleName());
            return null;
        }
    }

    // ---------------------------------------------------------------- the page

    private static String form(Context c, String kind, String error) {
        int title, hint;
        String example;
        switch (kind) {
            case TOKEN:
                title = R.string.web_token_title;
                hint = R.string.web_token_hint;
                example = "123456789:AAF…";
                break;
            case ALBUM:
                title = R.string.web_album_title;
                hint = R.string.web_album_hint;
                example = "https://www.icloud.com/sharedalbum/…";
                break;
            default:
                title = R.string.web_cal_title;
                hint = R.string.web_cal_hint;
                example = "https://calendar.google.com/…/basic.ics";
                break;
        }
        String text = c.getString(hint);
        if (kind.equals(CALENDAR)) text += "\n\n" + c.getString(R.string.cal_howto_google) + "\n\n" + c.getString(R.string.cal_howto_apple);
        String f = "<form method=post action=\"/" + kind + "?c=" + code + "\">"
                + "<textarea name=v required autofocus autocapitalize=off autocomplete=off spellcheck=false placeholder=\"" + esc(example) + "\"></textarea>"
                + "<button>" + esc(c.getString(R.string.web_send)) + "</button></form>";
        return page(c, c.getString(title), text, error, f);
    }

    private static String page(Context c, String title, String text, String error, String form) {
        return "<!doctype html><html lang=" + Lang.locale(c).getLanguage() + "><meta charset=utf-8>"
                + "<meta name=viewport content=\"width=device-width,initial-scale=1\"><title>Lagoon TV</title>"
                + "<style>body{font:17px/1.45 -apple-system,system-ui,sans-serif;background:#0c1016;color:#fff;margin:0 auto;"
                + "padding:40px 20px;max-width:520px}h1{font-size:26px;margin:0 0 12px}p{color:#c3c8ce;margin:0 0 20px}"
                + ".err{color:#ffb4a8}textarea{width:100%;box-sizing:border-box;min-height:132px;font:16px ui-monospace,Menlo,monospace;"
                + "border:0;border-radius:18px;padding:14px 16px;background:#1d232b;color:#fff;resize:vertical}"
                + "button{margin-top:16px;width:100%;height:54px;border:0;border-radius:27px;background:#fff;color:#0b0f14;"
                + "font:600 17px -apple-system,system-ui,sans-serif}</style>"
                + "<h1>" + esc(title) + "</h1><p>" + esc(text).replace("\n", "<br>") + "</p>"
                + (error == null ? "" : "<p class=err>" + esc(error) + "</p>")
                + (form == null ? "" : form) + "</html>";
    }

    // ---------------------------------------------------------------- plumbing

    private static void reply(OutputStream out, int status, String html) throws Exception {
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + " " + (status == 200 ? "OK" : "Error") + "\r\n"
                + "Content-Type: text/html; charset=utf-8\r\nContent-Length: " + b.length + "\r\n"
                + "Cache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nConnection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(b);
        out.flush();
    }

    /** The request line and headers, up to the blank line; null for an empty or oversized head. */
    private static String readHead(InputStream in) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int prev3 = 0, prev2 = 0, prev1 = 0, ch;
        while ((ch = in.read()) >= 0) {
            b.write(ch);
            if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && ch == '\n') return b.toString("ISO-8859-1");
            if (b.size() > 8192) return null;
            prev3 = prev2;
            prev2 = prev1;
            prev1 = ch;
        }
        return null;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        return out.toByteArray();
    }

    private static String param(String query, String name) {
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                try {
                    return URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                } catch (Exception e) {
                    return "";
                }
            }
        }
        return "";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Only the home network (and adb's loopback): a caller from outside, through a VPN or a router, is refused. */
    private static boolean local(InetAddress a) {
        return a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress();
    }

    /** The TV's IPv4 address on its current network, the one a phone on the same Wi-Fi can reach. */
    private static String address(Context c) {
        try {
            ConnectivityManager cm = c.getSystemService(ConnectivityManager.class);
            Network n = cm.getActiveNetwork();
            LinkProperties lp = n == null ? null : cm.getLinkProperties(n);
            if (lp == null) return null;
            for (LinkAddress la : lp.getLinkAddresses()) {
                InetAddress a = la.getAddress();
                if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
            }
        } catch (Exception e) {
            Log.w(TAG, "address: " + e.getClass().getSimpleName());
        }
        return null;
    }
}
