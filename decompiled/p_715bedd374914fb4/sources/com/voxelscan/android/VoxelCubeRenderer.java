package com.voxelscan.android;

import android.opengl.GLES20;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
final class VoxelCubeRenderer {
    private static final int FLOATS_PER_VERTEX = 6;
    private static final String FRAGMENT = "precision mediump float;\nvarying vec3 vColor;\nvoid main(){ gl_FragColor = vec4(vColor, 1.0); }";
    private static final String VERTEX = "uniform mat4 uViewProjection;\nattribute vec3 aPosition;\nattribute vec3 aColor;\nvarying vec3 vColor;\nvoid main(){ vColor = aColor; gl_Position = uViewProjection * vec4(aPosition, 1.0); }";
    private int colorAttrib;
    private int positionAttrib;
    private int program;
    private int viewProjectionUniform;
    private static final int[][] NEIGHBORS = {new int[]{1, 0, 0}, new int[]{-1, 0, 0}, new int[]{0, 1, 0}, new int[]{0, -1, 0}, new int[]{0, 0, 1}, new int[]{0, 0, -1}};
    private static final float[] FACE_LIGHT = {0.86f, 0.72f, 1.0f, 0.58f, 0.92f, 0.68f};
    private static final float[][][] FACES = {new float[][]{new float[]{1.0f, -1.0f, -1.0f}, new float[]{1.0f, 1.0f, -1.0f}, new float[]{1.0f, 1.0f, 1.0f}, new float[]{1.0f, -1.0f, -1.0f}, new float[]{1.0f, 1.0f, 1.0f}, new float[]{1.0f, -1.0f, 1.0f}}, new float[][]{new float[]{-1.0f, -1.0f, 1.0f}, new float[]{-1.0f, 1.0f, 1.0f}, new float[]{-1.0f, 1.0f, -1.0f}, new float[]{-1.0f, -1.0f, 1.0f}, new float[]{-1.0f, 1.0f, -1.0f}, new float[]{-1.0f, -1.0f, -1.0f}}, new float[][]{new float[]{-1.0f, 1.0f, -1.0f}, new float[]{-1.0f, 1.0f, 1.0f}, new float[]{1.0f, 1.0f, 1.0f}, new float[]{-1.0f, 1.0f, -1.0f}, new float[]{1.0f, 1.0f, 1.0f}, new float[]{1.0f, 1.0f, -1.0f}}, new float[][]{new float[]{-1.0f, -1.0f, 1.0f}, new float[]{-1.0f, -1.0f, -1.0f}, new float[]{1.0f, -1.0f, -1.0f}, new float[]{-1.0f, -1.0f, 1.0f}, new float[]{1.0f, -1.0f, -1.0f}, new float[]{1.0f, -1.0f, 1.0f}}, new float[][]{new float[]{1.0f, -1.0f, 1.0f}, new float[]{1.0f, 1.0f, 1.0f}, new float[]{-1.0f, 1.0f, 1.0f}, new float[]{1.0f, -1.0f, 1.0f}, new float[]{-1.0f, 1.0f, 1.0f}, new float[]{-1.0f, -1.0f, 1.0f}}, new float[][]{new float[]{-1.0f, -1.0f, -1.0f}, new float[]{-1.0f, 1.0f, -1.0f}, new float[]{1.0f, 1.0f, -1.0f}, new float[]{-1.0f, -1.0f, -1.0f}, new float[]{1.0f, 1.0f, -1.0f}, new float[]{1.0f, -1.0f, -1.0f}}};
    private final HashMap<String, ChunkMesh> meshes = new HashMap<>();
    private final float[] worldPoint = new float[3];

    VoxelCubeRenderer() {
    }

    void create() {
        int iProgram = GlUtil.program(VERTEX, FRAGMENT);
        this.program = iProgram;
        this.positionAttrib = GLES20.glGetAttribLocation(iProgram, "aPosition");
        this.colorAttrib = GLES20.glGetAttribLocation(this.program, "aColor");
        this.viewProjectionUniform = GLES20.glGetUniformLocation(this.program, "uViewProjection");
    }

    void rebuildAll(VoxelWorld voxelWorld) {
        rebuildAll(voxelWorld, null);
    }

    void rebuildAll(VoxelWorld voxelWorld, VoxelColorPalette voxelColorPalette) {
        voxelWorld.markAllMeshDirty();
        while (updateDirty(voxelWorld, 64, voxelColorPalette) > 0) {
        }
    }

    int updateDirty(VoxelWorld voxelWorld, int i) {
        return updateDirty(voxelWorld, i, null);
    }

    int updateDirty(VoxelWorld voxelWorld, int i, VoxelColorPalette voxelColorPalette) {
        ArrayList<String> arrayListConsumeDirtyMeshChunks = voxelWorld.consumeDirtyMeshChunks(i);
        Iterator<String> it = arrayListConsumeDirtyMeshChunks.iterator();
        while (it.hasNext()) {
            rebuildChunk(voxelWorld, it.next(), voxelColorPalette);
        }
        return arrayListConsumeDirtyMeshChunks.size();
    }

