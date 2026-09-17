package vz;

/**
 * All sprite art, drawn as character grids and decoded once into column major
 * pixel arrays. A pixel of 0 is transparent, everything else carries
 * 0xFF000000 so the blitters can test it with a single compare.
 *
 * Rows shorter than the widest row are padded with transparent pixels, so the
 * art below can be edited without counting trailing dots. MIDP free.
 */
public final class Sprites {

    // frame ids
    public static final int DRONE = 0;
    public static final int TROOPER = 1;
    public static final int HEAVY = 2;
    public static final int BOSS = 3;
    public static final int NODE = 4;
    public static final int DEAD_SMALL = 5;
    public static final int DEAD_BIG = 6;
    public static final int MEDKIT = 7;
    public static final int ARMOR = 8;
    public static final int AMMO_R = 9;
    public static final int AMMO_P = 10;
    public static final int GUN_R = 11;
    public static final int GUN_P = 12;
    public static final int KEY_R = 13;
    public static final int KEY_G = 14;
    public static final int KEY_B = 15;
    public static final int VIEW_PISTOL = 16;
    public static final int VIEW_RIFLE = 17;
    public static final int VIEW_PLASMA = 18;
    public static final int FLASH = 19;
    public static final int COUNT = 20;

    public static final int[][] PIX = new int[COUNT][];
    public static final int[] W = new int[COUNT];
    public static final int[] H = new int[COUNT];

    /** Entity type to sprite frame. */
    private static final int[] FOR_TYPE = new int[Entity.T_COUNT];

    private static boolean ready;

    private Sprites() {
    }

    public static int frameFor(int entityType) {
        return FOR_TYPE[entityType];
    }

    public static int viewFor(int weapon) {
        if (weapon == Player.W_RIFLE) {
            return VIEW_RIFLE;
        }
        if (weapon == Player.W_PLASMA) {
            return VIEW_PLASMA;
        }
        return VIEW_PISTOL;
    }

    // ---- art -----------------------------------------------------------

    private static final String[] ART_DRONE = {
        "................",
        "......kkkk......",
        "....kkddddkk....",
        "...kdddbbdddk...",
        "..kddbbllbbddk..",
        ".kddbllwwllbddk.",
        ".kdbbllewellbbk.",
        "kddbbleeeeelbbdk",
        "kdbbblleeellbbdk",
        "kddbbbllllbbbddk",
        ".kdddbbbbbbdddk.",
        "..kkddddddddkk..",
        "....kkddddkk....",
        ".....kgggdk.....",
        "......kggk......",
        ".......gg.......",
    };
    private static final String KEYS_DRONE = "kdblegw";
    private static final int[] COL_DRONE = {
        0x14181E, 0x38424E, 0x5A687A, 0x8A9AAE, 0xFF4030, 0x30C8FF, 0xFFFFFF
    };

    private static final String[] ART_TROOPER = {
        "......kkkk......",
        ".....kbbbbk.....",
        "....kbllllbk....",
        "....kbeebeebk...",
        "....kbllllbk....",
        ".....kwwwwk.....",
        "...kkbbbbbbkk...",
        "..kbbbbwwbbbbk..",
        ".kbbbbbwwbbbbbk.",
        ".kbmbbbwwbbbmbk.",
        ".kmmbbbbbbbbmmk.",
        "..kkbbbbbbbbkk..",
        "...kbbbbbbbbk...",
        "...kbbbkkbbbk...",
        "...kbbk..kbbk...",
        "...kbbk..kbbk...",
        "...kbbk..kbbk...",
        "...kbbk..kbbk...",
        "...kmbk..kbmk...",
        "...kmmk..kmmk...",
        "..kkmmk..kmmkk..",
        "..kmmmk..kmmmk..",
        "..kmmk....kmmk..",
        "...kk......kk...",
    };
    private static final String KEYS_TROOPER = "kblmewg";
    private static final int[] COL_TROOPER = {
        0x100F14, 0x3E5A46, 0x5E8468, 0xA8A8B4, 0xFF5A20, 0xD8DCE4, 0x60E0A0
    };

