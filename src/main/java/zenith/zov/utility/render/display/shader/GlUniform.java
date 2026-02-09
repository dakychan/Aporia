package zenith.zov.utility.render.display.shader;

public final class GlUniform {
    private final GlProgram program;
    private final String name;

    GlUniform() {
        this.program = null;
        this.name = "";
    }

    GlUniform(GlProgram program, String name) {
        this.program = program;
        this.name = name;
    }

    public void set(float v0) {
        if (program != null) {
            program.setUniform(name, v0);
        }
    }

    public void set(float v0, float v1) {
        if (program != null) {
            program.setUniform(name, v0, v1);
        }
    }

    public void set(float v0, float v1, float v2) {
        if (program != null) {
            program.setUniform(name, v0, v1, v2);
        }
    }

    public void set(float v0, float v1, float v2, float v3) {
        if (program != null) {
            program.setUniform(name, v0, v1, v2, v3);
        }
    }

    public void set(int v0) {
        if (program != null) {
            program.setUniform(name, v0);
        }
    }
}
