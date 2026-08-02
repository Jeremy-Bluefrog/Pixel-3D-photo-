shader = """
uniform shader image;
uniform shader depthMap;
uniform float2 resolution;
uniform float2 offset;

half4 main(float2 fragCoord) {
    // 1. Convert pixel coordinates to normalized 0.0 - 1.0 uv
    float2 uv = fragCoord / resolution;
    
    // 2. Crop & Scale (oversampling to avoid black borders)
    float scale = 1.05; // 5% crop
    float2 scaledUv = (uv - 0.5) / scale + 0.5;
    
    // Normalize offset relative to resolution for uv manipulation
    float2 normalizedOffset = offset / resolution;

    // We do a simple iterative parallax mapping.
    // Since we are looking into the screen, depth 1.0 is near, 0.0 is far.
    // The camera moves by `offset`. 
    const int STEPS = 15;
    float stepSize = 1.0 / float(STEPS);
    float2 deltaUv = normalizedOffset * stepSize;
    
    float2 currentUv = scaledUv + normalizedOffset * 0.5; // Start shifted
    float currentDepth = 1.0;
    
    for (int i = 0; i < STEPS; i++) {
        // Read depth at current UV
        float2 sampleUv = clamp(currentUv, 0.0, 1.0);
        float sampledDepth = depthMap.eval(sampleUv * resolution).r;
        
        if (sampledDepth >= currentDepth) {
            // Found intersection
            break;
        }
        
        currentUv -= deltaUv;
        currentDepth -= stepSize;
    }
    
    // Clamp to edge to handle out of bounds (edge stretching)
    float2 finalUv = clamp(currentUv, 0.0, 1.0);
    return image.eval(finalUv * resolution);
}
"""
