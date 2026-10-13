package com.google.vr.dynamite.client;

import android.os.IInterface;
import android.os.RemoteException;

/* JADX INFO: loaded from: classes.dex */
public interface INativeLibraryLoader extends IInterface {
    int checkVersion(String str) throws RemoteException;

    long initializeAndLoadNativeLibrary(String str) throws RemoteException;
}
