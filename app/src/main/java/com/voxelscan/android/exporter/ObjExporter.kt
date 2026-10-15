package com.voxelscan.android.exporter

import com.voxelscan.android.model.VoxelWorld
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Locale

/**
 * Exporter for Wavefront .OBJ 3D mesh files with vertex colors or colored face materials.
 */
object ObjExporter {

    fun export(
        world: VoxelWorld,
        outputFile: File,
        confirmedOnly: Boolean = true
    ): File {
        val entries = if (confirmedOnly) world.confirmedEntries() else world.entries()
        if (entries.isEmpty()) {
            throw IllegalStateException("No voxels to export.")
        }

        val voxelSize = world.voxelSize
        val half = voxelSize * 0.5f

        BufferedWriter(FileWriter(outputFile)).use { writer ->
            writer.write("# VoxelScan AR - VoxelWorld Engine OBJ Export\n")
            writer.write("# Voxels: ${entries.size}\n")
            writer.write("# Resolution: ${voxelSize}m\n\n")

            // Cube 8 vertex templates relative to center
            val cubeVerts = arrayOf(
                floatArrayOf(-half, -half, -half),
                floatArrayOf( half, -half, -half),
                floatArrayOf( half,  half, -half),
                floatArrayOf(-half,  half, -half),
                floatArrayOf(-half, -half,  half),
                floatArrayOf( half, -half,  half),
                floatArrayOf( half,  half,  half),
                floatArrayOf(-half,  half,  half)
            )

            // 6 quad faces (1-based index offsets)
            val faceIndices = arrayOf(
                intArrayOf(1, 4, 3, 2), // Front
                intArrayOf(5, 6, 7, 8), // Back
                intArrayOf(1, 2, 6, 5), // Bottom
                intArrayOf(3, 4, 8, 7), // Top
                intArrayOf(1, 5, 8, 4), // Left
                intArrayOf(2, 3, 7, 6)  // Right
            )

            var vOffset = 1
            for (e in entries) {
                val cx = e.x * voxelSize
                val cy = e.y * voxelSize
                val cz = e.z * voxelSize

                val rNorm = (e.voxel.r / 255f).coerceIn(0f, 1f)
                val gNorm = (e.voxel.g / 255f).coerceIn(0f, 1f)
                val bNorm = (e.voxel.b / 255f).coerceIn(0f, 1f)

                // Write 8 vertices with RGB vertex color extension: v x y z r g b
                for (v in cubeVerts) {
                    writer.write(
                        String.format(
                            Locale.US,
                            "v %.4f %.4f %.4f %.3f %.3f %.3f\n",
                            cx + v[0], cy + v[1], cz + v[2],
                            rNorm, gNorm, bNorm
                        )
                    )
                }

                // Write 6 faces
                for (f in faceIndices) {
                    writer.write(
                        "f ${vOffset + f[0] - 1} ${vOffset + f[1] - 1} ${vOffset + f[2] - 1} ${vOffset + f[3] - 1}\n"
                    )
                }

                vOffset += 8
            }
        }

        return outputFile
    }
}
