package vz;

/**
 * Software raycaster: walls by DDA, billboard sprites with a per column depth
 * buffer, and the first person weapon on top.
 *
 * The view is rendered as square blocks of "block" screen pixels (2 for high
 * detail, 3 for low) written straight into one screen sized int buffer, so
 * every output pixel is touched exactly once and there is no separate upscale
 * pass. Nothing is allocated while rendering. MIDP free.
 */
public final class Raycaster {

    public static final int SCREEN_W = 240;
    public static final int VIEW_H = 174;
    public static final int MAX_RAYS = SCREEN_W / 2;

    /** Finished view, SCREEN_W * VIEW_H pixels of 0xRRGGBB. */
    public final int[] out = new int[SCREEN_W * VIEW_H];

    private final int[] zbuf = new int[MAX_RAYS];
    private final int[] camTab = new int[MAX_RAYS];
    private final int[] ceilRow = new int[VIEW_H];
    private final int[] floorRow = new int[VIEW_H];
    private final int[] order = new int[World.MAX_ENTS];
    private final int[] depths = new int[World.MAX_ENTS];

    private int block = 2;
    private int rays = SCREEN_W / 2;
    private int rows = VIEW_H / 2;

    /** Height in cells of each sprite frame, 16.16. */
    private static final int[] SPRITE_H = new int[Sprites.COUNT];
    private static boolean tablesReady;

    public Raycaster() {
        Textures.init();
        Sprites.init();
        initTables();
        setDetail(2);
    }

    private static synchronized void initTables() {
        if (tablesReady) {
            return;
        }
        for (int i = 0; i < Sprites.COUNT; i++) {
            SPRITE_H[i] = FX.ONE;
        }
        SPRITE_H[Sprites.DRONE] = (FX.ONE * 3) / 4;
        SPRITE_H[Sprites.TROOPER] = (FX.ONE * 9) / 10;
        SPRITE_H[Sprites.HEAVY] = FX.ONE;
        SPRITE_H[Sprites.BOSS] = (FX.ONE * 3) / 2;
        SPRITE_H[Sprites.NODE] = (FX.ONE * 5) / 4;
        SPRITE_H[Sprites.DEAD_SMALL] = FX.ONE / 4;
        SPRITE_H[Sprites.DEAD_BIG] = FX.ONE / 3;
        SPRITE_H[Sprites.MEDKIT] = (FX.ONE * 2) / 5;
        SPRITE_H[Sprites.ARMOR] = (FX.ONE * 2) / 5;
        SPRITE_H[Sprites.AMMO_R] = (FX.ONE * 2) / 5;
        SPRITE_H[Sprites.AMMO_P] = (FX.ONE * 2) / 5;
        SPRITE_H[Sprites.GUN_R] = FX.ONE / 2;
        SPRITE_H[Sprites.GUN_P] = FX.ONE / 2;
        SPRITE_H[Sprites.KEY_R] = FX.ONE / 3;
        SPRITE_H[Sprites.KEY_G] = FX.ONE / 3;
        SPRITE_H[Sprites.KEY_B] = FX.ONE / 3;
        tablesReady = true;
    }

    public int detail() {
        return block;
    }

    public int rays() {
        return rays;
    }

    public void setDetail(int b) {
        if (b < 2) {
            b = 2;
        } else if (b > 3) {
            b = 3;
        }
        block = b;
        rays = SCREEN_W / b;
        rows = VIEW_H / b;
        for (int c = 0; c < rays; c++) {
            camTab[c] = (((c << 1) - rays) << 16) / rays;
        }
        int horizon = rows >> 1;
        for (int y = 0; y < rows; y++) {
            // distance bands: the floor gets lighter towards the camera, the
            // ceiling darker towards the horizon
            int d = y < horizon ? horizon - y : y - horizon;
            int t = (d << 8) / (horizon + 1);
            ceilRow[y] = Textures.scale(0x28324A, 60 + ((t * 120) >> 8));
            floorRow[y] = Textures.scale(0x574C40, 55 + ((t * 200) >> 8));
        }
    }

    /** Fill a block of screen pixels with one colour. */
    private void blk(int col, int row, int rgb) {
        int b = block;
        int x0 = col * b;
        int y0 = row * b;
        int i = y0 * SCREEN_W + x0;
        for (int y = 0; y < b; y++) {
            for (int x = 0; x < b; x++) {
                out[i + x] = rgb;
            }
            i += SCREEN_W;
        }
    }

