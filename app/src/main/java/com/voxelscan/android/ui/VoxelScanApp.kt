package com.voxelscan.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxelscan.android.data.VoxelRepository
import com.voxelscan.android.telemetry.SensorTelemetry
import com.voxelscan.android.ui.screens.GalleryScreen
import com.voxelscan.android.ui.screens.LiveScanScreen
import com.voxelscan.android.ui.screens.MeshSnapSheet
import com.voxelscan.android.ui.screens.ParameterExportSheet
import com.voxelscan.android.ui.screens.SettingsScreen
import com.voxelscan.android.ui.screens.SplashScreen
import com.voxelscan.android.ui.theme.VoxelDarkBackground
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceBorder
import com.voxelscan.android.ui.theme.VoxelDarkSurfaceContainer
import com.voxelscan.android.ui.theme.VoxelJadePrimary
import com.voxelscan.android.ui.theme.VoxelMintLight
import com.voxelscan.android.ui.theme.VoxelMintPeak

enum class AppDestination {
    SPLASH,
    LIVE_SCAN,
    GALLERY,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxelScanApp() {
    val context = LocalContext.current
    val repository = remember { VoxelRepository(context) }
    val telemetry = remember { SensorTelemetry(context) }

    DisposableEffect(Unit) {
        telemetry.start()
        onDispose {
            telemetry.stop()
        }
    }

    var currentScreen by remember { mutableStateOf(AppDestination.SPLASH) }
    var showMeshSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val meshSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle back button
    BackHandler(enabled = currentScreen != AppDestination.SPLASH) {
        if (currentScreen != AppDestination.LIVE_SCAN) {
            currentScreen = AppDestination.LIVE_SCAN
        } else {
            currentScreen = AppDestination.SPLASH
        }
    }

    Scaffold(
        containerColor = VoxelDarkBackground,
        bottomBar = {
            if (currentScreen != AppDestination.SPLASH) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    NavigationBar(
                        containerColor = VoxelDarkSurfaceContainer.copy(alpha = 0.95f),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, VoxelDarkSurfaceBorder, RoundedCornerShape(24.dp))
                    ) {
                        // 1. Live Scan
                        NavigationBarItem(
                            selected = currentScreen == AppDestination.LIVE_SCAN,
                            onClick = { currentScreen = AppDestination.LIVE_SCAN },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppDestination.LIVE_SCAN) Icons.Filled.ViewInAr else Icons.Outlined.ViewInAr,
                                    contentDescription = "Live Scan",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VoxelMintPeak,
                                selectedTextColor = VoxelMintPeak,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = Color(0xFF16322C)
                            ),
                            modifier = Modifier.testTag("nav_scan")
                        )

                        // 2. Mesh & Snap Sheet Trigger
                        NavigationBarItem(
                            selected = showMeshSheet,
                            onClick = { showMeshSheet = true },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Mesh & Snap",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text("Mesh", fontSize = 11.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VoxelMintPeak,
                                selectedTextColor = VoxelMintPeak,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = Color(0xFF16322C)
                            ),
                            modifier = Modifier.testTag("nav_mesh")
                        )

                        // 3. Export Sheet Trigger
                        NavigationBarItem(
                            selected = showExportSheet,
                            onClick = { showExportSheet = true },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text("Export", fontSize = 11.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VoxelMintPeak,
                                selectedTextColor = VoxelMintPeak,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = Color(0xFF16322C)
                            ),
                            modifier = Modifier.testTag("nav_export")
                        )

                        // 4. Galerie
                        NavigationBarItem(
                            selected = currentScreen == AppDestination.GALLERY,
                            onClick = { currentScreen = AppDestination.GALLERY },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppDestination.GALLERY) Icons.Filled.Collections else Icons.Outlined.Collections,
                                    contentDescription = "Galerie",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text("Galerie", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VoxelMintPeak,
                                selectedTextColor = VoxelMintPeak,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = Color(0xFF16322C)
                            ),
                            modifier = Modifier.testTag("nav_gallery")
                        )

                        // 5. Sensoren / Settings
                        NavigationBarItem(
                            selected = currentScreen == AppDestination.SETTINGS,
                            onClick = { currentScreen = AppDestination.SETTINGS },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppDestination.SETTINGS) Icons.Filled.Sensors else Icons.Outlined.Sensors,
                                    contentDescription = "Sensoren",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text("Sensoren", fontSize = 11.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VoxelMintPeak,
                                selectedTextColor = VoxelMintPeak,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = Color(0xFF16322C)
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    AppDestination.SPLASH -> {
                        SplashScreen(
                            onStartScan = { currentScreen = AppDestination.LIVE_SCAN },
                            onOpenGallery = { currentScreen = AppDestination.GALLERY },
                            onOpenSettings = { currentScreen = AppDestination.SETTINGS }
                        )
                    }
                    AppDestination.LIVE_SCAN -> {
                        LiveScanScreen(
                            repository = repository,
                            telemetry = telemetry,
                            onOpenMeshSheet = { showMeshSheet = true },
                            onOpenExportSheet = { showExportSheet = true }
                        )
                    }
                    AppDestination.GALLERY -> {
                        GalleryScreen(
                            repository = repository,
                            onOpenInLiveScan = {
                                currentScreen = AppDestination.LIVE_SCAN
                            }
                        )
                    }
                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            telemetry = telemetry
                        )
                    }
                }
            }
        }

        // Bottom Sheets
        if (showMeshSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMeshSheet = false },
                sheetState = meshSheetState,
                containerColor = VoxelDarkSurfaceContainer
            ) {
                MeshSnapSheet(
                    repository = repository,
                    onDismiss = { showMeshSheet = false }
                )
            }
        }

        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                sheetState = exportSheetState,
                containerColor = VoxelDarkSurfaceContainer
            ) {
                ParameterExportSheet(
                    repository = repository,
                    onDismiss = { showExportSheet = false }
                )
            }
        }
    }
}
