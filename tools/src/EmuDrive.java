import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * Desktop only: drives the MIDlet running in MicroEmulator under Xvfb.
 *
 * MicroEmulator always shows its launcher first when it is given a .jad, so
 * the script starts with Enter. Screenshots are written after each step, which
 * is the only way to check the HUD and the menu screens without a phone.
 *
 * The keys matter more than they look. MicroEmulator's resizable device maps
 * the host's numeric keypad to the direction pad, not to the phone's digits:
 * NUMPAD2 arrives as "down", NUMPAD5 as the centre key, and NUMPAD0 and
 * NUMPAD9 arrive as nothing at all. An earlier version of this script pressed
 * the numeric keypad throughout and looked like it worked, because walking
 * backwards changes the view just as much as walking forwards does, while
 * strafing, switching weapons and the use key were never really tested. The
 * phone's digits are the host's top row, and phase two below measures which
 * host keys reach the game rather than assuming.
 *
 * Every step now says what it expected to happen and checks the screen
 * actually moved, so a key that quietly stops arriving fails the build instead
 * of producing a screenshot nobody looks at twice.
 */
public final class EmuDrive {

    /** Height of the MicroEmulator menu bar above the device screen. */
    private static final int MENU_BAR = 20;
    /** Rows of the 240x320 screen that hold the 3D view. */
    private static final int VIEW_ROWS = 174;
    /** How much of a region has to move before a key counts as answered. */
    private static final double RESPONDED = 0.02;

    private static Robot robot;
    private static String dir;
    private static int shot;
    private static int failures;

    public static void main(String[] args) throws Exception {
        dir = args.length > 0 ? args[0] : "shots";
        long wait = args.length > 1 ? Long.parseLong(args[1]) : 6000;
        int phase = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        new File(dir).mkdirs();
        robot = new Robot();
        robot.setAutoDelay(40);
        Thread.sleep(wait);

        // no window manager under Xvfb, so click the canvas to give it focus
        robot.mouseMove(120, 200);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(300);

        if (phase == 2) {
            probeKeys();
        } else {
            play();
        }
        System.out.println("wrote " + shot + " screenshots to " + dir);
        if (failures > 0) {
            System.out.println("!!! " + failures + " of the device's controls "
                    + "did not do anything");
            System.exit(1);
        }
    }

    // ---- phase one: play the first level --------------------------------

    private static void play() throws Exception {
        grab("emu-launcher");

        tap(KeyEvent.VK_ENTER);          // start the MIDlet from the launcher
        Thread.sleep(2500);
        grab("emu-splash");

        tap(KeyEvent.VK_ENTER);          // splash to main menu
        Thread.sleep(800);
        grab("emu-menu");

        tap(KeyEvent.VK_ENTER);          // new campaign, briefing
        Thread.sleep(800);
        grab("emu-brief");

        tap(KeyEvent.VK_ENTER);          // into the level
        Thread.sleep(1500);
        grab("emu-play");

        BufferedImage was = snap();
        hold(KeyEvent.VK_2, 900);        // walk forward
        Thread.sleep(300);
        report("2 walks forward", moved(was, 0, VIEW_ROWS));
        grab("emu-walk");

        was = snap();
        hold(KeyEvent.VK_6, 500);        // turn right
        hold(KeyEvent.VK_2, 700);
        Thread.sleep(300);
        report("6 turns", moved(was, 0, VIEW_ROWS));
        grab("emu-walk2");

        was = snap();
        hold(KeyEvent.VK_1, 600);        // strafe left
        Thread.sleep(300);
        report("1 strafes", moved(was, 0, VIEW_ROWS));
        was = snap();
        hold(KeyEvent.VK_3, 600);        // and back right again
        Thread.sleep(300);
        report("3 strafes the other way", moved(was, 0, VIEW_ROWS));

        report("5 fires", flashed(KeyEvent.VK_5, 1200));
        grab("emu-fire");

        // 7 and 9 change weapon, but the campaign starts with the pistol
        // alone and the rifle is a pickup, so there is nothing to change to
        // yet and nothing to assert. The probe in phase two shows the keys
        // themselves arrive.
        tap(KeyEvent.VK_9);
        Thread.sleep(400);
        grab("emu-weapon");
        tap(KeyEvent.VK_7);
        Thread.sleep(300);

        was = snap();
        tap(KeyEvent.VK_F1);             // left soft key: map overlay
        Thread.sleep(600);
        report("the left soft key opens the map", moved(was, 0, VIEW_ROWS));
        grab("emu-map");

        tap(KeyEvent.VK_F1);             // map off
        Thread.sleep(300);
        was = snap();
        tap(KeyEvent.VK_F2);             // right soft key: pause menu
        Thread.sleep(600);
        report("the right soft key pauses", moved(was, 0, VIEW_ROWS));
        grab("emu-pause");

        was = snap();
        tap(KeyEvent.VK_DOWN);
        tap(KeyEvent.VK_ENTER);          // upgrades screen
        Thread.sleep(600);
        report("the pause menu opens the upgrades screen",
                moved(was, 0, 300));
        grab("emu-upgrades");

        was = snap();
        tap(KeyEvent.VK_F2);             // leave the upgrades screen
        Thread.sleep(500);
        report("the right soft key leaves the upgrades screen",
                moved(was, 0, 300));
        tap(KeyEvent.VK_F2);             // back into the game
        Thread.sleep(400);
        grab("emu-resumed");
    }

