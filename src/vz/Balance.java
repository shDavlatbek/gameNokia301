package vz;

/**
 * Every tuning number of the game lives here so a play test only has to
 * touch one file. MIDP free.
 */
public final class Balance {

    /** Simulation ticks per second (the game loop targets 66 ms). */
    public static final int TPS = 15;

    // ---- player ----
    public static final int BASE_HP = 100;
    public static final int HP_PER_VIT = 15;
    public static final int BASE_ARMOR_CAP = 50;
    public static final int ARMOR_PER_ARM = 25;
    public static final int DMG_PER_POW = 12;
    public static final int SPEED_PER_AGI = 8;
    public static final int MAX_STAT = 5;

    /** Forward speed in 16.16 cells per tick (about 1.8 cells/s). */
    public static final int MOVE_SPEED = 7860;
    public static final int STRAFE_SPEED = 6000;
    /** Turn rate in angle units per tick (2048 units per turn). */
    public static final int TURN_RATE = 46;
    /** Player collision radius, 16.16. */
    public static final int RADIUS = 16384;

    // ---- experience ----
    public static final int XP_BASE = 60;
    public static final int XP_STEP = 40;
    public static final int POINTS_PER_LEVEL = 2;

    // ---- weapons: pistol, rifle, plasma ----
    public static final int W_COUNT = 3;
    public static final int[] W_DMG = { 12, 9, 40 };
    public static final int[] W_COOLDOWN = { 5, 2, 12 };
    public static final int[] W_SPREAD = { 6, 14, 4 };
    public static final int[] W_AMMO_MAX = { 0, 150, 40 };
    public static final int[] W_AMMO_PICK = { 0, 30, 10 };
    /** Hitscan range in cells, 16.16. */
    public static final int W_RANGE = 12 * FX.ONE;

    // ---- enemies, indexed by Entity.T_* ----
    public static final int[] E_HP = { 30, 50, 140, 600 };
    public static final int[] E_SPEED = { 5900, 3300, 2300, 3300 };
    public static final int[] E_DMG = { 8, 10, 18, 25 };
    public static final int[] E_COOLDOWN = { 12, 25, 35, 20 };
    public static final int[] E_XP = { 10, 20, 45, 200 };
    /** Melee reach (drone) and firing range for the others, 16.16. */
    public static final int MELEE_RANGE = 46000;
    public static final int SHOOT_RANGE = 8 * FX.ONE;
    public static final int WAKE_RANGE = 10 * FX.ONE;
    public static final int ENEMY_RADIUS = 14000;

    public static final int TARGET_HP = 300;

    // ---- pickups ----
    public static final int MEDKIT = 40;
    public static final int ARMOR_PACK = 25;

    // ---- doors ----
    /** Ticks for a door to slide fully open. */
    public static final int DOOR_TICKS = 10;

    private Balance() {
    }

    public static int xpToNext(int level) {
        return XP_BASE + XP_STEP * level;
    }
}
