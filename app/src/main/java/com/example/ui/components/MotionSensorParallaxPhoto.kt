package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.nativeCanvas
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
import androidx.compose.ui.graphics.asComposeRenderEffect
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
    depthBitmap: Bitmap? = null,
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

    val rawRoll = activeTilt.roll.coerceIn(-2f, 2f)
    val rawPitch = activeTilt.pitch.coerceIn(-2f, 2f)

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

    // Parallax translation factors in dp (Enhanced responsive 3D depth range)
    val bgTranslationX = (-animatedRoll * 16f * depthIntensity).dp
    val bgTranslationY = (-animatedPitch * 16f * depthIntensity).dp

    val midTranslationX = (animatedRoll * 6f * depthIntensity).dp
    val midTranslationY = (animatedPitch * 6f * depthIntensity).dp

    val fgTranslationX = (animatedRoll * 28f * depthIntensity).dp
    val fgTranslationY = (animatedPitch * 28f * depthIntensity).dp

    val shadowTranslationX = (animatedRoll * 34f * depthIntensity + 8f).dp
    val shadowTranslationY = (animatedPitch * 34f * depthIntensity + 10f).dp

    val aspectRatio = if (photoBitmap != null && photoBitmap.height > 0) {
        photoBitmap.width.toFloat() / photoBitmap.height.toFloat()
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    ) {
        if (photoBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Overscale slightly to prevent edge revealing during translation and 3D rotation
                        scaleX = 1.35f
                        scaleY = 1.35f
                        
                        // Apply 3D perspective rotation to the inner contents so it looks like a 3D model inside a static frame
                        rotationY = animatedRoll * 15f * depthIntensity
                        rotationX = -animatedPitch * 15f * depthIntensity
                        cameraDistance = 8f * density
                    }
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && depthBitmap != null) {
                    val shader = remember(photoBitmap, depthBitmap) {
                        val rs = RuntimeShader(DEPTH_DISPLACEMENT_SHADER)
                        rs.setInputShader("image", BitmapShader(photoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
                        rs.setInputShader("depthMap", BitmapShader(depthBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
                        rs.setFloatUniform("resolution", photoBitmap.width.toFloat(), photoBitmap.height.toFloat())
                        rs
                    }
                    
                    // Update uniforms for smooth animation
                    // 根據圖片解析度動態計算偏移量 (最大 10% 偏移)
                    val maxOffsetX = photoBitmap.width * 0.1f
                    val maxOffsetY = photoBitmap.height * 0.1f
                    shader.setFloatUniform("offset", animatedRoll * maxOffsetX * depthIntensity, animatedPitch * maxOffsetY * depthIntensity)
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawContext.canvas.nativeCanvas.apply {
                            save()
                            val scaleX = size.width / photoBitmap.width
                            val scaleY = size.height / photoBitmap.height
                            
                            // Apply local matrix to shader so it scales up the displaced pixels to the Canvas size
                            val matrix = Matrix().apply { setScale(scaleX, scaleY) }
                            shader.setLocalMatrix(matrix)
                            
                            val paint = Paint().apply {
                                this.shader = shader
                            }
                            drawRect(0f, 0f, size.width, size.height, paint)
                            restore()
                        }
                        
                        // Deep space vignette to hide edge bleed
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.70f
                            )
                        )
                    }
                } else {
                    // Fallback Layered Parallax for older APIs or missing depth map
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
                                        setToScale(0.80f, 0.80f, 0.85f, 1f)
                                    }
                                )
                            )
                            // Deep space vignette
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f)),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width * 0.70f
                                )
                            )
                        }
                    }

                    // Layer 2: Midground or Single-Photo Subject Elevation Layer
                    if (foregroundCutoutBitmap == null) {
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
                                    image = photoBitmap.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt())
                                )
                            }
                        }
                    }

                    // Layer 3: Foreground Drop Shadow Projection
                    if (foregroundCutoutBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset {
                                    IntOffset(
                                        shadowTranslationX.toPx().roundToInt(),
                                        shadowTranslationY.toPx().roundToInt()
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawImage(
                                    image = foregroundCutoutBitmap.asImageBitmap(),
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.35f), BlendMode.SrcIn)
                                )
                            }
                        }

                        // Layer 4: Foreground Cutout Subject (Pop-out 3D Layer)
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
