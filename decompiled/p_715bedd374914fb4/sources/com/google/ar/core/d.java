package com.google.ar.core;

/* JADX INFO: compiled from: ArCoreApk.java */
/* JADX INFO: loaded from: classes.dex */
final enum d extends ArCoreApk.Availability {
    /* synthetic */ d() {
        super("UNSUPPORTED_DEVICE_NOT_CAPABLE", 3, 100, null);
    }

    @Override // com.google.ar.core.ArCoreApk.Availability
    public final boolean isUnsupported() {
        return true;
    }
}
