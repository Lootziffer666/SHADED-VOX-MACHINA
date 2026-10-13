package com.google.ar.core;

import android.content.pm.PackageInstaller;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/* JADX INFO: compiled from: InstallServiceImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class ac extends PackageInstaller.SessionCallback {
    final Map a;
    final /* synthetic */ w b;
    final /* synthetic */ y c;

    ac(y yVar, w wVar) {
        this.b = wVar;
        Objects.requireNonNull(yVar);
        this.c = yVar;
        this.a = new HashMap();
    }

    @Override // android.content.pm.PackageInstaller.SessionCallback
    public final void onActiveChanged(int i, boolean z) {
    }

    @Override // android.content.pm.PackageInstaller.SessionCallback
    public final void onBadgingChanged(int i) {
    }

    @Override // android.content.pm.PackageInstaller.SessionCallback
    public final void onCreated(int i) {
        this.a.put(Integer.valueOf(i), this.c.g().getSessionInfo(i));
    }

    @Override // android.content.pm.PackageInstaller.SessionCallback
    public final void onFinished(int i, boolean z) {
        PackageInstaller.SessionInfo sessionInfo = (PackageInstaller.SessionInfo) this.a.remove(Integer.valueOf(i));
        if (sessionInfo == null || !"com.google.ar.core".equals(sessionInfo.getAppPackageName())) {
            return;
        }
        Log.i("ARCore-InstallService", "Detected ARCore install completion");
        this.b.a(x.COMPLETED);
    }

    @Override // android.content.pm.PackageInstaller.SessionCallback
    public final void onProgressChanged(int i, float f) {
    }
}
