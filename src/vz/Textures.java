package vz;

/**
 * Wall textures, generated in code so the jar carries no image files.
 *
 * Every texture is 32x32 stored column major (pix[x * 32 + y]) because the
 * raycaster reads one vertical strip at a time. Four pre shaded copies remove
 * all per pixel lighting math from the inner loop. MIDP free.
 */
public final class Textures {

    public static final int SIZE = 32;
    public static final int MASK = 31;
    public static final int AREA = SIZE * SIZE;
    public static final int SHADES = 4;

    // texture slots
    public static final int T_METAL = 0;
    public static final int T_TECH = 1;
    public static final int T_VENT = 2;
    public static final int T_HULL = 3;
    public static final int T_DOOR = 4;
    public static final int T_LOCK_R = 5;
    public static final int T_LOCK_G = 6;
    public static final int T_LOCK_B = 7;
    public static final int T_EXIT = 8;
    public static final int COUNT = 9;

    /** [shade][texture][x * 32 + y] */
    public static final int[][][] PIX = new int[SHADES][COUNT][];

    /** Cell value to texture slot. */
    private static final int[] SLOT = new int[16];

    private static final int[] SHADE_NUM = { 256, 208, 158, 112 };

    private static boolean ready;

    private Textures() {
    }

    public static int slotFor(int cell) {
        return SLOT[cell & 15];
    }

    public static synchronized void init() {
        if (ready) {
            return;
        }
        SLOT[Level.W_METAL] = T_METAL;
        SLOT[Level.W_TECH] = T_TECH;
        SLOT[Level.W_VENT] = T_VENT;
        SLOT[Level.W_HULL] = T_HULL;
        SLOT[Level.DOOR] = T_DOOR;
        SLOT[Level.LOCK_R] = T_LOCK_R;
        SLOT[Level.LOCK_G] = T_LOCK_G;
        SLOT[Level.LOCK_B] = T_LOCK_B;
        SLOT[Level.EXIT] = T_EXIT;

        int[][] base = new int[COUNT][AREA];
        FX.setSeed(0x51ED270B);
        metal(base[T_METAL]);
        tech(base[T_TECH]);
        vent(base[T_VENT]);
        hull(base[T_HULL]);
        door(base[T_DOOR], 0x00000000);
        door(base[T_LOCK_R], 0xD03830);
        door(base[T_LOCK_G], 0x38C050);
        door(base[T_LOCK_B], 0x3878E0);
        exit(base[T_EXIT]);

        for (int s = 0; s < SHADES; s++) {
            for (int t = 0; t < COUNT; t++) {
                if (s == 0) {
                    PIX[s][t] = base[t];
                } else {
                    int[] src = base[t];
                    int[] dst = new int[AREA];
                    int num = SHADE_NUM[s];
                    for (int i = 0; i < AREA; i++) {
                        dst[i] = scale(src[i], num);
                    }
                    PIX[s][t] = dst;
                }
            }
        }
        ready = true;
    }

    /** Multiply an 0xRRGGBB colour by num/256. */
    public static int scale(int c, int num) {
        int r = (((c >> 16) & 0xFF) * num) >> 8;
        int g = (((c >> 8) & 0xFF) * num) >> 8;
        int b = ((c & 0xFF) * num) >> 8;
        return (r << 16) | (g << 8) | b;
    }

    private static int mix(int a, int b, int t) {
        int ia = 256 - t;
        int r = ((((a >> 16) & 0xFF) * ia) + (((b >> 16) & 0xFF) * t)) >> 8;
        int g = ((((a >> 8) & 0xFF) * ia) + (((b >> 8) & 0xFF) * t)) >> 8;
        int bl = (((a & 0xFF) * ia) + ((b & 0xFF) * t)) >> 8;
        return (r << 16) | (g << 8) | bl;
    }

    private static void put(int[] p, int x, int y, int c) {
        p[(x & MASK) * SIZE + (y & MASK)] = c;
    }

    private static int get(int[] p, int x, int y) {
        return p[(x & MASK) * SIZE + (y & MASK)];
    }

    private static void fill(int[] p, int c) {
        for (int i = 0; i < AREA; i++) {
            p[i] = c;
        }
    }

    /** Grain that keeps the flat colours from looking like plastic. */
    private static void grain(int[] p, int amount) {
        for (int i = 0; i < AREA; i++) {
            int n = (FX.rnd(amount * 2 + 1) - amount);
            int r = FX.clamp(((p[i] >> 16) & 0xFF) + n, 0, 255);
            int g = FX.clamp(((p[i] >> 8) & 0xFF) + n, 0, 255);
            int b = FX.clamp((p[i] & 0xFF) + n, 0, 255);
            p[i] = (r << 16) | (g << 8) | b;
        }
    }

