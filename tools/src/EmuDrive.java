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
 */
public final class EmuDrive {

    /** Height of the MicroEmulator menu bar above the device screen. */
    private static final int MENU_BAR = 20;

    private static Robot robot;
    private static String dir;
    private static int shot;

    public static void main(String[] args) throws Exception {
        dir = args.length > 0 ? args[0] : "shots";
        new File(dir).mkdirs();
        robot = new Robot();
        robot.setAutoDelay(40);
        Thread.sleep(args.length > 1 ? Long.parseLong(args[1]) : 6000);

        // no window manager under Xvfb, so click the canvas to give it focus
        robot.mouseMove(120, 200);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(300);
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
        Thread.sleep(1200);
        grab("emu-play");

        hold(KeyEvent.VK_NUMPAD2, 900);  // walk forward
        Thread.sleep(300);
        grab("emu-walk");

        hold(KeyEvent.VK_NUMPAD6, 500);  // turn right
        hold(KeyEvent.VK_NUMPAD2, 700);
        Thread.sleep(300);
        grab("emu-walk2");

        hold(KeyEvent.VK_NUMPAD5, 600);  // shoot
        Thread.sleep(300);
        grab("emu-fire");

        // MicroEmulator's resizable device has no * or # key, so the map and
        // the menu are reached through the soft keys, exactly as on a phone.
        tap(KeyEvent.VK_F1);             // left soft key: map overlay
        Thread.sleep(600);
        grab("emu-map");

        tap(KeyEvent.VK_F1);             // map off
        Thread.sleep(300);
        tap(KeyEvent.VK_F2);             // right soft key: pause menu
        Thread.sleep(600);
        grab("emu-pause");

        tap(KeyEvent.VK_DOWN);
        tap(KeyEvent.VK_ENTER);          // upgrades screen
        Thread.sleep(600);
        grab("emu-upgrades");

        tap(KeyEvent.VK_NUMPAD0);        // leave the upgrades screen
        Thread.sleep(400);
        tap(KeyEvent.VK_F2);             // back into the game
        Thread.sleep(400);
        grab("emu-resumed");

        System.out.println("wrote " + shot + " screenshots to " + dir);
    }

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
        Rectangle all = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage img = robot.createScreenCapture(all);
        // The emulator window sits at the top left with a menu bar above the
        // device screen. Crop to the 240x320 screen itself so the shots show
        // only what the phone would show.
        int x = 0;
        int y = MENU_BAR;
        int w = Math.min(240, img.getWidth());
        int h = Math.min(320, img.getHeight() - y);
        BufferedImage crop = img.getSubimage(x, y, w, h);
        ImageIO.write(crop, "png", new File(dir + "/" + name + ".png"));
        shot++;
    }

    private EmuDrive() {
    }
}
