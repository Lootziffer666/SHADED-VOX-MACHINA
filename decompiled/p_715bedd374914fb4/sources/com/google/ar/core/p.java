package com.google.ar.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/* JADX INFO: compiled from: FaceCache.java */
/* JADX INFO: loaded from: classes.dex */
final class p extends LinkedHashMap {
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    p(q qVar) {
        super(1, 0.75f, true);
        Objects.requireNonNull(qVar);
    }

    @Override // java.util.LinkedHashMap
    protected final boolean removeEldestEntry(Map.Entry entry) {
        return size() > 10;
    }
}
