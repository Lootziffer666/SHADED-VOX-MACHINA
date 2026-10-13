package com.google.ar.core;

/* JADX INFO: compiled from: ArCoreApk.java */
/* JADX INFO: loaded from: classes.dex */
final enum f extends ArCoreApk.Availability {
    /* synthetic */ f() {
        super("SUPPORTED_APK_TOO_OLD", 5, 202, null);
    }

    @Override // com.google.ar.core.ArCoreApk.Availability
    public final boolean isSupported() {
        return true;
    }
}
