#version auto

precision lowp float;

in vec4 vertexColor;
in vec2 rectPosition;
in vec2 halfSize;
in float radius;

#include<shaderColor>
uniform float height;

out vec4 fragColor;

const float edgeSoftness = 1.5;

float roundedBoxSDF(vec2 centerPosition, vec2 size, float radius) {
    return length(max(abs(centerPosition) - size + radius, 0.)) - radius;
}

void main() {
    vec2 _position = vec2(rectPosition.x, height - rectPosition.y);
    float distance = roundedBoxSDF(gl_FragCoord.xy - _position, halfSize, radius);

    float alpha = 1.0 - smoothstep(0.0, edgeSoftness, distance);
    
    vec4 finalColor = vertexColor * shaderColor;
    finalColor.a = alpha * finalColor.a;
    
    fragColor = finalColor;
}