    private static final String[] ART_HEAVY = {
        "........kkkk........",
        ".......kddddk.......",
        "......kdbbbbdk......",
        "......kdeeeedk......",
        "......kdbbbbdk......",
        ".....kkddddddkk.....",
        "...kkddddddddddkk...",
        "..kddbbbbbbbbbbddk..",
        ".kddbbbbwwwwbbbbddk.",
        "kdmdbbbwwwwwwbbbdmdk",
        "kdmmdbbwwwwwwbbdmmdk",
        "kdmmmdbbwwwwbbdmmmdk",
        ".kdmmmdbbbbbbdmmmdk.",
        "..kddmmdbbbbdmmddk..",
        "...kkddbbbbbbddkk...",
        ".....kdbbbbbbdk.....",
        ".....kdbbddbbdk.....",
        ".....kdbbk.kbbdk....",
        "....kddbbk.kbbddk...",
        "....kdmbbk.kbbmdk...",
        "....kdmmbk.kbmmdk...",
        "....kdmmbk.kbmmdk...",
        "...kkdmmbk.kbmmdkk..",
        "...kdmmmdk.kdmmmdk..",
        "...kdmmdk...kdmmdk..",
        "....kkk......kkk....",
    };
    private static final String KEYS_HEAVY = "kdbmewg";
    private static final int[] COL_HEAVY = {
        0x0C0C10, 0x2C3038, 0x4A5260, 0x8C94A4, 0xFFB020, 0xB8C0CC, 0xFF7020
    };

    private static final String[] ART_BOSS = {
        "..........kkkkkkkk..........",
        "........kkddddddddkk........",
        "......kkddddddddddddkk......",
        ".....kddddbbbbbbbbddddk.....",
        "....kdddbbbeeeeeebbbdddk....",
        "....kddbbbeewwwweebbbddk....",
        "....kddbbbeeeeeeeebbbddk....",
        "....kdddbbbbbbbbbbbbdddk....",
        "...kkdddddbbbbbbbbdddddkk...",
        "..kddddmmmdddddddmmmddddk...",
        ".kdddmmmmmmdddddmmmmmmdddk..",
        "kdddmmmwwmmmdddmmmwwmmmdddk.",
        "kddmmmwwwwmmmdmmmwwwwmmmddk.",
        "kddmmmwwwwmmmdmmmwwwwmmmddk.",
        "kdddmmmwwmmmdddmmmwwmmmdddk.",
        ".kdddmmmmmmdddddmmmmmmdddk..",
        "..kdddddmmmdddddmmmddddddk..",
        "...kkddddddbbbbbbdddddkk....",
        ".....kdddddbbbbbbddddk......",
        ".....kdddbbbggggbbbdddk.....",
        ".....kdddbbggggggbbdddk.....",
        ".....kdddbbbggggbbbdddk.....",
        "......kdddbbbbbbbbddk.......",
        "......kddddbbbbbbdddk.......",
        ".....kkdddbbk..kbbdddkk.....",
        "....kkdddddk....kdddddkk....",
        "...kkdddmmdk....kdmmdddkk...",
        "...kdddmmmdk....kdmmmdddk...",
        "..kdddmmmmdk....kdmmmmdddk..",
        "..kddmmmmdk......kdmmmmddk..",
        "..kdmmmmdk........kdmmmmdk..",
        "..kdmmdk............kdmmdk..",
        "...kkk................kkk...",
        "............................",
    };
    private static final String KEYS_BOSS = "kdbmewg";
    private static final int[] COL_BOSS = {
        0x0A0A0E, 0x24242E, 0x3C4050, 0x6E7688, 0xFF3040, 0xFFE0A0, 0xFF8020
    };

    private static final String[] ART_NODE = {
        "....kkkkkkkk....",
        "...kmmmmmmmmk...",
        "...kmddddddmk...",
        "...kmdmmmmdmk...",
        "....kmmmmmmk....",
        "....kdmggmdk....",
        "...kdmgggggmdk..",
        "..kdmggwwgggmdk.",
        "..kdmggwwgggmdk.",
        "..kdmgggggggmdk.",
        "..kdmmgggggmmdk.",
        "...kdmmgggmmdk..",
        "....kdmmmmmdk...",
        "....kmdddddmk...",
        "....kmdmmmdmk...",
        "....kmdmmmdmk...",
        "....kmdmmmdmk...",
        "...kmmdmmmdmmk..",
        "...kmmdddddmmk..",
        "..kmmmmmmmmmmmk.",
        "..kmmdddddddmmk.",
        ".kmmmmmmmmmmmmmk",
        ".kmdddddddddddmk",
        ".kmmmmmmmmmmmmmk",
    };
    private static final String KEYS_NODE = "kmdgw";
    private static final int[] COL_NODE = {
        0x0C1014, 0x6A7280, 0x2E343C, 0x30E0C0, 0xEAFFFF
    };