    private void rebuildChunk(VoxelWorld voxelWorld, String str, VoxelColorPalette voxelColorPalette) {
        int iPackedColor;
        float f;
        ArrayList<VoxelWorld.Entry> arrayListConfirmedEntriesForChunk = voxelWorld.confirmedEntriesForChunk(str);
        Iterator<VoxelWorld.Entry> it = arrayListConfirmedEntriesForChunk.iterator();
        int i = 0;
        int i2 = 0;
        while (true) {
            char c = 1;
            if (!it.hasNext()) {
                break;
            }
            VoxelWorld.Entry next = it.next();
            int[][] iArr = NEIGHBORS;
            int length = iArr.length;
            int i3 = i;
            while (i3 < length) {
                int[] iArr2 = iArr[i3];
                if (!voxelWorld.hasConfirmedVoxel(next.x + iArr2[i], next.y + iArr2[c], next.z + iArr2[2])) {
                    i2++;
                }
                i3++;
                i = 0;
                c = 1;
            }
        }
        if (i2 == 0) {
            this.meshes.remove(str);
            return;
        }
        int i4 = i2 * 6;
        FloatBuffer floatBufferAsFloatBuffer = ByteBuffer.allocateDirect(i2 * 144).order(ByteOrder.nativeOrder()).asFloatBuffer();
        float fVoxelSize = voxelWorld.voxelSize();
        float f2 = 0.5f;
        float f3 = fVoxelSize * 0.5f;
        Iterator<VoxelWorld.Entry> it2 = arrayListConfirmedEntriesForChunk.iterator();
        while (it2.hasNext()) {
            VoxelWorld.Entry next2 = it2.next();
            float f4 = (next2.x + f2) * fVoxelSize;
            float f5 = next2.y * fVoxelSize;
            float f6 = (next2.z + f2) * fVoxelSize;
            if (voxelColorPalette == 0) {
                iPackedColor = (Math.round(next2.voxel.g) << 8) | (Math.round(next2.voxel.r) << 16) | Math.round(next2.voxel.b);
            } else {
                iPackedColor = voxelColorPalette.packedColor(next2.voxel);
            }
            int i5 = 0;
            while (true) {
                int[][] iArr3 = NEIGHBORS;
                if (i5 < iArr3.length) {
                    int[] iArr4 = iArr3[i5];
                    Iterator<VoxelWorld.Entry> it3 = it2;
                    float f7 = fVoxelSize;
                    if (voxelWorld.hasConfirmedVoxel(next2.x + iArr4[0], next2.y + iArr4[1], next2.z + iArr4[2])) {
                        f = f3;
                    } else {
                        float f8 = FACE_LIGHT[i5];
                        float f9 = (((iPackedColor >> 16) & 255) / 255.0f) * f8;
                        float f10 = (((iPackedColor >> 8) & 255) / 255.0f) * f8;
                        float f11 = ((iPackedColor & 255) / 255.0f) * f8;
                        float[][] fArr = FACES[i5];
                        int length2 = fArr.length;
                        int i6 = 0;
                        while (i6 < length2) {
                            float[] fArr2 = fArr[i6];
                            float[][] fArr3 = fArr;
                            int i7 = length2;
                            float f12 = f3;
                            voxelWorld.gridToWorld(f4 + (fArr2[0] * f3), f5 + (fArr2[1] * f3), f6 + (fArr2[2] * f3), this.worldPoint);
                            floatBufferAsFloatBuffer.put(this.worldPoint[0]);
                            floatBufferAsFloatBuffer.put(this.worldPoint[1]);
                            floatBufferAsFloatBuffer.put(this.worldPoint[2]);
                            floatBufferAsFloatBuffer.put(f9);
                            floatBufferAsFloatBuffer.put(f10);
                            floatBufferAsFloatBuffer.put(f11);
                            i6++;
                            f4 = f4;
                            f3 = f12;
                            fArr = fArr3;
                            length2 = i7;
                        }
                        f = f3;
                    }
                    i5++;
                    it2 = it3;
                    fVoxelSize = f7;
                    next2 = next2;
                    iPackedColor = iPackedColor;
                    f4 = f4;
                    f3 = f;
                }
            }
            f2 = 0.5f;
        }
        floatBufferAsFloatBuffer.position(0);
        this.meshes.put(str, new ChunkMesh(floatBufferAsFloatBuffer, i4));
    }

    void draw(float[] fArr) {
        if (this.meshes.isEmpty()) {
            return;
        }
        GLES20.glEnable(2929);
        GLES20.glDisable(3042);
        GLES20.glUseProgram(this.program);
        GLES20.glUniformMatrix4fv(this.viewProjectionUniform, 1, false, fArr, 0);
        Iterator<Map.Entry<String, ChunkMesh>> it = this.meshes.entrySet().iterator();
        while (it.hasNext()) {
            ChunkMesh value = it.next().getValue();
            value.vertices.position(0);
            GLES20.glVertexAttribPointer(this.positionAttrib, 3, 5126, false, 24, (Buffer) value.vertices);
            GLES20.glEnableVertexAttribArray(this.positionAttrib);
            value.vertices.position(3);
            GLES20.glVertexAttribPointer(this.colorAttrib, 3, 5126, false, 24, (Buffer) value.vertices);
            GLES20.glEnableVertexAttribArray(this.colorAttrib);
            GLES20.glDrawArrays(4, 0, value.vertexCount);
        }
    }

    private static final class ChunkMesh {
        final int vertexCount;
        final FloatBuffer vertices;

        ChunkMesh(FloatBuffer floatBuffer, int i) {
            this.vertices = floatBuffer;
            this.vertexCount = i;
        }
    }
}
