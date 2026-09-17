package vz;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * The one and only Displayable: game loop, input, state machine, HUD and all
 * menu screens.
 *
 * Series 40 notes that shape this class:
 *  - super(false) keeps key events, which GameCanvas would otherwise suppress
 *  - getKeyStates() only reports game actions, so the number keys are tracked
 *    in an own bit mask fed by keyPressed / keyReleased
 *  - the soft keys arrive as key codes -6 and -7, the centre key as -5
 *  - getGameAction() throws for unknown codes on some firmware, so it is
 *    always wrapped
 *  - the loop thread is created once and paused by hideNotify, never killed
 */
public final class Screen extends GameCanvas implements Runnable {

    private static final int ST_SPLASH = 0;
    private static final int ST_MENU = 1;
    private static final int ST_BRIEF = 2;
    private static final int ST_PLAY = 3;
    private static final int ST_PAUSE = 4;
    private static final int ST_LEVELUP = 5;
    private static final int ST_OPTIONS = 6;
    private static final int ST_HELP = 7;
    private static final int ST_ABOUT = 8;
    private static final int ST_DEAD = 9;
    private static final int ST_CLEAR = 10;
    private static final int ST_VICTORY = 11;
    private static final int ST_ERROR = 12;

    private static final int FRAME_MS = 66;

    // colours
    private static final int C_BG = 0x0B0E14;
    private static final int C_PANEL = 0x141A24;
    private static final int C_EDGE = 0x2C3A4E;
    private static final int C_TEXT = 0xC8D4E0;
    private static final int C_DIM = 0x7A8494;
    private static final int C_SEL = 0x40E0C0;
    private static final int C_HP = 0xE04030;
    private static final int C_ARMOR = 0x4090E0;
    private static final int C_XP = 0xE0B040;
    private static final int C_TITLE = 0x60E0FF;

    private final Main app;
    private final Player p = new Player();
    private final World world = new World();
    private Raycaster rc;

    private Thread thread;
    private volatile boolean running;
    private volatile boolean paused;

    private volatile int held;
    private volatile int tapped;

    private int state = ST_SPLASH;
    private int menuSel;
    private int scroll;
    private int splash = 45;
    private int levelUpReturn = ST_BRIEF;
    private boolean hasSave;
    private boolean lowDetail;
    private String error;

    private int viewX;
    private int viewW;
    private int panelY;

    private Font small;
    private Font bold;

    private final StringBuffer sb = new StringBuffer(32);
    private String hpStr = "";
    private String armStr = "";
    private String ammoStr = "";
    private String lvlStr = "";
    private int lastHp = -1;
    private int lastArm = -1;
    private int lastAmmo = -2;
    private int lastLvl = -1;
    private int lastWeapon = -1;
    private int fps;
    private int frames;
    private long fpsClock;

    public Screen(Main main) {
        super(false);
        app = main;
        setFullScreenMode(true);
        int flags = SaveStore.load(p);
        if (flags >= 0) {
            hasSave = (flags & SaveStore.F_HAS_SAVE) != 0;
            Sfx.on = (flags & SaveStore.F_SOUND) != 0;
            lowDetail = (flags & SaveStore.F_LOW_DETAIL) != 0;
        }
        if (flags < 0) {
            p.reset();
        }
    }

    // ---- lifecycle -----------------------------------------------------

    public void startLoop() {
        if (thread != null) {
            return;
        }
        running = true;
        paused = false;
        thread = new Thread(this);
        thread.start();
    }

    public void stopLoop() {
        running = false;
        thread = null;
    }

    public void pause() {
        paused = true;
        if (state == ST_PLAY) {
            state = ST_PAUSE;
            menuSel = 0;
        }
    }

    public void resume() {
        paused = false;
    }

    protected void hideNotify() {
        pause();
    }

    protected void showNotify() {
        paused = false;
    }

    protected void sizeChanged(int w, int h) {
        layout();
    }

    private void layout() {
        int w = getWidth();
        int h = getHeight();
        viewW = w < Raycaster.SCREEN_W ? w : Raycaster.SCREEN_W;
        viewX = (w - viewW) / 2;
        panelY = Raycaster.VIEW_H;
        if (panelY > h - 40) {
            panelY = h - 40;
        }
    }

    // ---- input ---------------------------------------------------------

