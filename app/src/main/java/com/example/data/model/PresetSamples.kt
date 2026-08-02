package com.example.data.model

object PresetSamples {
    val samplePhotos = listOf(
        SpatialPhoto(
            id = 1,
            title = "Pixel 9 Pro 霓虹夜景",
            sourceUri = "preset_neon_city",
            depthMapUri = "preset_neon_city_depth",
            captureType = SpatialCaptureType.PIXEL9_DUAL_CAMERA_SPATIAL,
            depthIntensity = 0.5f,
            focalPlane = 0.40f,
            blurAmount = 8.0f,
            layerSeparation = 1.8f,
            aiAnalysis = "前景：街頭霓虹指標與主角人影 (邊緣清晰度 9.8/10)\n中景：雨夜濕潤反射地面與移動車流\n背景：遠處摩天大樓景深散景",
            isFavorite = true
        ),
        SpatialPhoto(
            id = 2,
            title = "空間雙鏡頭 - 富士山雲海",
            sourceUri = "preset_mountain",
            depthMapUri = "preset_mountain_depth",
            captureType = SpatialCaptureType.PIXEL9_DUAL_CAMERA_SPATIAL,
            depthIntensity = 0.5f,
            focalPlane = 0.50f,
            blurAmount = 5.0f,
            layerSeparation = 1.5f,
            aiAnalysis = "前景：近處松樹與岩石層 (立體雙鏡頭計算)\n中景：圍繞山腰的層次雲海\n背景：夕陽餘暉下的富士山尖",
            isFavorite = false
        ),
        SpatialPhoto(
            id = 3,
            title = "AI 人像立體透視",
            sourceUri = "preset_portrait",
            depthMapUri = "preset_portrait_depth",
            captureType = SpatialCaptureType.AI_CONVERTED_2D,
            depthIntensity = 0.5f,
            focalPlane = 0.35f,
            blurAmount = 10.0f,
            layerSeparation = 2.0f,
            aiAnalysis = "前景：人物髮絲與臉部輪廓 (機器學習發光邊緣分析)\n中景：肩部與背景光斑\n背景：柔和羽化夢幻散景",
            isFavorite = true
        ),
        SpatialPhoto(
            id = 4,
            title = "空間建築對稱透視",
            sourceUri = "preset_architecture",
            depthMapUri = "preset_architecture_depth",
            captureType = SpatialCaptureType.AI_CONVERTED_2D,
            depthIntensity = 0.5f,
            focalPlane = 0.60f,
            blurAmount = 4.0f,
            layerSeparation = 1.4f,
            aiAnalysis = "前景：迴廊前柱與大理石地磚\n中景：幾何對稱穹頂結構\n背景：遠端透視光輝焦點",
            isFavorite = false
        )
    )
}
