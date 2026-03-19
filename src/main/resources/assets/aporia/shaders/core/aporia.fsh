#version 330

layout(std140) uniform ShapeData {
    vec4 bounds;   /* x, y, w, h */
    vec4 params;   /* radius, smoothing, mode, borderMode */
    vec4 params2;  /* thickness, fadeAtCorners, unused, unused */
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
    int  mode        = int(params.z);
    int  borderMode  = int(params.w);  /* 0=fill, 1=full stroke, 2=corners-only */
    float radius     = params.x;
    float smoothing  = max(params.y, 0.5);
    float thickness  = params2.x;      /* stroke thickness in px */
    float fadeCorner = params2.y;      /* 0..1 — how much alpha fades toward corners */
    float alpha      = vertColor.a;

    if (mode == 1) {
        /* circle */
        vec2  center = bounds.xy + bounds.zw * 0.5;
        float r      = bounds.z * 0.5;
        float d      = circleSDF(fragPos, center, r);
        alpha *= 1.0 - smoothstep(-smoothing, smoothing, d);
    } else if (mode == 2) {
        vec2  center   = bounds.xy + bounds.zw * 0.5;
        vec2  halfSize = bounds.zw * 0.5;
        float d        = roundedBoxSDF(fragPos, center, halfSize, radius);

        if (borderMode == 0) {
            /* normal fill */
            alpha *= 1.0 - smoothstep(-smoothing, smoothing, d);
        } else {
            /* stroke = ring between outer and inner SDF */
            float outer = 1.0 - smoothstep(-smoothing, smoothing, d);
            float inner = 1.0 - smoothstep(-smoothing, smoothing, d + thickness);
            float stroke = outer - inner;

            if (borderMode == 2) {
                /* corners-only: fade out the straight edges */
                /* measure how close we are to a corner arc vs a straight edge */
                vec2 lp = abs(fragPos - center);
                /* corner region: both axes are within radius of the corner */
                vec2 cornerDist = lp - (halfSize - radius);
                float inCorner = smoothstep(0.0, radius, max(cornerDist.x, 0.0))
                               * smoothstep(0.0, radius, max(cornerDist.y, 0.0));
                /* also fade straight edges based on fadeCorner param */
                float edgeFade = mix(1.0 - fadeCorner, 1.0, inCorner);
                stroke *= edgeFade;
            }

            alpha *= stroke;
        }
    }

    fragColor = vec4(vertColor.rgb, alpha);
}
