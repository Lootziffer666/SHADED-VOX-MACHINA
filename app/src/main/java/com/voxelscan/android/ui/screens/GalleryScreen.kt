package com.voxelscan.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxelscan.android.data.VoxelRepository
import com.voxelscan.android.model.VoxelProject
import com.voxelscan.android.ui.components.VoxelCanvas3D
import com.voxelscan.android.ui.components.VoxelColorMode
import com.voxelscan.android.ui.components.VoxelRenderMode
import com.voxelscan.android.ui.theme.VoxelAmber
import com.voxelscan.android.ui.theme.VoxelCoral
import com.voxelscan.android.ui.theme.VoxelCyanGlow
import com.voxelscan.android.ui.theme.VoxelDarkBackground
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceBorder
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceContainer
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceHigh
import com.voxelscan.android.ui.theme.VoxelJadePrimary
import com.voxelscan.android.ui.theme.VoxelLaserGreen
import com.voxelscan.android.ui.theme.VoxelMintLight
import com.voxelscan.android.ui.theme.VoxelMintPeak

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    repository: VoxelRepository,
    onOpenInLiveScan: (VoxelProject) -> Unit
) {
    val context = LocalContext.current
    val projects by repository.projects.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Alle") }
    val filters = remember { listOf("Alle", "Räume & Architektur", "Objekte", "★ Favoriten", "Exportiert") }

    var inspectingProject by remember { mutableStateOf<VoxelProject?>(null) }

    val filteredProjects = remember(projects, searchQuery, selectedFilter) {
        projects.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.title.contains(searchQuery, ignoreCase = true) ||
                p.tags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                "Alle" -> true
                "Räume & Architektur" -> p.category == "Räume & Architektur"
                "Objekte" -> p.category == "Objekte"
                "★ Favoriten" -> p.isFavorite
                "Exportiert" -> p.isExported
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoxelDarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VoxelJadePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VoxelScan AR",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(VoxelDarkSurfaceHigh)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "v2.4",
                                        color = VoxelMintLight,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Scans Galerie",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Total Count Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(VoxelDarkSurfaceContainer)
                            .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${projects.size} SCANS",
                            color = VoxelMintPeak,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Scans, Tags oder Chunks filtern...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Suche", tint = Color.Gray)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gallery_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VoxelDarkSurfaceContainer,
                        unfocusedContainerColor = VoxelDarkSurfaceContainer,
                        focusedBorderColor = VoxelMintLight,
                        unfocusedBorderColor = VoxelDarkSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // Horizontal Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) VoxelJadePrimary else VoxelDarkSurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) VoxelMintLight else VoxelDarkSurfaceBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Storage & Telemetry Diagnostic Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VoxelDarkSurfaceContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F1B1A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = "Speicher",
                                        tint = VoxelMintPeak,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "LiDAR Voxel-Speicher",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${projects.size} Projekte aktiv • TSDF Ok",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F1B1A))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "38.4 MB",
                                    color = VoxelMintPeak,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress gauge bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.30f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(VoxelJadePrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Ordner",
                                    tint = VoxelMintLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "/com.voxelscan.android/files/voxels",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "128 MB Kontingent",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Project Cards Stack
            items(filteredProjects) { project ->
                ProjectCard(
                    project = project,
                    onInspect = { inspectingProject = project },
                    onLoadIntoLive = {
                        repository.loadProjectIntoActiveWorld(project)
                        onOpenInLiveScan(project)
                    },
                    onToggleFavorite = { repository.toggleFavorite(project.id) },
                    onDelete = { repository.deleteProject(project.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Full 3D Inspector Bottom Sheet
        inspectingProject?.let { project ->
            ProjectInspectorSheet(
                project = project,
                repository = repository,
                onDismiss = { inspectingProject = null },
                onLoadIntoLive = {
                    repository.loadProjectIntoActiveWorld(project)
                    inspectingProject = null
                    onOpenInLiveScan(project)
                }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: VoxelProject,
    onInspect: () -> Unit,
    onLoadIntoLive: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VoxelDarkSurfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
    ) {
        Column {
            // 3D Isometric Viewport Canvas Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF0F1B1A))
            ) {
                VoxelCanvas3D(
                    entries = project.sampleVoxels,
                    modifier = Modifier.fillMaxSize(),
                    renderMode = VoxelRenderMode.CUBES,
                    colorMode = VoxelColorMode.TRUE_RGB,
                    initialYaw = 40f,
                    initialPitch = 25f,
                    allowInteraction = false
                )

                // Top badges inside viewport
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${project.voxelCount} Voxels",
                            color = VoxelMintPeak,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (project.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorit",
                            tint = if (project.isFavorite) VoxelAmber else Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Bottom badge: dimensions
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = project.boundsDescription,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Card Body
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = project.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${project.category} • ${project.timestampFormatted}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F1B1A))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${(project.voxelSizeMeters * 100).toInt()}cm Grid",
                            color = VoxelMintLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLoadIntoLive,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoxelJadePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Öffnen",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("3D Öffnen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onInspect,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3531))
                    ) {
                        Text("Details", fontSize = 12.sp, color = VoxelMintPeak)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1B1A))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Löschen",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectInspectorSheet(
    project: VoxelProject,
    repository: VoxelRepository,
    onDismiss: () -> Unit,
    onLoadIntoLive: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var renderMode by remember { mutableStateOf(VoxelRenderMode.CUBES) }
    var colorMode by remember { mutableStateOf(VoxelColorMode.TRUE_RGB) }
    var sliceFraction by remember { mutableFloatStateOf(1.0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VoxelDarkSurfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = project.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${project.voxelCount} Voxels • ${project.boundsDescription}",
                        color = VoxelMintLight,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive 3D Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1B1A))
                    .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(16.dp))
            ) {
                VoxelCanvas3D(
                    entries = project.sampleVoxels,
                    modifier = Modifier.fillMaxSize(),
                    renderMode = renderMode,
                    colorMode = colorMode,
                    sliceFraction = sliceFraction,
                    allowInteraction = true
                )

                // Render Mode Chips inside Inspector
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (renderMode == VoxelRenderMode.CUBES) VoxelJadePrimary else Color.Black.copy(alpha = 0.5f))
                            .clickable { renderMode = VoxelRenderMode.CUBES }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Cubes", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (renderMode == VoxelRenderMode.POINTS) VoxelCyanGlow else Color.Black.copy(alpha = 0.5f))
                            .clickable { renderMode = VoxelRenderMode.POINTS }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Points", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (renderMode == VoxelRenderMode.WIREFRAME) VoxelCoral else Color.Black.copy(alpha = 0.5f))
                            .clickable { renderMode = VoxelRenderMode.WIREFRAME }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Wire", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer Cutaway Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Cutaway",
                    tint = VoxelAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cutaway: ${(sliceFraction * 100).toInt()}%",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(80.dp)
                )
                Slider(
                    value = sliceFraction,
                    onValueChange = { sliceFraction = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = VoxelAmber, activeTrackColor = VoxelMintLight),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLoadIntoLive,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoxelJadePrimary)
                ) {
                    Icon(imageVector = Icons.Default.ViewInAr, contentDescription = "Öffnen")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("In Live AR laden", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        repository.loadProjectIntoActiveWorld(project)
                        val file = repository.exportActiveScan("vox", 255)
                        Toast.makeText(context, "${project.title} als .VOX exportiert", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3531))
                ) {
                    Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = "VOX Export", tint = VoxelMintPeak)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MagicaVOX", color = VoxelMintPeak, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
