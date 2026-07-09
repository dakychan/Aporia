#version 330

uniform sampler2D LogoTextureSampler;

layout(std140) uniform u_time {
    float time;
};

layout(std140) uniform LogoData {
    vec4 bounds;
    vec4 params;
};

in vec2 uv;
in vec4 vertColor;
in vec2 fragPos;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(LogoTextureSampler, uv);
    float pulse = 0.95 + 0.05 * sin(time * 2.0);
    vec4 color = texColor * vertColor * pulse;
    float alpha = texColor.a * vertColor.a;
    fragColor = vec4(color.rgb, alpha);
}