    private int mapKey(int key) {
        switch (key) {
            case KEY_NUM2: return World.A_FWD;
            case KEY_NUM8: return World.A_BACK;
            case KEY_NUM4: return World.A_TL;
            case KEY_NUM6: return World.A_TR;
            case KEY_NUM1: return World.A_SL;
            case KEY_NUM3: return World.A_SR;
            case KEY_NUM5: return World.A_FIRE;
            case KEY_NUM7: return World.A_WPREV;
            case KEY_NUM9: return World.A_WNEXT;
            case KEY_NUM0: return World.A_USE;
            case KEY_POUND: return World.A_MAP;
            case KEY_STAR: return World.A_MENU;
            case -5: return World.A_FIRE;     // centre key
            case -6: return World.A_MAP;      // left soft key
            case -7: return World.A_MENU;     // right soft key
            default: break;
        }
        int action = 0;
        try {
            action = getGameAction(key);
        } catch (Throwable t) {
            action = 0;
        }
        switch (action) {
            case UP: return World.A_FWD;
            case DOWN: return World.A_BACK;
            case LEFT: return World.A_TL;
            case RIGHT: return World.A_TR;
            case FIRE: return World.A_FIRE;
            case GAME_A: return World.A_SL;
            case GAME_B: return World.A_SR;
            case GAME_C: return World.A_WPREV;
            case GAME_D: return World.A_WNEXT;
            default: return 0;
        }
    }

    protected void keyPressed(int key) {
        int a = mapKey(key);
        held |= a;
        tapped |= a;
    }

    protected void keyReleased(int key) {
        held &= ~mapKey(key);
    }

    protected void keyRepeated(int key) {
        // ignored on purpose: holding a key must not spam discrete actions
    }

    // ---- loop ----------------------------------------------------------

    public void run() {
        layout();
        try {
            rc = new Raycaster();
            rc.setDetail(lowDetail ? 3 : 2);
        } catch (Throwable t) {
            fail(t);
        }
        fpsClock = System.currentTimeMillis();
        while (running) {
            long t0 = System.currentTimeMillis();
            if (!paused) {
                try {
                    step();
                    Graphics g = getGraphics();
                    draw(g);
                    flushGraphics();
                } catch (Throwable t) {
                    fail(t);
                }
            }
            frames++;
            if (t0 - fpsClock >= 1000) {
                fps = frames;
                frames = 0;
                fpsClock = t0;
            }
            long dt = System.currentTimeMillis() - t0;
            long nap = FRAME_MS - dt;
            try {
                Thread.sleep(nap > 1 ? nap : 1);
            } catch (InterruptedException e) {
                // keep going
            }
        }
    }

    private void fail(Throwable t) {
        state = ST_ERROR;
        error = t.getClass().getName();
    }

    private void step() {
        int ks = 0;
        try {
            ks = getKeyStates();
        } catch (Throwable t) {
            ks = 0;
        }
        int in = held;
        if ((ks & UP_PRESSED) != 0) {
            in |= World.A_FWD;
        }
        if ((ks & DOWN_PRESSED) != 0) {
            in |= World.A_BACK;
        }
        if ((ks & LEFT_PRESSED) != 0) {
            in |= World.A_TL;
        }
        if ((ks & RIGHT_PRESSED) != 0) {
            in |= World.A_TR;
        }
        if ((ks & FIRE_PRESSED) != 0) {
            in |= World.A_FIRE;
        }
        if ((ks & GAME_A_PRESSED) != 0) {
            in |= World.A_SL;
        }
        if ((ks & GAME_B_PRESSED) != 0) {
            in |= World.A_SR;
        }
        int tap = tapped;
        tapped = 0;

        switch (state) {
            case ST_SPLASH:
                splash--;
                if (splash <= 0 || tap != 0) {
                    state = ST_MENU;
                    menuSel = 0;
                }
                break;
            case ST_MENU:
                stepMenu(tap);
                break;
            case ST_BRIEF:
                if (pressedOk(tap)) {
                    beginLevel(p.curLevel);
                } else {
                    stepScroll(tap, Text.BRIEF[p.curLevel].length);
                }
                break;
            case ST_PLAY:
                stepPlay(in, tap);
                break;
            case ST_PAUSE:
                stepPause(tap);
                break;
            case ST_LEVELUP:
                stepLevelUp(tap);
                break;
            case ST_OPTIONS:
                stepOptions(tap);
                break;
            case ST_HELP:
                if (pressedBack(tap) || pressedOk(tap)) {
                    state = ST_MENU;
                } else {
                    stepScroll(tap, Text.HELP.length);
                }
                break;
            case ST_ABOUT:
                if (pressedBack(tap) || pressedOk(tap)) {
                    state = ST_MENU;
                } else {
                    stepScroll(tap, Text.ABOUT.length);
                }
                break;
            case ST_DEAD:
                stepDead(tap);
                break;
            case ST_CLEAR:
                if (pressedOk(tap)) {
                    afterClear();
                }
                break;
            case ST_VICTORY:
                if (pressedOk(tap)) {
                    state = ST_MENU;
                    menuSel = 0;
                }
                break;
            default:
                if (pressedOk(tap)) {
                    state = ST_MENU;
                }
                break;
        }
    }

