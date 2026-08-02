package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R

@Composable
fun VideoOrMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    customVideoUrl: String? = null
) {
    val context = LocalContext.current
    val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.loading_video}")
    var isVideoReady by remember { mutableStateOf(false) }

    // Instant smooth loading animation fallback
    val infiniteTransition = rememberInfiniteTransition(label = "loaderRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Video View (loaded in background with instant fallback)
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.start()
                        isVideoReady = true
                    }
                    setOnErrorListener { _, _, _ -> 
                        isVideoReady = false
                        true 
                    }
                    start()
                }
            },
            update = { view ->
                if (!view.isPlaying) {
                    view.start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Instant glowing morphing loader overlay shown until video is fully playing
        AnimatedVisibility(
            visible = !isVideoReady,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing ring
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotation)
                ) {
                    val strokeWidth = 8.dp.toPx()
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                primaryColor,
                                tertiaryColor,
                                Color.Transparent
                            )
                        ),
                        radius = size.toPx() / 2f - strokeWidth,
                        style = Stroke(width = strokeWidth)
                    )
                }

                // Inner pulsing core
                Box(
                    modifier = Modifier
                        .size(size * (0.55f * pulseScale))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    primaryColor,
                                    containerColor
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(size * 0.3f)
                    )
                }
            }
        }
    }
}

