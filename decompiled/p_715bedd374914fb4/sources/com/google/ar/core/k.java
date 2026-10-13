package com.google.ar.core;

import java.util.Objects;
import java.util.function.Consumer;

/* JADX INFO: compiled from: ArCoreApkImpl.java */
/* JADX INFO: loaded from: classes.dex */
final class k implements h {
    final /* synthetic */ Consumer a;
    final /* synthetic */ l b;

    k(l lVar, Consumer consumer) {
        this.a = consumer;
        Objects.requireNonNull(lVar);
        this.b = lVar;
    }

    @Override // com.google.ar.core.h
    public final void a(final ArCoreApk.Availability availability) {
        final Consumer consumer = this.a;
        this.b.e().post(new Runnable() { // from class: com.google.ar.core.j
            @Override // java.lang.Runnable
            public final /* synthetic */ void run() {
                consumer.accept(availability);
            }
        });
    }
}
