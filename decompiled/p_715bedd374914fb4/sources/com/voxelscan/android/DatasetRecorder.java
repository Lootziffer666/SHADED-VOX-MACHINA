package com.voxelscan.android;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import kotlin.UShort;
import kotlin.jvm.internal.ByteCompanionObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class DatasetRecorder {
    private static final long CAPTURE_INTERVAL_NANOS = 500000000;
    private float axisYaw;
    private volatile boolean finalizing;
    private volatile File lastArchive;
    private String lastError;
    private float maxDepth;
    private int nextIndex;
    private volatile boolean recording;
    private final File root;
    private String scanMode;
    private File sessionDir;
    private long startedAt;
    private float voxelSize;
    private final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue(6), new ThreadFactory() { // from class: com.voxelscan.android.DatasetRecorder$$ExternalSyntheticLambda2
        @Override // java.util.concurrent.ThreadFactory
        public final Thread newThread(Runnable runnable) {
            return DatasetRecorder.lambda$new$0(runnable);
        }
    });
    private final AtomicInteger written = new AtomicInteger();
    private final AtomicInteger dropped = new AtomicInteger();
    private long lastCaptureNanos = Long.MIN_VALUE;

    interface StopCallback {
        void onStopped(File file, int i, int i2, String str);
    }

    static /* synthetic */ Thread lambda$new$0(Runnable runnable) {
        Thread thread = new Thread(runnable, "VoxelScan-dataset-writer");
        thread.setDaemon(true);
        return thread;
    }

    DatasetRecorder(File file) {
        File file2 = new File(file, "datasets");
        this.root = file2;
        File[] fileArrListFiles = file2.listFiles(new FilenameFilter() { // from class: com.voxelscan.android.DatasetRecorder$$ExternalSyntheticLambda3
            @Override // java.io.FilenameFilter
            public final boolean accept(File file3, String str) {
                return str.endsWith(".vscan");
            }
        });
        if (fileArrListFiles != null) {
            for (File file3 : fileArrListFiles) {
                if (this.lastArchive == null || file3.lastModified() > this.lastArchive.lastModified()) {
                    this.lastArchive = file3;
                }
            }
        }
    }

    synchronized boolean start(float f, float f2, float f3, String str) {
        if (!this.recording && !this.finalizing) {
            if (!this.root.exists() && !this.root.mkdirs()) {
                return false;
            }
            this.startedAt = System.currentTimeMillis();
            File file = new File(this.root, "session-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(new Date(this.startedAt)));
            this.sessionDir = file;
            if (!file.mkdirs()) {
                return false;
            }
            try {
                writeText(new File(this.sessionDir, "frames.jsonl"), "");
                this.voxelSize = f;
                this.maxDepth = f2;
                this.axisYaw = f3;
                this.scanMode = str;
                this.nextIndex = 0;
                this.lastCaptureNanos = Long.MIN_VALUE;
                this.lastError = null;
                this.written.set(0);
                this.dropped.set(0);
                this.recording = true;
                return true;
            } catch (IOException e) {
                delete(this.sessionDir);
                this.sessionDir = null;
                this.lastError = e.getMessage();
                return false;
            }
        }
        return false;
    }

    synchronized void capture(long j, long j2, long j3, int i, float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4, int[] iArr, Image image, Image image2, Image image3) {
        long j4;
        if (this.recording && image != null) {
            long jNanoTime = System.nanoTime();
            long j5 = this.lastCaptureNanos;
            if (j5 == Long.MIN_VALUE || jNanoTime - j5 >= CAPTURE_INTERVAL_NANOS) {
                if (this.writer.getQueue().remainingCapacity() <= 1) {
                    this.dropped.incrementAndGet();
                    this.lastCaptureNanos = jNanoTime;
                    return;
                }
                try {
                    int i2 = 1 + this.nextIndex;
                    this.nextIndex = i2;
                    j4 = jNanoTime;
                    try {
                        final FrameData frameDataCopy = FrameData.copy(i2, j, j2, j3, System.currentTimeMillis(), i, fArr, fArr2, fArr3, fArr4, iArr, image, image2, image3);
                        this.writer.execute(new Runnable() { // from class: com.voxelscan.android.DatasetRecorder$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() throws Throwable {
                                this.f$0.lambda$capture$2(frameDataCopy);
                            }
                        });
                    } catch (Exception e) {
                        e = e;
                        this.dropped.incrementAndGet();
                        this.lastError = e.getMessage();
                    }
                } catch (Exception e2) {
                    e = e2;
                    j4 = jNanoTime;
                }
                this.lastCaptureNanos = j4;
            }
        }
    }

    synchronized boolean stop(final StopCallback stopCallback) {
        if (this.recording && !this.finalizing) {
            this.recording = false;
            this.finalizing = true;
            final File file = this.sessionDir;
            this.writer.execute(new Runnable() { // from class: com.voxelscan.android.DatasetRecorder$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() throws Throwable {
                    this.f$0.lambda$stop$3(file, stopCallback);
                }
            });
            return true;
        }
        return false;
    }

    boolean isRecording() {
        return this.recording;
    }

    boolean isFinalizing() {
        return this.finalizing;
    }

    int frameCount() {
        return this.written.get();
    }

    int droppedCount() {
        return this.dropped.get();
    }

    File lastArchive() {
        if (this.lastArchive == null || !this.lastArchive.isFile()) {
            return null;
        }
        return this.lastArchive;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v0, types: [java.lang.String] */
    /* JADX WARN: Type inference failed for: r2v1 */
    /* JADX WARN: Type inference failed for: r2v15, types: [com.voxelscan.android.DatasetRecorder] */
    /* JADX WARN: Type inference failed for: r2v16 */
    /* JADX WARN: Type inference failed for: r2v2 */
    /* JADX WARN: Type inference failed for: r2v3, types: [com.voxelscan.android.DatasetRecorder] */
    /* JADX WARN: Type inference failed for: r2v4 */
    /* JADX WARN: Type inference failed for: r2v5 */
    /* JADX WARN: Type inference failed for: r2v6 */
    /* JADX WARN: Type inference failed for: r2v7 */
    /* JADX INFO: renamed from: writeFrame, reason: merged with bridge method [inline-methods] */
    public void lambda$capture$2(FrameData frameData) throws Throwable {
        ?? r2 = "confidence";
        try {
            try {
                File fileDirectory = directory("rgb");
                File fileDirectory2 = directory("depth");
                File fileDirectory3 = directory("confidence");
                String str = String.format(Locale.US, "%06d", Integer.valueOf(frameData.index));
                File file = new File(fileDirectory, str + ".jpg");
                File file2 = new File(fileDirectory2, str + ".depth16");
                File file3 = new File(fileDirectory3, str + ".conf8");
                YuvImage yuvImage = new YuvImage(frameData.nv21, 17, frameData.imageWidth, frameData.imageHeight, null);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                try {
                    try {
                        if (!yuvImage.compressToJpeg(new Rect(0, 0, frameData.imageWidth, frameData.imageHeight), 90, bufferedOutputStream)) {
                            r2 = this;
                            try {
                                throw new IOException("JPEG compression failed");
                            } catch (Throwable th) {
                                th = th;
                                Throwable th2 = th;
                                try {
                                    bufferedOutputStream.close();
                                    throw th2;
                                } catch (Throwable th3) {
                                    th2.addSuppressed(th3);
                                    throw th2;
                                }
                            }
                        }
                        try {
                            bufferedOutputStream.close();
                            writeBytes(file2, frameData.depth16);
                            writeBytes(file3, frameData.confidence8);
                            JSONObject jSONObject = new JSONObject();
                            jSONObject.put("index", frameData.index);
                            jSONObject.put("frameTimestampNanos", frameData.frameTime);
                            jSONObject.put("depthTimestampNanos", frameData.depthTime);
                            jSONObject.put("cameraTimestampNanos", frameData.cameraTime);
                            jSONObject.put("captureTimeMillis", frameData.captureTime);
                            jSONObject.put("displayRotation", frameData.displayRotation);
                            jSONObject.put("rgb", "rgb/" + file.getName());
                            jSONObject.put("depth", "depth/" + file2.getName());
                            jSONObject.put("confidence", "confidence/" + file3.getName());
                            jSONObject.put("imageSize", ints(frameData.imageWidth, frameData.imageHeight));
                            jSONObject.put("depthSize", ints(frameData.depthWidth, frameData.depthHeight));
                            jSONObject.put("intrinsicsImageSize", ints(frameData.intrinsicsWidth, frameData.intrinsicsHeight));
                            jSONObject.put("focalLength", floats(frameData.fx, frameData.fy));
                            jSONObject.put("principalPoint", floats(frameData.cx, frameData.cy));
                            jSONObject.put("translation", floats(frameData.tx, frameData.ty, frameData.tz));
                            jSONObject.put("rotationQuaternion", floats(frameData.qx, frameData.qy, frameData.qz, frameData.qw));
                            r2 = this;
                            FileWriter fileWriter = new FileWriter(new File(r2.sessionDir, "frames.jsonl"), true);
                            try {
                                fileWriter.write(jSONObject.toString());
                                fileWriter.write(10);
                                fileWriter.close();
                                r2.written.incrementAndGet();
                            } catch (Throwable th4) {
                                try {
                                    fileWriter.close();
                                    throw th4;
                                } catch (Throwable th5) {
                                    th4.addSuppressed(th5);
                                    throw th4;
                                }
                            }
                        } catch (Exception e) {
                            e = e;
                            r2 = this;
                            r2.dropped.incrementAndGet();
                            r2.lastError = e.getMessage();
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        r2 = this;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    r2 = this;
                }
            } catch (Exception e2) {
                e = e2;
                r2 = this;
            }
        } catch (Exception e3) {
            e = e3;
            r2.dropped.incrementAndGet();
            r2.lastError = e.getMessage();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code duplicated, block: B:43:0x0115  */
    /* JADX WARN: Code duplicated, block: B:48:0x010e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX INFO: renamed from: finish, reason: merged with bridge method [inline-methods] */
    public void lambda$stop$3(File file, StopCallback stopCallback) throws Throwable {
        String str;
        File file2;
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("format", "VoxelScan Dataset");
            jSONObject.put("version", 1);
            jSONObject.put("createdAtMillis", this.startedAt);
            jSONObject.put("completedAtMillis", System.currentTimeMillis());
            jSONObject.put("frameCount", this.written.get());
            jSONObject.put("droppedFrames", this.dropped.get());
            jSONObject.put("scanMode", this.scanMode);
            jSONObject.put("voxelSizeMeters", this.voxelSize);
            jSONObject.put("maxDepthMeters", this.maxDepth);
            jSONObject.put("axisYawDegrees", this.axisYaw);
            jSONObject.put("coordinateSystem", "ARCore world; right-handed; camera forward is -Z");
            jSONObject.put("depthEncoding", "little-endian uint16 millimeters; tightly packed rows");
            jSONObject.put("confidenceEncoding", "uint8 0-255; tightly packed rows");
            String str2 = this.lastError;
            if (str2 != null) {
                jSONObject.put("lastWriteError", str2);
            }
            writeText(new File(file, "manifest.json"), jSONObject.toString(2));
            file2 = new File(this.root, "VoxelScan-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(this.startedAt)) + ".vscan");
            try {
                try {
                    zip(file, file2);
                    this.lastArchive = file2;
                    delete(file);
                    synchronized (this) {
                        this.sessionDir = null;
                        this.finalizing = false;
                    }
                    if (stopCallback != null) {
                        stopCallback.onStopped(file2, this.written.get(), this.dropped.get(), null);
                    }
                } catch (Exception e) {
                    e = e;
                    String message = e.getMessage();
                    if (file2 != null) {
                        try {
                            file2.delete();
                        } catch (Throwable th) {
                            str = message;
                            th = th;
                            synchronized (this) {
                                this.sessionDir = null;
                                this.finalizing = false;
                            }
                            if (stopCallback != null) {
                                stopCallback.onStopped(file2, this.written.get(), this.dropped.get(), str);
                            }
                            throw th;
                        }
                    }
                    synchronized (this) {
                        this.sessionDir = null;
                        this.finalizing = false;
                    }
                    if (stopCallback != null) {
                        stopCallback.onStopped(null, this.written.get(), this.dropped.get(), message);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                str = null;
                synchronized (this) {
                    this.sessionDir = null;
                    this.finalizing = false;
                    if (stopCallback != null) {
                        stopCallback.onStopped(file2, this.written.get(), this.dropped.get(), str);
                    }
                    throw th;
                }
            }
        } catch (Exception e2) {
            e = e2;
            file2 = null;
        } catch (Throwable th3) {
            th = th3;
            str = null;
            file2 = null;
            synchronized (this) {
                this.sessionDir = null;
                this.finalizing = false;
                if (stopCallback != null) {
                    stopCallback.onStopped(file2, this.written.get(), this.dropped.get(), str);
                }
                throw th;
            }
        }
    }

    private File directory(String str) throws IOException {
        File file = new File(this.sessionDir, str);
        if (file.exists() || file.mkdirs()) {
            return file;
        }
        throw new IOException("Cannot create " + str);
    }

    private static void writeBytes(File file, byte[] bArr) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
        try {
            bufferedOutputStream.write(bArr);
            bufferedOutputStream.close();
        } catch (Throwable th) {
            try {
                bufferedOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void writeText(File file, String str) throws IOException {
        FileWriter fileWriter = new FileWriter(file, false);
        try {
            fileWriter.write(str);
            fileWriter.close();
        } catch (Throwable th) {
            try {
                fileWriter.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void zip(File file, File file2) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file2)));
        try {
            zipOutputStream.setLevel(1);
            addZip(zipOutputStream, file, file);
            zipOutputStream.close();
        } catch (Throwable th) {
            try {
                zipOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void addZip(ZipOutputStream zipOutputStream, File file, File file2) throws IOException {
        File[] fileArrListFiles = file2.listFiles();
        if (fileArrListFiles != null) {
            for (File file3 : fileArrListFiles) {
                addZip(zipOutputStream, file, file3);
            }
            return;
        }
        zipOutputStream.putNextEntry(new ZipEntry(file.toURI().relativize(file2.toURI()).getPath()));
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file2));
        try {
            byte[] bArr = new byte[65536];
            while (true) {
                int i = bufferedInputStream.read(bArr);
                if (i < 0) {
                    bufferedInputStream.close();
                    zipOutputStream.closeEntry();
                    return;
                }
                zipOutputStream.write(bArr, 0, i);
            }
        } catch (Throwable th) {
            try {
                bufferedInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    private static void delete(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                delete(file2);
            }
        }
        file.delete();
    }

    private static JSONArray floats(float... fArr) throws JSONException {
        JSONArray jSONArray = new JSONArray();
        for (float f : fArr) {
            jSONArray.put(f);
        }
        return jSONArray;
    }

    private static JSONArray ints(int... iArr) {
        JSONArray jSONArray = new JSONArray();
        for (int i : iArr) {
            jSONArray.put(i);
        }
        return jSONArray;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte[] depth16(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer byteBufferOrder = plane.getBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int iPosition = byteBufferOrder.position();
        byte[] bArr = new byte[width * height * 2];
        int i = 0;
        for (int i2 = 0; i2 < height; i2++) {
            for (int i3 = 0; i3 < width; i3++) {
                int rowStride = (plane.getRowStride() * i2) + iPosition + (plane.getPixelStride() * i3);
                int i4 = rowStride + 1 < byteBufferOrder.limit() ? byteBufferOrder.getShort(rowStride) & UShort.MAX_VALUE : 0;
                int i5 = i + 1;
                bArr[i] = (byte) i4;
                i += 2;
                bArr[i5] = (byte) (i4 >>> 8);
            }
        }
        return bArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte[] confidence8(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer byteBufferDuplicate = plane.getBuffer().duplicate();
        int iPosition = byteBufferDuplicate.position();
        byte[] bArr = new byte[width * height];
        int i = 0;
        for (int i2 = 0; i2 < height; i2++) {
            int i3 = 0;
            while (i3 < width) {
                int rowStride = (plane.getRowStride() * i2) + iPosition + (plane.getPixelStride() * i3);
                int i4 = i + 1;
                bArr[i] = rowStride < byteBufferDuplicate.limit() ? byteBufferDuplicate.get(rowStride) : (byte) 0;
                i3++;
                i = i4;
            }
        }
        return bArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte[] nv21(Image image) {
        if (image.getFormat() != 35) {
            throw new IllegalArgumentException("Expected YUV camera image");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if ((width & 1) != 0 || (height & 1) != 0) {
            throw new IllegalArgumentException("Odd camera dimensions");
        }
        int i = width * height;
        byte[] bArr = new byte[(i * 3) / 2];
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer byteBufferDuplicate = plane.getBuffer().duplicate();
        int iPosition = byteBufferDuplicate.position();
        for (int i2 = 0; i2 < height; i2++) {
            for (int i3 = 0; i3 < width; i3++) {
                int rowStride = (plane.getRowStride() * i2) + iPosition + (plane.getPixelStride() * i3);
                bArr[(i2 * width) + i3] = rowStride < byteBufferDuplicate.limit() ? byteBufferDuplicate.get(rowStride) : (byte) 0;
            }
        }
        Image.Plane plane2 = image.getPlanes()[1];
        Image.Plane plane3 = image.getPlanes()[2];
        ByteBuffer byteBufferDuplicate2 = plane2.getBuffer().duplicate();
        ByteBuffer byteBufferDuplicate3 = plane3.getBuffer().duplicate();
        int iPosition2 = byteBufferDuplicate2.position();
        int iPosition3 = byteBufferDuplicate3.position();
        for (int i4 = 0; i4 < height / 2; i4++) {
            for (int i5 = 0; i5 < width / 2; i5++) {
                int rowStride2 = (plane3.getRowStride() * i4) + iPosition3 + (plane3.getPixelStride() * i5);
                int rowStride3 = (plane2.getRowStride() * i4) + iPosition2 + (plane2.getPixelStride() * i5);
                int i6 = i + 1;
                int iLimit = byteBufferDuplicate3.limit();
                byte b = ByteCompanionObject.MIN_VALUE;
                bArr[i] = rowStride2 < iLimit ? byteBufferDuplicate3.get(rowStride2) : (byte) -128;
                i += 2;
                if (rowStride3 < byteBufferDuplicate2.limit()) {
                    b = byteBufferDuplicate2.get(rowStride3);
                }
                bArr[i6] = b;
            }
        }
        return bArr;
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class FrameData {
        long cameraTime;
        long captureTime;
        byte[] confidence8;
        float cx;
        float cy;
        byte[] depth16;
        int depthHeight;
        long depthTime;
        int depthWidth;
        int displayRotation;
        long frameTime;
        float fx;
        float fy;
        int imageHeight;
        int imageWidth;
        int index;
        int intrinsicsHeight;
        int intrinsicsWidth;
        byte[] nv21;
        float qw;
        float qx;
        float qy;
        float qz;
        float tx;
        float ty;
        float tz;

        private FrameData() {
        }

        static FrameData copy(int i, long j, long j2, long j3, long j4, int i2, float[] fArr, float[] fArr2, float[] fArr3, float[] fArr4, int[] iArr, Image image, Image image2, Image image3) {
            FrameData frameData = new FrameData();
            frameData.index = i;
            frameData.frameTime = j;
            frameData.depthTime = j2;
            frameData.cameraTime = j3;
            frameData.captureTime = j4;
            frameData.displayRotation = i2;
            frameData.imageWidth = image.getWidth();
            frameData.imageHeight = image.getHeight();
            frameData.depthWidth = image2.getWidth();
            frameData.depthHeight = image2.getHeight();
            frameData.intrinsicsWidth = iArr[0];
            frameData.intrinsicsHeight = iArr[1];
            frameData.fx = fArr3[0];
            frameData.fy = fArr3[1];
            frameData.cx = fArr4[0];
            frameData.cy = fArr4[1];
            frameData.tx = fArr[0];
            frameData.ty = fArr[1];
            frameData.tz = fArr[2];
            frameData.qx = fArr2[0];
            frameData.qy = fArr2[1];
            frameData.qz = fArr2[2];
            frameData.qw = fArr2[3];
            frameData.nv21 = DatasetRecorder.nv21(image);
            frameData.depth16 = DatasetRecorder.depth16(image2);
            frameData.confidence8 = DatasetRecorder.confidence8(image3);
            return frameData;
        }
    }
}
