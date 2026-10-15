package com.voxelscan.android.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.voxelscan.android.data.VoxelRepository
import com.voxelscan.android.scanner.ArCoreCameraSurface
import com.voxelscan.android.scanner.ArCoreScannerEngine
import com.voxelscan.android.scanner.ScannerRuntimeState
import com.voxelscan.android.telemetry.SensorTelemetry
import com.voxelscan.android.ui.components.VoxelCanvas3D
import com.voxelscan.android.ui.components.VoxelColorMode
import com.voxelscan.android.ui.components.VoxelRenderMode
import com.voxelscan.android.ui.theme.VoxelAmber
import com.voxelscan.android.ui.theme.VoxelJadePrimary
import com.voxelscan.android.ui.theme.VoxelLaserGreen
import com.voxelscan.android.ui.theme.VoxelMintLight
import com.voxelscan.android.ui.theme.VoxelMintPeak
import java.util.Locale

/**
 * Phase-1 Live Scan: real ARCore camera + Raw Depth only.
 *
 * The previous sine/cosine/random point generator has deliberately been removed from the
 * production scan path. If ARCore or depth fails, the HUD exposes the failed boundary instead of
 * substituting synthetic geometry.
 */
@Composable
fun LiveScanScreen(
    repository: VoxelRepository,
    @Suppress("UNUSED_PARAMETER") telemetry: SensorTelemetry,
    onOpenMeshSheet: () -> Unit,
    onOpenExportSheet: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    if (activity == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ARCore host Activity unavailable", color = Color.White)
        }
        return
    }

    val engine = remember(activity) { ArCoreScannerEngine(activity) }
    val cameraSurface = remember(engine) { ArCoreCameraSurface(context, engine) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) engine.markCameraPermissionMissing()
    }

    val diagnostics by engine.diagnostics.collectAsState()
    val isScanning by repository.isScanning.collectAsState()
    val currentScanning by rememberUpdatedState(isScanning)
    var worldTick by remember { mutableIntStateOf(0) }

    SideEffect {
        engine.captureEnabled = isScanning
    }

    // Correct lifecycle ordering: resume Session before GL surface; pause GL surface before Session.
    DisposableEffect(lifecycleOwner, hasCameraPermission, engine, cameraSurface) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (hasCameraPermission) {
                        engine.resume()
                        cameraSurface.onResume()
                    } else {
                        engine.markCameraPermissionMissing()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    cameraSurface.onPause()
                    engine.pause()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (hasCameraPermission) {
                engine.resume()
                cameraSurface.onResume()
            } else {
                engine.markCameraPermissionMissing()
            }
        }

        onDispose {
            cameraSurface.onPause()
            engine.pause()
            lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(engine) {
        onDispose { engine.destroy() }
    }

    // Real ARCore world-space points enter the existing VoxelWorld here.
    LaunchedEffect(engine) {
        var frameId = 0L
        engine.pointBatches.collect { batch ->
            if (!currentScanning) return@collect
            frameId++
            batch.forEach { point ->
                repository.activeWorld.addPoint(
                    worldX = point.x,
                    worldY = point.y,
                    worldZ = point.z,
                    r = 46,
                    g = 236,
                    b = 188,
                    weight = (point.confidence / 255f).coerceAtLeast(0.13f),
                    frameId = frameId
                )
            }
            worldTick++
        }
    }

    val entries = remember(worldTick, repository.activeWorld.count()) {
        repository.activeWorld.entries()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { cameraSurface },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!hasCameraPermission) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Camera permission is required for real ARCore Depth.",
                    color = Color.White
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("CAMERA FREIGEBEN")
                }
            }
        }

        // Truthful diagnostic bridge. Every value comes from the live ARCore/depth path.
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xD90A1211))
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = diagnosticText(
                        diagnostics = diagnostics,
                        voxelCount = repository.activeWorld.count()
                    ),
                    color = VoxelMintLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }

        // Small independent voxel proof: camera stays visible while real accumulated geometry is
        // visible too. This intentionally does not pretend to be final AR registration yet.
        if (entries.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(12.dp)
                    .size(154.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC07110F))
                    .border(1.dp, VoxelMintLight.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            ) {
                VoxelCanvas3D(
                    entries = entries,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    renderMode = VoxelRenderMode.CUBES,
                    colorMode = VoxelColorMode.TRUE_RGB,
                    allowInteraction = true
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { repository.toggleScanning() },
                    enabled = hasCameraPermission &&
                        diagnostics.runtimeState != ScannerRuntimeState.ERROR,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) VoxelAmber else VoxelJadePrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (isScanning) "PAUSE" else "SCAN")
                }

                OutlinedButton(
                    onClick = {
                        repository.clearActiveScan()
                        worldTick++
                    }
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Scan leeren")
                }
                OutlinedButton(onClick = onOpenMeshSheet) {
                    Icon(Icons.Default.Tune, contentDescription = "Mesh")
                }
                OutlinedButton(onClick = onOpenExportSheet) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                }
            }

            val stateLine = when {
                isScanning -> "REAL DEPTH CAPTURE • ${(repository.activeWorld.voxelSize * 100).toInt()} cm GRID"
                diagnostics.lastFrameAcceptedPoints > 0 -> "REAL DEPTH READY • TAP SCAN"
                else -> "MOVE PHONE SLOWLY • WAITING FOR DEPTH"
            }
            Text(
                text = stateLine,
                color = if (isScanning) VoxelLaserGreen else VoxelMintPeak,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(Color(0xC9000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun diagnosticText(
    diagnostics: com.voxelscan.android.scanner.ScannerDiagnostics,
    voxelCount: Int
): String {
    val supported = when (diagnostics.depthSupported) {
        true -> "YES"
        false -> "NO"
        null -> "—"
    }
    val error = diagnostics.lastError ?: "—"
    return buildString {
        append("ARCore: ${diagnostics.runtimeState.name} | Tracking: ${diagnostics.trackingState}\n")
        append("Depth: $supported / ${diagnostics.depthMode} | Frames: ${diagnostics.depthFrames} | ")
        append(String.format(Locale.US, "%.1f FPS", diagnostics.depthFps))
        append(" | ${diagnostics.depthWidth}×${diagnostics.depthHeight}\n")
        append("Valid: ${diagnostics.validDepthSamples} | Conf: ${diagnostics.confidenceMin}/")
        append("${diagnostics.confidenceAverage}/${diagnostics.confidenceMax} | ")
        append(">=32: ${diagnostics.confidenceAtLeast32} | >=128: ${diagnostics.confidenceAtLeast128}\n")
        append("Accepted: ${diagnostics.acceptedPoints} (+${diagnostics.lastFrameAcceptedPoints}) | ")
        append("Voxels: $voxelCount | NotYet: ${diagnostics.notYetAvailableCount}\n")
        append("Error: $error")
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
