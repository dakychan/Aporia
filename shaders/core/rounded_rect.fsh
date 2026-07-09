#version 330

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

float circleSDF(vec2 p, vec2 center, float r) {
    return length(p - center) - r;
}

float roundedBoxSDF(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(p - center) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    int   mode       = int(params.z);
    int   borderMode = int(params.w);
    float radius     = params.x;
    float smoothing  = max(params.y, 0.5);
    float thickness  = params2.x;
    float fadeCorner = params2.y;

    vec2  center   = bounds.xy + bounds.zw * 0.5;
    vec2  halfSize = bounds.zw * 0.5;

    float sdfMask = 1.0;
    float sdfVal = 0.0;
    
    // 1. Считаем базовый SDF в зависимости от режима
    if (mode == 1) {
        sdfVal = circleSDF(fragPos, center, halfSize.x);
    } else if (mode == 2) {
        sdfVal = roundedBoxSDF(fragPos, center, halfSize, radius);
    } else {
        fragColor = vertColor;
        return;
    }

    // 2. Аппаратное сглаживание через экранные градиенты (SDF дельта)
    // dFdx и dFdy считают изменение расстояния на один пиксель монитора.
    // Это гарантирует нулевой алиасинг при любом разрешении и зуме.
    vec2  derivatives = vec2(dFdx(sdfVal), dFdy(sdfVal));
    float delta = length(derivatives);
    float finalSmoothing = (delta > 0.0) ? delta * smoothing : smoothing;

    // 3. Логика маскирования и отрисовки контуров
    if (mode == 1) {
        sdfMask = 1.0 - smoothstep(-finalSmoothing, finalSmoothing, sdfVal);
    } else if (mode == 2) {
        if (borderMode == 0) {
            // Обычная сплошная скругленная плашка
            sdfMask = 1.0 - smoothstep(-finalSmoothing, finalSmoothing, sdfVal);
        } else {
            // Продвинутый Stroke (Обводка)
            float outer  = 1.0 - smoothstep(-finalSmoothing, finalSmoothing, sdfVal);
            float inner  = 1.0 - smoothstep(-finalSmoothing, finalSmoothing, sdfVal + thickness);
            float stroke = outer - inner;
            
            // borderMode == 2: Эффект затухания бордера на закруглениях углов
            if (borderMode == 2) {
                vec2  cornerDist = abs(fragPos - center) - (halfSize - radius);
                float inCorner   = smoothstep(0.0, radius, max(cornerDist.x, 0.0))
                                 * smoothstep(0.0, radius, max(cornerDist.y, 0.0));
                stroke *= mix(1.0 - fadeCorner, 1.0, inCorner);
            }
            sdfMask = stroke;
        }
    }

    // Финальный вывод цвета геометрии с наложением идеальной маски
    fragColor = vec4(vertColor.rgb, vertColor.a * sdfMask);
}
