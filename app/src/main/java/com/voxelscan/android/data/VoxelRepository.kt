package com.voxelscan.android.data

import android.content.Context
import com.voxelscan.android.exporter.MagicaVoxExporter
import com.voxelscan.android.exporter.ObjExporter
import com.voxelscan.android.exporter.PlyExporter
import com.voxelscan.android.model.VoxelProject
import com.voxelscan.android.model.VoxelWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central repository managing current scan session, saved projects, and 3D export generation.
 */
class VoxelRepository(private val context: Context) {

    val activeWorld = VoxelWorld()

    private val _projects = MutableStateFlow<List<VoxelProject>>(VoxelProject.createShowcaseProjects())
    val projects: StateFlow<List<VoxelProject>> = _projects.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun setScanning(active: Boolean) {
        _isScanning.value = active
    }

    fun toggleScanning() {
        _isScanning.value = !_isScanning.value
    }

    fun clearActiveScan() {
        activeWorld.clear()
        _isScanning.value = false
    }

    fun saveCurrentScanAsProject(title: String, category: String = "Objekte"): VoxelProject {
        val entries = activeWorld.entries()
        val bounds = activeWorld.calculateBounds(entries)
        val boundsDesc = String.format(
            Locale.US,
            "%.1fm × %.1fm × %.1fm",
            bounds.widthMeters(activeWorld.voxelSize),
            bounds.heightMeters(activeWorld.voxelSize),
            bounds.depthMeters(activeWorld.voxelSize)
        )
        val now = SimpleDateFormat("Heute, HH:mm", Locale.GERMANY).format(Date())

        val newProject = VoxelProject(
            title = title.ifBlank { "Live Scan #${_projects.value.size + 1}" },
            category = category,
            voxelCount = entries.size,
            voxelSizeMeters = activeWorld.voxelSize,
            boundsDescription = boundsDesc,
            fileSizeBytes = (entries.size * 250L),
            timestampFormatted = now,
            isFavorite = false,
            isExported = false,
            tags = listOf("LiDAR", "Neu", "${(activeWorld.voxelSize * 100).toInt()}cm Grid"),
            sampleVoxels = entries
        )

        _projects.value = listOf(newProject) + _projects.value
        return newProject
    }

    fun toggleFavorite(projectId: String) {
        _projects.value = _projects.value.map {
            if (it.id == projectId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun deleteProject(projectId: String) {
        _projects.value = _projects.value.filter { it.id != projectId }
    }

    fun exportActiveScan(format: String, paletteSize: Int = 255): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()

        return when (format.lowercase()) {
            "vox" -> {
                val file = File(dir, "voxelscan_$timestamp.vox")
                MagicaVoxExporter.export(activeWorld, file, paletteSize)
            }
            "obj" -> {
                val file = File(dir, "voxelscan_$timestamp.obj")
                ObjExporter.export(activeWorld, file)
            }
            "ply" -> {
                val file = File(dir, "voxelscan_$timestamp.ply")
                PlyExporter.export(activeWorld, file)
            }
            else -> {
                val file = File(dir, "voxelscan_$timestamp.ply")
                PlyExporter.export(activeWorld, file)
            }
        }
    }

    fun loadProjectIntoActiveWorld(project: VoxelProject) {
        activeWorld.clear()
        activeWorld.setVoxelSize(project.voxelSizeMeters)
        val voxels = project.sampleVoxels
        for (e in voxels) {
            activeWorld.addPoint(
                worldX = e.x * activeWorld.voxelSize,
                worldY = e.y * activeWorld.voxelSize,
                worldZ = e.z * activeWorld.voxelSize,
                r = e.voxel.r.toInt(),
                g = e.voxel.g.toInt(),
                b = e.voxel.b.toInt(),
                weight = e.voxel.samples
            )
        }
    }
}
