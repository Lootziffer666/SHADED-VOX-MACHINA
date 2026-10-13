package com.google.ar.core;

import java.util.Objects;

/* JADX INFO: compiled from: ArCoreApkImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class i implements h {
    final /* synthetic */ l a;

    i(l lVar) {
        Objects.requireNonNull(lVar);
        this.a = lVar;
    }

    @Override // com.google.ar.core.h
    public final void a(ArCoreApk.Availability availability) {
        l lVar = this.a;
        synchronized (lVar) {
            lVar.f(availability);
            lVar.g();
        }
    }
}
