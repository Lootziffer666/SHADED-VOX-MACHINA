package com.voxelscan.android.model

import kotlin.math.max
import kotlin.math.min

/**
 * Represents a single volumetric element in 3D grid space.
 * Accumulates color and confidence evidence from sensor observations.
 */
data class Voxel(
    var r: Float,
    var g: Float,
    var b: Float,
    var samples: Float = 1.0f,
    var lastSeen: Long = System.currentTimeMillis()
) {
    var freeSamples: Float = 0.0f
    var lastHitFrame: Long = Long.MIN_VALUE
    var lastFreeFrame: Long = Long.MIN_VALUE
    var lastFreeSeen: Long = 0L

    companion object {
        const val MAX_HIT_EVIDENCE = 30.0f
        const val MAX_FREE_EVIDENCE = 36.0f
        const val DEFAULT_CONFIRM_THRESHOLD = 3.0f
    }

    /**
     * Blends new sensor RGB hit observation with existing voxel evidence.
     */
    fun add(
        newR: Int,
        newG: Int,
        newB: Int,
        weight: Float = 1.0f,
        time: Long = System.currentTimeMillis(),
        frameId: Long = 0L
    ) {
        val safeWeight = max(0.05f, weight)
        val prevSamples = this.samples
        val total = prevSamples + safeWeight

        this.r = ((this.r * prevSamples) + (newR * safeWeight)) / total
        this.g = ((this.g * prevSamples) + (newG * safeWeight)) / total
        this.b = ((this.b * prevSamples) + (newB * safeWeight)) / total

        if (this.lastHitFrame != frameId) {
            this.samples = min(MAX_HIT_EVIDENCE, prevSamples + weight)
            this.freeSamples *= 0.94f // Decay free space evidence when hit confirms solid surface
            this.lastHitFrame = frameId
        }
        this.lastSeen = time
    }

    /**
     * Records free-space raycast observation (carving).
     */
    fun observeFree(
        weight: Float = 1.0f,
        time: Long = System.currentTimeMillis(),
        frameId: Long = 0L
    ) {
        if (this.lastFreeFrame == frameId) return
        this.freeSamples = min(MAX_FREE_EVIDENCE, this.freeSamples + weight)
        this.lastFreeSeen = time
        this.lastFreeFrame = frameId
    }

    /**
     * Whether this voxel has sufficient observation evidence to be considered a real surface.
     */
    fun isConfirmed(threshold: Float = DEFAULT_CONFIRM_THRESHOLD): Boolean {
        return samples >= threshold && freeSamples < (samples * 0.72f) + 3.0f
    }

    /**
     * True if free space rays have contradicted this voxel's existence.
     */
    fun isContradicted(): Boolean {
        return freeSamples >= 5.0f && freeSamples > (samples * 0.82f) + 3.5f
    }

    /**
     * Confidence rating between 0.0 and 1.0.
     */
    fun confidence(): Float {
        val hitRatio = min(1.0f, samples / 18.0f)
        val freePenalty = min(1.0f, freeSamples / 20.0f) * 0.4f
        return max(0.05f, hitRatio - freePenalty)
    }

    /**
     * Returns 24-bit RGB packed integer 0x00RRGGBB.
     */
    fun packedColor(): Int {
        val red = r.toInt().coerceIn(0, 255)
        val green = g.toInt().coerceIn(0, 255)
        val blue = b.toInt().coerceIn(0, 255)
        return (red shl 16) or (green shl 8) or blue
    }
}
