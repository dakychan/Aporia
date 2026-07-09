#version 330

uniform sampler2D Sampler0;

in vec2 screenUv;
in vec4 vColor;

out vec4 fragColor;

void main() {
    vec2 texel = vec2(1.0) / vec2(textureSize(Sampler0, 0));
    
    // Веса для нормального распределения (в сумме дают 1.0)
    float w0 = 0.227027;
    float w1 = 0.121622;
    float w2 = 0.054054;

    vec4 c = texture(Sampler0, screenUv) * w0;

    // Блюрим по горизонтали и вертикали вместо диагоналей
    c += texture(Sampler0, screenUv + vec2(texel.x, 0.0)) * w1;
    c += texture(Sampler0, screenUv - vec2(texel.x, 0.0)) * w1;
    c += texture(Sampler0, screenUv + vec2(0.0, texel.y)) * w1;
    c += texture(Sampler0, screenUv - vec2(0.0, texel.y)) * w1;

    c += texture(Sampler0, screenUv + vec2(texel.x * 2.0, 0.0)) * w2;
    c += texture(Sampler0, screenUv - vec2(texel.x * 2.0, 0.0)) * w2;
    c += texture(Sampler0, screenUv + vec2(0.0, texel.y * 2.0)) * w2;
    c += texture(Sampler0, screenUv - vec2(0.0, texel.y * 2.0)) * w2;

    fragColor = vec4(c.rgb, c.a * vColor.a);
}

