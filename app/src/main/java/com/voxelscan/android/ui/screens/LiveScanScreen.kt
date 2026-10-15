package com.voxelscan.android.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxelscan.android.data.VoxelRepository
import com.voxelscan.android.telemetry.SensorTelemetry
import com.voxelscan.android.ui.components.VoxelCanvas3D
import com.voxelscan.android.ui.components.VoxelColorMode
import com.voxelscan.android.ui.components.VoxelRenderMode
import com.voxelscan.android.ui.theme.VoxelAmber
import com.voxelscan.android.ui.theme.VoxelCoral
import com.voxelscan.android.ui.theme.VoxelCyanGlow
import com.voxelscan.android.ui.theme.VoxelDarkBackground
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceBorder
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceContainer
import com.voxelscan.android.ui.theme.VoxelJadePrimary
import com.voxelscan.android.ui.theme.VoxelLaserGreen
import com.voxelscan.android.ui.theme.VoxelMintLight
import com.voxelscan.android.ui.theme.VoxelMintPeak
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScanScreen(
    repository: VoxelRepository,
    telemetry: SensorTelemetry,
    onOpenMeshSheet: () -> Unit,
    onOpenExportSheet: () -> Unit
) {
    val context = LocalContext.current
    val isScanning by repository.isScanning.collectAsState()
    val telemetryState by telemetry.state.collectAsState()

    var renderMode by remember { mutableStateOf(VoxelRenderMode.CUBES) }
    var colorMode by remember { mutableStateOf(VoxelColorMode.TRUE_RGB) }
    var sliceFraction by remember { mutableFloatStateOf(1.0f) }
    var showSliceSlider by remember { mutableStateOf(false) }

    var selectedResolutionIndex by remember { mutableIntStateOf(2) } // 5cm default
    val resolutions = remember { listOf(0.01f, 0.02f, 0.05f, 0.10f) }
    val resolutionLabels = remember { listOf("1cm", "2cm", "5cm", "10cm") }

    var saveDialogOpen by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }

    var updateTick by remember { mutableIntStateOf(0) }

    // Real-time scan point generation loop when scanning is active
    LaunchedEffect(isScanning) {
        if (!isScanning) return@LaunchedEffect
        var timeSec = 0f
        var frame = 0L
        while (isScanning) {
            delay(40) // ~25-30 depth frames per second
            timeSec += 0.04f
            frame++

            val sample = telemetry.sampleArDepthHit(timeSec)
            repository.activeWorld.addPoint(
                worldX = sample.x,
                worldY = sample.y,
                worldZ = sample.z,
                r = sample.r,
                g = sample.g,
                b = sample.b,
                frameId = frame
            )

            // Periodic free-space ray carving
            if (frame % 3L == 0L) {
                repository.activeWorld.carveRay(
                    cameraX = 0f,
                    cameraY = 0f,
                    cameraZ = -0.5f,
                    hitX = sample.x,
                    hitY = sample.y,
                    hitZ = sample.z,
                    frameId = frame
                )
            }
            updateTick++
        }
    }

    // Capture single burst
    fun captureBurst() {
        val baseTime = System.currentTimeMillis() / 1000f
        for (i in 0..12) {
            val sample = telemetry.sampleArDepthHit(baseTime + (i * 0.15f))
            repository.activeWorld.addPoint(
                worldX = sample.x,
                worldY = sample.y,
                worldZ = sample.z,
                r = sample.r,
                g = sample.g,
                b = sample.b
            )
        }
        updateTick++
        Toast.makeText(context, "+13 LiDAR Tiefenpunkte erfasst", Toast.LENGTH_SHORT).show()
    }

    val entries = remember(updateTick, repository.activeWorld.count()) {
        repository.activeWorld.entries()
    }
    val bounds = remember(entries) {
        repository.activeWorld.calculateBounds(entries)
    }

    val pulseTransition = rememberInfiniteTransition(label = "recPulse")
    val recordGlowScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoxelDarkBackground)
    ) {
        // 3D Viewport
        VoxelCanvas3D(
            entries = entries,
            modifier = Modifier.fillMaxSize(),
            renderMode = renderMode,
            colorMode = colorMode,
            sliceFraction = sliceFraction,
            initialYaw = 38f,
            initialPitch = 24f,
            allowInteraction = true
        )

        // Holographic Crosshair HUD in Viewport Center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.2.dp, VoxelMintLight.copy(alpha = 0.45f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isScanning) VoxelLaserGreen else VoxelCoral)
            )
        }

        // Top HUD Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Row 1: Engine status, LiDAR active ping & 6DoF telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LiDAR / 6DoF badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VoxelDarkSurfaceContainer.copy(alpha = 0.9f))
                        .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) VoxelLaserGreen else VoxelAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "SCAN AKTIV" else "BEREIT",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 6DoF Gyro readout
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VoxelDarkSurfaceContainer.copy(alpha = 0.9f))
                        .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "P:%+.0f° R:%+.0f° Y:%.0f°",
                            telemetryState.pitchDegrees,
                            telemetryState.rollDegrees,
                            telemetryState.yawDegrees
                        ),
                        color = VoxelMintLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // FPS & Memory badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VoxelDarkSurfaceContainer.copy(alpha = 0.9f))
                        .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "60 FPS • ${String.format(Locale.US, "%.1f", repository.activeWorld.memoryUsageKB() / 1024f + 32.4f)} MB",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Tactical Quick Controls Strip (Resolution, View Mode, Color Mode, Slices)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VoxelDarkSurfaceContainer.copy(alpha = 0.88f))
                    .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolution Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GRID:",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    resolutionLabels.forEachIndexed { index, label ->
                        val isSelected = selectedResolutionIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) VoxelJadePrimary else Color.Transparent)
                                .clickable {
                                    if (repository.activeWorld.setVoxelSize(resolutions[index])) {
                                        selectedResolutionIndex = index
                                    } else {
                                        Toast.makeText(context, "Auflösung nur bei leerem Scan änderbar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // View Mode Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            renderMode = when (renderMode) {
                                VoxelRenderMode.CUBES -> VoxelRenderMode.POINTS
                                VoxelRenderMode.POINTS -> VoxelRenderMode.WIREFRAME
                                VoxelRenderMode.WIREFRAME -> VoxelRenderMode.CUBES
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Ansichtsmodus",
                            tint = when (renderMode) {
                                VoxelRenderMode.CUBES -> VoxelMintPeak
                                VoxelRenderMode.POINTS -> VoxelCyanGlow
                                VoxelRenderMode.WIREFRAME -> VoxelCoral
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Color Mode Toggle
                    IconButton(
                        onClick = {
                            colorMode = when (colorMode) {
                                VoxelColorMode.TRUE_RGB -> VoxelColorMode.HEATMAP
                                VoxelColorMode.HEATMAP -> VoxelColorMode.PALETTE
                                VoxelColorMode.PALETTE -> VoxelColorMode.CONFIDENCE
                                VoxelColorMode.CONFIDENCE -> VoxelColorMode.TRUE_RGB
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Farbmodus",
                            tint = when (colorMode) {
                                VoxelColorMode.TRUE_RGB -> VoxelMintLight
                                VoxelColorMode.HEATMAP -> VoxelCoral
                                VoxelColorMode.PALETTE -> VoxelAmber
                                VoxelColorMode.CONFIDENCE -> VoxelLaserGreen
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Layer Cutaway Slice Toggle
                    IconButton(
                        onClick = { showSliceSlider = !showSliceSlider },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Schichten",
                            tint = if (showSliceSlider || sliceFraction < 1f) VoxelAmber else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Optional Layer Slice Slider
            AnimatedVisibility(visible = showSliceSlider) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VoxelDarkSurfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCHICHT: ${(sliceFraction * 100).toInt()}%",
                            color = VoxelMintLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = sliceFraction,
                            onValueChange = { sliceFraction = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = VoxelAmber,
                                activeTrackColor = VoxelMintLight
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Bounding Box and Voxel Count Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entries.size} Voxels",
                    color = VoxelMintPeak,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "  |  ${String.format(Locale.US, "%.1f×%.1f×%.1f m", bounds.widthMeters(repository.activeWorld.voxelSize), bounds.heightMeters(repository.activeWorld.voxelSize), bounds.depthMeters(repository.activeWorld.voxelSize))}",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Tactical Scanning Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                VoxelDarkSurfaceContainer.copy(alpha = 0.95f),
                                Color(0xFF0D1617)
                            )
                        )
                    )
                    .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(28.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clear Scan Button
                IconButton(
                    onClick = {
                        val cleared = repository.clearActiveScan()
                        updateTick++
                        Toast.makeText(context, "$cleared Voxels zurückgesetzt", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .testTag("clear_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Zurücksetzen",
                        tint = Color.LightGray
                    )
                }

                // Shutter Point Capture Burst
                IconButton(
                    onClick = { captureBurst() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .testTag("shutter_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Punkt Erfassen",
                        tint = VoxelMintLight
                    )
                }

                // Central Record / Pause Shutter Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("record_toggle_button")
                        .clickable {
                            repository.toggleScanning()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(recordGlowScale)
                                .border(2.dp, VoxelLaserGreen.copy(alpha = 0.6f), CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isScanning) Color(0xFFE11D48) else VoxelJadePrimary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isScanning) "Pause" else "Aufnahme Starten",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Mesh & Snap Sheet Trigger
                IconButton(
                    onClick = onOpenMeshSheet,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .testTag("mesh_snap_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Mesh & Snap",
                        tint = VoxelAmber
                    )
                }

                // Save Scan to Gallery
                IconButton(
                    onClick = {
                        if (entries.isEmpty()) {
                            Toast.makeText(context, "Keine Voxels zum Speichern", Toast.LENGTH_SHORT).show()
                        } else {
                            saveTitleInput = "Scan #${repository.projects.value.size + 1}"
                            saveDialogOpen = true
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .testTag("save_scan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Speichern",
                        tint = VoxelMintPeak
                    )
                }
            }
        }
    }

    // Save Scan Dialog
    if (saveDialogOpen) {
        AlertDialog(
            onDismissRequest = { saveDialogOpen = false },
            title = {
                Text("Scan in Galerie speichern", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "${entries.size} Voxels erfasst (${resolutionLabels[selectedResolutionIndex]} Grid)",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("Projektname") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.saveCurrentScanAsProject(saveTitleInput)
                        saveDialogOpen = false
                        Toast.makeText(context, "Scan erfolgreich gespeichert!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoxelJadePrimary)
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { saveDialogOpen = false }) {
                    Text("Abbrechen", color = Color.Gray)
                }
            },
            containerColor = VoxelDarkSurfaceContainer
        )
    }
}
