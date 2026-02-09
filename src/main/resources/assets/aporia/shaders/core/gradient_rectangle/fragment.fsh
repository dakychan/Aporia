#version 150

#moj_import <dynamictransforms.glsl>
#moj_import <zenith:uniforms.glsl>
#moj_import <zenith:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

out vec4 OutColor;

vec4 bilinearInterpolation(vec2 uv) {
    vec4 topColor = mix(TopLeftColor, TopRightColor, uv.x);
    vec4 bottomColor = mix(BottomLeftColor, BottomRightColor, uv.x);
    return mix(topColor, bottomColor, uv.y);
}

void main() {
    vec2 center = Size * 0.5;
    vec2 uv = FragCoord;

    vec4 gradientColor = bilinearInterpolation(uv);

    float distance = roundedBoxSDF(center - (FragCoord * Size), center - 1.0, Radius);
    float alpha = 1.0 - smoothstep(1.0 - Smoothness.x, 1.0, distance);

    vec4 finalColor = vec4(gradientColor.rgb, gradientColor.a * alpha);

    if (finalColor.a == 0.0) { // alpha test
        discard;
    }

    OutColor = finalColor * ColorModulator;
}
