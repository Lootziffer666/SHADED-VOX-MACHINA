package com.google.ar.core;

/* JADX INFO: loaded from: classes.dex */
public class ArCoreNativeInterop {
    private ArCoreNativeInterop() {
    }

    public static long getNativeHandle(Frame frame) {
        return frame.nativeHandle;
    }

    public static long getSymbolTable(Session session) {
        return session.nativeSymbolTableHandle;
    }

    public static long getNativeHandle(Session session) {
        return session.nativeGetSessionNativeHandle(session.nativeWrapperHandle);
    }
}
