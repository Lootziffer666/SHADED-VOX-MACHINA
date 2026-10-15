package com.voxelscan.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.voxelscan.android.model.VoxelWorld
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class VoxelRenderMode {
    CUBES,
    POINTS,
    WIREFRAME
}

enum class VoxelColorMode {
    TRUE_RGB,
    HEATMAP,
    PALETTE,
    CONFIDENCE
}

@Composable
fun VoxelCanvas3D(
    entries: List<VoxelWorld.Entry>,
    modifier: Modifier = Modifier,
    renderMode: VoxelRenderMode = VoxelRenderMode.CUBES,
    colorMode: VoxelColorMode = VoxelColorMode.TRUE_RGB,
    sliceFraction: Float = 1.0f,
    initialYaw: Float = 35f,
    initialPitch: Float = 28f,
    allowInteraction: Boolean = true
) {
    var yaw by remember { mutableFloatStateOf(initialYaw) }
    var pitch by remember { mutableFloatStateOf(initialPitch) }
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    val interactionModifier = if (allowInteraction) {
        modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                panOffsetX += pan.x
                panOffsetY += pan.y
                scale = (scale * zoom).coerceIn(0.4f, 4.0f)
                yaw = (yaw + pan.x * 0.4f) % 360f
                pitch = (pitch - pan.y * 0.3f).coerceIn(-85f, 85f)
            }
        }
    } else {
        modifier
    }

    Canvas(modifier = interactionModifier.fillMaxSize()) {
        if (entries.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val centerX = w * 0.5f + panOffsetX
        val centerY = h * 0.5f + panOffsetY

        // Calculate elevation bounds for slice filtering
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE
        for (e in entries) {
            if (e.y < minY) minY = e.y
            if (e.y > maxY) maxY = e.y
        }
        val elevationCutoff = if (maxY >= minY) {
            minY + ((maxY - minY) * sliceFraction).toInt()
        } else {
            maxY
        }

        val filteredEntries = entries.filter { it.y <= elevationCutoff }
        if (filteredEntries.isEmpty()) return@Canvas

        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()
        val radPitch = Math.toRadians(pitch.toDouble()).toFloat()

        val cosY = cos(radYaw)
        val sinY = sin(radYaw)
        val cosP = cos(radPitch)
        val sinP = sin(radPitch)

        // Determine base voxel display radius in pixels based on canvas size
        val baseVoxelRadius = (w / 38f) * scale

        // Project and sort voxels back-to-front (Painter's algorithm)
        class ProjectedVoxel(
            val screenX: Float,
            val screenY: Float,
            val depth: Float,
            val entry: VoxelWorld.Entry
        )

        val projectedList = ArrayList<ProjectedVoxel>(filteredEntries.size)

        for (e in filteredEntries) {
            val ex = e.x.toFloat()
            val ey = e.y.toFloat()
            val ez = e.z.toFloat()

            // Rotate around Y axis (Yaw)
            val rx = (ex * cosY) - (ez * sinY)
            val rz = (ex * sinY) + (ez * cosY)

            // Rotate around X axis (Pitch)
            val ry = (ey * cosP) - (rz * sinP)
            val depth = (ey * sinP) + (rz * cosP)

            // Isometric/Orthographic with slight perspective
            val persp = 1.0f + (depth * 0.015f)
            val sx = centerX + (rx * baseVoxelRadius * persp)
            val sy = centerY - (ry * baseVoxelRadius * persp)

            projectedList.add(ProjectedVoxel(sx, sy, depth, e))
        }

        // Sort by depth (farthest first)
        projectedList.sortBy { it.depth }

        // Render each projected voxel
        for (pv in projectedList) {
            val e = pv.entry
            val baseColor = resolveColor(e, colorMode, minY, maxY)

            when (renderMode) {
                VoxelRenderMode.CUBES -> {
                    drawIsometricCube(
                        centerX = pv.screenX,
                        centerY = pv.screenY,
                        size = baseVoxelRadius * 0.92f,
                        color = baseColor
                    )
                }
                VoxelRenderMode.POINTS -> {
                    val radius = (baseVoxelRadius * 0.45f).coerceAtLeast(3f)
                    drawCircle(
                        color = baseColor.copy(alpha = 0.85f),
                        radius = radius,
                        center = Offset(pv.screenX, pv.screenY)
                    )
                    // Glow ring
                    drawCircle(
                        color = baseColor.copy(alpha = 0.25f),
                        radius = radius * 1.8f,
                        center = Offset(pv.screenX, pv.screenY),
                        style = Stroke(width = 1.2f)
                    )
                }
                VoxelRenderMode.WIREFRAME -> {
                    drawWireframeCube(
                        centerX = pv.screenX,
                        centerY = pv.screenY,
                        size = baseVoxelRadius * 0.92f,
                        color = baseColor
                    )
                }
            }
        }
    }
}

