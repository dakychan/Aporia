#version 330

uniform sampler2D InputTexture;
in vec2 fragUv;
out vec4 fragColor;

void main() {
    fragColor = texture(InputTexture, fragUv);
}
