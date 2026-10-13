package com.google.ar.core;

import com.google.ar.core.exceptions.FatalException;

/* JADX INFO: loaded from: classes.dex */
public enum TrackingState {
    TRACKING(0),
    PAUSED(1),
    STOPPED(2);

    final int nativeCode;

    TrackingState(int i) {
        this.nativeCode = i;
    }

    static TrackingState forNumber(int i) {
        for (TrackingState trackingState : values()) {
            if (trackingState.nativeCode == i) {
                return trackingState;
            }
        }
        throw new FatalException(q.b((byte) 49, i, "Unexpected value for native TrackingState, value="));
    }
}
