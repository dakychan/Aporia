package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import lombok.Getter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.api.types.EventType;
import aporia.su.util.events.impl.entity.InputEvent;
import aporia.su.util.events.impl.entity.RotationUpdateEvent;
import aporia.su.util.events.impl.player.CameraPositionEvent;
import aporia.su.util.events.impl.player.PacketEvent;
import aporia.su.util.events.impl.TickEvent;
import aporia.su.modules.impl.combat.aura.Angle;
import aporia.su.modules.impl.combat.aura.AngleConfig;
import aporia.su.modules.impl.combat.aura.AngleConnection;
import aporia.su.modules.impl.combat.aura.MathAngle;
import aporia.su.modules.impl.combat.aura.impl.LinearConstructor;
import aporia.su.modules.impl.combat.aura.impl.RotateConstructor;
import aporia.su.modules.impl.combat.aura.rotations.MatrixAngle;
import aporia.su.modules.impl.combat.aura.rotations.SPAngle;
import aporia.su.modules.impl.combat.aura.target.TargetFinder;
import aporia.su.modules.impl.combat.tpaura.IsxodHandler;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.util.Instance;
import aporia.su.util.user.render.math.TaskPriority;
import aporia.su.util.user.render.math.MathUtils;
import aporia.su.util.user.player.move.MoveUtil;
import aporia.su.util.events.impl.ui.Event3D;
import aporia.su.util.user.render.Render3D;
import aporia.su.util.user.render.color.ColorUtil;

/**
 * TpAura module - teleport to target, attack, return.
 * <p>
 * Modes:
 * <ul>
 *   <li>Default - classic TP with packet splitting</li>
 *   <li>Spoof - Matrix/Grim bypass with advanced rotations</li>
 * </ul>
 * <p>
 * Features:
 * <ul>
 *   <li>Track TP - gradually teleport to distant targets</li>
 *   <li>Advanced rotations (Matrix/SpookyTime) for Spoof mode</li>
 *   <li>Packet splitting for long-distance teleportation</li>
 * </ul>
 */
@Getter
public class TpAura extends ModuleStructure {
    
    @Native(type = Native.Type.VMProtectBeginUltra)
    public static TpAura getInstance() {
        return Instance.get(TpAura.class);
    }
    
    private final SelectSetting mode = new SelectSetting("Режим", "Mode")
            .value("Default", "Spoof")
            .selected("Default");
    
    private final BooleanSetting followAura = new BooleanSetting("Следовать за аурой", "Follow aura rotation")
            .setValue(true);
    
    private final SliderSettings freeCamSpeed = new SliderSettings("Скорость камеры", "FreeCam speed")
            .range(0.5f, 5.0f)
            .setValue(2.0f)
            .visible(() -> !followAura.isValue());
    
    private final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Target types")
            .value("Игроки", "Мобы", "Животные", "Креатив")
            .selected("Игроки");
    
    private final SliderSettings attackRange = new SliderSettings("Дистанция", "Search range")
            .range(10.0f, 100.0f)
            .setValue(30.0f)
            .visible(() -> mode.isSelected("Default"));
    
    private final SliderSettings cycleDelay = new SliderSettings("Задержка", "Delay between attack cycles in ticks")
            .range(1.0f, 40.0f)
            .setValue(10.0f)
            .visible(() -> mode.isSelected("Default"));
    
    private final SliderSettings maxHits = new SliderSettings("Макс ударов", "Max hits per cycle")
            .range(1.0f, 10.0f)
            .setValue(3.0f)
            .visible(() -> mode.isSelected("Default"));
    
    private final BooleanSetting autoReturn = new BooleanSetting("Авто-возврат", "Auto return to saved position")
            .setValue(true)
            .visible(() -> mode.isSelected("Default"));
    
    private final BooleanSetting smoothRotation = new BooleanSetting("Плавная ротация", "Smooth rotation")
            .setValue(true)
            .visible(() -> mode.isSelected("Default"));
    
    private final SliderSettings spoofRange = new SliderSettings("Дистанция", "Attack range")
            .range(3.0f, 100.0f)
            .setValue(30.0f)
            .visible(() -> mode.isSelected("Spoof"));
    
    private final SliderSettings spoofDelay = new SliderSettings("Задержка между ударами", "Delay between attacks in ticks")
            .range(1.0f, 20.0f)
            .setValue(5.0f)
            .visible(() -> mode.isSelected("Spoof"));
    
