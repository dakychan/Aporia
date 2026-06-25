package so.aporia.utils.user.render.core;

import java.util.*;

/**
 * Registry for shader libraries — reusable GLSL code blocks.
 * Register libraries once, include them in any shader.
 */
public class ShaderLibraryRegistry {

    public static final ShaderLibraryRegistry INSTANCE = new ShaderLibraryRegistry();

    private final Map<String, ShaderLibrary> libraries = new LinkedHashMap<>();

    public ShaderLibrary register(ShaderLibrary library) {
        libraries.put(library.getName(), library);
        return library;
    }

    public Optional<ShaderLibrary> get(String name) {
        return Optional.ofNullable(libraries.get(name));
    }

    public ShaderLibrary require(String name) {
        ShaderLibrary lib = libraries.get(name);
        if (lib == null) throw new IllegalArgumentException("Library not found: " + name);
        return lib;
    }

    /**
     * Combines multiple libraries into one GLSL include block.
     */
    public String include(String... names) {
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            sb.append(require(name).include());
            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean has(String name) {
        return libraries.containsKey(name);
    }

    public Map<String, ShaderLibrary> all() {
        return Map.copyOf(libraries);
    }

    public void clear() {
        libraries.clear();
    }
}
