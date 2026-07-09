#version 330

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec2 screenUv;
out vec2 modelUv;
out vec3 vViewPos;
out vec3 vNormal;
out vec4 vColor;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vec4 clipPos = ProjMat * viewPos;
    gl_Position = clipPos;

    // screenUv → [0..1], Y-flip для совместимости с back-buffer.
    vec2 ndc = clipPos.xy / clipPos.w;
    screenUv = ndc * 0.5 + 0.5;
    screenUv.y = 1.0 - screenUv.y;

    modelUv = UV0;
    vViewPos = viewPos.xyz;
    vNormal = normalize(mat3(ModelViewMat) * Normal);
    vColor = Color;
}
