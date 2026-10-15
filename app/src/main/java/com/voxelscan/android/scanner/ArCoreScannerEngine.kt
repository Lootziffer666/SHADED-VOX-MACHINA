package com.voxelscan.android.scanner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Owns the ARCore session and the real Full Depth -> world point pipeline.
 *
 * Phase 2 deliberately uses AUTOMATIC + acquireDepthImage16Bits(): the first phone test proved
 * RAW_DEPTH_ONLY is too sparse on textureless indoor surfaces. Raw Depth can later be fused back
 * in as a high-precision anchor source; it is no longer the only geometry source.
 */
class ArCoreScannerEngine(private val activity: Activity) {
    private val frameLock = Any()

    private val _diagnostics = MutableStateFlow(ScannerDiagnostics())
    val diagnostics: StateFlow<ScannerDiagnostics> = _diagnostics.asStateFlow()

    private val _pointBatches = MutableSharedFlow<List<ScannerPoint>>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val pointBatches: SharedFlow<List<ScannerPoint>> = _pointBatches.asSharedFlow()

    @Volatile
    var captureEnabled: Boolean = false

    private var session: Session? = null
    private var sessionResumed = false
    private var installRequested = false
    private var cameraTextureName = -1

    private var viewportRotation = 0
    private var viewportWidth = 1
    private var viewportHeight = 1

    private var lastDepthTimestamp = -1L
    private var lastDepthRealtimeNanos = 0L
    private var acceptedPointsTotal = 0L

    fun markCameraPermissionMissing() {
        _diagnostics.value = _diagnostics.value.copy(
            runtimeState = ScannerRuntimeState.NEEDS_PERMISSION,
            trackingState = "STOPPED",
            lastError = "Camera permission required"
        )
    }

