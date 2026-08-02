package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Fluid Morphing Blob Loader.
 * Accurately replicates the Google Material 3 Expressive AI Studio loading video animation.
 * Features organic shape-shifting (squircle -> clover -> oval -> cloud -> starburst -> pentagon -> sparkle)
 * with rotating multi-stop vibrant gradients.
 */
@Composable
fun ExpressiveMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "morphLoader")

    // Phase driving shape morphing (0f..1f)
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morphProgress"
    )

    // Gradient rotation angle (0f..360f)
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientRotation"
    )

    // Breathing scale pulsation
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    val gradientColors = listOf(
        Color(0xFF00F2FE), // Vivid Cyan
        Color(0xFF00E676), // Emerald Green
        Color(0xFFFFD600), // Solar Yellow
        Color(0xFFFF6D00), // Bright Orange
        Color(0xFFFF2A6D), // Magenta Coral
        Color(0xFF9D4EDD), // Deep Violet
        Color(0xFF2979FF)  // Electric Blue
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val baseRadius = (minOf(size.toPx(), size.toPx()) / 2f) * 0.72f * scalePulse

            // Calculate organic shape morph parameters based on morphProgress
            // Shapes in sequence:
            // 0.0 - 0.15: Squircle / Rounded box
            // 0.15 - 0.30: 4-petal clover
            // 0.30 - 0.45: Tilted Oval
            // 0.45 - 0.60: Organic Cloud / Gear (8 harmonics)
            // 0.60 - 0.75: Rounded Pentagon
            // 0.75 - 0.90: 4-Point Sparkle Star
            // 0.90 - 1.00: Soft Rounded Triangle
            
            val phase = morphProgress * 7f
            val cycle = phase.toInt() % 7
            val frac = phase - phase.toInt()

            val p1 = getShapeParams(cycle)
            val p2 = getShapeParams((cycle + 1) % 7)

            val lobes = lerp(p1.lobes, p2.lobes, frac)
            val amplitude = lerp(p1.amplitude, p2.amplitude, frac)
            val sharpness = lerp(p1.sharpness, p2.sharpness, frac)
            val secondaryLobes = lerp(p1.secLobes, p2.secLobes, frac)
            val secondaryAmp = lerp(p1.secAmp, p2.secAmp, frac)

            val path = Path()
            val steps = 120
            for (i in 0..steps) {
                val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
                
                // Polar harmonics formula for fluid organic shape morphing
                val harmonic1 = cos(lobes * angle)
                val harmonic2 = sin(secondaryLobes * angle)
                val shapeMod = 1f + amplitude * Math.pow(Math.abs(harmonic1).toDouble(), sharpness.toDouble()).toFloat() * Math.signum(harmonic1) + secondaryAmp * harmonic2

                val r = baseRadius * shapeMod
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            // 1. Render Volumetric Glow Pass
            rotate(degrees = gradientRotation, pivot = center) {
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradientColors[cycle % gradientColors.size].copy(alpha = 0.45f),
                            gradientColors[(cycle + 2) % gradientColors.size].copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.5f
                    )
                )
            }

            // 2. Render Main Vivid Fluid Morphing Shape Pass
            rotate(degrees = -gradientRotation * 0.7f, pivot = center) {
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            gradientColors[cycle % gradientColors.size],
                            gradientColors[(cycle + 1) % gradientColors.size],
                            gradientColors[(cycle + 3) % gradientColors.size],
                            gradientColors[(cycle + 5) % gradientColors.size]
                        ),
                        start = androidx.compose.ui.geometry.Offset(center.x - baseRadius, center.y - baseRadius),
                        end = androidx.compose.ui.geometry.Offset(center.x + baseRadius, center.y + baseRadius)
                    )
                )
            }
        }
    }
}

private data class ShapeParams(
    val lobes: Float,
    val amplitude: Float,
    val sharpness: Float,
    val secLobes: Float,
    val secAmp: Float
)

private fun getShapeParams(index: Int): ShapeParams {
    return when (index) {
        0 -> ShapeParams(lobes = 4f, amplitude = 0.16f, sharpness = 0.6f, secLobes = 2f, secAmp = 0.05f)   // Squircle blob
        1 -> ShapeParams(lobes = 4f, amplitude = 0.32f, sharpness = 1.0f, secLobes = 8f, secAmp = 0.04f)   // 4-petal flower
        2 -> ShapeParams(lobes = 2f, amplitude = 0.28f, sharpness = 0.8f, secLobes = 3f, secAmp = 0.02f)   // Tilted oval
        3 -> ShapeParams(lobes = 8f, amplitude = 0.22f, sharpness = 0.7f, secLobes = 4f, secAmp = 0.08f)   // Organic cloud
        4 -> ShapeParams(lobes = 5f, amplitude = 0.25f, sharpness = 0.9f, secLobes = 2f, secAmp = 0.03f)   // Soft pentagon
        5 -> ShapeParams(lobes = 4f, amplitude = 0.42f, sharpness = 1.8f, secLobes = 4f, secAmp = 0.06f)   // 4-point sparkle star
        6 -> ShapeParams(lobes = 3f, amplitude = 0.30f, sharpness = 1.2f, secLobes = 6f, secAmp = 0.04f)   // Soft triangle
        else -> ShapeParams(lobes = 4f, amplitude = 0.20f, sharpness = 0.8f, secLobes = 2f, secAmp = 0.05f)
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
