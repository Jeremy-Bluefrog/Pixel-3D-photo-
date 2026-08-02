package com.example.ml

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LdiResult(
    val foreground: Bitmap,
    val background: Bitmap
)

class LayeredDepthProcessor {

    /**
     * Segments a camera preview frame (or any Bitmap) into foreground and background
     * layers based on a depth map, using the Layered Depth Image (LDI) approach.
     */
    suspend fun processLayeredDepth(
        sourceFrame: Bitmap,
        depthMap: Bitmap,
        threshold: Float = 0.45f
    ): LdiResult = withContext(Dispatchers.Default) {
        val width = sourceFrame.width
        val height = sourceFrame.height

        val scaledDepth = Bitmap.createScaledBitmap(depthMap, width, height, true)

        val srcPixels = IntArray(width * height)
        val depthPixels = IntArray(width * height)
        
        val fgPixels = IntArray(width * height)
        val bgPixels = IntArray(width * height)

        sourceFrame.getPixels(srcPixels, 0, width, 0, 0, width, height)
        scaledDepth.getPixels(depthPixels, 0, width, 0, 0, width, height)

        val isBg = BooleanArray(width * height)

        for (i in 0 until width * height) {
            val depthValue = Color.red(depthPixels[i]) / 255f
            if (depthValue >= threshold) {
                // Foreground
                fgPixels[i] = srcPixels[i]
                isBg[i] = false
                bgPixels[i] = Color.TRANSPARENT // To be inpainted
            } else {
                // Background
                fgPixels[i] = Color.TRANSPARENT
                isBg[i] = depthValue < (threshold - 0.05f) // Stricter background threshold for inpainting
                bgPixels[i] = srcPixels[i]
            }
        }

        // Inpaint background holes
        inpaintBackground(bgPixels, isBg, width, height)

        val fgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        fgBitmap.setPixels(fgPixels, 0, width, 0, 0, width, height)

        val bgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bgBitmap.setPixels(bgPixels, 0, width, 0, 0, width, height)

        LdiResult(fgBitmap, bgBitmap)
    }

    /**
     * Fills the holes in the background layer (where the foreground was removed)
     * using a Distance-Weighted 4-Directional Propagation Inpainting algorithm.
     */
    private fun inpaintBackground(bgPixels: IntArray, isBg: BooleanArray, width: Int, height: Int) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                if (!isBg[i]) {
                    var leftColor = bgPixels[i]
                    var leftDist = width
                    for (dx in x downTo 0) {
                        if (isBg[y * width + dx]) { leftColor = bgPixels[y * width + dx]; leftDist = x - dx; break }
                    }
                    var rightColor = bgPixels[i]
                    var rightDist = width
                    for (dx in x until width) {
                        if (isBg[y * width + dx]) { rightColor = bgPixels[y * width + dx]; rightDist = dx - x; break }
                    }
                    var upColor = bgPixels[i]
                    var upDist = height
                    for (dy in y downTo 0) {
                        if (isBg[dy * width + x]) { upColor = bgPixels[dy * width + x]; upDist = y - dy; break }
                    }
                    var downColor = bgPixels[i]
                    var downDist = height
                    for (dy in y until height) {
                        if (isBg[dy * width + x]) { downColor = bgPixels[dy * width + x]; downDist = dy - y; break }
                    }

                    // Weight by inverse squared distance for better edge preservation and smooth blending
                    val wl = 1f / (leftDist * leftDist + 1f)
                    val wr = 1f / (rightDist * rightDist + 1f)
                    val wu = 1f / (upDist * upDist + 1f)
                    val wd = 1f / (downDist * downDist + 1f)
                    val sumW = wl + wr + wu + wd

                    val r = ((Color.red(leftColor) * wl + Color.red(rightColor) * wr + Color.red(upColor) * wu + Color.red(downColor) * wd) / sumW).toInt()
                    val g = ((Color.green(leftColor) * wl + Color.green(rightColor) * wr + Color.green(upColor) * wu + Color.green(downColor) * wd) / sumW).toInt()
                    val b = ((Color.blue(leftColor) * wl + Color.blue(rightColor) * wr + Color.blue(upColor) * wu + Color.blue(downColor) * wd) / sumW).toInt()
                    val a = 255

                    bgPixels[i] = Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                }
            }
        }
        
        // Apply a fast 5x5 blur ONLY on the inpainted regions to smooth out the propagation seams
        val tempPixels = bgPixels.copyOf()
        for (y in 2 until height - 2) {
            for (x in 2 until width - 2) {
                if (!isBg[y * width + x]) {
                    var sr = 0; var sg = 0; var sb = 0
                    for (dy in -2..2) {
                        for (dx in -2..2) {
                            val c = tempPixels[(y + dy) * width + (x + dx)]
                            sr += Color.red(c)
                            sg += Color.green(c)
                            sb += Color.blue(c)
                        }
                    }
                    bgPixels[y * width + x] = Color.rgb(sr / 25, sg / 25, sb / 25)
                }
            }
        }
    }
}
