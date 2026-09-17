import vz.Balance;
import vz.Entity;
import vz.FX;
import vz.Level;
import vz.Levels;
import vz.Player;
import vz.World;

/**
 * Desktop only: plays the whole campaign with a bot, driving the real world
 * simulation, the real enemy AI and the real collision code. Every level must
 * be finishable; the numbers it prints are the balance report.
 *
 * Navigation is gradient descent on a breadth first distance field measured
 * from the goal, so the bot cannot oscillate between two goals.
 */
public final class PlayBot {

    private static final int MAX_TICKS = Balance.TPS * 60 * 5;
    private static final int GRID = Level.MAX;
    private static final int GOAL_TIMEOUT = Balance.TPS * 45;

    private final World w = new World();
    private final Player p = new Player();
    private final int[] dist = new int[GRID * GRID];
    private final int[] queue = new int[GRID * GRID];

    private boolean verbose;
    private int goalCell = -1;
    private Entity goalEnt;
    private int goalTicks;
    private int fightTicks;
    private Entity locked;
    private int weaponSwitchWait;
    private int shots;
    private int wallBumps;
    private int lastStep = -2;
    private int lastHeld;

    public static void main(String[] args) {
        PlayBot bot = new PlayBot();
        bot.verbose = args.length > 0 && args[0].equals("-v");
        System.exit(bot.run() > 0 ? 1 : 0);
    }

    private int run() {
        int failures = 0;
        int totalTicks = 0;
        int deaths = 0;
        int firstTry = 0;
        System.out.println("campaign playthrough");
        int only = System.getProperty("level") != null
                ? Integer.parseInt(System.getProperty("level")) - 1 : -1;
        for (int n = 0; n < Levels.COUNT; n++) {
            if (only >= 0 && n != only) {
                continue;
            }
            boolean done = false;
            for (int attempt = 1; attempt <= 3 && !done; attempt++) {
                w.start(n, p);
                reset();
                int ticks = 0;
                while (ticks < MAX_TICKS && !w.complete && !w.dead) {
                    stepBot();
                    ticks++;
                    if (verbose && ticks % 150 == 0) {
                        trace(ticks);
                    }
                }
                totalTicks += ticks;
                if (w.complete) {
                    done = true;
                    if (attempt == 1) {
                        firstTry++;
                    }
                    report(n, ticks, attempt);
                    spendPoints();
                    if (n < Levels.COUNT - 1) {
                        p.curLevel = n + 1;
                        p.enterLevel();
                    }
                } else if (w.dead) {
                    deaths++;
                    int near = 0;
                    StringBuilder types = new StringBuilder();
                    for (int i = 0; i < w.nEnts; i++) {
                        Entity e = w.ents[i];
                        if (e.active && e.isEnemy() && e.state != Entity.S_DEAD
                                && FX.len(e.x - w.px, e.y - w.py) < 8 * FX.ONE) {
                            near++;
                            types.append(e.type).append(' ');
                        }
                    }
                    System.out.println("  level " + (n + 1) + ": died after "
                            + (ticks / Balance.TPS) + "s (attempt " + attempt
                            + "), kills " + p.kills + ", weapon " + p.weapon
                            + ", ammoR " + p.ammoR + ", ammoP " + p.ammoP
                            + ", " + near + " enemies within 8 cells (types "
                            + types.toString().trim() + ")");
                    p.hp = p.maxHp();
                } else {
                    System.out.println("  level " + (n + 1) + ": NOT FINISHED in "
                            + (ticks / Balance.TPS) + "s at cell " + (w.px >> 16) + ","
                            + (w.py >> 16) + ", goal " + (goalCell % GRID) + ","
                            + (goalCell / GRID) + ", objective done "
                            + w.objectiveDone() + ", keys " + p.keys);
                }
            }
            if (!done) {
                failures++;
            }
        }
        System.out.println("  total " + (totalTicks / Balance.TPS) + "s of play, "
                + deaths + " deaths, final level " + p.level + ", " + shots + " shots");
        System.out.println("  cleared on the first attempt: " + firstTry + " of "
                + Levels.COUNT);
        if (failures == 0) {
            System.out.println("  every level finished by the bot");
        }
        return failures;
    }

