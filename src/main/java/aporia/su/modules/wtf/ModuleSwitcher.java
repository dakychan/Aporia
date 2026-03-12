package aporia.su.modules.wtf;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import aporia.su.util.interfaces.IMinecraft;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.api.EventManager;
import aporia.su.util.events.impl.entity.KeyEvent;
import aporia.su.modules.module.ModuleStructure;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleSwitcher implements IMinecraft {
    List<ModuleStructure> moduleStructures;

    public ModuleSwitcher(List<ModuleStructure> moduleStructures, EventManager eventManager) {
        this.moduleStructures = moduleStructures;
        eventManager.register(this);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        for (ModuleStructure moduleStructure : moduleStructures) {
            if (event.key() == moduleStructure.getKey() && mc.currentScreen == null) {
                try {
                    handleModuleState(moduleStructure, event.action());
                } catch (Exception e) {
//                    handleException(module.getName(), e);
                }
            }
        }
    }

    private void handleModuleState(ModuleStructure moduleStructure, int action) {
        if (moduleStructure.getType() == 1 && action == 1) {
            moduleStructure.switchState();
        }
    }
}
