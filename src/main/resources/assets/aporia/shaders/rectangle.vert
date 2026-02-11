#version auto

in vec4 pos;
in vec4 color;
in vec2 _rectPosition;
in vec2 _size;
in float _radius;

#include<matrices>

out vec4 vertexColor;
out vec2 fragCoord;
out vec2 rectPosition;
out vec2 rectSize;
out float cornerRadius;

void main() {
    gl_Position = projMat * modelViewMat * pos;
    vertexColor = color;
    fragCoord = pos.xy;
    rectPosition = _rectPosition;
    rectSize = _size;
    cornerRadius = _radius;
}
