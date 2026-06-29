#version 330

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 uv;
out vec4 vertColor;
out vec2 fragPos;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    uv = UV0;
    vertColor = Color;
    fragPos = Position.xy;
}
