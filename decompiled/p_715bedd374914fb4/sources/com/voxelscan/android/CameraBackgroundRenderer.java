package com.voxelscan.android;

import android.opengl.GLES20;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* JADX INFO: loaded from: classes.dex */
final class CameraBackgroundRenderer {
    private static final String FRAGMENT = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nuniform samplerExternalOES uTexture;\nvarying vec2 vTexCoord;\nvoid main(){ gl_FragColor = texture2D(uTexture, vTexCoord); }";
    private static final String VERTEX = "attribute vec2 aPosition;\nattribute vec2 aTexCoord;\nvarying vec2 vTexCoord;\nvoid main(){ vTexCoord = aTexCoord; gl_Position = vec4(aPosition, 0.0, 1.0); }";
    private int positionAttrib;
    private int program;
    private int texAttrib;
    private final FloatBuffer vertices = floatBuffer(new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f});
    private final FloatBuffer textureCoordinates = floatBuffer(new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f});

    CameraBackgroundRenderer() {
    }

    void create() {
        int iProgram = GlUtil.program(VERTEX, FRAGMENT);
        this.program = iProgram;
        this.positionAttrib = GLES20.glGetAttribLocation(iProgram, "aPosition");
        this.texAttrib = GLES20.glGetAttribLocation(this.program, "aTexCoord");
    }

    void draw(int i) {
        GLES20.glDisable(2929);
        GLES20.glDepthMask(false);
        GLES20.glUseProgram(this.program);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, i);
        this.vertices.position(0);
        GLES20.glVertexAttribPointer(this.positionAttrib, 2, 5126, false, 0, (Buffer) this.vertices);
        GLES20.glEnableVertexAttribArray(this.positionAttrib);
        this.textureCoordinates.position(0);
        GLES20.glVertexAttribPointer(this.texAttrib, 2, 5126, false, 0, (Buffer) this.textureCoordinates);
        GLES20.glEnableVertexAttribArray(this.texAttrib);
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDepthMask(true);
        GLES20.glEnable(2929);
    }

    FloatBuffer screenCoordinates() {
        this.vertices.position(0);
        return this.vertices;
    }

    FloatBuffer textureCoordinates() {
        this.textureCoordinates.position(0);
        return this.textureCoordinates;
    }

    private static FloatBuffer floatBuffer(float[] fArr) {
        FloatBuffer floatBufferAsFloatBuffer = ByteBuffer.allocateDirect(fArr.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        floatBufferAsFloatBuffer.put(fArr);
        floatBufferAsFloatBuffer.position(0);
        return floatBufferAsFloatBuffer;
    }
}
