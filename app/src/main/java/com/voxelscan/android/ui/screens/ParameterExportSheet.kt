package com.voxelscan.android.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.core.content.FileProvider
import com.voxelscan.android.data.VoxelRepository
import com.voxelscan.android.ui.theme.VoxelAmber
import com.voxelscan.android.ui.theme.VoxelCoral
import com.voxelscan.android.ui.theme.VoxelCyanGlow
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceBorder
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceContainer
import com.voxelscan.android.ui.theme.VoxelJadePrimary
import com.voxelscan.android.ui.theme.VoxelMintLight
import com.voxelscan.android.ui.theme.VoxelMintPeak
import java.io.File

@Composable
fun ParameterExportSheet(
    repository: VoxelRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedFormatIndex by remember { mutableIntStateOf(0) }
    val formats = remember { listOf("vox", "obj", "ply", "xyz") }
    val formatLabels = remember { listOf(".VOX (MagicaVoxel)", ".OBJ (Wavefront 3D)", ".PLY (Stanford Cloud)", ".XYZ (ASCII Point)") }

    var selectedPaletteIndex by remember { mutableIntStateOf(4) } // 255 colors default
    val paletteSizes = remember { listOf(0, 8, 16, 32, 64, 255) }
    val paletteLabels = remember { listOf("RAW", "8 Farben", "16 Farben", "32 Farben", "64 Farben", "255 VOX") }

    var isZUp by remember { mutableStateOf(false) }

    val voxelCount = repository.activeWorld.count()

    fun triggerShare(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VoxelScan 3D Export - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Voxel-Modell exportieren"))
        } catch (e: Exception) {
            Toast.makeText(context, "Fehler beim Teilen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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

        Text(
            text = "Parameter & Export",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "3D-Formate, Farbpaletten & Koordinatensysteme",
            color = VoxelMintLight,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 1: 3D Format Selector
        Text(
            text = "Zielformat",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            formatLabels.forEachIndexed { index, label ->
                val isSelected = selectedFormatIndex == index
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
                        .clickable { selectedFormatIndex = index }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) VoxelMintPeak else Color.Gray, CircleShape)
                            .padding(3.dp)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(VoxelMintPeak)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Color Palette Quantization
        Text(
            text = "Farbpaletten-Reduktion",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            paletteLabels.take(4).forEachIndexed { index, label ->
                val isSelected = selectedPaletteIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) VoxelJadePrimary else Color(0xFF0F1B1A))
                        .border(
                            1.dp,
                            if (isSelected) VoxelMintLight else VoxelDarkSurfaceBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedPaletteIndex = index }
                        .padding(vertical = 8.dp),
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

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            paletteLabels.drop(4).forEachIndexed { dropIndex, label ->
                val actualIndex = dropIndex + 4
                val isSelected = selectedPaletteIndex == actualIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) VoxelJadePrimary else Color(0xFF0F1B1A))
                        .border(
                            1.dp,
                            if (isSelected) VoxelMintLight else VoxelDarkSurfaceBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedPaletteIndex = actualIndex }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3: Coordinate Orientation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1B1A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, VoxelDarkSurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Koordinaten-Achse", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (isZUp) "Z-Up (MagicaVoxel / CAD)" else "Y-Up (Unity / Blender / WebGL)", color = VoxelAmber, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isZUp) VoxelJadePrimary else Color.Transparent)
                            .clickable { isZUp = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Y-Up", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isZUp) VoxelJadePrimary else Color.Transparent)
                            .clickable { isZUp = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Z-Up", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons: Save & Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (voxelCount == 0) {
                        Toast.makeText(context, "Keine Voxels im aktuellen Scan vorhanden", Toast.LENGTH_SHORT).show()
                    } else {
                        val format = formats[selectedFormatIndex]
                        val pal = paletteSizes[selectedPaletteIndex]
                        val file = repository.exportActiveScan(format, pal)
                        Toast.makeText(context, "${file.name} exportiert (${file.length() / 1024} KB)", Toast.LENGTH_LONG).show()
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("export_save_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoxelJadePrimary)
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportieren", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    if (voxelCount == 0) {
                        Toast.makeText(context, "Keine Voxels im aktuellen Scan vorhanden", Toast.LENGTH_SHORT).show()
                    } else {
                        val format = formats[selectedFormatIndex]
                        val pal = paletteSizes[selectedPaletteIndex]
                        val file = repository.exportActiveScan(format, pal)
                        triggerShare(file)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("export_share_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxelMintLight)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Teilen")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Teilen", fontWeight = FontWeight.Bold)
            }
        }
    }
}
