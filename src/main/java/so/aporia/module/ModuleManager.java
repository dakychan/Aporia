package so.aporia.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for all modules.
 * Handles registration, keybind dispatch, and lifecycle.
 */
public final class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {}

    public void register(Module module) {
        modules.add(module);
    }

    public void registerAll(Module... mods) {
        for (Module m : mods) register(m);
    }

    public Collection<Module> getAll() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category() == category) out.add(m);
        return out;
    }

    public Module get(String name) {
        for (Module m : modules) if (m.name().equalsIgnoreCase(name)) return m;
        return null;
    }
}
