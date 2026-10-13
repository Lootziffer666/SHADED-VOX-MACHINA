package com.google.ar.core;

import android.app.Activity;
import android.util.Log;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: compiled from: InstallServiceImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class af implements Runnable {
    final /* synthetic */ AtomicBoolean a;
    final /* synthetic */ Activity b;
    final /* synthetic */ ag c;
    final /* synthetic */ w d;

    af(ag agVar, AtomicBoolean atomicBoolean, Activity activity, w wVar) {
        this.a = atomicBoolean;
        this.b = activity;
        this.d = wVar;
        Objects.requireNonNull(agVar);
        this.c = agVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        if (this.a.getAndSet(true)) {
            return;
        }
        Log.w("ARCore-InstallService", "requestInstall timed out, launching fullscreen.");
        ag agVar = this.c;
        agVar.c.i(this.b, this.d);
    }
}
