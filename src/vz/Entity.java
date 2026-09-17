package vz;

/**
 * One thing in the world: an enemy, a reactor node or a pickup.
 * Instances are created once into a pool and reused, so gameplay never
 * allocates. MIDP free.
 */
public final class Entity {

    // enemies, must stay 0..3 to index the Balance.E_* tables
    public static final int T_DRONE = 0;
    public static final int T_TROOPER = 1;
    public static final int T_HEAVY = 2;
    public static final int T_BOSS = 3;
    // destructible objective
    public static final int T_NODE = 4;
    // pickups
    public static final int T_MEDKIT = 5;
    public static final int T_ARMOR = 6;
    public static final int T_AMMO_R = 7;
    public static final int T_AMMO_P = 8;
    public static final int T_GUN_R = 9;
    public static final int T_GUN_P = 10;
    public static final int T_KEY_R = 11;
    public static final int T_KEY_G = 12;
    public static final int T_KEY_B = 13;
    public static final int T_COUNT = 14;

    // states
    public static final int S_IDLE = 0;
    public static final int S_CHASE = 1;
    public static final int S_ATTACK = 2;
    public static final int S_PAIN = 3;
    public static final int S_DEAD = 4;

    public int type;
    public int x;
    public int y;
    public int hp;
    public int state;
    public int timer;
    public int cooldown;
    public int stuck;
    public int dirX;
    public int dirY;
    public boolean active;
    /** Cached line of sight to the player, refreshed every few ticks. */
    public boolean sees;
    /** Camera space depth, filled in by the raycaster each frame. */
    public int depth;
    public int screenX;

    public boolean isEnemy() {
        return type <= T_BOSS;
    }

    public boolean isPickup() {
        return type >= T_MEDKIT;
    }

    public boolean blocksShots() {
        return type <= T_NODE;
    }

    public void spawn(int t, int cellX, int cellY) {
        type = t;
        x = (cellX << 16) + FX.HALF;
        y = (cellY << 16) + FX.HALF;
        state = S_IDLE;
        timer = 0;
        cooldown = 0;
        stuck = 0;
        dirX = 0;
        dirY = 0;
        active = true;
        sees = false;
        depth = 0;
        screenX = 0;
        if (t <= T_BOSS) {
            hp = Balance.E_HP[t];
        } else if (t == T_NODE) {
            hp = Balance.TARGET_HP;
        } else {
            hp = 1;
        }
    }
}
