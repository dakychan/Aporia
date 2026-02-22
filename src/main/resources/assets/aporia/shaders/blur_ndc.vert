#version 330 core

in vec3 Position;
in vec2 Texture;

out vec2 vUv;

void main() {
    // Прямой проход NDC координат без трансформаций
    gl_Position = vec4(Position, 1.0);
    vUv = Texture;
}
