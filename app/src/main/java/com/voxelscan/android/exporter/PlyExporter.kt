package com.voxelscan.android.exporter

import com.voxelscan.android.model.VoxelWorld
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Locale

/**
 * Exporter for Stanford .PLY point cloud and volumetric vertex format.
 */
object PlyExporter {

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

        BufferedWriter(FileWriter(outputFile)).use { writer ->
            writer.write("ply\n")
            writer.write("format ascii 1.0\n")
            writer.write("comment VoxelScan AR - VoxelWorld Engine PLY Export\n")
            writer.write("element vertex ${entries.size}\n")
            writer.write("property float x\n")
            writer.write("property float y\n")
            writer.write("property float z\n")
            writer.write("property uchar red\n")
            writer.write("property uchar green\n")
            writer.write("property uchar blue\n")
            writer.write("property float confidence\n")
            writer.write("end_header\n")

            for (e in entries) {
                val vx = e.x * voxelSize
                val vy = e.y * voxelSize
                val vz = e.z * voxelSize
                val r = e.voxel.r.toInt().coerceIn(0, 255)
                val g = e.voxel.g.toInt().coerceIn(0, 255)
                val b = e.voxel.b.toInt().coerceIn(0, 255)
                val conf = e.voxel.confidence()

                writer.write(
                    String.format(
                        Locale.US,
                        "%.4f %.4f %.4f %d %d %d %.2f\n",
                        vx, vy, vz, r, g, b, conf
                    )
                )
            }
        }

        return outputFile
    }
}
