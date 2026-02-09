package zenith.zov.base.autobuy.item;

import net.minecraft.item.ItemStack;
import zenith.zov.base.autobuy.enchantes.Enchant;

import java.util.ArrayList;

public class EnchantItemBuy extends ItemBuy {

    protected final ArrayList<Enchant> enchants = new ArrayList<>();

    public EnchantItemBuy(ItemStack itemStack, String displayName, String searchName, Category maxSumBuy) {
        super(itemStack, displayName, searchName, maxSumBuy);
    }
    public EnchantItemBuy(ItemStack itemStack, String searchName, Category maxSumBuy) {
        super(itemStack, searchName, maxSumBuy);
    }


    public boolean isBuy(ItemStack stack) {
        if(!super.isBuy(stack)) return false;

        for(Enchant enchant : enchants) {
            if(!enchant.isEnchanted(stack)){
                return false;
            }
        }

        return true;
    }
    public void addEnchant(Enchant enchant) {
        enchants.add(enchant);
    }
    
    public ArrayList<Enchant> getEnchants() {
        return enchants;
    }
}
