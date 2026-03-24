#version 150

uniform sampler2D InputTexture;

layout(std140) uniform BlurData {
    vec2  Resolution;
    float Strength;
    float Direction;
    float Saturation;
    float _pad0;
    float _pad1;
    float _pad2;
};

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 texelSize = 1.0 / Resolution;
    float sigma = max(Strength, 0.5);
    vec2 dir = Direction < 0.5 ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    float w[5];
    w[0] = 0.2270270270;
    w[1] = 0.1945945946;
    w[2] = 0.1216216216;
    w[3] = 0.0540540541;
    w[4] = 0.0162162162;
    vec4 color = texture(InputTexture, TexCoord) * w[0];
    for (int i = 1; i <= 4; i++) {
        float offset = float(i) * sigma * 0.5;
        color += texture(InputTexture, TexCoord + dir * texelSize * offset) * w[i];
        color += texture(InputTexture, TexCoord - dir * texelSize * offset) * w[i];
    }
    OutColor = vec4(color.rgb, 1.0);
    if (Direction < 0.5) {
        float luma   = dot(OutColor.rgb, vec3(0.2126, 0.7152, 0.0722));
        float factor = Saturation * 2.0; // 0→0, 0.5→1.0(normal), 1.0→2.0
        OutColor.rgb = mix(vec3(luma), OutColor.rgb, factor);
    }
}