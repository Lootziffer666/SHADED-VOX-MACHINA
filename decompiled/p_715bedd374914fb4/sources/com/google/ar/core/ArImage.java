package com.google.ar.core;

import android.graphics.Rect;
import android.media.Image;
import com.google.ar.core.exceptions.FatalException;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes.dex */
class ArImage extends com.google.ar.core.dependencies.b {
    long a;
    private final Session b;
    private final long c;

    ArImage(Session session, long j) {
        this.b = session;
        this.a = j;
        this.c = session.nativeSymbolTableHandle;
    }

    private native void nativeClose(long j, long j2);

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: nativeGetBuffer, reason: merged with bridge method [inline-methods] */
    public native ByteBuffer c(long j, long j2, int i);

    private native int nativeGetFormat(long j, long j2);

    private native int nativeGetHeight(long j, long j2);

    private native int nativeGetNumberOfPlanes(long j, long j2);

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: nativeGetPixelStride, reason: merged with bridge method [inline-methods] */
    public native int b(long j, long j2, int i);

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: nativeGetRowStride, reason: merged with bridge method [inline-methods] */
    public native int a(long j, long j2, int i);

    private native long nativeGetTimestamp(long j, long j2);

    private native int nativeGetWidth(long j, long j2);

    static native void nativeLoadSymbols();

    @Override // android.media.Image, java.lang.AutoCloseable
    public final void close() {
        nativeClose(this.c, this.a);
        this.a = 0L;
    }

    final /* synthetic */ Session d() {
        return this.b;
    }

    @Override // android.media.Image
    public final Rect getCropRect() {
        return new Rect(0, 0, getWidth(), getHeight());
    }

    @Override // android.media.Image
    public final int getFormat() {
        int iNativeGetFormat = nativeGetFormat(this.b.nativeWrapperHandle, this.a);
        if (iNativeGetFormat != -1) {
            return iNativeGetFormat;
        }
        throw new FatalException("Unknown error in ArImage.getFormat().");
    }

    @Override // android.media.Image
    public final int getHeight() {
        int iNativeGetHeight = nativeGetHeight(this.b.nativeWrapperHandle, this.a);
        if (iNativeGetHeight != -1) {
            return iNativeGetHeight;
        }
        throw new FatalException("Unknown error in ArImage.getHeight().");
    }

    @Override // android.media.Image
    public final Image.Plane[] getPlanes() {
        int iNativeGetNumberOfPlanes = nativeGetNumberOfPlanes(this.b.nativeWrapperHandle, this.a);
        if (iNativeGetNumberOfPlanes == -1) {
            throw new FatalException("Unknown error in ArImage.getPlanes().");
        }
        o[] oVarArr = new o[iNativeGetNumberOfPlanes];
        for (int i = 0; i < iNativeGetNumberOfPlanes; i++) {
            oVarArr[i] = new o(this, this.a, i);
        }
        return oVarArr;
    }

    @Override // android.media.Image
    public final long getTimestamp() {
        long jNativeGetTimestamp = nativeGetTimestamp(this.b.nativeWrapperHandle, this.a);
        if (jNativeGetTimestamp != -1) {
            return jNativeGetTimestamp;
        }
        throw new FatalException("Unknown error in ArImage.getTimestamp().");
    }

    @Override // android.media.Image
    public final int getWidth() {
        int iNativeGetWidth = nativeGetWidth(this.b.nativeWrapperHandle, this.a);
        if (iNativeGetWidth != -1) {
            return iNativeGetWidth;
        }
        throw new FatalException("Unknown error in ArImage.getWidth().");
    }

    @Override // android.media.Image
    public final void setCropRect(Rect rect) {
        throw new UnsupportedOperationException("This is a read-only image.");
    }

    @Override // android.media.Image
    public final void setTimestamp(long j) {
        throw new UnsupportedOperationException("This is a read-only image.");
    }
}
