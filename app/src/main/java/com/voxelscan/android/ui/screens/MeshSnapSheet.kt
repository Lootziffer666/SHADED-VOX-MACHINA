package com.voxelscan.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.voxelscan.android.ui.theme.VoxelAmber
import com.voxelscan.android.ui.theme.VoxelCoral
import com.voxelscan.android.ui.theme.VoxelCyanGlow
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceBorder
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceContainer
import com.voxelscan.android.ui.theme.VoxelJadePrimary
import com.voxelscan.android.ui.theme.VoxelLaserGreen
import com.voxelscan.android.ui.theme.VoxelMintLight
import com.voxelscan.android.ui.theme.VoxelMintPeak
import java.util.Locale

@Composable
fun MeshSnapSheet(
    repository: VoxelRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var yawDeg by remember { mutableFloatStateOf(repository.activeWorld.axisYawDegrees) }
    var confirmThreshold by remember { mutableFloatStateOf(3.0f) }
    var selectedSnapIndex by remember { mutableIntStateOf(2) } // 0.05m
    val snapResolutions = remember { listOf(0.01f, 0.02f, 0.05f, 0.10f) }
    val snapLabels = remember { listOf("0.01m (1cm)", "0.02m (2cm)", "0.05m (5cm)", "0.10m (10cm)") }

    val rawCount = repository.activeWorld.count()
    val confirmedCount = repository.activeWorld.confirmedCount(confirmThreshold)
    val estimatedTriangles = confirmedCount * 12 // 6 quad faces * 2 triangles

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VoxelDarkSurfaceContainer)
            .padding(20.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.Gray.copy(alpha = 0.5f))
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mesh & Snap",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "VoxelWorld Engine Geometrie-Optimierung",
                    color = VoxelMintLight,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "TSDF ACTIVE",
                    color = VoxelLaserGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: Axis Yaw & Ground Alignment
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B1A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Achsen-Ausrichtung (Yaw)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format(Locale.US, "%+.1f°", yawDeg),
                        color = VoxelAmber,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = yawDeg,
                    onValueChange = {
                        yawDeg = it
                        repository.activeWorld.setAxisYaw(it)
                    },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = VoxelAmber,
                        activeTrackColor = VoxelJadePrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            yawDeg = 0f
                            repository.activeWorld.setAxisYaw(0f)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auf 0°", fontSize = 12.sp, color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            yawDeg = 45f
                            repository.activeWorld.setAxisYaw(45f)
                            Toast.makeText(context, "Achse an Raumkante ausgerichtet (45°)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoxelJadePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Snap",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Snap", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section 2: Snap to Grid Resolution
        Text(
            text = "Raster-Auflösung (Grid Snap)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            snapLabels.forEachIndexed { index, label ->
                val isSelected = selectedSnapIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) VoxelJadePrimary else Color(0xFF0F1B1A))
                        .border(
                            1.dp,
                            if (isSelected) VoxelMintLight else VoxelDarkSurfaceBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedSnapIndex = index
                            repository.activeWorld.setVoxelSize(snapResolutions[index])
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.split(" ")[0],
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3: Noise Filter & Outlier Cleaning
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B1A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rauschfilter (Min. Samples)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${confirmThreshold.toInt()} Hits",
                        color = VoxelMintPeak,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = confirmThreshold,
                    onValueChange = { confirmThreshold = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = VoxelMintPeak,
                        activeTrackColor = VoxelMintLight
                    )
                )

                Button(
                    onClick = {
                        val removed = repository.activeWorld.removeOutliers(minNeighbors = 2)
                        Toast.makeText(context, "$removed isolierte Voxels entfernt", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remove_outliers_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3531))
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Outlier Filter",
                        modifier = Modifier.size(16.dp),
                        tint = VoxelCoral
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ausreißer filtern (Outlier Removal)", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Processing Metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rohdaten", color = Color.Gray, fontSize = 10.sp)
                Text("$rawCount", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bestätigt", color = Color.Gray, fontSize = 10.sp)
                Text("$confirmedCount", color = VoxelMintPeak, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Dreiecke (Mesh)", color = Color.Gray, fontSize = 10.sp)
                Text("$estimatedTriangles", color = VoxelCyanGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_mesh_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VoxelJadePrimary)
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Fertig")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Übernehmen & Schließen", fontWeight = FontWeight.Bold)
        }
    }
}
