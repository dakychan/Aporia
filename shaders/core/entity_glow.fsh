#version 330

in vec3 vNormal;
in vec3 vViewPos;
in float vFresnel;

out vec4 fragColor;

layout(std140) uniform DrawParams {
    vec4 Color;
    vec4 Params;
} params;

void main() {
    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
}
