package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XC_MethodHook;

public final class XposedHelpers {
    private XposedHelpers() {}
    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader,
            String methodName, Object... parameterTypesAndCallback) {
        throw new RuntimeException("stub");
    }
    public static int getIntField(Object obj, String fieldName) { throw new RuntimeException("stub"); }
}
