package com.google.ar.core;

import com.google.ar.core.exceptions.FatalException;

/* JADX INFO: loaded from: classes.dex */
public enum PlaybackStatus {
    NONE(0),
    OK(1),
    IO_ERROR(2),
    FINISHED(3);

    final int nativeCode;

    PlaybackStatus(int i) {
        this.nativeCode = i;
    }

    static PlaybackStatus forNumber(int i) {
        for (PlaybackStatus playbackStatus : values()) {
            if (playbackStatus.nativeCode == i) {
                return playbackStatus;
            }
        }
        throw new FatalException(q.b((byte) 50, i, "Unexpected value for native PlaybackStatus, value="));
    }
}
