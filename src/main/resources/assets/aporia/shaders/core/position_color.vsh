#version 150

#moj_import <dynamictransforms.glsl>
#moj_import <projection.glsl>
#moj_import <zenith:common.glsl>

in vec3 Position; // POSITION_COLOR vertex attributes
in vec4 Color;

out vec2 FragCoord;
out vec4 FragColor;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