    public void render(Level lvl, World w) {
        int px = w.px;
        int py = w.py;
        int dirX = FX.cos(w.ang);
        int dirY = FX.sin(w.ang);
        int planeX = -FX.mul(dirY, FX.TAN30);
        int planeY = FX.mul(dirX, FX.TAN30);
        byte[] cells = lvl.cells;

        for (int c = 0; c < rays; c++) {
            int cam = camTab[c];
            int rdx = dirX + FX.mul(planeX, cam);
            int rdy = dirY + FX.mul(planeY, cam);
            int ax = rdx < 0 ? -rdx : rdx;
            int ay = rdy < 0 ? -rdy : rdy;
            int ddx = ax < 32 ? 0x3FFFFFF : (int) ((1L << 32) / (long) ax);
            int ddy = ay < 32 ? 0x3FFFFFF : (int) ((1L << 32) / (long) ay);

            int mapX = px >> 16;
            int mapY = py >> 16;
            int stepX;
            int stepY;
            int sdx;
            int sdy;
            if (rdx < 0) {
                stepX = -1;
                sdx = (int) (((long) (px & 0xFFFF) * (long) ddx) >> 16);
            } else {
                stepX = 1;
                sdx = (int) (((long) (FX.ONE - (px & 0xFFFF)) * (long) ddx) >> 16);
            }
            if (rdy < 0) {
                stepY = -1;
                sdy = (int) (((long) (py & 0xFFFF) * (long) ddy) >> 16);
            } else {
                stepY = 1;
                sdy = (int) (((long) (FX.ONE - (py & 0xFFFF)) * (long) ddy) >> 16);
            }

            int side = 0;
            int cell = 0;
            int guard = 0;
            while (guard < 96) {
                guard++;
                if (sdx < sdy) {
                    sdx += ddx;
                    mapX += stepX;
                    side = 0;
                } else {
                    sdy += ddy;
                    mapY += stepY;
                    side = 1;
                }
                if (mapX < 0 || mapY < 0 || mapX >= lvl.w || mapY >= lvl.h) {
                    cell = Level.W_METAL;
                    break;
                }
                cell = cells[(mapY << Level.SHIFT) + mapX];
                if (cell == Level.FLOOR) {
                    continue;
                }
                if (cell >= Level.DOOR && cell <= Level.LOCK_B) {
                    if (lvl.doorOpenAt(mapX, mapY) >= FX.ONE) {
                        continue;       // fully open doors are walked through
                    }
                }
                if (cell == Level.EXIT) {
                    continue;           // the exit is a floor tile
                }
                break;
            }

            int perp = side == 0 ? sdx - ddx : sdy - ddy;
            if (perp < 512) {
                perp = 512;
            }
            zbuf[c] = perp;

            int lineH = (int) (((long) rows << 16) / (long) perp);
            int wallX;
            if (side == 0) {
                wallX = py + (int) (((long) perp * (long) rdy) >> 16);
            } else {
                wallX = px + (int) (((long) perp * (long) rdx) >> 16);
            }
            int tx = (wallX & 0xFFFF) >> 11;
            if ((side == 0 && rdx > 0) || (side == 1 && rdy < 0)) {
                tx = Textures.MASK - tx;
            }
            if (cell >= Level.DOOR && cell <= Level.LOCK_B) {
                // slide the door texture sideways while it opens
                tx = (tx + (lvl.doorOpenAt(mapX, mapY) >> 11)) & Textures.MASK;
            }
            int shade = perp < (2 * FX.ONE) ? 0 : perp < (4 * FX.ONE) ? 1
                    : perp < (8 * FX.ONE) ? 2 : 3;
            if (side == 1 && shade < Textures.SHADES - 1) {
                shade++;
            }
            int[] tex = Textures.PIX[shade][Textures.slotFor(cell)];
            int texBase = tx * Textures.SIZE;

            int drawStart = (rows - lineH) >> 1;
            int drawEnd = drawStart + lineH;
            int step = (Textures.SIZE << 16) / (lineH < 1 ? 1 : lineH);
            int tpos = 0;
            if (drawStart < 0) {
                tpos = -drawStart * step;
                drawStart = 0;
            }
            if (drawEnd > rows) {
                drawEnd = rows;
            }
            int y = 0;
            for (; y < drawStart; y++) {
                blk(c, y, ceilRow[y]);
            }
            for (; y < drawEnd; y++) {
                int t = tpos >> 16;
                if (t > Textures.MASK) {
                    t = Textures.MASK;
                }
                blk(c, y, tex[texBase + t]);
                tpos += step;
            }
            for (; y < rows; y++) {
                blk(c, y, floorRow[y]);
            }
        }

        drawSprites(w, dirX, dirY, planeX, planeY);
        drawWeapon(w);
        if (w.hurt > 0) {
            tintEdges(0xC01010, w.hurt);
        }
    }

