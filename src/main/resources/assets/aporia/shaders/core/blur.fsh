#version 150

uniform sampler2D InputTexture;

in vec4 FragColor;
in vec2 TexCoord;
out vec4 OutColor;

void main() {
    // Sample screen texture with simple blur
    vec2 texelSize = 1.0 / vec2(textureSize(InputTexture, 0));
    vec4 color = vec4(0.0);
    
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize * 5.0;
            color += texture(InputTexture, TexCoord + offset);
        }
    }
    color /= 25.0;
    
    OutColor = color;
}
