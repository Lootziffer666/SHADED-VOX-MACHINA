package com.voxelscan.android;

import android.opengl.GLES20;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
final class VoxelPointRenderer {
    private static final int FLOATS_PER_VERTEX = 7;
    private static final String FRAGMENT = "precision mediump float;\nvarying vec3 vColor;\nvoid main(){\n  vec2 p = gl_PointCoord - vec2(0.5);\n  if (dot(p, p) > 0.25) discard;\n  gl_FragColor = vec4(vColor, 0.94);\n}";
    private static final String VERTEX = "uniform mat4 uViewProjection;\nattribute vec3 aPosition;\nattribute vec3 aColor;\nattribute float aPointSize;\nvarying vec3 vColor;\nvoid main(){ vColor = aColor; gl_Position = uViewProjection * vec4(aPosition, 1.0); gl_PointSize = aPointSize; }";
    private boolean blockMode;
    private int colorAttrib;
    private int pointSizeAttrib;
    private int positionAttrib;
    private int program;
    private FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private int vertexCount;
    private int viewProjectionUniform;

    VoxelPointRenderer() {
    }

    void create() {
        int iProgram = GlUtil.program(VERTEX, FRAGMENT);
        this.program = iProgram;
        this.positionAttrib = GLES20.glGetAttribLocation(iProgram, "aPosition");
        this.colorAttrib = GLES20.glGetAttribLocation(this.program, "aColor");
        this.pointSizeAttrib = GLES20.glGetAttribLocation(this.program, "aPointSize");
        this.viewProjectionUniform = GLES20.glGetUniformLocation(this.program, "uViewProjection");
    }

    void update(VoxelWorld voxelWorld) {
        update(voxelWorld, null);
    }

    void update(VoxelWorld voxelWorld, VoxelColorPalette voxelColorPalette) {
        int iPackedColor;
        ArrayList<VoxelWorld.Entry> arrayListEntries = voxelWorld.entries();
        int size = arrayListEntries.size();
        this.vertexCount = size;
        int iMax = Math.max(1, size * 7);
        if (this.vertexBuffer.capacity() < iMax) {
            this.vertexBuffer = ByteBuffer.allocateDirect(iMax * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        }
        this.vertexBuffer.position(0);
        for (VoxelWorld.Entry entry : arrayListEntries) {
            this.vertexBuffer.put(voxelWorld.worldX(entry));
            this.vertexBuffer.put(voxelWorld.worldY(entry));
            this.vertexBuffer.put(voxelWorld.worldZ(entry));
            float fConfidence = (entry.voxel.confidence() * 0.55f) + 0.45f;
            if (voxelColorPalette == null) {
                iPackedColor = (Math.round(entry.voxel.r) << 16) | (Math.round(entry.voxel.g) << 8) | Math.round(entry.voxel.b);
            } else {
                iPackedColor = voxelColorPalette.packedColor(entry.voxel);
            }
            this.vertexBuffer.put((((iPackedColor >> 16) & 255) / 255.0f) * fConfidence);
            this.vertexBuffer.put((((iPackedColor >> 8) & 255) / 255.0f) * fConfidence);
            this.vertexBuffer.put(((iPackedColor & 255) / 255.0f) * fConfidence);
            this.vertexBuffer.put(pointSize(entry.voxel));
        }
        this.vertexBuffer.position(0);
    }

    void draw(float[] fArr, int i) {
        if (this.vertexCount == 0) {
            return;
        }
        GLES20.glEnable(2929);
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(770, 771);
        GLES20.glUseProgram(this.program);
        GLES20.glUniformMatrix4fv(this.viewProjectionUniform, 1, false, fArr, 0);
        this.vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(this.positionAttrib, 3, 5126, false, 28, (Buffer) this.vertexBuffer);
        GLES20.glEnableVertexAttribArray(this.positionAttrib);
        this.vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(this.colorAttrib, 3, 5126, false, 28, (Buffer) this.vertexBuffer);
        GLES20.glEnableVertexAttribArray(this.colorAttrib);
        this.vertexBuffer.position(6);
        GLES20.glVertexAttribPointer(this.pointSizeAttrib, 1, 5126, false, 28, (Buffer) this.vertexBuffer);
        GLES20.glEnableVertexAttribArray(this.pointSizeAttrib);
        GLES20.glDrawArrays(0, 0, this.vertexCount);
    }

    void setBlockMode(boolean z) {
        this.blockMode = z;
    }

    private float pointSize(Voxel voxel) {
        float fConfidence = voxel.confidence();
        float fMin = Math.min(1.0f, voxel.samples / 30.0f);
        boolean z = this.blockMode;
        return (z ? 12.0f : 4.0f) + (fConfidence * (z ? 16.0f : 10.0f)) + (fMin * (z ? 10.0f : 8.0f));
    }
}
