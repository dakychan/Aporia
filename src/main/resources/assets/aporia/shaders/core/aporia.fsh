#version 330

layout(std140) uniform ShapeData {
    vec4 bounds;
    vec4 params;
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
    int mode = int(params.z);
    float radius = params.x;
    float smoothing = max(params.y, 0.5);
    float alpha = vertColor.a;

    if (mode == 1) {
        vec2 center = bounds.xy + bounds.zw * 0.5;
        float r = bounds.z * 0.5;
        float d = circleSDF(fragPos, center, r);
        alpha *= 1.0 - smoothstep(-smoothing, smoothing, d);
    } else if (mode == 2) {
        vec2 center = bounds.xy + bounds.zw * 0.5;
        vec2 halfSize = bounds.zw * 0.5;
        float d = roundedBoxSDF(fragPos, center, halfSize, radius);
        alpha *= 1.0 - smoothstep(-smoothing, smoothing, d);
    }

    fragColor = vec4(vertColor.rgb, alpha);
}
