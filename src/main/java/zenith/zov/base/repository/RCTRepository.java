package zenith.zov.base.repository;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.notify.NotifyManager;
import zenith.zov.utility.game.player.PlayerInventoryUtil;
import zenith.zov.Zenith;
import zenith.zov.utility.game.server.ServerHandler;
import zenith.zov.utility.math.StopWatch;
import zenith.zov.utility.interfaces.IClient;

public class RCTRepository implements IClient {
    private final StopWatch stopWatch = new StopWatch();
    private boolean lobby;
    private int anarchy;

    public RCTRepository() {
        EventManager.register(this);
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (anarchy != 0 && e.getPacket() instanceof GameMessageS2CPacket message && e.isReceive()) {
            String text = message.content().getString().toLowerCase();
            if (!text.contains("хаб") && text.contains("не удалось")) {
                NotifyManager.getInstance().addNotification("[RCT]️", net.minecraft.text.Text.literal(" На данную анархию " + Formatting.RED + "нельзя" + Formatting.RESET + " зайти"));
                anarchy = 0;
            }
        }
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (anarchy == 0) return;

        ServerHandler serverHandler = Zenith.getInstance().getServerHandler();
        if (!serverHandler.isHolyWorld()) {
            anarchy = 0;
            return;
        }

        int currentAnarchy = serverHandler.getAnarchy();
        if (lobby) {
            if (currentAnarchy == -1) lobby = false;
            else mc.player.networkHandler.sendChatCommand("hub");
            return;
        }

        if (currentAnarchy == anarchy) {
            anarchy = 0;
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getTitle().getString().equals("Выбор Лайт анархии:"))  {
            boolean secondScreen = screen.getScreenHandler().getInventory().size() < 10;
            int[] slots = anarchy < 15 ? new int[]{0, 0} : anarchy < 33 ? new int[]{1, 14} : anarchy < 48 ? new int[]{2, 32} : new int[]{3, 47};
            if (secondScreen) PlayerInventoryUtil.clickSlot(slots[0], 0, SlotActionType.PICKUP, false);
            else PlayerInventoryUtil.clickSlot(17 + anarchy - slots[1], 0, SlotActionType.PICKUP, false);
            return;
        }

        if (stopWatch.every(500)) mc.player.networkHandler.sendChatCommand("lite");
    }

    public void reconnect(int anarchy) {
        if (anarchy > 0 && anarchy < 64) {
            this.anarchy = anarchy;
            this.lobby = true;
        } else {
            NotifyManager.getInstance().addNotification("[RCT]", net.minecraft.text.Text.literal(" Не верный " + Formatting.RED + "лайт"));
        }
    }
}
