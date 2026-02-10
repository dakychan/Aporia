#version 150

uniform sampler2D Sampler0;
uniform vec2 BlurDir;
uniform float Radius;
uniform vec4 ColorTint;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 blurred = vec4(0.0);
    float totalWeight = 0.0;

    int samples = int(Radius);
    for (int i = -samples; i <= samples; i++) {
        float weight = exp(-float(i * i) / (2.0 * Radius * Radius));
        vec2 offset = BlurDir * float(i) / textureSize(Sampler0, 0);
        blurred += texture(Sampler0, texCoord + offset) * weight;
        totalWeight += weight;
    }
    
    blurred /= totalWeight;

    fragColor = mix(blurred, vec4(ColorTint.rgb, 1.0), ColorTint.a * 0.3);
}
