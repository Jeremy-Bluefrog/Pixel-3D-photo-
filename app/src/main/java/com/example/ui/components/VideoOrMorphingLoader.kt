package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

@Composable
fun VideoOrMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    val context = LocalContext.current
    
    // Check if loading_video exists in res/raw
    val videoResId = remember {
        context.resources.getIdentifier("loading_video", "raw", context.packageName)
    }

    if (videoResId != 0) {
        // Video exists, use VideoView
        Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val uri = Uri.parse("android.resource://${ctx.packageName}/$videoResId")
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            // Optional: crop or scale video properly
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Fallback to our generated loader
        ExpressiveMorphingLoader(modifier = modifier, size = size)
    }
}