    private void drawSprites(World w, int dirX, int dirY, int planeX, int planeY) {
        int n = 0;
        int det = FX.mul(planeX, dirY) - FX.mul(dirX, planeY);
        if (det == 0) {
            return;
        }
        int invDet = FX.div(FX.ONE, det);
        for (int i = 0; i < w.nEnts; i++) {
            Entity e = w.ents[i];
            if (!e.active) {
                continue;
            }
            int sx = e.x - w.px;
            int sy = e.y - w.py;
            int tx = FX.mul(invDet, FX.mul(dirY, sx) - FX.mul(dirX, sy));
            int ty = FX.mul(invDet, FX.mul(planeX, sy) - FX.mul(planeY, sx));
            e.depth = ty;
            if (ty < FX.ONE / 6) {
                continue;
            }
            e.screenX = (rays >> 1) + ((FX.div(tx, ty) * (rays >> 1)) >> 16);
            depths[n] = ty;
            order[n] = i;
            n++;
        }
        // insertion sort, far to near
        for (int i = 1; i < n; i++) {
            int d = depths[i];
            int idx = order[i];
            int j = i - 1;
            while (j >= 0 && depths[j] < d) {
                depths[j + 1] = depths[j];
                order[j + 1] = order[j];
                j--;
            }
            depths[j + 1] = d;
            order[j + 1] = idx;
        }
        for (int k = 0; k < n; k++) {
            drawSprite(w.ents[order[k]], w);
        }
    }

    private void drawSprite(Entity e, World w) {
        int frame;
        if (e.state == Entity.S_DEAD) {
            frame = e.type == Entity.T_BOSS || e.type == Entity.T_HEAVY
                    ? Sprites.DEAD_BIG : Sprites.DEAD_SMALL;
        } else {
            frame = Sprites.frameFor(e.type);
        }
        int[] pix = Sprites.PIX[frame];
        int artW = Sprites.W[frame];
        int artH = Sprites.H[frame];
        int ty = e.depth;
        int lineH = (int) (((long) rows << 16) / (long) ty);
        int spriteH = FX.mul(lineH, SPRITE_H[frame]);
        if (spriteH < 2) {
            return;
        }
        int spriteW = (spriteH * artW) / artH;
        if (spriteW < 1) {
            return;
        }
        int bottom = (rows >> 1) + (lineH >> 1);
        // pickups and corpses lie on the floor, walkers stand on it
        int top = bottom - spriteH;
        if (e.isPickup()) {
            top += spriteH / 8;
            bottom += spriteH / 8;
        }
        if (e.state == Entity.S_IDLE && e.isEnemy()) {
            // gentle idle bob
            int bob = (FX.sin((w.tick * 24 + (e.x >> 12)) & FX.ANG_MASK) * (spriteH / 32)) >> 16;
            top += bob;
            bottom += bob;
        }
        int left = e.screenX - (spriteW >> 1);
        int stepX = (artW << 16) / spriteW;
        int stepY = (artH << 16) / spriteH;

        int shade = ty < (4 * FX.ONE) ? 0 : ty < (8 * FX.ONE) ? 1 : 2;
        boolean pain = e.state == Entity.S_PAIN;

        int y0 = top < 0 ? 0 : top;
        int y1 = bottom > rows ? rows : bottom;
        for (int col = left < 0 ? 0 : left; col < left + spriteW; col++) {
            if (col >= rays) {
                break;
            }
            if (ty >= zbuf[col]) {
                continue;
            }
            int ax = ((col - left) * stepX) >> 16;
            if (ax < 0) {
                ax = 0;
            } else if (ax >= artW) {
                ax = artW - 1;
            }
            int colBase = ax * artH;
            for (int row = y0; row < y1; row++) {
                int ay = ((row - top) * stepY) >> 16;
                if (ay < 0 || ay >= artH) {
                    continue;
                }
                int c = pix[colBase + ay];
                if (c == 0) {
                    continue;
                }
                c &= 0xFFFFFF;
                if (pain) {
                    c = ((c >> 1) & 0x7F7F7F) + 0x808080;
                } else if (shade == 1) {
                    c = Textures.scale(c, 180);
                } else if (shade == 2) {
                    c = Textures.scale(c, 120);
                }
                blk(col, row, c);
            }
        }
    }

