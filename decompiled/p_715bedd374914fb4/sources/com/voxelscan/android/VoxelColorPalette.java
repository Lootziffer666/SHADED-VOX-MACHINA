package com.voxelscan.android;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import kotlin.UShort;

/* JADX INFO: loaded from: classes.dex */
final class VoxelColorPalette {
    private static final int BUCKET_BITS = 5;
    private static final int BUCKET_COUNT = 32768;
    private static final int BUCKET_LEVELS = 32;
    private static final int KMEANS_ITERATIONS = 5;
    private static final int MAX_COLORS = 255;
    private final short[] bucketToColor;
    private final int[] colors;

    private static int colorDistance(int i, int i2) {
        int i3 = (i >> 16) & 255;
        int i4 = (i2 >> 16) & 255;
        int i5 = i3 - i4;
        int i6 = ((i >> 8) & 255) - ((i2 >> 8) & 255);
        int i7 = (i & 255) - (i2 & 255);
        int i8 = (i3 + i4) >> 1;
        return ((((i8 + 512) * i5) * i5) >> 8) + (i6 * 4 * i6) + ((((767 - i8) * i7) * i7) >> 8);
    }

    private VoxelColorPalette(int[] iArr, short[] sArr) {
        this.colors = iArr;
        this.bucketToColor = sArr;
    }

    static VoxelColorPalette from(ArrayList<VoxelWorld.Entry> arrayList, int i) {
        int iMax = Math.max(1, Math.min(255, i));
        double[] dArr = new double[32768];
        double[] dArr2 = new double[32768];
        double[] dArr3 = new double[32768];
        double[] dArr4 = new double[32768];
        int i2 = 0;
        for (VoxelWorld.Entry entry : arrayList) {
            int iBucketKey = bucketKey(entry.voxel.r, entry.voxel.g, entry.voxel.b);
            if (dArr4[iBucketKey] == 0.0d) {
                i2++;
            }
            double dMax = Math.max(1.0d, entry.voxel.samples);
            dArr[iBucketKey] = dArr[iBucketKey] + (((double) entry.voxel.r) * dMax);
            dArr2[iBucketKey] = dArr2[iBucketKey] + (((double) entry.voxel.g) * dMax);
            dArr3[iBucketKey] = dArr3[iBucketKey] + (((double) entry.voxel.b) * dMax);
            dArr4[iBucketKey] = dArr4[iBucketKey] + dMax;
            i2 = i2;
        }
        if (i2 == 0) {
            int[] iArr = {pack(128, 128, 128)};
            return new VoxelColorPalette(iArr, mapAllBuckets(iArr));
        }
        int[] iArr2 = new int[i2];
        int i3 = 0;
        for (int i4 = 0; i4 < 32768; i4++) {
            if (dArr4[i4] > 0.0d) {
                iArr2[i3] = i4;
                i3++;
            }
        }
        int[] iArrSeedCenters = seedCenters(iArr2, dArr, dArr2, dArr3, dArr4, Math.min(iMax, i2));
        refineCenters(iArr2, dArr, dArr2, dArr3, dArr4, iArrSeedCenters);
        return new VoxelColorPalette(iArrSeedCenters, mapAllBuckets(iArrSeedCenters));
    }

    int packedColor(Voxel voxel) {
        return this.colors[this.bucketToColor[bucketKey(voxel.r, voxel.g, voxel.b)] & UShort.MAX_VALUE];
    }

    int indexFor(Voxel voxel) {
        return (this.bucketToColor[bucketKey(voxel.r, voxel.g, voxel.b)] & UShort.MAX_VALUE) + 1;
    }

    int size() {
        return this.colors.length;
    }

