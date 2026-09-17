import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

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
 * Desktop only: renders the game's 3D view straight to PNG files so the
 * graphics can be reviewed without a phone or an emulator. Works because the
 * core classes never touch javax.microedition.
 */
public final class FrameDump {

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : "shots";
        new File(dir).mkdirs();

        Textures.init();
        Sprites.init();
        atlas(dir + "/atlas-textures.png");
        sheet(dir + "/atlas-sprites.png");

        Player p = new Player();
        World w = new World();
        Raycaster rc = new Raycaster();

        for (int i = 0; i < Levels.COUNT; i++) {
            w.start(i, p);
            rc.render(w.lvl, w);
            save(rc, dir + "/level" + (i + 1) + "-spawn.png");

            for (int t = 0; t < 25; t++) {
                w.tick(World.A_FWD, 0);
            }
            rc.render(w.lvl, w);
            save(rc, dir + "/level" + (i + 1) + "-walk.png");
        }

        // A hand built scene: one of every enemy at a known distance, with
        // pickups on the floor, so scaling and sorting are easy to judge.
        w.start(0, p);
        p.weapons = 7;
        p.ammoR = 60;
        p.ammoP = 20;
        showcase(w, rc, dir);

        // low detail mode, same scene
        rc.setDetail(3);
        rc.render(w.lvl, w);
        save(rc, dir + "/showcase-lowdetail.png");
        rc.setDetail(2);

        System.out.println("frames written to " + dir);
    }

    private static void showcase(World w, Raycaster rc, String dir) throws Exception {
        // find a long open corridor to stage the scene in
        Level lvl = w.lvl;
        int bestX = 1;
        int bestY = 1;
        int bestRun = 0;
        for (int y = 1; y < lvl.h - 1; y++) {
            int run = 0;
            for (int x = 1; x < lvl.w - 1; x++) {
                if (!lvl.solid(x, y)) {
                    run++;
                    if (run > bestRun) {
                        bestRun = run;
                        bestX = x - run + 1;
                        bestY = y;
                    }
                } else {
                    run = 0;
                }
            }
        }
        w.px = (bestX << 16) + FX.HALF;
        w.py = (bestY << 16) + FX.HALF;
        w.ang = 0;                          // looking along +x
        for (int i = 0; i < w.nEnts; i++) {
            w.ents[i].active = false;
        }
        System.out.println("showcase corridor: (" + bestX + "," + bestY
                + ") run " + bestRun);
        int n = 0;
        int[] types = { Entity.T_TROOPER, Entity.T_MEDKIT, Entity.T_DRONE,
            Entity.T_KEY_B, Entity.T_HEAVY, Entity.T_NODE };
        for (int i = 0; i < types.length && n < w.ents.length; i++) {
            int cx = bestX + 3 + i * 2;
            if (cx >= bestX + bestRun) {
                continue;
            }
            w.ents[n].spawn(types[i], cx, bestY);
            n++;
        }
        w.nEnts = Math.max(w.nEnts, n);
        rc.render(lvl, w);
        save(rc, dir + "/showcase.png");

        // same scene, weapon firing and the player hurt
        w.flash = 3;
        w.hurt = 4;
        w.p.weapon = Player.W_PLASMA;
        rc.render(lvl, w);
        save(rc, dir + "/showcase-fire.png");
        w.flash = 0;
        w.hurt = 0;
        w.p.weapon = Player.W_PISTOL;

        // each weapon view on its own, to judge the first person art
        for (int i = 0; i < w.nEnts; i++) {
            w.ents[i].active = false;
        }
        for (int wi = 0; wi < 3; wi++) {
            w.p.weapon = wi;
            rc.render(lvl, w);
            save(rc, dir + "/weapon" + (wi + 1) + ".png");
        }
        w.p.weapon = Player.W_PISTOL;

        // a boss portrait at close range
        for (int i = 0; i < w.nEnts; i++) {
            w.ents[i].active = false;
        }
        w.ents[0].spawn(Entity.T_BOSS, Math.min(bestX + 4, bestX + bestRun - 1), bestY);
        w.nEnts = 1;
        rc.render(lvl, w);
        save(rc, dir + "/showcase-boss.png");
    }

    private static void save(Raycaster rc, String path) throws Exception {
        BufferedImage img = new BufferedImage(Raycaster.SCREEN_W, Raycaster.VIEW_H,
                BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Raycaster.SCREEN_W, Raycaster.VIEW_H, rc.out, 0,
                Raycaster.SCREEN_W);
        ImageIO.write(img, "png", new File(path));
    }

    /** All wall textures at every shade level, for eyeballing the art. */
    private static void atlas(String path) throws Exception {
        int s = Textures.SIZE;
        int w = Textures.COUNT * (s + 2);
        int h = Textures.SHADES * (s + 2);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int sh = 0; sh < Textures.SHADES; sh++) {
            for (int t = 0; t < Textures.COUNT; t++) {
                int[] pix = Textures.PIX[sh][t];
                for (int x = 0; x < s; x++) {
                    for (int y = 0; y < s; y++) {
                        img.setRGB(t * (s + 2) + x, sh * (s + 2) + y, pix[x * s + y]);
                    }
                }
            }
        }
        ImageIO.write(img, "png", new File(path));
    }

    /** Every sprite frame on a chequerboard, magnified so the art is legible. */
    private static void sheet(String path) throws Exception {
        int scale = 4;
        int pad = 3;
        int perRow = 7;
        int rows = (Sprites.COUNT + perRow - 1) / perRow;
        int cellW = 0;
        int cellH = 0;
        for (int i = 0; i < Sprites.COUNT; i++) {
            cellW = Math.max(cellW, Sprites.W[i]);
            cellH = Math.max(cellH, Sprites.H[i]);
        }
        cellW = cellW * scale + pad * 2;
        cellH = cellH * scale + pad * 2;
        BufferedImage img = new BufferedImage(cellW * perRow, cellH * rows,
                BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                img.setRGB(x, y, ((x >> 3) + (y >> 3)) % 2 == 0 ? 0x2A2A2A : 0x4A4A4A);
            }
        }
        for (int i = 0; i < Sprites.COUNT; i++) {
            int ox = (i % perRow) * cellW + pad;
            int oy = (i / perRow) * cellH + pad;
            int[] pix = Sprites.PIX[i];
            for (int x = 0; x < Sprites.W[i]; x++) {
                for (int y = 0; y < Sprites.H[i]; y++) {
                    int c = pix[x * Sprites.H[i] + y];
                    if (c == 0) {
                        continue;
                    }
                    for (int sy = 0; sy < scale; sy++) {
                        for (int sx = 0; sx < scale; sx++) {
                            img.setRGB(ox + x * scale + sx, oy + y * scale + sy,
                                    c & 0xFFFFFF);
                        }
                    }
                }
            }
        }
        ImageIO.write(img, "png", new File(path));
    }

    private FrameDump() {
    }
}
