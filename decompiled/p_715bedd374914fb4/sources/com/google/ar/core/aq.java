package com.google.ar.core;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.view.Surface;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: compiled from: SharedCamera.java */
/* JADX INFO: loaded from: classes.dex */
final class aq {
    private CameraDevice a = null;
    private final Map b = new HashMap();
    private SurfaceTexture c = null;
    private Surface d = null;

    private aq() {
    }

    public final CameraDevice a() {
        return this.a;
    }

    public final void b(CameraDevice cameraDevice) {
        this.a = cameraDevice;
    }

    public final void c(String str, List list) {
        this.b.put(str, list);
    }

    public final SurfaceTexture d() {
        return this.c;
    }

    public final void e(SurfaceTexture surfaceTexture) {
        this.c = surfaceTexture;
    }

    public final Surface f() {
        return this.d;
    }

    public final void g(Surface surface) {
        this.d = surface;
    }

    /* synthetic */ aq(byte[] bArr) {
    }
}