    private static final String[] ART_DEAD_SMALL = {
        "................",
        "................",
        "....kkk..kk.....",
        "..kkdddkkddkk...",
        ".kdddbbdddbbdk..",
        "kddbbbbdbbbbddk.",
        "kdbbdddbbdddbbdk",
        ".kkkkkkkkkkkkkk.",
    };
    private static final String KEYS_DEAD = "kdb";
    private static final int[] COL_DEAD = { 0x0C0C10, 0x33363E, 0x555C68 };

    private static final String[] ART_DEAD_BIG = {
        "....................",
        "......kkk..kkk......",
        "...kkkdddkkdddkkk...",
        "..kdddbbbddbbbdddk..",
        ".kddbbbbbddbbbbbddk.",
        "kddbbbdddbbdddbbbddk",
        "kdbbddbbbddbbbddbbdk",
        "kdbdddbbdddbbdddbbdk",
        ".kkkkkkkkkkkkkkkkkk.",
        "....................",
    };

    private static final String[] ART_MEDKIT = {
        "..kkkkkkkkkk",
        ".kwwwwwwwwwk",
        "kwwwwwwwwwwk",
        "kwwwwwrwwwwk",
        "kwwwwwrwwwwk",
        "kwwwrrrrrwwk",
        "kwwwrrrrrwwk",
        "kwwwwwrwwwwk",
        "kwwwwwrwwwwk",
        "kwwwwwwwwwwk",
        ".kwwwwwwwwwk",
        "..kkkkkkkkkk",
    };
    private static final String KEYS_MEDKIT = "kwr";
    private static final int[] COL_MEDKIT = { 0x202428, 0xE8ECF0, 0xE02830 };

    private static final String[] ART_ARMOR = {
        "..kkkkkkkk..",
        ".kbbbbbbbbk.",
        "kbblllllbbbk",
        "kblllwwlllbk",
        "kblllwwlllbk",
        "kbllwwwwllbk",
        "kbbllwwllbbk",
        "kbbblllbbbbk",
        ".kbbbllbbbk.",
        ".kbbbbbbbbk.",
        "..kbbbbbbk..",
        "...kkkkkk...",
    };
    private static final String KEYS_ARMOR = "kblw";
    private static final int[] COL_ARMOR = { 0x14202C, 0x2A5A90, 0x4A8AC8, 0xC8E8FF };

    private static final String[] ART_AMMO_R = {
        "............",
        "..kkkkkkkk..",
        ".kddddddddk.",
        "kdggggggggdk",
        "kdgyyggyygdk",
        "kdgyyggyygdk",
        "kdggggggggdk",
        "kdgyyggyygdk",
        "kdgyyggyygdk",
        "kdggggggggdk",
        ".kddddddddk.",
        "..kkkkkkkk..",
    };
    private static final String KEYS_AMMO = "kdgy";
    private static final int[] COL_AMMO_R = { 0x101410, 0x2A3A28, 0x4A6A44, 0xD8C040 };

    private static final String[] ART_AMMO_P = {
        "...kkkkkk...",
        "..kddddddk..",
        "..kdppppdk..",
        "..kdpwwpdk..",
        "..kdpwwpdk..",
        "..kdpwwpdk..",
        "..kdpwwpdk..",
        "..kdpwwpdk..",
        "..kdppppdk..",
        "..kddddddk..",
        "...kkkkkk...",
        "............",
    };
    private static final String KEYS_AMMO_P = "kdpw";
    private static final int[] COL_AMMO_P = { 0x140C20, 0x30204A, 0x8040E0, 0xE0C0FF };

    private static final String[] ART_GUN_R = {
        "................",
        "................",
        "...kkkkkkkkkkk..",
        "..kmmmmmmmmmmmk.",
        ".kmmllllllllmmmk",
        "kmmmmmmmmmmmmmmk",
        "kmmkkkkkkkkkkkk.",
        "kmmk............",
        ".kmmk...........",
        "..kmmmk.........",
        "...kmmmk........",
        "....kkkk........",
    };
    private static final String KEYS_GUN = "kml";
    private static final int[] COL_GUN_R = { 0x101216, 0x7A8290, 0xAEB6C6 };

