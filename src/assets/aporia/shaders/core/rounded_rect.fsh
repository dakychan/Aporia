#version 330

layout(std140) uniform Rect {
    vec4 rect;
};

layout(std140) uniform Radius {
    vec4 radiusData;
};

layout(std140) uniform Smoothing {
    vec4 smoothingData;
};

in vec2 texCoord;
in vec4 vertColor;

out vec4 fragColor;

float roundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    float r = radiusData.x;
    float smoothing = smoothingData.x;
    vec2 center = rect.xy + rect.zw * 0.5;
    vec2 halfSize = rect.zw * 0.5;
    vec2 p = (texCoord * rect.zw + rect.xy) - center;
    float d = roundedBox(p, halfSize, r);
    float alpha = 1.0 - smoothstep(-smoothing, smoothing, d);
    fragColor = vertColor * vec4(1.0, 1.0, 1.0, alpha);
}
