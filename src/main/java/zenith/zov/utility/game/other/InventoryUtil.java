package zenith.zov.utility.game.other;

import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import zenith.zov.utility.interfaces.IMinecraft;
@Deprecated
public class InventoryUtil implements IMinecraft {

    public enum Switch {
        Normal,
        Silent,
        Alternative,
        None
    }

    public enum Swing {
        MainHand,
        OffHand,
        Packet,
        None
    }

    public static int findItem(Item item) {
        return findItem(item, 0, 35);
    }

    public static int findHotbar(Item item) {
        return findItem(item, 0, 8);
    }

    public static int findInventory(Item item) {
        return findItem(item, 9, 35);
    }

    public static int findItem(Item item, int start, int end) {
        for (int i = end; i >= start; i--) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findEmptySlot(int start, int end) {
        for (int i = end; i >= start; i--) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static void switchSlot(Switch mode, int slot, int previousSlot) {
        if (slot == -1 || previousSlot == -1) return;

        switch (mode) {
            case Normal -> {
                mc.player.getInventory().setSelectedSlot(slot);
            }
            case Silent -> {
                mc.player.getInventory().setSelectedSlot(slot);
                NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
            case Alternative -> swapItems(slot, previousSlot);
        }
    }

    public static void switchBack(Switch mode, int slot, int previousSlot) {
        if (slot == -1 || previousSlot == -1) return;

        switch (mode) {
            case Normal -> {
                mc.player.getInventory().setSelectedSlot(previousSlot);
            }
            case Silent -> {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                NetworkUtils.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
            }
            case Alternative -> swapItems(slot, previousSlot);
        }
    }

    public static void swapItems(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
        
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
    }

    public static void swap(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(targetSlot), 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, indexToSlot(slot), 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
    }

    public static void bypassSwap(int slot, int targetSlot) {
        if (slot == -1 || targetSlot == -1) return;
        // bupass bude!
        swap(slot, targetSlot);
    }

    public static int indexToSlot(int index) {
        if (index >= 0 && index <= 8) return 36 + index;
        return index;
    }

    public static void swing(Swing mode) {
        switch (mode) {
            case MainHand -> mc.player.swingHand(Hand.MAIN_HAND);
            case OffHand -> mc.player.swingHand(Hand.OFF_HAND);
            case Packet -> NetworkUtils.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
    }
} 
