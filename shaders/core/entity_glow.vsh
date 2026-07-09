#version 330 core

in vec3 Position;
in vec3 Normal;

out vec3 vNormal;
out vec3 vViewPos;
out float vFresnel;

layout(std140) uniform ModelViewProj {
    mat4 uProj;
    mat4 uView;
    mat4 uModel;
};

void main() {
    mat4 mvMat = uView * uModel;
    gl_Position = uProj * mvMat * vec4(Position, 1.0);
    
    vNormal = normalize(mat3(mvMat) * Normal);
    vViewPos = vec3(mvMat * vec4(Position, 1.0));
    
    vec3 viewDir = normalize(-vViewPos);
    vFresnel = 1.0 - max(dot(vNormal, viewDir), 0.0);
    vFresnel = pow(vFresnel, 2.0); // Усиливаем края
}