    private final SliderSettings spoofMaxHits = new SliderSettings("Ударов до ТП", "Hits before TP again")
            .range(2.0f, 50.0f)
            .setValue(5.0f)
            .visible(() -> mode.isSelected("Spoof"));
    
    private final SelectSetting spoofRotationType = new SelectSetting("Тип ротации", "Rotation type")
            .value("Matrix", "SpookyTime")
            .selected("Matrix")
            .visible(() -> mode.isSelected("Spoof"));
    
    private final BooleanSetting trackMode = new BooleanSetting("Трек ТП", "Track TP - gradually teleport to target")
            .setValue(false);
    
    private final SliderSettings trackDistance = new SliderSettings("Дистанция трека", "Track distance per second")
            .range(10.0f, 50.0f)
            .setValue(20.0f)
            .visible(() -> trackMode.isValue());
    
    private final IsxodHandler isxodHandler = new IsxodHandler();
    private final TargetFinder targetFinder = new TargetFinder();
    
    @lombok.experimental.NonFinal
    public static LivingEntity target;
    
    @lombok.experimental.NonFinal
    public LivingEntity lastTarget;
    
    private int tickCounter = 0;
    private int hitCounter = 0;
    private Vec3d savedPosition = null;
    private boolean attackSuccessful = false;
    
    public Vec3d freeCamPos, freeCamPrevPos;
    private float freeCamYaw = 0, freeCamPitch = 0;
    private long lastDamageTime = 0;
    private long lastHealTime = 0;
    private long lastToggleTime = 0;
    private boolean isTransitioning = false;
    private Vec3d transitionStart = null;
    private Vec3d transitionTarget = null;
    private float transitionProgress = 0;
    
