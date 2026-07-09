#version 150

in vec2 texCoord;
out vec2 v_texCoord;

void main() {
    v_texCoord = texCoord;
    gl_Position = vec4(texCoord * 2.0 - 1.0, 0.0, 1.0);
}
