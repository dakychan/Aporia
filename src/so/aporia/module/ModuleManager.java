/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module;

import so.aporia.module.impl.combat.Aura;
import so.aporia.module.impl.misc.AutoConfig;
import so.aporia.module.impl.misc.AutoEZ;
import so.aporia.module.impl.misc.ClickGui;
import so.aporia.module.impl.misc.DiscordRPCModule;
import so.aporia.module.impl.misc.PacketDebug;
import so.aporia.module.impl.misc.ServerHelper;
import so.aporia.module.impl.misc.TestModule;
import so.aporia.module.impl.move.AutoSprint;
import so.aporia.module.impl.render.Beautifully;
import so.aporia.module.impl.render.Hud;
import so.aporia.module.impl.render.PlayerESP;

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

    private ModuleManager() {
        registerAll(
                // render
            new Hud(),
            new Beautifully(),
            new PlayerESP(),
            new ClickGui(),
                // misc
            new ServerHelper(),
            new AutoConfig(),
            new AutoEZ(),
            new TestModule(),
            new PacketDebug(),
            new DiscordRPCModule(),
                // move
            new AutoSprint(),
                // combat
            new Aura()
        );
    }

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
