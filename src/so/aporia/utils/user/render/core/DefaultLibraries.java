package so.aporia.utils.user.render.core;

/**
 * Pre-built shader libraries for common GLSL operations.
 */
public final class DefaultLibraries {

    public static final ShaderLibrary KAWASE_BLUR = ShaderLibrary.create("kawase_blur")
            .source("""
                    vec4 kawaseBlur(sampler2D tex, vec2 uv, vec2 texelSize, float offset) {
                        vec4 color = vec4(0.0);
                        color += texture(tex, uv + vec2(-offset, -offset) * texelSize) * 0.25;
                        color += texture(tex, uv + vec2( offset, -offset) * texelSize) * 0.25;
                        color += texture(tex, uv + vec2(-offset,  offset) * texelSize) * 0.25;
                        color += texture(tex, uv + vec2( offset,  offset) * texelSize) * 0.25;
                        return color;
                    }
                    """)
            .uniform("texelSize", "vec2")
            .uniform("offset", "float");

    public static final ShaderLibrary ROUNDED_SDF = ShaderLibrary.create("rounded_sdf")
            .source("""
                    float roundedBoxSDF(vec2 p, vec2 b, float r) {
                        vec2 q = abs(p) - b + vec2(r);
                        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
                    }
                    float roundedBoxStroke(vec2 p, vec2 b, float r, float w) {
                        return roundedBoxSDF(p, b, r) - w * 0.5;
                    }
                    """)
            .uniform("size", "vec2")
            .uniform("radius", "float");

    public static final ShaderLibrary SMOOTH_EDGE = ShaderLibrary.create("smooth_edge")
            .source("""
                    float smoothEdge(float dist, float width) {
                        return 1.0 - smoothstep(-width * 0.5, width * 0.5, dist);
                    }
                    float glowEdge(float dist, float width, float glow) {
                        float inner = 1.0 - smoothstep(-width * 0.5, width * 0.5, dist);
                        float outer = 1.0 - smoothstep(-glow * 0.5, width * 0.5, dist);
                        return max(inner, outer * 0.5);
                    }
                    """)
            .uniform("edgeWidth", "float")
            .uniform("glowRadius", "float");

    public static final ShaderLibrary COLOR_UTIL = ShaderLibrary.create("color_util")
            .source("""
                    vec4 withAlpha(vec4 color, float a) {
                        return vec4(color.rgb, color.a * a);
                    }
                    vec3 rgb2hsv(vec3 c) {
                        vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
                        vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
                        vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
                        float d = q.x - min(q.w, q.y);
                        float e = 1.0e-10;
                        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
                    }
                    vec3 hsv2rgb(vec3 c) {
                        vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
                        vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
                    }
                    """);

    public static void registerAll() {
        ShaderLibraryRegistry r = ShaderLibraryRegistry.INSTANCE;
        r.register(KAWASE_BLUR);
        r.register(ROUNDED_SDF);
        r.register(SMOOTH_EDGE);
        r.register(COLOR_UTIL);
    }

    private DefaultLibraries() {}
}
