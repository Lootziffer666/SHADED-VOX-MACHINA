package com.google.ar.core;

import android.view.View;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.util.Objects;

/* JADX INFO: compiled from: InstallActivity.java */
/* JADX INFO: loaded from: classes.dex */
final class t implements View.OnClickListener {
    final /* synthetic */ InstallActivity a;
    private final /* synthetic */ int b;

    t(InstallActivity installActivity, int i, byte[] bArr) {
        this.b = i;
        Objects.requireNonNull(installActivity);
        this.a = installActivity;
    }

    t(InstallActivity installActivity, int i) {
        this.b = i;
        Objects.requireNonNull(installActivity);
        this.a = installActivity;
    }

    @Override // android.view.View.OnClickListener
    public final void onClick(View view) {
        if (this.b != 0) {
            this.a.c(new UnavailableUserDeclinedInstallationException());
        } else {
            InstallActivity installActivity = this.a;
            installActivity.b();
            installActivity.d();
        }
    }
}
