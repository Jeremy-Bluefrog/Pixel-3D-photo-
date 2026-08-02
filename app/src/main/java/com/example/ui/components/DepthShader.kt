package com.example.ui.components

import org.intellij.lang.annotations.Language

@Language("AGSL")
const val DEPTH_DISPLACEMENT_SHADER = """
    uniform shader image;
    uniform shader depthMap;
    uniform float2 resolution;
    uniform float2 offset;
    
    half4 main(float2 fragCoord) {
        // Sample depth map. We assume white (1.0) is close and black (0.0) is far.
        // We can scale the offset by the depth value.
        // Normalize fragCoord to 0..1 for texture sampling if needed, but in AGSL we use exact coords.
        half depth = depthMap.eval(fragCoord).r;
        
        // Apply displacement based on depth.
        // Foreground (depth close to 1.0) moves more with the offset
        // Background (depth close to 0.0) moves less or opposite
        // We shift the image coordinates opposite to the offset to create the parallax effect
        // Subtract 0.5 from depth so mid-ground stays still
        float depthFactor = (float(depth) - 0.5) * 2.0; 
        
        float2 displacedCoord = fragCoord - (offset * depthFactor);
        
        // Clamp displacedCoord to image boundaries to avoid edge bleeding
        displacedCoord = clamp(displacedCoord, float2(0.0), resolution);
        
        return image.eval(displacedCoord);
    }
"""
