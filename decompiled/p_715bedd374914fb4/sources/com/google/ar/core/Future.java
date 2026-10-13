package com.google.ar.core;

/* JADX INFO: loaded from: classes.dex */
public interface Future {
    boolean cancel();

    FutureState getState();
}
