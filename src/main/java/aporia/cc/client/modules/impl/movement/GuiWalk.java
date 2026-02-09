package aporia.cc.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;


import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import aporia.cc.base.events.impl.other.EventClickSlot;
import aporia.cc.base.events.impl.other.EventCloseScreen;
import aporia.cc.base.events.impl.player.EventUpdate;
import aporia.cc.base.events.impl.server.EventPacket;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.utility.game.player.MovingUtil;
import aporia.cc.utility.game.player.PlayerIntersectionUtil;
import aporia.cc.utility.game.player.PlayerInventoryComponent;
import aporia.cc.utility.game.player.PlayerInventoryUtil;
import aporia.cc.utility.game.server.AutoBuyUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "Gui Walk", category = Category.MOVEMENT, description = "Можно ходить в инвентаре или контейнере")
public final class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();

    private GuiWalk() {
    }

    private final List<Packet<?>> packets = new ArrayList<>();

    @EventTarget
    public void onPacket(EventPacket e) {
        switch (e.getPacket()) {
            case ClickSlotC2SPacket slot when (!packets.isEmpty() || MovingUtil.hasPlayerMovement()) && PlayerInventoryComponent.shouldSkipExecution() -> {

                packets.add(slot);
                e.cancel();

            }
            case CloseScreenS2CPacket screen when screen.getSyncId() == 0 -> e.cancel();
            default -> {
            }
        }
    }

    @EventTarget
    public void onTick(EventUpdate e) {

        if (!PlayerInventoryUtil.isServerScreen() && PlayerInventoryComponent.shouldSkipExecution() && (!packets.isEmpty() || mc.player.currentScreenHandler.getCursorStack().isEmpty())) {
            PlayerInventoryComponent.updateMoveKeys();
        }

    }

    @EventTarget
    public void onClickSlot(EventClickSlot e) {
        System.out.println(e.getSlotId() +" " + e.getButton());
        if (mc.player.currentScreenHandler.isValid(e.getSlotId())) {
            Slot slot = mc.player.currentScreenHandler.getSlot(e.getSlotId());
            System.out.println(AutoBuyUtil.getKey(slot.getStack()));
        }
        SlotActionType actionType = e.getActionType();
        if ((!packets.isEmpty() || MovingUtil.hasPlayerMovement()) && ((e.getButton() == 1 &&( !actionType.equals(SlotActionType.SWAP)   &&! actionType.equals(SlotActionType.THROW))  ))) {
            e.setCancelled(true);

        }
    }

    @EventTarget
    public void onCloseScreen(EventCloseScreen e) {
        if (!packets.isEmpty()) PlayerInventoryComponent.addTask(() -> {
            packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
            packets.clear();
            PlayerInventoryUtil.updateSlots();

        });
    }
}