    /** Blit a sprite frame in screen pixels, used for the weapon and flash. */
    private void blitScreen(int frame, int dx, int dy, int scale) {
        int[] pix = Sprites.PIX[frame];
        int artW = Sprites.W[frame];
        int artH = Sprites.H[frame];
        for (int x = 0; x < artW; x++) {
            int sx = dx + x * scale;
            if (sx + scale <= 0 || sx >= SCREEN_W) {
                continue;
            }
            int colBase = x * artH;
            for (int y = 0; y < artH; y++) {
                int c = pix[colBase + y];
                if (c == 0) {
                    continue;
                }
                c &= 0xFFFFFF;
                int sy = dy + y * scale;
                for (int yy = 0; yy < scale; yy++) {
                    int py = sy + yy;
                    if (py < 0 || py >= VIEW_H) {
                        continue;
                    }
                    int i = py * SCREEN_W + sx;
                    for (int xx = 0; xx < scale; xx++) {
                        int pxx = sx + xx;
                        if (pxx >= 0 && pxx < SCREEN_W) {
                            out[i + xx] = c;
                        }
                    }
                }
            }
        }
    }

    private void drawWeapon(World w) {
        int frame = Sprites.viewFor(w.p.weapon);
        int artW = Sprites.W[frame];
        int scale = 3;
        int bob = (FX.sin(w.bob) * 3) >> 16;
        int kick = w.flash > 0 ? 4 : 0;
        int dx = (SCREEN_W - artW * scale) / 2 + 14 + ((FX.cos(w.bob) * 2) >> 16);
        int dy = VIEW_H - Sprites.H[frame] * scale + 10 + bob + kick;
        if (w.flash > 0) {
            int fw = Sprites.W[Sprites.FLASH] * 4;
            blitScreen(Sprites.FLASH, dx + artW * scale / 2 - fw / 2, dy - 22, 4);
        }
        blitScreen(frame, dx, dy, scale);
        drawCrosshair(w.aimLocked);
    }

    private void drawCrosshair(boolean locked) {
        int cx = SCREEN_W >> 1;
        int cy = (rows >> 1) * block;
        int c = locked ? 0xFF4040 : 0xB0D8E0;
        for (int i = 3; i <= 7; i++) {
            px(cx + i, cy, c);
            px(cx - i, cy, c);
            px(cx, cy + i, c);
            px(cx, cy - i, c);
        }
        px(cx, cy, c);
    }

    private void px(int x, int y, int c) {
        if (x >= 0 && x < SCREEN_W && y >= 0 && y < VIEW_H) {
            out[y * SCREEN_W + x] = c;
        }
    }

    /** Cheap damage feedback: tint the edges instead of the whole view. */
    private void tintEdges(int rgb, int strength) {
        int t = FX.clamp(strength * 40, 0, 200);
        int bandY = 14;
        int bandX = 10;
        for (int y = 0; y < VIEW_H; y++) {
            boolean edgeRow = y < bandY || y >= VIEW_H - bandY;
            int i = y * SCREEN_W;
            if (edgeRow) {
                for (int x = 0; x < SCREEN_W; x++) {
                    out[i + x] = blend(out[i + x], rgb, t);
                }
            } else {
                for (int x = 0; x < bandX; x++) {
                    out[i + x] = blend(out[i + x], rgb, t);
                    out[i + SCREEN_W - 1 - x] = blend(out[i + SCREEN_W - 1 - x], rgb, t);
                }
            }
        }
    }

    private static int blend(int a, int b, int t) {
        int ia = 256 - t;
        int r = ((((a >> 16) & 0xFF) * ia) + (((b >> 16) & 0xFF) * t)) >> 8;
        int g = ((((a >> 8) & 0xFF) * ia) + (((b >> 8) & 0xFF) * t)) >> 8;
        int bl = (((a & 0xFF) * ia) + ((b & 0xFF) * t)) >> 8;
        return (r << 16) | (g << 8) | bl;
    }
}
