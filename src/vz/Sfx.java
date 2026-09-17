package vz;

import javax.microedition.media.Manager;

/**
 * Sound effects through the single tone generator every MIDP 2.0 phone has.
 * Every call is guarded: on Series 40 a busy audio device throws, and a game
 * must never die because a beep failed.
 */
public final class Sfx {

    public static boolean on = true;

    private Sfx() {
    }

    private static void tone(int note, int ms, int vol) {
        if (!on) {
            return;
        }
        try {
            Manager.playTone(note, ms, vol);
        } catch (Throwable t) {
            on = false;     // broken audio on this device, stop trying
        }
    }

    public static void shot(int weapon) {
        if (weapon == Player.W_PLASMA) {
            tone(84, 70, 70);
        } else if (weapon == Player.W_RIFLE) {
            tone(72, 30, 60);
        } else {
            tone(66, 40, 60);
        }
    }

    public static void hit() {
        tone(52, 25, 45);
    }

    public static void kill() {
        tone(40, 90, 70);
    }

    public static void pickup() {
        tone(88, 60, 60);
    }

    public static void door() {
        tone(48, 90, 50);
    }

    public static void deny() {
        tone(36, 120, 55);
    }

    public static void hurt() {
        tone(45, 60, 75);
    }

    public static void levelUp() {
        tone(96, 120, 80);
    }

    public static void die() {
        tone(30, 400, 85);
    }

    public static void complete() {
        tone(90, 200, 80);
    }

    public static void menu() {
        tone(76, 20, 40);
    }

    /** Turn the events of one tick into at most one tone. */
    public static void play(int events, int weapon) {
        if ((events & World.EV_DIE) != 0) {
            die();
        } else if ((events & World.EV_COMPLETE) != 0) {
            complete();
        } else if ((events & World.EV_LEVELUP) != 0) {
            levelUp();
        } else if ((events & World.EV_KILL) != 0) {
            kill();
        } else if ((events & World.EV_SHOT) != 0) {
            shot(weapon);
        } else if ((events & World.EV_HURT) != 0) {
            hurt();
        } else if ((events & World.EV_PICKUP) != 0) {
            pickup();
        } else if ((events & World.EV_DOOR) != 0) {
            door();
        } else if ((events & World.EV_DENY) != 0 || (events & World.EV_NOAMMO) != 0) {
            deny();
        } else if ((events & World.EV_HIT) != 0) {
            hit();
        }
    }
}
