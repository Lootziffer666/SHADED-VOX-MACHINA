package com.google.ar.core.dependencies;

import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: compiled from: IInstallServiceCallback.java */
/* JADX INFO: loaded from: classes.dex */
public abstract class i extends d implements j {
    public i() {
        super("com.google.android.play.core.install.protocol.IInstallServiceCallback");
    }

    @Override // com.google.ar.core.dependencies.d
    protected final boolean a(int i, Parcel parcel) throws RemoteException {
        if (i == 1) {
            Bundle bundle = (Bundle) e.a(parcel, Bundle.CREATOR);
            e.d(parcel);
            b(bundle);
        } else if (i == 2) {
            Bundle bundle2 = (Bundle) e.a(parcel, Bundle.CREATOR);
            e.d(parcel);
            c(bundle2);
        } else {
            if (i != 3) {
                return false;
            }
            e.d(parcel);
        }
        return true;
    }
}
