package com.voxelscan.android;

import android.app.Activity;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import android.view.View;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraIntrinsics;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import kotlin.UByte;
import kotlin.UShort;

/* JADX INFO: loaded from: classes.dex */
final class VoxelScanRenderer implements GLSurfaceView.Renderer, View.OnTouchListener {
    private final Activity activity;
    private PlaneInfo bestFloor;
    private PlaneInfo bestWall;
    private final DatasetRecorder datasetRecorder;
    private int depthFps;
    private int depthFrames;
    private boolean hasCameraPose;
    private int height;
    private int labAcceptedSamples;
    private int labRejectedSamples;
    private int labStableMotionFrames;
    private float lastGestureCenterX;
    private float lastGestureCenterY;
    private long lastIntegrationNanos;
    private float lastPinchDistance;
    private Pose lastReliablePose;
    private long lastStatsNanos;
    private float lastTouchX;
    private float lastTouchY;
    private Pose lastTrackingPose;
    private PlaneInfo manualFloor;
    private PlaneInfo manualWall;
    private boolean multiTouchTransition;
    private volatile SurfaceRequest pendingSurfaceRequest;
    private volatile boolean reviewMode;
    private VoxelColorPalette reviewPalette;
    private volatile int reviewPaletteSize;
    private volatile boolean scanning;
    private Session session;
    private StatsListener statsListener;
    final GLSurfaceView surfaceView;
    private boolean trackingDiscontinuity;
    private TrackingIssueListener trackingIssueListener;
    private boolean trackingRecovery;
    private int trackingRecoveryFrames;
    private int width;
    final VoxelWorld world = new VoxelWorld();
    private final CameraBackgroundRenderer backgroundRenderer = new CameraBackgroundRenderer();
    private final VoxelPointRenderer voxelRenderer = new VoxelPointRenderer();
    private final VoxelCubeRenderer cubeRenderer = new VoxelCubeRenderer();
    private int cameraTextureId = -1;
    private long lastIntegratedDepthTimestamp = Long.MIN_VALUE;
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] viewProjection = new float[16];
    private final float[] cameraPoint = new float[4];
    private final float[] worldPoint = new float[4];
    private final float[] poseMatrix = new float[16];
    private final float[] reviewCenter = new float[3];
    private float reviewYaw = 0.7f;
    private float reviewPitch = 0.45f;
    private float reviewDistance = 2.4f;
    private float reviewMinDistance = 0.15f;
    private float reviewMaxDistance = 18.0f;
    private volatile boolean blockMode = true;
    private ScanMode scanMode = ScanMode.INTERIOR;
    private volatile float maxScanDepthMeters = 5.0f;
    private TrackingState previousTrackingState = TrackingState.STOPPED;
    private float exposureScale = 1.0f;
    private boolean labMotionSafe = true;
    private int labAcceptancePercent = 100;

    interface CleanCallback {
        void onCleaned(int i);
    }

    interface LoadCallback {
        void onLoaded(int i, String str);
    }

    interface PaletteCallback {
        void onPaletteReady(int i, String str);
    }

    enum ScanMode {
        INTERIOR,
        EXTERIOR
    }

    interface SnapCallback {
        void onSnapped(boolean z, int i);
    }

    interface StatsListener {
        void onStats(String str);
    }

    enum SurfaceType {
        FLOOR,
        WALL
    }

    interface TrackingIssueListener {
        void onTrackingIssue(String str);
    }

    private int minimumDepthConfidence(int i) {
        return 170;
    }

    VoxelScanRenderer(Activity activity) {
        this.activity = activity;
        this.datasetRecorder = new DatasetRecorder(activity.getCacheDir());
        GLSurfaceView gLSurfaceView = new GLSurfaceView(activity);
        this.surfaceView = gLSurfaceView;
        gLSurfaceView.setEGLContextClientVersion(2);
        gLSurfaceView.setPreserveEGLContextOnPause(true);
        gLSurfaceView.setRenderer(this);
        gLSurfaceView.setRenderMode(1);
        gLSurfaceView.setOnTouchListener(this);
    }

    void setSession(Session session) {
        this.session = session;
        if (this.width <= 0 || this.height <= 0) {
            return;
        }
        session.setDisplayGeometry(this.activity.getWindowManager().getDefaultDisplay().getRotation(), this.width, this.height);
    }

    void setStatsListener(StatsListener statsListener) {
        this.statsListener = statsListener;
    }

    void setTrackingIssueListener(TrackingIssueListener trackingIssueListener) {
        this.trackingIssueListener = trackingIssueListener;
    }

    boolean toggleScanning() {
        if (this.reviewMode) {
            return exitReview(true);
        }
        if (!this.scanning && this.trackingDiscontinuity) {
            notifyTrackingIssue("Tracking alignment is still unsafe. Return to the last scanned area or export and press NEW.");
            return false;
        }
        this.scanning = !this.scanning;
        if (this.scanning) {
            this.trackingRecoveryFrames = 0;
            if (this.world.voxelCount() == 0) {
                this.exposureScale = 1.0f;
            }
        }
        return this.scanning;
    }

    boolean enterReview() {
        this.scanning = false;
        this.reviewMode = true;
        this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$enterReview$0();
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$enterReview$0() {
        synchronized (this.world) {
            frameReviewCamera();
            rebuildReviewVisuals();
        }
    }

    boolean exitReview(boolean z) {
        this.reviewMode = false;
        this.scanning = z;
        this.reviewPalette = null;
        this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$exitReview$1();
            }
        });
        return this.scanning;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exitReview$1() {
        synchronized (this.world) {
            this.voxelRenderer.update(this.world);
            this.cubeRenderer.rebuildAll(this.world);
        }
    }

    boolean isReviewMode() {
        return this.reviewMode;
    }

    boolean isScanning() {
        return this.scanning;
    }

    boolean setGridAxisFromCurrentCamera() {
        synchronized (this.world) {
            if (this.hasCameraPose && this.world.voxelCount() <= 0) {
                float[] fArr = this.poseMatrix;
                float f = -fArr[8];
                float f2 = -fArr[10];
                if (((float) Math.sqrt((f * f) + (f2 * f2))) < 0.001f) {
                    return false;
                }
                synchronized (this.world) {
                    this.world.setAxisYaw((float) Math.atan2(f, f2));
                }
                return true;
            }
            return false;
        }
    }

    float axisYawDegrees() {
        float fAxisYawDegrees;
        synchronized (this.world) {
            fAxisYawDegrees = this.world.axisYawDegrees();
        }
        return fAxisYawDegrees;
    }

    boolean toggleBlockMode() {
        this.blockMode = !this.blockMode;
        if (this.blockMode) {
            this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$toggleBlockMode$2();
                }
            });
        }
        return this.blockMode;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$toggleBlockMode$2() {
        synchronized (this.world) {
            this.cubeRenderer.rebuildAll(this.world, this.reviewMode ? this.reviewPalette : null);
        }
    }

    ScanMode toggleScanMode() {
        ScanMode scanMode = this.scanMode == ScanMode.INTERIOR ? ScanMode.EXTERIOR : ScanMode.INTERIOR;
        this.scanMode = scanMode;
        return scanMode;
    }

    ScanMode scanMode() {
        return this.scanMode;
    }

    boolean setGridAxisFromBestWall() {
        if (this.bestWall == null) {
            return false;
        }
        synchronized (this.world) {
            if (this.world.voxelCount() > 0) {
                return false;
            }
            this.world.setAxisYaw(this.bestWall.wallYawRadians);
            return true;
        }
    }

    boolean isBlockMode() {
        return this.blockMode;
    }

    int voxelCount() {
        int iVoxelCount;
        synchronized (this.world) {
            iVoxelCount = this.world.voxelCount();
        }
        return iVoxelCount;
    }

    boolean hasReconstructionData() {
        boolean zHasReconstructionData;
        synchronized (this.world) {
            zHasReconstructionData = this.world.hasReconstructionData();
        }
        return zHasReconstructionData;
    }

    int reviewPaletteSize() {
        return this.reviewPaletteSize;
    }

    void setReviewPaletteSize(final int i, final PaletteCallback paletteCallback) {
        if (this.reviewMode) {
            this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$setReviewPaletteSize$3(i, paletteCallback);
                }
            });
        } else if (paletteCallback != null) {
            paletteCallback.onPaletteReady(0, "Open REVIEW before changing the palette.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setReviewPaletteSize$3(int i, PaletteCallback paletteCallback) {
        int size;
        try {
            synchronized (this.world) {
                this.reviewPaletteSize = Math.max(0, Math.min(255, i));
                rebuildReviewVisuals();
                VoxelColorPalette voxelColorPalette = this.reviewPalette;
                size = voxelColorPalette == null ? 0 : voxelColorPalette.size();
            }
            if (paletteCallback != null) {
                paletteCallback.onPaletteReady(size, null);
            }
        } catch (Throwable th) {
            if (paletteCallback != null) {
                paletteCallback.onPaletteReady(0, th.getMessage());
            }
        }
    }

    boolean setVoxelSize(float f) {
        boolean voxelSize;
        synchronized (this.world) {
            voxelSize = this.world.setVoxelSize(f);
            if (voxelSize) {
                this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.lambda$setVoxelSize$4();
                    }
                });
            }
        }
        return voxelSize;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setVoxelSize$4() {
        synchronized (this.world) {
            this.voxelRenderer.update(this.world);
            this.cubeRenderer.rebuildAll(this.world);
        }
    }

    float voxelSize() {
        float fVoxelSize;
        synchronized (this.world) {
            fVoxelSize = this.world.voxelSize();
        }
        return fVoxelSize;
    }

    void setMaxScanDepth(float f) {
        this.maxScanDepthMeters = Math.max(0.5f, Math.min(20.0f, f));
    }

    float maxScanDepth() {
        return this.maxScanDepthMeters;
    }

    boolean startDatasetRecording() {
        boolean zStart;
        synchronized (this.world) {
            zStart = this.datasetRecorder.start(this.world.voxelSize(), this.maxScanDepthMeters, this.world.axisYawDegrees(), this.scanMode.name());
        }
        return zStart;
    }

    boolean stopDatasetRecording(DatasetRecorder.StopCallback stopCallback) {
        return this.datasetRecorder.stop(stopCallback);
    }

    boolean isDatasetRecording() {
        return this.datasetRecorder.isRecording();
    }

    boolean isDatasetFinalizing() {
        return this.datasetRecorder.isFinalizing();
    }

    File lastDatasetFile() {
        return this.datasetRecorder.lastArchive();
    }

    void replaceWorldFromJson(final String str, final LoadCallback loadCallback) {
        this.scanning = false;
        this.reviewMode = true;
        this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$replaceWorldFromJson$5(str, loadCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$replaceWorldFromJson$5(String str, LoadCallback loadCallback) {
        int iReplaceFromJson;
        try {
            synchronized (this.world) {
                iReplaceFromJson = this.world.replaceFromJson(str);
                frameReviewCamera();
                rebuildReviewVisuals();
            }
            if (loadCallback != null) {
                loadCallback.onLoaded(iReplaceFromJson, null);
            }
        } catch (Exception e) {
            if (loadCallback != null) {
                loadCallback.onLoaded(0, e.getMessage());
            }
        }
    }

    void markSurfaceFromCrosshair(SurfaceType surfaceType, SnapCallback snapCallback) {
        this.pendingSurfaceRequest = new SurfaceRequest(surfaceType, snapCallback);
    }

    void cleanWorld(final CleanCallback cleanCallback) {
        this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$cleanWorld$6(cleanCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$cleanWorld$6(CleanCallback cleanCallback) {
        int iCleanup;
        synchronized (this.world) {
            iCleanup = this.world.cleanup();
            if (this.reviewMode) {
                rebuildReviewVisuals();
            } else {
                this.voxelRenderer.update(this.world);
                this.cubeRenderer.rebuildAll(this.world);
            }
        }
        if (cleanCallback != null) {
            cleanCallback.onCleaned(iCleanup);
        }
    }

    void resetWorld(final Runnable runnable) {
        this.scanning = false;
        this.reviewMode = false;
        this.manualFloor = null;
        this.manualWall = null;
        this.pendingSurfaceRequest = null;
        this.lastIntegrationNanos = System.nanoTime();
        this.lastTrackingPose = null;
        this.lastReliablePose = null;
        this.previousTrackingState = TrackingState.STOPPED;
        this.trackingRecovery = false;
        this.trackingDiscontinuity = false;
        this.trackingRecoveryFrames = 0;
        this.exposureScale = 1.0f;
        this.labStableMotionFrames = 0;
        this.labMotionSafe = true;
        this.labAcceptedSamples = 0;
        this.labRejectedSamples = 0;
        this.labAcceptancePercent = 100;
        this.reviewPaletteSize = 0;
        this.reviewPalette = null;
        this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$resetWorld$7(runnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$resetWorld$7(Runnable runnable) {
        synchronized (this.world) {
            this.world.clear();
            this.voxelRenderer.update(this.world);
            this.cubeRenderer.rebuildAll(this.world);
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    void snapFloor(final SnapCallback snapCallback) {
        this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$snapFloor$8(snapCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$snapFloor$8(SnapCallback snapCallback) {
        int iSnapFloor = 0;
        boolean z = this.bestFloor != null;
        if (z) {
            synchronized (this.world) {
                iSnapFloor = this.world.snapFloor(this.bestFloor.centerY, Math.max(this.world.voxelSize() * 1.75f, 0.1f));
                if (this.reviewMode) {
                    rebuildReviewVisuals();
                } else {
                    this.voxelRenderer.update(this.world);
                    this.cubeRenderer.rebuildAll(this.world);
                }
            }
        }
        if (snapCallback != null) {
            snapCallback.onSnapped(z, iSnapFloor);
        }
    }

    void snapWall(final SnapCallback snapCallback) {
        this.surfaceView.queueEvent(new Runnable() { // from class: com.voxelscan.android.VoxelScanRenderer$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$snapWall$9(snapCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$snapWall$9(SnapCallback snapCallback) {
        int iSnapWall = 0;
        boolean z = this.bestWall != null;
        if (z) {
            synchronized (this.world) {
                iSnapWall = this.world.snapWall(this.bestWall.centerX, this.bestWall.centerZ, this.bestWall.normalX, this.bestWall.normalZ, Math.max(this.world.voxelSize() * 1.75f, 0.1f));
                if (this.reviewMode) {
                    rebuildReviewVisuals();
                } else {
                    this.voxelRenderer.update(this.world);
                    this.cubeRenderer.rebuildAll(this.world);
                }
            }
        }
        if (snapCallback != null) {
            snapCallback.onSnapped(z, iSnapWall);
        }
    }

    void onResume() {
        this.surfaceView.onResume();
    }

    void onPause() {
        this.surfaceView.onPause();
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        GLES20.glClearColor(0.04f, 0.05f, 0.06f, 1.0f);
        this.cameraTextureId = GlUtil.createExternalTexture();
        this.backgroundRenderer.create();
        this.voxelRenderer.create();
        this.cubeRenderer.create();
        Session session = this.session;
        if (session != null) {
            session.setCameraTextureName(this.cameraTextureId);
        }
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 gl10, int i, int i2) {
        this.width = i;
        this.height = i2;
        GLES20.glViewport(0, 0, i, i2);
        Session session = this.session;
        if (session != null) {
            session.setDisplayGeometry(this.activity.getWindowManager().getDefaultDisplay().getRotation(), i, i2);
        }
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 gl10) {
        int i;
        boolean zIntegrateDepth;
        GLES20.glClear(16640);
        if (this.reviewMode) {
            buildReviewViewProjection();
            if (this.blockMode) {
                this.cubeRenderer.draw(this.viewProjection);
            } else {
                this.voxelRenderer.draw(this.viewProjection, this.height);
            }
            emitReviewStats();
            return;
        }
        Session session = this.session;
        if (session == null || (i = this.cameraTextureId) < 0) {
            return;
        }
        session.setCameraTextureName(i);
        try {
            Frame frameUpdate = this.session.update();
            if (frameUpdate.hasDisplayGeometryChanged()) {
                frameUpdate.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, this.backgroundRenderer.screenCoordinates(), Coordinates2d.TEXTURE_NORMALIZED, this.backgroundRenderer.textureCoordinates());
            }
            this.backgroundRenderer.draw(this.cameraTextureId);
            Camera camera = frameUpdate.getCamera();
            camera.getPose().toMatrix(this.poseMatrix, 0);
            this.hasCameraPose = camera.getTrackingState() == TrackingState.TRACKING;
            boolean zUpdateTrackingGuard = updateTrackingGuard(camera);
            updatePlaneTracking();
            processSurfaceRequest(frameUpdate, camera);
            camera.getProjectionMatrix(this.projection, 0, 0.05f, 40.0f);
            camera.getViewMatrix(this.view, 0);
            Matrix.multiplyMM(this.viewProjection, 0, this.projection, 0, this.view, 0);
            if (this.scanning && camera.getTrackingState() == TrackingState.TRACKING && zUpdateTrackingGuard) {
                long jNanoTime = System.nanoTime();
                if (jNanoTime - this.lastIntegrationNanos > 120000000) {
                    synchronized (this.world) {
                        zIntegrateDepth = integrateDepth(frameUpdate, camera);
                        if (zIntegrateDepth && !this.datasetRecorder.isRecording()) {
                            this.voxelRenderer.update(this.world);
                            if (this.blockMode) {
                                this.cubeRenderer.updateDirty(this.world, 6);
                            }
                        }
                    }
                    this.lastIntegrationNanos = jNanoTime;
                    if (zIntegrateDepth) {
                        this.depthFrames++;
                    }
                }
            }
            if (!this.datasetRecorder.isRecording()) {
                if (this.blockMode) {
                    synchronized (this.world) {
                        this.cubeRenderer.updateDirty(this.world, 2);
                    }
                    this.cubeRenderer.draw(this.viewProjection);
                } else {
                    this.voxelRenderer.draw(this.viewProjection, this.height);
                }
            }
            emitStats(camera.getTrackingState());
        } catch (Throwable th) {
            StatsListener statsListener = this.statsListener;
            if (statsListener != null) {
                statsListener.onStats("AR frame error: " + th.getMessage());
            }
        }
    }

    /* JADX WARN: Code duplicated, block: B:38:0x0146  */
    /* JADX WARN: Code duplicated, block: B:60:0x0226  */
    private boolean integrateDepth(Frame frame, Camera camera) throws Throwable {
        Image imageAcquireRawDepthConfidenceImage;
        Image image;
        int i;
        ByteBuffer byteBuffer;
        ByteBuffer byteBuffer2;
        char c;
        int i2;
        int i3;
        ArrayList<Float> arrayList;
        Image imageAcquireCameraImage = null;
        try {
            Image imageAcquireRawDepthImage16Bits = frame.acquireRawDepthImage16Bits();
            try {
                imageAcquireRawDepthConfidenceImage = frame.acquireRawDepthConfidenceImage();
                try {
                    try {
                        long timestamp = imageAcquireRawDepthImage16Bits.getTimestamp();
                        if (timestamp == this.lastIntegratedDepthTimestamp) {
                            if (imageAcquireRawDepthImage16Bits != null) {
                                imageAcquireRawDepthImage16Bits.close();
                            }
                            if (imageAcquireRawDepthConfidenceImage != null) {
                                imageAcquireRawDepthConfidenceImage.close();
                            }
                            return false;
                        }
                        this.lastIntegratedDepthTimestamp = timestamp;
                        try {
                            imageAcquireCameraImage = frame.acquireCameraImage();
                        } catch (NotYetAvailableException unused) {
                        }
                        image = imageAcquireCameraImage;
                        try {
                            CameraIntrinsics textureIntrinsics = camera.getTextureIntrinsics();
                            float[] focalLength = textureIntrinsics.getFocalLength();
                            float[] principalPoint = textureIntrinsics.getPrincipalPoint();
                            int[] imageDimensions = textureIntrinsics.getImageDimensions();
                            char c2 = 3;
                            if (this.datasetRecorder.isRecording() && image != null) {
                                Pose pose = camera.getPose();
                                float[] fArr = new float[3];
                                float[] fArr2 = new float[4];
                                pose.getTranslation(fArr, 0);
                                pose.getRotationQuaternion(fArr2, 0);
                                this.datasetRecorder.capture(frame.getTimestamp(), timestamp, image.getTimestamp(), this.activity.getWindowManager().getDefaultDisplay().getRotation(), fArr, fArr2, focalLength, principalPoint, imageDimensions, image, imageAcquireRawDepthImage16Bits, imageAcquireRawDepthConfidenceImage);
                            }
                            if (this.datasetRecorder.isRecording()) {
                                boolean z = image != null;
                                if (imageAcquireRawDepthImage16Bits != null) {
                                    imageAcquireRawDepthImage16Bits.close();
                                }
                                if (imageAcquireRawDepthConfidenceImage != null) {
                                    imageAcquireRawDepthConfidenceImage.close();
                                }
                                if (image != null) {
                                    image.close();
                                }
                                return z;
                            }
                            int width = imageAcquireRawDepthImage16Bits.getWidth();
                            int height = imageAcquireRawDepthImage16Bits.getHeight();
                            float f = width;
                            float fMax = (focalLength[0] * f) / Math.max(1.0f, imageDimensions[0]);
                            float f2 = height;
                            float fMax2 = (focalLength[1] * f2) / Math.max(1.0f, imageDimensions[1]);
                            float fMax3 = (principalPoint[0] * f) / Math.max(1.0f, imageDimensions[0]);
                            float fMax4 = (principalPoint[1] * f2) / Math.max(1.0f, imageDimensions[1]);
                            ByteBuffer byteBufferOrder = imageAcquireRawDepthImage16Bits.getPlanes()[0].getBuffer().order(ByteOrder.LITTLE_ENDIAN);
                            ByteBuffer buffer = imageAcquireRawDepthConfidenceImage.getPlanes()[0].getBuffer();
                            int rowStride = imageAcquireRawDepthImage16Bits.getPlanes()[0].getRowStride();
                            int pixelStride = imageAcquireRawDepthImage16Bits.getPlanes()[0].getPixelStride();
                            int rowStride2 = imageAcquireRawDepthConfidenceImage.getPlanes()[0].getRowStride();
                            int pixelStride2 = imageAcquireRawDepthConfidenceImage.getPlanes()[0].getPixelStride();
                            long jCurrentTimeMillis = System.currentTimeMillis();
                            long timestamp2 = frame.getTimestamp();
                            float[] fArr3 = this.poseMatrix;
                            float f3 = fArr3[12];
                            float f4 = fArr3[13];
                            float f5 = fArr3[14];
                            ArrayList<Float> arrayList2 = new ArrayList<>();
                            int i4 = 0;
                            while (i4 < height) {
                                int i5 = 0;
                                while (i5 < width) {
                                    int i6 = (i4 * rowStride) + (i5 * pixelStride);
                                    if (i6 + 1 < byteBufferOrder.capacity() && (i = (i4 * rowStride2) + (i5 * pixelStride2)) < buffer.capacity()) {
                                        int i7 = buffer.get(i) & UByte.MAX_VALUE;
                                        int i8 = 65535 & byteBufferOrder.getShort(i6);
                                        if (i8 < 300 || i8 > Math.round(this.maxScanDepthMeters * 1000.0f) || i7 < minimumDepthConfidence(i8)) {
                                            arrayList = arrayList2;
                                            byteBuffer = buffer;
                                            byteBuffer2 = byteBufferOrder;
                                            c = c2;
                                            i2 = i5;
                                            i3 = i4;
                                        } else {
                                            int i9 = i5;
                                            int i10 = i4;
                                            ArrayList<Float> arrayList3 = arrayList2;
                                            byteBuffer = buffer;
                                            byteBuffer2 = byteBufferOrder;
                                            c = c2;
                                            if (isLocallyConsistentDepth(byteBufferOrder, rowStride, pixelStride, width, height, i9, i10, i8)) {
                                                float f6 = i8 / 1000.0f;
                                                float[] fArr4 = this.cameraPoint;
                                                i2 = i9;
                                                fArr4[0] = ((i2 - fMax3) * f6) / fMax;
                                                i3 = i10;
                                                fArr4[1] = ((-(i3 - fMax4)) * f6) / fMax2;
                                                fArr4[2] = -f6;
                                                fArr4[c] = 1.0f;
                                                Matrix.multiplyMV(this.worldPoint, 0, this.poseMatrix, 0, fArr4, 0);
                                                applyManualSurfaceConstraints(this.worldPoint);
                                                int[] iArrSampleCameraColor = sampleCameraColor(image, i2, i3, width, height);
                                                if (arrayList3.size() >= 256 || ((i2 / 3) + (i3 / 3)) % 7 != 0) {
                                                    arrayList = arrayList3;
                                                } else {
                                                    VoxelWorld voxelWorld = this.world;
                                                    float[] fArr5 = this.worldPoint;
                                                    float fExposureScaleObservation = voxelWorld.exposureScaleObservation(fArr5[0], fArr5[1], fArr5[2], iArrSampleCameraColor[0], iArrSampleCameraColor[1], iArrSampleCameraColor[2], timestamp2);
                                                    if (Float.isNaN(fExposureScaleObservation)) {
                                                        arrayList = arrayList3;
                                                    } else {
                                                        arrayList = arrayList3;
                                                        arrayList.add(Float.valueOf(fExposureScaleObservation));
                                                    }
                                                }
                                                int[] iArrApplyExposureScale = applyExposureScale(iArrSampleCameraColor);
                                                float f7 = i7 / 255.0f;
                                                float f8 = f7 * f7;
                                                float fMax5 = ((1.15f * f8) + 0.35f) / Math.max(1.0f, 0.35f * f6);
                                                float fMax6 = Math.max(this.world.voxelSize() * 1.5f, (f6 * 0.018f) + 0.035f);
                                                VoxelWorld voxelWorld2 = this.world;
                                                float[] fArr6 = this.worldPoint;
                                                voxelWorld2.carveFreeSpace(f3, f4, f5, fArr6[0], fArr6[1], fArr6[2], fMax6, f8 * 0.65f, jCurrentTimeMillis, timestamp2);
                                                VoxelWorld voxelWorld3 = this.world;
                                                float[] fArr7 = this.worldPoint;
                                                voxelWorld3.add(fArr7[0], fArr7[1], fArr7[2], iArrApplyExposureScale[0], iArrApplyExposureScale[1], iArrApplyExposureScale[2], fMax5, jCurrentTimeMillis, timestamp2);
                                            } else {
                                                i2 = i9;
                                                i3 = i10;
                                                arrayList = arrayList3;
                                            }
                                        }
                                    } else {
                                        arrayList = arrayList2;
                                        byteBuffer = buffer;
                                        byteBuffer2 = byteBufferOrder;
                                        c = c2;
                                        i2 = i5;
                                        i3 = i4;
                                    }
                                    i5 = i2 + 3;
                                    arrayList2 = arrayList;
                                    i4 = i3;
                                    buffer = byteBuffer;
                                    byteBufferOrder = byteBuffer2;
                                    c2 = c;
                                }
                                i4 += 3;
                                c2 = c2;
                            }
                            updateExposureScale(arrayList2);
                            if (imageAcquireRawDepthImage16Bits != null) {
                                imageAcquireRawDepthImage16Bits.close();
                            }
                            if (imageAcquireRawDepthConfidenceImage != null) {
                                imageAcquireRawDepthConfidenceImage.close();
                            }
                            if (image != null) {
                                image.close();
                            }
                            return true;
                        } catch (NotYetAvailableException unused2) {
                        } catch (IllegalStateException unused3) {
                            imageAcquireCameraImage = imageAcquireRawDepthImage16Bits;
                            if (imageAcquireCameraImage != null) {
                                imageAcquireCameraImage.close();
                            }
                            if (imageAcquireRawDepthConfidenceImage != null) {
                                imageAcquireRawDepthConfidenceImage.close();
                            }
                            if (image != null) {
                                image.close();
                            }
                            return false;
                        } catch (Throwable th) {
                            th = th;
                            imageAcquireCameraImage = imageAcquireRawDepthImage16Bits;
                            if (imageAcquireCameraImage != null) {
                                imageAcquireCameraImage.close();
                            }
                            if (imageAcquireRawDepthConfidenceImage != null) {
                                imageAcquireRawDepthConfidenceImage.close();
                            }
                            if (image != null) {
                                image.close();
                            }
                            throw th;
                        }
                    } catch (NotYetAvailableException unused4) {
                        image = null;
                    }
                } catch (IllegalStateException unused5) {
                    image = null;
                } catch (Throwable th2) {
                    th = th2;
                    image = null;
                }
            } catch (NotYetAvailableException unused6) {
                imageAcquireRawDepthConfidenceImage = null;
                image = null;
            } catch (IllegalStateException unused7) {
                imageAcquireRawDepthConfidenceImage = null;
                image = null;
            } catch (Throwable th3) {
                th = th3;
                imageAcquireRawDepthConfidenceImage = null;
                image = null;
            }
            imageAcquireCameraImage = imageAcquireRawDepthImage16Bits;
        } catch (NotYetAvailableException unused8) {
            imageAcquireRawDepthConfidenceImage = null;
            image = null;
        } catch (IllegalStateException unused9) {
            imageAcquireRawDepthConfidenceImage = null;
            image = null;
        } catch (Throwable th4) {
            th = th4;
            imageAcquireRawDepthConfidenceImage = null;
            image = null;
        }
        if (imageAcquireCameraImage != null) {
            imageAcquireCameraImage.close();
        }
        if (imageAcquireRawDepthConfidenceImage != null) {
            imageAcquireRawDepthConfidenceImage.close();
        }
        if (image != null) {
            image.close();
        }
        return false;
    }

    /* JADX WARN: Code duplicated, block: B:14:0x0035  */
    private int spatiallyFilteredDepth(ByteBuffer byteBuffer, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        int[] iArr = new int[9];
        int iMax = Math.max(35, Math.min(110, Math.round(i7 * 0.02f)));
        int i8 = 0;
        for (int i9 = -1; i9 <= 1; i9++) {
            for (int i10 = -1; i10 <= 1; i10++) {
                int i11 = i5 + i10;
                int i12 = i6 + i9;
                if (i11 >= 0 && i12 >= 0 && i11 < i3) {
                    if (i12 < i4) {
                        int i13 = (i12 * i) + (i11 * i2);
                        if (i13 + 1 < byteBuffer.capacity()) {
                            int i14 = byteBuffer.getShort(i13) & UShort.MAX_VALUE;
                            if (i14 > 0 && Math.abs(i14 - i7) <= iMax) {
                                iArr[i8] = i14;
                                i8++;
                            }
                        }
                    }
                }
            }
        }
        if (i8 < 5) {
            return 0;
        }
        Arrays.sort(iArr, 0, i8);
        return iArr[i8 / 2];
    }

    private boolean isLocallyConsistentDepth(ByteBuffer byteBuffer, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        int i8;
        int iMax = Math.max(45, Math.min(140, Math.round(i7 * 0.025f)));
        int i9 = 0;
        int i10 = -1;
        while (i10 <= 1) {
            int i11 = i9;
            int i12 = -1;
            while (i12 <= 1) {
                if (i12 == 0 && i10 == 0) {
                    i8 = i12;
                } else {
                    i8 = i12;
                    if (depthAgrees(byteBuffer, i, i2, i3, i4, i5 + i12, i6 + i10, i7, iMax)) {
                        i11++;
                    }
                }
                i12 = i8 + 1;
            }
            i10++;
            i9 = i11;
        }
        return i9 >= 3;
    }

    private boolean depthAgrees(ByteBuffer byteBuffer, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        int i9;
        if (i5 < 0 || i6 < 0 || i5 >= i3 || i6 >= i4) {
            return false;
        }
        int i10 = (i6 * i) + (i5 * i2);
        return i10 + 1 < byteBuffer.capacity() && (i9 = byteBuffer.getShort(i10) & UShort.MAX_VALUE) > 0 && Math.abs(i9 - i7) <= i8;
    }

    private int[] sampleCameraColor(Image image, int i, int i2, int i3, int i4) {
        if (image == null || image.getFormat() != 35) {
            return new int[]{150, 190, 205};
        }
        int iMax = Math.max(0, Math.min(image.getWidth() - 1, (i * image.getWidth()) / Math.max(1, i3)));
        int iMax2 = Math.max(0, Math.min(image.getHeight() - 1, (i2 * image.getHeight()) / Math.max(1, i4)));
        Image.Plane plane = image.getPlanes()[0];
        Image.Plane plane2 = image.getPlanes()[1];
        Image.Plane plane3 = image.getPlanes()[2];
        int i5 = plane.getBuffer().get((plane.getRowStride() * iMax2) + (plane.getPixelStride() * iMax)) & UByte.MAX_VALUE;
        int i6 = iMax / 2;
        int i7 = iMax2 / 2;
        int i8 = (plane2.getBuffer().get((plane2.getRowStride() * i7) + (plane2.getPixelStride() * i6)) & UByte.MAX_VALUE) - 128;
        int i9 = (plane3.getBuffer().get((i7 * plane3.getRowStride()) + (i6 * plane3.getPixelStride())) & UByte.MAX_VALUE) - 128;
        int i10 = (i5 - 16) * 298;
        return new int[]{clamp((((i9 * 409) + i10) + 128) >> 8), clamp((((i10 - (i8 * 100)) - (i9 * 208)) + 128) >> 8), clamp(((i10 + (i8 * 516)) + 128) >> 8)};
    }

    private int[] applyExposureScale(int[] iArr) {
        return new int[]{clamp(Math.round(iArr[0] * this.exposureScale)), clamp(Math.round(iArr[1] * this.exposureScale)), clamp(Math.round(iArr[2] * this.exposureScale))};
    }

    private void updateExposureScale(ArrayList<Float> arrayList) {
        if (arrayList.size() < 12) {
            return;
        }
        Collections.sort(arrayList);
        int size = arrayList.size() / 5;
        float fFloatValue = (this.exposureScale * 0.78f) + (arrayList.get(Math.max(0, Math.min(arrayList.size() - 1, size + ((arrayList.size() - (size * 2)) / 2)))).floatValue() * 0.22f);
        this.exposureScale = fFloatValue;
        this.exposureScale = Math.max(0.65f, Math.min(1.55f, fFloatValue));
    }

    private boolean updateTrackingGuard(Camera camera) {
        boolean zHasReconstructionData;
        Pose pose;
        TrackingState trackingState = camera.getTrackingState();
        if (trackingState != TrackingState.TRACKING) {
            if (this.scanning && this.previousTrackingState == TrackingState.TRACKING && this.lastReliablePose != null) {
                this.trackingRecovery = true;
                this.trackingRecoveryFrames = 0;
            }
            this.labStableMotionFrames = 0;
            this.labMotionSafe = false;
            this.lastTrackingPose = null;
            this.previousTrackingState = trackingState;
            return false;
        }
        Pose pose2 = camera.getPose();
        synchronized (this.world) {
            zHasReconstructionData = this.world.hasReconstructionData();
        }
        if (!zHasReconstructionData && this.trackingDiscontinuity) {
            this.trackingDiscontinuity = false;
            this.trackingRecovery = false;
            this.trackingRecoveryFrames = 0;
        }
        if (this.trackingDiscontinuity) {
            Pose pose3 = this.lastReliablePose;
            if (pose3 != null && poseDistance(pose2, pose3) < 0.22f && poseAngleDegrees(pose2, this.lastReliablePose) < 15.0f) {
                int i = this.trackingRecoveryFrames + 1;
                this.trackingRecoveryFrames = i;
                if (i >= 30) {
                    this.trackingDiscontinuity = false;
                    this.trackingRecovery = false;
                    this.lastReliablePose = pose2;
                    notifyTrackingIssue("Tracking alignment recovered. You can press START and continue.");
                }
            } else {
                this.trackingRecoveryFrames = 0;
            }
            this.lastTrackingPose = pose2;
            this.previousTrackingState = trackingState;
            return false;
        }
        Pose pose4 = this.lastTrackingPose;
        float fPoseDistance = pose4 == null ? 0.0f : poseDistance(pose2, pose4);
        Pose pose5 = this.lastTrackingPose;
        float fPoseAngleDegrees = pose5 != null ? poseAngleDegrees(pose2, pose5) : 0.0f;
        if (this.scanning && zHasReconstructionData && this.lastTrackingPose != null && (fPoseDistance > 0.35f || fPoseAngleDegrees > 28.0f)) {
            stopForTrackingJump();
            this.lastTrackingPose = pose2;
            this.previousTrackingState = trackingState;
            return false;
        }
        if (this.trackingRecovery && (pose = this.lastReliablePose) != null) {
            if (poseDistance(pose2, pose) > 0.45f || poseAngleDegrees(pose2, this.lastReliablePose) > 30.0f) {
                stopForTrackingJump();
                this.lastTrackingPose = pose2;
                this.previousTrackingState = trackingState;
                return false;
            }
            int i2 = this.trackingRecoveryFrames + 1;
            this.trackingRecoveryFrames = i2;
            if (i2 < 12) {
                this.lastTrackingPose = pose2;
                this.previousTrackingState = trackingState;
                return false;
            }
            this.trackingRecovery = false;
        }
        this.labMotionSafe = true;
        this.lastTrackingPose = pose2;
        this.previousTrackingState = trackingState;
        if (this.scanning) {
            this.lastReliablePose = pose2;
        }
        return true;
    }

    private void stopForTrackingJump() {
        this.scanning = false;
        this.trackingDiscontinuity = true;
        this.trackingRecovery = false;
        this.trackingRecoveryFrames = 0;
        notifyTrackingIssue("Tracking jump detected. Scan paused to protect alignment. Return to the last scanned area; if it does not recover, export and press NEW.");
    }

    private void notifyTrackingIssue(String str) {
        TrackingIssueListener trackingIssueListener = this.trackingIssueListener;
        if (trackingIssueListener != null) {
            trackingIssueListener.onTrackingIssue(str);
        }
    }

    private static float poseDistance(Pose pose, Pose pose2) {
        float fTx = pose.tx() - pose2.tx();
        float fTy = pose.ty() - pose2.ty();
        float fTz = pose.tz() - pose2.tz();
        return (float) Math.sqrt((fTx * fTx) + (fTy * fTy) + (fTz * fTz));
    }

    private static float poseAngleDegrees(Pose pose, Pose pose2) {
        float[] rotationQuaternion = pose.getRotationQuaternion();
        float[] rotationQuaternion2 = pose2.getRotationQuaternion();
        return (float) Math.toDegrees(Math.acos(Math.max(-1.0f, Math.min(1.0f, Math.abs((rotationQuaternion[0] * rotationQuaternion2[0]) + (rotationQuaternion[1] * rotationQuaternion2[1]) + (rotationQuaternion[2] * rotationQuaternion2[2]) + (rotationQuaternion[3] * rotationQuaternion2[3]))))) * 2.0d);
    }

    private void emitStats(TrackingState trackingState) {
        String str;
        String str2;
        String str3;
        long jNanoTime = System.nanoTime();
        if (jNanoTime - this.lastStatsNanos > 1000000000) {
            this.depthFps = this.depthFrames;
            this.depthFrames = 0;
            int i = this.labAcceptedSamples;
            int i2 = this.labRejectedSamples + i;
            if (i2 > 0) {
                this.labAcceptancePercent = Math.round((i * 100.0f) / i2);
            }
            this.labAcceptedSamples = 0;
            this.labRejectedSamples = 0;
            this.lastStatsNanos = jNanoTime;
        }
        if (this.statsListener != null) {
            synchronized (this.world) {
                StatsListener statsListener = this.statsListener;
                StringBuilder sbAppend = new StringBuilder("Capture: ").append(this.datasetRecorder.isRecording() ? "DATASET ONLY" : "VOXELS").append("  |  Depth: ARCore  |  Tracking: ").append(trackingState).append("  |  Mode: ").append(this.scanMode.name()).append("  |  Axis Z: ").append(Math.round(this.world.axisYawDegrees())).append("deg  |  Floor: ");
                if (this.manualFloor != null) {
                    str = "MANUAL";
                } else {
                    str = this.bestFloor == null ? "NO" : "AUTO";
                }
                StringBuilder sbAppend2 = sbAppend.append(str).append("  |  Wall: ");
                if (this.manualWall != null) {
                    str2 = "MANUAL";
                } else {
                    str2 = this.bestWall == null ? "NO" : "AUTO";
                }
                StringBuilder sbAppend3 = sbAppend2.append(str2).append("  |  View: ").append(this.blockMode ? "VOXELS" : "DOTS").append("  |  Size: ").append(Math.round(this.world.voxelSize() * 100.0f)).append("cm  |  Range: ").append(Math.round(this.maxScanDepthMeters)).append("m  |  Dataset: ").append(datasetStatus()).append("  |  Voxels: ").append(voxelCountStatus()).append("  |  Confirmed: ").append(this.world.confirmedCount()).append("  |  Chunks: ").append(this.world.chunkCount()).append("  |  High confidence: ").append(Math.round(this.world.highConfidenceRatio() * 100.0f)).append("%  |  Color lock: ").append(String.format(Locale.US, "%.2fx", Float.valueOf(this.exposureScale))).append("  |  Guard: ");
                if (this.trackingDiscontinuity) {
                    str3 = "BLOCKED";
                } else {
                    str3 = this.trackingRecovery ? "RECOVERING" : "OK";
                }
                statsListener.onStats(sbAppend3.append(str3).append("  |  Depth FPS: ").append(this.depthFps).toString());
            }
        }
    }

    private void emitReviewStats() {
        if (this.statsListener != null) {
            synchronized (this.world) {
                this.statsListener.onStats("REVIEW  |  Drag: rotate  |  Pinch: zoom  |  View: " + (this.blockMode ? "VOXELS" : "DOTS") + "  |  Palette: " + (this.reviewPaletteSize == 0 ? "RAW" : Integer.valueOf(this.reviewPaletteSize)) + "  |  Size: " + Math.round(this.world.voxelSize() * 100.0f) + "cm  |  Dataset: " + datasetStatus() + "  |  Voxels: " + voxelCountStatus() + "  |  Confirmed: " + this.world.confirmedCount() + "  |  Chunks: " + this.world.chunkCount() + "  |  High confidence: " + Math.round(this.world.highConfidenceRatio() * 100.0f) + "%");
            }
        }
    }

    private String datasetStatus() {
        if (this.datasetRecorder.isFinalizing()) {
            return "SAVING";
        }
        if (!this.datasetRecorder.isRecording()) {
            return "OFF";
        }
        int iDroppedCount = this.datasetRecorder.droppedCount();
        return "REC " + this.datasetRecorder.frameCount() + (iDroppedCount > 0 ? " (-" + iDroppedCount + ")" : "");
    }

    private String voxelCountStatus() {
        return Integer.toString(this.world.voxelCount()) + "/" + this.world.maxVoxelCount();
    }

    private void rebuildReviewVisuals() {
        ArrayList<VoxelWorld.Entry> arrayListEntries = this.world.entries();
        VoxelColorPalette voxelColorPaletteFrom = (this.reviewPaletteSize <= 0 || arrayListEntries.isEmpty()) ? null : VoxelColorPalette.from(arrayListEntries, this.reviewPaletteSize);
        this.reviewPalette = voxelColorPaletteFrom;
        this.voxelRenderer.update(this.world, voxelColorPaletteFrom);
        this.cubeRenderer.rebuildAll(this.world, this.reviewPalette);
    }

    private void updatePlaneTracking() {
        Session session = this.session;
        if (session == null) {
            return;
        }
        PlaneInfo planeInfoFromFloor = null;
        PlaneInfo planeInfoFromWall = null;
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING && plane.getSubsumedBy() == null) {
                float fMax = Math.max(0.01f, plane.getExtentX() * plane.getExtentZ());
                if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING) {
                    if (planeInfoFromFloor == null || fMax > planeInfoFromFloor.area) {
                        planeInfoFromFloor = PlaneInfo.fromFloor(plane, fMax);
                    }
                } else if (plane.getType() == Plane.Type.VERTICAL && (planeInfoFromWall == null || fMax > planeInfoFromWall.area)) {
                    planeInfoFromWall = PlaneInfo.fromWall(plane, fMax);
                }
            }
        }
        this.bestFloor = planeInfoFromFloor;
        this.bestWall = planeInfoFromWall;
    }

    private void processSurfaceRequest(Frame frame, Camera camera) {
        int iSnapWall;
        SurfaceRequest surfaceRequest = this.pendingSurfaceRequest;
        if (surfaceRequest == null) {
            return;
        }
        HitResult hitResult = null;
        this.pendingSurfaceRequest = null;
        if (camera.getTrackingState() != TrackingState.TRACKING) {
            if (surfaceRequest.callback != null) {
                surfaceRequest.callback.onSnapped(false, 0);
                return;
            }
            return;
        }
        for (HitResult hitResult2 : frame.hitTest(this.width * 0.5f, this.height * 0.5f)) {
            Trackable trackable = hitResult2.getTrackable();
            if (trackable instanceof Plane) {
                Plane plane = (Plane) trackable;
                boolean z = surfaceRequest.type == SurfaceType.FLOOR && plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING;
                boolean z2 = surfaceRequest.type == SurfaceType.WALL && plane.getType() == Plane.Type.VERTICAL;
                if ((z || z2) && plane.isPoseInPolygon(hitResult2.getHitPose())) {
                    hitResult = hitResult2;
                    break;
                } else if (hitResult == null) {
                    hitResult = hitResult2;
                }
            } else if (hitResult == null) {
                hitResult = hitResult2;
            }
        }
        if (hitResult == null) {
            if (surfaceRequest.callback != null) {
                surfaceRequest.callback.onSnapped(false, 0);
                return;
            }
            return;
        }
        Pose hitPose = hitResult.getHitPose();
        synchronized (this.world) {
            if (surfaceRequest.type == SurfaceType.FLOOR) {
                PlaneInfo planeInfoFromFloorPoint = PlaneInfo.fromFloorPoint(hitPose);
                this.manualFloor = planeInfoFromFloorPoint;
                iSnapWall = this.world.snapFloor(planeInfoFromFloorPoint.centerY, Math.max(this.world.voxelSize() * 1.75f, 0.12f));
            } else {
                PlaneInfo planeInfoFromWallHit = PlaneInfo.fromWallHit(hitResult, camera.getPose());
                this.manualWall = planeInfoFromWallHit;
                iSnapWall = this.world.snapWall(planeInfoFromWallHit.centerX, this.manualWall.centerZ, this.manualWall.normalX, this.manualWall.normalZ, Math.max(this.world.voxelSize() * 1.75f, 0.12f));
            }
            this.voxelRenderer.update(this.world);
            this.cubeRenderer.rebuildAll(this.world);
        }
        if (surfaceRequest.callback != null) {
            surfaceRequest.callback.onSnapped(true, iSnapWall);
        }
    }

    private void applyManualSurfaceConstraints(float[] fArr) {
        float fMax = Math.max(this.world.voxelSize() * 1.5f, 0.1f);
        PlaneInfo planeInfo = this.manualFloor;
        if (planeInfo != null && Math.abs(fArr[1] - planeInfo.centerY) <= fMax) {
            fArr[1] = this.manualFloor.centerY;
        }
        PlaneInfo planeInfo2 = this.manualWall;
        if (planeInfo2 != null) {
            float f = ((fArr[0] - planeInfo2.centerX) * this.manualWall.normalX) + ((fArr[2] - this.manualWall.centerZ) * this.manualWall.normalZ);
            if (Math.abs(f) <= fMax) {
                fArr[0] = fArr[0] - (this.manualWall.normalX * f);
                fArr[2] = fArr[2] - (f * this.manualWall.normalZ);
            }
        }
    }

    private void buildReviewViewProjection() {
        int i = this.height;
        Matrix.perspectiveM(this.projection, 0, 62.0f, i == 0 ? 1.0f : this.width / i, 0.05f, 80.0f);
        float fCos = (float) Math.cos(this.reviewPitch);
        float fSin = this.reviewCenter[0] + (((float) Math.sin(this.reviewYaw)) * fCos * this.reviewDistance);
        float fSin2 = this.reviewCenter[1] + (((float) Math.sin(this.reviewPitch)) * this.reviewDistance);
        float fCos2 = this.reviewCenter[2] + (((float) Math.cos(this.reviewYaw)) * fCos * this.reviewDistance);
        float[] fArr = this.view;
        float[] fArr2 = this.reviewCenter;
        Matrix.setLookAtM(fArr, 0, fSin, fSin2, fCos2, fArr2[0], fArr2[1], fArr2[2], 0.0f, 1.0f, 0.0f);
        Matrix.multiplyMM(this.viewProjection, 0, this.projection, 0, this.view, 0);
    }

    private void frameReviewCamera() {
        this.world.reviewCenter(this.reviewCenter);
        float fReviewRadius = this.world.reviewRadius();
        this.reviewDistance = fReviewRadius;
        this.reviewMinDistance = Math.max(this.world.voxelSize() * 1.5f, Math.min(0.35f, 0.04f * fReviewRadius));
        this.reviewMaxDistance = Math.max(18.0f, fReviewRadius * 8.0f);
    }

    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!this.reviewMode) {
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.lastTouchX = motionEvent.getX();
            this.lastTouchY = motionEvent.getY();
            this.lastPinchDistance = 0.0f;
            this.multiTouchTransition = false;
            return true;
        }
        if (actionMasked == 5 && motionEvent.getPointerCount() >= 2) {
            this.lastPinchDistance = pinchDistance(motionEvent);
            this.lastGestureCenterX = gestureCenterX(motionEvent);
            this.lastGestureCenterY = gestureCenterY(motionEvent);
            this.multiTouchTransition = true;
            return true;
        }
        if (actionMasked == 6) {
            this.lastPinchDistance = 0.0f;
            this.multiTouchTransition = true;
            return true;
        }
        if (actionMasked != 2) {
            if (actionMasked == 1 || actionMasked == 3) {
                this.lastPinchDistance = 0.0f;
                this.multiTouchTransition = false;
            }
            return true;
        }
        if (motionEvent.getPointerCount() >= 2) {
            float fPinchDistance = pinchDistance(motionEvent);
            float fGestureCenterX = gestureCenterX(motionEvent);
            float fGestureCenterY = gestureCenterY(motionEvent);
            float f = this.lastPinchDistance;
            if (f > 1.0f && fPinchDistance > 1.0f) {
                float f2 = this.reviewDistance * (f / fPinchDistance);
                this.reviewDistance = f2;
                this.reviewDistance = Math.max(this.reviewMinDistance, Math.min(this.reviewMaxDistance, f2));
                panReview(fGestureCenterX - this.lastGestureCenterX, fGestureCenterY - this.lastGestureCenterY);
            }
            this.lastPinchDistance = fPinchDistance;
            this.lastGestureCenterX = fGestureCenterX;
            this.lastGestureCenterY = fGestureCenterY;
            this.multiTouchTransition = true;
        } else {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if (this.multiTouchTransition) {
                this.lastTouchX = x;
                this.lastTouchY = y;
                this.multiTouchTransition = false;
                return true;
            }
            this.reviewYaw -= (x - this.lastTouchX) * 0.008f;
            float f3 = this.reviewPitch + ((y - this.lastTouchY) * 0.008f);
            this.reviewPitch = f3;
            this.reviewPitch = Math.max(-1.25f, Math.min(1.25f, f3));
            this.lastTouchX = x;
            this.lastTouchY = y;
        }
        return true;
    }

    private void panReview(float f, float f2) {
        float fTan = ((this.reviewDistance * 2.0f) * ((float) Math.tan(Math.toRadians(31.0d)))) / Math.max(1, this.height);
        float fSin = (float) Math.sin(this.reviewYaw);
        float fCos = (float) Math.cos(this.reviewYaw);
        float fSin2 = (float) Math.sin(this.reviewPitch);
        float fCos2 = (float) Math.cos(this.reviewPitch);
        float f3 = -fSin;
        float f4 = -fCos;
        float[] fArr = this.reviewCenter;
        fArr[0] = fArr[0] + (((f4 * f) + (f3 * fSin2 * f2)) * fTan);
        fArr[1] = fArr[1] + (fCos2 * f2 * fTan);
        fArr[2] = fArr[2] + ((((-f3) * f) + (fSin2 * f4 * f2)) * fTan);
    }

    private float pinchDistance(MotionEvent motionEvent) {
        float x = motionEvent.getX(0) - motionEvent.getX(1);
        float y = motionEvent.getY(0) - motionEvent.getY(1);
        return (float) Math.sqrt((x * x) + (y * y));
    }

    private float gestureCenterX(MotionEvent motionEvent) {
        return (motionEvent.getX(0) + motionEvent.getX(1)) * 0.5f;
    }

    private float gestureCenterY(MotionEvent motionEvent) {
        return (motionEvent.getY(0) + motionEvent.getY(1)) * 0.5f;
    }

    private static int clamp(int i) {
        return Math.max(0, Math.min(255, i));
    }

    private static final class PlaneInfo {
        final float area;
        final float centerX;
        final float centerY;
        final float centerZ;
        final float normalX;
        final float normalZ;
        final float wallYawRadians;

        private PlaneInfo(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
            this.centerX = f;
            this.centerY = f2;
            this.centerZ = f3;
            this.normalX = f4;
            this.normalZ = f5;
            this.wallYawRadians = f6;
            this.area = f7;
        }

        static PlaneInfo fromFloor(Plane plane, float f) {
            return new PlaneInfo(plane.getCenterPose().tx(), plane.getCenterPose().ty(), plane.getCenterPose().tz(), 0.0f, 0.0f, 0.0f, f);
        }

        static PlaneInfo fromFloorPoint(Pose pose) {
            return new PlaneInfo(pose.tx(), pose.ty(), pose.tz(), 0.0f, 0.0f, 0.0f, Float.MAX_VALUE);
        }

        static PlaneInfo fromWall(Plane plane, float f) {
            float f2;
            float[] fArr = new float[3];
            float f3 = 1.0f;
            plane.getCenterPose().getTransformedAxis(1, 1.0f, fArr, 0);
            float f4 = fArr[0];
            float f5 = fArr[2];
            float fSqrt = (float) Math.sqrt((f4 * f4) + (f5 * f5));
            if (fSqrt < 0.001f) {
                f2 = 0.0f;
            } else {
                f3 = f4 / fSqrt;
                f2 = f5 / fSqrt;
            }
            float f6 = f2;
            float f7 = f3;
            return new PlaneInfo(plane.getCenterPose().tx(), plane.getCenterPose().ty(), plane.getCenterPose().tz(), f7, f6, (float) Math.atan2(f6, -f7), f);
        }

        /* JADX WARN: Code duplicated, block: B:7:0x002a  */
        static PlaneInfo fromWallHit(HitResult hitResult, Pose pose) {
            float fTx;
            float fTz;
            float f;
            Pose hitPose = hitResult.getHitPose();
            Trackable trackable = hitResult.getTrackable();
            float f2 = 1.0f;
            if (trackable instanceof Plane) {
                Plane plane = (Plane) trackable;
                if (plane.getType() == Plane.Type.VERTICAL) {
                    float[] fArr = new float[3];
                    plane.getCenterPose().getTransformedAxis(1, 1.0f, fArr, 0);
                    fTx = fArr[0];
                    fTz = fArr[2];
                } else {
                    fTx = pose.tx() - hitPose.tx();
                    fTz = pose.tz() - hitPose.tz();
                }
            } else {
                fTx = pose.tx() - hitPose.tx();
                fTz = pose.tz() - hitPose.tz();
            }
            float fSqrt = (float) Math.sqrt((fTx * fTx) + (fTz * fTz));
            if (fSqrt < 0.001f) {
                f = 0.0f;
            } else {
                f2 = fTx / fSqrt;
                f = fTz / fSqrt;
            }
            float f3 = f;
            float f4 = f2;
            return new PlaneInfo(hitPose.tx(), hitPose.ty(), hitPose.tz(), f4, f3, (float) Math.atan2(f3, -f4), Float.MAX_VALUE);
        }
    }

    private static final class SurfaceRequest {
        final SnapCallback callback;
        final SurfaceType type;

        SurfaceRequest(SurfaceType surfaceType, SnapCallback snapCallback) {
            this.type = surfaceType;
            this.callback = snapCallback;
        }
    }
}
