package java.util;
public abstract class Calendar {
    public static final int YEAR = 1, MONTH = 2, DAY_OF_MONTH = 5, HOUR_OF_DAY = 11, MINUTE = 12, SECOND = 13;
    protected Calendar() {}
    public static synchronized Calendar getInstance() { return null; }
    public static synchronized Calendar getInstance(TimeZone zone) { return null; }
    public final Date getTime() { return null; }
    public final void setTime(Date date) {}
    public int get(int field) { return 0; }
    public void set(int field, int value) {}
    public TimeZone getTimeZone() { return null; }
    public void setTimeZone(TimeZone value) {}
}
