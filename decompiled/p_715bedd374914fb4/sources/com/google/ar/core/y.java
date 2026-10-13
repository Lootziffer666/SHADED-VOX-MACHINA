package com.google.ar.core;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageInstaller;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.google.ar.core.exceptions.FatalException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/* JADX INFO: compiled from: InstallService.java */
/* JADX INFO: loaded from: classes.dex */
final class y {
    private final Queue a;
    private Context b;
    private com.google.ar.core.dependencies.h c;
    private final ServiceConnection d;
    private BroadcastReceiver e;
    private Context f;
    private PackageInstaller g;
    private PackageInstaller.SessionCallback h;
    private volatile int i;

    y() {
    }

    y(byte[] bArr) {
        this();
        this.a = new ArrayDeque();
        this.i = 1;
        this.d = new z(this);
    }

    static /* synthetic */ Bundle k() {
        Bundle bundle = new Bundle();
        bundle.putCharSequence("package.name", "com.google.ar.core");
        return bundle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: l, reason: merged with bridge method [inline-methods] */
    public final synchronized void d(IBinder iBinder) {
        com.google.ar.core.dependencies.h hVarB = com.google.ar.core.dependencies.g.b(iBinder);
        Log.i("ARCore-InstallService", "Install service connected");
        this.c = hVarB;
        this.i = 3;
        Iterator it = this.a.iterator();
        while (it.hasNext()) {
            ((Runnable) it.next()).run();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: m, reason: merged with bridge method [inline-methods] */
    public final synchronized void e() {
        Log.i("ARCore-InstallService", "Install service disconnected");
        this.i = 1;
        this.c = null;
    }

    private static void o(Activity activity, w wVar) {
        boolean z;
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.google.ar.core"));
            l lVarA = l.a();
            for (ResolveInfo resolveInfo : activity.getPackageManager().queryIntentActivities(intent, 65536)) {
                if (resolveInfo.activityInfo != null && resolveInfo.activityInfo.name.equals("com.sec.android.app.samsungapps.MainForChina")) {
                    z = true;
                    lVarA.b = !z;
                    activity.startActivity(intent);
                }
            }
            z = false;
            lVarA.b = !z;
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            wVar.b(new FatalException("Failed to launch installer.", e));
        }
    }

    private static void p(Activity activity, Bundle bundle, w wVar) {
        PendingIntent pendingIntent = (PendingIntent) bundle.getParcelable("resolution.intent");
        if (pendingIntent == null) {
            Log.e("ARCore-InstallService", "Did not get pending intent.");
            wVar.b(new FatalException("Installation intent failed to unparcel."));
        } else {
            try {
                activity.startIntentSenderForResult(pendingIntent.getIntentSender(), 1234, new Intent(activity, activity.getClass()), 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                wVar.b(new FatalException("Installation Intent failed", e));
            }
        }
    }

    public final synchronized void a(Context context) {
        this.b = context;
        Intent intent = new Intent("com.google.android.play.core.install.BIND_INSTALL_SERVICE").setPackage("com.android.vending");
        ServiceConnection serviceConnection = this.d;
        if (context.bindService(intent, serviceConnection, 1)) {
            this.i = 2;
            return;
        }
        this.i = 1;
        this.b = null;
        Log.w("ARCore-InstallService", "bindService returned false.");
        context.unbindService(serviceConnection);
    }

    public final synchronized void c(Context context, h hVar) {
        try {
            n(new ab(this, context, hVar));
        } catch (ah unused) {
            Log.e("ARCore-InstallService", "Play Store install service could not be bound.");
            hVar.a(ArCoreApk.Availability.UNKNOWN_ERROR);
        }
    }

    final /* synthetic */ com.google.ar.core.dependencies.h f() {
        return this.c;
    }

    final /* synthetic */ PackageInstaller g() {
        return this.g;
    }

    public final void h(Activity activity, w wVar) {
        if (this.h == null) {
            try {
                this.g = activity.getPackageManager().getPackageInstaller();
                ac acVar = new ac(this, wVar);
                this.h = acVar;
                this.g.registerSessionCallback(acVar);
            } catch (NullPointerException unused) {
                wVar.b(new FatalException("Unable to obtain Android PackageInstaller; is this a Play Instant App?"));
            }
        }
        if (this.e == null) {
            this.e = new ad(this, wVar);
            this.f = activity;
            if (Build.VERSION.SDK_INT >= 33) {
                this.f.registerReceiver(this.e, new IntentFilter("com.google.android.play.core.install.ACTION_INSTALL_STATUS"), 2);
            } else {
                this.f.registerReceiver(this.e, new IntentFilter("com.google.android.play.core.install.ACTION_INSTALL_STATUS"));
            }
        }
        try {
            n(new ag(this, activity, wVar));
        } catch (ah unused2) {
            Log.w("ARCore-InstallService", "requestInstall bind failed, launching fullscreen.");
            o(activity, wVar);
        }
    }

    final /* synthetic */ void i(Activity activity, w wVar) {
        o(activity, wVar);
    }

    final /* synthetic */ void j(Activity activity, Bundle bundle, w wVar) {
        p(activity, bundle, wVar);
    }

    private final synchronized void n(Runnable runnable) throws ah {
        int i = this.i;
        int i2 = i - 1;
        if (i == 0) {
            throw null;
        }
        if (i2 == 0) {
            throw new ah();
        }
        if (i2 == 1) {
            this.a.offer(runnable);
        } else {
            if (i2 != 2) {
                return;
            }
            runnable.run();
        }
    }

    public final synchronized void b() {
        int i = this.i;
        int i2 = i - 1;
        if (i == 0) {
            throw null;
        }
        if (i2 == 1 || i2 == 2) {
            this.b.unbindService(this.d);
            this.b = null;
            this.i = 1;
        }
        BroadcastReceiver broadcastReceiver = this.e;
        if (broadcastReceiver != null) {
            this.f.unregisterReceiver(broadcastReceiver);
        }
        PackageInstaller.SessionCallback sessionCallback = this.h;
        if (sessionCallback != null) {
            this.g.unregisterSessionCallback(sessionCallback);
            this.h = null;
        }
    }
}
