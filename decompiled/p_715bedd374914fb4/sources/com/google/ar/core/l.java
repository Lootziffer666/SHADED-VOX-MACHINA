package com.google.ar.core;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import com.google.ar.core.exceptions.FatalException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.util.Objects;
import java.util.function.Consumer;

/* JADX INFO: compiled from: ArCoreApkImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class l extends ArCoreApk {
    private static final l d = new l();
    Exception a;
    private boolean e;
    private int f;
    private long g;
    private ArCoreApk.Availability h;
    private boolean i;
    private y j;
    private boolean k;
    private boolean l;
    private int m;
    private final Handler c = new Handler(Looper.getMainLooper());
    boolean b = true;

    l() {
    }

    public static l a() {
        return d;
    }

    private final void h(Context context, h hVar) {
        ArCoreApk.Availability availability;
        if (!j()) {
            hVar.a(ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE);
            return;
        }
        try {
            if (d(context)) {
                c();
                try {
                    availability = ak.a(context) != null ? ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD : ArCoreApk.Availability.SUPPORTED_INSTALLED;
                } catch (UnavailableDeviceNotCompatibleException unused) {
                    availability = ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE;
                } catch (UnavailableUserDeclinedInstallationException | RuntimeException unused2) {
                    availability = ArCoreApk.Availability.UNKNOWN_ERROR;
                }
                hVar.a(availability);
                return;
            }
            if (k(context) != -1) {
                hVar.a(ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD);
            } else if (i(context)) {
                hVar.a(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED);
            } else {
                b(context).c(context, hVar);
            }
        } catch (FatalException e) {
            Log.e("ARCore-ArCoreApk", "Error while checking app details and ARCore status", e);
            hVar.a(ArCoreApk.Availability.UNKNOWN_ERROR);
        }
    }

    private final boolean i(Context context) {
        l(context);
        return this.l;
    }

    private static boolean j() {
        return true;
    }

    private static int k(Context context) {
        if (SessionCreateJniHelper.useProjectedApk(context)) {
            return SessionCreateJniHelper.getMinApkVersion(context);
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo("com.google.ar.core", 4);
            int i = packageInfo.versionCode;
            if (i == 0) {
                return (packageInfo.services == null || packageInfo.services.length == 0) ? -1 : 0;
            }
            return i;
        } catch (PackageManager.NameNotFoundException unused) {
            return -1;
        }
    }

    private final synchronized void l(Context context) {
        if (this.k) {
            return;
        }
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        try {
            Bundle bundle = packageManager.getApplicationInfo(packageName, 128).metaData;
            if (!bundle.containsKey("com.google.ar.core")) {
                throw new FatalException("Application manifest must contain meta-data com.google.ar.core");
            }
            this.l = ((String) Objects.requireNonNull(bundle.getString("com.google.ar.core"))).equals("required");
            if (!bundle.containsKey("com.google.ar.core.min_apk_version")) {
                throw new FatalException("Application manifest must contain meta-data com.google.ar.core.min_apk_version");
            }
            this.m = bundle.getInt("com.google.ar.core.min_apk_version");
            try {
                ActivityInfo[] activityInfoArr = packageManager.getPackageInfo(packageName, 1).activities;
                String canonicalName = InstallActivity.class.getCanonicalName();
                for (ActivityInfo activityInfo : activityInfoArr) {
                    if (canonicalName.equals(activityInfo.name)) {
                        this.k = true;
                        return;
                    }
                }
                StringBuilder sb = new StringBuilder(String.valueOf(canonicalName).length() + 43);
                sb.append("Application manifest must contain activity ");
                sb.append(canonicalName);
                throw new FatalException(sb.toString());
            } catch (PackageManager.NameNotFoundException e) {
                throw new FatalException("Could not load application package info", e);
            }
        } catch (PackageManager.NameNotFoundException e2) {
            throw new FatalException("Could not load application package metadata", e2);
        }
    }

    private static final ArCoreApk.InstallStatus m(Activity activity) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        PendingIntent pendingIntentA = ak.a(activity);
        if (pendingIntentA != null) {
            try {
                Log.i("ARCore-ArCoreApk", "Starting setup activity");
                activity.startIntentSender(pendingIntentA.getIntentSender(), null, 0, 0, 0, Build.VERSION.SDK_INT > 33 ? ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(1).toBundle() : null);
                return ArCoreApk.InstallStatus.INSTALL_REQUESTED;
            } catch (IntentSender.SendIntentException | RuntimeException e) {
                Log.w("ARCore-ArCoreApk", "Setup activity launch failed", e);
            }
        }
        return ArCoreApk.InstallStatus.INSTALLED;
    }

    final synchronized y b(Context context) {
        if (this.j == null) {
            y yVar = new y(null);
            yVar.a(context.getApplicationContext());
            this.j = yVar;
        }
        return this.j;
    }

    final synchronized void c() {
        if (this.a == null) {
            this.f = 0;
        }
        this.e = false;
        y yVar = this.j;
        if (yVar != null) {
            yVar.b();
            this.j = null;
        }
    }

    @Override // com.google.ar.core.ArCoreApk
    public final ArCoreApk.Availability checkAvailability(Context context) {
        synchronized (this) {
            ArCoreApk.Availability availability = this.h;
            if ((availability == null || availability.isUnknown()) && !this.i) {
                this.i = true;
                h(context, new i(this));
            }
            ArCoreApk.Availability availability2 = this.h;
            if (availability2 != null) {
                if (!availability2.isUnsupported()) {
                    this.h = null;
                }
                return availability2;
            }
            if (this.i) {
                return ArCoreApk.Availability.UNKNOWN_CHECKING;
            }
            Log.e("ARCore-ArCoreApk", "request not running but result is null?");
            return ArCoreApk.Availability.UNKNOWN_ERROR;
        }
    }

    @Override // com.google.ar.core.ArCoreApk
    public final void checkAvailabilityAsync(Context context, Consumer<ArCoreApk.Availability> consumer) {
        h(context, new k(this, consumer));
    }

    final boolean d(Context context) {
        l(context);
        return k(context) == 0 || k(context) >= this.m;
    }

    final /* synthetic */ Handler e() {
        return this.c;
    }

    final /* synthetic */ void f(ArCoreApk.Availability availability) {
        this.h = availability;
    }

    final /* synthetic */ void g() {
        this.i = false;
    }

    @Override // com.google.ar.core.ArCoreApk
    public final ArCoreApk.InstallStatus requestInstall(Activity activity, boolean z) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        return requestInstall(activity, z, i(activity) ? ArCoreApk.InstallBehavior.REQUIRED : ArCoreApk.InstallBehavior.OPTIONAL, i(activity) ? ArCoreApk.UserMessageType.APPLICATION : ArCoreApk.UserMessageType.USER_ALREADY_INFORMED);
    }

    @Override // com.google.ar.core.ArCoreApk
    public final ArCoreApk.InstallStatus requestInstall(Activity activity, boolean z, ArCoreApk.InstallBehavior installBehavior, ArCoreApk.UserMessageType userMessageType) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        if (!j()) {
            throw new UnavailableDeviceNotCompatibleException();
        }
        if (d(activity)) {
            c();
            return m(activity);
        }
        if (!this.e) {
            Exception exc = this.a;
            if (exc != null) {
                if (z) {
                    Log.w("ARCore-ArCoreApk", "Clearing previous failure: ", exc);
                    this.a = null;
                } else {
                    if (!(exc instanceof UnavailableDeviceNotCompatibleException)) {
                        if (exc instanceof UnavailableUserDeclinedInstallationException) {
                            Log.e("ARCore-ArCoreApk", "Throwing UnavailableUserDeclinedInstallationException");
                            throw ((UnavailableUserDeclinedInstallationException) exc);
                        }
                        if (exc instanceof RuntimeException) {
                            Log.e("ARCore-ArCoreApk", "Throwing RuntimeException.");
                            throw ((RuntimeException) exc);
                        }
                        throw new RuntimeException("Unexpected exception type", exc);
                    }
                    Log.e("ARCore-ArCoreApk", "Throwing UnavailableDeviceNotCompatibleException");
                    throw ((UnavailableDeviceNotCompatibleException) exc);
                }
            }
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis - this.g > 5000) {
                this.f = 0;
            }
            int i = this.f + 1;
            this.f = i;
            this.g = jUptimeMillis;
            if (i <= 2) {
                try {
                    activity.startActivity(new Intent(activity, (Class<?>) InstallActivity.class).putExtra("message", userMessageType).putExtra("behavior", installBehavior));
                    this.e = true;
                    return ArCoreApk.InstallStatus.INSTALL_REQUESTED;
                } catch (ActivityNotFoundException e) {
                    throw new FatalException("Failed to launch InstallActivity.", e);
                }
            }
            throw new FatalException("Requesting ARCore installation too rapidly.");
        }
        return ArCoreApk.InstallStatus.INSTALL_REQUESTED;
    }
}
