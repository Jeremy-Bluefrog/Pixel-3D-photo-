package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.GyroscopeSensorManager
import com.example.sensor.TiltData
import kotlin.math.roundToInt

/**
 * A Jetpack Compose component that leverages Motion Sensor (Gyroscope / Accelerometer) data
 * to apply a subtle, multi-layered parallax translation effect to photos.
 * Creates an immersive 3D hologram-like viewing experience with dynamic light reflection.
 */
@Composable
fun MotionSensorParallaxPhoto(
    photoBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    foregroundCutoutBitmap: Bitmap? = null,
    backgroundInpaintedBitmap: Bitmap? = null,
    depthIntensity: Float = 1.0f,
    showHologramSheen: Boolean = true,
    externalTiltData: TiltData? = null
) {
    val context = LocalContext.current

    // Auto-manage sensor lifecycle if external tilt data is not supplied
    val sensorManager = remember(context) { GyroscopeSensorManager(context) }
    
    if (externalTiltData == null) {
        DisposableEffect(sensorManager) {
            sensorManager.startListening()
            onDispose {
                sensorManager.stopListening()
            }
        }
    }

    val sensorTilt by sensorManager.tiltState.collectAsState()
    val activeTilt = externalTiltData ?: sensorTilt

    // Touch interaction drag offsets for manual tilt
    var touchRollOffset by remember { mutableFloatStateOf(0f) }
    var touchPitchOffset by remember { mutableFloatStateOf(0f) }

    val rawRoll = (activeTilt.roll + touchRollOffset).coerceIn(-2f, 2f)
    val rawPitch = (activeTilt.pitch + touchPitchOffset).coerceIn(-2f, 2f)

    // Smooth physics spring interpolation
    val animatedRoll by animateFloatAsState(
        targetValue = rawRoll,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "animatedRoll"
    )

    val animatedPitch by animateFloatAsState(
        targetValue = rawPitch,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "animatedPitch"
    )

    // Parallax translation factors in dp
    val bgTranslationX = (-animatedRoll * 18f * depthIntensity).dp
    val bgTranslationY = (-animatedPitch * 18f * depthIntensity).dp

    val midTranslationX = (animatedRoll * 8f * depthIntensity).dp
    val midTranslationY = (animatedPitch * 8f * depthIntensity).dp

    val fgTranslationX = (animatedRoll * 28f * depthIntensity).dp
    val fgTranslationY = (animatedPitch * 28f * depthIntensity).dp

    val aspectRatio = if (photoBitmap != null && photoBitmap.height > 0) {
        photoBitmap.width.toFloat() / photoBitmap.height.toFloat()
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
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
                        touchRollOffset = (touchRollOffset + dragAmount.x / 160f).coerceIn(-1.5f, 1.5f)
                        touchPitchOffset = (touchPitchOffset + dragAmount.y / 160f).coerceIn(-1.5f, 1.5f)
                    }
                )
            }
            .graphicsLayer {
                // Subtle 3D perspective rotation for hologram feel
                rotationY = animatedRoll * 10f * depthIntensity
                rotationX = -animatedPitch * 10f * depthIntensity
                cameraDistance = 16f * density
            }
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    ) {
        if (photoBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Overscale slightly to prevent edge revealing during translation
                        scaleX = 1.20f
                        scaleY = 1.20f
                    }
            ) {
                // Layer 1: Background Parallax Layer (Shifted negative Z)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                bgTranslationX.toPx().roundToInt(),
                                bgTranslationY.toPx().roundToInt()
                            )
                        }
                ) {
                    val bgSource = backgroundInpaintedBitmap ?: photoBitmap
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = bgSource.asImageBitmap(),
                            dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            colorFilter = ColorFilter.colorMatrix(
                                ColorMatrix().apply {
                                    setToScale(0.85f, 0.85f, 0.90f, 1f)
                                }
                            )
                        )
                    }
                }

                // Layer 2: Midground Main Photo Layer
                if (foregroundCutoutBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    midTranslationX.toPx().roundToInt(),
                                    midTranslationY.toPx().roundToInt()
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(
                                image = photoBitmap.asImageBitmap(),
                                dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt())
                            )
                        }
                    }
                }

                // Layer 3: Foreground Parallax Cutout Layer (Shifted positive Z)
                if (foregroundCutoutBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    fgTranslationX.toPx().roundToInt(),
                                    fgTranslationY.toPx().roundToInt()
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(
                                image = foregroundCutoutBitmap.asImageBitmap(),
                                dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt())
                            )
                        }
                    }
                }

                // Layer 4: Interactive Holographic Light Reflection / Sheen
                if (showHologramSheen) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 0.28f
                            }
                    ) {
                        val centerX = size.width * (0.5f + animatedRoll * 0.4f)
                        val centerY = size.height * (0.5f + animatedPitch * 0.4f)

                        // Holographic spectrum rainbow shimmer gradient
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE0F2FE).copy(alpha = 0.6f),
                                    Color(0xFFDDD6FE).copy(alpha = 0.4f),
                                    Color(0xFFFBCFE8).copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                center = Offset(centerX, centerY),
                                radius = size.width * 0.85f
                            ),
                            blendMode = BlendMode.Screen
                        )

                        // Linear light streak reflection moving with roll tilt
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                start = Offset(centerX - size.width * 0.4f, centerY - size.height * 0.4f),
                                end = Offset(centerX + size.width * 0.4f, centerY + size.height * 0.4f)
                            ),
                            blendMode = BlendMode.Plus
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Motion Sensor Parallax Ready\n(Select or capture a photo)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}
