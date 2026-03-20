#version 150

in vec3 Position;
in vec4 Color;

out vec4 FragColor;
out vec2 TexCoord;

void main() {
    gl_Position = vec4(Position, 1.0);
    FragColor = Color;
    // Convert NDC to UV for screen texture
    TexCoord = vec2(Position.x * 0.5 + 0.5, Position.y * 0.5 + 0.5);
}
