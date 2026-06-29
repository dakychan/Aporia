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
    
    // ЖЕСТКИЙ ФИКС БЕЛОГО НАЛЕТА И ТРЕУГОЛЬНИКОВ:
    // Переходим с косого диагонального Кавасе на ультра-гладкий 9-точечный крест.
    // Это исключает появление математических складок и фейдов на ярком фоне.
    vec2 d = texelSize * (Offset * 0.75); 
    vec2 uv = TexCoord;

    // Сэмплирование крестом (Центр + 4 стороны ближе + 4 стороны дальше)
    vec3 color = texture(InputTexture, uv).rgb * 4.0;
    
    color += texture(InputTexture, uv + vec2(-d.x, 0.0)).rgb * 2.0;
    color += texture(InputTexture, uv + vec2( d.x, 0.0)).rgb * 2.0;
    color += texture(InputTexture, uv + vec2(0.0, -d.y)).rgb * 2.0;
    color += texture(InputTexture, uv + vec2(0.0,  d.y)).rgb * 2.0;
    
    color += texture(InputTexture, uv + vec2(-d.x * 2.0, 0.0)).rgb * 1.0;
    color += texture(InputTexture, uv + vec2( d.x * 2.0, 0.0)).rgb * 1.0;
    color += texture(InputTexture, uv + vec2(0.0, -d.y * 2.0)).rgb * 1.0;
    color += texture(InputTexture, uv + vec2(0.0,  d.y * 2.0)).rgb * 1.0;

    // Нормируем сумму весов (4 + 2*4 + 1*4 = 16)
    vec3 finalBlur = color / 16.0;

    // Принудительно забиваем альфу в 1.0, чтобы небо гарантированно не чернело
    OutColor = vec4(finalBlur, 1.0);
}
