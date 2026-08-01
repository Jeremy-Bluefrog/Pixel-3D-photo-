package com.example.ml

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.DepthHeatmapPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object DepthMapGenerator {

    /**
     * Generates a 2D Depth Map from an input image bitmap.
     * Uses multi-scale perspective gradient heuristics, edge contrast, center saliency,
     * luminance depth cues, and 3x3 spatial smoothing filter.
     */
    suspend fun generateDepthMap(
        sourceBitmap: Bitmap,
        focalPlane: Float = 0.5f,
        contrast: Float = 1.3f,
        palette: DepthHeatmapPalette = DepthHeatmapPalette.GRAYSCALE
    ): Bitmap = withContext(Dispatchers.Default) {
        // Downsample for performance if image is very large
        val scaleWidth = min(sourceBitmap.width, 512)
        val scaleHeight = (scaleWidth * (sourceBitmap.height.toFloat() / sourceBitmap.width)).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, scaleWidth, scaleHeight, true)

        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        val rawDepths = FloatArray(width * height)
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = sqrt(centerX * centerX + centerY * centerY)

        // Pass 1: Multi-cue depth value computation
        for (y in 0 until height) {
            val verticalPerspective = y.toFloat() / height // Ground-plane perspective (bottom = closer)

            for (x in 0 until width) {
                val index = y * width + x
                val pixel = pixels[index]

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Luminance cue (lighter areas tend to pop forward in human perception)
                val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

                // Subject saliency weight (center focus)
                val dx = x - centerX
                val dy = y - centerY
                val distFromCenter = sqrt(dx * dx + dy * dy) / maxRadius
                val centerWeight = (1.0f - distFromCenter).coerceIn(0f, 1f).pow(1.6f)

                // Edge gradient strength (high detail subjects in focus)
                var edgeVal = 0f
                if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                    val leftPixel = pixels[index - 1]
                    val rightPixel = pixels[index + 1]
                    val topPixel = pixels[index - width]
                    val bottomPixel = pixels[index + width]

                    val diffX = abs(Color.red(rightPixel) - Color.red(leftPixel)) +
                            abs(Color.green(rightPixel) - Color.green(leftPixel)) +
                            abs(Color.blue(rightPixel) - Color.blue(leftPixel))

                    val diffY = abs(Color.red(bottomPixel) - Color.red(topPixel)) +
                            abs(Color.green(bottomPixel) - Color.green(topPixel)) +
                            abs(Color.blue(bottomPixel) - Color.blue(topPixel))

                    edgeVal = ((diffX + diffY) / (3f * 255f * 2f)).coerceIn(0f, 1f)
                }

                // Composite raw depth formula
                var depth = (verticalPerspective * 0.40f) + (centerWeight * 0.42f) + (luminance * 0.10f) + (edgeVal * 0.08f)
                depth = ((depth - focalPlane) * contrast + focalPlane).coerceIn(0f, 1f)
                rawDepths[index] = depth
            }
        }

        // Pass 2: 3x3 Spatial Box/Blur Filter for smooth continuous depth transitions
        val smoothedDepths = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                    var sum = 0f
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            sum += rawDepths[(y + dy) * width + (x + dx)]
                        }
                    }
                    smoothedDepths[index] = sum / 9f
                } else {
                    smoothedDepths[index] = rawDepths[index]
                }
            }
        }

        // Pass 3: Color mapping to depth bitmap
        val depthPixels = IntArray(width * height)
        for (i in 0 until width * height) {
            depthPixels[i] = mapDepthToColor(smoothedDepths[i], palette)
        }

        val depthBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        depthBitmap.setPixels(depthPixels, 0, width, 0, 0, width, height)
        depthBitmap
    }

    /**
     * Applies real-time 3D Gyroscope Depth-Displacement Effect on a photo using the AI-generated depth map.
     * Reads the grayscale depth bitmap (depthBitmap) calculated by DepthMapGenerator / DepthAnythingEstimator,
     * and shifts each pixel proportionally based on gyroscope roll & pitch angles.
     */
    suspend fun applyGyroDepthParallax(
        sourceBitmap: Bitmap,
        depthBitmap: Bitmap,
        roll: Float,
        pitch: Float,
        depthIntensity: Float = 1.0f,
        maxDisplacementPx: Float = 24f
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val scaledDepth = if (depthBitmap.width != width || depthBitmap.height != height) {
            Bitmap.createScaledBitmap(depthBitmap, width, height, true)
        } else depthBitmap

        val srcPixels = IntArray(width * height)
        val depthPixels = IntArray(width * height)
        val outputPixels = IntArray(width * height)

        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        scaledDepth.getPixels(depthPixels, 0, width, 0, 0, width, height)

        val shiftX = roll * maxDisplacementPx * depthIntensity
        val shiftY = pitch * maxDisplacementPx * depthIntensity

        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                val index = yOffset + x
                // Normalize depth value to centered range [-0.5, 0.5]
                val rawDepth = Color.red(depthPixels[index]) / 255f
                val depthVal = rawDepth - 0.5f

                val sampleX = (x - (shiftX * depthVal)).toInt().coerceIn(0, width - 1)
                val sampleY = (y - (shiftY * depthVal)).toInt().coerceIn(0, height - 1)

                outputPixels[index] = srcPixels[sampleY * width + sampleX]
            }
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        resultBitmap
    }

    /**
     * Extracts a subject foreground layer with smooth anti-aliased transparency based on depth threshold.
     */
    suspend fun extractForegroundLayer(
        sourceBitmap: Bitmap,
        depthBitmap: Bitmap,
        threshold: Float = 0.45f
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val scaledDepth = Bitmap.createScaledBitmap(depthBitmap, width, height, true)

        val srcPixels = IntArray(width * height)
        val depthPixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)

        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        scaledDepth.getPixels(depthPixels, 0, width, 0, 0, width, height)

        for (i in 0 until width * height) {
            val depthValue = Color.red(depthPixels[i]) / 255f
            val srcColor = srcPixels[i]

            // Smooth sigmoid / feathering curve for soft edge cutouts
            val alphaFactor = when {
                depthValue >= threshold + 0.05f -> 1.0f
                depthValue <= threshold - 0.15f -> 0.0f
                else -> (depthValue - (threshold - 0.15f)) / 0.20f
            }.coerceIn(0f, 1f)

            val alphaInt = (alphaFactor * 255).toInt()
            resultPixels[i] = (alphaInt shl 24) or (srcColor and 0x00FFFFFF)
        }

        val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        fgBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
        fgBitmap
    }

    fun mapDepthToColor(depth: Float, palette: DepthHeatmapPalette): Int {
        val v = (depth * 255).toInt().coerceIn(0, 255)
        return when (palette) {
            DepthHeatmapPalette.GRAYSCALE -> {
                Color.rgb(v, v, v)
            }
            DepthHeatmapPalette.THERMAL_RAINBOW -> {
                val h = (1.0f - depth) * 240f
                Color.HSVToColor(floatArrayOf(h, 0.9f, 0.95f))
            }
            DepthHeatmapPalette.CYBERPUNK_NEON -> {
                val r = (depth * 255).toInt()
                val g = ((1.0f - abs(depth - 0.5f) * 2) * 255).toInt()
                val b = ((1.0f - depth) * 255).toInt()
                Color.rgb(r, g, b)
            }
            DepthHeatmapPalette.OCEAN_DEPTH -> {
                val r = (depth * 255).toInt()
                val g = (depth * 200).toInt()
                val b = (100 + (1f - depth) * 155).toInt()
                Color.rgb(r, g, b)
            }
        }
    }
}

