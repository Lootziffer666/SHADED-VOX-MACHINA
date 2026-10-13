package com.google.vr.dynamite.client;

import android.content.Context;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import dalvik.system.DexClassLoader;

/* JADX INFO: loaded from: classes.dex */
public final class DynamiteClient {
    private static final ArrayMap a = new ArrayMap();

    private DynamiteClient() {
    }

    public static synchronized int checkVersion(Context context, String str, String str2, String str3) {
        f fVar = new f(str, str2);
        e remoteLibraryLoaderFromInfo = getRemoteLibraryLoaderFromInfo(fVar);
        try {
            INativeLibraryLoader iNativeLibraryLoaderNewNativeLibraryLoader = remoteLibraryLoaderFromInfo.a(context).newNativeLibraryLoader(ObjectWrapper.b(remoteLibraryLoaderFromInfo.b(context)), ObjectWrapper.b(context));
            if (iNativeLibraryLoaderNewNativeLibraryLoader != null) {
                return iNativeLibraryLoaderNewNativeLibraryLoader.checkVersion(str3);
            }
            String string = fVar.toString();
            StringBuilder sb = new StringBuilder(string.length() + 72);
            sb.append("Failed to load native library ");
            sb.append(string);
            sb.append(" from remote package: no loader available.");
            Log.e("DynamiteClient", sb.toString());
            return -1;
        } catch (RemoteException | d | IllegalArgumentException | IllegalStateException | SecurityException | UnsatisfiedLinkError e) {
            String string2 = fVar.toString();
            StringBuilder sb2 = new StringBuilder(string2.length() + 54);
            sb2.append("Failed to load native library ");
            sb2.append(string2);
            sb2.append(" from remote package:\n  ");
            Log.e("DynamiteClient", sb2.toString(), e);
            return -1;
        }
    }

    public static synchronized ClassLoader getRemoteClassLoader(Context context, String str, String str2) {
        Context remoteContext = getRemoteContext(context, str, str2);
        if (remoteContext == null) {
            return null;
        }
        return remoteContext.getClassLoader();
    }

    public static synchronized Context getRemoteContext(Context context, String str, String str2) {
        f fVar;
        fVar = new f(str, str2);
        try {
        } catch (d e) {
            String string = fVar.toString();
            StringBuilder sb = new StringBuilder(string.length() + 52);
            sb.append("Failed to get remote Context");
            sb.append(string);
            sb.append(" from remote package:\n  ");
            Log.e("DynamiteClient", sb.toString(), e);
            return null;
        }
        return getRemoteLibraryLoaderFromInfo(fVar).b(context);
    }

    public static synchronized ClassLoader getRemoteDexClassLoader(Context context, String str) {
        Context remoteContext = getRemoteContext(context, str, null);
        if (remoteContext == null) {
            return null;
        }
        try {
            return new DexClassLoader(remoteContext.getPackageCodePath(), context.getCodeCacheDir().getAbsolutePath(), remoteContext.getApplicationInfo().nativeLibraryDir, context.getClassLoader());
        } catch (RuntimeException e) {
            Log.e("DynamiteClient", "Failed to create class loader for remote package\n ", e);
            return null;
        }
    }

    private static synchronized e getRemoteLibraryLoaderFromInfo(f fVar) {
        ArrayMap arrayMap = a;
        e eVar = (e) arrayMap.get(fVar);
        if (eVar != null) {
            return eVar;
        }
        e eVar2 = new e(fVar);
        arrayMap.put(fVar, eVar2);
        return eVar2;
    }

    public static synchronized long loadNativeRemoteLibrary(Context context, String str, String str2) {
        f fVar = new f(str, str2);
        e remoteLibraryLoaderFromInfo = getRemoteLibraryLoaderFromInfo(fVar);
        try {
            INativeLibraryLoader iNativeLibraryLoaderNewNativeLibraryLoader = remoteLibraryLoaderFromInfo.a(context).newNativeLibraryLoader(ObjectWrapper.b(remoteLibraryLoaderFromInfo.b(context)), ObjectWrapper.b(context));
            if (iNativeLibraryLoaderNewNativeLibraryLoader != null) {
                return iNativeLibraryLoaderNewNativeLibraryLoader.initializeAndLoadNativeLibrary(str2);
            }
            String string = fVar.toString();
            StringBuilder sb = new StringBuilder(string.length() + 72);
            sb.append("Failed to load native library ");
            sb.append(string);
            sb.append(" from remote package: no loader available.");
            Log.e("DynamiteClient", sb.toString());
            return 0L;
        } catch (RemoteException | d | IllegalArgumentException | IllegalStateException | SecurityException | UnsatisfiedLinkError e) {
            String string2 = fVar.toString();
            StringBuilder sb2 = new StringBuilder(string2.length() + 54);
            sb2.append("Failed to load native library ");
            sb2.append(string2);
            sb2.append(" from remote package:\n  ");
            Log.e("DynamiteClient", sb2.toString(), e);
            return 0L;
        }
    }
}
