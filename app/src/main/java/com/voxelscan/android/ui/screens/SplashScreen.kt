package com.voxelscan.android.ui.screens

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
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxelscan.android.model.Voxel
import com.voxelscan.android.model.VoxelWorld
import com.voxelscan.android.ui.components.VoxelCanvas3D
import com.voxelscan.android.ui.theme.VoxelAmber
import com.voxelscan.android.ui.theme.VoxelCoral
import com.voxelscan.android.ui.theme.VoxelDarkBackground
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceBorder
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceContainer
import com.voxelscan.android.ui.theme.VoxelJadePrimary
import com.voxelscan.android.ui.theme.VoxelLaserGreen
import com.voxelscan.android.ui.theme.VoxelMintLight
import com.voxelscan.android.ui.theme.VoxelMintPeak

@Composable
fun SplashScreen(
    onStartScan: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sculptureScale"
    )

    // Sample voxels for the iconic central 3D sculpture
    val logoVoxels = remember {
        listOf(
            // Jade base voxels
            VoxelWorld.Entry(0, 0, 0, Voxel(36f, 156f, 129f, 10f)),
            VoxelWorld.Entry(1, 0, 0, Voxel(29f, 131f, 108f, 10f)),
            VoxelWorld.Entry(0, 0, 1, Voxel(29f, 131f, 108f, 10f)),
            VoxelWorld.Entry(-1, 0, 0, Voxel(36f, 156f, 129f, 10f)),
            VoxelWorld.Entry(0, 0, -1, Voxel(36f, 156f, 129f, 10f)),
            // Middle tier
            VoxelWorld.Entry(0, 1, 0, Voxel(46f, 204f, 113f, 15f)), // Emerald
            VoxelWorld.Entry(1, 1, 0, Voxel(245f, 170f, 56f, 12f)), // Amber
            VoxelWorld.Entry(-1, 1, 0, Voxel(46f, 204f, 113f, 12f)),
            VoxelWorld.Entry(0, 1, 1, Voxel(253f, 151f, 84f, 12f)), // Coral
            // Floating peak crown
            VoxelWorld.Entry(0, 2, 0, Voxel(152f, 244f, 215f, 20f)), // Mint peak
            // Orbiting satellites
            VoxelWorld.Entry(2, 2, 1, Voxel(253f, 151f, 84f, 8f)),
            VoxelWorld.Entry(-2, 1, -1, Voxel(56f, 189f, 248f, 8f))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF182D27),
                        VoxelDarkBackground
                    ),
                    radius = 1200f
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LiDAR Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VoxelDarkSurfaceContainer)
                        .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(VoxelLaserGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LiDAR AKTIV",
                        color = VoxelMintLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Auto 6DoF Mode Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VoxelDarkSurfaceContainer)
                        .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(20.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensor Status",
                        tint = VoxelCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUTO 6DoF",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Central 3D Sculpture & Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    VoxelCanvas3D(
                        entries = logoVoxels,
                        modifier = Modifier.fillMaxSize(),
                        initialYaw = 45f,
                        initialPitch = 30f,
                        allowInteraction = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "VoxelScan AR",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "SHADED-VOX-MACHINA",
                    color = VoxelMintPeak,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "VoxelWorld Engine v2.4 • Echtzeit LiDAR Rekonstruktion",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_scan_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoxelJadePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Scan Icon",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scan Starten",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenGallery,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("gallery_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VoxelMintLight
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Collections,
                            contentDescription = "Galerie",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galerie (5)", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("settings_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VoxelAmber
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Sensoren",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sensoren", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
