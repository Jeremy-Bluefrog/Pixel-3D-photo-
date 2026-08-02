package com.example.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

const val DEFAULT_LOADER_VIDEO_URL = "https://github.com/Jeremy-Bluefrog/Pixel-3D-photo-/releases/download/0.0.1/67a66e82-4d7c-4e6b-8933-af6948a37268_6c_GPIThinking_C2.mp4"

@Composable
fun VideoOrMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    customVideoUrl: String? = DEFAULT_LOADER_VIDEO_URL
) {
    val context = LocalContext.current
    var hasError by remember(customVideoUrl) { mutableStateOf(false) }

    val rawResId = remember {
        context.resources.getIdentifier("loading_video", "raw", context.packageName)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!hasError) {
            var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
            
            DisposableEffect(rawResId, customVideoUrl) {
                onDispose {
                    try {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                    } catch (_: Exception) {}
                    mediaPlayer = null
                }
            }

            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                try {
                                    val surface = Surface(surfaceTexture)
                                    val mp = MediaPlayer().apply {
                                        setSurface(surface)
                                        
                                        // Prioritize customVideoUrl if specified and valid, otherwise use raw resource
                                        if (!customVideoUrl.isNullOrBlank()) {
                                            try {
                                                setDataSource(ctx, Uri.parse(customVideoUrl))
                                            } catch (e: Exception) {
                                                Log.e("Loader", "Failed to set custom video URL, trying raw res: ${e.message}")
                                                if (rawResId != 0) {
                                                    val afd = ctx.resources.openRawResourceFd(rawResId)
                                                    if (afd != null) {
                                                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                                        afd.close()
                                                    } else {
                                                        hasError = true
                                                    }
                                                } else {
                                                    hasError = true
                                                }
                                            }
                                        } else if (rawResId != 0) {
                                            val afd = ctx.resources.openRawResourceFd(rawResId)
                                            if (afd != null) {
                                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                                afd.close()
                                            } else {
                                                hasError = true
                                            }
                                        } else {
                                            hasError = true
                                        }
                                        
                                        isLooping = true
                                        setOnPreparedListener {
                                            try {
                                                it.start()
                                            } catch (e: Exception) {
                                                Log.e("Loader", "Error starting player: ${e.message}")
                                                hasError = true
                                            }
                                        }
                                        setOnErrorListener { _, what, extra ->
                                            Log.e("Loader", "MediaPlayer error: what=$what extra=$extra")
                                            hasError = true
                                            true
                                        }
                                        prepareAsync()
                                    }
                                    mediaPlayer = mp
                                } catch (e: Exception) {
                                    Log.e("Loader", "Exception preparing MediaPlayer: ${e.message}")
                                    hasError = true
                                }
                            }
                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {}
                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                try {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                } catch (_: Exception) {}
                                mediaPlayer = null
                                return true
                            }
                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
