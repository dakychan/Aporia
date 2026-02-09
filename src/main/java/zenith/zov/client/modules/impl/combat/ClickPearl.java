package zenith.zov.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;


import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.utility.game.other.InventoryUtil;
import zenith.zov.utility.game.other.NetworkUtils;
import zenith.zov.utility.game.player.PlayerInventoryUtil;

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