    /** Confirm: 5, the centre key, or the left soft key on a menu screen. */
    private boolean pressedOk(int tap) {
        return (tap & World.A_FIRE) != 0 || (tap & World.A_MAP) != 0;
    }

    private boolean pressedBack(int tap) {
        return (tap & World.A_MENU) != 0;
    }

    private int moveSel(int tap, int sel, int count) {
        if ((tap & World.A_FWD) != 0) {
            sel--;
            Sfx.menu();
        }
        if ((tap & World.A_BACK) != 0) {
            sel++;
            Sfx.menu();
        }
        if (sel < 0) {
            sel = count - 1;
        } else if (sel >= count) {
            sel = 0;
        }
        return sel;
    }

    private void stepScroll(int tap, int lines) {
        if ((tap & World.A_FWD) != 0 && scroll > 0) {
            scroll--;
        }
        if ((tap & World.A_BACK) != 0 && scroll < lines - 1) {
            scroll++;
        }
    }

    private String[] menuItems() {
        return hasSave ? Text.MENU_SAVE : Text.MENU_NEW;
    }

    private void stepMenu(int tap) {
        String[] items = menuItems();
        menuSel = moveSel(tap, menuSel, items.length);
        if (!pressedOk(tap)) {
            return;
        }
        String pick = items[menuSel];
        if (pick == Text.MENU_SAVE[0] && hasSave) {
            scroll = 0;
            state = ST_BRIEF;
        } else if (pick.equals("NEW CAMPAIGN")) {
            p.reset();
            hasSave = false;
            SaveStore.clear();
            scroll = 0;
            state = ST_BRIEF;
        } else if (pick.equals("OPTIONS")) {
            menuSel = 0;
            state = ST_OPTIONS;
        } else if (pick.equals("HOW TO PLAY")) {
            scroll = 0;
            state = ST_HELP;
        } else if (pick.equals("ABOUT")) {
            scroll = 0;
            state = ST_ABOUT;
        } else {
            app.exit();
        }
    }

    private void beginLevel(int index) {
        world.start(index, p);
        rc.setDetail(lowDetail ? 3 : 2);
        hasSave = true;
        SaveStore.save(p, flags());
        state = ST_PLAY;
        invalidateHud();
        System.gc();
    }

    private int flags() {
        int f = SaveStore.F_HAS_SAVE;
        if (Sfx.on) {
            f |= SaveStore.F_SOUND;
        }
        if (lowDetail) {
            f |= SaveStore.F_LOW_DETAIL;
        }
        return f;
    }

    private void stepPlay(int in, int tap) {
        if (pressedBack(tap)) {
            state = ST_PAUSE;
            menuSel = 0;
            return;
        }
        world.tick(in, tap);
        Sfx.play(world.events, p.weapon);
        if (world.dead) {
            state = ST_DEAD;
            menuSel = 0;
        } else if (world.complete) {
            SaveStore.save(p, flags());
            state = ST_CLEAR;
        }
    }

    private void afterClear() {
        boolean last = p.curLevel >= Levels.COUNT - 1;
        if (p.points > 0) {
            levelUpReturn = last ? ST_VICTORY : ST_BRIEF;
            menuSel = 0;
            state = ST_LEVELUP;
            if (!last) {
                p.curLevel++;
                SaveStore.save(p, flags());
            }
            scroll = 0;
            return;
        }
        if (last) {
            state = ST_VICTORY;
            return;
        }
        p.curLevel++;
        SaveStore.save(p, flags());
        scroll = 0;
        state = ST_BRIEF;
    }

    private void stepPause(int tap) {
        menuSel = moveSel(tap, menuSel, Text.MENU_PAUSE.length);
        if (pressedBack(tap)) {
            state = ST_PLAY;
            return;
        }
        if (!pressedOk(tap)) {
            return;
        }
        switch (menuSel) {
            case 0:
                state = ST_PLAY;
                break;
            case 1:
                levelUpReturn = ST_PLAY;
                menuSel = 0;
                state = ST_LEVELUP;
                break;
            case 2:
                Sfx.on = !Sfx.on;
                SaveStore.save(p, flags());
                break;
            case 3:
                beginLevel(p.curLevel);
                break;
            default:
                SaveStore.save(p, flags());
                state = ST_MENU;
                menuSel = 0;
                break;
        }
    }

