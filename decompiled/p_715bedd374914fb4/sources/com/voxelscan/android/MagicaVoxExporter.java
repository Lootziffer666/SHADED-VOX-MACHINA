package com.voxelscan.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class MagicaVoxExporter {
    private static final int MAX_AXIS_SIZE = 256;
    private static final int MAX_MODELS = 4096;
    private static final int MAX_USABLE_COLORS = 255;
    private static final int PALETTE_SIZE = 256;

    private MagicaVoxExporter() {
    }

    static File write(VoxelWorld voxelWorld, File file) throws Exception {
        return write(voxelWorld, file, 255);
    }

    static File write(VoxelWorld voxelWorld, File file, int i) throws Exception {
        int i2;
        ArrayList<VoxelWorld.Entry> arrayListEntries = voxelWorld.entries();
        if (arrayListEntries.isEmpty()) {
            throw new IllegalStateException("No confirmed voxels to export.");
        }
        if (arrayListEntries.size() > 2000000) {
            throw new IllegalStateException("This scan is too large for a practical MagicaVoxel export.");
        }
        Bounds bounds = bounds(arrayListEntries);
        int i3 = (bounds.maxX - bounds.minX) + 1;
        int i4 = (bounds.maxZ - bounds.minZ) + 1;
        int i5 = (bounds.maxY - bounds.minY) + 1;
        VoxelColorPalette voxelColorPaletteFrom = VoxelColorPalette.from(arrayListEntries, i);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (i3 <= 256 && i4 <= 256 && i5 <= 256) {
            append(byteArrayOutputStream, chunk("SIZE", ints(i3, i4, i5), new byte[0]));
            append(byteArrayOutputStream, chunk("XYZI", xyzi(arrayListEntries, bounds, voxelColorPaletteFrom), new byte[0]));
            i2 = 150;
        } else {
            ArrayList<Model> arrayListSplitModels = splitModels(arrayListEntries);
            if (arrayListSplitModels.size() > 4096) {
                throw new IllegalStateException("Scene requires " + arrayListSplitModels.size() + " VOX chunks; maximum is 4096.");
            }
            append(byteArrayOutputStream, chunk("PACK", ints(arrayListSplitModels.size()), new byte[0]));
            for (Model model : arrayListSplitModels) {
                append(byteArrayOutputStream, chunk("SIZE", ints(256, 256, 256), new byte[0]));
                append(byteArrayOutputStream, chunk("XYZI", modelXyzi(model, voxelColorPaletteFrom), new byte[0]));
            }
            appendSceneGraph(byteArrayOutputStream, arrayListSplitModels);
            i2 = 200;
        }
        append(byteArrayOutputStream, chunk("RGBA", voxelColorPaletteFrom.magicaVoxelBytes(), new byte[0]));
        byte[] bArrChunk = chunk("MAIN", new byte[0], byteArrayOutputStream.toByteArray());
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        writeString(byteArrayOutputStream2, "VOX ");
        writeIntLE(byteArrayOutputStream2, i2);
        byteArrayOutputStream2.write(bArrChunk);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            fileOutputStream.write(byteArrayOutputStream2.toByteArray());
            fileOutputStream.close();
            return file;
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static byte[] xyzi(ArrayList<VoxelWorld.Entry> arrayList, Bounds bounds, VoxelColorPalette voxelColorPalette) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((arrayList.size() * 4) + 4);
        writeIntLE(byteArrayOutputStream, arrayList.size());
        for (VoxelWorld.Entry entry : arrayList) {
            int i = entry.x - bounds.minX;
            int i2 = bounds.maxZ - entry.z;
            int i3 = entry.y - bounds.minY;
            byteArrayOutputStream.write(i & 255);
            byteArrayOutputStream.write(i2 & 255);
            byteArrayOutputStream.write(i3 & 255);
            byteArrayOutputStream.write(voxelColorPalette.indexFor(entry.voxel) & 255);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static ArrayList<Model> splitModels(ArrayList<VoxelWorld.Entry> arrayList) {
        HashMap map = new HashMap();
        for (VoxelWorld.Entry entry : arrayList) {
            int i = entry.x;
            int i2 = -entry.z;
            int i3 = entry.y;
            int iFloorDiv = Math.floorDiv(i, 256);
            int iFloorDiv2 = Math.floorDiv(i2, 256);
            int iFloorDiv3 = Math.floorDiv(i3, 256);
            String str = iFloorDiv + "," + iFloorDiv2 + "," + iFloorDiv3;
            Model model = (Model) map.get(str);
            if (model == null) {
                model = new Model(iFloorDiv, iFloorDiv2, iFloorDiv3);
                map.put(str, model);
            }
            model.voxels.add(new ModelVoxel(Math.floorMod(i, 256), Math.floorMod(i2, 256), Math.floorMod(i3, 256), entry.voxel));
        }
        ArrayList<Model> arrayList2 = new ArrayList<>((Collection<? extends Model>) map.values());
        arrayList2.sort(new Comparator() { // from class: com.voxelscan.android.MagicaVoxExporter$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return MagicaVoxExporter.lambda$splitModels$0((MagicaVoxExporter.Model) obj, (MagicaVoxExporter.Model) obj2);
            }
        });
        return arrayList2;
    }

    static /* synthetic */ int lambda$splitModels$0(Model model, Model model2) {
        int iCompare = Integer.compare(model.tileX, model2.tileX);
        if (iCompare != 0) {
            return iCompare;
        }
        int iCompare2 = Integer.compare(model.tileY, model2.tileY);
        return iCompare2 != 0 ? iCompare2 : Integer.compare(model.tileZ, model2.tileZ);
    }

    private static byte[] modelXyzi(Model model, VoxelColorPalette voxelColorPalette) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((model.voxels.size() * 4) + 4);
        writeIntLE(byteArrayOutputStream, model.voxels.size());
        for (ModelVoxel modelVoxel : model.voxels) {
            byteArrayOutputStream.write(modelVoxel.x & 255);
            byteArrayOutputStream.write(modelVoxel.y & 255);
            byteArrayOutputStream.write(modelVoxel.z & 255);
            byteArrayOutputStream.write(voxelColorPalette.indexFor(modelVoxel.voxel) & 255);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static void appendSceneGraph(ByteArrayOutputStream byteArrayOutputStream, ArrayList<Model> arrayList) {
        int[] iArr = new int[arrayList.size()];
        int iMin = Integer.MAX_VALUE;
        int iMin2 = Integer.MAX_VALUE;
        int iMin3 = Integer.MAX_VALUE;
        for (int i = 0; i < arrayList.size(); i++) {
            Model model = arrayList.get(i);
            iMin = Math.min(iMin, model.tileX);
            iMin2 = Math.min(iMin2, model.tileY);
            iMin3 = Math.min(iMin3, model.tileZ);
            iArr[i] = (i * 2) + 2;
        }
        append(byteArrayOutputStream, transformNode(0, 1, null, null));
        append(byteArrayOutputStream, groupNode(1, iArr));
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            Model model2 = arrayList.get(i2);
            int i3 = iArr[i2];
            int i4 = i3 + 1;
            append(byteArrayOutputStream, transformNode(i3, i4, "VoxelScan " + model2.tileX + "," + model2.tileY + "," + model2.tileZ, ((model2.tileX - iMin) * 256) + " " + ((model2.tileY - iMin2) * 256) + " " + ((model2.tileZ - iMin3) * 256)));
            append(byteArrayOutputStream, shapeNode(i4, i2));
        }
        append(byteArrayOutputStream, layerChunk());
    }

    private static byte[] transformNode(int i, int i2, String str, String str2) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        append(byteArrayOutputStream, ints(i));
        append(byteArrayOutputStream, str == null ? dict(new String[0]) : dict("_name", str));
        append(byteArrayOutputStream, ints(i2, -1, 0, 1));
        append(byteArrayOutputStream, str2 == null ? dict(new String[0]) : dict("_t", str2));
        return chunk("nTRN", byteArrayOutputStream.toByteArray(), new byte[0]);
    }

    private static byte[] groupNode(int i, int[] iArr) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        append(byteArrayOutputStream, ints(i));
        append(byteArrayOutputStream, dict(new String[0]));
        append(byteArrayOutputStream, ints(iArr.length));
        append(byteArrayOutputStream, ints(iArr));
        return chunk("nGRP", byteArrayOutputStream.toByteArray(), new byte[0]);
    }

    private static byte[] shapeNode(int i, int i2) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        append(byteArrayOutputStream, ints(i));
        append(byteArrayOutputStream, dict(new String[0]));
        append(byteArrayOutputStream, ints(1, i2));
        append(byteArrayOutputStream, dict(new String[0]));
        return chunk("nSHP", byteArrayOutputStream.toByteArray(), new byte[0]);
    }

    private static byte[] layerChunk() {
        return chunk("LAYR", concat(ints(0), dict("_name", "VoxelScan"), ints(-1)), new byte[0]);
    }

    private static byte[] dict(String... strArr) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        writeIntLE(byteArrayOutputStream, strArr.length / 2);
        for (int i = 0; i < strArr.length; i += 2) {
            append(byteArrayOutputStream, voxString(strArr[i]));
            append(byteArrayOutputStream, voxString(strArr[i + 1]));
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] voxString(String str) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        writeIntLE(byteArrayOutputStream, str.length());
        writeString(byteArrayOutputStream, str);
        return byteArrayOutputStream.toByteArray();
    }

    private static void append(ByteArrayOutputStream byteArrayOutputStream, byte[] bArr) {
        byteArrayOutputStream.write(bArr, 0, bArr.length);
    }

    private static Bounds bounds(ArrayList<VoxelWorld.Entry> arrayList) {
        Bounds bounds = new Bounds();
        for (VoxelWorld.Entry entry : arrayList) {
            bounds.minX = Math.min(bounds.minX, entry.x);
            bounds.minY = Math.min(bounds.minY, entry.y);
            bounds.minZ = Math.min(bounds.minZ, entry.z);
            bounds.maxX = Math.max(bounds.maxX, entry.x);
            bounds.maxY = Math.max(bounds.maxY, entry.y);
            bounds.maxZ = Math.max(bounds.maxZ, entry.z);
        }
        return bounds;
    }

    private static byte[] ints(int... iArr) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(iArr.length * 4);
        for (int i : iArr) {
            writeIntLE(byteArrayOutputStream, i);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] chunk(String str, byte[] bArr, byte[] bArr2) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bArr.length + 12 + bArr2.length);
        writeString(byteArrayOutputStream, str);
        writeIntLE(byteArrayOutputStream, bArr.length);
        writeIntLE(byteArrayOutputStream, bArr2.length);
        byteArrayOutputStream.write(bArr, 0, bArr.length);
        byteArrayOutputStream.write(bArr2, 0, bArr2.length);
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] concat(byte[]... bArr) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] bArr2 : bArr) {
            byteArrayOutputStream.write(bArr2, 0, bArr2.length);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static void writeString(ByteArrayOutputStream byteArrayOutputStream, String str) {
        for (int i = 0; i < str.length(); i++) {
            byteArrayOutputStream.write(str.charAt(i));
        }
    }

    private static void writeIntLE(ByteArrayOutputStream byteArrayOutputStream, int i) {
        byteArrayOutputStream.write(i & 255);
        byteArrayOutputStream.write((i >> 8) & 255);
        byteArrayOutputStream.write((i >> 16) & 255);
        byteArrayOutputStream.write((i >> 24) & 255);
    }

    private static final class Bounds {
        int maxX;
        int maxY;
        int maxZ;
        int minX;
        int minY;
        int minZ;

        private Bounds() {
            this.minX = Integer.MAX_VALUE;
            this.minY = Integer.MAX_VALUE;
            this.minZ = Integer.MAX_VALUE;
            this.maxX = Integer.MIN_VALUE;
            this.maxY = Integer.MIN_VALUE;
            this.maxZ = Integer.MIN_VALUE;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class Model {
        final int tileX;
        final int tileY;
        final int tileZ;
        final ArrayList<ModelVoxel> voxels = new ArrayList<>();

        Model(int i, int i2, int i3) {
            this.tileX = i;
            this.tileY = i2;
            this.tileZ = i3;
        }
    }

    private static final class ModelVoxel {
        final Voxel voxel;
        final int x;
        final int y;
        final int z;

        ModelVoxel(int i, int i2, int i3, Voxel voxel) {
            this.x = i;
            this.y = i2;
            this.z = i3;
            this.voxel = voxel;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class AdaptivePalette {
        private static final int BUCKET_BITS = 5;
        private static final int BUCKET_LEVELS = 32;
        private final HashMap<Integer, Integer> bucketToPaletteIndex;
        private final ArrayList<ColorCentroid> colors;

        private AdaptivePalette(ArrayList<ColorCentroid> arrayList, HashMap<Integer, Integer> map) {
            this.colors = arrayList;
            this.bucketToPaletteIndex = map;
        }

        static AdaptivePalette from(ArrayList<VoxelWorld.Entry> arrayList) {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (VoxelWorld.Entry entry : arrayList) {
                int iBucketKey = bucketKey(entry.voxel);
                ColorBucket colorBucket = (ColorBucket) linkedHashMap.get(Integer.valueOf(iBucketKey));
                if (colorBucket == null) {
                    colorBucket = new ColorBucket();
                    linkedHashMap.put(Integer.valueOf(iBucketKey), colorBucket);
                }
                colorBucket.add(entry.voxel.r, entry.voxel.g, entry.voxel.b, Math.max(1.0f, entry.voxel.samples));
            }
            ArrayList arrayList2 = new ArrayList(linkedHashMap.values());
            arrayList2.sort(new Comparator() { // from class: com.voxelscan.android.MagicaVoxExporter$AdaptivePalette$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return Float.compare(((MagicaVoxExporter.AdaptivePalette.ColorBucket) obj2).weight, ((MagicaVoxExporter.AdaptivePalette.ColorBucket) obj).weight);
                }
            });
            ArrayList arrayList3 = new ArrayList();
            HashMap map = new HashMap();
            int iMin = Math.min(255, arrayList2.size());
            int i = 0;
            while (i < iMin) {
                ColorBucket colorBucket2 = (ColorBucket) arrayList2.get(i);
                arrayList3.add(colorBucket2.centroid());
                i++;
                map.put(Integer.valueOf(colorBucket2.key), Integer.valueOf(i));
            }
            if (arrayList3.isEmpty()) {
                arrayList3.add(new ColorCentroid(128, 128, 128));
            }
            for (Map.Entry entry2 : linkedHashMap.entrySet()) {
                if (!map.containsKey(entry2.getKey())) {
                    map.put((Integer) entry2.getKey(), Integer.valueOf(nearestIndex(((ColorBucket) entry2.getValue()).centroid(), arrayList3)));
                }
            }
            return new AdaptivePalette(arrayList3, map);
        }

        int indexFor(Voxel voxel) {
            Integer num = this.bucketToPaletteIndex.get(Integer.valueOf(bucketKey(voxel)));
            if (num != null) {
                return num.intValue();
            }
            return nearestIndex(new ColorCentroid(Math.round(voxel.r), Math.round(voxel.g), Math.round(voxel.b)), this.colors);
        }

        byte[] bytes() {
            ColorCentroid colorCentroid;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            for (int i = 0; i < 255; i++) {
                if (i < this.colors.size()) {
                    colorCentroid = this.colors.get(i);
                } else {
                    ArrayList<ColorCentroid> arrayList = this.colors;
                    colorCentroid = arrayList.get(arrayList.size() - 1);
                }
                ColorCentroid colorCentroid2 = colorCentroid;
                byteArrayOutputStream.write(colorCentroid2.r);
                byteArrayOutputStream.write(colorCentroid2.g);
                byteArrayOutputStream.write(colorCentroid2.b);
                byteArrayOutputStream.write(255);
            }
            byteArrayOutputStream.write(0);
            byteArrayOutputStream.write(0);
            byteArrayOutputStream.write(0);
            byteArrayOutputStream.write(0);
            return byteArrayOutputStream.toByteArray();
        }

        private static int nearestIndex(ColorCentroid colorCentroid, ArrayList<ColorCentroid> arrayList) {
            int i = 1;
            int i2 = Integer.MAX_VALUE;
            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                ColorCentroid colorCentroid2 = arrayList.get(i3);
                int i4 = colorCentroid.r - colorCentroid2.r;
                int i5 = colorCentroid.g - colorCentroid2.g;
                int i6 = colorCentroid.b - colorCentroid2.b;
                int i7 = (i4 * i4) + (i5 * i5) + (i6 * i6);
                if (i7 < i2) {
                    i = i3 + 1;
                    i2 = i7;
                }
            }
            return i;
        }

        private static int bucketKey(Voxel voxel) {
            return Math.max(0, Math.min(31, Math.round((voxel.b * 31.0f) / 255.0f))) | (Math.max(0, Math.min(31, Math.round((voxel.r * 31.0f) / 255.0f))) << 10) | (Math.max(0, Math.min(31, Math.round((voxel.g * 31.0f) / 255.0f))) << 5);
        }

        /* JADX INFO: Access modifiers changed from: private */
        static final class ColorBucket {
            float b;
            float g;
            int key;
            float r;
            float weight;

            private ColorBucket() {
            }

            void add(float f, float f2, float f3, float f4) {
                if (this.weight == 0.0f) {
                    this.key = Math.max(0, Math.min(31, Math.round((31.0f * f3) / 255.0f))) | (Math.max(0, Math.min(31, Math.round((f * 31.0f) / 255.0f))) << 10) | (Math.max(0, Math.min(31, Math.round((f2 * 31.0f) / 255.0f))) << 5);
                }
                this.r += f * f4;
                this.g += f2 * f4;
                this.b += f3 * f4;
                this.weight += f4;
            }

            ColorCentroid centroid() {
                return new ColorCentroid(Math.round(this.r / this.weight), Math.round(this.g / this.weight), Math.round(this.b / this.weight));
            }
        }

        private static final class ColorCentroid {
            final int b;
            final int g;
            final int r;

            ColorCentroid(int i, int i2, int i3) {
                this.r = Math.max(0, Math.min(255, i));
                this.g = Math.max(0, Math.min(255, i2));
                this.b = Math.max(0, Math.min(255, i3));
            }
        }
    }
}
