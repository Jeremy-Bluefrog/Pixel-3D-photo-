package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

const val DEFAULT_LOADER_VIDEO_URL = "https://github.com/Jeremy-Bluefrog/Pixel-3D-photo-/releases/download/0.0.1/67a66e82-4d7c-4e6b-8933-af6948a37268_6c_GPIThinking_C2.mp4"

@Composable
fun VideoOrMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    customVideoUrl: String? = DEFAULT_LOADER_VIDEO_URL
) {
    val context = LocalContext.current
    var hasError by remember(customVideoUrl) { mutableStateOf(false) }

    val rawResId = remember {
        context.resources.getIdentifier("loading_video", "raw", context.packageName)
    }

    val videoUri = remember(customVideoUrl, rawResId) {
        when {
            rawResId != 0 -> Uri.parse("android.resource://${context.packageName}/$rawResId")
            !customVideoUrl.isNullOrBlank() -> Uri.parse(customVideoUrl)
            else -> null
        }
    }

    if (videoUri != null && !hasError) {
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            hasError = true
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Fallback to Expressive Morphing 3D Loader
        ExpressiveMorphingLoader(modifier = modifier, size = size)
    }
}