    private void stepLevelUp(int tap) {
        menuSel = moveSel(tap, menuSel, Text.STAT_NAME.length);
        if (pressedOk(tap)) {
            if (p.spend(menuSel)) {
                Sfx.levelUp();
                SaveStore.save(p, flags());
            } else {
                Sfx.deny();
            }
        }
        if (pressedBack(tap) || (tap & World.A_USE) != 0) {
            state = levelUpReturn;
            menuSel = 0;
            scroll = 0;
        }
    }

    private void stepOptions(int tap) {
        menuSel = moveSel(tap, menuSel, 3);
        if (pressedBack(tap)) {
            state = ST_MENU;
            menuSel = 0;
            return;
        }
        if (!pressedOk(tap)) {
            return;
        }
        if (menuSel == 0) {
            Sfx.on = !Sfx.on;
            Sfx.menu();
        } else if (menuSel == 1) {
            lowDetail = !lowDetail;
            rc.setDetail(lowDetail ? 3 : 2);
        } else {
            state = ST_MENU;
            menuSel = 0;
        }
        SaveStore.save(p, flags());
    }

    private void stepDead(int tap) {
        menuSel = moveSel(tap, menuSel, 2);
        if (!pressedOk(tap)) {
            return;
        }
        if (menuSel == 0) {
            beginLevel(p.curLevel);
        } else {
            state = ST_MENU;
            menuSel = 0;
        }
    }

    // ---- drawing -------------------------------------------------------

    private void fonts() {
        if (small == null) {
            small = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            bold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        }
    }

    private void draw(Graphics g) {
        fonts();
        if (viewW == 0) {
            layout();
        }
        switch (state) {
            case ST_PLAY:
                drawPlay(g);
                break;
            case ST_SPLASH:
                drawSplash(g);
                break;
            case ST_MENU:
                drawMenu(g, Text.TITLE, menuItems(), menuSel, Text.SUBTITLE);
                break;
            case ST_BRIEF:
                drawBrief(g);
                break;
            case ST_PAUSE:
                drawPlay(g);
                drawOverlayMenu(g, Text.PAUSED, Text.MENU_PAUSE, menuSel);
                break;
            case ST_LEVELUP:
                drawLevelUp(g);
                break;
            case ST_OPTIONS:
                drawOptions(g);
                break;
            case ST_HELP:
                drawLines(g, "HOW TO PLAY", Text.HELP);
                break;
            case ST_ABOUT:
                drawLines(g, "ABOUT", Text.ABOUT);
                break;
            case ST_DEAD:
                drawDead(g);
                break;
            case ST_CLEAR:
                drawClear(g);
                break;
            case ST_VICTORY:
                drawVictory(g);
                break;
            default:
                drawError(g);
                break;
        }
    }

