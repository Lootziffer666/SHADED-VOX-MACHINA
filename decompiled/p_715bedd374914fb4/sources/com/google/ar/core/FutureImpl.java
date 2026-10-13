package com.google.ar.core;

/* JADX INFO: loaded from: classes.dex */
class FutureImpl implements Future {
    final long nativeCallbackContext;
    final long nativeFuture;
    final long nativeSymbolTableHandle;
    final Session session;

    FutureImpl(Session session, long j, long j2) {
        this.session = session;
        this.nativeSymbolTableHandle = session.nativeSymbolTableHandle;
        this.nativeFuture = j;
        this.nativeCallbackContext = j2;
    }

    static native void nativeReleaseFuture(long j, long j2);

    @Override // com.google.ar.core.Future
    public boolean cancel() {
        return nativeCancel(this.session.nativeWrapperHandle, this.nativeFuture, this.nativeCallbackContext);
    }

    protected void finalize() throws Throwable {
        nativeReleaseFuture(this.nativeSymbolTableHandle, this.nativeFuture);
        super.finalize();
    }

    @Override // com.google.ar.core.Future
    public FutureState getState() {
        return FutureState.forNumber(nativeGetState(this.session.nativeWrapperHandle, this.nativeFuture));
    }

    native boolean nativeCancel(long j, long j2, long j3);

    native int nativeGetState(long j, long j2);
}
