package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

const val DEFAULT_LOADER_GIF_URL = "https://github.com/Jeremy-Bluefrog/Pixel-3D-photo-/releases/download/0.0.1/loading.gif"

@Composable
fun VideoOrMorphingLoader(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    customVideoUrl: String? = null
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val rawResId = remember {
        context.resources.getIdentifier("loading_gif", "raw", context.packageName)
    }

    val model = remember(customVideoUrl, rawResId) {
        when {
            rawResId != 0 -> "android.resource://${context.packageName}/$rawResId"
            !customVideoUrl.isNullOrBlank() -> customVideoUrl
            else -> DEFAULT_LOADER_GIF_URL
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "Loading Animation",
            modifier = Modifier.fillMaxSize()
        )
    }
}
