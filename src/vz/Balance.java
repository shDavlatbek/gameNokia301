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
    /** Player collision radius, 16.16. A fifth of a cell is forgiving. */
    public static final int RADIUS = 13107;
    /** How far per tick the player is eased back to the corridor centre. */
    public static final int NUDGE = 3000;

    // ---- experience ----
    public static final int XP_BASE = 60;
    public static final int XP_STEP = 40;
    public static final int POINTS_PER_LEVEL = 2;

    // ---- weapons: pistol, rifle, plasma ----
    public static final int W_COUNT = 3;
    public static final int[] W_DMG = { 12, 11, 40 };
    public static final int[] W_COOLDOWN = { 5, 3, 12 };
    /**
     * Aim slack in angle units (2048 per turn) added to the target's own
     * silhouette. A keypad can only turn in steps, so the window has to be
     * generous or shooting feels broken: 80 units is about 14 degrees.
     */
    public static final int[] W_SPREAD = { 80, 100, 60 };
    public static final int[] W_AMMO_MAX = { 0, 150, 40 };
    public static final int[] W_AMMO_PICK = { 0, 40, 12 };
    /** Hitscan range in cells, 16.16. */
    public static final int W_RANGE = 12 * FX.ONE;

    // ---- enemies, indexed by Entity.T_* ----
    public static final int[] E_HP = { 30, 50, 140, 500 };
    public static final int[] E_SPEED = { 5900, 3300, 2300, 3300 };
    public static final int[] E_DMG = { 7, 9, 16, 22 };
    public static final int[] E_COOLDOWN = { 18, 30, 40, 25 };
    public static final int[] E_XP = { 10, 20, 45, 200 };
    /** Melee reach (drone) and firing range for the others, 16.16. */
    public static final int MELEE_RANGE = 46000;
    public static final int SHOOT_RANGE = 8 * FX.ONE;
    public static final int WAKE_RANGE = 10 * FX.ONE;
    /** How far gunfire carries. Shorter than sight, so packs wake in turn. */
    public static final int NOISE_RANGE = 5 * FX.ONE;
    public static final int ENEMY_RADIUS = 14000;

    public static final int TARGET_HP = 300;

    // ---- pickups ----
    public static final int MEDKIT = 45;
    public static final int ARMOR_PACK = 30;
    /** Armour every mission starts with, whatever was left over. */
    public static final int START_ARMOR = 20;
    /** Ticks an enemy needs to react after it notices the player. */
    public static final int REACTION = 8;
    /** Hit chance at point blank range, falling by 6 per cell. */
    public static final int ACCURACY = 70;

    // ---- doors ----
    /** Ticks for a door to slide fully open. */
    public static final int DOOR_TICKS = 10;

    private Balance() {
    }

    public static int xpToNext(int level) {
        return XP_BASE + XP_STEP * level;
    }
}
