package io.github.vivchar.lagoon;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.nio.charset.StandardCharsets;

/**
 * A small QR encoder for one short link: byte mode, error correction level L, versions 1 to 10 (up to 271
 * bytes). Follows the standard step by step, the way Project Nayuki's reference encoder lays it out: data and
 * Reed–Solomon codewords, function patterns, the zigzag placement, and the mask with the lowest penalty.
 */
final class Qr {
    private static final int[] ECC_PER_BLOCK = {-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18};
    private static final int[] BLOCKS = {-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4};

    private final int size;
    private final boolean[][] modules, function;

    private Qr(int version) {
        size = version * 4 + 17;
        modules = new boolean[size][size];
        function = new boolean[size][size];
    }

    /** The code as a bitmap, {@code scale} pixels a module, with a 4-module quiet zone, dark on white. */
    static Bitmap bitmap(String text, int scale) {
        Qr q = encode(text.getBytes(StandardCharsets.UTF_8));
        int n = (q.size + 8) * scale;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.RGB_565);
        b.eraseColor(Color.WHITE);
        int[] dark = new int[scale * scale];
        java.util.Arrays.fill(dark, 0xFF0B0F14);
        for (int y = 0; y < q.size; y++)
            for (int x = 0; x < q.size; x++)
                if (q.modules[y][x]) b.setPixels(dark, 0, scale, (x + 4) * scale, (y + 4) * scale, scale, scale);
        return b;
    }

    static Qr encode(byte[] data) {
        int version = 1;
        while (true) {
            int cap = dataCodewords(version) * 8;
            int need = 4 + (version < 10 ? 8 : 16) + data.length * 8;
            if (need <= cap) break;
            if (++version > 10) throw new IllegalArgumentException("too long for a QR up to version 10");
        }
        // the bit stream: byte mode, length, data, terminator, padding
        int capBits = dataCodewords(version) * 8;
        Bits bb = new Bits(capBits);
        bb.append(4, 4);
        bb.append(data.length, version < 10 ? 8 : 16);
        for (byte b : data) bb.append(b & 0xFF, 8);
        bb.append(0, Math.min(4, capBits - bb.len));
        bb.append(0, (8 - bb.len % 8) % 8);
        for (int pad = 0xEC; bb.len < capBits; pad ^= 0xEC ^ 0x11) bb.append(pad, 8);
        byte[] codewords = new byte[bb.len / 8];
        for (int i = 0; i < bb.len; i++) if (bb.get(i)) codewords[i >>> 3] |= 1 << (7 - (i & 7));

        Qr q = new Qr(version);
        q.drawFunctions(version);
        q.drawCodewords(q.interleave(version, codewords));
        int best = 0;
        long min = Long.MAX_VALUE;
        for (int m = 0; m < 8; m++) {
            q.mask(m);
            q.drawFormat(m);
            long p = q.penalty();
            if (p < min) {
                min = p;
                best = m;
            }
            q.mask(m);   // a mask is its own inverse
        }
        q.mask(best);
        q.drawFormat(best);
        return q;
    }

    // ---------------------------------------------------------------- sizes

    private static int rawModules(int v) {
        int r = (16 * v + 128) * v + 64;
        if (v >= 2) {
            int align = v / 7 + 2;
            r -= (25 * align - 10) * align - 55;
            if (v >= 7) r -= 36;
        }
        return r;
    }

    private static int dataCodewords(int v) {
        return rawModules(v) / 8 - ECC_PER_BLOCK[v] * BLOCKS[v];
    }

    private static int[] alignment(int v) {
        if (v == 1) return new int[0];
        int n = v / 7 + 2;
        int step = (v * 4 + n * 2 + 1) / (n * 2 - 2) * 2;
        int[] r = new int[n];
        r[0] = 6;
        for (int i = n - 1, pos = v * 4 + 10; i >= 1; i--, pos -= step) r[i] = pos;
        return r;
    }

    // ---------------------------------------------------------------- function patterns

    private void set(int x, int y, boolean dark) {
        modules[y][x] = dark;
        function[y][x] = true;
    }

    private void drawFunctions(int version) {
        for (int i = 0; i < size; i++) {
            set(6, i, i % 2 == 0);
            set(i, 6, i % 2 == 0);
        }
        finder(3, 3);
        finder(size - 4, 3);
        finder(3, size - 4);
        int[] pos = alignment(version);
        int n = pos.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (!(i == 0 && j == 0 || i == 0 && j == n - 1 || i == n - 1 && j == 0))
                    for (int dy = -2; dy <= 2; dy++)
                        for (int dx = -2; dx <= 2; dx++)
                            set(pos[i] + dx, pos[j] + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
        drawFormat(0);
        if (version >= 7) {
            int rem = version;
            for (int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
            int bits = version << 12 | rem;
            for (int i = 0; i < 18; i++) {
                boolean bit = ((bits >>> i) & 1) != 0;
                int a = size - 11 + i % 3, b = i / 3;
                set(a, b, bit);
                set(b, a, bit);
            }
        }
    }

    private void finder(int x, int y) {
        for (int dy = -4; dy <= 4; dy++)
            for (int dx = -4; dx <= 4; dx++) {
                int d = Math.max(Math.abs(dx), Math.abs(dy)), xx = x + dx, yy = y + dy;
                if (xx >= 0 && xx < size && yy >= 0 && yy < size) set(xx, yy, d != 2 && d != 4);
            }
    }

    /** Format bits: level L (01) and the mask, BCH-coded, in both copies. */
    private void drawFormat(int mask) {
        int data = 1 << 3 | mask;
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = (data << 10 | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) set(8, i, bit(bits, i));
        set(8, 7, bit(bits, 6));
        set(8, 8, bit(bits, 7));
        set(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) set(14 - i, 8, bit(bits, i));
        for (int i = 0; i < 8; i++) set(size - 1 - i, 8, bit(bits, i));
        for (int i = 8; i < 15; i++) set(8, size - 15 + i, bit(bits, i));
        set(8, size - 8, true);
    }

    private static boolean bit(int x, int i) {
        return ((x >>> i) & 1) != 0;
    }

    // ---------------------------------------------------------------- data

    private byte[] interleave(int version, byte[] data) {
        int blocks = BLOCKS[version], ecc = ECC_PER_BLOCK[version];
        int raw = rawModules(version) / 8;
        int shortBlocks = blocks - raw % blocks, shortLen = raw / blocks;
        byte[] divisor = divisor(ecc);
        byte[][] out = new byte[blocks][];
        for (int i = 0, k = 0; i < blocks; i++) {
            int len = shortLen - ecc + (i < shortBlocks ? 0 : 1);
            byte[] dat = java.util.Arrays.copyOfRange(data, k, k + len);
            k += len;
            byte[] block = java.util.Arrays.copyOf(dat, shortLen + 1);
            byte[] r = remainder(dat, divisor);
            System.arraycopy(r, 0, block, block.length - ecc, ecc);
            out[i] = block;
        }
        byte[] result = new byte[raw];
        int n = 0;
        for (int i = 0; i < out[0].length; i++)
            for (int j = 0; j < blocks; j++)
                if (i != shortLen - ecc || j >= shortBlocks) result[n++] = out[j][i];
        return result;
    }

    private void drawCodewords(byte[] data) {
        int i = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < size; vert++)
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean up = ((right + 1) & 2) == 0;
                    int y = up ? size - 1 - vert : vert;
                    if (!function[y][x] && i < data.length * 8) {
                        modules[y][x] = bit(data[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
        }
    }

    private void mask(int m) {
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                boolean inv;
                switch (m) {
                    case 0: inv = (x + y) % 2 == 0; break;
                    case 1: inv = y % 2 == 0; break;
                    case 2: inv = x % 3 == 0; break;
                    case 3: inv = (x + y) % 3 == 0; break;
                    case 4: inv = (x / 3 + y / 2) % 2 == 0; break;
                    case 5: inv = x * y % 2 + x * y % 3 == 0; break;
                    case 6: inv = (x * y % 2 + x * y % 3) % 2 == 0; break;
                    default: inv = ((x + y) % 2 + x * y % 3) % 2 == 0; break;
                }
                if (!function[y][x] && inv) modules[y][x] = !modules[y][x];
            }
    }

    /** Runs of five or more, 2×2 blocks and the dark/light balance: enough to pick a readable mask. */
    private long penalty() {
        long p = 0;
        int dark = 0;
        for (int y = 0; y < size; y++) {
            int runX = 1, runY = 1;
            for (int x = 0; x < size; x++) {
                if (modules[y][x]) dark++;
                if (x > 0) {
                    if (modules[y][x] == modules[y][x - 1]) {
                        if (++runX == 5) p += 3;
                        else if (runX > 5) p++;
                    } else runX = 1;
                    if (modules[x][y] == modules[x - 1][y]) {
                        if (++runY == 5) p += 3;
                        else if (runY > 5) p++;
                    } else runY = 1;
                }
                if (x > 0 && y > 0 && modules[y][x] == modules[y][x - 1] && modules[y][x] == modules[y - 1][x] && modules[y][x] == modules[y - 1][x - 1]) p += 3;
            }
        }
        int total = size * size;
        p += (Math.abs(dark * 20 - total * 10) + total - 1) / total * 10 - 10;
        return p;
    }

    // ---------------------------------------------------------------- Reed–Solomon over GF(256), x^8+x^4+x^3+x^2+1

    private static byte[] divisor(int degree) {
        byte[] r = new byte[degree];
        r[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                r[j] = (byte) mul(r[j] & 0xFF, root);
                if (j + 1 < degree) r[j] ^= r[j + 1];
            }
            root = mul(root, 0x02);
        }
        return r;
    }

    private static byte[] remainder(byte[] data, byte[] divisor) {
        byte[] r = new byte[divisor.length];
        for (byte b : data) {
            int factor = (b ^ r[0]) & 0xFF;
            System.arraycopy(r, 1, r, 0, r.length - 1);
            r[r.length - 1] = 0;
            for (int i = 0; i < r.length; i++) r[i] ^= mul(divisor[i] & 0xFF, factor);
        }
        return r;
    }

    private static int mul(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z & 0xFF;
    }

    private static final class Bits {
        final boolean[] b;
        int len;

        Bits(int cap) {
            b = new boolean[cap + 32];
        }

        void append(int value, int n) {
            for (int i = n - 1; i >= 0; i--) b[len++] = ((value >>> i) & 1) != 0;
        }

        boolean get(int i) {
            return b[i];
        }
    }
}
