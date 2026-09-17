package vz;

/**
 * All user visible text in one place: menus, mission briefings, help and the
 * in game messages. Constants only, so nothing is built at runtime.
 * MIDP free.
 */
public final class Text {

    public static final String TITLE = "VANGUARD ZERO";
    public static final String SUBTITLE = "Orbital Assault";
    public static final String VERSION = "v1.0";

    public static final String[] MENU_NEW = {
        "NEW CAMPAIGN", "OPTIONS", "HOW TO PLAY", "ABOUT", "EXIT"
    };
    public static final String[] MENU_SAVE = {
        "CONTINUE", "NEW CAMPAIGN", "OPTIONS", "HOW TO PLAY", "ABOUT", "EXIT"
    };

    public static final String[] MENU_PAUSE = {
        "RESUME", "UPGRADES", "SOUND", "RESTART LEVEL", "QUIT TO MENU"
    };

    public static final String[] STAT_NAME = {
        "VITALITY", "POWER", "AGILITY", "ARMOUR"
    };
    public static final String[] STAT_HINT = {
        "+15 max health",
        "+12% weapon damage",
        "+8% move speed",
        "+25 armour capacity"
    };

    public static final String[] WEAPON_NAME = { "PISTOL", "RIFLE", "PLASMA" };

    public static final String[] OBJECTIVE = {
        "REACH THE EXIT",
        "DESTROY THE REACTOR NODES",
        "DESTROY THE GUARDIAN"
    };

    /** Mission briefings, one block of lines per level. */
    public static final String[][] BRIEF = {
        {
            "Station Kestrel has gone dark.",
            "Its own defence network, ARBITER,",
            "turned the drones on the crew.",
            "",
            "You are Vanguard Zero, dropped",
            "into the docking ring alone.",
            "",
            "Find the keycard, clear the ring",
            "and reach the inner lift."
        },
        {
            "The hydroponics deck feeds the",
            "station and cools ARBITER's core.",
            "",
            "Two reactor nodes keep its",
            "shields online. Burn them both,",
            "then take the service lift down.",
            "",
            "Heavier units are awake now."
        },
        {
            "The reactor spine runs the length",
            "of the station. ARBITER has sealed",
            "it behind two locked bulkheads.",
            "",
            "Take both keycards, push through",
            "the spine and reach the bridge",
            "lift on the far side."
        },
        {
            "The command bridge. ARBITER's",
            "core is guarded by a Warden, the",
            "last thing the crew ever built.",
            "",
            "Kill it, walk into the core and",
            "end this."
        }
    };

    public static final String[] HELP = {
        "MOVE",
        "2 or UP    forward",
        "8 or DOWN  back",
        "4 / 6      turn",
        "1 / 3      strafe",
        "",
        "FIGHT",
        "5 or FIRE  shoot",
        "7 / 9      change weapon",
        "0          open door / use",
        "",
        "OTHER",
        "# or left soft   map",
        "* or right soft  menu",
        "",
        "Kill enemies to earn XP.",
        "Every level up gives 2 points",
        "to spend on your stats."
    };

    public static final String[] ABOUT = {
        "VANGUARD ZERO",
        "A first person shooter for",
        "Series 40 phones.",
        "",
        "Written in Java ME,",
        "MIDP 2.0 / CLDC 1.1.",
        "Software raycaster, no",
        "floating point at runtime.",
        "",
        "All art generated in code.",
        "",
        "Built for the Nokia 301."
    };

    // in game messages
    public static final String M_LOCKED_R = "RED KEYCARD REQUIRED";
    public static final String M_LOCKED_G = "GREEN KEYCARD REQUIRED";
    public static final String M_LOCKED_B = "BLUE KEYCARD REQUIRED";
    public static final String M_KEY_R = "RED KEYCARD TAKEN";
    public static final String M_KEY_G = "GREEN KEYCARD TAKEN";
    public static final String M_KEY_B = "BLUE KEYCARD TAKEN";
    public static final String M_MEDKIT = "MEDKIT";
    public static final String M_ARMOR = "ARMOUR PLATING";
    public static final String M_AMMO_R = "RIFLE ROUNDS";
    public static final String M_AMMO_P = "PLASMA CELLS";
    public static final String M_GUN_R = "RIFLE ACQUIRED";
    public static final String M_GUN_P = "PLASMA GUN ACQUIRED";
    public static final String M_NO_AMMO = "OUT OF AMMO";
    public static final String M_LEVEL_UP = "LEVEL UP";
    public static final String M_NODE_DOWN = "REACTOR NODE DESTROYED";
    public static final String M_NODES_LEFT = "NODES STILL ONLINE";
    public static final String M_GUARD_ALIVE = "THE GUARDIAN STILL LIVES";
    public static final String M_EXIT_OPEN = "LIFT UNLOCKED";
    public static final String M_FULL_HEALTH = "HEALTH FULL";
    public static final String M_FULL_ARMOR = "ARMOUR FULL";
    public static final String M_FULL_AMMO = "AMMO FULL";

    public static final String DEAD = "YOU ARE DEAD";
    public static final String COMPLETE = "SECTOR CLEAR";
    public static final String VICTORY = "ARBITER IS DOWN";
    public static final String VICTORY_2 = "The station is yours.";
    public static final String PRESS_FIRE = "PRESS 5";
    public static final String SOFT_BACK = "BACK";
    public static final String SOFT_OK = "OK";
    public static final String PAUSED = "PAUSED";
    public static final String SPEND = "SPEND POINTS";
    public static final String NO_POINTS = "NO POINTS TO SPEND";
    public static final String ON = "ON";
    public static final String OFF = "OFF";
    public static final String SOUND = "SOUND";
    public static final String DETAIL = "DETAIL";
    public static final String DETAIL_HIGH = "HIGH";
    public static final String DETAIL_LOW = "LOW";

    private Text() {
    }
}
