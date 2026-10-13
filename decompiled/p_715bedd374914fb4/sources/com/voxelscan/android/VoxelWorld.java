package com.voxelscan.android;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class VoxelWorld {
    private static final int CHUNK_SIZE = 16;
    private static final int CONFIRM_SAMPLES = 5;
    static final float DEFAULT_VOXEL_SIZE = 0.05f;
    private static final int MAX_LAB_TSDF_CELLS = 450000;
    private static final int MAX_VOXELS = 3000;
    private static final int TEST_MAX_NEW_VOXELS_PER_FRAME = 96;
    private float axisYawRadians;
    private int insertionsThisFrame;
    private final HashMap<String, HashMap<String, Voxel>> chunks = new HashMap<>();
    private final HashSet<String> occupied = new HashSet<>();
    private final LinkedHashSet<String> dirtyMeshChunks = new LinkedHashSet<>();
    private final HashMap<Long, TsdfCell> labTsdf = new HashMap<>();
    private final HashSet<Long> labSurface = new HashSet<>();
    private float voxelSize = DEFAULT_VOXEL_SIZE;
    private float cosAxis = 1.0f;
    private float sinAxis = 0.0f;
    private long insertionFrame = Long.MIN_VALUE;

    interface Filter {
        boolean remove(int i, int i2, int i3, Voxel voxel);
    }

    private static long packedKey(int i, int i2, int i3) {
        return ((((long) i2) & 2097151) << 21) | ((((long) i) & 2097151) << 42) | (((long) i3) & 2097151);
    }

    private static int unpackCoordinate(long j, int i) {
        int i2 = (int) ((j >> i) & 2097151);
        return (1048576 & i2) != 0 ? i2 | (-2097152) : i2;
    }

    int maxVoxelCount() {
        return MAX_VOXELS;
    }

    VoxelWorld() {
    }

    float voxelSize() {
        return this.voxelSize;
    }

    boolean setVoxelSize(float f) {
        if (!this.occupied.isEmpty() || !this.labTsdf.isEmpty()) {
            return false;
        }
        this.voxelSize = f;
        return true;
    }

    int clear() {
        int size = this.occupied.size();
        this.dirtyMeshChunks.addAll(this.chunks.keySet());
        this.chunks.clear();
        this.occupied.clear();
        this.labTsdf.clear();
        this.labSurface.clear();
        return size;
    }

    boolean add(float f, float f2, float f3, int i, int i2, int i3, float f4, long j, long j2) {
        float fWorldToGridX = worldToGridX(f, f3);
        float fWorldToGridZ = worldToGridZ(f, f3);
        int iVoxelIndex = voxelIndex(fWorldToGridX);
        int iVoxelIndex2 = voxelIndex(f2);
        int iVoxelIndex3 = voxelIndex(fWorldToGridZ);
        int iFloorDiv = floorDiv(iVoxelIndex, 16);
        int iFloorDiv2 = floorDiv(iVoxelIndex2, 16);
        int iFloorDiv3 = floorDiv(iVoxelIndex3, 16);
        int iMod = mod(iVoxelIndex, 16);
        int iMod2 = mod(iVoxelIndex2, 16);
        int iMod3 = mod(iVoxelIndex3, 16);
        String strKey = key(iFloorDiv, iFloorDiv2, iFloorDiv3);
        String strKey2 = key(iMod, iMod2, iMod3);
        HashMap<String, Voxel> map = this.chunks.get(strKey);
        if (map == null) {
            map = new HashMap<>();
            this.chunks.put(strKey, map);
        }
        Voxel voxel = map.get(strKey2);
        if (voxel == null) {
            if (this.insertionFrame != j2) {
                this.insertionFrame = j2;
                this.insertionsThisFrame = 0;
            }
            if (this.insertionsThisFrame >= TEST_MAX_NEW_VOXELS_PER_FRAME) {
                if (map.isEmpty()) {
                    this.chunks.remove(strKey);
                }
                return false;
            }
            if (this.occupied.size() >= MAX_VOXELS) {
                if (map.isEmpty()) {
                    this.chunks.remove(strKey);
                }
                return false;
            }
            Voxel voxel2 = new Voxel(i, i2, i3, f4, j);
            voxel2.lastHitFrame = j2;
            map.put(strKey2, voxel2);
            this.occupied.add(key(iVoxelIndex, iVoxelIndex2, iVoxelIndex3));
            this.insertionsThisFrame++;
        } else {
            voxel.add(i, i2, i3, f4, j, j2);
        }
        markVoxelDirty(iVoxelIndex, iVoxelIndex2, iVoxelIndex3);
        return true;
    }

    float exposureScaleObservation(float f, float f2, float f3, int i, int i2, int i3, long j) {
        Voxel voxel = getVoxel(voxelIndex(worldToGridX(f, f3)), voxelIndex(f2), voxelIndex(worldToGridZ(f, f3)));
        if (voxel != null && voxel.isConfirmed(5.0f) && voxel.lastHitFrame != j) {
            float f4 = (i * 0.2126f) + (i2 * 0.7152f) + (i3 * 0.0722f);
            float f5 = (voxel.r * 0.2126f) + (voxel.g * 0.7152f) + (voxel.b * 0.0722f);
            if (f4 >= 18.0f && f5 >= 18.0f) {
                return Math.max(0.6f, Math.min(1.65f, f5 / f4));
            }
        }
        return Float.NaN;
    }

    void carveFreeSpace(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, long j, long j2) {
        float f9 = f4 - f;
        float f10 = f5 - f2;
        float f11 = f6 - f3;
        float fSqrt = (float) Math.sqrt((f9 * f9) + (f10 * f10) + (f11 * f11));
        float f12 = fSqrt - f7;
        if (f12 <= 0.35f) {
            return;
        }
        float f13 = f9 / fSqrt;
        float f14 = f10 / fSqrt;
        float f15 = f11 / fSqrt;
        int i = Integer.MIN_VALUE;
        int i2 = Integer.MIN_VALUE;
        float f16 = 0.3f;
        int i3 = Integer.MIN_VALUE;
        while (f16 < f12) {
            float f17 = f + (f13 * f16);
            float f18 = f3 + (f15 * f16);
            int iVoxelIndex = voxelIndex(worldToGridX(f17, f18));
            int iVoxelIndex2 = voxelIndex(f2 + (f14 * f16));
            int iVoxelIndex3 = voxelIndex(worldToGridZ(f17, f18));
            if (iVoxelIndex != i || iVoxelIndex2 != i3 || iVoxelIndex3 != i2) {
                observeFree(iVoxelIndex, iVoxelIndex2, iVoxelIndex3, f8, j, j2);
                i = iVoxelIndex;
                i3 = iVoxelIndex2;
                i2 = iVoxelIndex3;
            }
            f16 += this.voxelSize * 0.8f;
        }
    }

    /* JADX WARN: Code duplicated, block: B:37:0x018a  */
    /* JADX WARN: Code duplicated, block: B:38:0x018c  */
    /* JADX WARN: Code duplicated, block: B:41:0x01a1  */
    /* JADX WARN: Code duplicated, block: B:42:0x01ae  */
    void integrateSurfaceTsdf(float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, int i3, float f7, long j, long j2) {
        TsdfCell tsdfCell;
        int i4;
        float fMin;
        boolean z;
        int i5;
        VoxelWorld voxelWorld = this;
        float f8 = f4 - f;
        float f9 = f5 - f2;
        float f10 = f6 - f3;
        float fSqrt = (float) Math.sqrt((f8 * f8) + (f9 * f9) + (f10 * f10));
        if (fSqrt < 0.3f) {
            return;
        }
        float f11 = f8 / fSqrt;
        float f12 = f9 / fSqrt;
        float f13 = f10 / fSqrt;
        float fMax = Math.max(voxelWorld.voxelSize * 1.75f, (fSqrt * 0.012f) + 0.055f);
        float fMax2 = Math.max(0.3f, fSqrt - fMax);
        float f14 = fSqrt + fMax;
        float fMax3 = Math.max(0.3f, fSqrt - Math.max(voxelWorld.voxelSize * 2.25f, Math.max(fMax, (0.025f * fSqrt) + 0.09f)));
        float fMax4 = Math.max(voxelWorld.voxelSize * 0.8f, 0.016f);
        int i6 = Integer.MIN_VALUE;
        int i7 = Integer.MIN_VALUE;
        int i8 = Integer.MIN_VALUE;
        for (float f15 = 0.3f; f15 < fMax3; f15 += fMax4) {
            float f16 = f + (f11 * f15);
            float f17 = f3 + (f13 * f15);
            int iVoxelIndex = voxelWorld.voxelIndex(voxelWorld.worldToGridX(f16, f17));
            int iVoxelIndex2 = voxelWorld.voxelIndex(f2 + (f12 * f15));
            int iVoxelIndex3 = voxelWorld.voxelIndex(voxelWorld.worldToGridZ(f16, f17));
            if (iVoxelIndex != i6 || iVoxelIndex2 != i7 || iVoxelIndex3 != i8) {
                long jPackedKey = packedKey(iVoxelIndex, iVoxelIndex2, iVoxelIndex3);
                TsdfCell tsdfCell2 = voxelWorld.labTsdf.get(Long.valueOf(jPackedKey));
                if (tsdfCell2 != null && tsdfCell2.observeFree(f7 * 0.3f, j, j2)) {
                    refreshTsdfSurface(jPackedKey, j, j2);
                }
                i8 = iVoxelIndex3;
                i6 = iVoxelIndex;
                i7 = iVoxelIndex2;
            }
        }
        float fMax5 = Math.max(voxelWorld.voxelSize * 0.55f, 0.012f);
        int i9 = Integer.MIN_VALUE;
        int i10 = Integer.MIN_VALUE;
        int i11 = Integer.MIN_VALUE;
        while (fMax2 <= f14) {
            float f18 = f + (f11 * fMax2);
            float f19 = f3 + (f13 * fMax2);
            int iVoxelIndex4 = voxelWorld.voxelIndex(voxelWorld.worldToGridX(f18, f19));
            int iVoxelIndex5 = voxelWorld.voxelIndex(f2 + (f12 * fMax2));
            int iVoxelIndex6 = voxelWorld.voxelIndex(voxelWorld.worldToGridZ(f18, f19));
            if (iVoxelIndex4 != i9 || iVoxelIndex5 != i10 || iVoxelIndex6 != i11) {
                long jPackedKey2 = packedKey(iVoxelIndex4, iVoxelIndex5, iVoxelIndex6);
                TsdfCell tsdfCell3 = voxelWorld.labTsdf.get(Long.valueOf(jPackedKey2));
                if (tsdfCell3 == null) {
                    if (voxelWorld.labTsdf.size() >= MAX_LAB_TSDF_CELLS) {
                        i4 = iVoxelIndex4;
                        i5 = iVoxelIndex6;
                    } else {
                        tsdfCell3 = new TsdfCell();
                        voxelWorld.labTsdf.put(Long.valueOf(jPackedKey2), tsdfCell3);
                        tsdfCell = tsdfCell3;
                        float f20 = voxelWorld.voxelSize;
                        float f21 = (iVoxelIndex4 + 0.5f) * f20;
                        i4 = iVoxelIndex4;
                        float f22 = (iVoxelIndex6 + 0.5f) * f20;
                        float f23 = voxelWorld.cosAxis;
                        float f24 = voxelWorld.sinAxis;
                        fMin = Math.min(1.0f, Math.abs(fSqrt - ((((((f21 * f23) + (f22 * f24)) - f) * f11) + ((((iVoxelIndex5 + 0.5f) * f20) - f2) * f12)) + (((((-f21) * f24) + (f22 * f23)) - f3) * f13))) / fMax);
                        if (fMin <= 0.6f) {
                            z = true;
                        } else {
                            z = false;
                        }
                        if (tsdfCell.integrate(fMin, f7, j, j2, i, i2, i3, z)) {
                            i5 = iVoxelIndex6;
                            refreshTsdfSurface(jPackedKey2, j, j2);
                        } else {
                            i5 = iVoxelIndex6;
                        }
                    }
                } else {
                    tsdfCell = tsdfCell3;
                    float f25 = voxelWorld.voxelSize;
                    float f26 = (iVoxelIndex4 + 0.5f) * f25;
                    i4 = iVoxelIndex4;
                    float f27 = (iVoxelIndex6 + 0.5f) * f25;
                    float f28 = voxelWorld.cosAxis;
                    float f29 = voxelWorld.sinAxis;
                    fMin = Math.min(1.0f, Math.abs(fSqrt - ((((((f26 * f28) + (f27 * f29)) - f) * f11) + ((((iVoxelIndex5 + 0.5f) * f25) - f2) * f12)) + (((((-f26) * f29) + (f27 * f28)) - f3) * f13))) / fMax);
                    if (fMin <= 0.6f) {
                        z = true;
                    } else {
                        z = false;
                    }
                    if (tsdfCell.integrate(fMin, f7, j, j2, i, i2, i3, z)) {
                        i5 = iVoxelIndex6;
                        refreshTsdfSurface(jPackedKey2, j, j2);
                    } else {
                        i5 = iVoxelIndex6;
                    }
                }
                i9 = i4;
                i10 = iVoxelIndex5;
                i11 = i5;
            }
            fMax2 += fMax5;
            voxelWorld = this;
        }
    }

    private void refreshTsdfSurface(long j, long j2, long j3) {
        TsdfCell tsdfCell = this.labTsdf.get(Long.valueOf(j));
        if (tsdfCell == null) {
            return;
        }
        boolean zContains = this.labSurface.contains(Long.valueOf(j));
        boolean z = true;
        boolean z2 = tsdfCell.weight >= 4.5f && tsdfCell.colorWeight >= 1.35f && !tsdfCell.isContradicted(j2) && tsdfCell.distance <= 0.46f;
        if (!zContains) {
            z = z2;
        } else if (tsdfCell.isStronglyContradicted(j2)) {
            z = false;
        }
        int iUnpackCoordinate = unpackCoordinate(j, 42);
        int iUnpackCoordinate2 = unpackCoordinate(j, 21);
        int iUnpackCoordinate3 = unpackCoordinate(j, 0);
        if (!z) {
            if (zContains) {
                this.labSurface.remove(Long.valueOf(j));
                removeVoxelOnly(iUnpackCoordinate, iUnpackCoordinate2, iUnpackCoordinate3);
                return;
            }
            return;
        }
        this.labSurface.add(Long.valueOf(j));
        int iClampColor = clampColor(Math.round(tsdfCell.r));
        int iClampColor2 = clampColor(Math.round(tsdfCell.g));
        int iClampColor3 = clampColor(Math.round(tsdfCell.b));
        Voxel voxel = getVoxel(iUnpackCoordinate, iUnpackCoordinate2, iUnpackCoordinate3);
        if (voxel == null) {
            Voxel voxel2 = new Voxel(iClampColor, iClampColor2, iClampColor3, Math.max(5.0f, Math.min(30.0f, tsdfCell.weight)), j2);
            voxel2.lastHitFrame = j3;
            putGridVoxel(iUnpackCoordinate, iUnpackCoordinate2, iUnpackCoordinate3, voxel2);
            return;
        }
        voxel.r = iClampColor;
        voxel.g = iClampColor2;
        voxel.b = iClampColor3;
        voxel.samples = Math.max(5.0f, Math.min(30.0f, tsdfCell.weight));
        voxel.freeSamples = 0.0f;
        voxel.lastSeen = j2;
        voxel.lastHitFrame = j3;
        markVoxelDirty(iUnpackCoordinate, iUnpackCoordinate2, iUnpackCoordinate3);
    }

    void setAxisYaw(float f) {
        this.axisYawRadians = f;
        this.cosAxis = (float) Math.cos(f);
        this.sinAxis = (float) Math.sin(this.axisYawRadians);
    }

    float axisYawDegrees() {
        return (float) Math.toDegrees(this.axisYawRadians);
    }

    static /* synthetic */ boolean lambda$removeLowConfidence$0(int i, int i2, int i3, int i4, Voxel voxel) {
        return voxel.samples < ((float) i);
    }

    int removeLowConfidence(final int i) {
        return removeWhere(new Filter() { // from class: com.voxelscan.android.VoxelWorld$$ExternalSyntheticLambda1
            @Override // com.voxelscan.android.VoxelWorld.Filter
            public final boolean remove(int i2, int i3, int i4, Voxel voxel) {
                return VoxelWorld.lambda$removeLowConfidence$0(i, i2, i3, i4, voxel);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$removeIsolated$1(int i, int i2, int i3, int i4, Voxel voxel) {
        return countNeighbors(i2, i3, i4) < i;
    }

    int removeIsolated(final int i) {
        return removeWhere(new Filter() { // from class: com.voxelscan.android.VoxelWorld$$ExternalSyntheticLambda2
            @Override // com.voxelscan.android.VoxelWorld.Filter
            public final boolean remove(int i2, int i3, int i4, Voxel voxel) {
                return this.f$0.lambda$removeIsolated$1(i, i2, i3, i4, voxel);
            }
        });
    }

    int cleanup() {
        ArrayList<int[]> arrayList = new ArrayList();
        long jCurrentTimeMillis = System.currentTimeMillis();
        Iterator<Map.Entry<String, HashMap<String, Voxel>>> it = this.chunks.entrySet().iterator();
        while (true) {
            char c = 1;
            char c2 = 0;
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<String, HashMap<String, Voxel>> next = it.next();
            int[] key = parseKey(next.getKey());
            for (Map.Entry<String, Voxel> entry : next.getValue().entrySet()) {
                int[] key2 = parseKey(entry.getKey());
                int i = (key[c2] * 16) + key2[c2];
                int i2 = (key[c] * 16) + key2[c];
                int i3 = (key[2] * 16) + key2[2];
                Voxel value = entry.getValue();
                boolean z = !value.isConfirmed(5.0f) && jCurrentTimeMillis - value.lastSeen > 1200;
                boolean z2 = value.samples < 12.0f && countConfirmedNeighbors26(i, i2, i3) < 2;
                if (value.isContradicted() || z || z2) {
                    arrayList.add(new int[]{i, i2, i3});
                }
                c = 1;
                c2 = 0;
            }
        }
        for (int[] iArr : arrayList) {
            delete(iArr[0], iArr[1], iArr[2]);
        }
        return arrayList.size();
    }

    int discardRecentWeak(final long j, final float f) {
        return removeWhere(new Filter() { // from class: com.voxelscan.android.VoxelWorld$$ExternalSyntheticLambda0
            @Override // com.voxelscan.android.VoxelWorld.Filter
            public final boolean remove(int i, int i2, int i3, Voxel voxel) {
                return VoxelWorld.lambda$discardRecentWeak$2(j, f, i, i2, i3, voxel);
            }
        });
    }

    static /* synthetic */ boolean lambda$discardRecentWeak$2(long j, float f, int i, int i2, int i3, Voxel voxel) {
        return voxel.lastSeen >= j && voxel.samples < f;
    }

    int snapFloor(float f, float f2) {
        ArrayList<Move> arrayList = new ArrayList<>();
        int iVoxelIndex = voxelIndex(f);
        for (Entry entry : entries()) {
            if (Math.abs(worldY(entry) - f) <= f2 && entry.y != iVoxelIndex) {
                arrayList.add(new Move(entry.x, entry.y, entry.z, entry.x, iVoxelIndex, entry.z, entry.voxel));
            }
        }
        return applyMoves(arrayList);
    }

    int snapWall(float f, float f2, float f3, float f4, float f5) {
        float fSqrt = (float) Math.sqrt((f3 * f3) + (f4 * f4));
        if (fSqrt < 0.001f) {
            return 0;
        }
        float f6 = f3 / fSqrt;
        float f7 = f4 / fSqrt;
        ArrayList<Move> arrayList = new ArrayList<>();
        for (Entry entry : entries()) {
            float fWorldX = worldX(entry);
            float fWorldZ = worldZ(entry);
            float f8 = ((fWorldX - f) * f6) + ((fWorldZ - f2) * f7);
            if (Math.abs(f8) <= f5) {
                float f9 = fWorldX - (f8 * f6);
                float f10 = fWorldZ - (f8 * f7);
                int iVoxelIndex = voxelIndex(worldToGridX(f9, f10));
                int iVoxelIndex2 = voxelIndex(worldToGridZ(f9, f10));
                if (iVoxelIndex != entry.x || iVoxelIndex2 != entry.z) {
                    arrayList.add(new Move(entry.x, entry.y, entry.z, iVoxelIndex, entry.y, iVoxelIndex2, entry.voxel));
                }
            }
        }
        return applyMoves(arrayList);
    }

    int voxelCount() {
        return this.occupied.size();
    }

    boolean hasReconstructionData() {
        return (this.occupied.isEmpty() && this.labTsdf.isEmpty()) ? false : true;
    }

    int confirmedCount() {
        Iterator<Entry> it = entries().iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().voxel.isConfirmed(5.0f)) {
                i++;
            }
        }
        return i;
    }

    boolean hasConfirmedVoxel(int i, int i2, int i3) {
        Voxel voxel = getVoxel(i, i2, i3);
        return voxel != null && voxel.isConfirmed(5.0f);
    }

    int chunkCount() {
        return this.chunks.size();
    }

    int tsdfCellCount() {
        return this.labTsdf.size();
    }

    float highConfidenceRatio() {
        Iterator<Entry> it = entries().iterator();
        int i = 0;
        int i2 = 0;
        while (it.hasNext()) {
            i++;
            if (it.next().voxel.confidence() >= 0.7f) {
                i2++;
            }
        }
        if (i == 0) {
            return 0.0f;
        }
        return i2 / i;
    }

    float reviewRadius() {
        ArrayList<Entry> arrayListEntries = entries();
        if (arrayListEntries.isEmpty()) {
            return 2.0f;
        }
        float f = this.voxelSize;
        float fMax = Float.NEGATIVE_INFINITY;
        float fMax2 = Float.NEGATIVE_INFINITY;
        float fMin = Float.POSITIVE_INFINITY;
        float fMin2 = Float.POSITIVE_INFINITY;
        float fMin3 = Float.POSITIVE_INFINITY;
        float fMax3 = Float.NEGATIVE_INFINITY;
        for (Entry entry : arrayListEntries) {
            float[] fArrGridToWorld = gridToWorld((entry.x + 0.5f) * f, (entry.y + 0.5f) * f, (entry.z + 0.5f) * f);
            float f2 = fArrGridToWorld[0];
            float f3 = fArrGridToWorld[1];
            float f4 = fArrGridToWorld[2];
            fMin = Math.min(fMin, f2);
            fMin2 = Math.min(fMin2, f3);
            fMin3 = Math.min(fMin3, f4);
            fMax = Math.max(fMax, f2);
            fMax3 = Math.max(fMax3, f3);
            fMax2 = Math.max(fMax2, f4);
        }
        float f5 = fMax - fMin;
        float f6 = fMax3 - fMin2;
        float f7 = fMax2 - fMin3;
        return Math.max(1.2f, ((float) Math.sqrt((f5 * f5) + (f6 * f6) + (f7 * f7))) * 0.75f);
    }

    void reviewCenter(float[] fArr) {
        ArrayList<Entry> arrayListEntries = entries();
        if (arrayListEntries.isEmpty()) {
            fArr[0] = 0.0f;
            fArr[1] = 0.8f;
            fArr[2] = -1.5f;
            return;
        }
        float f = this.voxelSize;
        float fMax = Float.NEGATIVE_INFINITY;
        float fMax2 = Float.NEGATIVE_INFINITY;
        float fMin = Float.POSITIVE_INFINITY;
        float fMin2 = Float.POSITIVE_INFINITY;
        float fMin3 = Float.POSITIVE_INFINITY;
        float fMax3 = Float.NEGATIVE_INFINITY;
        for (Entry entry : arrayListEntries) {
            float[] fArrGridToWorld = gridToWorld((entry.x + 0.5f) * f, (entry.y + 0.5f) * f, (entry.z + 0.5f) * f);
            float f2 = fArrGridToWorld[0];
            float f3 = fArrGridToWorld[1];
            float f4 = fArrGridToWorld[2];
            fMin = Math.min(fMin, f2);
            fMin2 = Math.min(fMin2, f3);
            fMin3 = Math.min(fMin3, f4);
            fMax = Math.max(fMax, f2);
            fMax3 = Math.max(fMax3, f3);
            fMax2 = Math.max(fMax2, f4);
        }
        fArr[0] = (fMin + fMax) * 0.5f;
        fArr[1] = (fMin2 + fMax3) * 0.5f;
        fArr[2] = (fMin3 + fMax2) * 0.5f;
    }

    ArrayList<Entry> entries() {
        ArrayList<Entry> arrayList = new ArrayList<>();
        for (Map.Entry<String, HashMap<String, Voxel>> entry : this.chunks.entrySet()) {
            int[] key = parseKey(entry.getKey());
            for (Map.Entry<String, Voxel> entry2 : entry.getValue().entrySet()) {
                int[] key2 = parseKey(entry2.getKey());
                int i = (key[0] * 16) + key2[0];
                int i2 = (key[1] * 16) + key2[1];
                int i3 = (key[2] * 16) + key2[2];
                if (entry2.getValue().isConfirmed(5.0f)) {
                    arrayList.add(new Entry(i, i2, i3, entry2.getValue()));
                }
            }
        }
        return arrayList;
    }

    ArrayList<String> consumeDirtyMeshChunks(int i) {
        ArrayList<String> arrayList = new ArrayList<>();
        Iterator<String> it = this.dirtyMeshChunks.iterator();
        while (it.hasNext() && arrayList.size() < i) {
            arrayList.add(it.next());
            it.remove();
        }
        return arrayList;
    }

    ArrayList<Entry> confirmedEntriesForChunk(String str) {
        ArrayList<Entry> arrayList = new ArrayList<>();
        HashMap<String, Voxel> map = this.chunks.get(str);
        if (map == null) {
            return arrayList;
        }
        int[] key = parseKey(str);
        for (Map.Entry<String, Voxel> entry : map.entrySet()) {
            Voxel value = entry.getValue();
            if (value.isConfirmed(5.0f)) {
                int[] key2 = parseKey(entry.getKey());
                arrayList.add(new Entry((key[0] * 16) + key2[0], (key[1] * 16) + key2[1], (key[2] * 16) + key2[2], value));
            }
        }
        return arrayList;
    }

    void markAllMeshDirty() {
        this.dirtyMeshChunks.addAll(this.chunks.keySet());
    }

    File writeJson(File file) throws Exception {
        return writeJson(file, 0);
    }

    File writeJson(File file, int i) throws Exception {
        int iPackedColor;
        ArrayList<Entry> arrayListEntries = entries();
        VoxelColorPalette voxelColorPaletteFrom = i > 0 ? VoxelColorPalette.from(arrayListEntries, i) : null;
        StringBuilder sb = new StringBuilder(this.occupied.size() * 72);
        sb.append("{\"version\":1,\"coordinateSystem\":\"right-handed-y-up-camera-forward-negative-z\",\"voxelSize\":");
        sb.append(this.voxelSize).append(",\"origin\":[0,0,0],\"gridAxisYawDegrees\":");
        sb.append(String.format(Locale.US, "%.3f", Float.valueOf(axisYawDegrees()))).append(",\"paletteColors\":");
        sb.append(voxelColorPaletteFrom == null ? 0 : voxelColorPaletteFrom.size()).append(",\"voxels\":[");
        boolean z = true;
        for (Entry entry : arrayListEntries) {
            if (!z) {
                sb.append(',');
            }
            if (voxelColorPaletteFrom == null) {
                iPackedColor = (Math.round(entry.voxel.r) << 16) | (Math.round(entry.voxel.g) << 8) | Math.round(entry.voxel.b);
            } else {
                iPackedColor = voxelColorPaletteFrom.packedColor(entry.voxel);
            }
            sb.append(String.format(Locale.US, "{\"x\":%d,\"y\":%d,\"z\":%d,\"color\":[%d,%d,%d],\"confidence\":%.3f,\"sampleCount\":%.1f}", Integer.valueOf(entry.x), Integer.valueOf(entry.y), Integer.valueOf(entry.z), Integer.valueOf((iPackedColor >> 16) & 255), Integer.valueOf((iPackedColor >> 8) & 255), Integer.valueOf(iPackedColor & 255), Float.valueOf(entry.voxel.confidence()), Float.valueOf(entry.voxel.samples)));
            z = false;
        }
        sb.append("]}");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            fileOutputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
            return file;
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
                throw th;
            } catch (Throwable th2) {
                th.addSuppressed(th2);
                throw th;
            }
        }
    }

    int replaceFromJson(String str) throws Exception {
        JSONObject jSONObject = new JSONObject(str);
        float f = (float) jSONObject.getDouble("voxelSize");
        if (f < 0.005f || f > 1.0f) {
            throw new IllegalArgumentException("Unsupported voxel size in JSON.");
        }
        JSONArray jSONArray = jSONObject.getJSONArray("voxels");
        if (jSONArray.length() > MAX_VOXELS) {
            throw new IllegalArgumentException("JSON contains more than 3000 voxels.");
        }
        ArrayList arrayList = new ArrayList(jSONArray.length());
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject jSONObject2 = jSONArray.getJSONObject(i);
            JSONArray jSONArray2 = jSONObject2.getJSONArray("color");
            if (jSONArray2.length() < 3) {
                throw new IllegalArgumentException("Invalid voxel color at index " + i + ".");
            }
            arrayList.add(new ImportedVoxel(jSONObject2.getInt("x"), jSONObject2.getInt("y"), jSONObject2.getInt("z"), clampColor(jSONArray2.getInt(0)), clampColor(jSONArray2.getInt(1)), clampColor(jSONArray2.getInt(2)), Math.min(30.0f, Math.max(5.0f, (float) jSONObject2.optDouble("sampleCount", 5.0d)))));
        }
        this.dirtyMeshChunks.addAll(this.chunks.keySet());
        this.chunks.clear();
        this.occupied.clear();
        this.labTsdf.clear();
        this.labSurface.clear();
        this.voxelSize = f;
        setAxisYaw((float) Math.toRadians(jSONObject.optDouble("gridAxisYawDegrees", 0.0d)));
        long jCurrentTimeMillis = System.currentTimeMillis();
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            ImportedVoxel importedVoxel = (ImportedVoxel) arrayList.get(i2);
            Voxel voxel = new Voxel(importedVoxel.r, importedVoxel.g, importedVoxel.b, importedVoxel.samples, jCurrentTimeMillis);
            voxel.lastHitFrame = i2;
            putGridVoxel(importedVoxel.x, importedVoxel.y, importedVoxel.z, voxel);
        }
        return arrayList.size();
    }

    private int removeWhere(Filter filter) {
        ArrayList<int[]> arrayList = new ArrayList();
        for (Map.Entry<String, HashMap<String, Voxel>> entry : this.chunks.entrySet()) {
            int[] key = parseKey(entry.getKey());
            for (Map.Entry<String, Voxel> entry2 : entry.getValue().entrySet()) {
                int[] key2 = parseKey(entry2.getKey());
                int i = (key[0] * 16) + key2[0];
                int i2 = (key[1] * 16) + key2[1];
                int i3 = (key[2] * 16) + key2[2];
                if (filter.remove(i, i2, i3, entry2.getValue())) {
                    arrayList.add(new int[]{i, i2, i3});
                }
            }
        }
        for (int[] iArr : arrayList) {
            delete(iArr[0], iArr[1], iArr[2]);
        }
        return arrayList.size();
    }

    private int applyMoves(ArrayList<Move> arrayList) {
        if (arrayList.isEmpty()) {
            return 0;
        }
        for (Move move : arrayList) {
            delete(move.fromX, move.fromY, move.fromZ);
        }
        for (Move move2 : arrayList) {
            mergeVoxel(move2.toX, move2.toY, move2.toZ, move2.voxel);
        }
        return arrayList.size();
    }

    private void mergeVoxel(int i, int i2, int i3, Voxel voxel) {
        String strKey = key(floorDiv(i, 16), floorDiv(i2, 16), floorDiv(i3, 16));
        String strKey2 = key(mod(i, 16), mod(i2, 16), mod(i3, 16));
        HashMap<String, Voxel> map = this.chunks.get(strKey);
        if (map == null) {
            map = new HashMap<>();
            this.chunks.put(strKey, map);
        }
        Voxel voxel2 = map.get(strKey2);
        if (voxel2 == null) {
            map.put(strKey2, voxel);
            this.occupied.add(key(i, i2, i3));
        } else {
            voxel2.add(Math.round(voxel.r), Math.round(voxel.g), Math.round(voxel.b), voxel.samples, voxel.lastSeen, voxel.lastHitFrame);
            voxel2.freeSamples = Math.min(36.0f, voxel2.freeSamples + voxel.freeSamples);
        }
        markVoxelDirty(i, i2, i3);
    }

    private void putGridVoxel(int i, int i2, int i3, Voxel voxel) {
        String strKey = key(floorDiv(i, 16), floorDiv(i2, 16), floorDiv(i3, 16));
        String strKey2 = key(mod(i, 16), mod(i2, 16), mod(i3, 16));
        HashMap<String, Voxel> map = this.chunks.get(strKey);
        if (map == null) {
            map = new HashMap<>();
            this.chunks.put(strKey, map);
        }
        map.put(strKey2, voxel);
        this.occupied.add(key(i, i2, i3));
        markVoxelDirty(i, i2, i3);
    }

    private void delete(int i, int i2, int i3) {
        removeVoxelOnly(i, i2, i3);
    }

    private void removeVoxelOnly(int i, int i2, int i3) {
        String strKey = key(floorDiv(i, 16), floorDiv(i2, 16), floorDiv(i3, 16));
        HashMap<String, Voxel> map = this.chunks.get(strKey);
        if (map == null) {
            return;
        }
        map.remove(key(mod(i, 16), mod(i2, 16), mod(i3, 16)));
        this.occupied.remove(key(i, i2, i3));
        if (map.isEmpty()) {
            this.chunks.remove(strKey);
        }
        markVoxelDirty(i, i2, i3);
    }

    private void observeFree(int i, int i2, int i3, float f, long j, long j2) {
        Voxel voxel = getVoxel(i, i2, i3);
        if (voxel == null) {
            return;
        }
        boolean zIsConfirmed = voxel.isConfirmed(5.0f);
        voxel.observeFree(f, j, j2);
        if (voxel.isContradicted()) {
            delete(i, i2, i3);
        } else if (zIsConfirmed != voxel.isConfirmed(5.0f)) {
            markVoxelDirty(i, i2, i3);
        }
    }

    private int voxelIndex(float f) {
        return (int) Math.floor(f / this.voxelSize);
    }

    float worldX(Entry entry) {
        return gridToWorld((entry.x + 0.5f) * this.voxelSize, (entry.y + 0.5f) * this.voxelSize, (entry.z + 0.5f) * this.voxelSize)[0];
    }

    float worldY(Entry entry) {
        return (entry.y + 0.5f) * this.voxelSize;
    }

    float worldZ(Entry entry) {
        return gridToWorld((entry.x + 0.5f) * this.voxelSize, (entry.y + 0.5f) * this.voxelSize, (entry.z + 0.5f) * this.voxelSize)[2];
    }

    void gridToWorld(float f, float f2, float f3, float[] fArr) {
        float f4 = this.cosAxis;
        float f5 = this.sinAxis;
        fArr[0] = (f * f4) + (f3 * f5);
        fArr[1] = f2;
        fArr[2] = ((-f) * f5) + (f3 * f4);
    }

    private float worldToGridX(float f, float f2) {
        return (f * this.cosAxis) - (f2 * this.sinAxis);
    }

    private float worldToGridZ(float f, float f2) {
        return (f * this.sinAxis) + (f2 * this.cosAxis);
    }

    private float[] gridToWorld(float f, float f2, float f3) {
        float f4 = this.cosAxis;
        float f5 = this.sinAxis;
        return new float[]{(f * f4) + (f3 * f5), f2, ((-f) * f5) + (f3 * f4)};
    }

    private Voxel getVoxel(int i, int i2, int i3) {
        HashMap<String, Voxel> map = this.chunks.get(key(floorDiv(i, 16), floorDiv(i2, 16), floorDiv(i3, 16)));
        if (map == null) {
            return null;
        }
        return map.get(key(mod(i, 16), mod(i2, 16), mod(i3, 16)));
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [boolean, int] */
    private int countNeighbors(int i, int i2, int i3) {
        ?? Contains = this.occupied.contains(key(i + 1, i2, i3));
        int i4 = Contains;
        if (this.occupied.contains(key(i - 1, i2, i3))) {
            i4 = Contains + 1;
        }
        int i5 = i4;
        if (this.occupied.contains(key(i, i2 + 1, i3))) {
            i5 = i4 + 1;
        }
        int i6 = i5;
        if (this.occupied.contains(key(i, i2 - 1, i3))) {
            i6 = i5 + 1;
        }
        int i7 = i6;
        if (this.occupied.contains(key(i, i2, i3 + 1))) {
            i7 = i6 + 1;
        }
        return this.occupied.contains(key(i, i2, i3 + (-1))) ? i7 + 1 : i7;
    }

    private int countConfirmedNeighbors26(int i, int i2, int i3) {
        int i4 = 0;
        for (int i5 = -1; i5 <= 1; i5++) {
            for (int i6 = -1; i6 <= 1; i6++) {
                for (int i7 = -1; i7 <= 1; i7++) {
                    if ((i7 != 0 || i6 != 0 || i5 != 0) && hasConfirmedVoxel(i + i7, i2 + i6, i3 + i5)) {
                        i4++;
                    }
                }
            }
        }
        return i4;
    }

    private void markVoxelDirty(int i, int i2, int i3) {
        int iFloorDiv = floorDiv(i, 16);
        int iFloorDiv2 = floorDiv(i2, 16);
        int iFloorDiv3 = floorDiv(i3, 16);
        this.dirtyMeshChunks.add(key(iFloorDiv, iFloorDiv2, iFloorDiv3));
        if (mod(i, 16) == 0) {
            this.dirtyMeshChunks.add(key(iFloorDiv - 1, iFloorDiv2, iFloorDiv3));
        }
        if (mod(i, 16) == 15) {
            this.dirtyMeshChunks.add(key(iFloorDiv + 1, iFloorDiv2, iFloorDiv3));
        }
        if (mod(i2, 16) == 0) {
            this.dirtyMeshChunks.add(key(iFloorDiv, iFloorDiv2 - 1, iFloorDiv3));
        }
        if (mod(i2, 16) == 15) {
            this.dirtyMeshChunks.add(key(iFloorDiv, iFloorDiv2 + 1, iFloorDiv3));
        }
        if (mod(i3, 16) == 0) {
            this.dirtyMeshChunks.add(key(iFloorDiv, iFloorDiv2, iFloorDiv3 - 1));
        }
        if (mod(i3, 16) == 15) {
            this.dirtyMeshChunks.add(key(iFloorDiv, iFloorDiv2, iFloorDiv3 + 1));
        }
    }

    private static int floorDiv(int i, int i2) {
        return (int) Math.floor(((double) i) / ((double) i2));
    }

    private static int mod(int i, int i2) {
        int i3 = i % i2;
        return i3 < 0 ? i3 + i2 : i3;
    }

    private static String key(int i, int i2, int i3) {
        return i + "," + i2 + "," + i3;
    }

    private static int clampColor(int i) {
        return Math.max(0, Math.min(255, i));
    }

    private static int[] parseKey(String str) {
        String[] strArrSplit = str.split(",");
        return new int[]{Integer.parseInt(strArrSplit[0]), Integer.parseInt(strArrSplit[1]), Integer.parseInt(strArrSplit[2])};
    }

    private static final class TsdfCell {
        float b;
        float colorWeight;
        float distance;
        int freeObservationStreak;
        float freeWeight;
        float g;
        long lastFrame;
        long lastFreeFrame;
        long lastFreeSeen;
        long lastSeen;
        long lastSurfaceSeen;
        float r;
        float weight;

        private TsdfCell() {
            this.lastFrame = Long.MIN_VALUE;
            this.lastFreeFrame = Long.MIN_VALUE;
        }

        boolean integrate(float f, float f2, long j, long j2, int i, int i2, int i3, boolean z) {
            if (this.lastFrame == j2) {
                return false;
            }
            float fMax = Math.max(VoxelWorld.DEFAULT_VOXEL_SIZE, Math.min(2.0f, f2));
            float f3 = this.weight;
            float f4 = f3 + fMax;
            this.distance = ((this.distance * f3) + (f * fMax)) / f4;
            this.weight = Math.min(30.0f, f4);
            if (z) {
                this.freeWeight *= 0.9f;
                this.freeObservationStreak = 0;
                this.lastSurfaceSeen = j;
                if (this.colorWeight >= 5.0f) {
                    float f5 = i - this.r;
                    float f6 = i2 - this.g;
                    float f7 = i3 - this.b;
                    fMax *= Math.max(0.12f, 1.0f - (((float) Math.sqrt(((f5 * f5) + (f6 * f6)) + (f7 * f7))) / 220.0f));
                }
                float f8 = this.colorWeight;
                float f9 = f8 + fMax;
                this.r = ((this.r * f8) + (i * fMax)) / f9;
                this.g = ((this.g * f8) + (i2 * fMax)) / f9;
                this.b = ((this.b * f8) + (i3 * fMax)) / f9;
                this.colorWeight = Math.min(30.0f, f9);
            }
            this.lastSeen = j;
            this.lastFrame = j2;
            return true;
        }

        boolean observeFree(float f, long j, long j2) {
            if (this.lastFreeFrame == j2) {
                return false;
            }
            long j3 = this.lastFreeSeen;
            if (j3 > 0 && j - j3 <= 450) {
                this.freeObservationStreak = Math.min(60, this.freeObservationStreak + 1);
            } else {
                this.freeObservationStreak = 1;
            }
            this.freeWeight = Math.min(36.0f, this.freeWeight + Math.max(0.03f, f));
            this.lastSeen = j;
            this.lastFreeSeen = j;
            this.lastFreeFrame = j2;
            return true;
        }

        boolean isStronglyContradicted(long j) {
            if (this.freeObservationStreak >= 12 && j - this.lastSurfaceSeen > 1500) {
                float f = this.freeWeight;
                if (f >= 12.0f && f > (this.weight * 0.55f) + 5.0f) {
                    return true;
                }
            }
            return false;
        }

        boolean isContradicted(long j) {
            float f = this.freeWeight;
            if (f < 5.0f) {
                return false;
            }
            float f2 = this.weight;
            if (f2 >= 12.0f) {
                return j - this.lastSurfaceSeen > 10000 && f >= 34.0f && f > (f2 * 0.9f) + 6.0f;
            }
            return f > (f2 * 0.75f) + 5.0f;
        }
    }

    private static final class Move {
        final int fromX;
        final int fromY;
        final int fromZ;
        final int toX;
        final int toY;
        final int toZ;
        final Voxel voxel;

        Move(int i, int i2, int i3, int i4, int i5, int i6, Voxel voxel) {
            this.fromX = i;
            this.fromY = i2;
            this.fromZ = i3;
            this.toX = i4;
            this.toY = i5;
            this.toZ = i6;
            this.voxel = voxel;
        }
    }

    private static final class ImportedVoxel {
        final int b;
        final int g;
        final int r;
        final float samples;
        final int x;
        final int y;
        final int z;

        ImportedVoxel(int i, int i2, int i3, int i4, int i5, int i6, float f) {
            this.x = i;
            this.y = i2;
            this.z = i3;
            this.r = i4;
            this.g = i5;
            this.b = i6;
            this.samples = f;
        }
    }

    static final class Entry {
        final Voxel voxel;
        final int x;
        final int y;
        final int z;

        Entry(int i, int i2, int i3, Voxel voxel) {
            this.x = i;
            this.y = i2;
            this.z = i3;
            this.voxel = voxel;
        }
    }
}