    // ---- phase two: which keyboard keys reach the game ------------------

    /**
     * Press each candidate in a level and measure how much of the screen
     * moved. A key that reaches the game changes the view or the heads up
     * display far more than the idle animation does, and this is the only
     * way to find out: which host keys MicroEmulator forwards is not written
     * down anywhere in its jars.
     */
    private static void probeKeys() throws Exception {
        tap(KeyEvent.VK_ENTER);          // launcher
        Thread.sleep(2500);
        tap(KeyEvent.VK_ENTER);          // splash
        Thread.sleep(800);
        tap(KeyEvent.VK_ENTER);          // new campaign
        Thread.sleep(800);
        tap(KeyEvent.VK_ENTER);          // briefing, into the level
        Thread.sleep(1500);
        grab("probe-play");

        System.out.println("      key         view    hud");
        probe("(nothing)", -1, 500);
        probe("NUMPAD2", KeyEvent.VK_NUMPAD2, 500);
        probe("NUMPAD0", KeyEvent.VK_NUMPAD0, 500);
        probe("NUMPAD9", KeyEvent.VK_NUMPAD9, 300);
        probe("DOWN", KeyEvent.VK_DOWN, 500);
        probe("UP", KeyEvent.VK_UP, 500);
        probe("LEFT", KeyEvent.VK_LEFT, 400);
        probe("RIGHT", KeyEvent.VK_RIGHT, 400);
        probe("row 2", KeyEvent.VK_2, 500);
        probe("row 8", KeyEvent.VK_8, 500);
        probe("row 4", KeyEvent.VK_4, 400);
        probe("row 6", KeyEvent.VK_6, 400);
        probe("row 1", KeyEvent.VK_1, 500);
        probe("row 3", KeyEvent.VK_3, 500);
        probe("row 5", KeyEvent.VK_5, 600);
        probe("row 9", KeyEvent.VK_9, 300);
        probe("row 7", KeyEvent.VK_7, 300);
        probe("row 0", KeyEvent.VK_0, 400);
        probe("ENTER", KeyEvent.VK_ENTER, 300);
        probe("F1", KeyEvent.VK_F1, 700);
        tap(KeyEvent.VK_F1);
        Thread.sleep(400);
        probe("F2", KeyEvent.VK_F2, 700);
        grab("probe-end");
    }

    private static void probe(String name, int code, long ms) throws Exception {
        BufferedImage before = snap();
        if (code >= 0) {
            hold(code, ms);
        } else {
            Thread.sleep(ms);
        }
        Thread.sleep(250);
        BufferedImage after = snap();
        System.out.println("      " + pad(name)
                + pct(changed(before, after, 0, VIEW_ROWS)) + "\t"
                + pct(changed(before, after, VIEW_ROWS, 300)));
    }

