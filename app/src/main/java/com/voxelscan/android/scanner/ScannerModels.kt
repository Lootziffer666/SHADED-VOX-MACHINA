package com.voxelscan.android.scanner

/** Runtime state of the real ARCore scanner backend. */
enum class ScannerRuntimeState {
    IDLE,
    NEEDS_PERMISSION,
    INSTALL_REQUESTED,
    READY,
    ERROR
}

/** A real world-space point derived from an ARCore raw-depth sample. */
data class ScannerPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val confidence: Int
)

/** Truthful observability for every boundary in the Phase-1 scan pipeline. */
data class ScannerDiagnostics(
    val runtimeState: ScannerRuntimeState = ScannerRuntimeState.IDLE,
    val trackingState: String = "STOPPED",
    val depthSupported: Boolean? = null,
    val depthMode: String = "DISABLED",
    val depthFrames: Long = 0L,
    val depthFps: Float = 0f,
    val depthWidth: Int = 0,
    val depthHeight: Int = 0,
    val validDepthSamples: Int = 0,
    val confidenceMin: Int = 0,
    val confidenceAverage: Int = 0,
    val confidenceMax: Int = 0,
    val confidenceAtLeast32: Int = 0,
    val confidenceAtLeast128: Int = 0,
    val acceptedPoints: Long = 0L,
    val lastFrameAcceptedPoints: Int = 0,
    val notYetAvailableCount: Long = 0L,
    val lastError: String? = null
)
