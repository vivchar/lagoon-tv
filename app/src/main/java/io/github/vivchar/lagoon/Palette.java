package io.github.vivchar.lagoon;

import android.graphics.Color;

/**
 * Colours of the bay at one moment. Keyframes for four times of day, taken from the painted backdrops
 * (assets/scene); anything in between is a blend.
 */
final class Palette {
    enum Phase { MORNING, DAY, SUNSET, NIGHT }

    /** Where the sky's colours sit, top to the horizon. */
    static final float[] SKY_AT = {0, 110, 220, 320, 400, 442};

    Phase phase;
    /** 0 by day, 1 at full night: stars, the moon and the lights on the hills follow it. */
    float night;
    int[] sky;
    /** The sky low on the right, towards the sunset. */
    int skyWarm;
    int sun, glow;
    float sr, gr;
    int cloudHi, cloudLo;
    /** How much of the thin streaks of cloud there are, 0..1. */
    float streaks;
    /** The far hazy hill; the forest in shadow, in the middle and lit; the haze over the foot of the hills. */
    int far, hillDark, hillMid, hillLit, haze;
    float hazeA;
    int islandDark, islandLit;
    int rock, rockLit;
    /** The sea at the horizon, in the middle, near the shore, and the shallow turquoise strip. */
    int[] sea;
    int glint;
    int sand0, sand1, shade, wet, foam;
    int palm, rim, trunk;
    /** Morning mist over the hills, 0..1. */
    float mist;

    private Palette() {
    }

    private static int c(String hex) {
        return Color.parseColor(hex);
    }

    private static int[] cs(String... hex) {
        int[] out = new int[hex.length];
        for (int i = 0; i < hex.length; i++) out[i] = c(hex[i]);
        return out;
    }