    private static final String[] ART_GUN_P = {
        "................",
        ".....kkkkkk.....",
        "...kkppppppkk...",
        "..kppwwwwwwppk..",
        ".kppwwppppwwppk.",
        "kkppwwppppwwppkk",
        "kppppwwwwwwppppk",
        "kkkppppppppppkkk",
        "...kkppppppkk...",
        "....kkpppkk.....",
        ".....kkkkk......",
        "................",
    };
    private static final String KEYS_GUN_P = "kpw";
    private static final int[] COL_GUN_P = { 0x140C20, 0x8040E0, 0xE8D0FF };

    private static final String[] ART_KEY = {
        "............",
        "..kkkkkkkk..",
        ".kmmmmmmmmk.",
        "kmccccccccmk",
        "kmcwwwwwwcmk",
        "kmcwccccwcmk",
        "kmcwwwwwwcmk",
        "kmccccccccmk",
        "kmmmmmmmmmmk",
        ".kmmkkkkmmk.",
        "..kk....kk..",
        "............",
    };
    private static final String KEYS_KEY = "kmcw";

    /** First person weapon views, drawn at double size. */
    private static final String[] ART_VIEW_PISTOL = {
        "........................................",
        "........................................",
        "..................kkkkkk................",
        "................kkmmmmmmkk..............",
        "...............kmmmmmmmmmmk.............",
        "..............kmmmllllmmmmk.............",
        "..............kmmlllllllmmk.............",
        "..............kmmlllllllmmk.............",
        "..............kmmmllllmmmmk.............",
        "..............kkmmmmmmmmmkk.............",
        "...............kkmmmmmmmkk..............",
        "..............kdkkmmmmmkkdk.............",
        "...........kddddkkmmmmmkkdddk...........",
        "........kdddddddkkmmmmmkkddddddk........",
        ".....kddddddddddkkmmmmmkkdddddddddk.....",
        "..kddddddddddddkkkmmmkkkdddddddddddddk..",
        "kddddddwwddddddkkkkkkkkkddddddwwddddddd.",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
    };
    private static final String KEYS_VIEW = "kmldwgey";
    private static final int[] COL_VIEW_PISTOL = {
        0x0C0E12, 0x6A7280, 0x9AA4B4, 0x3A4450, 0xE8ECF4, 0x40E0FF, 0xFFC040, 0xFFF080
    };

    private static final String[] ART_VIEW_RIFLE = {
        "..............kkkkkkkkkkkk..............",
        ".............kmmmmmmmmmmmmk.............",
        ".............kmllllllllllmk.............",
        ".............kmmmmmmmmmmmmk.............",
        "..............kkkmmmmmmkkk..............",
        "...............kmmmmmmmmk...............",
        "..............kmmmmmmmmmmk..............",
        "..............kmmllllllmmk..............",
        "..............kmmllllllmmk..............",
        "..............kmmmmmmmmmmk..............",
        "..............kmmmmmmmmmmk..............",
        "..............kmmmmmmmmmmk..............",
        "............kdkkmmmmmmmmkkdk............",
        ".........kddddkkmmmmmmmmkkddddk.........",
        "......kdddddddkkmmmmmmmmkkdddddddk......",
        "...kddddddddddkkkmmmmmmkkkddddddddddk...",
        "kdddddddwwddddkkkkkkkkkkddddddwwdddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
    };

    private static final String[] ART_VIEW_PLASMA = {
        "................kkkkkkkk................",
        "..............kkggggggggkk..............",
        ".............kgggeeeeeegggk.............",
        "............kgggeeewwweeegggk...........",
        "............kggeeewwwwweeeggk...........",
        "............kggeeewwwwweeeggk...........",
        "............kgggeeewwweeegggk...........",
        ".............kgggeeeeeegggk.............",
        "..............kkggggggggkk..............",
        "..............kmmmmmmmmmmk..............",
        "..............kmmllllllmmk..............",
        "..............kmmmmmmmmmmk..............",
        "............kdkkmmmmmmmmkkdk............",
        ".........kddddkkmmmmmmmmkkddddk.........",
        "......kdddddddkkmmmmmmmmkkdddddddk......",
        "...kddddddddddkkkmmmmmmkkkddddddddddk...",
        "kdddddddwwddddkkkkkkkkkkddddddwwdddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
        "kdddddddddddddddddddddddddddddddddddddd",
    };

    private static final String[] ART_FLASH = {
        "....ww....",
        "..w.ww.w..",
        "...ywy....",
        ".wyyoyyw..",
        "wwyoooyww.",
        ".wyyoyyw..",
        "...ywy....",
        "..w.ww.w..",
        "....ww....",
        "..........",
    };
    private static final String KEYS_FLASH = "woy";
    private static final int[] COL_FLASH = { 0xFFFFE0, 0xFF8018, 0xFFD040 };

