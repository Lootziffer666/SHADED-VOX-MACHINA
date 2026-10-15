package com.voxelscan.android.model

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D spatial voxel world managing volumetric grid cells, coordinate transformations,
 * raycast carving, spatial filtering, and bounding box metrics.
 */
class VoxelWorld(
    initialVoxelSize: Float = DEFAULT_VOXEL_SIZE
) {
    companion object {
        const val DEFAULT_VOXEL_SIZE = 0.05f // 5cm default
        const val MAX_VOXELS_LIMIT = 50000

        fun packKey(x: Int, y: Int, z: Int): Long {
            val mask = 0x1FFFFFL // 21-bit signed coordinate
            val px = (x.toLong() and mask) shl 42
            val py = (y.toLong() and mask) shl 21
            val pz = z.toLong() and mask
            return px or py or pz
        }

        fun unpackX(key: Long): Int {
            val raw = ((key ushr 42) and 0x1FFFFFL).toInt()
            return if ((raw and 0x100000) != 0) raw or -0x200000 else raw
        }

        fun unpackY(key: Long): Int {
            val raw = ((key ushr 21) and 0x1FFFFFL).toInt()
            return if ((raw and 0x100000) != 0) raw or -0x200000 else raw
        }

        fun unpackZ(key: Long): Int {
            val raw = (key and 0x1FFFFFL).toInt()
            return if ((raw and 0x100000) != 0) raw or -0x200000 else raw
        }
    }

    data class Entry(val x: Int, val y: Int, val z: Int, val voxel: Voxel)

    data class Bounds(
        val minX: Int, val minY: Int, val minZ: Int,
        val maxX: Int, val maxY: Int, val maxZ: Int
    ) {
        val sizeX: Int get() = if (maxX >= minX) (maxX - minX + 1) else 0
        val sizeY: Int get() = if (maxY >= minY) (maxY - minY + 1) else 0
        val sizeZ: Int get() = if (maxZ >= minZ) (maxZ - minZ + 1) else 0

        fun widthMeters(voxelSize: Float): Float = sizeX * voxelSize
        fun heightMeters(voxelSize: Float): Float = sizeY * voxelSize
        fun depthMeters(voxelSize: Float): Float = sizeZ * voxelSize
    }

    var voxelSize: Float = initialVoxelSize
        private set

    var axisYawDegrees: Float = 0.0f
        private set

    private var cosYaw: Float = 1.0f
    private var sinYaw: Float = 0.0f

    private val voxels = ConcurrentHashMap<Long, Voxel>()

    fun setVoxelSize(size: Float): Boolean {
        if (voxels.isNotEmpty()) return false
        voxelSize = size.coerceIn(0.01f, 0.20f)
        return true
    }

    fun setAxisYaw(degrees: Float) {
        axisYawDegrees = degrees
        val rad = Math.toRadians(degrees.toDouble()).toFloat()
        cosYaw = cos(rad)
        sinYaw = sin(rad)
    }

    fun clear(): Int {
        val count = voxels.size
        voxels.clear()
        return count
    }

    fun count(): Int = voxels.size

    fun confirmedCount(threshold: Float = Voxel.DEFAULT_CONFIRM_THRESHOLD): Int {
        return voxels.values.count { it.isConfirmed(threshold) }
    }

    private fun worldToGridX(x: Float, z: Float): Float = (x * cosYaw) - (z * sinYaw)
    private fun worldToGridZ(x: Float, z: Float): Float = (x * sinYaw) + (z * cosYaw)

    private fun toIndex(coord: Float): Int = floor(coord / voxelSize).toInt()

    fun addPoint(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        r: Int,
        g: Int,
        b: Int,
        weight: Float = 1.0f,
        time: Long = System.currentTimeMillis(),
        frameId: Long = 0L
    ): Boolean {
        if (voxels.size >= MAX_VOXELS_LIMIT) return false

        val gx = worldToGridX(worldX, worldZ)
        val gz = worldToGridZ(worldX, worldZ)

        val ix = toIndex(gx)
        val iy = toIndex(worldY)
        val iz = toIndex(gz)

        val key = packKey(ix, iy, iz)
        val existing = voxels[key]
        if (existing != null) {
            existing.add(r, g, b, weight, time, frameId)
        } else {
            val v = Voxel(r.toFloat(), g.toFloat(), b.toFloat(), weight, time)
            v.lastHitFrame = frameId
            voxels[key] = v
        }
        return true
    }

    /**
     * Bresenham-like ray carving along camera ray to remove phantom voxels in free space.
     */
    fun carveRay(
        cameraX: Float,
        cameraY: Float,
        cameraZ: Float,
        hitX: Float,
        hitY: Float,
        hitZ: Float,
        frameId: Long = 0L
    ) {
        val dx = hitX - cameraX
        val dy = hitY - cameraY
        val dz = hitZ - cameraZ
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        if (distance < voxelSize * 2f) return

        val steps = (distance / (voxelSize * 0.8f)).toInt().coerceIn(2, 64)
        val stepX = dx / steps
        val stepY = dy / steps
        val stepZ = dz / steps

        val time = System.currentTimeMillis()

        // Stop 1 step before hit to preserve surface
        for (i in 1 until (steps - 1)) {
            val cx = cameraX + stepX * i
            val cy = cameraY + stepY * i
            val cz = cameraZ + stepZ * i

            val gx = worldToGridX(cx, cz)
            val gz = worldToGridZ(cx, cz)
            val ix = toIndex(gx)
            val iy = toIndex(cy)
            val iz = toIndex(gz)

            val key = packKey(ix, iy, iz)
            voxels[key]?.let { voxel ->
                voxel.observeFree(0.8f, time, frameId)
                if (voxel.isContradicted()) {
                    voxels.remove(key)
                }
            }
        }
    }

    fun entries(): List<Entry> {
        val list = ArrayList<Entry>(voxels.size)
        for ((key, voxel) in voxels) {
            list.add(Entry(unpackX(key), unpackY(key), unpackZ(key), voxel))
        }
        return list
    }

    fun confirmedEntries(threshold: Float = Voxel.DEFAULT_CONFIRM_THRESHOLD): List<Entry> {
        val list = ArrayList<Entry>()
        for ((key, voxel) in voxels) {
            if (voxel.isConfirmed(threshold)) {
                list.add(Entry(unpackX(key), unpackY(key), unpackZ(key), voxel))
            }
        }
        return list
    }

    fun calculateBounds(entries: List<Entry> = entries()): Bounds {
        if (entries.isEmpty()) return Bounds(0, 0, 0, 0, 0, 0)
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE

        for (e in entries) {
            if (e.x < minX) minX = e.x
            if (e.y < minY) minY = e.y
            if (e.z < minZ) minZ = e.z
            if (e.x > maxX) maxX = e.x
            if (e.y > maxY) maxY = e.y
            if (e.z > maxZ) maxZ = e.z
        }
        return Bounds(minX, minY, minZ, maxX, maxY, maxZ)
    }

    /**
     * Filter isolated voxels having fewer than [minNeighbors] adjacent solid cells.
     */
    fun removeOutliers(minNeighbors: Int = 2): Int {
        val toRemove = mutableListOf<Long>()
        for ((key, _) in voxels) {
            val x = unpackX(key)
            val y = unpackY(key)
            val z = unpackZ(key)

            var neighbors = 0
            for (dx in -1..1) {
                for (dy in -1..1) {
                    for (dz in -1..1) {
                        if (dx == 0 && dy == 0 && dz == 0) continue
                        val nKey = packKey(x + dx, y + dy, z + dz)
                        if (voxels.containsKey(nKey)) {
                            neighbors++
                        }
                    }
                }
            }
            if (neighbors < minNeighbors) {
                toRemove.add(key)
            }
        }
        for (k in toRemove) {
            voxels.remove(k)
        }
        return toRemove.size
    }

    /**
     * Estimated memory usage in Kilobytes for telemetry HUD.
     */
    fun memoryUsageKB(): Float {
        // approx 96 bytes per loaded Voxel object + hash table node
        return (voxels.size * 96f) / 1024f
    }
}
