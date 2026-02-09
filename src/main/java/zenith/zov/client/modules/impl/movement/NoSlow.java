package zenith.zov.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;


import net.minecraft.util.Hand;
import zenith.zov.base.events.impl.player.EventSlowWalking;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;

@ModuleAnnotation(name = "NoSlow", category = Category.MOVEMENT, description = "Убирает замедление во время еды")
public final class NoSlow extends Module {
    public static final NoSlow INSTANCE = new NoSlow();

    private NoSlow() {
    }

    private final ModeSetting mode = new ModeSetting("Мод");
    private final ModeSetting.Value grimNew = new ModeSetting.Value(mode, "Grim New");
    private final ModeSetting.Value hw = new ModeSetting.Value(mode, "Grim old").select();
    private BooleanSetting sprint = new BooleanSetting("Спринт",true, hw::isSelected);
    private  int ticks = 0;
    @EventTarget
    public void onItemUse(EventSlowWalking e) {
        if(grimNew.isSelected()){
            if(mc.player.getItemUseTime() %2==0){
                e.setCancelled(true);
            }
        }
        if(hw.isSelected()){
            Hand hand = mc.player.getActiveHand();
            if(sprint.isEnabled()){
                mc.player.setSprinting(mc.player.canSprint()
                        && mc.player.input.getMovementInput().lengthSquared() > 0
                        && !mc.player.isBlind()
                        && !mc.player.isGliding()
                        && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater()));
            }
            PlayerIntersectionUtil.useItem(hand.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND);
            e.setCancelled(true);
        }

    }

//  так называемы мега зако обход
    @EventTarget
    public void update(EventUpdate tickEvent) {
        if (mc.player.isUsingItem() &&mc.player.isOnGround()) {
//           mc.player.setSprinting(true);
//           mc.player.sendSprintingPacket();
//           mc.player.jump();
         //  ticks = 1;
        }else {
            ticks=0;
        }
    }
}


