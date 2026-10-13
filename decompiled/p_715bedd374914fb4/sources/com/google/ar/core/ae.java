package com.google.ar.core;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import com.google.ar.core.exceptions.FatalException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: compiled from: InstallServiceImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class ae extends com.google.ar.core.dependencies.i {
    final /* synthetic */ AtomicBoolean a;
    final /* synthetic */ Activity b;
    final /* synthetic */ ag c;
    final /* synthetic */ w d;

    ae(ag agVar, AtomicBoolean atomicBoolean, w wVar, Activity activity) {
        this.a = atomicBoolean;
        this.d = wVar;
        this.b = activity;
        Objects.requireNonNull(agVar);
        this.c = agVar;
    }

    @Override // com.google.ar.core.dependencies.j
    public final void b(Bundle bundle) throws RemoteException {
        if (this.a.getAndSet(true)) {
        }
        int i = bundle.getInt("error.code", -100);
        int i2 = bundle.getInt("install.status", 0);
        if (i2 == 4) {
            this.d.a(x.COMPLETED);
            return;
        }
        if (i != 0) {
            StringBuilder sb = new StringBuilder(String.valueOf(i).length() + 40);
            sb.append("requestInstall = ");
            sb.append(i);
            sb.append(", launching fullscreen.");
            Log.w("ARCore-InstallService", sb.toString());
            ag agVar = this.c;
            agVar.c.i(this.b, this.d);
            return;
        }
        if (bundle.containsKey("resolution.intent")) {
            ag agVar2 = this.c;
            agVar2.c.j(this.b, bundle, this.d);
            return;
        }
        if (i2 == 10) {
            this.d.b(new FatalException("Unexpected REQUIRES_UI_INTENT install status without an intent."));
            return;
        }
        switch (i2) {
            case 1:
            case 2:
            case 3:
                this.d.a(x.ACCEPTED);
                break;
            case 4:
                this.d.a(x.COMPLETED);
                break;
            case 5:
                this.d.b(new FatalException("Unexpected FAILED install status without error."));
                break;
            case 6:
                this.d.a(x.CANCELLED);
                break;
            default:
                this.d.b(new FatalException(q.b((byte) 27, i2, "Unexpected install status: ")));
                break;
        }
    }

    @Override // com.google.ar.core.dependencies.j
    public final void c(Bundle bundle) throws RemoteException {
    }
}
