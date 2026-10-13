package com.google.ar.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.util.Objects;

/* JADX INFO: compiled from: InstallServiceImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class ad extends BroadcastReceiver {
    final /* synthetic */ w a;

    ad(y yVar, w wVar) {
        this.a = wVar;
        Objects.requireNonNull(yVar);
    }

    @Override // android.content.BroadcastReceiver
    public final void onReceive(Context context, Intent intent) {
        boolean zEquals = "com.google.android.play.core.install.ACTION_INSTALL_STATUS".equals(intent.getAction());
        Bundle extras = intent.getExtras();
        if (zEquals && extras != null && extras.containsKey("install.status")) {
            int i = extras.getInt("install.status");
            if (i == 1 || i == 2 || i == 3) {
                this.a.a(x.ACCEPTED);
            } else if (i == 4) {
                this.a.a(x.COMPLETED);
            } else {
                if (i != 6) {
                    return;
                }
                this.a.a(x.CANCELLED);
            }
        }
    }
}
