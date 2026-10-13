package com.google.ar.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/* JADX INFO: compiled from: InstallActivity.java */
/* JADX INFO: loaded from: classes.dex */
final class s implements h {
    final /* synthetic */ AtomicReference a;

    s(InstallActivity installActivity, AtomicReference atomicReference) {
        this.a = atomicReference;
        Objects.requireNonNull(installActivity);
    }

    @Override // com.google.ar.core.h
    public final void a(ArCoreApk.Availability availability) {
        this.a.set(availability);
    }
}
