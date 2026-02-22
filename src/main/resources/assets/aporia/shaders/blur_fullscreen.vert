#version 330 core

in vec4 pos;
in vec2 UV;
in vec4 color;

layout(std140) uniform Projection {
    mat4 projMat;
};
uniform mat4 modelViewMat;

out vec2 vUv;

void main() {
    vUv = UV;
    gl_Position = projMat * modelViewMat * pos;
}
