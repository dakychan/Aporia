#version 150

uniform sampler2D InputTexture;

layout(std140) uniform PostData {
    float Saturation; // 0=grayscale, 0.5=normal, 1.0=enhanced
    float _pad0;
    float _pad1;
    float _pad2;
};

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec4 c = texture(InputTexture, TexCoord);

    // luminance (perceptual weights)
    float luma = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 gray  = vec3(luma);

    // 0.0 → grayscale, 0.5 → original, 1.0 → boosted (+50% saturation)
    float factor = Saturation * 2.0; // remap: 0→0, 0.5→1.0, 1.0→2.0
    vec3 result  = mix(gray, c.rgb, factor);

    // subtle contrast lift at high saturation to avoid washed-out look
    result = mix(result, pow(max(result, 0.0), vec3(0.95)), max(factor - 1.0, 0.0) * 0.3);

    OutColor = vec4(result, c.a);
}
