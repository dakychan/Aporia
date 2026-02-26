package cc.apr.event.impl;

import cc.apr.event.api.Event;
import cc.apr.module.Module;

public class ModuleToggleEvent extends Event {
    public final Module module;
    public final boolean enabled;

    public ModuleToggleEvent(Module module, boolean enabled) {
        this.module = module;
        this.enabled = enabled;
    }
}
