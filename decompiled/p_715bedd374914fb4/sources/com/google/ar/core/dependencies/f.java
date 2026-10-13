package com.google.ar.core.dependencies;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

/* JADX INFO: compiled from: IInstallService.java */
/* JADX INFO: loaded from: classes.dex */
public final class f extends c implements h {
    f(IBinder iBinder) {
        super(iBinder, "com.google.android.play.core.install.protocol.IInstallService");
    }

    @Override // com.google.ar.core.dependencies.h
    public final void d(String str, List list, Bundle bundle, j jVar) throws RemoteException {
        Parcel parcelA = a();
        parcelA.writeString(str);
        parcelA.writeTypedList(list);
        e.b(parcelA, bundle);
        e.c(parcelA, jVar);
        c(1, parcelA);
    }

    @Override // com.google.ar.core.dependencies.h
    public final void e(String str, Bundle bundle, j jVar) throws RemoteException {
        Parcel parcelA = a();
        parcelA.writeString(str);
        e.b(parcelA, bundle);
        e.c(parcelA, jVar);
        c(2, parcelA);
    }
}
