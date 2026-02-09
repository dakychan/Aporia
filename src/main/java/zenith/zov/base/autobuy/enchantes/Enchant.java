package zenith.zov.base.autobuy.enchantes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;

@AllArgsConstructor
@Getter
@Setter
public abstract class Enchant {
    protected final String name;
    protected final String checked;
    protected int minLevel;
    public abstract boolean isEnchanted(ItemStack stack) ;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [checked=" + checked + ", level=" + minLevel + "]";
    }
}
