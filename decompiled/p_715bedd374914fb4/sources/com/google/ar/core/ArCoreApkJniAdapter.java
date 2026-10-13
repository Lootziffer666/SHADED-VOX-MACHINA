package com.google.ar.core;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.ar.core.exceptions.ResourceExhaustedException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/* JADX INFO: loaded from: classes.dex */
final class ArCoreApkJniAdapter {
    public static final /* synthetic */ int a = 0;
    private static final Map b;

    static {
        HashMap map = new HashMap();
        b = map;
        map.put(IllegalArgumentException.class, Integer.valueOf(ai.ERROR_INVALID_ARGUMENT.G));
        map.put(ResourceExhaustedException.class, Integer.valueOf(ai.ERROR_RESOURCE_EXHAUSTED.G));
        map.put(UnavailableArcoreNotInstalledException.class, Integer.valueOf(ai.UNAVAILABLE_ARCORE_NOT_INSTALLED.G));
        map.put(UnavailableDeviceNotCompatibleException.class, Integer.valueOf(ai.UNAVAILABLE_DEVICE_NOT_COMPATIBLE.G));
        map.put(UnavailableApkTooOldException.class, Integer.valueOf(ai.UNAVAILABLE_APK_TOO_OLD.G));
        map.put(UnavailableSdkTooOldException.class, Integer.valueOf(ai.UNAVAILABLE_SDK_TOO_OLD.G));
        map.put(UnavailableUserDeclinedInstallationException.class, Integer.valueOf(ai.UNAVAILABLE_USER_DECLINED_INSTALLATION.G));
    }

    private ArCoreApkJniAdapter() {
    }

    private static int b(Throwable th) {
        Log.e("ARCore-ArCoreApkJniAdap", "Exception details:", th);
        Map map = b;
        Class<?> cls = th.getClass();
        return map.containsKey(cls) ? ((Integer) map.get(cls)).intValue() : ai.ERROR_FATAL.G;
    }

    static int checkAvailability(Context context) {
        try {
            return ArCoreApk.getInstance().checkAvailability(context).nativeCode;
        } catch (Throwable th) {
            b(th);
            return ArCoreApk.Availability.UNKNOWN_ERROR.nativeCode;
        }
    }

    static void checkAvailabilityAsync(Context context, final long j, final long j2) {
        final Consumer<ArCoreApk.Availability> consumer = new Consumer() { // from class: com.google.ar.core.n
            @Override // java.util.function.Consumer
            public final /* synthetic */ void accept(Object obj) {
                ArCoreApkJniAdapter.nativeInvokeAvailabilityCallback(j, j2, ((ArCoreApk.Availability) obj).nativeCode);
            }
        };
        try {
            ArCoreApk.getInstance().checkAvailabilityAsync(context, consumer);
        } catch (Throwable th) {
            b(th);
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.google.ar.core.m
                @Override // java.lang.Runnable
                public final /* synthetic */ void run() {
                    int i = ArCoreApkJniAdapter.a;
                    consumer.accept(ArCoreApk.Availability.UNKNOWN_ERROR);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static native void nativeInvokeAvailabilityCallback(long j, long j2, int i);

    static int requestInstall(Activity activity, boolean z, int[] iArr) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        try {
            iArr[0] = ArCoreApk.getInstance().requestInstall(activity, z).nativeCode;
            return ai.SUCCESS.G;
        } catch (Throwable th) {
            return b(th);
        }
    }

    static int requestInstallCustom(Activity activity, boolean z, int i, int i2, int[] iArr) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        try {
            iArr[0] = ArCoreApk.getInstance().requestInstall(activity, z, ArCoreApk.InstallBehavior.forNumber(i), ArCoreApk.UserMessageType.forNumber(i2)).nativeCode;
            return ai.SUCCESS.G;
        } catch (Throwable th) {
            return b(th);
        }
    }
}
