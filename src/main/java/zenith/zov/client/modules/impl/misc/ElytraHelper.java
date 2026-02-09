package zenith.zov.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import zenith.zov.base.events.impl.input.EventKey;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.client.modules.impl.movement.ElytraRecast;
import zenith.zov.utility.game.player.MovingUtil;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.player.PlayerInventoryUtil;


import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
 @ModuleAnnotation(name = "ElytraHelper", description = "Помощник для элитр",category = Category.MISC)
public final class ElytraHelper extends Module {
    public static final ElytraHelper INSTANCE = new ElytraHelper();
    private final KeySetting elytraSetting = new KeySetting("Кнопка свапа");
    private final  KeySetting fireworkSetting = new KeySetting("Кнопка фейерверка");
    private final  BooleanSetting startSetting = new BooleanSetting("Авто взлет", true);
    private final  BooleanSetting quicSetting = new BooleanSetting("Быстрый взлет",false, startSetting::isVisible);

    private ElytraHelper() {


    }


    @EventTarget
    public void onKey(EventKey e) {

        if (e.isKeyDown(elytraSetting.getKeyCode())) {
            Slot slot = chestPlate();
            if (slot != null) {

                PlayerInventoryUtil.moveItem(slot, 6, true);

            }
        } else if (e.isKeyDown(fireworkSetting.getKeyCode()) && mc.player.isGliding()) {
            PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
        }
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && !mc.player.isTouchingWater()) {

            if(!ElytraRecast.INSTANCE.isEnabled() ||(  (mc.player.isUsingItem()|| !MovingUtil.hasPlayerMovement()))) {
                if (mc.player.isOnGround()) {
                    mc.options.jumpKey.setPressed(true);
                } else if (!mc.player.isGliding()) {

                    PlayerIntersectionUtil.startFallFlying();
                    if(quicSetting.isEnabled()){
                        PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
                    }
                }
            }
        }
    }


    private Slot chestPlate() {
        if (Objects.requireNonNull(mc.player).getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA))
            return PlayerInventoryUtil.getSlot(List.of(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE));
        else return PlayerInventoryUtil.getSlot(Items.ELYTRA);
    }
}

