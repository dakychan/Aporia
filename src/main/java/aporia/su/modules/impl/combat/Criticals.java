package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import aporia.su.util.events.impl.player.AttackEvent;
import lombok.experimental.NonFinal;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.events.api.EventHandler;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.util.user.render.math.MathUtils;

/**
 * Модуль для критических ударов с обходом античитов.
 * Поддерживает различные режимы обхода включая Grim AC.
 */
public class Criticals extends ModuleStructure {

    private final SelectSetting mode = new SelectSetting("Режим", "Выбор режима критов")
            .value("Packet", "Grim", "Jump", "Matrix", "Vanilla")
            .selected("Grim");

    @NonFinal
    private boolean attackPending = false;

    public Criticals() {
        super("Criticals", "Критические удары с обходом античитов", ModuleCategory.COMBAT);
        settings(mode);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        attackPending = false;
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        attackPending = false;
    }

    /**
     * Обработчик атаки - вызывается перед ударом по сущности.
     */
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        /** Если работает TpAura, пусть она сама решает как критовать */
        if (TpAura.getInstance() != null && TpAura.getInstance().isState()) {
            return;
        }

        /** Выполняем критический удар в зависимости от режима */
        switch (mode.getSelected()) {
            case "Packet" -> {
                /** Packet работает только на земле */
                if (mc.player.isOnGround() && !mc.player.getAbilities().flying) {
                    performPacketCrit();
                }
            }
            case "Grim" -> {
                /** Grim работает всегда, даже в полете */
                performGrimCrit();
            }
            case "Jump" -> {
                /** Jump работает всегда - просто прыжок */
                performJumpCrit();
            }
            case "Matrix" -> {
                /** Matrix работает только на земле */
                if (mc.player.isOnGround() && !mc.player.getAbilities().flying) {
                    performMatrixCrit();
                }
            }
            case "Vanilla" -> {
                /** Vanilla работает только на земле */
                if (mc.player.isOnGround() && !mc.player.getAbilities().flying) {
                    performVanillaCrit();
                }
            }
        }
    }

    /**
     * Packet режим - классический обход через пакеты позиции.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void performPacketCrit() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        /** Отправляем пакеты микро-прыжка */
        sendPositionPacket(x, y + 0.0625, z, yaw, pitch, false);
        sendPositionPacket(x, y, z, yaw, pitch, false);
    }

    /**
     * Grim режим - обход Grim AC через манипуляцию fallDistance.
     * Основан на методе grimSuperBypass$$$ из Thunder Hack.
     * Работает даже в полете!
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void performGrimCrit() {
        /** 1. Устанавливаем фейковую дистанцию падения */
        float fakeFallDistance = MathUtils.getRandom(1.0E-5f, 1.0E-4f);
        mc.player.fallDistance = fakeFallDistance;

        /** 2. Вычисляем смещение вниз (отрицательное значение fallDistance) */
        double offsetY = -fakeFallDistance;

        /** 3. Добавляем шум к углу обзора для обхода детекции */
        float noisyPitch = mc.player.getPitch() + MathUtils.getRandom(-0.001f, 0.001f);
        float noisyYaw = mc.player.getYaw() + MathUtils.getRandom(-0.001f, 0.001f);

        /** 4. Отправляем пакет с микро-смещением вниз */
        sendPositionPacket(
                mc.player.getX(),
                mc.player.getY() + offsetY,
                mc.player.getZ(),
                noisyYaw,
                noisyPitch,
                false
        );

        /**
         * Логика работы:
         * - Grim проверяет fallDistance для определения падения
         * - Мы ставим микро-значение (не 0, но очень маленькое)
         * - Отправляем позицию чуть ниже текущей
         * - Grim думает что игрок начал падать и разрешает крит
         * - Шум в углах обзора маскирует SetRotation от бота
         * - Работает даже во флае, так как не требует onGround
         */
    }

    /**
     * Matrix режим - обход Matrix AC.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void performMatrixCrit() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        /** Matrix требует более агрессивные значения */
        sendPositionPacket(x, y + 0.11, z, yaw, pitch, false);
        sendPositionPacket(x, y + 0.1100013579, z, yaw, pitch, false);
        sendPositionPacket(x, y + 0.0000013579, z, yaw, pitch, false);
    }

    /**
     * Jump режим - работает всегда, даже в полете.
     * Добавляет вертикальную скорость для имитации прыжка.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void performJumpCrit() {
        /** Добавляем вертикальную скорость вверх */
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, velocity.y + 0.1, velocity.z);
        
        /** Устанавливаем fallDistance для критов */
        mc.player.fallDistance = 0.1f;
    }

    /**
     * Vanilla режим - простой прыжок для ванильного сервера.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void performVanillaCrit() {
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    /**
     * Вспомогательный метод для отправки пакета позиции.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void sendPositionPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, mc.player.horizontalCollision)
        );
    }
}
