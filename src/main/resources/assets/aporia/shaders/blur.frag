#version 330 core

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uTexture;
uniform vec2 uTexelSize;
uniform float uRadius;

void main() {
    vec4 sum = vec4(0.0);
    float total = 0.0;
    
    for (float x = -uRadius; x <= uRadius; x++) {
        for (float y = -uRadius; y <= uRadius; y++) {
            vec2 offset = vec2(x, y) * uTexelSize;
            float weight = 1.0 / (1.0 + length(vec2(x, y)));
            sum += texture(uTexture, vTexCoord + offset) * weight;
            total += weight;
        }
    }
    
    FragColor = sum / total;
}
