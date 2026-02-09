package zenith.zov.client.screens.autobuy.items;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import zenith.zov.base.autobuy.enchantes.minecraft.EnchantVanilla;
import zenith.zov.base.autobuy.item.ItemBuy;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;
import zenith.zov.client.screens.menu.settings.impl.MenuBooleanSetting;
import zenith.zov.client.screens.menu.settings.impl.MenuSliderSetting;

import java.util.List;

@Getter
@Setter
public class AutoInventoryElytra extends ExtendAutoInventoryItem {
    private EnchantVanilla enchantVanilla =new EnchantVanilla("Прочность","minecraft:unbreaking",0);
    private EnchantVanilla meding =new EnchantVanilla("Починка","minecraft:mending",1);
    private MenuSliderSetting maxDamage = new MenuSliderSetting(new NumberSetting("Макс процент поломки",0.8f,0,1,0.1f));

    private MenuSliderSetting unBrekingSetting = new MenuSliderSetting(new NumberSetting("Прочность лвл",0,0,8,1,(oldValue, newValue) ->
    {
        enchantVanilla.setMinLevel((int) newValue);
    }));
    private MenuBooleanSetting meddingState = new MenuBooleanSetting(new BooleanSetting("Починка",false));
    public AutoInventoryElytra(ItemBuy itemBuy) {

        super(itemBuy);

    }

    @Override
    public AutoInventoryElytra copy() {
        AutoInventoryElytra autoInventoryElytra = new AutoInventoryElytra(this.getItemBuy());
        autoInventoryElytra.enchantVanilla = new EnchantVanilla(enchantVanilla.getName(),enchantVanilla.getChecked(),enchantVanilla.getMinLevel());
        autoInventoryElytra.meding = new EnchantVanilla(meding.getName(),meding.getChecked(),meding.getMinLevel());
        autoInventoryElytra.maxDamage.getSetting().setCurrent(this.maxDamage.getSetting().getCurrent());
        autoInventoryElytra.unBrekingSetting.getSetting().setCurrent(this.unBrekingSetting.getSetting().getCurrent());
        return autoInventoryElytra;
    }

    @Override
    public List<MenuSetting> getEnchants() {
        return List.of(maxDamage,unBrekingSetting,meddingState);
    }

    @Override
    public boolean isBuy(ItemStack stack) {

        if(! super.isBuy(stack) ||getDurabilityPercent(stack)<maxDamage.getSetting().getCurrent()) return false;


        return enchantVanilla.isEnchanted(stack) && (meddingState.getSetting().isEnabled()||meding.isEnchanted(stack));
    }
    private float getDurabilityPercent(ItemStack stack) {
        int damage = stack.getDamage();
        int maxDamage = stack.getMaxDamage();

        return (float)(maxDamage - damage) / maxDamage;
    }

    @Override
    public void load(JsonObject obj) {

        super.load(obj);


        if (obj.has("maxDamage")) {
            float v = obj.get("maxDamage").getAsFloat();
            this.maxDamage.getSetting().setCurrent(v);
        }

        if (obj.has("unbreakingLevel")) {
            float lvl = obj.get("unbreakingLevel").getAsFloat();
            this.unBrekingSetting.getSetting().setCurrent(lvl);
            this.enchantVanilla.setMinLevel((int) lvl);
        }

        if (obj.has("mendingEnabled")) {
            boolean enabled = obj.get("mendingEnabled").getAsBoolean();
            this.meddingState.getSetting().setEnabled(enabled);
        }


        if (obj.has("unbreaking")) {
            JsonObject unb = obj.getAsJsonObject("unbreaking");
            if (unb.has("minLevel")) {
                int min = unb.get("minLevel").getAsInt();
                this.enchantVanilla.setMinLevel(min);
                this.unBrekingSetting.getSetting().setCurrent(min);
            }
        }


        if (obj.has("mending")) {
            JsonObject mend = obj.getAsJsonObject("mending");
            if (mend.has("minLevel")) {
                this.meding.setMinLevel(mend.get("minLevel").getAsInt());
            }
        }
    }

    @Override
    public JsonObject save() {
        JsonObject obj = super.save();


        obj.addProperty("maxDamage", this.maxDamage.getSetting().getCurrent());
        obj.addProperty("unbreakingLevel", this.unBrekingSetting.getSetting().getCurrent());
        obj.addProperty("mendingEnabled", this.meddingState.getSetting().isEnabled());


        JsonObject unb = new JsonObject();
        unb.addProperty("name", this.enchantVanilla.getName());
        unb.addProperty("minLevel", this.enchantVanilla.getMinLevel());
        obj.add("unbreaking", unb);


        JsonObject mend = new JsonObject();
        mend.addProperty("name", this.meding.getName());
        mend.addProperty("minLevel", this.meding.getMinLevel());
        obj.add("mending", mend);

        return obj;
    }
}
