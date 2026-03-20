#version 330

in vec2 Position;
in vec2 UV0;
out vec2 fragUv;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
    fragUv = UV0;
}
