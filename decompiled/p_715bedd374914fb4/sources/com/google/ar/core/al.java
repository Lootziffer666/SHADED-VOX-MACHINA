package com.google.ar.core;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;

/* JADX INFO: loaded from: classes.dex */
final /* synthetic */ class al implements Runnable {
    private final /* synthetic */ int a;
    private final /* synthetic */ Object b;
    private final /* synthetic */ Object c;

    /* synthetic */ al(Object obj, Object obj2, int i) {
        this.a = i;
        this.b = obj;
        this.c = obj2;
    }

    @Override // java.lang.Runnable
    public final /* synthetic */ void run() {
        switch (this.a) {
            case 0:
                int i = an.d;
                ((CameraDevice.StateCallback) this.b).onDisconnected((CameraDevice) this.c);
                break;
            case 1:
                int i2 = an.d;
                ((CameraDevice.StateCallback) this.b).onOpened((CameraDevice) this.c);
                break;
            case 2:
                int i3 = an.d;
                ((CameraDevice.StateCallback) this.b).onClosed((CameraDevice) this.c);
                break;
            case 3:
                int i4 = ao.d;
                ((CameraCaptureSession.StateCallback) this.b).onConfigured((CameraCaptureSession) this.c);
                break;
            case 4:
                int i5 = ao.d;
                ((CameraCaptureSession.StateCallback) this.b).onConfigureFailed((CameraCaptureSession) this.c);
                break;
            case 5:
                int i6 = ao.d;
                ((CameraCaptureSession.StateCallback) this.b).onReady((CameraCaptureSession) this.c);
                break;
            case 6:
                int i7 = ao.d;
                ((CameraCaptureSession.StateCallback) this.b).onActive((CameraCaptureSession) this.c);
                break;
            default:
                int i8 = ao.d;
                ((CameraCaptureSession.StateCallback) this.b).onClosed((CameraCaptureSession) this.c);
                break;
        }
    }
}
