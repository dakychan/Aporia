#version 330

uniform sampler2D Sampler0;

in vec2 screenUv;
in vec4 vColor;

out vec4 fragColor;

/* Kawase-style 5-tap с весами — лёгкий и приятный блюр.
   На вход — фрагменты подсветки игрока (Sampler0 = back-buffer). */
void main() {
    vec2 texel = vec2(1.0) / vec2(textureSize(Sampler0, 0));

    // 9-tap дискретный гаусс, оптимизированный для игроков.
    const float w0 = 0.227027;
    const float w1 = 0.194594;
    const float w2 = 0.121622;
    const float w3 = 0.054054;
    const float w4 = 0.016216;

    vec4 c = texture(Sampler0, screenUv) * w0;
    c += texture(Sampler0, screenUv + vec2( texel.x,  texel.y)) * w1;
    c += texture(Sampler0, screenUv + vec2(-texel.x,  texel.y)) * w1;
    c += texture(Sampler0, screenUv + vec2( texel.x, -texel.y)) * w1;
    c += texture(Sampler0, screenUv + vec2(-texel.x, -texel.y)) * w1;
    c += texture(Sampler0, screenUv + vec2( texel.x * 2.0,  texel.y * 2.0)) * w2;
    c += texture(Sampler0, screenUv + vec2(-texel.x * 2.0,  texel.y * 2.0)) * w2;
    c += texture(Sampler0, screenUv + vec2( texel.x * 2.0, -texel.y * 2.0)) * w2;
    c += texture(Sampler0, screenUv + vec2(-texel.x * 2.0, -texel.y * 2.0)) * w2;
    c += texture(Sampler0, screenUv + vec2( texel.x * 3.0,  texel.y * 3.0)) * w3;
    c += texture(Sampler0, screenUv + vec2(-texel.x * 3.0,  texel.y * 3.0)) * w3;
    c += texture(Sampler0, screenUv + vec2( texel.x * 3.0, -texel.y * 3.0)) * w3;
    c += texture(Sampler0, screenUv + vec2(-texel.x * 3.0, -texel.y * 3.0)) * w3;
    c += texture(Sampler0, screenUv + vec2( texel.x * 4.0,  texel.y * 4.0)) * w4;
    c += texture(Sampler0, screenUv + vec2(-texel.x * 4.0,  texel.y * 4.0)) * w4;
    c += texture(Sampler0, screenUv + vec2( texel.x * 4.0, -texel.y * 4.0)) * w4;
    c += texture(Sampler0, screenUv + vec2(-texel.x * 4.0, -texel.y * 4.0)) * w4;

    // Смешиваем с модельным цветом игрока — даёт мягкое окрашивание.
    fragColor = vec4(c.rgb, c.a * vColor.a);
}
