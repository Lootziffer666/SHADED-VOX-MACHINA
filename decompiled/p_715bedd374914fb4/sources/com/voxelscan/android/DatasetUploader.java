package com.voxelscan.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class DatasetUploader {

    interface Callback {
        void onComplete(boolean z, String str);
    }

    DatasetUploader() {
    }

    static void upload(final File file, final Callback callback) {
        new Thread(new Runnable() { // from class: com.voxelscan.android.DatasetUploader$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DatasetUploader.performUpload(file, callback);
            }
        }, "VoxelScan-dataset-uploader").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void performUpload(File file, Callback callback) {
        try {
            throw new IllegalStateException("Owner upload is not configured");
        } catch (Exception e) {
            callback.onComplete(false, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static void validateDataset(File file) throws Exception {
        if (file == null || !file.isFile()) {
            throw new IllegalStateException("Dataset file is missing");
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            ZipEntry entry = zipFile.getEntry("manifest.json");
            ZipEntry entry2 = zipFile.getEntry("frames.jsonl");
            if (entry == null || entry2 == null) {
                throw new IllegalStateException("Dataset archive is incomplete");
            }
            if (new JSONObject(readResponse(zipFile.getInputStream(entry))).optInt("frameCount", 0) <= 0) {
                throw new IllegalStateException("Dataset contains no frames");
            }
            zipFile.close();
        } catch (Throwable th) {
            try {
                zipFile.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static String readResponse(InputStream inputStream) throws Exception {
        int i;
        if (inputStream == null) {
            return "";
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byte[] bArr = new byte[4096];
                while (byteArrayOutputStream.size() < 16384 && (i = inputStream.read(bArr)) >= 0) {
                    byteArrayOutputStream.write(bArr, 0, i);
                }
                String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                byteArrayOutputStream.close();
                if (inputStream != null) {
                    inputStream.close();
                }
                return string;
            } catch (Throwable th) {
                try {
                    byteArrayOutputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (Throwable th3) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th4) {
                    th3.addSuppressed(th4);
                }
            }
            throw th3;
        }
    }
}
