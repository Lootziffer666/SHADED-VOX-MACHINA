package com.google.vr.dynamite.client;

import java.util.Objects;

/* JADX INFO: compiled from: TargetLibraryInfo.java */
/* JADX INFO: loaded from: classes.dex */
final class f {
    private final String a;
    private final String b;

    public f(String str, String str2) {
        this.a = str;
        this.b = str2;
    }

    public final String a() {
        return this.a;
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof f) {
            f fVar = (f) obj;
            if (Objects.equals(this.a, fVar.a) && Objects.equals(this.b, fVar.b)) {
                return true;
            }
        }
        return false;
    }

    public final int hashCode() {
        return (Objects.hashCode(this.a) * 37) + Objects.hashCode(this.b);
    }

    public final String toString() {
        return "[packageName=" + this.a + ",libraryName=" + this.b + "]";
    }
}
