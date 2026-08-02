package com.example.ui.components

import org.intellij.lang.annotations.Language

@Language("AGSL")
const val DEPTH_DISPLACEMENT_SHADER = """
    uniform shader image;
    uniform shader depthMap;
    uniform float2 resolution;
    uniform float2 offset;
    
    half4 main(float2 fragCoord) {
        // We use Parallax Occlusion Mapping (POM) via Ray Marching to simulate a true 3D surface
        const int STEPS = 24;
        float stepSize = 1.0 / float(STEPS);
        
        // offset represents the camera movement.
        // The total texture coordinate shift from z=1 (foreground) to z=0 (background)
        float2 rayDir = (offset * 2.0) * stepSize;
        
        // Start ray at z = 1.0 (foreground plane)
        float2 currentTexCoord = fragCoord - offset;
        float currentZ = 1.0;
        
        // Initial depth sample
        float sampledDepth = depthMap.eval(currentTexCoord).r;
        
        // Ray marching loop to find the intersection with the 3D depth surface
        for (int i = 0; i < STEPS; i++) {
            if (sampledDepth >= currentZ) {
                break;
            }
            currentTexCoord += rayDir;
            currentZ -= stepSize;
            
            // Re-sample depth at the new coordinate
            float2 clampedCoord = clamp(currentTexCoord, float2(0.0), resolution);
            sampledDepth = depthMap.eval(clampedCoord).r;
        }
        
        // We found the intersection point in the 3D mesh.
        currentTexCoord = clamp(currentTexCoord, float2(0.0), resolution);
        return image.eval(currentTexCoord);
    }
"""