    byte[] magicaVoxelBytes() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
        int[] iArr = this.colors;
        int i = iArr[iArr.length - 1];
        int i2 = 0;
        while (i2 < 255) {
            int[] iArr2 = this.colors;
            int i3 = i2 < iArr2.length ? iArr2[i2] : i;
            byteArrayOutputStream.write((i3 >> 16) & 255);
            byteArrayOutputStream.write((i3 >> 8) & 255);
            byteArrayOutputStream.write(i3 & 255);
            byteArrayOutputStream.write(255);
            i2++;
        }
        byteArrayOutputStream.write(0);
        byteArrayOutputStream.write(0);
        byteArrayOutputStream.write(0);
        byteArrayOutputStream.write(0);
        return byteArrayOutputStream.toByteArray();
    }

    private static int[] seedCenters(int[] iArr, double[] dArr, double[] dArr2, double[] dArr3, double[] dArr4, int i) {
        int[] iArr2 = iArr;
        int i2 = i;
        int[] iArr3 = new int[i2];
        int i3 = 0;
        int i4 = iArr2[0];
        for (int i5 : iArr2) {
            if (dArr4[i5] > dArr4[i4]) {
                i4 = i5;
            }
        }
        iArr3[0] = centroid(i4, dArr, dArr2, dArr3, dArr4);
        int i6 = 1;
        while (i6 < i2) {
            int i7 = iArr2[i3];
            int length = iArr2.length;
            double d = -1.0d;
            int i8 = i3;
            while (i8 < length) {
                int i9 = iArr2[i8];
                int iCentroid = centroid(i9, dArr, dArr2, dArr3, dArr4);
                int iMin = Integer.MAX_VALUE;
                for (int i10 = i3; i10 < i6; i10++) {
                    iMin = Math.min(iMin, colorDistance(iCentroid, iArr3[i10]));
                }
                int[] iArr4 = iArr3;
                double dSqrt = ((double) iMin) * Math.sqrt(dArr4[i9]);
                if (dSqrt > d) {
                    d = dSqrt;
                    i7 = i9;
                }
                i8++;
                iArr3 = iArr4;
                i3 = 0;
                iArr2 = iArr;
            }
            iArr3[i6] = centroid(i7, dArr, dArr2, dArr3, dArr4);
            i6++;
            i2 = i;
            i3 = 0;
            iArr2 = iArr;
        }
        return iArr3;
    }

    private static void refineCenters(int[] iArr, double[] dArr, double[] dArr2, double[] dArr3, double[] dArr4, int[] iArr2) {
        double[] dArr5 = new double[iArr2.length];
        double[] dArr6 = new double[iArr2.length];
        double[] dArr7 = new double[iArr2.length];
        double[] dArr8 = new double[iArr2.length];
        for (int i = 0; i < 5; i++) {
            Arrays.fill(dArr5, 0.0d);
            Arrays.fill(dArr6, 0.0d);
            Arrays.fill(dArr7, 0.0d);
            Arrays.fill(dArr8, 0.0d);
            for (int i2 : iArr) {
                int iNearestIndex = nearestIndex(centroid(i2, dArr, dArr2, dArr3, dArr4), iArr2);
                dArr5[iNearestIndex] = dArr5[iNearestIndex] + dArr[i2];
                dArr6[iNearestIndex] = dArr6[iNearestIndex] + dArr2[i2];
                dArr7[iNearestIndex] = dArr7[iNearestIndex] + dArr3[i2];
                dArr8[iNearestIndex] = dArr8[iNearestIndex] + dArr4[i2];
            }
            for (int i3 = 0; i3 < iArr2.length; i3++) {
                double d = dArr8[i3];
                if (d > 0.0d) {
                    iArr2[i3] = pack((int) Math.round(dArr5[i3] / d), (int) Math.round(dArr6[i3] / dArr8[i3]), (int) Math.round(dArr7[i3] / dArr8[i3]));
                }
            }
        }
    }

    private static short[] mapAllBuckets(int[] iArr) {
        short[] sArr = new short[32768];
        for (int i = 0; i < 32768; i++) {
            sArr[i] = (short) nearestIndex(pack(Math.round((((i >> 10) & 31) * 255.0f) / 31.0f), Math.round((((i >> 5) & 31) * 255.0f) / 31.0f), Math.round(((i & 31) * 255.0f) / 31.0f)), iArr);
        }
        return sArr;
    }

    private static int nearestIndex(int i, int[] iArr) {
        int i2 = Integer.MAX_VALUE;
        int i3 = 0;
        for (int i4 = 0; i4 < iArr.length; i4++) {
            int iColorDistance = colorDistance(i, iArr[i4]);
            if (iColorDistance < i2) {
                i3 = i4;
                i2 = iColorDistance;
            }
        }
        return i3;
    }

    private static int centroid(int i, double[] dArr, double[] dArr2, double[] dArr3, double[] dArr4) {
        return pack((int) Math.round(dArr[i] / dArr4[i]), (int) Math.round(dArr2[i] / dArr4[i]), (int) Math.round(dArr3[i] / dArr4[i]));
    }

    private static int bucketKey(float f, float f2, float f3) {
        return (Math.max(0, Math.min(31, Math.round((f * 31.0f) / 255.0f))) << 10) | (Math.max(0, Math.min(31, Math.round((f2 * 31.0f) / 255.0f))) << 5) | Math.max(0, Math.min(31, Math.round((f3 * 31.0f) / 255.0f)));
    }

    private static int pack(int i, int i2, int i3) {
        return (Math.max(0, Math.min(255, i)) << 16) | (Math.max(0, Math.min(255, i2)) << 8) | Math.max(0, Math.min(255, i3));
    }
}
