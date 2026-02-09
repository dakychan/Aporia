#version 150

#moj_import <dynamictransforms.glsl>
#moj_import <zenith:uniforms.glsl>

in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;

uniform sampler2D Sampler0;

out vec4 OutColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    vec4 sample = texture(Sampler0, TexCoord);
    float msdfDist = median(sample.rgb);
    float mtsdfDist = mix(msdfDist, sample.a, 0.28);
    float dist = mtsdfDist - 0.5 + Thickness;
    vec2 unitRange = vec2(Range) / vec2(textureSize(Sampler0, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(TexCoord);
    float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
    float alpha = smoothstep(-Smoothness.x, Smoothness.x, dist * screenPxRange);
    alpha = pow(alpha, 0.8);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (Outline > 0.5) {
        color = mix(OutlineColor, FragColor, alpha);
        color.a *= smoothstep(-Smoothness.x, Smoothness.x, (dist + OutlineThickness) * screenPxRange);
    }

    if (EnableFadeout > 0.5) {
        float fadeAlpha = 1.0;
        float relativeX = GlobalPos.x - TextPosX;
        float normalizedX = relativeX / MaxWidth;
        if (normalizedX > FadeoutStart) {
            fadeAlpha = 1.0 - smoothstep(FadeoutStart, FadeoutEnd, normalizedX);
        }
        color.a *= fadeAlpha;
    }

    OutColor = color * ColorModulator;
}
