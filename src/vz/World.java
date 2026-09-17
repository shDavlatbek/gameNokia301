package vz;

/**
 * The simulation: player movement, doors, pickups, combat, enemy AI and the
 * mission objective. Knows nothing about MIDP, so the desktop harness can run
 * and test it. All entities come from a pool, so a tick allocates nothing.
 */
public final class World {

    public static final int MAX_ENTS = 48;

    // input action bits
    public static final int A_FWD = 1;
    public static final int A_BACK = 2;
    public static final int A_TL = 4;
    public static final int A_TR = 8;
    public static final int A_SL = 16;
    public static final int A_SR = 32;
    public static final int A_FIRE = 64;
    public static final int A_USE = 128;
    public static final int A_WPREV = 256;
    public static final int A_WNEXT = 512;
    public static final int A_MAP = 1024;
    public static final int A_MENU = 2048;

    // events reported to the shell, which turns them into sound
    public static final int EV_SHOT = 1;
    public static final int EV_HIT = 2;
    public static final int EV_KILL = 4;
    public static final int EV_PICKUP = 8;
    public static final int EV_DOOR = 16;
    public static final int EV_DENY = 32;
    public static final int EV_HURT = 64;
    public static final int EV_LEVELUP = 128;
    public static final int EV_DIE = 256;
    public static final int EV_COMPLETE = 512;
    public static final int EV_NOAMMO = 1024;

    public final Level lvl = new Level();
    public Player p;
    public final Entity[] ents = new Entity[MAX_ENTS];
    public int nEnts;

    public int px;
    public int py;
    public int ang;
    public int tick;
    public int events;
    public int flash;
    public int hurt;
    public int bob;
    public int nodesLeft;
    public boolean bossAlive;
    public boolean dead;
    public boolean complete;
    public boolean showMap;
    public boolean aimLocked;
    public String msg;
    public int msgTimer;

    private int cooldown;
    private int turnRamp;
    private int bossSpawnTimer;

    public World() {
        for (int i = 0; i < MAX_ENTS; i++) {
            ents[i] = new Entity();
        }
    }

    public void start(int levelIndex, Player player) {
        p = player;
        p.curLevel = levelIndex;
        p.enterLevel();
        lvl.load(levelIndex);
        px = (lvl.startX << 16) + FX.HALF;
        py = (lvl.startY << 16) + FX.HALF;
        ang = lvl.startAng;
        tick = 0;
        events = 0;
        flash = 0;
        hurt = 0;
        bob = 0;
        cooldown = 0;
        turnRamp = 0;
        dead = false;
        complete = false;
        showMap = false;
        aimLocked = false;
        msg = null;
        msgTimer = 0;
        nodesLeft = 0;
        bossAlive = false;
        bossSpawnTimer = 0;
        nEnts = 0;
        for (int i = 0; i < MAX_ENTS; i++) {
            ents[i].active = false;
        }
        for (int i = 0; i < lvl.nSpawns && nEnts < MAX_ENTS; i++) {
            int type = lvl.spawns[i * 3];
            ents[nEnts].spawn(type, lvl.spawns[i * 3 + 1], lvl.spawns[i * 3 + 2]);
            if (type == Entity.T_NODE) {
                nodesLeft++;
            } else if (type == Entity.T_BOSS) {
                bossAlive = true;
            }
            nEnts++;
        }
        FX.setSeed(0x1234567 + levelIndex * 7717);
        markSeen();
    }

    public boolean objectiveDone() {
        if (lvl.objective == Levels.OBJ_NODES) {
            return nodesLeft <= 0;
        }
        if (lvl.objective == Levels.OBJ_BOSS) {
            return !bossAlive;
        }
        return true;
    }

    public void say(String s) {
        msg = s;
        msgTimer = Balance.TPS * 2;
    }

    // ---- main tick -----------------------------------------------------

