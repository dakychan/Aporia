package zenith.zov.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;



import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.utility.math.Timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ModuleAnnotation(name = "AutoDuels", category = Category.MISC,description = "Кидает дуэль на RW")
public final class AutoDuels extends Module {

    public static final AutoDuels INSTANCE = new AutoDuels();
    private AutoDuels() {
    }
    private final ModeSetting mode = new ModeSetting( "Кит");
    private final ModeSetting.Value shield = new ModeSetting.Value(mode, "Щиты");
    private final ModeSetting.Value shipi = new ModeSetting.Value(mode, "Шипы");
    private final ModeSetting.Value bow = new ModeSetting.Value(mode, "Лук");
    private final ModeSetting.Value totem = new ModeSetting.Value(mode, "Тотемы");
    private final ModeSetting.Value noDebuff = new ModeSetting.Value(mode, "НоуДебафф");
    private final ModeSetting.Value balls = new ModeSetting.Value(mode, "Шары");
    private final ModeSetting.Value classik = new ModeSetting.Value(mode, "Классик");
    private final ModeSetting.Value cheats = new ModeSetting.Value(mode, "Читерский рай");
    private final ModeSetting.Value nezer = new ModeSetting.Value(mode, "Незер");
    {
        mode.setValue(classik);
    }
    private final Timer timer = new Timer();
    private final List<String> sent = new ArrayList<>();

    @EventTarget
    public void onUpdate(EventUpdate event) {
        List<String> playerNames = new ArrayList<>();

        Collections.shuffle(playerNames);

        for (PlayerListEntry entry : mc.player.networkHandler.getPlayerList()) {
            playerNames.add(entry.getProfile().getName());
        }

        for (String name : playerNames) {
            if (timer.finished(750) && !sent.contains(name) && !name.equals(mc.player.getNameForScoreboard())) {
                mc.player.networkHandler.sendChatCommand("duel " + name);
                sent.add(name);
                timer.reset();
            }
        }

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            String title = mc.currentScreen.getTitle().getString();

            if (title.contains("Выбор набора")) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, mode.getValues().indexOf(mode.getRandomEnabledElement()), 0, SlotActionType.PICKUP, mc.player);
            } else if (title.contains("Настройка поединка")) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
            }
        }
    }

    @EventTarget
    public void onReceivePacket(EventPacket event) {
        if(!event.isReceive())return;
        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String msg = packet.content().getString();

            if (msg.contains("Принял") && !msg.contains("не принял")) {
                sent.clear();
                this.toggle();
            }
            
            if (msg.contains("дуэль") && (msg.contains("найдена") || msg.contains("началась") || msg.contains("старт"))) {
                sent.clear();
                this.toggle();
            }
            
            if (msg.contains("победил") || msg.contains("проиграл") || msg.contains("ничья")) {
                sent.clear();
                this.toggle();
            }

            if (msg.contains("Баланс") || msg.contains("отключил запросы")) {
                event.cancel();
            }
        }
    }

    @Override
    public void onEnable() {
        if(mc.player==null){
            this.setEnabled(false);
            return;
        }
        timer.reset();
        super.onEnable();
    }
}