    private void report(int n, int ticks, int attempt) {
        System.out.println("  level " + (n + 1) + " " + Levels.NAME[n] + ": cleared in "
                + (ticks / Balance.TPS) + "s, hp " + p.hp + "/" + p.maxHp()
                + ", armour " + p.armor + ", kills " + p.kills + ", xp level "
                + p.level + ", points " + p.points
                + (attempt > 1 ? " (attempt " + attempt + ")" : ""));
    }

    private void trace(int ticks) {
        System.out.println("    t" + ticks + " cell " + (w.px >> 16) + "," + (w.py >> 16)
                + " goal " + (goalCell % GRID) + "," + (goalCell / GRID)
                + " goalTicks " + goalTicks + " hp " + p.hp + " keys " + p.keys
                + " fight " + fightTicks + " objDone " + w.objectiveDone()
                + " distHere " + dist[cellOf(w.px, w.py)]
                + " step " + lastStep + " held " + lastHeld
                + " bumps " + wallBumps);
    }

    private void reset() {
        goalCell = -1;
        goalEnt = null;
        goalTicks = 0;
        fightTicks = 0;
        locked = null;
        weaponSwitchWait = 0;
    }

    /** Strongest weapon that is owned and has ammo. */
    private int bestWeapon() {
        for (int w = Balance.W_COUNT - 1; w >= 0; w--) {
            if (p.hasWeapon(w) && p.ammo(w) != 0) {
                return w;
            }
        }
        return Player.W_PISTOL;
    }

    private void spendPoints() {
        int guard = 0;
        while (p.points > 0 && guard < 40) {
            guard++;
            if (p.spend(0)) {
                continue;
            }
            if (p.spend(1)) {
                continue;
            }
            if (p.spend(3)) {
                continue;
            }
            if (!p.spend(2)) {
                break;
            }
        }
    }

    // ---- one tick ------------------------------------------------------

    private void stepBot() {
        int held = 0;
        int tapped = 0;
        int cell = cellOf(w.px, w.py);

        // use the strongest loaded weapon, the way a player would
        int best = bestWeapon();
        if (best != p.weapon && weaponSwitchWait <= 0) {
            tapped |= World.A_WNEXT;
            weaponSwitchWait = 3;
        }
        if (weaponSwitchWait > 0) {
            weaponSwitchWait--;
        }

        Entity threat = lockedThreat();
        boolean fighting = false;
        if (threat != null) {
            int want = FX.angleOf(threat.x - w.px, threat.y - w.py);
            int diff = FX.angDiff(w.ang, want);
            int d = FX.len(threat.x - w.px, threat.y - w.py);
            if (d < 7 * FX.ONE) {
                fighting = true;
                fightTicks++;
                if (diff > 40) {
                    held |= World.A_TR;
                } else if (diff < -40) {
                    held |= World.A_TL;
                } else {
                    held |= World.A_FIRE;
                    shots++;
                    if (diff > 10) {
                        held |= World.A_TR;
                    } else if (diff < -10) {
                        held |= World.A_TL;
                    }
                    // hurt and cornered: back away while shooting
                    if (p.hp * 3 < p.maxHp() && d < 3 * FX.ONE) {
                        held |= World.A_BACK;
                    } else if (d < 2 * FX.ONE) {
                        held |= World.A_SR;      // sidestep in melee range
                    }
                }
            } else if (diff > -30 && diff < 30) {
                held |= World.A_FIRE;
            }
        } else if (fightTicks > 0) {
            fightTicks = 0;
        }

        chooseGoal(cell);
        goalTicks++;
        int step = downhill(cell);
        lastStep = step;
        if (!fighting && step >= 0) {
            int tx = ((step % GRID) << 16) + FX.HALF;
            int ty = ((step / GRID) << 16) + FX.HALF;
            int want = FX.angleOf(tx - w.px, ty - w.py);
            int diff = FX.angDiff(w.ang, want);
            // Turn without walking when badly off heading, then walk
            // straight: correcting every tick makes the path a zig zag and
            // wedges the body on corners.
            if (diff > 60) {
                held |= World.A_TR;
            } else if (diff < -60) {
                held |= World.A_TL;
            } else {
                held |= World.A_FWD;
                if (diff > 30) {
                    held |= World.A_TR;
                } else if (diff < -30) {
                    held |= World.A_TL;
                }
            }
            int sx = step % GRID;
            int sy = step / GRID;
            int c = w.lvl.cell(sx, sy);
            if (c >= Level.DOOR && c <= Level.LOCK_B
                    && w.lvl.doorOpenAt(sx, sy) < FX.ONE) {
                tapped |= World.A_USE;
            }
        }

        lastHeld = held;
        int beforeX = w.px;
        int beforeY = w.py;
        w.tick(held, tapped);
        if ((held & World.A_FWD) != 0 && w.px == beforeX && w.py == beforeY) {
            wallBumps++;
        }
    }

