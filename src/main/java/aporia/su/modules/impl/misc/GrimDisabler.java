package aporia.su.modules.impl.misc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.Instance;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.impl.TickEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.*;
import aporia.su.util.user.string.PlayerInteractionHelper;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GrimDisabler extends ModuleStructure {

    public static GrimDisabler getInstance() {
        return Instance.get(GrimDisabler.class);
    }

    // ==================== SETTINGS ====================

    private final SelectSetting mode = new SelectSetting("Mode", "Bypass mode")
            .value("TeleportSpoof", "ElytraSpoof", "Smart")
            .selected("Smart");

    private final BooleanSetting bypassGroundSpoof = new BooleanSetting("GroundSpoof", "Bypass GroundSpoof check")
            .setValue(true);

    private final BooleanSetting bypassPhase = new BooleanSetting("Phase", "Bypass Phase check")
            .setValue(true);

    private final BooleanSetting bypassAntiKB = new BooleanSetting("AntiKB", "Bypass AntiKnockback")
            .setValue(true);

    private final BooleanSetting bypassAntiExplosion = new BooleanSetting("AntiExplosion", "Bypass AntiExplosion")
            .setValue(true);

    private final SliderSettings interval = new SliderSettings("Interval", "Ticks between spoof packets")
            .range(1.0f, 20.0f)
            .setValue(5.0f);

    private final BooleanSetting debug = new BooleanSetting("Debug", "Show debug messages")
            .setValue(false);

    // ==================== STATE ====================

    @NonFinal
    int tickCounter = 0;

    public GrimDisabler() {
        super("GrimDisabler", ModuleCategory.MISC);
        settings(mode, bypassGroundSpoof, bypassPhase, bypassAntiKB,
                bypassAntiExplosion, interval, debug);
    }

    @Override
    public void activate() {
        tickCounter = 0;
    }

    @Override
    public void deactivate() {
        tickCounter = 0;
    }

    // ==================== MAIN LOGIC ====================

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        tickCounter++;

        switch (mode.getSelected()) {
            case "TeleportSpoof" -> handleTeleportSpoof();
            case "ElytraSpoof" -> handleTeleportSpoof(); // Та же логика
            case "Smart" -> handleSmart();
        }
    }

    private void handleTeleportSpoof() {
        int ticks = (int) interval.getValue();

        if (tickCounter % ticks == 0) {
            sendSpoofPacket();
        }
    }

    private void handleSmart() {
        boolean inCombat = isInCombat();
        boolean hasVel = hasVelocity();

        if (inCombat || hasVel) {
            if (tickCounter % 2 == 0) {
                sendSpoofPacket();
            }
        } else {
            handleTeleportSpoof();
        }
    }

    // ==================== CORE METHOD ====================

    private void sendSpoofPacket() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Вариант 1: getEntityPos() - если есть
        Vec3d pos = mc.player.getEntityPos();

        // Вариант 2: создать вручную
        // Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        ThreadLocalRandom random = ThreadLocalRandom.current();

        PlayerMoveC2SPacket.Full packet = new PlayerMoveC2SPacket.Full(
                pos.x + random.nextDouble(-0.0001, 0.0001),
                pos.y + random.nextDouble(-0.0001, 0.0001),
                pos.z + random.nextDouble(-0.0001, 0.0001),
                mc.player.getYaw(),
                mc.player.getPitch(),
                mc.player.isOnGround(),
                false
        );

        PlayerInteractionHelper.sendPacketWithOutEvent(packet);
    }

    // ==================== HELPERS ====================

    private boolean isInCombat() {
        try {
            var tpAura = Class.forName("aporia.su.modules.impl.combat.TpAura")
                    .getMethod("getInstance")
                    .invoke(null);

            LivingEntity target = (LivingEntity) tpAura.getClass()
                    .getMethod("getTarget")
                    .invoke(tpAura);

            return target != null && target.isAlive();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasVelocity() {
        Vec3d vel = mc.player.getVelocity();
        return Math.abs(vel.x) > 0.1 || Math.abs(vel.z) > 0.1;
    }
}