#version 330

layout(std140) uniform ModelViewProj {
    mat4 ModelViewProjMat;
    mat4 ModelMat;
    mat4 NormalMat;
};

layout(std140) uniform DrawParams {
    vec4 modeParams;   /* x=drawMode  y=glowIntensity  z=glowRadius  w=unused */
    vec4 shapeParams;  /* x=cornerRadius  y=edgeSoftness  z=strokeWidth  w=strokeOnly */
    vec4 colorParams;  /* x=rimPower  y=rimIntensity  z=fresnelPower  w=unused */
    vec4 panelParams;  /* xy=centerXZ  zw=halfSizeXZ  (for SDF in world XY) */
};

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec4 vertColor;
out vec3 vertNormal;
out vec3 vertWorldPos;
out vec3 vertViewPos;

void main() {
    vec4 worldPos = ModelMat * vec4(Position, 1.0);
    vertWorldPos = worldPos.xyz;

    vec4 viewPos = ModelViewProjMat * vec4(Position, 1.0);
    vertViewPos = viewPos.xyz;

    vertNormal = normalize(mat3(NormalMat) * Normal);
    vertColor = Color;
    gl_Position = viewPos;
}
