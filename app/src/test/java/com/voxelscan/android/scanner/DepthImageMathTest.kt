package com.voxelscan.android.scanner

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepthImageMathTest {

    @Test
    fun unsignedConfidencePreservesFullByteRange() {
        assertEquals(0, DepthImageMath.unsignedByte(0x00.toByte()))
        assertEquals(127, DepthImageMath.unsignedByte(0x7F.toByte()))
        assertEquals(128, DepthImageMath.unsignedByte(0x80.toByte()))
        assertEquals(255, DepthImageMath.unsignedByte(0xFF.toByte()))
    }

    @Test
    fun byteOffsetUsesRowAndPixelStride() {
        assertEquals(0, DepthImageMath.byteOffset(x = 0, y = 0, rowStride = 16, pixelStride = 2))
        assertEquals(6, DepthImageMath.byteOffset(x = 3, y = 0, rowStride = 16, pixelStride = 2))
        assertEquals(38, DepthImageMath.byteOffset(x = 3, y = 2, rowStride = 16, pixelStride = 2))
    }

    @Test
    fun littleEndianDepthIsUnsigned16Bit() {
        val buffer = ByteBuffer.wrap(byteArrayOf(
            0x34.toByte(), 0x12.toByte(),
            0xFF.toByte(), 0xFF.toByte()
        ))

        assertEquals(0x1234, DepthImageMath.readUnsignedLittleEndian16(buffer, 0))
        assertEquals(65535, DepthImageMath.readUnsignedLittleEndian16(buffer, 2))
    }

    @Test
    fun denseDepthAcceptsOnlyConfiguredScanRange() {
        assertFalse(DepthImageMath.isUsableDenseDepth(0))
        assertFalse(DepthImageMath.isUsableDenseDepth(99))
        assertTrue(DepthImageMath.isUsableDenseDepth(100))
        assertTrue(DepthImageMath.isUsableDenseDepth(2500))
        assertTrue(DepthImageMath.isUsableDenseDepth(5000))
        assertFalse(DepthImageMath.isUsableDenseDepth(5001))
    }

    @Test
    fun depthPixelUsesIntrinsicsScaledToDepthImage() {
        // Source camera intrinsics are for 640x480. The depth image is 320x240,
        // so focal length and principal point must be scaled by 0.5.
        val point = DepthImageMath.depthPixelToCameraPoint(
            pixelX = 210f,
            pixelY = 100f,
            depthMillimetres = 2000,
            sourceFx = 500f,
            sourceFy = 500f,
            sourceCx = 320f,
            sourceCy = 240f,
            sourceWidth = 640,
            sourceHeight = 480,
            depthWidth = 320,
            depthHeight = 240
        )

        assertTrue(kotlin.math.abs(point.x - 0.4f) < 0.0001f)
        assertTrue(kotlin.math.abs(point.y - 0.16f) < 0.0001f)
        assertTrue(kotlin.math.abs(point.z - (-2.0f)) < 0.0001f)
    }
}
