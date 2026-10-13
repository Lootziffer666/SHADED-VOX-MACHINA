package com.google.ar.core;

import java.util.Map;

/* JADX INFO: compiled from: FaceCache.java */
/* JADX INFO: loaded from: classes.dex */
final class q {
    final Map a = new p(this);

    q() {
    }

    static /* synthetic */ String b(byte b, int i, String str) {
        StringBuilder sb = new StringBuilder(String.valueOf(i).length() + b);
        sb.append(str);
        sb.append(i);
        return sb.toString();
    }

    public final synchronized AugmentedFace a(long j, Session session) {
        Map map = this.a;
        Long lValueOf = Long.valueOf(j);
        AugmentedFace augmentedFace = (AugmentedFace) map.get(lValueOf);
        if (augmentedFace != null) {
            return augmentedFace;
        }
        AugmentedFace augmentedFace2 = new AugmentedFace(j, session);
        map.put(lValueOf, augmentedFace2);
        return augmentedFace2;
    }
}
