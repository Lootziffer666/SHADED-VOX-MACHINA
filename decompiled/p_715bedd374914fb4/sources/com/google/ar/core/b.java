package com.google.ar.core;

/* JADX INFO: compiled from: ArCoreApk.java */
/* JADX INFO: loaded from: classes.dex */
final enum b extends ArCoreApk.Availability {
    /* JADX WARN: Illegal instructions before constructor call */
    /* synthetic */ b() {
        int i = 1;
        super("UNKNOWN_CHECKING", i, i, null);
    }

    @Override // com.google.ar.core.ArCoreApk.Availability
    public final boolean isTransient() {
        return true;
    }

    @Override // com.google.ar.core.ArCoreApk.Availability
    public final boolean isUnknown() {
        return true;
    }
}
