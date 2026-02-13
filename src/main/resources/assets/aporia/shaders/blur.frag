#version 330 core

uniform sampler2D Sampler0;
uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texelSize = 1.0 / textureSize(Sampler0, 0);
    vec4 result = vec4(0.0);
    float totalWeight = 0.0;
    
    for (float i = -Radius; i <= Radius; i += 1.0) {
        float weight = exp(-(i * i) / (2.0 * Radius * Radius));
        vec2 offset = BlurDir * i * texelSize;
        result += texture(Sampler0, texCoord + offset) * weight;
        totalWeight += weight;
    }
    
    fragColor = result / totalWeight;
}
