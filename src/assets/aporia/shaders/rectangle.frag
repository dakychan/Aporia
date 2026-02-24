#version auto

precision lowp float;

in vec4 vertexColor;
in vec2 fragCoord;
in vec2 rectPosition;
in vec2 rectSize;
in float cornerRadius;

#include<shaderColor>
uniform float height;

out vec4 fragColor;

const float edgeSoftness = 1.0;

float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
}

void main() {
    vec2 screenPos = vec2(fragCoord.x, height - fragCoord.y);
    vec2 centerPos = screenPos - rectPosition - rectSize * 0.5;
    
    float distance = roundedBoxSDF(centerPos, rectSize * 0.5, cornerRadius);
    
    float smoothedAlpha = 1.0 - smoothstep(0.0, edgeSoftness, distance);
    
    vec4 finalColor = vertexColor * shaderColor;
    finalColor.a = smoothedAlpha * finalColor.a;
    
    fragColor = finalColor;
}
