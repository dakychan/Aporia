package zenith.zov.utility.mixin.accessors;

import net.minecraft.component.MergedComponentMap;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
@Mixin(ItemStack.class)
public interface ItemStackAccessor {

    @Accessor("components")
    MergedComponentMap getComponents();
}
