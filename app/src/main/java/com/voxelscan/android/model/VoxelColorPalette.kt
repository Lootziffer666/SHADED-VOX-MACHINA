package com.voxelscan.android.model

import kotlin.math.sqrt

/**
 * 256-color palette manager for MagicaVoxel VOX binary chunk encoding and color reduction.
 */
class VoxelColorPalette private constructor(
    val colors: IntArray // array of 256 ARGB colors
) {
    companion object {
        const val PALETTE_SIZE = 256

        /**
         * Builds a 256-color palette from world entries with quantization.
         * If maxColors < 255, quantizes down to maxColors dominant colors.
         */
        fun from(entries: List<VoxelWorld.Entry>, maxColors: Int = 255): VoxelColorPalette {
            val palette = IntArray(PALETTE_SIZE)

            if (entries.isEmpty()) {
                // Populate default gradient palette
                return defaultPalette()
            }

            // Collect unique colors
            val colorFreq = HashMap<Int, Int>()
            for (e in entries) {
                // round to reduce noise
                val r = (e.voxel.r.toInt() and 0xF8)
                val g = (e.voxel.g.toInt() and 0xF8)
                val b = (e.voxel.b.toInt() and 0xF8)
                val rgb = (r shl 16) or (g shl 8) or b
                colorFreq[rgb] = (colorFreq[rgb] ?: 0) + 1
            }

            // Sort by frequency
            val sortedColors = colorFreq.entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(maxColors.coerceIn(4, 255))

            // In MagicaVoxel: Index 0 is reserved / empty. Usable indices are 1..255.
            palette[0] = 0x00000000 // Alpha 0
            for (i in sortedColors.indices) {
                palette[i + 1] = sortedColors[i] or -0x1000000 // Full opacity
            }

            // Fill remaining slots with gradient fallback
            for (i in (sortedColors.size + 1) until PALETTE_SIZE) {
                val t = i.toFloat() / PALETTE_SIZE
                val r = (36 + (t * 200)).toInt()
                val g = (140 + (t * 80)).toInt().coerceAtMost(255)
                val b = (120 + ((1f - t) * 100)).toInt().coerceAtMost(255)
                palette[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }

            return VoxelColorPalette(palette)
        }

        fun defaultPalette(): VoxelColorPalette {
            val palette = IntArray(PALETTE_SIZE)
            palette[0] = 0
            for (i in 1 until PALETTE_SIZE) {
                val t = (i - 1) / 254f
                val r = (sin(t * Math.PI) * 220).toInt()
                val g = (sin((t + 0.33) * Math.PI) * 230).toInt().coerceIn(0, 255)
                val b = (sin((t + 0.66) * Math.PI) * 240).toInt().coerceIn(0, 255)
                palette[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            return VoxelColorPalette(palette)
        }

        private fun sin(rad: Double): Double = kotlin.math.abs(kotlin.math.sin(rad))
    }

    /**
     * Finds nearest 1-based palette index for the given RGB color.
     */
    fun findNearestIndex(r: Int, g: Int, b: Int): Int {
        var closestIdx = 1
        var minDistance = Float.MAX_VALUE

        // Iterate through active palette indices 1..255
        for (i in 1 until PALETTE_SIZE) {
            val color = colors[i]
            val pr = (color shr 16) and 0xFF
            val pg = (color shr 8) and 0xFF
            val pb = color and 0xFF

            val dr = (r - pr)
            val dg = (g - pg)
            val db = (b - pb)
            val dist = (dr * dr * 0.299f) + (dg * dg * 0.587f) + (db * db * 0.114f)

            if (dist < minDistance) {
                minDistance = dist
                closestIdx = i
                if (dist == 0f) break
            }
        }
        return closestIdx
    }

    /**
     * Generates 1024-byte RGBA buffer required by MagicaVoxel RGBA chunk.
     */
    fun magicaVoxelBytes(): ByteArray {
        val bytes = ByteArray(1024)
        for (i in 0 until 256) {
            val c = colors[i]
            val offset = i * 4
            bytes[offset] = ((c shr 16) and 0xFF).toByte() // R
            bytes[offset + 1] = ((c shr 8) and 0xFF).toByte()  // G
            bytes[offset + 2] = (c and 0xFF).toByte()         // B
            bytes[offset + 3] = ((c ushr 24) and 0xFF).toByte() // A
        }
        return bytes
    }
}
