package com.voxelscan.android.model

import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Project data model for saved 3D voxel scans.
 */
data class VoxelProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String, // "Räume & Architektur", "Objekte", etc.
    val voxelCount: Int,
    val voxelSizeMeters: Float = 0.05f,
    val boundsDescription: String,
    val fileSizeBytes: Long,
    val timestampFormatted: String,
    var isFavorite: Boolean = false,
    var isExported: Boolean = false,
    val tags: List<String> = emptyList(),
    val sampleVoxels: List<VoxelWorld.Entry> = emptyList()
) {
    companion object {
        /**
         * Generates the pre-loaded showcase scan projects matching the HTML gallery mockups.
         */
        fun createShowcaseProjects(): List<VoxelProject> {
            return listOf(
                VoxelProject(
                    id = "proj-1-livingroom",
                    title = "Wohnzimmer Ostwand",
                    category = "Räume & Architektur",
                    voxelCount = 1420,
                    voxelSizeMeters = 0.05f,
                    boundsDescription = "2.8m × 2.2m × 1.4m",
                    fileSizeBytes = 394200L,
                    timestampFormatted = "Heute, 14:22",
                    isFavorite = true,
                    isExported = true,
                    tags = listOf("Interieur", "Möbel", "LiDAR"),
                    sampleVoxels = generateLivingRoomVoxels()
                ),
                VoxelProject(
                    id = "proj-2-mechcore",
                    title = "Cyberpunk Mech Core",
                    category = "Objekte",
                    voxelCount = 2850,
                    voxelSizeMeters = 0.02f,
                    boundsDescription = "0.8m × 1.2m × 0.8m",
                    fileSizeBytes = 724800L,
                    timestampFormatted = "Gestern, 18:45",
                    isFavorite = true,
                    isExported = true,
                    tags = listOf("Mech", "Skulptur", "High-Res"),
                    sampleVoxels = generateMechCoreVoxels()
                ),
                VoxelProject(
                    id = "proj-3-stele",
                    title = "Antike Stele / Artefakt",
                    category = "Objekte",
                    voxelCount = 1890,
                    voxelSizeMeters = 0.02f,
                    boundsDescription = "0.6m × 1.6m × 0.6m",
                    fileSizeBytes = 485100L,
                    timestampFormatted = "12. Okt 2026",
                    isFavorite = false,
                    isExported = false,
                    tags = listOf("Archäologie", "Relief", "Texturiert"),
                    sampleVoxels = generateSteleVoxels()
                ),
                VoxelProject(
                    id = "proj-4-espresso",
                    title = "Espresso Machine Studio",
                    category = "Objekte",
                    voxelCount = 980,
                    voxelSizeMeters = 0.01f,
                    boundsDescription = "0.4m × 0.5m × 0.4m",
                    fileSizeBytes = 248000L,
                    timestampFormatted = "10. Okt 2026",
                    isFavorite = false,
                    isExported = true,
                    tags = listOf("Hardware", "Tischgerät", "1cm Grid"),
                    sampleVoxels = generateEspressoVoxels()
                ),
                VoxelProject(
                    id = "proj-5-streetcorner",
                    title = "Neo-Tokyo Street Corner",
                    category = "Räume & Architektur",
                    voxelCount = 3200,
                    voxelSizeMeters = 0.10f,
                    boundsDescription = "6.4m × 4.8m × 5.2m",
                    fileSizeBytes = 912500L,
                    timestampFormatted = "08. Okt 2026",
                    isFavorite = true,
                    isExported = false,
                    tags = listOf("Outdoor", "Fassade", "Nacht"),
                    sampleVoxels = generateStreetCornerVoxels()
                )
            )
        }

        private fun generateLivingRoomVoxels(): List<VoxelWorld.Entry> {
            val list = mutableListOf<VoxelWorld.Entry>()
            // Floor plane
            for (x in -8..8) {
                for (z in -6..6) {
                    list.add(VoxelWorld.Entry(x, 0, z, Voxel(160f, 155f, 145f, 8f)))
                }
            }
            // Back wall
            for (x in -8..8) {
                for (y in 1..10) {
                    list.add(VoxelWorld.Entry(x, y, -6, Voxel(29f, 131f, 108f, 12f))) // Emerald wall
                }
            }
            // Sofa/Furniture blocks
            for (x in -4..4) {
                for (z in -2..2) {
                    for (y in 1..3) {
                        list.add(VoxelWorld.Entry(x, y, z, Voxel(253f, 151f, 84f, 15f))) // Amber couch
                    }
                }
            }
            // Coffee table
            for (x in -2..2) {
                for (z in 3..5) {
                    list.add(VoxelWorld.Entry(x, 2, z, Voxel(124f, 215f, 188f, 10f))) // Jade tabletop
                }
            }
            return list
        }

        private fun generateMechCoreVoxels(): List<VoxelWorld.Entry> {
            val list = mutableListOf<VoxelWorld.Entry>()
            // Central core reactor
            for (y in -6..6) {
                val r = 5 - kotlin.math.abs(y) / 2
                for (x in -r..r) {
                    for (z in -r..r) {
                        if (x * x + z * z <= r * r) {
                            val isSurface = (x * x + z * z >= (r - 1) * (r - 1)) || y == -6 || y == 6
                            if (isSurface) {
                                val red = if (kotlin.math.abs(y) <= 2) 56f else 36f
                                val green = if (kotlin.math.abs(y) <= 2) 189f else 156f
                                val blue = if (kotlin.math.abs(y) <= 2) 248f else 129f
                                list.add(VoxelWorld.Entry(x, y + 6, z, Voxel(red, green, blue, 14f)))
                            }
                        }
                    }
                }
            }
            // Surrounding mechanical pylons
            val pylonPositions = listOf(Pair(-6, -6), Pair(6, -6), Pair(-6, 6), Pair(6, 6))
            for ((px, pz) in pylonPositions) {
                for (y in 0..12) {
                    list.add(VoxelWorld.Entry(px, y, pz, Voxel(245f, 170f, 56f, 10f)))
                }
            }
            return list
        }

        private fun generateSteleVoxels(): List<VoxelWorld.Entry> {
            val list = mutableListOf<VoxelWorld.Entry>()
            // Tapered ancient monolith
            for (y in 0..16) {
                val w = 4 - (y / 5)
                for (x in -w..w) {
                    for (z in -w..w) {
                        if (x == -w || x == w || z == -w || z == w) {
                            val r = 70f + (y * 5)
                            val g = 140f + (y * 3)
                            val b = 130f + (y * 2)
                            list.add(VoxelWorld.Entry(x, y, z, Voxel(r, g, b, 9f)))
                        }
                    }
                }
            }
            return list
        }

        private fun generateEspressoVoxels(): List<VoxelWorld.Entry> {
            val list = mutableListOf<VoxelWorld.Entry>()
            for (x in -4..4) {
                for (y in 0..6) {
                    for (z in -4..4) {
                        if (y == 0 || y == 6 || x == -4 || x == 4 || z == -4) {
                            list.add(VoxelWorld.Entry(x, y, z, Voxel(200f, 205f, 210f, 12f))) // Metallic steel
                        }
                    }
                }
            }
            // Portafilter & amber dials
            list.add(VoxelWorld.Entry(0, 3, 5, Voxel(245f, 170f, 56f, 15f)))
            list.add(VoxelWorld.Entry(0, 3, 6, Voxel(40f, 40f, 40f, 15f)))
            return list
        }

        private fun generateStreetCornerVoxels(): List<VoxelWorld.Entry> {
            val list = mutableListOf<VoxelWorld.Entry>()
            // Pavement
            for (x in -10..10) {
                for (z in -10..10) {
                    list.add(VoxelWorld.Entry(x, 0, z, Voxel(50f, 55f, 60f, 10f)))
                }
            }
            // Corner shop facade
            for (x in -8..-1) {
                for (y in 1..12) {
                    for (z in -8..-1) {
                        if (x == -1 || z == -1) {
                            val color = if (y in 3..6 && (x in -6..-3 || z in -6..-3)) {
                                Voxel(56f, 189f, 248f, 16f) // Neon window
                            } else {
                                Voxel(29f, 131f, 108f, 12f) // Facade tile
                            }
                            list.add(VoxelWorld.Entry(x, y, z, color))
                        }
                    }
                }
            }
            return list
        }
    }
}
