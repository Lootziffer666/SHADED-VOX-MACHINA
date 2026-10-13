package com.google.ar.core;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Handler;
import java.util.Objects;

/* JADX INFO: compiled from: SharedCamera.java */
/* JADX INFO: loaded from: classes.dex */
final class ao extends CameraCaptureSession.StateCallback {
    public static final /* synthetic */ int d = 0;
    final /* synthetic */ Handler a;
    final /* synthetic */ CameraCaptureSession.StateCallback b;
    final /* synthetic */ SharedCamera c;

    ao(SharedCamera sharedCamera, Handler handler, CameraCaptureSession.StateCallback stateCallback) {
        this.a = handler;
        this.b = stateCallback;
        Objects.requireNonNull(sharedCamera);
        this.c = sharedCamera;
    }

    @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
    public final void onActive(CameraCaptureSession cameraCaptureSession) {
        this.a.post(new al(this.b, cameraCaptureSession, 6));
        this.c.g(cameraCaptureSession);
    }

    @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
    public final void onClosed(CameraCaptureSession cameraCaptureSession) {
        this.a.post(new al(this.b, cameraCaptureSession, 7));
        this.c.h(cameraCaptureSession);
    }

    @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
    public final void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
        this.a.post(new al(this.b, cameraCaptureSession, 4));
        this.c.i(cameraCaptureSession);
    }

    @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
    public final void onConfigured(CameraCaptureSession cameraCaptureSession) {
        this.a.post(new al(this.b, cameraCaptureSession, 3));
        SharedCamera sharedCamera = this.c;
        sharedCamera.j(cameraCaptureSession);
        if (sharedCamera.n().a() != null) {
            sharedCamera.c();
        }
    }

    @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
    public final void onReady(CameraCaptureSession cameraCaptureSession) {
        this.a.post(new al(this.b, cameraCaptureSession, 5));
        this.c.k(cameraCaptureSession);
    }
}