    private int cellOf(int x, int y) {
        return ((y >> 16) * GRID) + (x >> 16);
    }

    // ---- threats -------------------------------------------------------

    private Entity lockedThreat() {
        if (locked != null) {
            int d = FX.len(locked.x - w.px, locked.y - w.py);
            boolean worth = locked.active && locked.state != Entity.S_DEAD
                    && d < 9 * FX.ONE && fightTicks < Balance.TPS * 15
                    && w.lvl.sight(w.px, w.py, locked.x, locked.y);
            if (worth) {
                return locked;
            }
            locked = null;
            fightTicks = 0;
        }
        Entity best = null;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < w.nEnts; i++) {
            Entity e = w.ents[i];
            if (!e.active || e.state == Entity.S_DEAD || !e.blocksShots()) {
                continue;
            }
            if (e.type == Entity.T_NODE && w.lvl.objective != Levels.OBJ_NODES) {
                continue;
            }
            int d = FX.len(e.x - w.px, e.y - w.py);
            if (d >= bestD || d > 8 * FX.ONE) {
                continue;
            }
            if (!w.lvl.sight(w.px, w.py, e.x, e.y)) {
                continue;
            }
            bestD = d;
            best = e;
        }
        locked = best;
        return best;
    }

    // ---- goals ---------------------------------------------------------

    private void chooseGoal(int cell) {
        boolean stale = goalCell < 0 || goalTicks > GOAL_TIMEOUT;
        if (goalEnt != null && (!goalEnt.active
                || (goalEnt.blocksShots() && goalEnt.state == Entity.S_DEAD))) {
            stale = true;
        }
        if (goalEnt == null && goalCell == cell) {
            stale = true;
        }
        if (goalEnt != null) {
            goalCell = cellOf(goalEnt.x, goalEnt.y);
        }
        if (!stale) {
            fill(goalCell);
            if (dist[cell] >= 0) {
                return;
            }
        }

        goalTicks = 0;
        goalEnt = null;
        goalCell = -1;

        // 1. the mission objective
        if (!w.objectiveDone()) {
            Entity best = null;
            int bestD = Integer.MAX_VALUE;
            for (int i = 0; i < w.nEnts; i++) {
                Entity e = w.ents[i];
                if (!e.active || e.state == Entity.S_DEAD) {
                    continue;
                }
                boolean wanted = w.lvl.objective == Levels.OBJ_NODES
                        ? e.type == Entity.T_NODE : e.type == Entity.T_BOSS;
                if (!wanted) {
                    continue;
                }
                fill(cellOf(e.x, e.y));
                if (dist[cell] < 0 || dist[cell] >= bestD) {
                    continue;
                }
                bestD = dist[cell];
                best = e;
            }
            if (best != null) {
                goalEnt = best;
                goalCell = cellOf(best.x, best.y);
                fill(goalCell);
                return;
            }
        }

        // 2. a weapon we do not own yet
        for (int t = Entity.T_GUN_R; t <= Entity.T_GUN_P; t++) {
            int owned = t == Entity.T_GUN_R ? Player.W_RIFLE : Player.W_PLASMA;
            if (p.hasWeapon(owned)) {
                continue;
            }
            Entity gun = nearest(cell, t, t);
            if (gun != null) {
                goalEnt = gun;
                goalCell = cellOf(gun.x, gun.y);
                fill(goalCell);
                return;
            }
        }

        // 3. a medkit when badly hurt
        if (p.hp * 5 < p.maxHp() * 2) {
            Entity med = nearest(cell, Entity.T_MEDKIT, Entity.T_MEDKIT);
            if (med != null) {
                goalEnt = med;
                goalCell = cellOf(med.x, med.y);
                fill(goalCell);
                return;
            }
        }

        // 4. the exit, if the keycards allow it
        int exit = (w.lvl.exitY * GRID) + w.lvl.exitX;
        fill(exit);
        if (dist[cell] >= 0) {
            goalCell = exit;
            return;
        }

        // 5. otherwise the nearest keycard
        Entity key = nearest(cell, Entity.T_KEY_R, Entity.T_KEY_B);
        if (key != null) {
            goalEnt = key;
            goalCell = cellOf(key.x, key.y);
            fill(goalCell);
            return;
        }

        // 6. nothing obvious: walk to the farthest cell we can still reach
        fill(cell);
        int far = cell;
        int farD = 0;
        for (int i = 0; i < dist.length; i++) {
            if (dist[i] > farD) {
                farD = dist[i];
                far = i;
            }
        }
        goalCell = far;
        fill(goalCell);
    }

    private Entity nearest(int cell, int typeLo, int typeHi) {
        Entity best = null;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < w.nEnts; i++) {
            Entity e = w.ents[i];
            if (!e.active || e.type < typeLo || e.type > typeHi) {
                continue;
            }
            fill(cellOf(e.x, e.y));
            if (dist[cell] < 0 || dist[cell] >= bestD) {
                continue;
            }
            bestD = dist[cell];
            best = e;
        }
        return best;
    }

    /** Neighbour cell that is closer to the goal, or -1. */
    private int downhill(int cell) {
        if (goalCell < 0) {
            return -1;
        }
        if (dist[cell] < 0) {
            fill(goalCell);
            if (dist[cell] < 0) {
                return -1;
            }
        }
        if (dist[cell] == 0) {
            return cell;         // standing on the goal, walk to its centre
        }
        int cx = cell % GRID;
        int cy = cell / GRID;
        int best = -1;
        int bestD = dist[cell];
        for (int d = 0; d < 4; d++) {
            int nx = cx + (d == 0 ? 1 : d == 1 ? -1 : 0);
            int ny = cy + (d == 2 ? 1 : d == 3 ? -1 : 0);
            if (nx < 0 || ny < 0 || nx >= w.lvl.w || ny >= w.lvl.h) {
                continue;
            }
            int id = ny * GRID + nx;
            if (dist[id] >= 0 && dist[id] < bestD) {
                bestD = dist[id];
                best = id;
            }
        }
        return best;
    }

    /** Breadth first distance to every cell from the given one. */
    private void fill(int goal) {
        for (int i = 0; i < dist.length; i++) {
            dist[i] = -1;
        }
        if (goal < 0) {
            return;
        }
        int head = 0;
        int tail = 0;
        dist[goal] = 0;
        queue[tail++] = goal;
        while (head < tail) {
            int cur = queue[head++];
            int cx = cur % GRID;
            int cy = cur / GRID;
            for (int d = 0; d < 4; d++) {
                int nx = cx + (d == 0 ? 1 : d == 1 ? -1 : 0);
                int ny = cy + (d == 2 ? 1 : d == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= w.lvl.w || ny >= w.lvl.h) {
                    continue;
                }
                int id = ny * GRID + nx;
                if (dist[id] >= 0) {
                    continue;
                }
                int c = w.lvl.cell(nx, ny);
                if (c >= Level.W_METAL && c <= Level.W_HULL) {
                    continue;
                }
                if (c >= Level.LOCK_R && c <= Level.LOCK_B
                        && (p.keys & w.lvl.keyFor(c)) == 0) {
                    continue;
                }
                dist[id] = dist[cur] + 1;
                queue[tail++] = id;
            }
        }
    }

    private PlayBot() {
    }
}
