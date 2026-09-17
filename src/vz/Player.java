package vz;

/**
 * Player progress: health, gear and the RPG stats.
 * Also owns the save record layout so the desktop tests can round trip it
 * without touching RMS. MIDP free.
 */
public final class Player {

    public static final int SAVE_BYTES = 32;
    public static final int W_PISTOL = 0;
    public static final int W_RIFLE = 1;
    public static final int W_PLASMA = 2;

    public int level;
    public int xp;
    public int points;
    public int vit;
    public int pow;
    public int agi;
    public int arm;

    public int hp;
    public int armor;
    public int weapons;
    public int weapon;
    public int ammoR;
    public int ammoP;
    public int keys;

    public int curLevel;
    public int kills;
    public int ticks;

    public Player() {
        reset();
    }

    public void reset() {
        level = 1;
        xp = 0;
        points = 0;
        vit = 0;
        pow = 0;
        agi = 0;
        arm = 0;
        weapons = 1;
        weapon = W_PISTOL;
        ammoR = 0;
        ammoP = 0;
        keys = 0;
        curLevel = 0;
        kills = 0;
        ticks = 0;
        hp = maxHp();
        armor = 0;
    }

    public int maxHp() {
        return Balance.BASE_HP + Balance.HP_PER_VIT * vit;
    }

    public int armorCap() {
        return Balance.BASE_ARMOR_CAP + Balance.ARMOR_PER_ARM * arm;
    }

    public int dmgPct() {
        return 100 + Balance.DMG_PER_POW * pow;
    }

    public int speedPct() {
        return 100 + Balance.SPEED_PER_AGI * agi;
    }

    public int xpToNext() {
        return Balance.xpToNext(level);
    }

    /** Start of a level: full health, keycards dropped, a little armour. */
    public void enterLevel() {
        hp = maxHp();
        keys = 0;
        if (armor < Balance.START_ARMOR) {
            armor = Balance.START_ARMOR;
        }
        if (armor > armorCap()) {
            armor = armorCap();
        }
        if ((weapons & (1 << weapon)) == 0) {
            weapon = W_PISTOL;
        }
    }

    public boolean hasWeapon(int w) {
        return (weapons & (1 << w)) != 0;
    }

    public int ammo(int w) {
        if (w == W_RIFLE) {
            return ammoR;
        }
        if (w == W_PLASMA) {
            return ammoP;
        }
        return -1;
    }

    public void useAmmo(int w) {
        if (w == W_RIFLE && ammoR > 0) {
            ammoR--;
        } else if (w == W_PLASMA && ammoP > 0) {
            ammoP--;
        }
    }

    public void addAmmo(int w, int n) {
        if (w == W_RIFLE) {
            ammoR = FX.clamp(ammoR + n, 0, Balance.W_AMMO_MAX[W_RIFLE]);
        } else if (w == W_PLASMA) {
            ammoP = FX.clamp(ammoP + n, 0, Balance.W_AMMO_MAX[W_PLASMA]);
        }
    }

    /** Next weapon that is owned and loaded, searching in the given direction. */
    public int nextWeapon(int dir) {
        for (int i = 1; i <= Balance.W_COUNT; i++) {
            int w = (weapon + dir * i + Balance.W_COUNT * 4) % Balance.W_COUNT;
            if (hasWeapon(w) && ammo(w) != 0) {
                return w;
            }
        }
        return weapon;
    }

    /** Apply damage through armour. Returns true when the player dies. */
    public boolean hurt(int dmg) {
        if (dmg <= 0) {
            return false;
        }
        if (armor > 0) {
            int soak = dmg >> 1;
            if (soak > armor) {
                soak = armor;
            }
            armor -= soak;
            dmg -= soak;
        }
        hp -= dmg;
        if (hp < 0) {
            hp = 0;
        }
        return hp == 0;
    }

    public void heal(int n) {
        hp = FX.clamp(hp + n, 0, maxHp());
    }

    public void addArmor(int n) {
        armor = FX.clamp(armor + n, 0, armorCap());
    }

    /** Award experience. Returns the number of levels gained. */
    public int gainXp(int n) {
        xp += n;
        int gained = 0;
        while (xp >= xpToNext()) {
            xp -= xpToNext();
            level++;
            points += Balance.POINTS_PER_LEVEL;
            gained++;
        }
        return gained;
    }

    /** Spend one point. Stat 0 vitality, 1 power, 2 agility, 3 armour. */
    public boolean spend(int stat) {
        if (points <= 0) {
            return false;
        }
        switch (stat) {
            case 0:
                if (vit >= Balance.MAX_STAT) {
                    return false;
                }
                vit++;
                hp += Balance.HP_PER_VIT;
                break;
            case 1:
                if (pow >= Balance.MAX_STAT) {
                    return false;
                }
                pow++;
                break;
            case 2:
                if (agi >= Balance.MAX_STAT) {
                    return false;
                }
                agi++;
                break;
            default:
                if (arm >= Balance.MAX_STAT) {
                    return false;
                }
                arm++;
                break;
        }
        points--;
        return true;
    }

