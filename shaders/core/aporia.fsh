#version 330

uniform sampler2D BlurTextureSampler;
uniform float time;

layout(std140) uniform ShapeData {
    vec4 bounds;        // xy = pos, zw = size
    vec4 params;        // x = corner radius
    vec4 params2;
    vec4 screen;        // xy = screen resolution
};

in vec2 uv;
in vec2 fragPos;
in vec2 screenPos;
out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(p - center) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    float containerRadius = params.x;
    vec2 panelCenter = bounds.xy + bounds.zw * 0.5;
    vec2 panelHalfSize = bounds.zw * 0.5;
    vec2 fragPosLocal = fragPos;
    
    float boundsSDF = roundedBoxSDF(fragPosLocal, panelCenter, panelHalfSize, containerRadius);
    vec2 gradSDF = vec2(dFdx(boundsSDF), dFdy(boundsSDF));
    float containerSmoothing = max(length(gradSDF), 0.001);
    float containerMask = clamp(0.5 - boundsSDF / containerSmoothing, 0.0, 1.0);
    
    if (containerMask <= 0.005) discard;
    
    // ===== НАСТРОЙКИ =====
    float edge_width = 0.08;         
    float corner_boost = 1.5;        
    float FresnelPower = 2.0;        
    float DistortStrength = 0.03;    
    float darken = 0.12;            
    // =====================
    
    vec2 pos = fragPosLocal - panelCenter;
    float distToEdge = abs(roundedBoxSDF(pos, vec2(0.0), panelHalfSize, containerRadius));
    
    float angle = atan(pos.y, pos.x);
    float cornerFactor = 1.0 + (corner_boost - 1.0) * abs(sin(2.0 * angle));
    float edge_width_corner = edge_width * cornerFactor;
    
    float max_dist = min(panelHalfSize.x, panelHalfSize.y) * edge_width_corner;
    float edge_gradient = 1.0 - smoothstep(0.0, max_dist, distToEdge);
    
    float base = edge_gradient;
    float fresnel = pow(clamp(base, 0.0, 1.0), FresnelPower);
    fresnel = clamp(fresnel, 0.0, 1.0);
    
    vec2 dir = normalize(pos);
    vec2 distortedTexCoord = screenPos + dir * fresnel * DistortStrength * containerMask;
    
    vec4 texColor = texture(BlurTextureSampler, distortedTexCoord);
    
    vec3 finalColor = mix(texColor.rgb, vec3(0.0), darken);
    fragColor = vec4(finalColor, containerMask);
}