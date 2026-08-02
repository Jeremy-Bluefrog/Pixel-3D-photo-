package com.example.data.model

data class SpatialPhoto(
    val id: Long = 0,
    val title: String,
    val sourceUri: String,
    val depthMapUri: String = "",
    val captureType: SpatialCaptureType = SpatialCaptureType.AI_CONVERTED_2D,
    val depthIntensity: Float = 0.5f,
    val focalPlane: Float = 0.5f,
    val blurAmount: Float = 6.0f,
    val layerSeparation: Float = 1.5f,
    val aiAnalysis: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val width: Int = 1080,
    val height: Int = 1440
)

enum class SpatialCaptureType {
    AI_CONVERTED_2D,
    PIXEL9_DUAL_CAMERA_SPATIAL
}

enum class Render3DMode {
    PARALLAX_TILT,
    WIGGLE_STEREOGRAM,
    ANAGLYPH_3D,
    DEPTH_MAP_HEATMAP,
    LAYER_CUTOUT,
    POINT_CLOUD_SPLAT
}

enum class DepthHeatmapPalette {
    GRAYSCALE,
    THERMAL_RAINBOW,
    CYBERPUNK_NEON,
    OCEAN_DEPTH
}
