#version 150

#moj_import <dynamictransforms.glsl>
#moj_import <projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 TexCoord;
out vec4 FragColor;
out vec2 GlobalPos;

void main() {
    TexCoord = UV0;
    FragColor = Color;
    GlobalPos = Position.xy;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
