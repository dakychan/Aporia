package aporia.su.util.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import aporia.su.util.events.api.events.Event;
import aporia.su.modules.module.ModuleStructure;

@Getter
@AllArgsConstructor
public class ModuleToggleEvent implements Event {
    private final ModuleStructure module;
    private final boolean enabled;
}