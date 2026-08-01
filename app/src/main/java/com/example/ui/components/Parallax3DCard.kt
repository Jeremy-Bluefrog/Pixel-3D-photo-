package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Render3DMode
import com.example.data.model.SpatialPhoto
import com.example.sensor.TiltData
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PixelDarkSurface
import com.example.ui.theme.SpatialAmber
import kotlin.math.roundToInt

@Composable
fun Parallax3DCard(
    photo: SpatialPhoto,
    tiltData: TiltData,
    renderMode: Render3DMode = Render3DMode.PARALLAX_TILT,
    customDepthBitmap: Bitmap? = null,
    foregroundBitmap: Bitmap? = null,
    sourceBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val effectiveRoll = tiltData.roll
    val effectivePitch = tiltData.pitch

    // Wiggle stereogram animation timer
    val wiggleAnim = remember { Animatable(0f) }
    LaunchedEffect(renderMode) {
        if (renderMode == Render3DMode.WIGGLE_STEREOGRAM) {
            wiggleAnim.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(120, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    val currentRoll = if (renderMode == Render3DMode.WIGGLE_STEREOGRAM) {
        if (wiggleAnim.value > 0.5f) 0.8f else -0.8f
    } else effectiveRoll

    val depthMultiplier = photo.depthIntensity

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio( photo.width.toFloat() / photo.height.toFloat().coerceAtLeast(1f) )
            .clip(RoundedCornerShape(24.dp))
            .background(PixelDarkSurface)
            .border(1.5.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        when (renderMode) {
            Render3DMode.PARALLAX_TILT, Render3DMode.WIGGLE_STEREOGRAM -> {
                // Background Layer (Moves opposite & slower)
                val bgOffsetX = (-currentRoll * 12 * depthMultiplier).dp
                val bgOffsetY = (-effectivePitch * 12 * depthMultiplier).dp

                // Midground Layer (Moves slightly)
                val midOffsetX = (currentRoll * 8 * depthMultiplier).dp
                val midOffsetY = (effectivePitch * 8 * depthMultiplier).dp

                // Foreground Layer (Moves faster towards user)
                val fgOffsetX = (currentRoll * 28 * depthMultiplier).dp
                val fgOffsetY = (effectivePitch * 28 * depthMultiplier).dp

                // Simulated or actual source image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.15f
                            scaleY = 1.15f
                        }
                ) {
                    // Background layer frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(bgOffsetX.toPx().roundToInt(), bgOffsetY.toPx().roundToInt()) }
                    ) {
                        if (sourceBitmap != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = sourceBitmap.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    colorFilter = ColorFilter.colorMatrix(
                                        ColorMatrix().apply { setToScale(0.88f, 0.88f, 0.92f, 1f) }
                                    )
                                )
                            }
                        } else {
                            // High-tech fallback canvas backdrop
                            PlaceholderDepthArt(photo = photo, layer = "BG", tiltX = currentRoll, tiltY = effectivePitch)
                        }
                    }

                    // Midground layer frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(midOffsetX.toPx().roundToInt(), midOffsetY.toPx().roundToInt()) }
                    ) {
                        if (sourceBitmap != null && foregroundBitmap == null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = sourceBitmap.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt())
                                )
                            }
                        } else if (sourceBitmap == null) {
                            PlaceholderDepthArt(photo = photo, layer = "MID", tiltX = currentRoll, tiltY = effectivePitch)
                        }
                    }

                    // Foreground Cutout Layer
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
                            PlaceholderDepthArt(photo = photo, layer = "FG", tiltX = currentRoll, tiltY = effectivePitch)
                        }
                    }
                }

                // Specular Light Sheen Overlay on Tilt
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sheenX = (size.width / 2f) + (currentRoll * size.width * 0.4f)
                    val sheenY = (size.height / 2f) + (effectivePitch * size.height * 0.4f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(sheenX, sheenY),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width * 0.6f,
                        center = Offset(sheenX, sheenY)
                    )
                }
            }

            Render3DMode.ANAGLYPH_3D -> {
                // Red / Cyan 3D glasses offset
                val offsetPx = (currentRoll * 20 * depthMultiplier).dp

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
                            PlaceholderDepthArt(photo = photo, layer = "ANAGLYPH_RED", tiltX = currentRoll, tiltY = effectivePitch)
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
                            PlaceholderDepthArt(photo = photo, layer = "ANAGLYPH_CYAN", tiltX = currentRoll, tiltY = effectivePitch)
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
                        PlaceholderDepthArt(photo = photo, layer = "DEPTH_MAP", tiltX = currentRoll, tiltY = effectivePitch)
                    }
                }
            }

            Render3DMode.LAYER_CUTOUT -> {
                val fgOffsetX = (currentRoll * 35 * depthMultiplier).dp
                val fgOffsetY = (effectivePitch * 35 * depthMultiplier).dp

                Box(modifier = Modifier.fillMaxSize()) {
                    // Dark background grid
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color(0xFF0F172A))
                        val gridStep = 40.dp.toPx()
                        for (x in 0..(size.width / gridStep).toInt()) {
                            drawLine(
                                color = GlassBorder,
                                start = Offset(x * gridStep, 0f),
                                end = Offset(x * gridStep, size.height)
                            )
                        }
                        for (y in 0..(size.height / gridStep).toInt()) {
                            drawLine(
                                color = GlassBorder,
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
                            PlaceholderDepthArt(photo = photo, layer = "FG_CUTOUT", tiltX = currentRoll, tiltY = effectivePitch)
                        }
                    }
                }
            }
        }

        // Live Mode Indicator Tag
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = when (renderMode) {
                    Render3DMode.PARALLAX_TILT -> "裸視 3D 視差"
                    Render3DMode.WIGGLE_STEREOGRAM -> "3D 搖擺立體圖 (12Hz)"
                    Render3DMode.ANAGLYPH_3D -> "紅藍 3D 眼鏡模式"
                    Render3DMode.DEPTH_MAP_HEATMAP -> "AI 深度熱力圖"
                    Render3DMode.LAYER_CUTOUT -> "前景物體分離層"
                },
                color = NeonCyan,
                fontSize = 11.sp
            )
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
                        colors = listOf(NeonCyan.copy(alpha = 0.85f), CyberMagenta.copy(alpha = 0.7f)),
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
