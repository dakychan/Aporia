package zenith.zov.utility.mixin.accessors;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DrawContext.class)
public interface DrawContextAccessor {

    @Invoker("drawItemBar")
    void callDrawItemBar(ItemStack stack, int x, int y);
    @Invoker("drawCooldownProgress")
    void callDrawCooldownProgress(ItemStack stack, int x, int y);
}
