#version 330 core

precision lowp float;

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform vec2 direction;
uniform float radius;

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(Sampler0, 0));
    vec4 result = vec4(0.0);
    float totalWeight = 0.0;
    
    for (float i = -radius; i <= radius; i += 1.0) {
        float weight = exp(-(i * i) / (2.0 * radius * radius));
        vec2 offset = direction * texelSize * i;
        result += texture(Sampler0, texCoord + offset) * weight;
        totalWeight += weight;
    }
    
    fragColor = result / totalWeight;
}
