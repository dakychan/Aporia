#version 330

layout(std140) uniform ModelViewProj {
    mat4 ModelViewProjMat;
    mat4 ModelMat;
    mat4 NormalMat;
};

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec4 vertColor;
out vec3 vertNormal;
out vec3 vertWorldPos;

void main() {
    vec4 worldPos = ModelMat * vec4(Position, 1.0);
    vertWorldPos = worldPos.xyz;
    vertNormal = normalize(mat3(NormalMat) * Normal);
    vertColor = Color;
    gl_Position = ModelViewProjMat * vec4(Position, 1.0);
}
