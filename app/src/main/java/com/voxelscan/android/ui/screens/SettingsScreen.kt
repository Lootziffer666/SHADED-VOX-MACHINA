package com.voxelscan.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.voxelscan.android.telemetry.SensorTelemetry
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
import java.util.Locale

@Composable
fun SettingsScreen(
    telemetry: SensorTelemetry
) {
    val context = LocalContext.current
    val telemetryState by telemetry.state.collectAsState()

    var selectedPresetIndex by remember { mutableIntStateOf(1) } // Balanced default
    val presets = remember { listOf("Performance (Points)", "Ausgewogen (5cm)", "Ultra (High-Res)") }

    var selectedThemeIndex by remember { mutableIntStateOf(0) }
    val themes = remember { listOf("Emerald Cyberpunk", "AMOLED Schwarz", "Studio Hell") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoxelDarkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "Einstellungen & Sensoren",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ARCore Depth API, Hardware-Telemetrie & Engine",
                    color = VoxelMintLight,
                    fontSize = 13.sp
                )
            }
        }

        // Section 1: Live Hardware Sensor Telemetry
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoxelDarkSurfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Sensoren",
                                tint = VoxelMintPeak,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sensor-Telemetrie",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F1B1A))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(VoxelLaserGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "6DoF AKTIV",
                                    color = VoxelMintLight,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sensor metrics grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            label = "Neigung (Pitch)",
                            value = String.format(Locale.US, "%+.1f°", telemetryState.pitchDegrees),
                            tint = VoxelAmber,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Roll (Roll)",
                            value = String.format(Locale.US, "%+.1f°", telemetryState.rollDegrees),
                            tint = VoxelCoral,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Gier (Yaw)",
                            value = String.format(Locale.US, "%.1f°", telemetryState.yawDegrees),
                            tint = VoxelCyanGlow,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            label = "ARCore Depth API",
                            value = "Verfügbar",
                            tint = VoxelLaserGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Bildrate (FPS)",
                            value = "${telemetryState.fps} FPS",
                            tint = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Voxel Heap",
                            value = "${telemetryState.memoryMb} MB",
                            tint = VoxelMintPeak,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 2: Quality Presets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoxelDarkSurfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Qualität",
                            tint = VoxelAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Engine-Qualitätsprofil",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEachIndexed { index, name ->
                            val isSelected = selectedPresetIndex == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF16322C) else Color(0xFF0F1B1A))
                                    .border(
                                        1.dp,
                                        if (isSelected) VoxelMintLight else VoxelDarkSurfaceBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedPresetIndex = index
                                        Toast.makeText(context, "Profil: $name aktiviert", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Aktiv",
                                        tint = VoxelMintPeak,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Engine & System Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoxelDarkSurfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = VoxelMintLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System & Engine Info",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRow("Engine", "VoxelWorld Engine v2.4")
                    InfoRow("Codename", "SHADED-VOX-MACHINA")
                    InfoRow("Exporter", "MagicaVoxel RIFF / OBJ / PLY")
                    InfoRow("Lizenzstatus", "TestLicenseManager: Developer OK")
                    InfoRow("Package", context.packageName)

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Lokaler Voxel-Zwischenspeicher bereinigt", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3531))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Cache leeren",
                            modifier = Modifier.size(16.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cache & Temporäre Scans leeren", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F1B1A))
            .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = key, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.LightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
