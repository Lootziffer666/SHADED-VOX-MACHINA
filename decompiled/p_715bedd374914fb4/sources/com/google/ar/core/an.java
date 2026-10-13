package com.google.ar.core;

import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import java.util.Objects;

/* JADX INFO: compiled from: SharedCamera.java */
/* JADX INFO: loaded from: classes.dex */
final class an extends CameraDevice.StateCallback {
    public static final /* synthetic */ int d = 0;
    final /* synthetic */ Handler a;
    final /* synthetic */ CameraDevice.StateCallback b;
    final /* synthetic */ SharedCamera c;

    an(SharedCamera sharedCamera, Handler handler, CameraDevice.StateCallback stateCallback) {
        this.a = handler;
        this.b = stateCallback;
        Objects.requireNonNull(sharedCamera);
        this.c = sharedCamera;
    }

    @Override // android.hardware.camera2.CameraDevice.StateCallback
    public final void onClosed(CameraDevice cameraDevice) {
        this.a.post(new al(this.b, cameraDevice, 2));
        this.c.e(cameraDevice);
    }

    @Override // android.hardware.camera2.CameraDevice.StateCallback
    public final void onDisconnected(CameraDevice cameraDevice) {
        this.a.post(new al(this.b, cameraDevice, 0));
        this.c.f(cameraDevice);
    }

    @Override // android.hardware.camera2.CameraDevice.StateCallback
    public final void onError(final CameraDevice cameraDevice, final int i) {
        final CameraDevice.StateCallback stateCallback = this.b;
        this.a.post(new Runnable() { // from class: com.google.ar.core.am
            @Override // java.lang.Runnable
            public final /* synthetic */ void run() {
                int i2 = an.d;
                stateCallback.onError(cameraDevice, i);
            }
        });
        this.c.b();
    }

    @Override // android.hardware.camera2.CameraDevice.StateCallback
    public final void onOpened(CameraDevice cameraDevice) {
        SharedCamera sharedCamera = this.c;
        sharedCamera.n().b(cameraDevice);
        this.a.post(new al(this.b, cameraDevice, 1));
        sharedCamera.d(cameraDevice);
        sharedCamera.n().e(sharedCamera.l());
        sharedCamera.n().g(sharedCamera.m());
    }
}
