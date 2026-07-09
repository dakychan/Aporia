#version 150

uniform sampler2D InputTexture;

layout(std140) uniform KawaseData {
    vec2  InputResolution; 
    float Offset;          
    float _pad0, _pad1, _pad2;
};

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 texelSize = 1.0 / InputResolution;
    vec2 uv = TexCoord;
    
    // Усиленный блюр + базовый шаг 0.5
    vec2 d = texelSize * (Offset * 1.2 + 0.5);
    
    // Аппаратное смешивание 4 пикселей
    vec2 f = fract(uv * InputResolution - 0.5);
    vec2 c = (uv * InputResolution - 0.5 - f) / InputResolution;
    
    vec3 c00 = texture(InputTexture, c).rgb;
    vec3 c10 = texture(InputTexture, c + vec2(texelSize.x, 0.0)).rgb;
    vec3 c01 = texture(InputTexture, c + vec2(0.0, texelSize.y)).rgb;
    vec3 c11 = texture(InputTexture, c + texelSize).rgb;
    
    vec3 baseColor = mix(mix(c00, c10, f.x), mix(c01, c11, f.x), f.y) * 4.0;
    
    // Кавасе по диагонали
    vec3 color = baseColor;
    color += texture(InputTexture, uv + vec2(-d.x, -d.y)).rgb;
    color += texture(InputTexture, uv + vec2( d.x, -d.y)).rgb;
    color += texture(InputTexture, uv + vec2(-d.x,  d.y)).rgb;
    color += texture(InputTexture, uv + vec2( d.x,  d.y)).rgb;

    vec3 finalBlur = color / 8.0;

    OutColor = vec4(finalBlur, 1.0);
}