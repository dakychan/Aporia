#version 330

uniform sampler2D Sampler0;

in vec2 screenUv;
in vec4 vColor;

out vec4 fragColor;

void main() {
    vec2 blurDir = vec2(0.0015, 0.0015);
    float w[5] = float[5](0.0625, 0.25, 0.375, 0.25, 0.0625);
    vec4 color = vec4(0.0);
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * blurDir;
            color += texture(Sampler0, screenUv + offset) * w[x + 2] * w[y + 2];
        }
    }
    fragColor = color;
}