    private void clear(Graphics g, int colour) {
        g.setColor(colour);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawSplash(Graphics g) {
        clear(g, C_BG);
        int w = getWidth();
        int h = getHeight();
        g.setFont(bold);
        g.setColor(C_TITLE);
        g.drawString(Text.TITLE, w / 2, h / 2 - 20, Graphics.HCENTER | Graphics.TOP);
        g.setFont(small);
        g.setColor(C_DIM);
        g.drawString(Text.SUBTITLE, w / 2, h / 2 + 6, Graphics.HCENTER | Graphics.TOP);
        g.setColor(C_EDGE);
        g.drawRect(20, h / 2 - 34, w - 41, 60);
    }

    private void drawMenu(Graphics g, String title, String[] items, int sel, String sub) {
        clear(g, C_BG);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(C_TITLE);
        g.drawString(title, w / 2, 26, Graphics.HCENTER | Graphics.TOP);
        if (sub != null) {
            g.setFont(small);
            g.setColor(C_DIM);
            g.drawString(sub, w / 2, 26 + bold.getHeight(), Graphics.HCENTER | Graphics.TOP);
        }
        g.setColor(C_EDGE);
        g.drawLine(16, 74, w - 17, 74);
        drawItems(g, items, sel, 92, w);
        g.setFont(small);
        g.setColor(C_DIM);
        g.drawString(Text.VERSION, w - 6, getHeight() - small.getHeight() - 4,
                Graphics.RIGHT | Graphics.TOP);
        g.drawString("5 = SELECT", 6, getHeight() - small.getHeight() - 4,
                Graphics.LEFT | Graphics.TOP);
    }

    private void drawItems(Graphics g, String[] items, int sel, int y, int w) {
        g.setFont(small);
        int lh = small.getHeight() + 8;
        for (int i = 0; i < items.length; i++) {
            int iy = y + i * lh;
            if (i == sel) {
                g.setColor(0x1E2C3A);
                g.fillRect(14, iy - 2, w - 29, lh - 2);
                g.setColor(C_SEL);
                g.drawRect(14, iy - 2, w - 29, lh - 2);
                g.setColor(C_SEL);
            } else {
                g.setColor(C_TEXT);
            }
            g.drawString(items[i], w / 2, iy + 1, Graphics.HCENTER | Graphics.TOP);
        }
    }

    private void drawOverlayMenu(Graphics g, String title, String[] items, int sel) {
        int w = getWidth();
        int h = getHeight();
        int boxH = items.length * (small.getHeight() + 8) + 44;
        int y0 = (h - boxH) / 2;
        g.setColor(C_BG);
        g.fillRect(10, y0, w - 21, boxH);
        g.setColor(C_SEL);
        g.drawRect(10, y0, w - 21, boxH);
        g.setFont(bold);
        g.setColor(C_TITLE);
        g.drawString(title, w / 2, y0 + 8, Graphics.HCENTER | Graphics.TOP);
        drawItems(g, items, sel, y0 + 34, w);
    }

    private void drawBrief(Graphics g) {
        clear(g, C_BG);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(C_TITLE);
        sb.setLength(0);
        sb.append("SECTOR ");
        sb.append(p.curLevel + 1);
        g.drawString(sb.toString(), w / 2, 12, Graphics.HCENTER | Graphics.TOP);
        g.setFont(small);
        g.setColor(C_TEXT);
        g.drawString(Levels.NAME[p.curLevel], w / 2, 12 + bold.getHeight(),
                Graphics.HCENTER | Graphics.TOP);
        g.setColor(C_XP);
        g.drawString(Text.OBJECTIVE[Levels.OBJECTIVE[p.curLevel]], w / 2,
                14 + bold.getHeight() + small.getHeight(),
                Graphics.HCENTER | Graphics.TOP);
        g.setColor(C_EDGE);
        int top = 18 + bold.getHeight() + small.getHeight() * 2;
        g.drawLine(12, top, w - 13, top);
        body(g, Text.BRIEF[p.curLevel], top + 6);
        footer(g, Text.PRESS_FIRE);
    }

    private void drawLines(Graphics g, String title, String[] lines) {
        clear(g, C_BG);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(C_TITLE);
        g.drawString(title, w / 2, 10, Graphics.HCENTER | Graphics.TOP);
        g.setColor(C_EDGE);
        g.drawLine(12, 10 + bold.getHeight() + 2, w - 13, 10 + bold.getHeight() + 2);
        body(g, lines, 10 + bold.getHeight() + 8);
        footer(g, "2/8 SCROLL   5 BACK");
    }

    /** Draw a block of lines from the current scroll offset. */
    private void body(Graphics g, String[] lines, int y) {
        g.setFont(small);
        int lh = small.getHeight() + 1;
        int room = (getHeight() - y - small.getHeight() - 8) / lh;
        if (scroll > lines.length - 1) {
            scroll = lines.length - 1;
        }
        int end = scroll + room;
        if (end > lines.length) {
            end = lines.length;
        }
        for (int i = scroll; i < end; i++) {
            g.setColor(lines[i].length() > 0 && lines[i].charAt(0) != ' '
                    && isHeading(lines[i]) ? C_SEL : C_TEXT);
            g.drawString(lines[i], 10, y + (i - scroll) * lh, Graphics.LEFT | Graphics.TOP);
        }
        if (end < lines.length) {
            g.setColor(C_DIM);
            g.drawString("...", getWidth() - 20, y + room * lh - lh,
                    Graphics.LEFT | Graphics.TOP);
        }
    }

    private boolean isHeading(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                return false;
            }
        }
        return s.length() < 14;
    }

    private void footer(Graphics g, String s) {
        g.setFont(small);
        g.setColor(C_DIM);
        g.drawString(s, getWidth() / 2, getHeight() - small.getHeight() - 3,
                Graphics.HCENTER | Graphics.TOP);
    }

    private void drawOptions(Graphics g) {
        clear(g, C_BG);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(C_TITLE);
        g.drawString("OPTIONS", w / 2, 26, Graphics.HCENTER | Graphics.TOP);
        String[] items = new String[3];
        sb.setLength(0);
        sb.append(Text.SOUND);
        sb.append("  ");
        sb.append(Sfx.on ? Text.ON : Text.OFF);
        items[0] = sb.toString();
        sb.setLength(0);
        sb.append(Text.DETAIL);
        sb.append("  ");
        sb.append(lowDetail ? Text.DETAIL_LOW : Text.DETAIL_HIGH);
        items[1] = sb.toString();
        items[2] = Text.SOFT_BACK;
        drawItems(g, items, menuSel, 90, w);
        footer(g, "5 CHANGE");
    }

    private void drawLevelUp(Graphics g) {
        clear(g, C_BG);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(C_TITLE);
        g.drawString(Text.SPEND, w / 2, 10, Graphics.HCENTER | Graphics.TOP);
        g.setFont(small);
        g.setColor(C_XP);
        sb.setLength(0);
        sb.append("LEVEL ");
        sb.append(p.level);
        sb.append("   POINTS ");
        sb.append(p.points);
        g.drawString(sb.toString(), w / 2, 10 + bold.getHeight(),
                Graphics.HCENTER | Graphics.TOP);

        int y = 16 + bold.getHeight() + small.getHeight();
        int lh = small.getHeight() * 2 + 10;
        for (int i = 0; i < Text.STAT_NAME.length; i++) {
            int iy = y + i * lh;
            if (i == menuSel) {
                g.setColor(0x1E2C3A);
                g.fillRect(8, iy - 2, w - 17, lh - 4);
                g.setColor(C_SEL);
                g.drawRect(8, iy - 2, w - 17, lh - 4);
            }
            g.setColor(i == menuSel ? C_SEL : C_TEXT);
            g.drawString(Text.STAT_NAME[i], 14, iy, Graphics.LEFT | Graphics.TOP);
            // pips showing how many points are already spent
            int px = w - 20;
            for (int k = Balance.MAX_STAT - 1; k >= 0; k--) {
                g.setColor(k < p.stat(i) ? C_XP : C_EDGE);
                g.fillRect(px - 8, iy + 2, 6, 6);
                px -= 10;
            }
            g.setColor(C_DIM);
            g.drawString(Text.STAT_HINT[i], 14, iy + small.getHeight(),
                    Graphics.LEFT | Graphics.TOP);
        }
        footer(g, p.points > 0 ? "5 SPEND   0 DONE" : "0 DONE");
    }

    private void drawDead(Graphics g) {
        clear(g, 0x200A0A);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(0xFF6050);
        g.drawString(Text.DEAD, w / 2, 40, Graphics.HCENTER | Graphics.TOP);
        String[] items = { "RETRY LEVEL", "QUIT TO MENU" };
        drawItems(g, items, menuSel, 100, w);
    }

    private void drawClear(Graphics g) {
        clear(g, C_BG);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(C_SEL);
        g.drawString(Text.COMPLETE, w / 2, 24, Graphics.HCENTER | Graphics.TOP);
        g.setFont(small);
        g.setColor(C_TEXT);
        g.drawString(Levels.NAME[p.curLevel], w / 2, 24 + bold.getHeight(),
                Graphics.HCENTER | Graphics.TOP);
        stats(g, 24 + bold.getHeight() + small.getHeight() * 2);
        footer(g, Text.PRESS_FIRE);
    }

    private void drawVictory(Graphics g) {
        clear(g, 0x0A1A14);
        int w = getWidth();
        g.setFont(bold);
        g.setColor(C_SEL);
        g.drawString(Text.VICTORY, w / 2, 24, Graphics.HCENTER | Graphics.TOP);
        g.setFont(small);
        g.setColor(C_TEXT);
        g.drawString(Text.VICTORY_2, w / 2, 24 + bold.getHeight(),
                Graphics.HCENTER | Graphics.TOP);
        stats(g, 24 + bold.getHeight() + small.getHeight() * 2);
        footer(g, Text.PRESS_FIRE);
    }

    private void stats(Graphics g, int y) {
        g.setFont(small);
        int lh = small.getHeight() + 4;
        int w = getWidth();
        line(g, "LEVEL", p.level, 14, y, w);
        line(g, "KILLS", p.kills, 14, y + lh, w);
        line(g, "POINTS", p.points, 14, y + lh * 2, w);
        g.setColor(C_DIM);
        g.drawString("TIME", 14, y + lh * 3, Graphics.LEFT | Graphics.TOP);
        sb.setLength(0);
        int secs = p.ticks / Balance.TPS;
        sb.append(secs / 60);
        sb.append(':');
        int rem = secs % 60;
        if (rem < 10) {
            sb.append('0');
        }
        sb.append(rem);
        g.setColor(C_TEXT);
        g.drawString(sb.toString(), w - 14, y + lh * 3, Graphics.RIGHT | Graphics.TOP);
    }

    private void line(Graphics g, String label, int value, int x, int y, int w) {
        g.setColor(C_DIM);
        g.drawString(label, x, y, Graphics.LEFT | Graphics.TOP);
        g.setColor(C_TEXT);
        g.drawString(String.valueOf(value), w - x, y, Graphics.RIGHT | Graphics.TOP);
    }

    private void drawError(Graphics g) {
        clear(g, 0x201010);
        g.setFont(small);
        g.setColor(0xFFA0A0);
        g.drawString("ERROR", 8, 10, Graphics.LEFT | Graphics.TOP);
        if (error != null) {
            g.drawString(error, 8, 10 + small.getHeight() * 2, Graphics.LEFT | Graphics.TOP);
        }
        footer(g, "5 MENU");
    }

    // ---- gameplay screen ------------------------------------------------

    private void invalidateHud() {
        lastHp = -1;
        lastArm = -1;
        lastAmmo = -2;
        lastLvl = -1;
        lastWeapon = -1;
    }

    private void drawPlay(Graphics g) {
        rc.render(world.lvl, world);
        g.drawRGB(rc.out, 0, Raycaster.SCREEN_W, viewX, 0, viewW,
                Raycaster.VIEW_H, false);
        if (world.showMap) {
            drawMap(g);
        }
        drawHud(g);
        if (world.msg != null) {
            g.setFont(small);
            g.setColor(0x000000);
            g.fillRect(0, 2, getWidth(), small.getHeight() + 2);
            g.setColor(C_SEL);
            g.drawString(world.msg, getWidth() / 2, 3, Graphics.HCENTER | Graphics.TOP);
        }
    }

    private void drawHud(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        int y = panelY;
        g.setColor(C_PANEL);
        g.fillRect(0, y, w, h - y);
        g.setColor(C_EDGE);
        g.drawLine(0, y, w, y);
        g.setFont(small);

        int lh = small.getHeight();
        int barW = w - 74;

        if (p.hp != lastHp) {
            lastHp = p.hp;
            hpStr = build("HP ", p.hp);
        }
        if (p.armor != lastArm) {
            lastArm = p.armor;
            armStr = build("AR ", p.armor);
        }
        int ammo = p.ammo(p.weapon);
        if (ammo != lastAmmo || p.weapon != lastWeapon) {
            lastAmmo = ammo;
            lastWeapon = p.weapon;
            sb.setLength(0);
            sb.append(Text.WEAPON_NAME[p.weapon]);
            sb.append("  ");
            if (ammo < 0) {
                sb.append("--");
            } else {
                sb.append(ammo);
            }
            ammoStr = sb.toString();
        }
        if (p.level != lastLvl) {
            lastLvl = p.level;
            lvlStr = build("LV ", p.level);
        }

        int row = y + 4;
        g.setColor(C_TEXT);
        g.drawString(hpStr, 4, row, Graphics.LEFT | Graphics.TOP);
        bar(g, 66, row + 2, barW, lh - 4, p.hp, p.maxHp(), C_HP);
        row += lh + 2;
        g.setColor(C_TEXT);
        g.drawString(armStr, 4, row, Graphics.LEFT | Graphics.TOP);
        bar(g, 66, row + 2, barW, lh - 4, p.armor, p.armorCap(), C_ARMOR);
        row += lh + 2;
        g.setColor(C_TEXT);
        g.drawString(lvlStr, 4, row, Graphics.LEFT | Graphics.TOP);
        bar(g, 66, row + 2, barW, lh - 4, p.xp, p.xpToNext(), C_XP);
        row += lh + 4;

        g.setColor(C_SEL);
        g.drawString(ammoStr, 4, row, Graphics.LEFT | Graphics.TOP);
        // keycards
        int kx = w - 8;
        for (int i = 2; i >= 0; i--) {
            int bit = 1 << i;
            int col = i == 0 ? 0xE03028 : i == 1 ? 0x28B048 : 0x3060E0;
            g.setColor((p.keys & bit) != 0 ? col : C_EDGE);
            g.fillRect(kx - 8, row + 2, 7, 9);
            kx -= 11;
        }
        row += lh + 2;

        g.setColor(C_DIM);
        if (world.lvl.objective == Levels.OBJ_NODES && world.nodesLeft > 0) {
            g.drawString(build("NODES LEFT ", world.nodesLeft), 4, row,
                    Graphics.LEFT | Graphics.TOP);
        } else if (world.lvl.objective != Levels.OBJ_EXIT && world.objectiveDone()) {
            g.setColor(C_SEL);
            g.drawString(Text.M_EXIT_OPEN, 4, row, Graphics.LEFT | Graphics.TOP);
        } else {
            g.drawString(Text.OBJECTIVE[world.lvl.objective], 4, row,
                    Graphics.LEFT | Graphics.TOP);
        }
        row += lh + 1;
        if (p.points > 0) {
            g.setColor(C_XP);
            g.drawString(build("UPGRADE POINTS ", p.points), 4, row,
                    Graphics.LEFT | Graphics.TOP);
        } else {
            g.setColor(C_DIM);
            g.drawString("0 USE   # MAP   * MENU", 4, row, Graphics.LEFT | Graphics.TOP);
        }
        if (fps > 0 && world.showMap) {
            g.setColor(C_DIM);
            g.drawString(build("FPS ", fps), w - 4, y + 4, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private String build(String label, int value) {
        sb.setLength(0);
        sb.append(label);
        sb.append(value);
        return sb.toString();
    }

    private void bar(Graphics g, int x, int y, int w, int h, int value, int max,
            int colour) {
        if (max <= 0) {
            max = 1;
        }
        int fill = (w - 2) * FX.clamp(value, 0, max) / max;
        g.setColor(0x0A0C10);
        g.fillRect(x, y, w, h);
        g.setColor(C_EDGE);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(colour);
        g.fillRect(x + 1, y + 1, fill, h - 2);
    }

    /** Overview map of the cells the player has already seen. */
    private void drawMap(Graphics g) {
        Level lvl = world.lvl;
        int cell = 5;
        int mw = lvl.w * cell;
        int mh = lvl.h * cell;
        int ox = (getWidth() - mw) / 2;
        int oy = (Raycaster.VIEW_H - mh) / 2;
        if (oy < 2) {
            oy = 2;
        }
        g.setColor(0x000000);
        g.fillRect(ox - 2, oy - 2, mw + 4, mh + 4);
        for (int y = 0; y < lvl.h; y++) {
            for (int x = 0; x < lvl.w; x++) {
                if (!lvl.seen[(y << Level.SHIFT) + x]) {
                    continue;
                }
                int c = lvl.cell(x, y);
                int colour;
                if (c == Level.FLOOR) {
                    colour = 0x2A3440;
                } else if (c == Level.EXIT) {
                    colour = 0x30E070;
                } else if (c == Level.DOOR) {
                    colour = 0xC0A030;
                } else if (c == Level.LOCK_R) {
                    colour = 0xE03028;
                } else if (c == Level.LOCK_G) {
                    colour = 0x28B048;
                } else if (c == Level.LOCK_B) {
                    colour = 0x3060E0;
                } else {
                    colour = 0x707A88;
                }
                g.setColor(colour);
                g.fillRect(ox + x * cell, oy + y * cell, cell - 1, cell - 1);
            }
        }
        // enemies and items that are currently visible
        for (int i = 0; i < world.nEnts; i++) {
            Entity e = world.ents[i];
            if (!e.active || e.state == Entity.S_DEAD) {
                continue;
            }
            int ex = e.x >> 16;
            int ey = e.y >> 16;
            if (!lvl.seen[(ey << Level.SHIFT) + ex]) {
                continue;
            }
            if (!lvl.sight(world.px, world.py, e.x, e.y)) {
                continue;
            }
            g.setColor(e.isEnemy() ? 0xFF5040 : 0x40D0FF);
            g.fillRect(ox + ex * cell + 1, oy + ey * cell + 1, cell - 3, cell - 3);
        }
        int px = world.px >> 16;
        int py = world.py >> 16;
        g.setColor(0xFFFFFF);
        g.fillRect(ox + px * cell + 1, oy + py * cell + 1, cell - 2, cell - 2);
        // heading tick
        int hx = ox + px * cell + cell / 2 + ((FX.cos(world.ang) * cell) >> 16);
        int hy = oy + py * cell + cell / 2 + ((FX.sin(world.ang) * cell) >> 16);
        g.setColor(0xFFE060);
        g.fillRect(hx - 1, hy - 1, 2, 2);
    }
}
