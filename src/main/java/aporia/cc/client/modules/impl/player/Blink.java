package aporia.cc.client.modules.impl.player;

import lombok.Getter;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;
import com.darkmagician6.eventapi.EventTarget;
import aporia.cc.base.events.impl.player.EventUpdate;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.setting.impl.BooleanSetting;
import aporia.cc.client.modules.api.setting.impl.NumberSetting;
import aporia.cc.utility.math.Timer;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(name = "Blink", category = Category.PLAYER, description = "Задерживает пакеты чтобы было труднее попасть")
public final class Blink extends Module {
    public static final Blink INSTANCE = new Blink();
    
    private Blink() {
    }
    
    private final List<Packet<?>> packets = new ArrayList<>();
    @Getter
    private final Timer timer = new Timer();
    @Getter
    private final BooleanSetting pulse = new BooleanSetting("Пульсация", false);
    @Getter
    private final NumberSetting time = new NumberSetting("Время", 12f, 1f, 40f, 1f);
    private Vec3d lastPos;
    private boolean replaying;

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (pulse.isEnabled() && timer.finished((long)(time.getCurrent() * 50))) {
            onDisable();
            onEnable();
            timer.reset();
        }
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        packets.clear();
        lastPos = mc.player.getPos();
        timer.reset();
        replaying = false;
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        replaying = true;
        for (Packet<?> p : packets) {
            mc.player.networkHandler.sendPacket(p);
        }
        replaying = false;
        packets.clear();
        lastPos = null;
    }
}