    static Palette of(Phase phase) {
        Palette p = new Palette();
        p.phase = phase;
        switch (phase) {
            case MORNING:
                p.sky = cs("#86A8DF", "#9CB0DC", "#C2B5CC", "#E6BBB5", "#F4C2AA", "#EAC2C4");
                p.skyWarm = c("#FBD2B0");
                p.sun = c("#FFF4DE"); p.glow = c("#FFCFA8"); p.sr = 24; p.gr = 300;
                p.cloudHi = c("#FDD3B4"); p.cloudLo = c("#C4A4C4"); p.streaks = 0.9f;
                p.far = c("#A7ACCB"); p.hillDark = c("#4C5872"); p.hillMid = c("#6C7C96"); p.hillLit = c("#A9A2AE");
                p.haze = c("#E2C2C8"); p.hazeA = 0.4f;
                p.islandDark = c("#2A3C42"); p.islandLit = c("#8E8A5A");
                p.rock = c("#6A7594"); p.rockLit = c("#C9A6AA");
                p.sea = cs("#5A88BE", "#6C9EC4", "#6AA4BC", "#86BBB0");
                p.glint = c("#FFE6D6");
                p.sand0 = c("#E6B29C"); p.sand1 = c("#F2C2A6"); p.shade = c("#9A8AA6"); p.wet = c("#C9A5A0"); p.foam = c("#FFF6F0");
                p.palm = c("#3E4826"); p.rim = c("#C8B886"); p.trunk = c("#4C463A");
                p.mist = 0.6f;
                break;
            case SUNSET:
                p.sky = cs("#8C88AA", "#A3889F", "#C9848A", "#EC8763", "#F98F52", "#F49E66");
                p.skyWarm = c("#FFB25E");
                p.sun = c("#FFE9A8"); p.glow = c("#FF9A4A"); p.sr = 32; p.gr = 380;
                p.cloudHi = c("#FFA65C"); p.cloudLo = c("#B06A88"); p.streaks = 1;
                p.far = c("#A26A66"); p.hillDark = c("#3A3240"); p.hillMid = c("#5C4848"); p.hillLit = c("#A8653E");
                p.haze = c("#E68A6A"); p.hazeA = 0.3f;
                p.islandDark = c("#2A2218"); p.islandLit = c("#8C6A1C");
                p.rock = c("#584850"); p.rockLit = c("#D27858");
                p.sea = cs("#7A6C7E", "#A47C6C", "#6E8486", "#7E9A88");
                p.glint = c("#FFC27C");
                p.sand0 = c("#E88C5A"); p.sand1 = c("#F49A5E"); p.shade = c("#8E6672"); p.wet = c("#C27A5C"); p.foam = c("#FFDABA");
                p.palm = c("#483816"); p.rim = c("#E09A4A"); p.trunk = c("#4A3020");
                break;
            case NIGHT:
                p.night = 1;
                p.sky = cs("#04214E", "#072757", "#0B2C60", "#163569", "#1E4078", "#24467F");
                p.skyWarm = c("#2C5290");
                p.sun = c("#F4F1DC"); p.glow = c("#9FB4FF"); p.sr = 20; p.gr = 200;
                p.cloudHi = c("#7088BA"); p.cloudLo = c("#0E2A58"); p.streaks = 0.8f;
                p.far = c("#1B3B70"); p.hillDark = c("#03152E"); p.hillMid = c("#0A213E"); p.hillLit = c("#1E3048");
                p.haze = c("#1E4078"); p.hazeA = 0.3f;
                p.islandDark = c("#020C1A"); p.islandLit = c("#1A2A32");
                p.rock = c("#0E1E38"); p.rockLit = c("#3E4660");
                p.sea = cs("#032A5C", "#063666", "#0C4266", "#1A4E6E");
                p.glint = c("#C8D8FF");
                p.sand0 = c("#4A4C5E"); p.sand1 = c("#5E5A68"); p.shade = c("#2A3250"); p.wet = c("#384058"); p.foam = c("#A8C0E8");
                p.palm = c("#030E1A"); p.rim = c("#1E3A6A"); p.trunk = c("#050C16");
                break;
            case DAY:
            default:
                p.phase = Phase.DAY;
                p.sky = cs("#3F9BEB", "#4BA7F2", "#5DB0F3", "#7DBEF2", "#A9CDEE", "#C4DCEF");
                p.skyWarm = c("#D2E6F4");
                p.sun = c("#FFFFF6"); p.glow = c("#FFF6D8"); p.sr = 28; p.gr = 260;
                p.cloudHi = c("#FFFFFF"); p.cloudLo = c("#BCD0E6"); p.streaks = 0.25f;
                p.far = c("#6FA2CC"); p.hillDark = c("#2C5A3E"); p.hillMid = c("#4C8048"); p.hillLit = c("#9AB65E");
                p.haze = c("#A9CDEE"); p.hazeA = 0.45f;
                p.islandDark = c("#2A5430"); p.islandLit = c("#9CB44E");
                p.rock = c("#8A8890"); p.rockLit = c("#E6D0C0");
                p.sea = cs("#1C8CD4", "#2AA8DC", "#3ABBDC", "#74D4CC");
                p.glint = c("#FFFFFF");
                p.sand0 = c("#EEDAC0"); p.sand1 = c("#F8EAD4"); p.shade = c("#C8B4B4"); p.wet = c("#D8C4A6"); p.foam = c("#FFFFFF");
                p.palm = c("#5A7428"); p.rim = c("#BCC85A"); p.trunk = c("#7A6448");
                break;
        }
        return p;
    }

    /** A blend of two keyframes, f = 0 gives a, f = 1 gives b. */
    static Palette lerp(Palette a, Palette b, float f) {
        Palette p = new Palette();
        p.phase = f < 0.5f ? a.phase : b.phase;
        p.night = a.night + (b.night - a.night) * f;
        p.sky = mixAll(a.sky, b.sky, f);
        p.skyWarm = mix(a.skyWarm, b.skyWarm, f);
        p.sun = mix(a.sun, b.sun, f);
        p.glow = mix(a.glow, b.glow, f);
        p.sr = a.sr + (b.sr - a.sr) * f;
        p.gr = a.gr + (b.gr - a.gr) * f;
        p.cloudHi = mix(a.cloudHi, b.cloudHi, f);
        p.cloudLo = mix(a.cloudLo, b.cloudLo, f);
        p.streaks = a.streaks + (b.streaks - a.streaks) * f;
        p.far = mix(a.far, b.far, f);
        p.hillDark = mix(a.hillDark, b.hillDark, f);
        p.hillMid = mix(a.hillMid, b.hillMid, f);
        p.hillLit = mix(a.hillLit, b.hillLit, f);
        p.haze = mix(a.haze, b.haze, f);
        p.hazeA = a.hazeA + (b.hazeA - a.hazeA) * f;
        p.islandDark = mix(a.islandDark, b.islandDark, f);
        p.islandLit = mix(a.islandLit, b.islandLit, f);
        p.rock = mix(a.rock, b.rock, f);
        p.rockLit = mix(a.rockLit, b.rockLit, f);
        p.sea = mixAll(a.sea, b.sea, f);
        p.glint = mix(a.glint, b.glint, f);
        p.sand0 = mix(a.sand0, b.sand0, f);
        p.sand1 = mix(a.sand1, b.sand1, f);
        p.shade = mix(a.shade, b.shade, f);
        p.wet = mix(a.wet, b.wet, f);
        p.foam = mix(a.foam, b.foam, f);
        p.palm = mix(a.palm, b.palm, f);
        p.rim = mix(a.rim, b.rim, f);
        p.trunk = mix(a.trunk, b.trunk, f);
        p.mist = a.mist + (b.mist - a.mist) * f;
        return p;
    }

