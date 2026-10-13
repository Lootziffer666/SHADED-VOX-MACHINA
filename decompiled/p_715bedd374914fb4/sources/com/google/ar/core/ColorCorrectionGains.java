package com.google.ar.core;

import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public class ColorCorrectionGains {
    private final float blue;
    private final float greenEven;
    private final float greenOdd;
    private final float red;

    public ColorCorrectionGains(float f, float f2, float f3, float f4) {
        this.red = f;
        this.greenEven = f2;
        this.greenOdd = f3;
        this.blue = f4;
    }

    public float blue() {
        return this.blue;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ColorCorrectionGains)) {
            return false;
        }
        ColorCorrectionGains colorCorrectionGains = (ColorCorrectionGains) obj;
        return this.red == colorCorrectionGains.red && this.greenEven == colorCorrectionGains.greenEven && this.greenOdd == colorCorrectionGains.greenOdd && this.blue == colorCorrectionGains.blue;
    }

    public float greenEven() {
        return this.greenEven;
    }

    public float greenOdd() {
        return this.greenOdd;
    }

    public int hashCode() {
        return Objects.hash(Float.valueOf(this.red), Float.valueOf(this.greenEven), Float.valueOf(this.greenOdd), Float.valueOf(this.blue));
    }

    public float red() {
        return this.red;
    }
}
