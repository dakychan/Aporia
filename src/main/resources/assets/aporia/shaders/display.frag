#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    fragColor = texture(Sampler0, texCoord);
}
