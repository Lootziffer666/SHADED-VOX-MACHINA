package com.google.ar.core.dependencies;

import android.os.IBinder;
import android.os.IInterface;

/* JADX INFO: compiled from: IInstallService.java */
/* JADX INFO: loaded from: classes.dex */
public abstract class g extends d implements h {
    public static h b(IBinder iBinder) {
        if (iBinder == null) {
            return null;
        }
        IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface("com.google.android.play.core.install.protocol.IInstallService");
        return iInterfaceQueryLocalInterface instanceof h ? (h) iInterfaceQueryLocalInterface : new f(iBinder);
    }
}
