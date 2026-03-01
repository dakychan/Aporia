package aporia.su.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import aporia.su.events.api.EventHandler;
import aporia.su.events.impl.PacketEvent;
import aporia.su.events.impl.TickEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.Instance;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class Ambience extends ModuleStructure {

    public static Ambience getInstance() {
        return Instance.get(Ambience.class);
    }

    public SliderSettings time = new SliderSettings("Время", "Время суток (0-24000)")
            .range(0, 24000)
            .setValue(1000);

    private double animatedTime = 1000;

    public Ambience() {
        super("Ambience", "Изменяет время мира", ModuleCategory.RENDER);
        settings(time);
    }

    @Override
    public void activate() {
        animatedTime = time.getValue();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        double targetTime = time.getValue();
        double speed = 150 / 1000.0;
        double diff = targetTime - animatedTime;
        animatedTime += diff * speed;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
        }
    }

    public long getCustomTime() {
        return (long) animatedTime;
    }
}