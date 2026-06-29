#version 330

uniform sampler2D BlurTextureSampler;

layout(std140) uniform ShapeData {
    vec4 bounds;
    vec4 params;
    vec4 params2;
    vec4 screen;
};

in vec2 uv;
in vec2 fragPos;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(p - center) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    int   mode   = int(params.z);
    float radius = params.x;
    float distortionPower = params2.z == 0.0 ? 0.012 : clamp(params2.z * 0.02, 0.0, 0.025);

    vec2 idealFragPos = bounds.xy + uv * bounds.zw;
    vec2 center   = bounds.xy + bounds.zw * 0.5;
    vec2 halfSize = bounds.zw * 0.5;

    float sdfVal = roundedBoxSDF(idealFragPos, center, halfSize, radius);
    float sdfMask = step(0.0, -sdfVal); 

    if (sdfMask <= 0.005) {
        discard;
    }

    vec2 distortionVector = uv - vec2(0.5);
    float edgeFactor = smoothstep(-12.0, 0.0, sdfVal);
    vec2 finalDistortion = distortionVector * edgeFactor * distortionPower;

    vec2 screenUv = gl_FragCoord.xy / screen.xy;
    vec2 distortedScreenUv = screenUv + finalDistortion;
    distortedScreenUv = clamp(distortedScreenUv, vec2(0.005), vec2(0.995));

    float chromaShift = edgeFactor * 0.002; 
    float blurOffset = 0.0015; 
    
    float blurR = texture(BlurTextureSampler, distortedScreenUv + vec2(chromaShift, 0.0)).r;
    float blurG = texture(BlurTextureSampler, distortedScreenUv).g;
    float blurB = texture(BlurTextureSampler, distortedScreenUv - vec2(chromaShift, 0.0)).b;
    
    blurR += texture(BlurTextureSampler, distortedScreenUv + vec2(chromaShift + blurOffset, blurOffset)).r;
    blurG += texture(BlurTextureSampler, distortedScreenUv + vec2(blurOffset, blurOffset)).g;
    blurB += texture(BlurTextureSampler, distortedScreenUv + vec2(-chromaShift + blurOffset, blurOffset)).b;
    
    blurR += texture(BlurTextureSampler, distortedScreenUv + vec2(chromaShift - blurOffset, -blurOffset)).r;
    blurG += texture(BlurTextureSampler, distortedScreenUv + vec2(-blurOffset, -blurOffset)).g;
    blurB += texture(BlurTextureSampler, distortedScreenUv + vec2(-chromaShift - blurOffset, -blurOffset)).b;

    vec3 glassBase = vec3(blurR, blurG, blurB) / 3.0;

    glassBase = mix(glassBase, vec3(0.02, 0.03, 0.05), 0.06);

    float correctedY = 1.0 - uv.y;
    float bottomVolume = smoothstep(0.1, 1.0, correctedY) * 0.03;
    glassBase *= (1.0 - bottomVolume);

    float borderThickness = 1.5;
    float borderAlpha = smoothstep(-borderThickness, 0.0, sdfVal) * smoothstep(-borderThickness * 2.0, -borderThickness, sdfVal);
    vec3 borderColor = vec3(1.0, 1.0, 1.0);
    glassBase = mix(glassBase, borderColor, borderAlpha * 0.15);

    fragColor = vec4(glassBase, sdfMask);
}
