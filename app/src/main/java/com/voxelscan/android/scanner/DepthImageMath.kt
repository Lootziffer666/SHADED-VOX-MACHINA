package com.voxelscan.android.scanner

import java.nio.ByteBuffer

/** Pure helpers for decoding ARCore depth buffers and projecting depth pixels. */
object DepthImageMath {
    data class CameraPoint(val x: Float, val y: Float, val z: Float)

    /** Convert Kotlin/Java's signed Byte to the unsigned 0..255 value ARCore raw confidence uses. */
    fun unsignedByte(value: Byte): Int = value.toInt() and 0xFF

    /** Byte offset of pixel (x,y) in an Android Image plane with arbitrary padding/strides. */
    fun byteOffset(x: Int, y: Int, rowStride: Int, pixelStride: Int): Int =
        y * rowStride + x * pixelStride

    /** Decode one unsigned 16-bit little-endian value without changing buffer position/order. */
    fun readUnsignedLittleEndian16(buffer: ByteBuffer, offset: Int): Int {
        val low = unsignedByte(buffer.get(offset))
        val high = unsignedByte(buffer.get(offset + 1))
        return low or (high shl 8)
    }

    /** Phase-2 scan range. Zero means ARCore has no estimate for that pixel. */
    fun isUsableDenseDepth(depthMillimetres: Int): Boolean =
        depthMillimetres in 100..5000

    /**
     * Project a depth pixel to the physical camera coordinate system.
     * ARCore camera coordinates use +X right, +Y up and -Z forward.
     */
    fun depthPixelToCameraPoint(
        pixelX: Float,
        pixelY: Float,
        depthMillimetres: Int,
        sourceFx: Float,
        sourceFy: Float,
        sourceCx: Float,
        sourceCy: Float,
        sourceWidth: Int,
        sourceHeight: Int,
        depthWidth: Int,
        depthHeight: Int
    ): CameraPoint {
        require(sourceWidth > 0 && sourceHeight > 0 && depthWidth > 0 && depthHeight > 0)
        require(sourceFx > 0f && sourceFy > 0f)

        val scaleX = depthWidth.toFloat() / sourceWidth.toFloat()
        val scaleY = depthHeight.toFloat() / sourceHeight.toFloat()
        val fx = sourceFx * scaleX
        val fy = sourceFy * scaleY
        val cx = sourceCx * scaleX
        val cy = sourceCy * scaleY
        val depthMetres = depthMillimetres / 1000f

        return CameraPoint(
            x = (pixelX - cx) * depthMetres / fx,
            y = -(pixelY - cy) * depthMetres / fy,
            z = -depthMetres
        )
    }
}
