package com.google.ar.core;

import android.media.ImageReader;

/* JADX INFO: loaded from: classes.dex */
final /* synthetic */ class ap implements ImageReader.OnImageAvailableListener {
    static final /* synthetic */ ap a = new ap();

    private /* synthetic */ ap() {
    }

    @Override // android.media.ImageReader.OnImageAvailableListener
    public final /* synthetic */ void onImageAvailable(ImageReader imageReader) {
        SharedCamera.lambda$setDummyOnImageAvailableListener$0(imageReader);
    }
}
