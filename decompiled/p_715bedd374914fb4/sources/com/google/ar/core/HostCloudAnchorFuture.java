package com.google.ar.core;

import java.util.function.BiConsumer;

/* JADX INFO: loaded from: classes.dex */
public class HostCloudAnchorFuture extends FutureImpl {

    static class CallbackWrapper {
        private final BiConsumer a;

        public CallbackWrapper(BiConsumer biConsumer) {
            this.a = biConsumer;
        }

        public void accept(String str, int i) {
            this.a.accept(str, Anchor.CloudAnchorState.forNumber(i));
        }
    }

    HostCloudAnchorFuture(Session session, long j, long j2) {
        super(session, j, j2);
    }

    @Override // com.google.ar.core.FutureImpl, com.google.ar.core.Future
    public /* bridge */ /* synthetic */ boolean cancel() {
        return super.cancel();
    }

    public String getResultCloudAnchorId() {
        return nativeGetResultCloudAnchorId(this.session.nativeWrapperHandle, this.nativeFuture);
    }

    public Anchor.CloudAnchorState getResultCloudAnchorState() {
        return Anchor.CloudAnchorState.forNumber(nativeGetResultCloudAnchorState(this.session.nativeWrapperHandle, this.nativeFuture));
    }

    @Override // com.google.ar.core.FutureImpl, com.google.ar.core.Future
    public /* bridge */ /* synthetic */ FutureState getState() {
        return super.getState();
    }

    native String nativeGetResultCloudAnchorId(long j, long j2);

    native int nativeGetResultCloudAnchorState(long j, long j2);
}
