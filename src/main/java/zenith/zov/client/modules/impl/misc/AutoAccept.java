package zenith.zov.client.modules.impl.misc;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import zenith.zov.Zenith;
import com.darkmagician6.eventapi.EventTarget;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;

import java.util.Locale;

//EXPENSIVE???
@ModuleAnnotation(name = "AutoAccept", category = Category.MISC, description = "Автоматически принимает телепортацию")
public final class AutoAccept extends Module {
    public static final AutoAccept INSTANCE = new AutoAccept();
    
    private AutoAccept() {
    }

    private final BooleanSetting onlyFriend = new BooleanSetting("Только друзья", false);

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) return;
        if (!event.isReceive()) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String raw = packet.content().getString().toLowerCase(Locale.ROOT);
            if (raw.contains("телепортироваться") || raw.contains("has requested teleport") || raw.contains("просит к вам телепортироваться")) {
                if (onlyFriend.isEnabled()) {
                    boolean yes = false;

                    for (String friend : Zenith.getInstance().getFriendManager().getItems()) {
                        if (raw.contains(friend.toLowerCase(Locale.ROOT))) {
                            yes = true;
                            break;
                        }
                    }

                    if (!yes) return;
                }

                mc.player.networkHandler.sendChatCommand("tpaccept");
            }
        }
    }
}
