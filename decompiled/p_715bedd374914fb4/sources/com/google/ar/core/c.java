package com.google.ar.core;

/* JADX INFO: compiled from: ArCoreApk.java */
/* JADX INFO: loaded from: classes.dex */
final enum c extends ArCoreApk.Availability {
    /* JADX WARN: Illegal instructions before constructor call */
    /* synthetic */ c() {
        int i = 2;
        super("UNKNOWN_TIMED_OUT", i, i, null);
    }

    @Override // com.google.ar.core.ArCoreApk.Availability
    public final boolean isUnknown() {
        return true;
    }
}
