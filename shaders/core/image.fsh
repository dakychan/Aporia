#version 330

uniform sampler2D ImageTextureSampler;

layout(std140) uniform ShapeData {
    vec4 bounds;   /* x, y, w, h */
    vec4 params;   /* radius, smoothing, mode, borderMode */
    vec4 params2;  /* thickness, fadeAtCorners, unused, unused */
    vec4 screen;   /* screenW, screenH, unused, unused */
};

in vec2 uv;
in vec4 vertColor;
in vec2 fragPos;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(p - center) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    int   mode        = int(params.z);
    int   borderMode  = int(params.w);
    float radius      = params.x;
    float smoothing   = max(params.y, 0.5);
    float thickness   = params2.x; // Толщина обводки

    vec2  center   = bounds.xy + bounds.zw * 0.5;
    vec2  halfSize = bounds.zw * 0.5;

    float d = 0.0;

    // 1. Считаем базовое расстояние (SDF) в зависимости от режима
    if (mode == 1) {
        d = length(fragPos - center) - halfSize.x;
    } else {
        d = roundedBoxSDF(fragPos, center, halfSize, radius);
    }

    // Использование экранных градиентов для адаптивного сглаживания (чтобы при зуме не мылило)
    vec2 gradSDF = vec2(dFdx(d), dFdy(d));
    float delta = length(gradSDF);
    float finalSmoothing = (delta > 0.0) ? delta * smoothing : smoothing;

    float alphaMask = 1.0;
    
    // 2. Логика отрисовки: обычная плашка или контур (Border)
    if (borderMode == 1) {
        // Режим обводки: вырезаем внутреннюю часть на величину толщины (thickness)
        float borderSDF = abs(d + thickness * 0.5) - thickness * 0.5;
        alphaMask = 1.0 - smoothstep(-finalSmoothing, finalSmoothing, borderSDF);
    } else {
        // Режим сплошной заливки
        alphaMask = 1.0 - smoothstep(-finalSmoothing, finalSmoothing, d);
    }

    // 3. Сэмплируем текстуру (если это размытый задний фон из бэк-буфера)
    vec4 texColor = texture(ImageTextureSampler, uv);

    // Если текстура пустая или белая, можно красить её через vertColor для гибкости
    vec4 finalColor = texColor * vertColor;

    // Применяем нашу SDF маску к альфа-каналу
    fragColor = vec4(finalColor.rgb, finalColor.a * alphaMask);
}
