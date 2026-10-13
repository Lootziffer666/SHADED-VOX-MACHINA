package com.google.vr.dynamite.client;

/* JADX INFO: compiled from: LoaderException.java */
/* JADX INFO: loaded from: classes.dex */
public final class d extends Exception {
    private final int a = 1;

    @Override // java.lang.Throwable
    public final String getMessage() {
        String str = this.a != 1 ? "Unknown error" : "Package not available";
        StringBuilder sb = new StringBuilder(str.length() + 17);
        sb.append("LoaderException{");
        sb.append(str);
        sb.append("}");
        return sb.toString();
    }
}
