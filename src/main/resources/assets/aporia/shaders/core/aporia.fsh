#version 330

uniform sampler2D BlurTextureSampler;

layout(std140) uniform ShapeData {
    vec4 bounds;   /* x, y, w, h */
    vec4 params;   /* radius, smoothing, mode, borderMode */
    vec4 params2;  /* thickness, fadeAtCorners, useBlur, unused */
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
    bool  useBlur    = params2.z > 0.5;

    vec2  center   = bounds.xy + bounds.zw * 0.5;
    vec2  halfSize = bounds.zw * 0.5;

    /* SDF mask — shape clipping, always 0..1 */
    float sdfMask = 1.0;
    if (mode == 1) {
        float d = circleSDF(fragPos, center, halfSize.x);
        sdfMask = 1.0 - smoothstep(-smoothing, smoothing, d);
    } else if (mode == 2) {
        float d = roundedBoxSDF(fragPos, center, halfSize, radius);
        if (borderMode == 0) {
            sdfMask = 1.0 - smoothstep(-smoothing, smoothing, d);
        } else {
            float outer  = 1.0 - smoothstep(-smoothing, smoothing, d);
            float inner  = 1.0 - smoothstep(-smoothing, smoothing, d + thickness);
            float stroke = outer - inner;
            if (borderMode == 2) {
                vec2  cornerDist = abs(fragPos - center) - (halfSize - radius);
                float inCorner   = smoothstep(0.0, radius, max(cornerDist.x, 0.0))
                                 * smoothstep(0.0, radius, max(cornerDist.y, 0.0));
                stroke *= mix(1.0 - fadeCorner, 1.0, inCorner);
            }
            sdfMask = stroke;
        }
    }

    if (useBlur) {
        /* Sample blurTarget at screen-space UV */
        vec2 blurUv = gl_FragCoord.xy / screen.xy;
        /* no y-flip: gl_FragCoord.y=0 is bottom, blurTarget UV.y=0 is also bottom */
        vec3 blurColor = texture(BlurTextureSampler, blurUv).rgb;
        /* DEBUG: if blurColor is all black, sampler is not bound — show red */
        if (blurColor.r + blurColor.g + blurColor.b < 0.01) {
            fragColor = vec4(1.0, 0.0, 0.0, sdfMask);
            return;
        }
        /* Add white tint on top — vertColor.a is tint strength (small value like 0.07) */
        blurColor = mix(blurColor, vertColor.rgb, vertColor.a * 0.5);
        /* sdfMask clips the shape — blur is fully opaque inside */
        fragColor = vec4(blurColor, sdfMask);
    } else {
        /* Normal colored shape — alpha = vertColor.a * sdfMask */
        fragColor = vec4(vertColor.rgb, vertColor.a * sdfMask);
    }
}