    /** Create/configure/resume ARCore. Safe to call repeatedly from lifecycle ON_RESUME. */
    fun resume() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            markCameraPermissionMissing()
            return
        }

        synchronized(frameLock) {
            try {
                if (session == null) {
                    when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            installRequested = true
                            _diagnostics.value = _diagnostics.value.copy(
                                runtimeState = ScannerRuntimeState.INSTALL_REQUESTED,
                                trackingState = "STOPPED",
                                lastError = null
                            )
                            return
                        }
                        ArCoreApk.InstallStatus.INSTALLED -> Unit
                    }

                    val newSession = Session(activity)
                    val config = newSession.config
                    val automaticDepthSupported =
                        newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)

                    val selectedMode = if (automaticDepthSupported) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }
                    config.depthMode = selectedMode
                    newSession.configure(config)
                    newSession.setDisplayGeometry(
                        viewportRotation,
                        max(1, viewportWidth),
                        max(1, viewportHeight)
                    )
                    session = newSession

                    _diagnostics.value = _diagnostics.value.copy(
                        runtimeState = if (automaticDepthSupported) {
                            ScannerRuntimeState.READY
                        } else {
                            ScannerRuntimeState.ERROR
                        },
                        depthSupported = automaticDepthSupported,
                        depthMode = selectedMode.name,
                        depthSource = if (automaticDepthSupported) "FULL_DEPTH" else "NONE",
                        confidenceAvailable = false,
                        lastError = if (automaticDepthSupported) null else
                            "ARCore AUTOMATIC Depth is not supported by this camera"
                    )
                }

                if (!sessionResumed) {
                    session?.resume()
                    sessionResumed = true
                    if (_diagnostics.value.depthSupported == true) {
                        _diagnostics.value = _diagnostics.value.copy(
                            runtimeState = ScannerRuntimeState.READY,
                            lastError = null
                        )
                    }
                }
            } catch (e: CameraNotAvailableException) {
                fail("Camera unavailable: ${e.message ?: e.javaClass.simpleName}")
            } catch (t: Throwable) {
                fail("ARCore start failed: ${t.message ?: t.javaClass.simpleName}")
            }
        }
    }

    fun pause() {
        synchronized(frameLock) {
            if (sessionResumed) {
                try {
                    session?.pause()
                } catch (t: Throwable) {
                    _diagnostics.value = _diagnostics.value.copy(
                        lastError = "ARCore pause failed: ${t.message ?: t.javaClass.simpleName}"
                    )
                }
                sessionResumed = false
            }
            _diagnostics.value = _diagnostics.value.copy(trackingState = "STOPPED")
        }
    }

    fun destroy() {
        synchronized(frameLock) {
            try {
                session?.close()
            } finally {
                session = null
                sessionResumed = false
                cameraTextureName = -1
                _diagnostics.value = _diagnostics.value.copy(
                    runtimeState = ScannerRuntimeState.IDLE,
                    trackingState = "STOPPED"
                )
            }
        }
    }

    fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        viewportRotation = rotation
        viewportWidth = max(1, width)
        viewportHeight = max(1, height)
        synchronized(frameLock) {
            session?.setDisplayGeometry(viewportRotation, viewportWidth, viewportHeight)
        }
    }

    /** Called only from the GL thread. Returns the current frame for camera-background drawing. */
    fun updateFrame(textureName: Int): Frame? {
        synchronized(frameLock) {
            val activeSession = session ?: return null
            if (!sessionResumed) return null

            return try {
                if (cameraTextureName != textureName) {
                    activeSession.setCameraTextureName(textureName)
                    cameraTextureName = textureName
                }

                val frame = activeSession.update()
                val camera = frame.camera
                _diagnostics.value = _diagnostics.value.copy(
                    trackingState = camera.trackingState.name
                )

                if (camera.trackingState == TrackingState.TRACKING &&
                    _diagnostics.value.depthSupported == true
                ) {
                    processDenseDepth(frame)
                }
                frame
            } catch (t: Throwable) {
                _diagnostics.value = _diagnostics.value.copy(
                    lastError = "Frame update failed: ${t.message ?: t.javaClass.simpleName}"
                )
                null
            }
        }
    }

    /**
     * Full Depth is dense/smoothed and intentionally has no confidence image. Every non-zero depth
     * sample inside the configured 0.1..5m scan range is allowed into the proof pipeline.
     */
    private fun processDenseDepth(frame: Frame) {
        try {
            frame.acquireDepthImage16Bits().use { depthImage ->
                val timestamp = depthImage.timestamp
                if (timestamp == lastDepthTimestamp) return
                lastDepthTimestamp = timestamp

                val now = SystemClock.elapsedRealtimeNanos()
                val previousRealtime = lastDepthRealtimeNanos
                lastDepthRealtimeNanos = now

                val previous = _diagnostics.value
                val instantaneousFps = if (previousRealtime > 0L && now > previousRealtime) {
                    1_000_000_000f / (now - previousRealtime).toFloat()
                } else {
                    0f
                }
                val smoothedFps = when {
                    instantaneousFps <= 0f -> previous.depthFps
                    previous.depthFps <= 0f -> instantaneousFps
                    else -> previous.depthFps * 0.8f + instantaneousFps * 0.2f
                }

                val depthWidth = depthImage.width
                val depthHeight = depthImage.height
                val depthPlane = depthImage.planes[0]
                val depthBuffer = depthPlane.buffer

                // Keep the proof path bounded while still sampling the full image footprint.
                val targetSamples = 1500f
                val step = max(
                    1,
                    ceil(sqrt(depthWidth.toFloat() * depthHeight.toFloat() / targetSamples)).toInt()
                )

                val intrinsics = frame.camera.textureIntrinsics
                val focal = intrinsics.focalLength
                val principal = intrinsics.principalPoint
                val sourceDimensions = intrinsics.imageDimensions
                val pose = frame.camera.pose

                var valid = 0
                val accepted = ArrayList<ScannerPoint>(targetSamples.toInt())

                for (y in 0 until depthHeight step step) {
                    for (x in 0 until depthWidth step step) {
                        val depthOffset = DepthImageMath.byteOffset(
                            x,
                            y,
                            depthPlane.rowStride,
                            depthPlane.pixelStride
                        )
                        if (depthOffset < 0 || depthOffset + 1 >= depthBuffer.limit()) continue

                        val depthMm = DepthImageMath.readUnsignedLittleEndian16(depthBuffer, depthOffset)
                        if (!DepthImageMath.isUsableDenseDepth(depthMm)) continue
                        valid++

                        val cameraPoint = DepthImageMath.depthPixelToCameraPoint(
                            pixelX = x.toFloat(),
                            pixelY = y.toFloat(),
                            depthMillimetres = depthMm,
                            sourceFx = focal[0],
                            sourceFy = focal[1],
                            sourceCx = principal[0],
                            sourceCy = principal[1],
                            sourceWidth = sourceDimensions[0],
                            sourceHeight = sourceDimensions[1],
                            depthWidth = depthWidth,
                            depthHeight = depthHeight
                        )
                        val world = pose.transformPoint(
                            floatArrayOf(cameraPoint.x, cameraPoint.y, cameraPoint.z)
                        )
                        accepted.add(
                            ScannerPoint(
                                x = world[0],
                                y = world[1],
                                z = world[2],
                                // Full Depth exposes no confidence image. This is only fusion weight.
                                confidence = 255
                            )
                        )
                    }
                }

                acceptedPointsTotal += accepted.size
                _diagnostics.value = previous.copy(
                    runtimeState = ScannerRuntimeState.READY,
                    trackingState = frame.camera.trackingState.name,
                    depthSource = "FULL_DEPTH",
                    depthFrames = previous.depthFrames + 1L,
                    depthFps = smoothedFps,
                    depthWidth = depthWidth,
                    depthHeight = depthHeight,
                    validDepthSamples = valid,
                    confidenceAvailable = false,
                    confidenceMin = 0,
                    confidenceAverage = 0,
                    confidenceMax = 0,
                    confidenceAtLeast32 = 0,
                    confidenceAtLeast128 = 0,
                    acceptedPoints = acceptedPointsTotal,
                    lastFrameAcceptedPoints = accepted.size,
                    lastError = null
                )

                if (captureEnabled && accepted.isNotEmpty()) {
                    _pointBatches.tryEmit(accepted)
                }
            }
        } catch (_: NotYetAvailableException) {
            val previous = _diagnostics.value
            _diagnostics.value = previous.copy(
                notYetAvailableCount = previous.notYetAvailableCount + 1L
            )
        } catch (t: Throwable) {
            _diagnostics.value = _diagnostics.value.copy(
                lastError = "Depth read failed: ${t.message ?: t.javaClass.simpleName}"
            )
        }
    }

    private fun fail(message: String) {
        _diagnostics.value = _diagnostics.value.copy(
            runtimeState = ScannerRuntimeState.ERROR,
            trackingState = "STOPPED",
            lastError = message
        )
    }
}
