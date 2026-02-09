#version 150

#moj_import <dynamictransforms.glsl>
#moj_import <zenith:uniforms.glsl>
#moj_import <zenith:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    vec2 pixel = FragCoord.xy * Size;

    if (CornerIndex == 0.0) { // Top-Left
        if (pixel.x > center.x || pixel.y > center.y) discard;
    } else if (CornerIndex == 1.0) { // Top-Right
        if (pixel.x < center.x || pixel.y > center.y) discard;
    } else if (CornerIndex == 2.0) { // Bottom-Left
        if (pixel.x > center.x || pixel.y < center.y) discard;
    } else if (CornerIndex == 3.0) { // Bottom-Right
        if (pixel.x < center.x || pixel.y < center.y) discard;
    }
    float distance = roundedBoxSDF(center - (FragCoord * Size), center - 1.0, Radius);

    float alpha = smoothstep(1.0 - Thickness - Smoothness.x - Smoothness.y,
        1.0 - Thickness - Smoothness.y, distance); // internal edge
    alpha *= 1.0 - smoothstep(1.0 - Smoothness.y, 1.0, distance); // external edge

    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);

    if (finalColor.a == 0.0) { // alpha test
        discard;
    }

    OutColor = finalColor * ColorModulator;
}
