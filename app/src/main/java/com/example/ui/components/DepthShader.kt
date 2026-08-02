package com.example.ui.components

import org.intellij.lang.annotations.Language

@Language("AGSL")
const val DEPTH_DISPLACEMENT_SHADER = """
    uniform shader image;
    uniform shader depthMap;
    uniform float2 resolution;
    uniform float2 offset; // displacement from gyroscope
    
    half4 main(float2 fragCoord) {
        // Normalize coordinates to 0.0 - 1.0 UV space
        float2 uv = fragCoord / resolution;
        
        // 4. 遮蔽區處理與裁切 (Occlusion & Oversampling)
        // Crop & Scale to prevent showing empty borders when tilting
        float scale = 0.95;
        float2 scaledUv = (uv - 0.5) * scale + 0.5;
        
        // Normalize offset to UV space
        float2 maxDisplacement = offset / resolution;
        
        // 3. GPU 片段著色器視差計算 (Parallax Shader Rendering)
        // Parallax Occlusion Mapping (POM) via Ray Marching
        const int STEPS = 32; // Higher steps for smoother depth transitions
        float2 rayDelta = maxDisplacement / float(STEPS);
        
        // We start ray marching from the foreground (depth = 1.0)
        float2 currentUv = scaledUv + maxDisplacement;
        float currentDepth = 1.0;
        
        for (int i = 0; i < STEPS; i++) {
            // Clamp-to-edge to handle out-of-bounds UV sampling (edge stretching/inpainting)
            float2 sampleUv = clamp(currentUv, float2(0.001), float2(0.999));
            float sampledDepth = depthMap.eval(sampleUv * resolution).r;
            
            // If the sampled depth from the depth map is closer or equal to our current ray depth,
            // we hit the surface.
            if (sampledDepth >= currentDepth) {
                break;
            }
            
            // Step the ray further back into the image (towards depth 0.0)
            currentUv -= rayDelta;
            currentDepth -= 1.0 / float(STEPS);
        }
        
        // Final clamp-to-edge for safety to prevent edge tearing artifacts
        float2 finalUv = clamp(currentUv, float2(0.001), float2(0.999));
        
        return image.eval(finalUv * resolution);
    }
"""