    public TpAura() {
        super("TpAura", ModuleCategory.COMBAT);
        settings(mode, followAura, freeCamSpeed, targetType, attackRange, cycleDelay, maxHits, autoReturn, smoothRotation,
                 spoofRange, spoofDelay, spoofMaxHits, spoofRotationType,
                 trackMode, trackDistance);
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        reset();
        if (mc.player != null) {
            savedPosition = mc.player.getEntityPos();
            isxodHandler.saveIsxod();
            
            if (!followAura.isValue()) {
                freeCamPrevPos = freeCamPos = mc.player.getEyePos();
                freeCamYaw = mc.player.getYaw();
                freeCamPitch = mc.player.getPitch();
            }
        }
    }
    
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToggleTime < 2000 && mc.player != null && mc.player.getVelocity().lengthSquared() > 0.01) {
            return;
        }
        
        if (!followAura.isValue() && freeCamPos != null && mc.player != null) {
            isTransitioning = true;
            transitionStart = freeCamPos;
            transitionTarget = mc.player.getEyePos();
            transitionProgress = 0;
        } else {
            reset();
            AngleConnection.INSTANCE.startReturning();
            targetFinder.releaseTarget();
            target = null;
            lastTarget = null;
        }
        
        lastToggleTime = currentTime;
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        tickCounter++;
        
        if (target == null || !target.isAlive()) {
            return;
        }
        
        if (trackMode.isValue()) {
            handleTrackTP();
        }
        
        if (mode.isSelected("Default")) {
            handleDefaultMode();
        } else if (mode.isSelected("Spoof")) {
            handleSpoofMode();
        }
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String message = packet.content().getString().toLowerCase();
            if (message.contains("вы атаковали") || message.contains("вы были атакованы")) {
                attackSuccessful = true;
            }
            if (message.contains("урон") || message.contains("damage")) {
                lastDamageTime = System.currentTimeMillis();
            }
            if (message.contains("хил") || message.contains("heal") || message.contains("восстановлен")) {
                lastHealTime = System.currentTimeMillis();
            }
        }
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onInput(InputEvent event) {
        if (followAura.isValue() || mc.player == null) {
            return;
        }
        
        float speed = freeCamSpeed.getValue();
        boolean up = false, down = false;
        
        long window = mc.getWindow().getHandle();
        
        float forward = 0, sideways = 0;
        
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS || 
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_8) == GLFW.GLFW_PRESS) {
            forward += 1;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS || 
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_2) == GLFW.GLFW_PRESS) {
            forward -= 1;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS || 
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_4) == GLFW.GLFW_PRESS) {
            sideways += 1;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS || 
            GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_6) == GLFW.GLFW_PRESS) {
            sideways -= 1;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_9) == GLFW.GLFW_PRESS) {
            up = true;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_3) == GLFW.GLFW_PRESS) {
            down = true;
        }
        
        double[] motion = MoveUtil.calculateDirection(forward, sideways, speed);
        
        freeCamPrevPos = freeCamPos;
        freeCamPos = freeCamPos.add(motion[0], up ? speed : down ? -speed : 0, motion[1]);
        
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_7) == GLFW.GLFW_PRESS) {
            freeCamYaw -= 2.0f;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_1) == GLFW.GLFW_PRESS) {
            freeCamYaw += 2.0f;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_5) == GLFW.GLFW_PRESS) {
            freeCamPitch = Math.max(-90, Math.min(90, freeCamPitch - 2.0f));
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_0) == GLFW.GLFW_PRESS) {
            freeCamPitch = Math.max(-90, Math.min(90, freeCamPitch + 2.0f));
        }
    }
    
    @EventHandler
    public void onCameraPosition(CameraPositionEvent event) {
        if (isTransitioning) {
            transitionProgress += 0.05f;
            if (transitionProgress >= 1.0f) {
                isTransitioning = false;
                transitionProgress = 0;
                reset();
                AngleConnection.INSTANCE.startReturning();
                targetFinder.releaseTarget();
                target = null;
                lastTarget = null;
            } else {
                Vec3d smoothPos = transitionStart.lerp(transitionTarget, transitionProgress);
                event.setPos(smoothPos);
            }
        } else if (!followAura.isValue() && freeCamPos != null && freeCamPrevPos != null) {
            event.setPos(MathUtils.interpolate(freeCamPrevPos, freeCamPos));
        }
    }
    
    @EventHandler
    public void onRender3D(Event3D event) {
        if (!followAura.isValue() && mc.player != null) {
            renderPlayerBox(event);
        }
    }
    
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null) {
            return;
        }
        
        if (event.getType() == EventType.PRE) {
            LivingEntity previousTarget = target;
            target = updateTarget();
            
            if (previousTarget != null && target != null && previousTarget != target) {
                hitCounter = 0;
            }
            
            if (previousTarget != null && target == null) {
                hitCounter = 0;
                tickCounter = 0;
            }
            
            if (target != null) {
                rotateToTarget();
                lastTarget = target;
            }
        }
    }
    
    /**
     * Updates and returns current target entity.
     *
     * @return current target or null if no valid target found
     */
    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = mode.isSelected("Spoof") ? spoofRange.getValue() : attackRange.getValue();
        
        targetFinder.searchTargets(mc.world.getEntities(), range, 360, true);
        targetFinder.validateTarget(filter::isValid);
        return targetFinder.getCurrentTarget();
    }
    
    /**
     * Rotates player view towards target using configured rotation mode.
     */
    private void rotateToTarget() {
        if (target == null) {
            return;
        }
        
        Angle targetAngle = MathAngle.calculateAngle(target.getEntityPos().add(0, target.getHeight() / 2, 0));
        
        RotateConstructor rotationMode;
        
        if (mode.isSelected("Spoof")) {
            rotationMode = spoofRotationType.isSelected("Matrix") ? new MatrixAngle() : new SPAngle();
        } else if (mode.isSelected("Default")) {
            if (!smoothRotation.isValue()) {
                return;
            }
            rotationMode = new LinearConstructor();
        } else {
            return;
        }
        
        AngleConfig config = new AngleConfig(rotationMode, true, false);
        Angle.VecRotation rotation = new Angle.VecRotation(targetAngle, targetAngle.toVector());
        
        AngleConnection.INSTANCE.rotateTo(rotation, target, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
    }
    
    /**
     * Handles Default mode attack logic - classic TP to target.
     */
    private void handleDefaultMode() {
        if (tickCounter < cycleDelay.getValue()) {
            return;
        }
        
        if (hitCounter >= (int)maxHits.getValue()) {
            hitCounter = 0;
            tickCounter = 0;
            return;
        }
        
        if (mc.interactionManager != null && target != null && isxodHandler.hasSavedPosition()) {
            Vec3d isxod = isxodHandler.getIsxodPosition();
            Vec3d targetPos = target.getEntityPos();
            Vec3d direction = targetPos.subtract(isxod).normalize();
            Vec3d attackPos = targetPos.subtract(direction.multiply(3.5));
            
            teleportFromTo(isxod, attackPos);
            
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(mc.player.getActiveHand());
            
            if (autoReturn.isValue()) {
                teleportFromTo(attackPos, isxod);
            }
        }
        
        hitCounter++;
        tickCounter = 0;
    }
    
    /**
     * Handles Spoof mode attack logic - packet attacks with rare TP cycles.
     */
    private void handleSpoofMode() {
        if (tickCounter < spoofDelay.getValue()) {
            return;
        }
        
        if (!isxodHandler.hasSavedPosition()) {
            return;
        }
        
        Vec3d isxod = isxodHandler.getIsxodPosition();
        double distance = isxod.distanceTo(target.getEntityPos());
        
        if (distance > spoofRange.getValue()) {
            return;
        }
        
        if (mc.interactionManager != null) {
            int maxHits = (int)spoofMaxHits.getValue();
            
            if (hitCounter > 0 && hitCounter % maxHits == 0) {
                Vec3d targetPos = target.getEntityPos();
                Vec3d direction = targetPos.subtract(isxod).normalize();
                Vec3d attackPos = targetPos.subtract(direction.multiply(3.5));
                
                teleportFromTo(isxod, attackPos);
                
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(mc.player.getActiveHand());
                
                teleportFromTo(attackPos, isxod);
            } else {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(mc.player.getActiveHand());
            }
            
            hitCounter++;
            tickCounter = 0;
        }
    }
    
    /**
     * Teleports player from one position to another with packet splitting for long distances.
     * Splits packets every 10 blocks.
     *
     * @param fromPos starting position
     * @param toPos destination position
     */
    private void teleportFromTo(Vec3d fromPos, Vec3d toPos) {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        
        double distance = fromPos.distanceTo(toPos);
        
        if (distance > 10) {
            int packets = (int) Math.ceil(distance / 10.0);
            packets = Math.max(2, Math.min(packets, 15));
            
            for (int i = 1; i <= packets; i++) {
                double progress = (double) i / packets;
                Vec3d intermediatePos = fromPos.lerp(toPos, progress);
                
                mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                    intermediatePos.x,
                    intermediatePos.y,
                    intermediatePos.z,
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    false,
                    false
                ));
            }
        } else {
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                toPos.x,
                toPos.y,
                toPos.z,
                mc.player.getYaw(),
                mc.player.getPitch(),
                false,
                false
            ));
        }
        
        mc.player.setPos(toPos.x, toPos.y, toPos.z);
    }
    
    /**
     * Handles Track TP mode - gradually teleports to distant targets (horizontal only).
     */
    private void handleTrackTP() {
        if (target == null || mc.player == null) {
            return;
        }
        
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        
        Vec3d playerPosFlat = new Vec3d(playerPos.x, 0, playerPos.z);
        Vec3d targetPosFlat = new Vec3d(targetPos.x, 0, targetPos.z);
        double horizontalDistance = playerPosFlat.distanceTo(targetPosFlat);
        float trackDist = trackDistance.getValue();
        
        if (horizontalDistance > trackDist) {
            Vec3d direction = targetPosFlat.subtract(playerPosFlat).normalize();
            Vec3d newPosFlat = playerPosFlat.add(direction.multiply(trackDist));
            Vec3d newPos = new Vec3d(newPosFlat.x, playerPos.y, newPosFlat.z);
            
            teleportFromTo(playerPos, newPos);
        }
    }
    
    /**
     * Renders 3D box around player model with color based on state.
     */
    private void renderPlayerBox(Event3D event) {
        if (mc.player == null) {
            return;
        }
        
        Box playerBox = mc.player.getBoundingBox();
        
        long currentTime = System.currentTimeMillis();
        int color;
        
        if (currentTime - lastDamageTime < 500) {
            color = 0xFFFF0000;
        } else if (currentTime - lastHealTime < 500) {
            color = 0xFF00FF00;
        } else {
            color = 0xFFFFFFFF;
        }
        
        int fillColor = ColorUtil.multAlpha(color, 0.3f);
        
        Render3D.drawBoxWithCross(playerBox, color, fillColor, 2.0f);
    }
    
    /**
     * Returns whether FreeCam mode is active (for external checks).
     */
    public boolean isFreeCamActive() {
        return isState() && !followAura.isValue();
    }
    
    /**
     * Resets module state.
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void reset() {
        target = null;
        tickCounter = 0;
        hitCounter = 0;
        attackSuccessful = false;
        targetFinder.releaseTarget();
    }
}
