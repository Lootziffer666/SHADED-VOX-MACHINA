package com.google.ar.core;

import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.util.Objects;

/* JADX INFO: compiled from: InstallActivity.java */
/* JADX INFO: loaded from: classes.dex */
final class w {
    boolean a;
    final /* synthetic */ InstallActivity b;

    w(InstallActivity installActivity) {
        Objects.requireNonNull(installActivity);
        this.b = installActivity;
        this.a = false;
    }

    public final void b(Exception exc) {
        InstallActivity installActivity = this.b;
        synchronized (installActivity) {
            if (this.a) {
                return;
            }
            this.a = true;
            installActivity.f(x.CANCELLED);
            installActivity.c(exc);
        }
    }

    public final void a(x xVar) {
        InstallActivity installActivity = this.b;
        synchronized (installActivity) {
            if (this.a) {
                return;
            }
            installActivity.f(xVar);
            ArCoreApk.UserMessageType userMessageType = ArCoreApk.UserMessageType.APPLICATION;
            ArCoreApk.Availability availability = ArCoreApk.Availability.UNKNOWN_ERROR;
            int iOrdinal = xVar.ordinal();
            if (iOrdinal != 0) {
                if (iOrdinal == 1) {
                    installActivity.c(new UnavailableUserDeclinedInstallationException());
                } else if (iOrdinal == 2) {
                    if (!installActivity.g() && l.a().b) {
                        installActivity.e();
                    }
                    installActivity.c(null);
                }
                this.a = true;
            }
        }
    }
}
