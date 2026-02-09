package zenith.zov.client.modules.impl.player;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import com.darkmagician6.eventapi.EventTarget;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "NoDelay", category = Category.PLAYER, description = "Убирает задержку при использовании предметов")
public final class NoDelay extends Module {
    public static final NoDelay INSTANCE = new NoDelay();
    
    private NoDelay() {

    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.useKey.isPressed()) return;

        Hand activeHand = mc.player.getActiveHand();
        if (activeHand != null) {
            mc.interactionManager.interactItem(mc.player, activeHand);
        }
    }
}
