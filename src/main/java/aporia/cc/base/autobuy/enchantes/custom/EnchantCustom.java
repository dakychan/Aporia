package aporia.cc.base.autobuy.enchantes.custom;



import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import aporia.cc.base.autobuy.enchantes.Enchant;

public class EnchantCustom extends Enchant {


    public EnchantCustom(String name,String checked, int minLevel) {
        super(name,checked, minLevel);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtList customEnchants = customData.getNbt().getListOrEmpty("custom-enchantments");

            for (int i = 0; i < customEnchants.size(); i++) {
                NbtCompound ench = customEnchants.getCompoundOrEmpty(i);
                String type = ench.getString("type", "");
                int level = ench.getInt("level", 0);
                if (type.equals(this.checked)) {
                    return level >= minLevel;
                }
            }
        }

        return false;
    }
}
