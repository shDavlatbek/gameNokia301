package vz;

/**
 * Fixed point math for Vanguard Zero.
 *
 * Positions are 16.16 fixed point where ONE is one map cell.
 * Angles are integers 0..ANG-1 (ANG units make a full turn), so all
 * trigonometry is a table lookup and no floating point is used at runtime.
 *
 * This class must stay free of javax.microedition.* so the desktop
 * verification harness can use it (see build.sh "check").
 */
public final class FX {

    public static final int ONE = 65536;
    public static final int HALF = ONE / 2;

    /** Angle units in a full turn. */
    public static final int ANG = 2048;
    public static final int ANG_MASK = ANG - 1;

    /** sin(a) in 16.16 for every angle unit. */
    public static final int[] SIN = new int[ANG];

    /** tan(30 degrees) in 16.16: half of the 60 degree field of view. */
    public static final int TAN30 = 37837;

    private static int seed = 0x2545F491;

    static {
        for (int i = 0; i < ANG; i++) {
            double a = (double) i * 2.0 * Math.PI / (double) ANG;
            double s = Math.sin(a);
            SIN[i] = (int) (s * 65536.0 + (s < 0 ? -0.5 : 0.5));
        }
    }

    private FX() {
    }

    public static int sin(int a) {
        return SIN[a & ANG_MASK];
    }

    public static int cos(int a) {
        return SIN[(a + (ANG >> 2)) & ANG_MASK];
    }

    /** Multiply two 16.16 values. */
    public static int mul(int a, int b) {
        return (int) (((long) a * (long) b) >> 16);
    }

    /** Divide two 16.16 values. */
    public static int div(int a, int b) {
        if (b == 0) {
            return a < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return (int) ((((long) a) << 16) / (long) b);
    }

    /** Integer square root of a non negative long. */
    public static int isqrt(long v) {
        if (v <= 0) {
            return 0;
        }
        long rem = 0;
        long root = 0;
        for (int i = 0; i < 32; i++) {
            root <<= 1;
            rem = (rem << 2) | ((v >> 62) & 3);
            v <<= 2;
            if (root < rem) {
                root++;
                rem -= root;
                root++;
            }
        }
        return (int) (root >> 1);
    }

    /** Length of a 16.16 vector, in 16.16. */
    public static int len(int dx, int dy) {
        return isqrt((long) dx * (long) dx + (long) dy * (long) dy);
    }

    /** Angle of a 16.16 vector, in angle units. Table based, no atan2. */
    public static int angleOf(int dx, int dy) {
        int l = len(dx, dy);
        if (l == 0) {
            return 0;
        }
        int c = div(dx, l);
        if (c > ONE) {
            c = ONE;
        } else if (c < -ONE) {
            c = -ONE;
        }
        // Binary search the quarter turn where cos decreases monotonically.
        int lo = 0;
        int hi = ANG >> 1;
        while (lo < hi) {
            int mid = (lo + hi) >> 1;
            if (cos(mid) > c) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return dy >= 0 ? lo : (ANG - lo) & ANG_MASK;
    }

    /** Shortest signed difference from angle a to angle b. */
    public static int angDiff(int a, int b) {
        int d = (b - a) & ANG_MASK;
        if (d > (ANG >> 1)) {
            d -= ANG;
        }
        return d;
    }

    public static int abs(int v) {
        return v < 0 ? -v : v;
    }

    public static int clamp(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        return v > hi ? hi : v;
    }

    public static void setSeed(int s) {
        seed = s == 0 ? 1 : s;
    }

    /** Fast xorshift pseudo random, no allocation. */
    public static int rnd() {
        int x = seed;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        seed = x;
        return x;
    }

    /** Pseudo random value in 0..n-1. */
    public static int rnd(int n) {
        if (n <= 1) {
            return 0;
        }
        int v = rnd();
        if (v < 0) {
            v = -v;
            if (v < 0) {
                v = 0;
            }
        }
        return v % n;
    }
}
