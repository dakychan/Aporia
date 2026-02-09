package aporia.cc.client.modules.impl.combat;

import net.minecraft.item.Items;


import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(name = "ClickPearl", category = Category.COMBAT,description = "Кидает перл если он не в руках")
public final class ClickPearl extends Module {
    public static final ClickPearl INSTANCE = new ClickPearl();
    private ClickPearl() {
    }

    @Override
    public void onEnable() {
        PlayerInventoryUtil.swapAndUse(Items.ENDER_PEARL);
        super.onEnable();
        this.toggle();
    }
}