    public void tick(int held, int tapped) {
        events = 0;
        tick++;
        p.ticks++;
        if (flash > 0) {
            flash--;
        }
        if (hurt > 0) {
            hurt--;
        }
        if (msgTimer > 0) {
            msgTimer--;
            if (msgTimer == 0) {
                msg = null;
            }
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if ((tapped & A_MAP) != 0) {
            showMap = !showMap;
        }

        movePlayer(held);
        lvl.tickDoors();
        if ((tapped & A_USE) != 0) {
            use();
        }
        if ((tapped & A_WNEXT) != 0) {
            switchWeapon(1);
        }
        if ((tapped & A_WPREV) != 0) {
            switchWeapon(-1);
        }
        if ((held & A_FIRE) != 0 && cooldown == 0) {
            fire();
        }
        aimLocked = target(Balance.W_SPREAD[p.weapon]) >= 0;
        enemies();
        pickups();
        exitCheck();
        markSeen();
        if (p.hp <= 0 && !dead) {
            dead = true;
            events |= EV_DIE;
        }
    }

    private void movePlayer(int held) {
        int turn = 0;
        if ((held & A_TL) != 0) {
            turn -= 1;
        }
        if ((held & A_TR) != 0) {
            turn += 1;
        }
        if (turn == 0) {
            turnRamp = 0;
        } else {
            if (turnRamp < 256) {
                turnRamp += 96;
            }
            int rate = (Balance.TURN_RATE * (128 + (turnRamp >> 1))) >> 8;
            ang = (ang + turn * rate) & FX.ANG_MASK;
        }

        int speed = (Balance.MOVE_SPEED * p.speedPct()) / 100;
        int strafe = (Balance.STRAFE_SPEED * p.speedPct()) / 100;
        int mx = 0;
        int my = 0;
        int dirX = FX.cos(ang);
        int dirY = FX.sin(ang);
        if ((held & A_FWD) != 0) {
            mx += FX.mul(dirX, speed);
            my += FX.mul(dirY, speed);
        }
        if ((held & A_BACK) != 0) {
            mx -= FX.mul(dirX, speed * 3 / 4);
            my -= FX.mul(dirY, speed * 3 / 4);
        }
        if ((held & A_SL) != 0) {
            mx += FX.mul(dirY, strafe);
            my -= FX.mul(dirX, strafe);
        }
        if ((held & A_SR) != 0) {
            mx -= FX.mul(dirY, strafe);
            my += FX.mul(dirX, strafe);
        }
        if (mx != 0 || my != 0) {
            bob = (bob + 150) & FX.ANG_MASK;
            // one axis at a time, which is what gives wall sliding
            px = slideX(px, py, mx, Balance.RADIUS);
            py = slideY(px, py, my, Balance.RADIUS);
        }
    }

    /**
     * Move along x with a square body, returning the new x. A blocked move
     * simply does not happen, which is what gives wall sliding when the two
     * axes are stepped separately.
     */
    private int slideX(int x, int y, int dx, int r) {
        if (dx == 0) {
            return x;
        }
        int nx = x + dx;
        int edge = dx > 0 ? nx + r : nx - r;
        if (lvl.solidAt(edge, y - r) || lvl.solidAt(edge, y + r)
                || lvl.solidAt(edge, y)) {
            return x;
        }
        return nx;
    }

    /** Move along y with a square body, returning the new y. */
    private int slideY(int x, int y, int dy, int r) {
        if (dy == 0) {
            return y;
        }
        int ny = y + dy;
        int edge = dy > 0 ? ny + r : ny - r;
        if (lvl.solidAt(x - r, edge) || lvl.solidAt(x + r, edge)
                || lvl.solidAt(x, edge)) {
            return y;
        }
        return ny;
    }

    private void use() {
        int dirX = FX.cos(ang);
        int dirY = FX.sin(ang);
        for (int d = 1; d <= 2; d++) {
            int tx = px + FX.mul(dirX, (FX.ONE * d) / 2);
            int ty = py + FX.mul(dirY, (FX.ONE * d) / 2);
            int cx = tx >> 16;
            int cy = ty >> 16;
            int r = lvl.openDoor(cx, cy, p.keys);
            if (r == 1) {
                events |= EV_DOOR;
                return;
            }
            if (r == -1) {
                int c = lvl.cell(cx, cy);
                say(c == Level.LOCK_R ? Text.M_LOCKED_R
                        : c == Level.LOCK_G ? Text.M_LOCKED_G : Text.M_LOCKED_B);
                events |= EV_DENY;
                return;
            }
        }
    }

    private void switchWeapon(int dir) {
        int w = p.nextWeapon(dir);
        if (w != p.weapon) {
            p.weapon = w;
            cooldown = 2;
        }
    }

    private void fire() {
        int w = p.weapon;
        int ammo = p.ammo(w);
        if (ammo == 0) {
            say(Text.M_NO_AMMO);
            events |= EV_NOAMMO;
            cooldown = 6;
            int alt = p.nextWeapon(1);
            if (alt != w) {
                p.weapon = alt;
            }
            return;
        }
        p.useAmmo(w);
        cooldown = Balance.W_COOLDOWN[w];
        flash = 3;
        events |= EV_SHOT;
        // noise wakes nearby enemies
        for (int i = 0; i < nEnts; i++) {
            Entity e = ents[i];
            if (e.active && e.isEnemy() && e.state == Entity.S_IDLE
                    && dist(e) < Balance.WAKE_RANGE) {
                e.state = Entity.S_CHASE;
            }
        }
        int hitIdx = target(Balance.W_SPREAD[w]);
        if (hitIdx < 0) {
            return;
        }
        Entity e = ents[hitIdx];
        int dmg = (Balance.W_DMG[w] * p.dmgPct()) / 100;
        dmg += FX.rnd(dmg / 4 + 1);
        damage(e, dmg);
    }

    /** Index of the entity under the crosshair, or -1. */
    private int target(int slack) {
        int best = -1;
        int bestDist = Balance.W_RANGE;
        for (int i = 0; i < nEnts; i++) {
            Entity e = ents[i];
            if (!e.active || !e.blocksShots() || e.state == Entity.S_DEAD) {
                continue;
            }
            int dx = e.x - px;
            int dy = e.y - py;
            int d = FX.len(dx, dy);
            if (d <= 0 || d >= bestDist) {
                continue;
            }
            int diff = FX.angDiff(ang, FX.angleOf(dx, dy));
            if (diff < 0) {
                diff = -diff;
            }
            // the hit window is the silhouette of the target plus a little slack
            int half = (FX.div(Balance.ENEMY_RADIUS, d) * 326) >> 16;
            if (diff > half + slack) {
                continue;
            }
            if (!lvl.sight(px, py, e.x, e.y)) {
                continue;
            }
            best = i;
            bestDist = d;
        }
        return best;
    }

    private void damage(Entity e, int dmg) {
        e.hp -= dmg;
        events |= EV_HIT;
        if (e.hp > 0) {
            if (e.type == Entity.T_DRONE || e.type == Entity.T_TROOPER
                    || FX.rnd(100) < 30) {
                e.state = Entity.S_PAIN;
                e.timer = 3;
            } else if (e.state == Entity.S_IDLE) {
                e.state = Entity.S_CHASE;
            }
            return;
        }
        e.hp = 0;
        e.state = Entity.S_DEAD;
        e.timer = 0;
        events |= EV_KILL;
        if (e.type == Entity.T_NODE) {
            nodesLeft--;
            e.active = false;
            say(nodesLeft > 0 ? Text.M_NODE_DOWN : Text.M_EXIT_OPEN);
            return;
        }
        p.kills++;
        int gained = p.gainXp(Balance.E_XP[e.type]);
        if (gained > 0) {
            events |= EV_LEVELUP;
            say(Text.M_LEVEL_UP);
        }
        if (e.type == Entity.T_BOSS) {
            bossAlive = false;
            say(Text.M_EXIT_OPEN);
        }
    }

    private int dist(Entity e) {
        return FX.len(e.x - px, e.y - py);
    }

    // ---- enemies -------------------------------------------------------

    private void enemies() {
        for (int i = 0; i < nEnts; i++) {
            Entity e = ents[i];
            if (!e.active || !e.isEnemy() || e.state == Entity.S_DEAD) {
                continue;
            }
            if (e.cooldown > 0) {
                e.cooldown--;
            }
            if (e.state == Entity.S_PAIN) {
                e.timer--;
                if (e.timer <= 0) {
                    e.state = Entity.S_CHASE;
                }
                continue;
            }
            int d = dist(e);
            boolean sees = (tick + i) % 5 == 0
                    ? lvl.sight(px, py, e.x, e.y)
                    : e.state != Entity.S_IDLE;
            if (e.state == Entity.S_IDLE) {
                if (d < Balance.WAKE_RANGE && sees) {
                    e.state = Entity.S_CHASE;
                }
                continue;
            }
            if (e.type == Entity.T_BOSS) {
                bossTick(e, d);
            }
            boolean melee = e.type == Entity.T_DRONE;
            int reach = melee ? Balance.MELEE_RANGE : Balance.SHOOT_RANGE;
            if (sees && d < reach && e.cooldown == 0) {
                attack(e, d, melee);
                continue;
            }
            // close in, but ranged units keep some distance
            int want = melee ? 0 : (3 * FX.ONE) / 2;
            if (d > want) {
                step(e, e.x < px ? 1 : e.x > px ? -1 : 0, 1);
            } else if (d < want / 2) {
                step(e, 0, -1);
            }
        }
    }

    private void bossTick(Entity e, int d) {
        bossSpawnTimer++;
        if (bossSpawnTimer < Balance.TPS * 8) {
            return;
        }
        bossSpawnTimer = 0;
        int alive = 0;
        for (int i = 0; i < nEnts; i++) {
            Entity o = ents[i];
            if (o.active && o.type == Entity.T_DRONE && o.state != Entity.S_DEAD) {
                alive++;
            }
        }
        if (alive >= 3) {
            return;
        }
        for (int i = 0; i < nEnts; i++) {
            Entity o = ents[i];
            if (!o.active || o.state != Entity.S_DEAD) {
                continue;
            }
            o.spawn(Entity.T_DRONE, e.x >> 16, e.y >> 16);
            o.state = Entity.S_CHASE;
            return;
        }
        if (nEnts < MAX_ENTS) {
            ents[nEnts].spawn(Entity.T_DRONE, e.x >> 16, e.y >> 16);
            ents[nEnts].state = Entity.S_CHASE;
            nEnts++;
        }
    }

    private void attack(Entity e, int d, boolean melee) {
        e.state = Entity.S_ATTACK;
        e.cooldown = Balance.E_COOLDOWN[e.type];
        e.timer = 3;
        int dmg = Balance.E_DMG[e.type];
        boolean hits = true;
        if (!melee) {
            int cells = d >> 16;
            int chance = FX.clamp(85 - 6 * cells, 30, 85);
            hits = FX.rnd(100) < chance;
        }
        if (hits) {
            if (p.hurt(dmg)) {
                events |= EV_DIE;
            }
            hurt = 4;
            events |= EV_HURT;
        }
    }

    /**
     * Move an enemy towards (sign 1) or away from (sign -1) the player, with
     * wall sliding and a sidestep when it gets stuck.
     */
    private void step(Entity e, int unusedHint, int sign) {
        int dx = px - e.x;
        int dy = py - e.y;
        int d = FX.len(dx, dy);
        if (d <= 0) {
            return;
        }
        int sp = Balance.E_SPEED[e.type] * sign;
        int vx = (int) (((long) dx * (long) sp) / (long) d);
        int vy = (int) (((long) dy * (long) sp) / (long) d);
        if (e.stuck > 0) {
            e.stuck--;
            int tmp = vx;
            vx = -vy;
            vy = tmp;
        }
        int oldX = e.x;
        int oldY = e.y;
        e.x = slideX(e.x, e.y, vx, Balance.ENEMY_RADIUS);
        e.y = slideY(e.x, e.y, vy, Balance.ENEMY_RADIUS);
        if (e.x == oldX && e.y == oldY) {
            // try to open the door in the way, otherwise sidestep for a while
            int cx = (e.x + vx * 2) >> 16;
            int cy = (e.y + vy * 2) >> 16;
            if (lvl.openDoor(cx, cy, 0) == 1) {
                events |= EV_DOOR;
            } else if (e.stuck == 0) {
                e.stuck = 8;
            }
        }
    }

    // ---- pickups and exit ----------------------------------------------

    private void pickups() {
        for (int i = 0; i < nEnts; i++) {
            Entity e = ents[i];
            if (!e.active || !e.isPickup()) {
                continue;
            }
            if (dist(e) > (FX.ONE * 2) / 5) {
                continue;
            }
            if (!take(e)) {
                continue;
            }
            e.active = false;
            events |= EV_PICKUP;
        }
    }

    private boolean take(Entity e) {
        switch (e.type) {
            case Entity.T_MEDKIT:
                if (p.hp >= p.maxHp()) {
                    return false;
                }
                p.heal(Balance.MEDKIT);
                say(Text.M_MEDKIT);
                return true;
            case Entity.T_ARMOR:
                if (p.armor >= p.armorCap()) {
                    return false;
                }
                p.addArmor(Balance.ARMOR_PACK);
                say(Text.M_ARMOR);
                return true;
            case Entity.T_AMMO_R:
                if (p.ammoR >= Balance.W_AMMO_MAX[Player.W_RIFLE]) {
                    return false;
                }
                p.addAmmo(Player.W_RIFLE, Balance.W_AMMO_PICK[Player.W_RIFLE]);
                say(Text.M_AMMO_R);
                return true;
            case Entity.T_AMMO_P:
                if (p.ammoP >= Balance.W_AMMO_MAX[Player.W_PLASMA]) {
                    return false;
                }
                p.addAmmo(Player.W_PLASMA, Balance.W_AMMO_PICK[Player.W_PLASMA]);
                say(Text.M_AMMO_P);
                return true;
            case Entity.T_GUN_R:
                p.weapons |= 1 << Player.W_RIFLE;
                p.addAmmo(Player.W_RIFLE, Balance.W_AMMO_PICK[Player.W_RIFLE]);
                p.weapon = Player.W_RIFLE;
                say(Text.M_GUN_R);
                return true;
            case Entity.T_GUN_P:
                p.weapons |= 1 << Player.W_PLASMA;
                p.addAmmo(Player.W_PLASMA, Balance.W_AMMO_PICK[Player.W_PLASMA]);
                p.weapon = Player.W_PLASMA;
                say(Text.M_GUN_P);
                return true;
            case Entity.T_KEY_R:
                p.keys |= Level.KEY_R;
                say(Text.M_KEY_R);
                return true;
            case Entity.T_KEY_G:
                p.keys |= Level.KEY_G;
                say(Text.M_KEY_G);
                return true;
            default:
                p.keys |= Level.KEY_B;
                say(Text.M_KEY_B);
                return true;
        }
    }

    private void exitCheck() {
        if (complete) {
            return;
        }
        if (lvl.cell(px >> 16, py >> 16) != Level.EXIT) {
            return;
        }
        if (!objectiveDone()) {
            if (msgTimer == 0) {
                say(lvl.objective == Levels.OBJ_NODES
                        ? Text.M_NODES_LEFT : Text.M_GUARD_ALIVE);
            }
            return;
        }
        complete = true;
        events |= EV_COMPLETE;
    }

    /** Reveal the cells around the player for the map screen. */
    private void markSeen() {
        int cx = px >> 16;
        int cy = py >> 16;
        for (int y = cy - 3; y <= cy + 3; y++) {
            for (int x = cx - 3; x <= cx + 3; x++) {
                if (x == cx && y == cy) {
                    lvl.markSeen(x, y);
                } else if (lvl.sight(px, py, (x << 16) + FX.HALF, (y << 16) + FX.HALF)) {
                    lvl.markSeen(x, y);
                } else if (!lvl.solid(x, y)) {
                    continue;
                } else {
                    lvl.markSeen(x, y);
                }
            }
        }
    }
}
