#version 330

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform Rect {
    vec4 rect;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out vec4 vertColor;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    texCoord = UV0;
    vertColor = Color;
}
