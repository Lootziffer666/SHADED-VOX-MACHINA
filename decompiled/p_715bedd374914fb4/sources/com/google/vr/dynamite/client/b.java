package com.google.vr.dynamite.client;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/* JADX INFO: compiled from: INativeLibraryLoader.java */
/* JADX INFO: loaded from: classes.dex */
public final class b extends com.google.ar.core.dependencies.c implements INativeLibraryLoader {
    b(IBinder iBinder) {
        super(iBinder, "com.google.vr.dynamite.client.INativeLibraryLoader");
    }

    @Override // com.google.vr.dynamite.client.INativeLibraryLoader
    public final int checkVersion(String str) throws RemoteException {
        Parcel parcelA = a();
        parcelA.writeString(str);
        Parcel parcelB = b(2, parcelA);
        int i = parcelB.readInt();
        parcelB.recycle();
        return i;
    }

    @Override // com.google.vr.dynamite.client.INativeLibraryLoader
    public final long initializeAndLoadNativeLibrary(String str) throws RemoteException {
        Parcel parcelA = a();
        parcelA.writeString(str);
        Parcel parcelB = b(1, parcelA);
        long j = parcelB.readLong();
        parcelB.recycle();
        return j;
    }
}
