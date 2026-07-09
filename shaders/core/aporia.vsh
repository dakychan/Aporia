#version 330

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(std140) uniform ShapeData {
    vec4 bounds;
    vec4 params;
    vec4 params2;
    vec4 screen;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 uv;
out vec4 vertColor;
out vec2 fragPos;
out vec2 screenPos;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    uv = UV0;
    vertColor = Color;
    fragPos = Position.xy;
    
    // Передаем экранные координаты для блюра
    screenPos = gl_Position.xy * 0.5 + 0.5;
}