package com.google.ar.core.dependencies;

import android.os.Bundle;
import android.os.IInterface;
import android.os.RemoteException;
import java.util.List;

/* JADX INFO: compiled from: IInstallService.java */
/* JADX INFO: loaded from: classes.dex */
public interface h extends IInterface {
    void d(String str, List list, Bundle bundle, j jVar) throws RemoteException;

    void e(String str, Bundle bundle, j jVar) throws RemoteException;
}
