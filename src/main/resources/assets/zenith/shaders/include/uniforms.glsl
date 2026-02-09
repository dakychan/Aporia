#version 150

layout(std140) uniform ZenithUniforms {
    vec2 Size;
    vec4 Radius;
    vec2 Smoothness;
    float CornerSmoothness;
    float Thickness;
    float CornerIndex;
    float Progress;
    float Fade;
    float StripeWidth;
    vec4 TopLeftColor;
    vec4 BottomLeftColor;
    vec4 TopRightColor;
    vec4 BottomRightColor;
    float BlurRadius;
    vec2 Resolution;
    float Offset;
    float Saturation;
    float TintIntensity;
    vec3 TintColor;
    float Range;
    float Outline;
    float OutlineThickness;
    vec4 OutlineColor;
    float EnableFadeout;
    float FadeoutStart;
    float FadeoutEnd;
    float MaxWidth;
    float TextPosX;
};
