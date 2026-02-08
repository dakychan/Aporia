#version auto

in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

#include<shaderColor>
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float pxRange; // Distance range in pixels

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec3 msd = texture(Sampler0, texCoord).rgb;
    float sd = median(msd.r, msd.g, msd.b);
    float screenPxDistance = pxRange * (sd - 0.5);
    float opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);

    fragColor = ColorModulator * shaderColor * vertexColor * vec4(1.0, 1.0, 1.0, opacity);
}