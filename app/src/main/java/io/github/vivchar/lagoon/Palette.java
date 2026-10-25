package io.github.vivchar.lagoon;

/** The times of day of the bay, and which of the four painted backdrops (assets/scene) belongs to a moment. */
final class Palette {
    enum Phase { MORNING, DAY, SUNSET, NIGHT }

    private Palette() {
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
}
