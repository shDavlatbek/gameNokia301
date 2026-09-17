package java.lang;
public class Object {
    public Object() {}
    public boolean equals(Object o) { return false; }
    public final native Class getClass();
    public native int hashCode();
    public final native void notify();
    public final native void notifyAll();
    public String toString() { return null; }
    public final native void wait() throws InterruptedException;
    public final native void wait(long t) throws InterruptedException;
    public final native void wait(long t, int n) throws InterruptedException;
}
