package ru.event.impl;

import ru.event.api.Event;
import ru.module.Module;

/**
 * Событие переключения модуля
 */
public class ModuleToggleEvent extends Event {
    public final Module module;
    public final boolean enabled;

    public ModuleToggleEvent(Module module, boolean enabled) {
        this.module = module;
        this.enabled = enabled;
    }
}
