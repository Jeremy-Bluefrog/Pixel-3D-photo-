package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Render3DMode
import com.example.data.model.SpatialPhoto
import com.example.sensor.TiltData
import androidx.compose.material3.MaterialTheme
import kotlin.math.roundToInt

data class PointCloudParticle(
    val x: Float,
    val y: Float,
    val depth: Float,
    val color: Color
)

@Composable
fun Parallax3DCard(
    photo: SpatialPhoto,
    tiltData: TiltData,
    renderMode: Render3DMode = Render3DMode.PARALLAX_TILT,
    customDepthBitmap: Bitmap? = null,
    foregroundBitmap: Bitmap? = null,
    backgroundBitmap: Bitmap? = null,
    sourceBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val effectiveRoll = tiltData.roll
    val effectivePitch = tiltData.pitch

    // Touch interactive drag state
    var touchRollOffset by remember { mutableFloatStateOf(0f) }
    var touchPitchOffset by remember { mutableFloatStateOf(0f) }

    // Wiggle stereogram animation timer
    val wiggleAnim = remember { Animatable(0f) }
    LaunchedEffect(renderMode) {
        if (renderMode == Render3DMode.WIGGLE_STEREOGRAM) {
            wiggleAnim.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(110, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    val rawRoll = if (renderMode == Render3DMode.WIGGLE_STEREOGRAM) {
        if (wiggleAnim.value > 0.5f) 0.9f else -0.9f
    } else {
        effectiveRoll + touchRollOffset
    }

    val rawPitch = if (renderMode == Render3DMode.WIGGLE_STEREOGRAM) {
        0f
    } else {
        effectivePitch + touchPitchOffset
    }

    // Smooth physics spring interpolation for realistic fluid spatial movement
    val animatedRoll by animateFloatAsState(
        targetValue = rawRoll.coerceIn(-2f, 2f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "animatedRoll"
    )
    val animatedPitch by animateFloatAsState(
        targetValue = rawPitch.coerceIn(-2f, 2f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "animatedPitch"
    )

    val depthMultiplier = photo.depthIntensity

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(photo.width.toFloat() / photo.height.toFloat().coerceAtLeast(1f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        touchRollOffset = 0f
                        touchPitchOffset = 0f
                    },
                    onDragCancel = {
                        touchRollOffset = 0f
                        touchPitchOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        touchRollOffset = (touchRollOffset + dragAmount.x / 180f).coerceIn(-1.5f, 1.5f)
                        touchPitchOffset = (touchPitchOffset + dragAmount.y / 180f).coerceIn(-1.5f, 1.5f)
                    }
                )
            }
            .graphicsLayer {
                // True 3D perspective spatial tilt on card container
                rotationY = animatedRoll * 12f * depthMultiplier
                rotationX = -animatedPitch * 12f * depthMultiplier
                cameraDistance = 16f * density
            }
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
    ) {
        when (renderMode) {
            Render3DMode.PARALLAX_TILT, Render3DMode.WIGGLE_STEREOGRAM -> {
                // Background Layer (Moves opposite & deeper into Z-space)
                val bgOffsetX = (-animatedRoll * 16 * depthMultiplier).dp
                val bgOffsetY = (-animatedPitch * 16 * depthMultiplier).dp

                // Midground Layer (Slight movement)
                val midOffsetX = (animatedRoll * 6 * depthMultiplier).dp
                val midOffsetY = (animatedPitch * 6 * depthMultiplier).dp

                // Foreground Layer (Projects forward towards viewer)
                val fgOffsetX = (animatedRoll * 34 * depthMultiplier).dp
                val fgOffsetY = (animatedPitch * 34 * depthMultiplier).dp

                // Soft dynamic drop shadow offset for floating foreground cutout
                val shadowOffsetX = (animatedRoll * 18 * depthMultiplier + 6).dp
                val shadowOffsetY = (animatedPitch * 18 * depthMultiplier + 8).dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.22f
                            scaleY = 1.22f
                        }
                ) {
                    // 1. Background layer frame (Deeper back Z-plane with subtle darkness)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(bgOffsetX.toPx().roundToInt(), bgOffsetY.toPx().roundToInt()) }
                    ) {
                        val bgSource = backgroundBitmap ?: sourceBitmap
                        if (bgSource != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = bgSource.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    colorFilter = ColorFilter.colorMatrix(
                                        ColorMatrix().apply {
                                            setToScale(0.82f, 0.82f, 0.88f, 1f)
                                        }
                                    )
                                )
                                // Depth vignette
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.width * 0.75f
                                    )
                                )
                            }
                        } else {
                            PlaceholderDepthArt(photo = photo, layer = "BG", tiltX = animatedRoll, tiltY = animatedPitch)
                        }
                    }

                    // 2. Midground layer frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(midOffsetX.toPx().roundToInt(), midOffsetY.toPx().roundToInt()) }
                    ) {
                        val midSource = backgroundBitmap ?: sourceBitmap
                        if (midSource != null && foregroundBitmap == null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = midSource.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt())
                                )
                            }
                        } else if (sourceBitmap == null) {
                            PlaceholderDepthArt(photo = photo, layer = "MID", tiltX = animatedRoll, tiltY = animatedPitch)
                        }
                    }

                    // 3. Floating Foreground Shadow Layer
                    if (foregroundBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(shadowOffsetX.toPx().roundToInt(), shadowOffsetY.toPx().roundToInt()) }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = foregroundBitmap.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.4f), BlendMode.SrcIn)
                                )
                            }
                        }
                    }

                    // 4. Floating Foreground Cutout Subject (Pop-out 3D Effect)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(fgOffsetX.toPx().roundToInt(), fgOffsetY.toPx().roundToInt()) }
                    ) {
                        if (foregroundBitmap != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = foregroundBitmap.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt())
                                )
                            }
                        } else if (sourceBitmap == null) {
                            PlaceholderDepthArt(photo = photo, layer = "FG", tiltX = animatedRoll, tiltY = animatedPitch)
                        }
                    }
                }

                // Dynamic Realistic Light & Specular Sheen Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sheenX = (size.width / 2f) + (animatedRoll * size.width * 0.45f)
                    val sheenY = (size.height / 2f) + (animatedPitch * size.height * 0.45f)

                    // Specular light spot
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(sheenX, sheenY),
                            radius = size.width * 0.65f
                        ),
                        radius = size.width * 0.65f,
                        center = Offset(sheenX, sheenY)
                    )

                    // Linear holographic lens sweep reflection
                    val sweepAngle = (animatedRoll + animatedPitch) * 20f
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            start = Offset(sheenX - size.width * 0.5f, sheenY - size.height * 0.5f),
                            end = Offset(sheenX + size.width * 0.5f, sheenY + size.height * 0.5f)
                        ),
                        start = Offset(0f, sheenY - size.height * 0.3f),
                        end = Offset(size.width, sheenY + size.height * 0.3f),
                        strokeWidth = 18f
                    )
                }
            }

            Render3DMode.ANAGLYPH_3D -> {
                val offsetPx = (animatedRoll * 24 * depthMultiplier).dp

                Box(modifier = Modifier.fillMaxSize()) {
                    // Red channel left offset
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((-offsetPx.toPx()).roundToInt(), 0) }
                    ) {
                        if (sourceBitmap != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = sourceBitmap.asImageBitmap(),
                                    colorFilter = ColorFilter.tint(Color.Red, BlendMode.Screen)
                                )
                            }
                        } else {
                            PlaceholderDepthArt(photo = photo, layer = "ANAGLYPH_RED", tiltX = animatedRoll, tiltY = animatedPitch)
                        }
                    }

                    // Cyan channel right offset
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((offsetPx.toPx()).roundToInt(), 0) }
                    ) {
                        if (sourceBitmap != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = sourceBitmap.asImageBitmap(),
                                    colorFilter = ColorFilter.tint(Color.Cyan, BlendMode.Screen)
                                )
                            }
                        } else {
                            PlaceholderDepthArt(photo = photo, layer = "ANAGLYPH_CYAN", tiltX = animatedRoll, tiltY = animatedPitch)
                        }
                    }
                }
            }

            Render3DMode.DEPTH_MAP_HEATMAP -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (customDepthBitmap != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(image = customDepthBitmap.asImageBitmap())
                        }
                    } else {
                        PlaceholderDepthArt(photo = photo, layer = "DEPTH_MAP", tiltX = animatedRoll, tiltY = animatedPitch)
                    }
                }
            }

            Render3DMode.LAYER_CUTOUT -> {
                val fgOffsetX = (animatedRoll * 42 * depthMultiplier).dp
                val fgOffsetY = (animatedPitch * 42 * depthMultiplier).dp

                Box(modifier = Modifier.fillMaxSize()) {
                    // Dark background grid
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF0F172A))
                        val gridStep = 40.dp.toPx()
                        for (x in 0..(size.width / gridStep).toInt()) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(x * gridStep, 0f),
                                end = Offset(x * gridStep, size.height)
                            )
                        }
                        for (y in 0..(size.height / gridStep).toInt()) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(0f, y * gridStep),
                                end = Offset(size.width, y * gridStep)
                            )
                        }
                    }

                    // Floating Cutout Subject
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(fgOffsetX.toPx().roundToInt(), fgOffsetY.toPx().roundToInt()) }
                    ) {
                        if (foregroundBitmap != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(image = foregroundBitmap.asImageBitmap())
                            }
                        } else {
                            PlaceholderDepthArt(photo = photo, layer = "FG_CUTOUT", tiltX = animatedRoll, tiltY = animatedPitch)
                        }
                    }
                }
            }

            Render3DMode.POINT_CLOUD_SPLAT -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (sourceBitmap != null && customDepthBitmap != null) {
                        val pointCloud = remember(sourceBitmap, customDepthBitmap) {
                            val points = mutableListOf<PointCloudParticle>()
                            val stepX = (sourceBitmap.width / 60).coerceAtLeast(1)
                            val stepY = (sourceBitmap.height / 60).coerceAtLeast(1)
                            val width = sourceBitmap.width
                            val height = sourceBitmap.height
                            
                            val srcPixels = IntArray(width * height)
                            val depthPixels = IntArray(width * height)
                            sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
                            
                            val scaledDepth = if (customDepthBitmap.width != width || customDepthBitmap.height != height) {
                                Bitmap.createScaledBitmap(customDepthBitmap, width, height, true)
                            } else customDepthBitmap
                            scaledDepth.getPixels(depthPixels, 0, width, 0, 0, width, height)
                            
                            for (y in 0 until height step stepY) {
                                for (x in 0 until width step stepX) {
                                    val index = y * width + x
                                    val color = srcPixels[index]
                                    val depth = android.graphics.Color.red(depthPixels[index]) / 255f
                                    points.add(
                                        PointCloudParticle(
                                            x = x.toFloat() / width,
                                            y = y.toFloat() / height,
                                            depth = depth,
                                            color = Color(color).copy(alpha = 0.95f)
                                        )
                                    )
                                }
                            }
                            // Sort by depth so closer points are drawn last (Painters algorithm)
                            points.sortedBy { it.depth }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(Color(0xFF05080F)) // deep space background
                            val w = size.width
                            val h = size.height
                            
                            val tiltX = animatedRoll * 120f * depthMultiplier
                            val tiltY = animatedPitch * 120f * depthMultiplier

                            for (particle in pointCloud) {
                                val zShift = (particle.depth - 0.5f)
                                val projX = (particle.x * w) + (tiltX * zShift)
                                val projY = (particle.y * h) + (tiltY * zShift)
                                
                                val splatSize = (size.width / 45f) * (0.5f + particle.depth * 0.5f)
                                
                                drawCircle(
                                    color = particle.color,
                                    radius = splatSize,
                                    center = Offset(projX, projY)
                                )
                            }
                        }
                    } else {
                        PlaceholderDepthArt(photo = photo, layer = "DEPTH_MAP", tiltX = animatedRoll, tiltY = animatedPitch)
                    }
                }
            }
        }


    }
}