    public int stat(int i) {
        switch (i) {
            case 0: return vit;
            case 1: return pow;
            case 2: return agi;
            default: return arm;
        }
    }

    // ---- save record ----------------------------------------------------

    private static void putShort(byte[] b, int at, int v) {
        b[at] = (byte) (v >> 8);
        b[at + 1] = (byte) v;
    }

    private static int getShort(byte[] b, int at) {
        return ((b[at] & 0xFF) << 8) | (b[at + 1] & 0xFF);
    }

    private static void putInt(byte[] b, int at, int v) {
        b[at] = (byte) (v >> 24);
        b[at + 1] = (byte) (v >> 16);
        b[at + 2] = (byte) (v >> 8);
        b[at + 3] = (byte) v;
    }

    private static int getInt(byte[] b, int at) {
        return ((b[at] & 0xFF) << 24) | ((b[at + 1] & 0xFF) << 16)
                | ((b[at + 2] & 0xFF) << 8) | (b[at + 3] & 0xFF);
    }

    /** Pack into a fixed 32 byte record. Returns the number of bytes used. */
    public int toBytes(byte[] b, int flags) {
        for (int i = 0; i < SAVE_BYTES; i++) {
            b[i] = 0;
        }
        b[0] = (byte) 'V';
        b[1] = (byte) 'Z';
        b[2] = 1;
        b[3] = (byte) flags;
        b[4] = (byte) curLevel;
        b[5] = (byte) level;
        putInt(b, 6, xp);
        b[10] = (byte) points;
        b[11] = (byte) vit;
        b[12] = (byte) pow;
        b[13] = (byte) agi;
        b[14] = (byte) arm;
        putShort(b, 15, hp);
        putShort(b, 17, armor);
        b[19] = (byte) weapons;
        putShort(b, 20, ammoR);
        putShort(b, 22, ammoP);
        putShort(b, 24, kills > 65535 ? 65535 : kills);
        putInt(b, 26, ticks);
        // bytes 30 and 31 stay free: 30 is spare, 31 is the checksum
        int sum = 0;
        for (int i = 0; i < SAVE_BYTES - 1; i++) {
            sum ^= b[i] & 0xFF;
        }
        b[SAVE_BYTES - 1] = (byte) sum;
        return SAVE_BYTES;
    }

    /** Unpack a save record. Returns the flags byte, or -1 when invalid. */
    public int fromBytes(byte[] b, int len) {
        if (b == null || len < SAVE_BYTES) {
            return -1;
        }
        if (b[0] != 'V' || b[1] != 'Z' || b[2] != 1) {
            return -1;
        }
        int sum = 0;
        for (int i = 0; i < SAVE_BYTES - 1; i++) {
            sum ^= b[i] & 0xFF;
        }
        if ((byte) sum != b[SAVE_BYTES - 1]) {
            return -1;
        }
        int flags = b[3] & 0xFF;
        curLevel = FX.clamp(b[4] & 0xFF, 0, Levels.COUNT - 1);
        level = FX.clamp(b[5] & 0xFF, 1, 99);
        xp = getInt(b, 6);
        if (xp < 0) {
            xp = 0;
        }
        points = FX.clamp(b[10] & 0xFF, 0, 99);
        vit = FX.clamp(b[11] & 0xFF, 0, Balance.MAX_STAT);
        pow = FX.clamp(b[12] & 0xFF, 0, Balance.MAX_STAT);
        agi = FX.clamp(b[13] & 0xFF, 0, Balance.MAX_STAT);
        arm = FX.clamp(b[14] & 0xFF, 0, Balance.MAX_STAT);
        hp = FX.clamp(getShort(b, 15), 0, maxHp());
        armor = FX.clamp(getShort(b, 17), 0, armorCap());
        weapons = (b[19] & 0x07) | 1;
        ammoR = FX.clamp(getShort(b, 20), 0, Balance.W_AMMO_MAX[W_RIFLE]);
        ammoP = FX.clamp(getShort(b, 22), 0, Balance.W_AMMO_MAX[W_PLASMA]);
        kills = getShort(b, 24);
        ticks = getInt(b, 26);
        if (kills < 0) {
            kills = 0;
        }
        if (ticks < 0) {
            ticks = 0;
        }
        keys = 0;
        weapon = W_PISTOL;
        if (hp <= 0) {
            hp = maxHp();
        }
        return flags;
    }
}