    // ---- decoding ------------------------------------------------------

    public static synchronized void init() {
        if (ready) {
            return;
        }
        put(DRONE, ART_DRONE, KEYS_DRONE, COL_DRONE);
        put(TROOPER, ART_TROOPER, KEYS_TROOPER, COL_TROOPER);
        put(HEAVY, ART_HEAVY, KEYS_HEAVY, COL_HEAVY);
        put(BOSS, ART_BOSS, KEYS_BOSS, COL_BOSS);
        put(NODE, ART_NODE, KEYS_NODE, COL_NODE);
        put(DEAD_SMALL, ART_DEAD_SMALL, KEYS_DEAD, COL_DEAD);
        put(DEAD_BIG, ART_DEAD_BIG, KEYS_DEAD, COL_DEAD);
        put(MEDKIT, ART_MEDKIT, KEYS_MEDKIT, COL_MEDKIT);
        put(ARMOR, ART_ARMOR, KEYS_ARMOR, COL_ARMOR);
        put(AMMO_R, ART_AMMO_R, KEYS_AMMO, COL_AMMO_R);
        put(AMMO_P, ART_AMMO_P, KEYS_AMMO_P, COL_AMMO_P);
        put(GUN_R, ART_GUN_R, KEYS_GUN, COL_GUN_R);
        put(GUN_P, ART_GUN_P, KEYS_GUN_P, COL_GUN_P);
        put(KEY_R, ART_KEY, KEYS_KEY, new int[] { 0x181C20, 0x9098A4, 0xE03028, 0xFFD0C8 });
        put(KEY_G, ART_KEY, KEYS_KEY, new int[] { 0x181C20, 0x9098A4, 0x28B048, 0xC8FFD0 });
        put(KEY_B, ART_KEY, KEYS_KEY, new int[] { 0x181C20, 0x9098A4, 0x3060E0, 0xC8D8FF });
        put(VIEW_PISTOL, ART_VIEW_PISTOL, KEYS_VIEW, COL_VIEW_PISTOL);
        put(VIEW_RIFLE, ART_VIEW_RIFLE, KEYS_VIEW, COL_VIEW_PISTOL);
        put(VIEW_PLASMA, ART_VIEW_PLASMA, KEYS_VIEW, COL_VIEW_PISTOL);
        put(FLASH, ART_FLASH, KEYS_FLASH, COL_FLASH);

        FOR_TYPE[Entity.T_DRONE] = DRONE;
        FOR_TYPE[Entity.T_TROOPER] = TROOPER;
        FOR_TYPE[Entity.T_HEAVY] = HEAVY;
        FOR_TYPE[Entity.T_BOSS] = BOSS;
        FOR_TYPE[Entity.T_NODE] = NODE;
        FOR_TYPE[Entity.T_MEDKIT] = MEDKIT;
        FOR_TYPE[Entity.T_ARMOR] = ARMOR;
        FOR_TYPE[Entity.T_AMMO_R] = AMMO_R;
        FOR_TYPE[Entity.T_AMMO_P] = AMMO_P;
        FOR_TYPE[Entity.T_GUN_R] = GUN_R;
        FOR_TYPE[Entity.T_GUN_P] = GUN_P;
        FOR_TYPE[Entity.T_KEY_R] = KEY_R;
        FOR_TYPE[Entity.T_KEY_G] = KEY_G;
        FOR_TYPE[Entity.T_KEY_B] = KEY_B;
        ready = true;
    }

    private static void put(int id, String[] art, String keys, int[] colors) {
        int h = art.length;
        int w = 0;
        for (int y = 0; y < h; y++) {
            if (art[y].length() > w) {
                w = art[y].length();
            }
        }
        int[] pix = new int[w * h];
        for (int y = 0; y < h; y++) {
            String row = art[y];
            int len = row.length();
            for (int x = 0; x < w; x++) {
                int c = 0;
                if (x < len) {
                    char ch = row.charAt(x);
                    if (ch != '.' && ch != ' ') {
                        int k = keys.indexOf(ch);
                        if (k >= 0 && k < colors.length) {
                            c = 0xFF000000 | colors[k];
                        } else {
                            c = 0xFFFF00FF;   // missing palette entry, very visible
                        }
                    }
                }
                pix[x * h + y] = c;
            }
        }
        PIX[id] = pix;
        W[id] = w;
        H[id] = h;
    }
}
