#version 330

layout(std140) uniform DrawParams {
    vec4 modeParams;   /* x=drawMode  y=glowIntensity  z=glowRadius  w=unused */
    vec4 shapeParams;  /* x=cornerRadius  y=edgeSoftness  z=strokeWidth  w=strokeOnly */
    vec4 colorParams;  /* x=rimPower  y=rimIntensity  z=fresnelPower  w=unused */
    vec4 panelParams;  /* xy=quadCenterXZ  zw=quadHalfSizeXZ */
};

in vec4 vertColor;
in vec3 vertNormal;
in vec3 vertWorldPos;
in vec3 vertViewPos;

out vec4 fragColor;

const vec3 AMBIENT = vec3(0.35);
const vec3 LIGHT_DIR = normalize(vec3(0.5, 1.0, 0.5));
const vec3 LIGHT_COLOR = vec3(0.65);

/* ============ SDF primitives ============ */

float roundedBoxSDF(vec2 p, vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(p - center) - halfSize + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

/* ============ Glow helpers ============ */

vec3 glowColor(vec3 base, float intensity, float dist) {
    float glow = exp(-dist * dist * 8.0) * intensity;
    return base + base * glow * 2.0;
}

float edgeGlow(vec3 normal, vec3 viewDir, float power) {
    float fresnel = 1.0 - abs(dot(normalize(normal), normalize(viewDir)));
    return pow(fresnel, power);
}

/* ============ Lighting ============ */

vec3 calcLighting(vec3 base, vec3 N) {
    if (!gl_FrontFacing) N = -N;
    float diff = max(dot(N, LIGHT_DIR), 0.0);
    float wrap = max(dot(N, LIGHT_DIR) * 0.5 + 0.5, 0.0);
    return base * (AMBIENT + LIGHT_COLOR * mix(diff, wrap, 0.3));
}

/* ============ Main ============ */

void main() {
    int drawMode    = int(modeParams.x);
    float glowInt   = modeParams.y;
    float glowRad   = modeParams.z;

    float cornerR   = shapeParams.x;
    float edgeSoft  = max(shapeParams.y, 0.5);
    float strokeW   = shapeParams.z;
    int   strokeOnly= int(shapeParams.w);

    float rimPower  = colorParams.x;
    float rimIntens = colorParams.y;
    float fresnelP  = colorParams.z;

    vec2 quadCenter  = panelParams.xy;
    vec2 quadHalf    = panelParams.zw;

    vec3 base = vertColor.rgb;
    float alpha = vertColor.a;

    /* ---- MODE 0: Basic lit solid ---- */
    if (drawMode == 0) {
        vec3 lit = calcLighting(base, vertNormal);
        fragColor = vec4(lit, alpha);
        return;
    }

    /* ---- MODE 1: Wireframe glow lines ---- */
    if (drawMode == 1) {
        vec3 viewDir = -vertViewPos;
        float fresnel = edgeGlow(vertNormal, viewDir, fresnelP > 0.0 ? fresnelP : 3.0);
        vec3 glow = glowColor(base, glowInt, 0.0);
        glow += base * fresnel * rimIntens;

        vec3 lit = calcLighting(glow, vertNormal);
        fragColor = vec4(lit, alpha);
        return;
    }

    /* ---- MODE 2: SDF rounded rect panel ---- */
    if (drawMode == 2) {
        /* Use REAL pixel position from vertWorldPos, not vec2(0.0) */
        vec2 pixelPos = vertWorldPos.xy;
        float sdf = roundedBoxSDF(pixelPos, quadCenter, quadHalf, cornerR);

        float mask;
        if (strokeOnly == 1) {
            float outer = 1.0 - smoothstep(-edgeSoft, edgeSoft, sdf);
            float inner = 1.0 - smoothstep(-edgeSoft, edgeSoft, sdf + strokeW);
            mask = outer - inner;
        } else {
            mask = 1.0 - smoothstep(-edgeSoft, edgeSoft, sdf);
        }

        vec3 viewDir = -vertViewPos;
        float fresnel = edgeGlow(vertNormal, viewDir, 2.0);
        vec3 panelColor = base + base * fresnel * 0.15;

        vec3 lit = calcLighting(panelColor, vertNormal);
        fragColor = vec4(lit, alpha * mask);
        return;
    }

    /* ---- MODE 3: Solid + rim glow (ESP boxes) ---- */
    if (drawMode == 3) {
        vec3 viewDir = -vertViewPos;
        float fresnel = edgeGlow(vertNormal, viewDir, rimPower > 0.0 ? rimPower : 2.5);

        vec3 rim = base * fresnel * rimIntens;
        vec3 withRim = base + rim;

        vec3 lit = calcLighting(withRim, vertNormal);
        fragColor = vec4(lit, alpha);
        return;
    }

    /* ---- MODE 4: Unlit solid (no lighting, pure color) ---- */
    if (drawMode == 4) {
        fragColor = vec4(base, alpha);
        return;
    }

    /* ---- MODE 5: Unlit with glow edge ---- */
    if (drawMode == 5) {
        vec3 viewDir = -vertViewPos;
        float fresnel = edgeGlow(vertNormal, viewDir, 2.0);
        vec3 glow = base + base * fresnel * glowInt;
        fragColor = vec4(glow, alpha);
        return;
    }

    /* fallback */
    fragColor = vec4(base, alpha);
}
