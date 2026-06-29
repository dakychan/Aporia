#version 330

in vec4 vertColor;
in vec3 vertNormal;
in vec3 vertWorldPos;

out vec4 fragColor;

const vec3 AMBIENT = vec3(0.3);
const vec3 LIGHT_DIR = normalize(vec3(0.5, 1.0, 0.5));
const vec3 LIGHT_COLOR = vec3(0.7);

void main() {
    vec3 base = vertColor.rgb;
    float alpha = vertColor.a;

#ifndef NO_LIGHTING
    vec3 N = normalize(vertNormal);
    if (!gl_FrontFacing) N = -N;
    float diff = max(dot(N, LIGHT_DIR), 0.0);
    base *= (AMBIENT + LIGHT_COLOR * diff);
#endif

    fragColor = vec4(base, alpha);
}
