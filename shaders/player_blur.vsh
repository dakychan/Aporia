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

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
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
out float vTime;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vec4 clipPos = ProjMat * viewPos;
    gl_Position = clipPos;

    screenUv = (clipPos.xy / clipPos.w) * 0.5 + 0.5;
    screenUv.y = 1.0 - screenUv.y;

    modelUv = UV0;
    vViewPos = viewPos.xyz;
    vNormal = normalize(mat3(ModelViewMat) * Normal);
    vColor = Color;
    vTime = GameTime * 1200.0;
}
