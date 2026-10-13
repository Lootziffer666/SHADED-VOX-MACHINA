package com.google.ar.core;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import java.util.Objects;

/* JADX INFO: compiled from: InstallServiceImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class ab implements Runnable {
    final /* synthetic */ Context a;
    final /* synthetic */ h b;
    final /* synthetic */ y c;

    ab(y yVar, Context context, h hVar) {
        this.a = context;
        this.b = hVar;
        Objects.requireNonNull(yVar);
        this.c = yVar;
    }

    @Override // java.lang.Runnable
    public final void run() {
        try {
            this.c.f().e(this.a.getApplicationInfo().packageName, y.k(), new aa(this, this.b));
        } catch (RemoteException e) {
            Log.e("ARCore-InstallService", "requestInfo threw", e);
            this.b.a(ArCoreApk.Availability.UNKNOWN_ERROR);
        }
    }
}
