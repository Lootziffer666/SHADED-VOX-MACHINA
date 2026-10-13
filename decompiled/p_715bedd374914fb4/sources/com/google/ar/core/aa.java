package com.google.ar.core;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import java.util.Objects;

/* JADX INFO: compiled from: InstallServiceImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class aa extends com.google.ar.core.dependencies.i {
    final /* synthetic */ h a;

    aa(ab abVar, h hVar) {
        this.a = hVar;
        Objects.requireNonNull(abVar);
    }

    @Override // com.google.ar.core.dependencies.j
    public final void b(Bundle bundle) throws RemoteException {
    }

    @Override // com.google.ar.core.dependencies.j
    public final void c(Bundle bundle) throws RemoteException {
        int i = bundle.getInt("error.code", -100);
        if (i == -5) {
            Log.e("ARCore-InstallService", "The device is not supported.");
            this.a.a(ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE);
        } else if (i == -3) {
            Log.e("ARCore-InstallService", "The Google Play application must be updated.");
            this.a.a(ArCoreApk.Availability.UNKNOWN_ERROR);
        } else if (i == 0) {
            this.a.a(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED);
        } else {
            Log.e("ARCore-InstallService", q.b((byte) 22, i, "requestInfo returned: "));
            this.a.a(ArCoreApk.Availability.UNKNOWN_ERROR);
        }
    }
}
