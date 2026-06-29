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
    int   mode      = int(params.z);
    float radius    = params.x;
    float smoothing = max(params.y, 0.5);

    vec2  center   = bounds.xy + bounds.zw * 0.5;
    vec2  halfSize = bounds.zw * 0.5;

    float sdfMask = 1.0;

    if (mode == 1) {
        vec2  p = fragPos - center;
        float r = halfSize.x;
        float d = length(p) - r;
        sdfMask = 1.0 - smoothstep(-smoothing, smoothing, d);
    } else if (mode == 2) {
        float d = roundedBoxSDF(fragPos, center, halfSize, radius);
        sdfMask = 1.0 - smoothstep(-smoothing, smoothing, d);
    }

    vec4 texColor = texture(ImageTextureSampler, uv);
    fragColor = vec4(texColor.rgb, texColor.a * sdfMask * vertColor.a);
}