private fun resolveColor(
    entry: VoxelWorld.Entry,
    colorMode: VoxelColorMode,
    minY: Int,
    maxY: Int
): Color {
    val v = entry.voxel
    return when (colorMode) {
        VoxelColorMode.TRUE_RGB -> {
            Color(
                red = (v.r / 255f).coerceIn(0f, 1f),
                green = (v.g / 255f).coerceIn(0f, 1f),
                blue = (v.b / 255f).coerceIn(0f, 1f)
            )
        }
        VoxelColorMode.HEATMAP -> {
            val span = max(1, maxY - minY)
            val t = ((entry.y - minY).toFloat() / span).coerceIn(0f, 1f)
            // Cyan -> Emerald -> Amber -> Coral
            when {
                t < 0.33f -> {
                    val sub = t / 0.33f
                    Color(0.2f + 0.1f * sub, 0.7f + 0.2f * sub, 0.9f - 0.3f * sub)
                }
                t < 0.66f -> {
                    val sub = (t - 0.33f) / 0.33f
                    Color(0.3f + 0.6f * sub, 0.9f - 0.2f * sub, 0.6f - 0.4f * sub)
                }
                else -> {
                    val sub = (t - 0.66f) / 0.34f
                    Color(0.9f + 0.1f * sub, 0.7f - 0.4f * sub, 0.2f + 0.1f * sub)
                }
            }
        }
        VoxelColorMode.PALETTE -> {
            // Quantized emerald / amber / cyber mint palette
            val idx = (entry.x * 7 + entry.y * 13 + entry.z * 19).mod(5)
            when (idx) {
                0 -> Color(0xFF1D836C) // Jade
                1 -> Color(0xFF2ECC71) // Emerald
                2 -> Color(0xFFFD9754) // Coral Amber
                3 -> Color(0xFF98F4D7) // Mint
                else -> Color(0xFF38BDF8) // Cyan
            }
        }
        VoxelColorMode.CONFIDENCE -> {
            val conf = v.confidence()
            when {
                conf > 0.7f -> Color(0xFF2ECC71) // Solid confirmation (green)
                conf > 0.4f -> Color(0xFFF5AA38) // Medium confidence (amber)
                else -> Color(0xFF38BDF8)       // Preliminary hit (cyan)
            }
        }
    }
}

private fun DrawScope.drawIsometricCube(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color
) {
    val dx = size * 0.866f // cos(30 deg)
    val dy = size * 0.5f   // sin(30 deg)
    val h = size * 0.9f

    // 1. Top Face (Lighter shade)
    val topColor = Color(
        red = (color.red * 1.25f).coerceIn(0f, 1f),
        green = (color.green * 1.25f).coerceIn(0f, 1f),
        blue = (color.blue * 1.25f).coerceIn(0f, 1f)
    )
    val topPath = Path().apply {
        moveTo(centerX, centerY - h - dy)
        lineTo(centerX + dx, centerY - h)
        lineTo(centerX, centerY - h + dy)
        lineTo(centerX - dx, centerY - h)
        close()
    }
    drawPath(topPath, topColor)

    // 2. Left Face (Darker shade)
    val leftColor = Color(
        red = (color.red * 0.65f).coerceIn(0f, 1f),
        green = (color.green * 0.65f).coerceIn(0f, 1f),
        blue = (color.blue * 0.65f).coerceIn(0f, 1f)
    )
    val leftPath = Path().apply {
        moveTo(centerX - dx, centerY - h)
        lineTo(centerX, centerY - h + dy)
        lineTo(centerX, centerY + dy)
        lineTo(centerX - dx, centerY)
        close()
    }
    drawPath(leftPath, leftColor)

    // 3. Right Face (Mid-tone shade)
    val rightColor = Color(
        red = (color.red * 0.85f).coerceIn(0f, 1f),
        green = (color.green * 0.85f).coerceIn(0f, 1f),
        blue = (color.blue * 0.85f).coerceIn(0f, 1f)
    )
    val rightPath = Path().apply {
        moveTo(centerX, centerY - h + dy)
        lineTo(centerX + dx, centerY - h)
        lineTo(centerX + dx, centerY)
        lineTo(centerX, centerY + dy)
        close()
    }
    drawPath(rightPath, rightColor)

    // Subtle edge highlight
    drawPath(
        path = topPath,
        color = Color.White.copy(alpha = 0.2f),
        style = Stroke(width = 0.8f)
    )
}

private fun DrawScope.drawWireframeCube(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color
) {
    val dx = size * 0.866f
    val dy = size * 0.5f
    val h = size * 0.9f

    val stroke = Stroke(width = 1.0f)
    val wireColor = color.copy(alpha = 0.75f)

    // Top
    val topPath = Path().apply {
        moveTo(centerX, centerY - h - dy)
        lineTo(centerX + dx, centerY - h)
        lineTo(centerX, centerY - h + dy)
        lineTo(centerX - dx, centerY - h)
        close()
    }
    drawPath(topPath, wireColor, style = stroke)

    // Vertical edges
    drawLine(wireColor, Offset(centerX - dx, centerY - h), Offset(centerX - dx, centerY), 1.0f)
    drawLine(wireColor, Offset(centerX, centerY - h + dy), Offset(centerX, centerY + dy), 1.0f)
    drawLine(wireColor, Offset(centerX + dx, centerY - h), Offset(centerX + dx, centerY), 1.0f)

    // Bottom edges
    drawLine(wireColor, Offset(centerX - dx, centerY), Offset(centerX, centerY + dy), 1.0f)
    drawLine(wireColor, Offset(centerX, centerY + dy), Offset(centerX + dx, centerY), 1.0f)
}
