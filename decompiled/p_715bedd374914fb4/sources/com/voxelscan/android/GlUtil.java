package com.voxelscan.android;

import android.opengl.GLES20;
import android.opengl.GLES30;

/* JADX INFO: loaded from: classes.dex */
final class GlUtil {
    private GlUtil() {
    }

    static int createExternalTexture() {
        int[] iArr = new int[1];
        GLES20.glGenTextures(1, iArr, 0);
        GLES20.glBindTexture(36197, iArr[0]);
        GLES20.glTexParameteri(36197, 10241, 9729);
        GLES20.glTexParameteri(36197, 10240, 9729);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        return iArr[0];
    }

    static int program(String str, String str2) {
        int iShader = shader(35633, str);
        int iShader2 = shader(35632, str2);
        int iGlCreateProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(iGlCreateProgram, iShader);
        GLES20.glAttachShader(iGlCreateProgram, iShader2);
        GLES20.glLinkProgram(iGlCreateProgram);
        int[] iArr = new int[1];
        GLES20.glGetProgramiv(iGlCreateProgram, 35714, iArr, 0);
        if (iArr[0] == 0) {
            throw new IllegalStateException(GLES20.glGetProgramInfoLog(iGlCreateProgram));
        }
        GLES20.glDeleteShader(iShader);
        GLES20.glDeleteShader(iShader2);
        return iGlCreateProgram;
    }

    private static int shader(int i, String str) {
        int iGlCreateShader = GLES20.glCreateShader(i);
        GLES20.glShaderSource(iGlCreateShader, str);
        GLES20.glCompileShader(iGlCreateShader);
        int[] iArr = new int[1];
        GLES20.glGetShaderiv(iGlCreateShader, 35713, iArr, 0);
        if (iArr[0] != 0) {
            return iGlCreateShader;
        }
        throw new IllegalStateException(GLES20.glGetShaderInfoLog(iGlCreateShader));
    }

    static void check(String str) {
        int iGlGetError = GLES30.glGetError();
        if (iGlGetError != 0) {
            throw new IllegalStateException(str + " GL error " + iGlGetError);
        }
    }
}
