package com.google.ar.core;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: compiled from: InstallServiceImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class ag implements Runnable {
    final /* synthetic */ Activity a;
    final /* synthetic */ w b;
    final /* synthetic */ y c;

    ag(y yVar, Activity activity, w wVar) {
        this.a = activity;
        this.b = wVar;
        Objects.requireNonNull(yVar);
        this.c = yVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        try {
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            com.google.ar.core.dependencies.h hVarF = this.c.f();
            Activity activity = this.a;
            String str = activity.getApplicationInfo().packageName;
            List listSingletonList = Collections.singletonList(y.k());
            Bundle bundle = new Bundle();
            w wVar = this.b;
            hVarF.d(str, listSingletonList, bundle, new ae(this, atomicBoolean, wVar, activity));
            new Handler().postDelayed(new af(this, atomicBoolean, activity, wVar), 3000L);
        } catch (RemoteException e) {
            Log.w("ARCore-InstallService", "requestInstall threw, launching fullscreen.", e);
            this.c.i(this.a, this.b);
        }
    }
}
