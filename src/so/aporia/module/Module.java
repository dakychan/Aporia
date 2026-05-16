/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module;

/**
 * Base class for all Aporia modules.
 * <p>
 * Subclass, annotate event handlers with {@link so.aporia.utils.events.EventHandler},
 * and register via {@link ModuleManager}.
 */
public abstract class Module {

    /** Display name shown in UI. */
    private final String name;

    /** Category this module belongs to. */
    private final Category category;

    /** GLFW key code for toggle keybind, or -1 if none. */
    private int keybind;

    /** Whether the module is currently active. */
    private boolean enabled = false;

    protected Module(String name, Category category, int keybind) {
        this.name     = name;
        this.category = category;
        this.keybind  = keybind;
    }

    protected Module(String name, Category category) {
        this(name, category, -1);
    }

    /** Called when the module is toggled ON. Register event listeners here. */
    protected void onEnable() {}

    /** Called when the module is toggled OFF. Unregister event listeners here. */
    protected void onDisable() {}

    public final void enable() {
        if (enabled) return;
        enabled = true;
        so.aporia.utils.files.impl.ConfigFile.markModuleActivated(this);
        onEnable();
    }

    public final void disable() {
        if (!enabled) return;
        enabled = false;
        onDisable();
    }

    public final void toggle() {
        if (enabled) disable(); else enable();
    }

    public String   name()     { return name; }
    public Category category() { return category; }
    public boolean  isEnabled(){ return enabled; }
    public int      keybind()  { return keybind; }
    public void     setKeybind(int key) { this.keybind = key; }
}
