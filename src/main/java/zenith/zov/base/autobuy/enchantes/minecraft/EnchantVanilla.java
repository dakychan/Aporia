package zenith.zov.base.autobuy.enchantes.minecraft;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import zenith.zov.base.autobuy.enchantes.Enchant;

public class EnchantVanilla extends Enchant {


    public EnchantVanilla(String name,String checked, int minLevel) {
        super(name,checked, minLevel);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        if(minLevel<=0) return true;
        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        for (var entry : enchants.getEnchantmentEntries()) {
            String id = entry.getKey().getKey().toString();

            if (id.contains(checked)) {

                return entry.getIntValue()>= minLevel;
            }
        }

        return false;
    }
}