@Composable
fun PlaceholderDepthArt(
    photo: SpatialPhoto,
    layer: String,
    tiltX: Float,
    tiltY: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (layer) {
            "BG" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D1117), Color(0xFF161B22))
                    )
                )
                // Far stars / bokeh
                drawCircle(Color.White.copy(alpha = 0.3f), radius = 30f, center = Offset(w * 0.2f, h * 0.25f))
                drawCircle(Color.White.copy(alpha = 0.2f), radius = 50f, center = Offset(w * 0.8f, h * 0.3f))
                drawCircle(Color.White.copy(alpha = 0.15f), radius = 80f, center = Offset(w * 0.5f, h * 0.15f))
            }
            "MID" -> {
                // Midground perspective horizon lines
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.65f)
                    cubicTo(w * 0.3f, h * 0.55f, w * 0.7f, h * 0.75f, w, h * 0.6f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
            }
            "FG", "FG_CUTOUT" -> {
                // Main subject silhouette with neon depth edge
                val subjectX = w * 0.5f
                val subjectY = h * 0.52f
                val radius = w * 0.28f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.85f), Color(0xFF10B981).copy(alpha = 0.7f)),
                        center = Offset(subjectX, subjectY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(subjectX, subjectY)
                )

                // Outer depth contour ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = radius + 8f,
                    center = Offset(subjectX, subjectY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )
            }
            "ANAGLYPH_RED" -> {
                drawRect(Color(0xFF1E293B))
                drawCircle(
                    color = Color.Red.copy(alpha = 0.8f),
                    radius = w * 0.25f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
            "ANAGLYPH_CYAN" -> {
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.8f),
                    radius = w * 0.25f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
            "DEPTH_MAP" -> {
                // Grayscale/Heatmap visualization preview
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.DarkGray, Color.Gray, Color.White)
                    )
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.3f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
        }
    }
}
