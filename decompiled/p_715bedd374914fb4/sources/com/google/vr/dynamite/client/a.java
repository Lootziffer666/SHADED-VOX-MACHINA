package com.google.vr.dynamite.client;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: compiled from: ILoadedInstanceCreator.java */
/* JADX INFO: loaded from: classes.dex */
public final class a extends com.google.ar.core.dependencies.c implements ILoadedInstanceCreator {
    a(IBinder iBinder) {
        super(iBinder, "com.google.vr.dynamite.client.ILoadedInstanceCreator");
    }

    @Override // com.google.vr.dynamite.client.ILoadedInstanceCreator
    public final INativeLibraryLoader newNativeLibraryLoader(IObjectWrapper iObjectWrapper, IObjectWrapper iObjectWrapper2) throws RemoteException {
        INativeLibraryLoader bVar;
        Parcel parcelA = a();
        com.google.ar.core.dependencies.e.c(parcelA, iObjectWrapper);
        com.google.ar.core.dependencies.e.c(parcelA, iObjectWrapper2);
        Parcel parcelB = b(1, parcelA);
        IBinder strongBinder = parcelB.readStrongBinder();
        if (strongBinder == null) {
            bVar = null;
        } else {
            IInterface iInterfaceQueryLocalInterface = strongBinder.queryLocalInterface("com.google.vr.dynamite.client.INativeLibraryLoader");
            bVar = iInterfaceQueryLocalInterface instanceof INativeLibraryLoader ? (INativeLibraryLoader) iInterfaceQueryLocalInterface : new b(strongBinder);
        }
        parcelB.recycle();
        return bVar;
    }
}