    /**
     * The bay at a minute of the day, given today's sunrise and sunset (minutes since midnight).
     * Keyframes sit around sunrise and sunset; between them colours blend, so the light never jumps.
     */
    static Palette at(int minute, int rise, int set) {
        Palette night = of(Phase.NIGHT), morning = of(Phase.MORNING), day = of(Phase.DAY), sunset = of(Phase.SUNSET);
        Palette dusk = lerp(sunset, night, 0.55f);
        Object[][] keys = {
                {rise - 80, night}, {rise - 15, lerp(night, morning, 0.7f)}, {rise + 30, morning}, {rise + 110, morning},
                {rise + 200, day}, {set - 110, day}, {set - 25, sunset}, {set + 10, sunset}, {set + 35, dusk}, {set + 75, night}
        };
        int m = minute;
        if (m < rise - 80) return night;
        for (int i = 0; i < keys.length - 1; i++) {
            int t0 = (int) keys[i][0], t1 = (int) keys[i + 1][0];
            if (m >= t0 && m < t1) {
                float f = (m - t0) / (float) (t1 - t0);
                f = f * f * (3 - 2 * f);
                return lerp((Palette) keys[i][1], (Palette) keys[i + 1][1], f);
            }
        }
        return night;
    }

    /**
     * How much of each painted backdrop (morning, day, sunset, night) to show at a minute of the day. One at a
     * time, with a half-hour crossfade into the next: around sunrise, mid-morning, before sunset and at dusk.
     */
    static float[] backdrops(float minute, int rise, int set) {
        float[][] keys = {
                {rise - 25, 3}, {rise + 5, 0}, {rise + 120, 0}, {rise + 150, 1},
                {set - 60, 1}, {set - 30, 2}, {set + 5, 2}, {set + 35, 3}};
        float[] w = new float[4];
        float m = minute;
        if (m < keys[0][0] || m >= keys[keys.length - 1][0]) {
            w[3] = 1;
            return w;
        }
        for (int i = 0; i < keys.length - 1; i++) {
            if (m >= keys[i][0] && m < keys[i + 1][0]) {
                int a = (int) keys[i][1], b = (int) keys[i + 1][1];
                float f = (m - keys[i][0]) / (keys[i + 1][0] - keys[i][0]);
                f = f * f * (3 - 2 * f);
                w[a] += 1 - f;
                w[b] += f;
                return w;
            }
        }
        w[3] = 1;
        return w;
    }

    private static int[] mixAll(int[] a, int[] b, float f) {
        int[] out = new int[a.length];
        for (int i = 0; i < a.length; i++) out[i] = mix(a[i], b[i], f);
        return out;
    }

    static int mix(int a, int b, float f) {
        return Color.rgb(
                Math.round(Color.red(a) * (1 - f) + Color.red(b) * f),
                Math.round(Color.green(a) * (1 - f) + Color.green(b) * f),
                Math.round(Color.blue(a) * (1 - f) + Color.blue(b) * f));
    }

    static int alpha(int color, float a) {
        return Color.argb(Math.round(Math.max(0, Math.min(1, a)) * 255), Color.red(color), Color.green(color), Color.blue(color));
    }
}
