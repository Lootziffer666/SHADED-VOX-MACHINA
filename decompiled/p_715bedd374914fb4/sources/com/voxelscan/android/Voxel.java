package com.voxelscan.android;

/* JADX INFO: loaded from: classes.dex */
final class Voxel {
    private static final float MAX_FREE_EVIDENCE = 36.0f;
    private static final float MAX_HIT_EVIDENCE = 30.0f;
    float b;
    float freeSamples;
    float g;
    long lastFreeSeen;
    long lastSeen;
    float r;
    float samples;
    long lastHitFrame = Long.MIN_VALUE;
    long lastFreeFrame = Long.MIN_VALUE;

    Voxel(int i, int i2, int i3, float f, long j) {
        this.r = i;
        this.g = i2;
        this.b = i3;
        this.samples = f;
        this.lastSeen = j;
    }

    void add(int i, int i2, int i3, float f, long j, long j2) {
        float fMax = Math.max(0.05f, f);
        float f2 = this.samples;
        float f3 = f2 + fMax;
        this.r = ((this.r * f2) + (i * fMax)) / f3;
        this.g = ((this.g * f2) + (i2 * fMax)) / f3;
        this.b = ((this.b * f2) + (i3 * fMax)) / f3;
        if (this.lastHitFrame != j2) {
            this.samples = Math.min(MAX_HIT_EVIDENCE, f2 + f);
            this.freeSamples *= 0.94f;
            this.lastHitFrame = j2;
        }
        this.lastSeen = j;
    }

    void observeFree(float f, long j, long j2) {
        if (this.lastFreeFrame == j2) {
            return;
        }
        this.freeSamples = Math.min(MAX_FREE_EVIDENCE, this.freeSamples + f);
        this.lastFreeSeen = j;
        this.lastFreeFrame = j2;
    }

    boolean isConfirmed(float f) {
        float f2 = this.samples;
        return f2 >= f && this.freeSamples < (f2 * 0.72f) + 3.0f;
    }

    boolean isContradicted() {
        float f = this.freeSamples;
        return f >= 5.0f && f > (this.samples * 0.82f) + 3.5f;
    }

    float confidence() {
        float fMin = Math.min(1.0f, this.samples / 18.0f);
        float f = this.samples;
        return fMin * (f / Math.max(1.0f, this.freeSamples + f));
    }
}