    private static void metal(int[] p) {
        fill(p, 0x6B707A);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int c = get(p, x, y);
                // two plates with a recessed seam between them
                if (y == 15 || y == 16 || x == 0) {
                    c = 0x3A3E46;
                } else if (y == 14 || y == 17) {
                    c = 0x8A909C;
                } else if (x == 1) {
                    c = 0x828894;
                }
                // shallow vertical shading inside each plate
                int band = (y & 15);
                if (band > 2 && band < 13) {
                    c = mix(c, 0x50555E, (band - 2) * 8);
                }
                put(p, x, y, c);
            }
        }
        // rivets
        for (int y = 4; y < SIZE; y += 11) {
            for (int x = 5; x < SIZE; x += 10) {
                put(p, x, y, 0x9AA0AC);
                put(p, x + 1, y, 0x4A4E56);
                put(p, x, y + 1, 0x4A4E56);
            }
        }
        grain(p, 5);
    }

    private static void tech(int[] p) {
        fill(p, 0x27314A);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int c = get(p, x, y);
                if (x == 0 || x == 16) {
                    c = 0x161C2A;
                } else if (y == 0) {
                    c = 0x161C2A;
                } else if (y > 3 && y < 8 && (x & 15) > 2 && (x & 15) < 13) {
                    // lit indicator panel
                    c = ((x + y) & 3) == 0 ? 0x5FE6F0 : 0x2E7C8E;
                } else if (y > 20 && y < 24) {
                    c = mix(c, 0x0E1220, 120);
                }
                put(p, x, y, c);
            }
        }
        // a few cable runs
        for (int x = 0; x < SIZE; x++) {
            put(p, x, 12, 0x1B2233);
            put(p, x, 13, 0x364260);
            put(p, x, 27, 0x1B2233);
        }
        grain(p, 4);
    }

    private static void vent(int[] p) {
        fill(p, 0x4A5058);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int c;
                int s = y & 3;
                if (s == 0) {
                    c = 0x1C2026;
                } else if (s == 1) {
                    c = 0x2A2F36;
                } else if (s == 2) {
                    c = 0x656C76;
                } else {
                    c = 0x4E545C;
                }
                if (x == 0 || x == 15 || x == 31) {
                    c = 0x343A42;
                }
                put(p, x, y, c);
            }
        }
        grain(p, 3);
    }

    private static void hull(int[] p) {
        // value noise: random 8x8 lattice smoothed twice
        int[] lat = new int[AREA];
        for (int y = 0; y < SIZE; y += 8) {
            for (int x = 0; x < SIZE; x += 8) {
                int v = 90 + FX.rnd(60);
                for (int yy = 0; yy < 8; yy++) {
                    for (int xx = 0; xx < 8; xx++) {
                        lat[((x + xx) & MASK) * SIZE + ((y + yy) & MASK)] = v;
                    }
                }
            }
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int s = lat[((x - 1) & MASK) * SIZE + (y & MASK)]
                            + lat[((x + 1) & MASK) * SIZE + (y & MASK)]
                            + lat[(x & MASK) * SIZE + ((y - 1) & MASK)]
                            + lat[(x & MASK) * SIZE + ((y + 1) & MASK)]
                            + lat[(x & MASK) * SIZE + (y & MASK)] * 4;
                    lat[(x & MASK) * SIZE + (y & MASK)] = s >> 3;
                }
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int v = lat[x * SIZE + y];
                int c = ((v * 5 / 4) << 16) | ((v * 9 / 8) << 8) | v;
                if (((x + (y >> 3)) & 15) == 0) {
                    c = mix(c, 0x20242C, 150);
                }
                put(p, x, y, c);
            }
        }
        grain(p, 4);
    }

    private static void door(int[] p, int lightRgb) {
        fill(p, 0x7A7F88);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int c = get(p, x, y);
                if (x == 15 || x == 16) {
                    c = 0x24282E;              // centre seam
                } else if (x < 2 || x > 29) {
                    c = 0x3C4048;              // frame
                } else if (y < 3 || y > 28) {
                    c = 0x51565E;
                } else if (y > 6 && y < 11) {
                    // hazard stripes
                    c = ((x + y) & 7) < 4 ? 0xD8B02A : 0x2A2E34;
                } else if (y > 12 && y < 26) {
                    c = mix(c, 0x5C6169, ((y - 12) * 10) & 255);
                }
                put(p, x, y, c);
            }
        }
        if (lightRgb != 0) {
            for (int y = 17; y < 21; y++) {
                for (int x = 4; x < 13; x++) {
                    put(p, x, y, lightRgb);
                    put(p, x + 15, y, lightRgb);
                }
            }
            for (int x = 4; x < 13; x++) {
                put(p, x, 16, scale(lightRgb, 140));
                put(p, x + 15, 16, scale(lightRgb, 140));
            }
        }
        grain(p, 3);
    }

    private static void exit(int[] p) {
        fill(p, 0x123024);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int c;
                int d = (x + y) & 15;
                if (d < 6) {
                    c = 0x2FE07A;
                } else if (d < 9) {
                    c = 0x1C8A4C;
                } else {
                    c = 0x0E241C;
                }
                if (x < 2 || x > 29 || y < 2 || y > 29) {
                    c = 0x3A4048;
                }
                put(p, x, y, c);
            }
        }
        grain(p, 3);
    }
}
