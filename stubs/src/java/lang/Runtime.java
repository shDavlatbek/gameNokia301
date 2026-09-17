package java.lang;
public class Runtime {
    private Runtime() {}
    public static Runtime getRuntime() { return null; }
    public void exit(int status) {}
    public long freeMemory() { return 0; }
    public long totalMemory() { return 0; }
    public void gc() {}
}
