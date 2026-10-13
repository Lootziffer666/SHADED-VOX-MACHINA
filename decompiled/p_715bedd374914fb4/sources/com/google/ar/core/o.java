package com.google.ar.core;

import com.google.ar.core.exceptions.FatalException;
import java.nio.ByteBuffer;
import java.util.Objects;

/* JADX INFO: compiled from: ArImage.java */
/* JADX INFO: loaded from: classes.dex */
final class o extends com.google.ar.core.dependencies.a {
    final /* synthetic */ ArImage a;
    private final long b;
    private final int c;

    public o(ArImage arImage, long j, int i) {
        Objects.requireNonNull(arImage);
        this.a = arImage;
        this.b = j;
        this.c = i;
    }

    @Override // android.media.Image.Plane
    public final ByteBuffer getBuffer() {
        ArImage arImage = this.a;
        return arImage.c(arImage.d().nativeWrapperHandle, this.b, this.c).asReadOnlyBuffer();
    }

    @Override // android.media.Image.Plane
    public final int getPixelStride() {
        ArImage arImage = this.a;
        int iB = arImage.b(arImage.d().nativeWrapperHandle, this.b, this.c);
        if (iB != -1) {
            return iB;
        }
        throw new FatalException("Unknown error in ArImage.Plane.getPixelStride().");
    }

    @Override // android.media.Image.Plane
    public final int getRowStride() {
        ArImage arImage = this.a;
        int iA = arImage.a(arImage.d().nativeWrapperHandle, this.b, this.c);
        if (iA != -1) {
            return iA;
        }
        throw new FatalException("Unknown error in ArImage.Plane.getRowStride().");
    }
}
