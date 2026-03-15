package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import aporia.su.util.Instance;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.api.types.EventType;
import aporia.su.util.events.impl.entity.RotationUpdateEvent;
import aporia.su.util.events.impl.TickEvent;
import aporia.su.modules.impl.combat.aura.Angle;
import aporia.su.modules.impl.combat.aura.AngleConfig;
import aporia.su.modules.impl.combat.aura.AngleConnection;
import aporia.su.modules.impl.combat.aura.MathAngle;
import aporia.su.modules.impl.combat.aura.target.TargetFinder;
import aporia.su.modules.impl.combat.aura.target.Vector;
import aporia.su.modules.impl.combat.aura.rotations.MatrixAngle;
import aporia.su.modules.impl.combat.aura.impl.RotateConstructor;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.user.render.math.TaskPriority;
import aporia.su.util.user.string.PlayerInteractionHelper;

import java.util.concurrent.ThreadLocalRandom;

/**
 * MaceSpoof - МАССИВНЫЙ УРОН БУЛАВОЙ через spoof позиции.
 *
 *MusteryWorld + слабый Matrix = ЛЕГКИЙ BYPASS
 *
 * Режимы обхода:
 * 1. Flying - если включён AutoFlyMe/креатив (Grim exempt полностью!)
 * 2. Elytra - если есть элитры (Grim exempt!)
 * 3. Direct - просто спуфим пакеты (Matrix слабый)
 *
 * Концепция:
 * - Стоишь рядом с целью (реально)
 * - Spoof'ишь позицию СВЕРХУ (сервер думает ты падаешь)
 * - Бьёшь булавой → MACE SMASH DAMAGE
 * - Визуально стоишь на месте!
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaceSpoof extends ModuleStructure {

    @Native(type = Native.Type.VMProtectBeginUltra)
    public static MaceSpoof getInstance() {
        return Instance.get(MaceSpoof.class);
    }

    private final SelectSetting mode = new SelectSetting("Режим", "Bypass mode")
            .value("Auto", "Flying", "Elytra", "Direct")
            .selected("Auto");

    private final SliderSettings fallHeight = new SliderSettings("Высота падения", "Fake fall height for mace damage")
            .range(5.0f, 100.0f)
            .setValue(50.0f);

    private final SliderSettings attackRange = new SliderSettings("Дистанция атаки", "Attack range")
            .range(2.0f, 6.0f)
            .setValue(3.5f);

    private final SliderSettings hitsPerSecond = new SliderSettings("Ударов в секунду", "Hits per second")
            .range(1.0f, 20.0f)
            .setValue(10.0f);

    private final BooleanSetting autoSwap = new BooleanSetting("Авто-свап на булаву", "Auto swap to mace")
            .setValue(true);

    private final BooleanSetting requireMace = new BooleanSetting("Только с булавой", "Only with mace in hand")
            .setValue(true);

    private final BooleanSetting spoofGround = new BooleanSetting("Spoof onGround", "Spoof ground status")
            .setValue(true);

    private final BooleanSetting debugMode = new BooleanSetting("Дебаг", "Debug messages")
            .setValue(false);

    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");

    private final BooleanSetting ignoreWalls = new BooleanSetting("Бить сквозь стены", "Attack through walls")
            .setValue(true);

    @Setter
    @NonFinal
    LivingEntity target = null;

    @NonFinal
    long lastAttackTime = 0;

    @NonFinal
    boolean fakeGliding = false;

    @NonFinal
    String currentMode = "Auto";

    TargetFinder targetFinder = new TargetFinder();
    TargetFinder.EntityFilter entityFilter;

    public MaceSpoof() {
        super("MaceSpoof", ModuleCategory.COMBAT);
        settings(mode, fallHeight, attackRange, hitsPerSecond, autoSwap, requireMace,
                spoofGround, debugMode, targetType, ignoreWalls);
    }

    @Override
    public void activate() {
        resetState();
        updateEntityFilter();
        lastAttackTime = System.currentTimeMillis();
        fakeGliding = false;
        if (mode.isSelected("Auto")) {
            detectBestMode();
        } else {
            currentMode = mode.getSelected();
        }
    }

    @Override
    public void deactivate() {
        if (fakeGliding && mc.player != null) {
            mc.getNetworkHandler().sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
            fakeGliding = false;
        }

        AngleConnection.INSTANCE.clear();
        AngleConnection.INSTANCE.setRotation(null);
        resetState();
    }

    private void resetState() {
        target = null;
        lastAttackTime = 0;
        fakeGliding = false;
        targetFinder.releaseTarget();
    }

    private void updateEntityFilter() {
        this.entityFilter = new TargetFinder.EntityFilter(targetType.getSelected());
    }

    /**
     * Авто-определение лучшего режима обхода
     */
    private void detectBestMode() {
        if (mc.player != null && (mc.player.getAbilities().flying || mc.player.getAbilities().allowFlying)) {
            currentMode = "Flying";
            return;
        }
        if (hasElytraEquipped()) {
            currentMode = "Elytra";
            return;
        }
        currentMode = "Direct";
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (requireMace.isValue() && !isHoldingMace()) {
            if (autoSwap.isValue()) {
                if (!swapToMace()) {
                    return;
                }
            } else {
                return;
            }
        }
        long attackDelay = (long) (1000.0 / hitsPerSecond.getValue());
        if (System.currentTimeMillis() - lastAttackTime < attackDelay) {
            return;
        }
        findTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double distance = mc.player.distanceTo(target);
        if (distance > attackRange.getValue()) {
            return;
        }
        performSpoofAttack();
        lastAttackTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || target == null) {
            return;
        }
        if (event.getType() == EventType.PRE) {
            rotateToTarget();
        }
    }

    private void findTarget() {
        targetFinder.searchTargets(
                mc.world.getEntities(),
                attackRange.getValue() + 2.0f,
                180,
                ignoreWalls.isValue()
        );
        targetFinder.validateTarget(entityFilter::isValid);
        target = targetFinder.getCurrentTarget();
    }

    private void performSpoofAttack() {
        if (target == null || mc.player == null || mc.interactionManager == null) {
            return;
        }
        Vec3d realPos = mc.player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        double height = fallHeight.getValue();
        Vec3d spoofPos = new Vec3d(targetPos.x, targetPos.y + height, targetPos.z);
        float yaw = mc.player.getYaw();
        float pitch = 90.0f;
        switch (currentMode) {
            case "Flying" -> performFlyingSpoof(spoofPos, realPos, yaw, pitch);
            case "Elytra" -> performElytraSpoof(spoofPos, realPos, yaw, pitch);
            case "Direct" -> performDirectSpoof(spoofPos, realPos, yaw, pitch);
            default -> performDirectSpoof(spoofPos, realPos, yaw, pitch);
        }
    }

    /**
     * Flying mode - Grim полностью exempt когда isFlying!
     * Лучший режим если работает AutoFlyMe обход
     */
    private void performFlyingSpoof(Vec3d spoofPos, Vec3d realPos, float yaw, float pitch) {
        sendSpoofPacket(spoofPos, yaw, pitch, false);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        sendSpoofPacket(realPos, yaw, pitch, spoofGround.isValue());
    }

    /**
     * Elytra mode - Grim exempt для gliding игроков!
     */
    private void performElytraSpoof(Vec3d spoofPos, Vec3d realPos, float yaw, float pitch) {
        if (!mc.player.isGliding() && !fakeGliding) {
            PlayerInteractionHelper.sendPacketWithOutEvent(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
            fakeGliding = true;
        }
        sendSpoofPacket(spoofPos, yaw, pitch, false);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        sendSpoofPacket(realPos, yaw, pitch, spoofGround.isValue());
    }

    /**
     * Direct mode - просто спуфим пакеты
     * Работает на слабых античитах (Matrix на MusteryWorld)
     */
    private void performDirectSpoof(Vec3d spoofPos, Vec3d realPos, float yaw, float pitch) {
        sendSpoofPacket(spoofPos, yaw, pitch, false);
        sendSpoofPacket(spoofPos.x, spoofPos.y - 0.5, spoofPos.z, yaw, pitch, false);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        sendSpoofPacket(realPos, yaw, pitch, spoofGround.isValue());
    }

    private void sendSpoofPacket(Vec3d pos, float yaw, float pitch, boolean onGround) {
        sendSpoofPacket(pos.x, pos.y, pos.z, yaw, pitch, onGround);
    }

    private void sendSpoofPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        PlayerMoveC2SPacket.Full packet = new PlayerMoveC2SPacket.Full(
                x + random.nextDouble(-0.0001, 0.0001),
                y + random.nextDouble(-0.0001, 0.0001),
                z + random.nextDouble(-0.0001, 0.0001),
                yaw,
                pitch,
                onGround,
                false
        );
        PlayerInteractionHelper.sendPacketWithOutEvent(packet);
    }

    private void rotateToTarget() {
        if (target == null) return;
        Vec3d aimPoint = Vector.hitbox(target, 1, target.isOnGround() ? 0.9F : 1.4F, 1, 2);
        Angle targetAngle = MathAngle.calculateAngle(aimPoint);
        RotateConstructor rotator = new MatrixAngle();
        AngleConfig config = new AngleConfig(rotator, true, false);
        Angle.VecRotation rotation = new Angle.VecRotation(targetAngle, targetAngle.toVector());
        AngleConnection.INSTANCE.rotateTo(
                rotation,
                target,
                1,
                config,
                TaskPriority.HIGH_IMPORTANCE_1,
                this
        );
    }

    private boolean isHoldingMace() {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        return mainHand.getItem() == Items.MACE || offHand.getItem() == Items.MACE;
    }

    private boolean swapToMace() {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.MACE) {
                mc.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private boolean hasElytraEquipped() {
        if (mc.player == null) return false;
        ItemStack chestPlate = mc.player.getInventory().getStack(2); // Chest slot
        return chestPlate.getItem() == Items.ELYTRA;
    }

    /**
     * Получить текущий режим (для GUI)
     */
    public String getCurrentModeDisplay() {
        return currentMode;
    }
}