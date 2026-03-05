package aporia.su.util.events.impl.ui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import aporia.su.util.events.api.events.Event;

@Getter
@Setter
public class HotbarItemRenderEvent implements Event {
    private ItemStack stack;
    private final int hotbarIndex;

    public HotbarItemRenderEvent(ItemStack stack, int hotbarIndex) {
        this.stack = stack;
        this.hotbarIndex = hotbarIndex;
    }
}