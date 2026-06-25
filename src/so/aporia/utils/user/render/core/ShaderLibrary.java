package so.aporia.utils.user.render.core;

import java.util.*;

/**
 * Reusable GLSL code fragment that can be #included into shaders.
 * Acts as a shader "plugin" — register once, use in many shaders.
 *
 * Usage:
 *   ShaderLibrary blurLib = ShaderLibrary.create("blur")
 *       .source("""
 *           float kawaseBlur(sampler2D tex, vec2 uv, vec2 texelSize, float offset) {
 *               vec4 color = vec4(0.0);
 *               color += texture(tex, uv + vec2(-offset, -offset) * texelSize) * 0.25;
 *               color += texture(tex, uv + vec2( offset, -offset) * texelSize) * 0.25;
 *               color += texture(tex, uv + vec2(-offset,  offset) * texelSize) * 0.25;
 *               color += texture(tex, uv + vec2( offset,  offset) * texelSize) * 0.25;
 *               return color;
 *           }
 *       """)
 *       .uniform("texelSize", "vec2")
 *       .uniform("offset", "float");
 *
 *   // Include in shader source
 *   String fragmentSource = blurLib.include() + myFragmentCode;
 */
public class ShaderLibrary {

    private String name;
    private String glslSource;
    private final List<UniformDecl> uniforms = new ArrayList<>();

    private ShaderLibrary() {}

    public static ShaderLibrary create(String name) {
        ShaderLibrary lib = new ShaderLibrary();
        lib.name = name;
        return lib;
    }

    public ShaderLibrary source(String glsl) {
        this.glslSource = glsl;
        return this;
    }

    public ShaderLibrary uniform(String name, String glslType) {
        this.uniforms.add(new UniformDecl(name, glslType));
        return this;
    }

    /**
     * Returns GLSL uniform declarations for inclusion in a shader.
     */
    public String uniformDeclarations() {
        StringBuilder sb = new StringBuilder();
        sb.append("// -- ").append(name).append(" uniforms --\n");
        for (UniformDecl u : uniforms) {
            sb.append("uniform ").append(u.glslType).append(" ").append(u.name).append(";\n");
        }
        return sb.toString();
    }

    /**
     * Returns full GLSL source for inclusion in a shader (uniforms + functions).
     */
    public String include() {
        StringBuilder sb = new StringBuilder();
        sb.append("// -- library: ").append(name).append(" --\n");
        sb.append(uniformDeclarations());
        if (glslSource != null) {
            sb.append(glslSource);
        }
        sb.append("// -- end ").append(name).append(" --\n");
        return sb.toString();
    }

    public String getName() { return name; }
    public String getGlslSource() { return glslSource; }
    public List<UniformDecl> getUniforms() { return Collections.unmodifiableList(uniforms); }

    public static class UniformDecl {
        public final String name;
        public final String glslType;

        public UniformDecl(String name, String glslType) {
            this.name = name;
            this.glslType = glslType;
        }
    }
}
