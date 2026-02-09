package zenith.zov.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;


import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(name = "AutoAuth", category = Category.MISC, description = "Авто регистрация")
public final class AutoAuth extends Module {
    public static final AutoAuth INSTANCE = new AutoAuth();
    private AutoAuth() {
    }
    @EventTarget
    public void onReceive(EventPacket event) {
        if(!event.isReceive())return;
        if (event.getPacket() instanceof GameMessageS2CPacket chatMessagePacket) {
            if (mc.getNetworkHandler() == null) return;

            String password = "123123qq";
            String content = chatMessagePacket.content().getString().toLowerCase();

            if (content.contains("зарегистрируйтесь") || content.contains("/register")) {
                mc.getNetworkHandler().sendChatCommand("register %s %s".formatted(password, password));
            }
        }
    }
}
