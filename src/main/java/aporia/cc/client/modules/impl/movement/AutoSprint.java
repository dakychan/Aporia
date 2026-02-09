package aporia.cc.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;


import aporia.cc.base.events.impl.player.EventUpdate;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoSprint", category = Category.MOVEMENT, description = "Автоматически включает спринт")
public final class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();
    private AutoSprint() {
    }
    @EventTarget
    public void onUpdate(EventUpdate event) {
        mc.options.sprintKey.setPressed(true);
    }
}
