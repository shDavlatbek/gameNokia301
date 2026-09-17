package vz;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/**
 * MIDlet entry point. Owns nothing but the lifecycle: the canvas is created
 * once and kept, because Series 40 calls startApp again after a phone call.
 */
public class Main extends MIDlet {

    private Screen screen;

    public Main() {
    }

    protected void startApp() {
        if (screen == null) {
            screen = new Screen(this);
            Display.getDisplay(this).setCurrent(screen);
            screen.startLoop();
        } else {
            Display.getDisplay(this).setCurrent(screen);
            screen.resume();
        }
    }

    protected void pauseApp() {
        if (screen != null) {
            screen.pause();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (screen != null) {
            screen.stopLoop();
        }
    }

    /** Called by the canvas when the player picks Exit. */
    public void exit() {
        destroyApp(false);
        notifyDestroyed();
    }
}
