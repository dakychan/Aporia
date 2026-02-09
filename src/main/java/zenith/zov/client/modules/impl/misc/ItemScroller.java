package zenith.zov.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;


import zenith.zov.base.events.impl.other.EventClickSlot;
import zenith.zov.base.events.impl.render.EventHandledScreen;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.player.PlayerInventoryUtil;
import zenith.zov.utility.math.Timer;

@ModuleAnnotation(name = "ItemScroller",description = "Перемещение преметов без задержки",category = Category.MISC)
public final class ItemScroller extends Module {
    public static final ItemScroller INSTANCE = new ItemScroller();
    private ItemScroller() {

    }
    private final NumberSetting scrollerSetting = new NumberSetting("Задержка", 100,0,200,10);
    private final Timer timer = new Timer();



    @EventTarget
    public void onHandledScreen(EventHandledScreen e) {
        Slot hoverSlot = e.getSlotHover();
        
        if (PlayerIntersectionUtil.isKey(mc.options.dropKey.getDefaultKey())) {
            return;
        }
        
        SlotActionType actionType = PlayerIntersectionUtil.isKey(mc.options.attackKey.getDefaultKey()) ? SlotActionType.QUICK_MOVE : null;

        if (isShift() && !isCtrl() && hoverSlot != null && hoverSlot.hasStack() && actionType != null && timer.finished(scrollerSetting.getCurrent())) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hoverSlot.id, 0, actionType, mc.player);
        }
    }

    @EventTarget
    public void onClickSlot(EventClickSlot e) {
        if (e.getActionType() == SlotActionType.THROW) {
            return;
        }
        
        int slotId = e.getSlotId();
        if (slotId < 0 || slotId > mc.player.currentScreenHandler.slots.size()) return;
        Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
        Item item = slot.getStack().getItem();

        if (item != null && isCtrl() && timer.finished(50)) {
            PlayerInventoryUtil.slots().filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory))
                        .forEach(s -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, s.id, 1, e.getActionType(), mc.player));
        }
    }

    private boolean isShift() {
       return PlayerIntersectionUtil.isKey(mc.options.sneakKey.getDefaultKey());
    }
    private boolean isCtrl() {
        return PlayerIntersectionUtil.isKey(mc.options.sprintKey.getDefaultKey());
    }
}

