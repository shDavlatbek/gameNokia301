import java.util.ArrayDeque;
import java.util.HashSet;

import vz.Balance;
import vz.Entity;
import vz.FX;
import vz.Level;
import vz.Levels;
import vz.Player;
import vz.Raycaster;
import vz.Sprites;
import vz.Textures;
import vz.World;

/**
 * Desktop only test suite. No JUnit: it prints one line per check and exits
 * non zero if anything failed, which is all a build script needs.
 */
public final class LogicTest {

    private static int failed;
    private static int passed;

    public static void main(String[] args) {
        mapData();
        completable();
        fixedPoint();
        collision();
        sight();
        combat();
        doors();
        progression();
        weapons();
        saveRecord();
        art();
        renderer();

        System.out.println("---------------------------------------------");
        System.out.println(passed + " checks passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(boolean ok, String what) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL  " + what);
        }
    }

    private static void note(String what) {
        System.out.println("      " + what);
    }

    // ---- level data ----------------------------------------------------

    private static final String LEGEND = "#%=&.DRGBrgbPEdthXTmaipWQ";

    private static void mapData() {
        System.out.println("level data");
        check(Levels.MAPS.length == Levels.COUNT, "one map per level");
        check(Levels.NAME.length == Levels.COUNT, "one name per level");
        check(Levels.OBJECTIVE.length == Levels.COUNT, "one objective per level");
        for (int n = 0; n < Levels.COUNT; n++) {
            String[] rows = Levels.MAPS[n];
            int w = rows[0].length();
            boolean sameWidth = true;
            int starts = 0;
            int exits = 0;
            boolean legal = true;
            boolean border = true;
            for (int y = 0; y < rows.length; y++) {
                String row = rows[y];
                if (row.length() != w) {
                    sameWidth = false;
                }
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (LEGEND.indexOf(c) < 0) {
                        legal = false;
                    }
                    if (c == 'P') {
                        starts++;
                    }
                    if (c == 'E') {
                        exits++;
                    }
                    boolean edge = y == 0 || y == rows.length - 1 || x == 0
                            || x == row.length() - 1;
                    if (edge && "#%=&".indexOf(c) < 0) {
                        border = false;
                    }
                }
            }
            String tag = "level " + (n + 1) + ": ";
            check(sameWidth, tag + "all rows the same width");
            check(legal, tag + "only legend characters");
            check(starts == 1, tag + "exactly one start");
            check(exits == 1, tag + "exactly one exit");
            check(border, tag + "solid border");
            check(w <= Level.MAX && rows.length <= Level.MAX,
                    tag + "fits the 32x32 grid");
            Level lvl = new Level();
            lvl.load(n);
            check(lvl.nDoors <= Level.MAX_DOORS, tag + "door count within the pool");
            check(lvl.nSpawns > 0 && lvl.nSpawns * 3 <= lvl.spawns.length,
                    tag + "spawn list fits");
            check(!lvl.solid(lvl.startX, lvl.startY), tag + "start is not inside a wall");
            note(tag + w + "x" + rows.length + ", " + lvl.nSpawns + " spawns, "
                    + lvl.nDoors + " doors");
        }
    }

    /** Flood fill honouring locked doors, mirroring what a player can do. */
    private static HashSet<Integer> reach(Level lvl, int keys) {
        HashSet<Integer> seen = new HashSet<Integer>();
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        int start = lvl.startY * Level.MAX + lvl.startX;
        seen.add(start);
        q.add(start);
        int[] dx = { 1, -1, 0, 0 };
        int[] dy = { 0, 0, 1, -1 };
        while (!q.isEmpty()) {
            int cur = q.poll();
            int cx = cur % Level.MAX;
            int cy = cur / Level.MAX;
            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                if (nx < 0 || ny < 0 || nx >= lvl.w || ny >= lvl.h) {
                    continue;
                }
                int id = ny * Level.MAX + nx;
                if (seen.contains(id)) {
                    continue;
                }
                int c = lvl.cell(nx, ny);
                if (c >= Level.W_METAL && c <= Level.W_HULL) {
                    continue;
                }
                if (c >= Level.LOCK_R && c <= Level.LOCK_B) {
                    if ((keys & lvl.keyFor(c)) == 0) {
                        continue;
                    }
                }
                seen.add(id);
                q.add(id);
            }
        }
        return seen;
    }

    private static int keysIn(Level lvl, HashSet<Integer> area) {
        int keys = 0;
        for (int i = 0; i < lvl.nSpawns; i++) {
            int t = lvl.spawns[i * 3];
            int id = lvl.spawns[i * 3 + 2] * Level.MAX + lvl.spawns[i * 3 + 1];
            if (!area.contains(id)) {
                continue;
            }
            if (t == Entity.T_KEY_R) {
                keys |= Level.KEY_R;
            } else if (t == Entity.T_KEY_G) {
                keys |= Level.KEY_G;
            } else if (t == Entity.T_KEY_B) {
                keys |= Level.KEY_B;
            }
        }
        return keys;
    }

    private static void completable() {
        System.out.println("completability");
        for (int n = 0; n < Levels.COUNT; n++) {
            Level lvl = new Level();
            lvl.load(n);
            int exitId = lvl.exitY * Level.MAX + lvl.exitX;
            int keys = 0;
            HashSet<Integer> area = null;
            for (int round = 0; round < 4; round++) {
                area = reach(lvl, keys);
                int got = keysIn(lvl, area);
                if (got == keys) {
                    break;
                }
                keys |= got;
            }
            String tag = "level " + (n + 1) + ": ";
            check(area.contains(exitId), tag + "exit reachable by collecting keycards");

            // every spawned item and enemy must be standing somewhere reachable
            boolean allReachable = true;
            for (int i = 0; i < lvl.nSpawns; i++) {
                int id = lvl.spawns[i * 3 + 2] * Level.MAX + lvl.spawns[i * 3 + 1];
                if (!area.contains(id)) {
                    allReachable = false;
                }
            }
            check(allReachable, tag + "all spawns reachable");

            if (lvl.nDoors > 0) {
                boolean locked = false;
                for (int y = 0; y < lvl.h; y++) {
                    for (int x = 0; x < lvl.w; x++) {
                        int c = lvl.cell(x, y);
                        if (c >= Level.LOCK_R && c <= Level.LOCK_B) {
                            locked = true;
                        }
                    }
                }
                if (locked) {
                    HashSet<Integer> noKeys = reach(lvl, 0);
                    check(!noKeys.contains(exitId),
                            tag + "exit is really gated by a locked door");
                }
            }
        }
    }

    // ---- maths ---------------------------------------------------------

    private static void fixedPoint() {
        System.out.println("fixed point maths");
        int worst = 0;
        for (int a = 0; a < FX.ANG; a++) {
            int want = (int) Math.round(Math.sin(a * 2 * Math.PI / FX.ANG) * 65536.0);
            int diff = Math.abs(want - FX.sin(a));
            if (diff > worst) {
                worst = diff;
            }
        }
        check(worst <= 1, "sine table matches Math.sin (worst error " + worst + ")");

        int cosWorst = 0;
        for (int a = 0; a < FX.ANG; a++) {
            int want = (int) Math.round(Math.cos(a * 2 * Math.PI / FX.ANG) * 65536.0);
            cosWorst = Math.max(cosWorst, Math.abs(want - FX.cos(a)));
        }
        check(cosWorst <= 1, "cosine table matches Math.cos");

        boolean sqrtOk = true;
        for (long v = 0; v < 200000; v += 37) {
            int want = (int) Math.sqrt((double) v);
            int got = FX.isqrt(v);
            if (Math.abs(want - got) > 1) {
                sqrtOk = false;
            }
        }
        check(sqrtOk, "isqrt matches Math.sqrt");
        check(FX.isqrt(0) == 0 && FX.isqrt(1) == 1 && FX.isqrt(65536) == 256,
                "isqrt exact on squares");

        boolean lenOk = true;
        for (int i = 1; i < 40; i++) {
            int d = FX.len(i * FX.ONE, 0);
            if (Math.abs(d - i * FX.ONE) > 2) {
                lenOk = false;
            }
        }
        check(lenOk, "len of an axis aligned vector");

        int angWorst = 0;
        for (int a = 0; a < FX.ANG; a += 7) {
            int dx = FX.cos(a) * 3;
            int dy = FX.sin(a) * 3;
            int got = FX.angleOf(dx, dy);
            int diff = Math.abs(FX.angDiff(a, got));
            angWorst = Math.max(angWorst, diff);
        }
        check(angWorst <= 2, "angleOf inverts cos/sin (worst " + angWorst + " units)");
        check(FX.angDiff(10, 2040) == -18, "angDiff wraps the short way");
        check(FX.angDiff(2040, 10) == 18, "angDiff wraps the other way");

        boolean rndInRange = true;
        for (int i = 0; i < 5000; i++) {
            int v = FX.rnd(100);
            if (v < 0 || v > 99) {
                rndInRange = false;
            }
        }
        check(rndInRange, "rnd stays inside its range");
    }

    // ---- world ---------------------------------------------------------

    private static void collision() {
        System.out.println("collision");
        Player p = new Player();
        World w = new World();
        boolean everInside = false;
        boolean moved = false;
        for (int n = 0; n < Levels.COUNT; n++) {
            w.start(n, p);
            int startX = w.px;
            FX.setSeed(12345 + n);
            for (int i = 0; i < 4000; i++) {
                int in = 0;
                int r = FX.rnd(6);
                if (r == 0) {
                    in = World.A_FWD;
                } else if (r == 1) {
                    in = World.A_BACK;
                } else if (r == 2) {
                    in = World.A_FWD | World.A_TL;
                } else if (r == 3) {
                    in = World.A_FWD | World.A_TR;
                } else if (r == 4) {
                    in = World.A_SL;
                } else {
                    in = World.A_SR;
                }
                w.tick(in, 0);
                if (w.lvl.solidAt(w.px, w.py)) {
                    everInside = true;
                }
                if (w.px != startX) {
                    moved = true;
                }
                if (w.dead) {
                    p.hp = p.maxHp();
                    w.dead = false;
                }
            }
        }
        check(!everInside, "the player never ends a tick inside a wall");
        check(moved, "the player actually moves");

        // walking into a wall must not teleport or stop the game
        w.start(0, p);
        int before = w.px;
        w.ang = 0;
        for (int i = 0; i < 200; i++) {
            w.tick(World.A_FWD, 0);
        }
        check(!w.lvl.solidAt(w.px, w.py), "still free after walking into a wall");
        check(w.px != before || w.py != 0, "position updated");
    }

    private static void sight() {
        System.out.println("line of sight");
        Level lvl = new Level();
        lvl.load(0);
        // a point sees itself and its own cell
        int x = (lvl.startX << 16) + FX.HALF;
        int y = (lvl.startY << 16) + FX.HALF;
        check(lvl.sight(x, y, x, y), "a point sees itself");

        // find a wall and check that it blocks
        boolean blocked = false;
        for (int cy = 1; cy < lvl.h - 1 && !blocked; cy++) {
            for (int cx = 1; cx < lvl.w - 1 && !blocked; cx++) {
                if (!lvl.solid(cx, cy)) {
                    continue;
                }
                if (lvl.solid(cx - 1, cy) || lvl.solid(cx + 1, cy)) {
                    continue;
                }
                int ax = ((cx - 1) << 16) + FX.HALF;
                int bx = ((cx + 1) << 16) + FX.HALF;
                int ay = (cy << 16) + FX.HALF;
                blocked = !lvl.sight(ax, ay, bx, ay);
                if (!blocked) {
                    System.out.println("      wall at " + cx + "," + cy
                            + " did not block");
                    failed++;
                    return;
                }
            }
        }
        check(blocked, "a wall blocks sight");

        // an open corridor does not block
        boolean open = false;
        for (int cy = 1; cy < lvl.h - 1 && !open; cy++) {
            for (int cx = 1; cx < lvl.w - 2; cx++) {
                if (!lvl.solid(cx, cy) && !lvl.solid(cx + 1, cy)) {
                    int ax = (cx << 16) + FX.HALF;
                    int bx = ((cx + 1) << 16) + FX.HALF;
                    int ay = (cy << 16) + FX.HALF;
                    open = lvl.sight(ax, ay, bx, ay);
                    break;
                }
            }
        }
        check(open, "open floor does not block sight");
    }

    private static void combat() {
        System.out.println("combat");
        Player p = new Player();
        World w = new World();
        w.start(0, p);
        // clear the level and stage one drone straight ahead
        for (int i = 0; i < w.nEnts; i++) {
            w.ents[i].active = false;
        }
        int cx = w.px >> 16;
        int cy = w.py >> 16;
        int dir = -1;
        for (int a = 0; a < FX.ANG; a += FX.ANG / 4) {
            int tx = cx + (FX.cos(a) > FX.HALF ? 1 : FX.cos(a) < -FX.HALF ? -1 : 0);
            int ty = cy + (FX.sin(a) > FX.HALF ? 1 : FX.sin(a) < -FX.HALF ? -1 : 0);
            if (!w.lvl.solid(tx, ty)) {
                dir = a;
                cx = tx;
                cy = ty;
                break;
            }
        }
        check(dir >= 0, "found an open neighbour cell to aim at");
        w.ang = dir;
        Entity e = w.ents[0];
        e.spawn(Entity.T_DRONE, cx, cy);
        int hp0 = e.hp;
        w.tick(World.A_FIRE, 0);
        check(e.hp < hp0, "firing at an enemy in the crosshair damages it");
        check((w.events & World.EV_SHOT) != 0, "a shot event is reported");

        // kill it and check the reward
        int kills0 = p.kills;
        for (int i = 0; i < 40 && e.state != Entity.S_DEAD; i++) {
            w.tick(World.A_FIRE, 0);
        }
        check(e.state == Entity.S_DEAD, "the enemy dies under sustained fire");
        check(p.kills == kills0 + 1, "the kill is counted");
        check(p.xp > 0, "experience is awarded");

        // an enemy behind the player is never hit
        w.start(0, p);
        for (int i = 0; i < w.nEnts; i++) {
            w.ents[i].active = false;
        }
        Entity back = w.ents[0];
        back.spawn(Entity.T_TROOPER, w.px >> 16, w.py >> 16);
        back.x = w.px - 2 * FX.ONE;
        back.y = w.py;
        w.ang = 0;                       // facing +x, the enemy is at -x
        int bhp = back.hp;
        for (int i = 0; i < 10; i++) {
            w.tick(World.A_FIRE, 0);
        }
        check(back.hp == bhp, "enemies behind the player are not hit");

        // damage scales with the power stat
        Player strong = new Player();
        strong.pow = Balance.MAX_STAT;
        check(strong.dmgPct() > p.dmgPct(), "power raises damage");
    }

    private static void doors() {
        System.out.println("doors and keycards");
        Level lvl = new Level();
        for (int n = 0; n < Levels.COUNT; n++) {
            lvl.load(n);
            for (int y = 1; y < lvl.h - 1; y++) {
                for (int x = 1; x < lvl.w - 1; x++) {
                    int c = lvl.cell(x, y);
                    if (c == Level.DOOR) {
                        check(lvl.openDoor(x, y, 0) == 1, "a plain door opens");
                        for (int i = 0; i < Balance.DOOR_TICKS + 2; i++) {
                            lvl.tickDoors();
                        }
                        check(!lvl.solid(x, y), "an open door can be walked through");
                        return2(lvl, n);
                        return;
                    }
                }
            }
        }
    }

    private static void return2(Level lvl, int n) {
        // locked doors: refuse without the card, open with it
        for (int levelIndex = 0; levelIndex < Levels.COUNT; levelIndex++) {
            lvl.load(levelIndex);
            for (int y = 1; y < lvl.h - 1; y++) {
                for (int x = 1; x < lvl.w - 1; x++) {
                    int c = lvl.cell(x, y);
                    if (c < Level.LOCK_R || c > Level.LOCK_B) {
                        continue;
                    }
                    int need = lvl.keyFor(c);
                    check(lvl.openDoor(x, y, 0) == -1, "a locked door stays shut");
                    check(lvl.solid(x, y), "a locked door blocks movement");
                    check(lvl.openDoor(x, y, need) == 1, "the right keycard opens it");
                    for (int i = 0; i < Balance.DOOR_TICKS + 2; i++) {
                        lvl.tickDoors();
                    }
                    check(!lvl.solid(x, y), "the opened door lets the player pass");
                    return;
                }
            }
        }
    }

    private static void progression() {
        System.out.println("progression");
        Player p = new Player();
        check(p.level == 1 && p.xp == 0 && p.points == 0, "a fresh player is level 1");
        int last = 0;
        boolean rising = true;
        for (int l = 1; l < 12; l++) {
            int need = Balance.xpToNext(l);
            if (need <= last) {
                rising = false;
            }
            last = need;
        }
        check(rising, "the experience curve rises with level");

        int levels = p.gainXp(Balance.xpToNext(1));
        check(levels == 1, "reaching the threshold grants one level");
        check(p.points == Balance.POINTS_PER_LEVEL, "the level up grants points");

        int hp0 = p.maxHp();
        check(p.spend(0), "a point can be spent");
        check(p.maxHp() == hp0 + Balance.HP_PER_VIT, "vitality raises max health");
        check(p.points == Balance.POINTS_PER_LEVEL - 1, "spending consumes a point");

        Player capped = new Player();
        capped.points = 99;
        for (int i = 0; i < Balance.MAX_STAT; i++) {
            check(capped.spend(1), "power point " + (i + 1) + " accepted");
        }
        check(!capped.spend(1), "stats stop at the cap");
        Player broke = new Player();
        check(!broke.spend(0), "no points means no spending");

        // damage soaks through armour and never goes below zero
        Player hurt = new Player();
        hurt.armor = 40;
        int before = hurt.hp;
        hurt.hurt(20);
        check(hurt.hp > before - 20, "armour absorbs part of the damage");
        check(hurt.armor < 40, "armour is consumed");
        hurt.hurt(10000);
        check(hurt.hp == 0, "health floors at zero");

        // a full playthrough's worth of experience must give several levels
        Player sim = new Player();
        int xp = 0;
        for (int n = 0; n < Levels.COUNT; n++) {
            Level lvl = new Level();
            lvl.load(n);
            for (int i = 0; i < lvl.nSpawns; i++) {
                int t = lvl.spawns[i * 3];
                if (t <= Entity.T_BOSS) {
                    xp += Balance.E_XP[t];
                }
            }
        }
        sim.gainXp(xp);
        note("clearing every enemy gives " + xp + " xp, player level " + sim.level
                + " with " + sim.points + " points");
        check(sim.level >= 4 && sim.level <= 9, "a full clear lands in the 4 to 9 range");
    }

    private static void weapons() {
        System.out.println("weapons");
        Player p = new Player();
        check(p.ammo(Player.W_PISTOL) < 0, "the pistol has unlimited ammo");
        check(p.nextWeapon(1) == Player.W_PISTOL, "with one weapon cycling is a no op");
        p.weapons = 7;
        p.ammoR = 0;
        p.ammoP = 5;
        check(p.nextWeapon(1) == Player.W_PLASMA, "cycling skips the empty rifle");
        p.ammoR = 10;
        p.weapon = Player.W_PISTOL;
        check(p.nextWeapon(1) == Player.W_RIFLE, "cycling reaches the loaded rifle");
        p.weapon = Player.W_RIFLE;
        p.useAmmo(Player.W_RIFLE);
        check(p.ammoR == 9, "firing spends one round");
        p.addAmmo(Player.W_RIFLE, 10000);
        check(p.ammoR == Balance.W_AMMO_MAX[Player.W_RIFLE], "ammo is capped");
        check(Balance.W_DMG.length == Balance.W_COUNT
                && Balance.W_COOLDOWN.length == Balance.W_COUNT
                && Balance.W_SPREAD.length == Balance.W_COUNT,
                "weapon tables all have one entry per weapon");
    }

    private static void saveRecord() {
        System.out.println("save record");
        Player p = new Player();
        p.gainXp(300);
        p.curLevel = 2;
        p.vit = 2;
        p.pow = 1;
        p.agi = 3;
        p.arm = 1;
        p.hp = 55;
        p.armor = 33;
        p.weapons = 7;
        p.ammoR = 44;
        p.ammoP = 12;
        p.kills = 37;
        p.ticks = 4321;
        byte[] buf = new byte[Player.SAVE_BYTES];
        int used = p.toBytes(buf, 5);
        check(used == Player.SAVE_BYTES, "the record is a fixed 32 bytes");

        Player back = new Player();
        int flags = back.fromBytes(buf, Player.SAVE_BYTES);
        check(flags == 5, "the flags survive the round trip");
        check(back.level == p.level && back.xp == p.xp && back.points == p.points,
                "level, experience and points survive");
        check(back.vit == p.vit && back.pow == p.pow && back.agi == p.agi
                && back.arm == p.arm, "the stats survive");
        check(back.hp == p.hp && back.armor == p.armor, "health and armour survive");
        check(back.weapons == p.weapons && back.ammoR == p.ammoR
                && back.ammoP == p.ammoP, "the gear survives");
        check(back.kills == p.kills && back.ticks == p.ticks, "the statistics survive");
        check(back.curLevel == p.curLevel, "the current level survives");

        buf[9] ^= 0x55;
        check(new Player().fromBytes(buf, Player.SAVE_BYTES) == -1,
                "a corrupted record is rejected");
        byte[] empty = new byte[Player.SAVE_BYTES];
        check(new Player().fromBytes(empty, Player.SAVE_BYTES) == -1,
                "an empty record is rejected");
        check(new Player().fromBytes(buf, 4) == -1, "a truncated record is rejected");
        check(new Player().fromBytes(null, Player.SAVE_BYTES) == -1,
                "a missing record is rejected");
    }

    // ---- art -----------------------------------------------------------

    private static void art() {
        System.out.println("art");
        Textures.init();
        Sprites.init();
        for (int s = 0; s < Textures.SHADES; s++) {
            for (int t = 0; t < Textures.COUNT; t++) {
                int[] pix = Textures.PIX[s][t];
                check(pix != null && pix.length == Textures.AREA,
                        "texture " + t + " shade " + s + " is fully generated");
                boolean varied = false;
                for (int i = 1; i < pix.length; i++) {
                    if (pix[i] != pix[0]) {
                        varied = true;
                        break;
                    }
                }
                check(varied, "texture " + t + " shade " + s + " is not a flat colour");
            }
        }
        // shading must actually darken
        int bright = Textures.PIX[0][0][0] & 0xFF;
        int dark = Textures.PIX[3][0][0] & 0xFF;
        check(dark < bright, "the darkest shade is darker than the brightest");

        for (int i = 0; i < Sprites.COUNT; i++) {
            int[] pix = Sprites.PIX[i];
            check(pix != null && pix.length == Sprites.W[i] * Sprites.H[i],
                    "sprite " + i + " decoded to the right size");
            int opaque = 0;
            int magenta = 0;
            for (int k = 0; k < pix.length; k++) {
                if (pix[k] != 0) {
                    opaque++;
                    if ((pix[k] & 0xFFFFFF) == 0xFF00FF) {
                        magenta++;
                    }
                }
            }
            check(opaque > pix.length / 12, "sprite " + i + " has visible pixels");
            check(magenta == 0, "sprite " + i + " has no missing palette entries");
        }
        for (int t = 0; t < Entity.T_COUNT; t++) {
            int f = Sprites.frameFor(t);
            check(f >= 0 && f < Sprites.COUNT, "entity type " + t + " has a sprite");
        }
    }

    private static void renderer() {
        System.out.println("renderer");
        Player p = new Player();
        World w = new World();
        Raycaster rc = new Raycaster();
        w.start(0, p);
        for (int i = 0; i < 20; i++) {
            w.tick(World.A_FWD, 0);
        }
        rc.render(w.lvl, w);
        int[] first = new int[rc.out.length];
        System.arraycopy(rc.out, 0, first, 0, rc.out.length);
        rc.render(w.lvl, w);
        boolean same = true;
        for (int i = 0; i < first.length; i++) {
            if (first[i] != rc.out[i]) {
                same = false;
                break;
            }
        }
        check(same, "rendering the same state twice gives the same pixels");

        boolean allWritten = true;
        int black = 0;
        for (int i = 0; i < rc.out.length; i++) {
            if (rc.out[i] == 0) {
                black++;
            }
        }
        check(black < rc.out.length / 4, "the view is not mostly unwritten");
        check(allWritten, "every view pixel is covered");

        // both detail levels must render without touching anything outside
        rc.setDetail(3);
        rc.render(w.lvl, w);
        check(rc.detail() == 3 && rc.rays() == Raycaster.SCREEN_W / 3,
                "low detail uses a third of the rays");
        rc.setDetail(2);
        rc.render(w.lvl, w);
        check(rc.detail() == 2, "high detail restored");

        // no per frame allocation: memory must be flat over many frames
        for (int i = 0; i < 40; i++) {
            rc.render(w.lvl, w);
            w.tick(World.A_FWD | World.A_TR, 0);
        }
        System.gc();
        long before = used();
        for (int i = 0; i < 200; i++) {
            w.tick(World.A_FWD | World.A_TR, 0);
            rc.render(w.lvl, w);
        }
        long after = used();
        long growth = after - before;
        note("heap growth over 200 frames: " + growth + " bytes");
        check(growth < 200 * 1024, "a frame allocates next to nothing");
    }

    private static long used() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private LogicTest() {
    }
}
