package java.util;
public abstract class TimeZone {
    public TimeZone() {}
    public static synchronized TimeZone getDefault() { return null; }
    public static synchronized TimeZone getTimeZone(String ID) { return null; }
    public static String[] getAvailableIDs() { return null; }
    public String getID() { return null; }
    public abstract int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis);
    public abstract int getRawOffset();
    public abstract boolean useDaylightTime();
}
