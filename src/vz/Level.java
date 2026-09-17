package vz;

/**
 * One parsed level: the cell grid, the doors and the spawn list.
 *
 * The grid is a fixed 32x32 byte array reused for every level so loading a
 * level allocates nothing. MIDP free.
 */
public final class Level {

    public static final int MAX = 32;
    public static final int SHIFT = 5;

    // cell values
    public static final int FLOOR = 0;
    public static final int W_METAL = 1;
    public static final int W_TECH = 2;
    public static final int W_VENT = 3;
    public static final int W_HULL = 4;
    public static final int DOOR = 9;
    public static final int LOCK_R = 10;
    public static final int LOCK_G = 11;
    public static final int LOCK_B = 12;
    public static final int EXIT = 13;

    // keycard bits, shared with Player.keys
    public static final int KEY_R = 1;
    public static final int KEY_G = 2;
    public static final int KEY_B = 4;

    public static final int MAX_DOORS = 24;

    public final byte[] cells = new byte[MAX * MAX];
    /** Door index per cell, -1 where there is no door. */
    public final byte[] doorAt = new byte[MAX * MAX];
    /** How far each door has slid open, 0..FX.ONE. */
    public final int[] doorOpen = new int[MAX_DOORS];
    public final boolean[] seen = new boolean[MAX * MAX];

    public int nDoors;
    public int w;
    public int h;
    public int index;
    public int objective;
    public int startX;
    public int startY;
    public int startAng;
    public int exitX;
    public int exitY;

    /** Spawn list as (type, cellX, cellY) triples. */
    public final int[] spawns = new int[3 * 96];
    public int nSpawns;

    public void load(int n) {
        index = n;
        objective = Levels.OBJECTIVE[n];
        startAng = Levels.START_ANGLE[n];
        String[] rows = Levels.MAPS[n];
        h = rows.length;
        w = rows[0].length();
        nDoors = 0;
        nSpawns = 0;
        for (int i = 0; i < cells.length; i++) {
            cells[i] = 0;
            doorAt[i] = -1;
            seen[i] = false;
        }
        for (int i = 0; i < MAX_DOORS; i++) {
            doorOpen[i] = 0;
        }
        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < w; x++) {
                int i = (y << SHIFT) + x;
                char c = row.charAt(x);
                int cell = FLOOR;
                int spawn = -1;
                switch (c) {
                    case '#': cell = W_METAL; break;
                    case '%': cell = W_TECH; break;
                    case '=': cell = W_VENT; break;
                    case '&': cell = W_HULL; break;
                    case '.': cell = FLOOR; break;
                    case 'D': cell = DOOR; break;
                    case 'R': cell = LOCK_R; break;
                    case 'G': cell = LOCK_G; break;
                    case 'B': cell = LOCK_B; break;
                    case 'E':
                        cell = EXIT;
                        exitX = x;
                        exitY = y;
                        break;
                    case 'P':
                        startX = x;
                        startY = y;
                        break;
                    case 'd': spawn = Entity.T_DRONE; break;
                    case 't': spawn = Entity.T_TROOPER; break;
                    case 'h': spawn = Entity.T_HEAVY; break;
                    case 'X': spawn = Entity.T_BOSS; break;
                    case 'T': spawn = Entity.T_NODE; break;
                    case 'm': spawn = Entity.T_MEDKIT; break;
                    case 'a': spawn = Entity.T_ARMOR; break;
                    case 'i': spawn = Entity.T_AMMO_R; break;
                    case 'p': spawn = Entity.T_AMMO_P; break;
                    case 'W': spawn = Entity.T_GUN_R; break;
                    case 'Q': spawn = Entity.T_GUN_P; break;
                    case 'r': spawn = Entity.T_KEY_R; break;
                    case 'g': spawn = Entity.T_KEY_G; break;
                    case 'b': spawn = Entity.T_KEY_B; break;
                    default: cell = W_METAL; break;
                }
                cells[i] = (byte) cell;
                if (cell >= DOOR && cell <= LOCK_B && nDoors < MAX_DOORS) {
                    doorAt[i] = (byte) nDoors;
                    nDoors++;
                }
                if (spawn >= 0 && nSpawns * 3 + 2 < spawns.length) {
                    spawns[nSpawns * 3] = spawn;
                    spawns[nSpawns * 3 + 1] = x;
                    spawns[nSpawns * 3 + 2] = y;
                    nSpawns++;
                }
            }
        }
    }

    public int cell(int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= w || cy >= h) {
            return W_METAL;
        }
        return cells[(cy << SHIFT) + cx];
    }

    /** True when the cell blocks movement and sight. */
    public boolean solid(int cx, int cy) {
        int c = cell(cx, cy);
        if (c == FLOOR || c == EXIT) {
            return false;
        }
        if (c >= DOOR && c <= LOCK_B) {
            int d = doorAt[(cy << SHIFT) + cx];
            return d < 0 || doorOpen[d] < FX.ONE;
        }
        return true;
    }

    /** True when a 16.16 position is inside a solid cell. */
    public boolean solidAt(int x, int y) {
        return solid(x >> 16, y >> 16);
    }

    public int keyFor(int cellValue) {
        if (cellValue == LOCK_R) {
            return KEY_R;
        }
        if (cellValue == LOCK_G) {
            return KEY_G;
        }
        if (cellValue == LOCK_B) {
            return KEY_B;
        }
        return 0;
    }

    /**
     * Try to open the door in the given cell.
     * Returns 1 when it starts opening, 0 when there is no door, -1 when the
     * matching keycard is missing.
     */
    public int openDoor(int cx, int cy, int keys) {
        int c = cell(cx, cy);
        if (c < DOOR || c > LOCK_B) {
            return 0;
        }
        int d = doorAt[(cy << SHIFT) + cx];
        // Already open, or already sliding: pressing use again must not
        // restart the animation, or holding the key keeps the door shut.
        if (d < 0 || doorOpen[d] > 0) {
            return 0;
        }
        int need = keyFor(c);
        if (need != 0 && (keys & need) == 0) {
            return -1;
        }
        doorOpen[d] = 1;
        return 1;
    }

    /** Advance door animations. */
    public void tickDoors() {
        int step = FX.ONE / Balance.DOOR_TICKS;
        for (int i = 0; i < nDoors; i++) {
            if (doorOpen[i] > 0 && doorOpen[i] < FX.ONE) {
                doorOpen[i] += step;
                if (doorOpen[i] > FX.ONE) {
                    doorOpen[i] = FX.ONE;
                }
            }
        }
    }

    public int doorOpenAt(int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= w || cy >= h) {
            return 0;
        }
        int d = doorAt[(cy << SHIFT) + cx];
        return d < 0 ? 0 : doorOpen[d];
    }

    public void markSeen(int cx, int cy) {
        if (cx >= 0 && cy >= 0 && cx < w && cy < h) {
            seen[(cy << SHIFT) + cx] = true;
        }
    }

    /**
     * Line of sight between two 16.16 points. Samples every quarter cell,
     * which cannot skip a one cell thick wall.
     */
    public boolean sight(int x0, int y0, int x1, int y1) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int dist = FX.len(dx, dy);
        if (dist <= 0) {
            return true;
        }
        int steps = dist / (FX.ONE / 4);
        if (steps <= 0) {
            return true;
        }
        int sx = dx / steps;
        int sy = dy / steps;
        int x = x0;
        int y = y0;
        for (int i = 0; i < steps; i++) {
            x += sx;
            y += sy;
            if (solidAt(x, y)) {
                return false;
            }
        }
        return true;
    }
}
