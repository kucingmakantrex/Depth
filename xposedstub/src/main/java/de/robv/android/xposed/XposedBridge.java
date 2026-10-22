package de.robv.android.xposed;

public final class XposedBridge {
    private XposedBridge() {}
    public static void log(String text) { throw new RuntimeException("stub"); }
    public static void log(Throwable t) { throw new RuntimeException("stub"); }
}
