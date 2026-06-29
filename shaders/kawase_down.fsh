#version 150

uniform sampler2D InputTexture;

layout(std140) uniform KawaseData {
    vec2  InputResolution; 
    float Strength;       
    float Saturation;
    float _pad0, _pad1, _pad2;
};

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 texelSize = 1.0 / InputResolution;
    vec2 offset = texelSize * 0.5;
    vec2 uv = TexCoord;

    // 4 угла с весом 1
    vec4 color = texture(InputTexture, uv + vec2(-offset.x, -offset.y));
    color     += texture(InputTexture, uv + vec2( offset.x, -offset.y));
    color     += texture(InputTexture, uv + vec2(-offset.x,  offset.y));
    color     += texture(InputTexture, uv + vec2( offset.x,  offset.y));

    // Центральный пиксель с весом 4 (РАЗМЫВАЕТ КИРПИЧИ 2x2!)
    color += texture(InputTexture, uv) * 4.0;

    // Делим на 8 (4 угла + 4 от центра)
    color /= 8.0;

    // Сатурация
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    color.rgb = mix(vec3(luma), color.rgb, Saturation * 2.0);

    OutColor = vec4(color.rgb, 1.0);
}