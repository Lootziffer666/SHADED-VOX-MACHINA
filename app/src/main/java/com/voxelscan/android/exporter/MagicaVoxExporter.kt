package com.voxelscan.android.exporter

import com.voxelscan.android.model.VoxelColorPalette
import com.voxelscan.android.model.VoxelWorld
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Exporter for Ephtracy's MagicaVoxel .vox binary file format.
 */
object MagicaVoxExporter {

    private fun writeIntLE(stream: ByteArrayOutputStream, value: Int) {
        stream.write(value and 0xFF)
        stream.write((value shr 8) and 0xFF)
        stream.write((value shr 16) and 0xFF)
        stream.write((value shr 24) and 0xFF)
    }

    private fun createChunk(id: String, content: ByteArray, children: ByteArray = ByteArray(0)): ByteArray {
        val stream = ByteArrayOutputStream()
        // 4 bytes ID
        stream.write(id.toByteArray(Charsets.US_ASCII))
        // 4 bytes content size (LE)
        writeIntLE(stream, content.size)
        // 4 bytes children size (LE)
        writeIntLE(stream, children.size)
        // content bytes
        stream.write(content)
        // children bytes
        stream.write(children)
        return stream.toByteArray()
    }

    fun export(
        world: VoxelWorld,
        outputFile: File,
        paletteColors: Int = 255,
        confirmedOnly: Boolean = true
    ): File {
        val entries = if (confirmedOnly) world.confirmedEntries() else world.entries()
        if (entries.isEmpty()) {
            throw IllegalStateException("No voxels to export.")
        }

        val bounds = world.calculateBounds(entries)
        val sizeX = (bounds.sizeX).coerceIn(1, 256)
        val sizeY = (bounds.sizeY).coerceIn(1, 256)
        val sizeZ = (bounds.sizeZ).coerceIn(1, 256)

        val palette = VoxelColorPalette.from(entries, paletteColors)

        // 1. SIZE Chunk Content (3 x int32 LE)
        val sizeContentStream = ByteArrayOutputStream()
        writeIntLE(sizeContentStream, sizeX)
        writeIntLE(sizeContentStream, sizeZ) // Note: MagicaVoxel uses Z as vertical height in older specs, swapping for correct upright model
        writeIntLE(sizeContentStream, sizeY)
        val sizeChunk = createChunk("SIZE", sizeContentStream.toByteArray())

        // 2. XYZI Chunk Content (numVoxels + 4 bytes per voxel: x, y, z, colorIndex)
        val xyziContentStream = ByteArrayOutputStream()
        writeIntLE(xyziContentStream, entries.size)
        for (e in entries) {
            // Normalize coordinates into [0, size-1]
            val localX = (e.x - bounds.minX).coerceIn(0, sizeX - 1)
            val localY = (e.y - bounds.minY).coerceIn(0, sizeY - 1)
            val localZ = (e.z - bounds.minZ).coerceIn(0, sizeZ - 1)

            val colorIdx = palette.findNearestIndex(
                e.voxel.r.toInt(),
                e.voxel.g.toInt(),
                e.voxel.b.toInt()
            )

            xyziContentStream.write(localX)
            xyziContentStream.write(localZ) // Swapped Y/Z for upright MagicaVoxel orientation
            xyziContentStream.write(localY)
            xyziContentStream.write(colorIdx)
        }
        val xyziChunk = createChunk("XYZI", xyziContentStream.toByteArray())

        // 3. RGBA Chunk Content (1024 bytes)
        val rgbaChunk = createChunk("RGBA", palette.magicaVoxelBytes())

        // Assemble MAIN chunk children
        val mainChildren = ByteArrayOutputStream()
        mainChildren.write(sizeChunk)
        mainChildren.write(xyziChunk)
        mainChildren.write(rgbaChunk)

        val mainChunk = createChunk("MAIN", ByteArray(0), mainChildren.toByteArray())

        // Assemble full file: "VOX " + version 150 + MAIN chunk
        FileOutputStream(outputFile).use { fos ->
            val out = ByteArrayOutputStream()
            out.write("VOX ".toByteArray(Charsets.US_ASCII))
            writeIntLE(out, 150) // Version 150
            out.write(mainChunk)
            fos.write(out.toByteArray())
        }

        return outputFile
    }
}
