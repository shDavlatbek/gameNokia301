package vz;

import javax.microedition.rms.RecordStore;

/**
 * One tiny RMS record holding the player's progress and the options.
 * The store is opened and closed around every call and every failure is
 * swallowed: a phone with a full or broken record store still plays.
 */
public final class SaveStore {

    public static final int F_SOUND = 1;
    public static final int F_LOW_DETAIL = 2;
    public static final int F_HAS_SAVE = 4;

    private static final String NAME = "vzsave";

    private static final byte[] BUF = new byte[Player.SAVE_BYTES];

    private SaveStore() {
    }

    /** Returns the flags byte, or -1 when there is no usable save. */
    public static int load(Player p) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(NAME, false);
            if (rs.getNumRecords() < 1) {
                return -1;
            }
            int size = rs.getRecordSize(1);
            if (size < Player.SAVE_BYTES) {
                return -1;
            }
            rs.getRecord(1, BUF, 0);
            return p.fromBytes(BUF, Player.SAVE_BYTES);
        } catch (Throwable t) {
            return -1;
        } finally {
            close(rs);
        }
    }

    public static boolean save(Player p, int flags) {
        RecordStore rs = null;
        try {
            p.toBytes(BUF, flags);
            rs = RecordStore.openRecordStore(NAME, true);
            if (rs.getNumRecords() < 1) {
                rs.addRecord(BUF, 0, Player.SAVE_BYTES);
            } else {
                rs.setRecord(1, BUF, 0, Player.SAVE_BYTES);
            }
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            close(rs);
        }
    }

    /** Store only the options, keeping any campaign progress intact. */
    public static void saveOptions(Player p, int flags) {
        save(p, flags);
    }

    public static void clear() {
        try {
            RecordStore.deleteRecordStore(NAME);
        } catch (Throwable t) {
            // nothing to delete
        }
    }

    private static void close(RecordStore rs) {
        if (rs != null) {
            try {
                rs.closeRecordStore();
            } catch (Throwable t) {
                // ignored
            }
        }
    }
}