    private static String pad(String s) {
        StringBuffer b = new StringBuffer(s);
        while (b.length() < 12) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String pct(double v) {
        return ((int) (v * 100.0)) + "%";
    }

    // ---- looking at the screen ------------------------------------------

    private static boolean moved(BufferedImage was, int y0, int y1) {
        return changed(was, snap(), y0, y1) > RESPONDED;
    }

    /**
     * Hold a key and watch for a flash: the only thing firing changes is the
     * muzzle light and the weapon's recoil, and both are over within a frame
     * or two, so a single look after the key is released sees nothing.
     *
     * The comparison here is deliberately the raw one. Brightness matching is
     * right for judging whether the player moved, because the day can darken
     * every pixel at once, but a muzzle flash *is* a brightness change and
     * normalising it away is how this check first came back empty handed.
     */
    private static boolean flashed(int code, long ms) throws Exception {
        BufferedImage was = snap();
        robot.keyPress(code);
        double worst = 0;
        long until = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < until) {
            double d = raw(was, snap(), 0, VIEW_ROWS);
            if (d > worst) {
                worst = d;
            }
            Thread.sleep(30);
        }
        robot.keyRelease(code);
        Thread.sleep(150);
        return worst > RESPONDED;
    }

    private static void report(String what, boolean ok) {
        if (ok) {
            System.out.println("      ok    " + what);
        } else {
            failures++;
            System.out.println("      FAIL  " + what);
        }
    }

    /** Fraction of rows y0..y1 that moved, once brightness is matched up. */
    private static double changed(BufferedImage a, BufferedImage b, int y0,
            int y1) {
        return compare(a, b, y0, y1, true);
    }

    /** The same, keeping any overall change in brightness. */
    private static double raw(BufferedImage a, BufferedImage b, int y0,
            int y1) {
        return compare(a, b, y0, y1, false);
    }

    private static double compare(BufferedImage a, BufferedImage b, int y0,
            int y1, boolean matchLight) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int hi = Math.min(y1, Math.min(a.getHeight(), b.getHeight()));
        int gain = matchLight ? gain(a, b, y0, hi) : 256;
        int moved = 0;
        int total = 0;
        for (int y = y0; y < hi; y++) {
            for (int x = 0; x < w; x++) {
                if (!near(a.getRGB(x, y), b.getRGB(x, y), gain)) {
                    moved++;
                }
                total++;
            }
        }
        return total == 0 ? 0.0 : (double) moved / (double) total;
    }

    private static boolean near(int pa, int pb, int gain) {
        int dr = Math.abs(((pa >> 16) & 255) - lift((pb >> 16) & 255, gain));
        int dg = Math.abs(((pa >> 8) & 255) - lift((pb >> 8) & 255, gain));
        int db = Math.abs((pa & 255) - lift(pb & 255, gain));
        return dr <= 12 && dg <= 12 && db <= 12;
    }

    private static int lift(int c, int gain) {
        int v = (c * gain) >> 8;
        return v > 255 ? 255 : v;
    }

    /**
     * How much b has to be brightened, in 256ths, for the two frames to carry
     * the same total light, so that a muzzle flash or a change of weapon does
     * not read as the whole screen having moved.
     */
    private static int gain(BufferedImage a, BufferedImage b, int y0, int y1) {
        long sa = light(a, y0, y1);
        long sb = light(b, y0, y1);
        if (sb <= 0) {
            return 256;
        }
        long g = (sa << 8) / sb;
        if (g < 64) {
            g = 64;
        } else if (g > 1024) {
            g = 1024;
        }
        return (int) g;
    }

    private static long light(BufferedImage img, int y0, int y1) {
        long sum = 0;
        int hi = Math.min(y1, img.getHeight());
        for (int y = y0; y < hi; y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                sum += ((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255);
            }
        }
        return sum;
    }

    // ---- the robot ------------------------------------------------------

    private static void tap(int code) throws Exception {
        robot.keyPress(code);
        Thread.sleep(60);
        robot.keyRelease(code);
        Thread.sleep(120);
    }

    private static void hold(int code, long ms) throws Exception {
        robot.keyPress(code);
        Thread.sleep(ms);
        robot.keyRelease(code);
    }

    private static void grab(String name) throws Exception {
        ImageIO.write(snap(), "png", new File(dir + "/" + name + ".png"));
        shot++;
    }

    /** The 240x320 device screen, without writing it anywhere. */
    private static BufferedImage snap() {
        Rectangle all = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage img = robot.createScreenCapture(all);
        // The emulator window sits at the top left with a menu bar above the
        // device screen. Crop to the 240x320 screen itself so the shots show
        // only what the phone would show.
        int w = Math.min(240, img.getWidth());
        int h = Math.min(320, img.getHeight() - MENU_BAR);
        return img.getSubimage(0, MENU_BAR, w, h);
    }

    private EmuDrive() {
    }
}
