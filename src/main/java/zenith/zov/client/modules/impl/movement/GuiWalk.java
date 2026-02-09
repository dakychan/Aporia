package zenith.zov.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;


import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import zenith.zov.base.events.impl.other.EventClickSlot;
import zenith.zov.base.events.impl.other.EventCloseScreen;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.utility.game.player.MovingUtil;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.player.PlayerInventoryComponent;
import zenith.zov.utility.game.player.PlayerInventoryUtil;
import zenith.zov.utility.game.server.AutoBuyUtil;

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