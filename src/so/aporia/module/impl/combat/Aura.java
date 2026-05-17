/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.MultiSelectSetting;
import so.aporia.module.settings.NumberSetting;
import so.aporia.module.settings.SelectSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.TickEvent;
import so.aporia.utils.user.locale.LocaleManager;
import so.aporia.utils.user.rotation.RotationUtil;

/**
 * KillAura module — automatically attacks entities within range.
 * Uses server-side rotation with free camera decoupling.
 */
public class Aura extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final LocaleManager locale = LocaleManager.getInstance();

    public final SelectSetting combatMode = new SelectSetting(
            locale.get("module.aura.combat_mode"), locale.get("module.aura.combat_mode.desc"))
            .value("1.8", "1.9+").selected("1.9+");

    public final NumberSetting range = new NumberSetting(
            locale.get("module.aura.range"), locale.get("module.aura.range.desc"), 3.5, 1.0, 6.0, 0.1);

    public final NumberSetting rotationSpeed = new NumberSetting(
            locale.get("module.aura.rotation_speed"), locale.get("module.aura.rotation_speed.desc"), 90.0, 5.0, 180.0, 1.0);

    public final NumberSetting fov = new NumberSetting(
            locale.get("module.aura.fov"), locale.get("module.aura.fov.desc"), 180.0, 30.0, 180.0, 5.0);

    public final NumberSetting minCps = new NumberSetting(
            locale.get("module.aura.min_cps"), locale.get("module.aura.min_cps.desc"), 8, 1, 20, 1);

    public final NumberSetting maxCps = new NumberSetting(
            locale.get("module.aura.max_cps"), locale.get("module.aura.max_cps.desc"), 12, 1, 20, 1);

    public final MultiSelectSetting targets = new MultiSelectSetting(
            locale.get("module.aura.targets"), locale.get("module.aura.targets.desc"))
            .options("Players", "Mobs", "Animals");

    public final SelectSetting targetMode = new SelectSetting(
            locale.get("module.aura.target_mode"), locale.get("module.aura.target_mode.desc"))
            .value(locale.get("module.aura.target_closest"), locale.get("module.aura.target_health"))
            .selected(locale.get("module.aura.target_closest"));

    private long lastAttackTime = 0;
    private Entity lockedTarget = null;

    public Aura() {
        super("Aura", Category.COMBAT, -1);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        EventBus.INSTANCE.register(this);
        RotationUtil.sync();
        lockedTarget = null;
        lastAttackTime = 0;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        EventBus.INSTANCE.unregister(this);
        RotationUtil.reset();
        lockedTarget = null;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.level == null) return;

        Entity target = findTarget();
        if (target == null) {
            lockedTarget = null;
            return;
        }

        lockedTarget = target;
        RotationUtil.update(target, rotationSpeed.getFloat());

        if (canAttack()) {
            doAttack(target);
        }
    }

    private Entity findTarget() {
        if (lockedTarget != null
                && isValidTarget(lockedTarget)
                && mc.player.distanceTo(lockedTarget) <= range.getFloat()
                && isInFov(lockedTarget)) {
            return lockedTarget;
        }

        Entity best = null;
        double bestValue = Double.MAX_VALUE;
        float maxRange = range.getFloat();

        for (Player player : mc.level.players()) {
            if (!isValidTarget(player)) continue;

            float dist = mc.player.distanceTo(player);
            if (dist > maxRange || !isInFov(player)) continue;

            double value = targetMode.isSelected(locale.get("module.aura.target_closest"))
                    ? dist : player.getHealth() + player.getAbsorptionAmount();

            if (value < bestValue) {
                bestValue = value;
                best = player;
            }
        }

        return best;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player || !entity.isAlive()) return false;

        if (entity instanceof Player) return targets.isSelected("Players");
        if (entity instanceof Mob) return targets.isSelected("Mobs");
        if (entity instanceof Animal) return targets.isSelected("Animals");

        return false;
    }

    private boolean isInFov(Entity target) {
        if (fov.getFloat() >= 180.0f) return true;

        Vec3 eyes = mc.player.getEyePosition(1.0F);
        Vec3 targetPos = target.getEyePosition(1.0F);
        Vec3 diff = targetPos.subtract(eyes);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float playerYaw = RotationUtil.isActive() ? RotationUtil.getServerYaw() : mc.player.getYRot();
        float yawDiff = Math.abs(Mth.wrapDegrees(targetYaw - playerYaw));

        return yawDiff <= fov.getFloat() / 2.0f;
    }

    private void doAttack(Entity target) {
        mc.gameMode.attack(mc.player, target);
        lastAttackTime = System.currentTimeMillis();
    }

    private boolean canAttack() {
        if (combatMode.isSelected("1.8")) {
            long elapsed = System.currentTimeMillis() - lastAttackTime;
            float targetCps = minCps.getFloat() + (float) Math.random() * (maxCps.getFloat() - minCps.getFloat());
            return elapsed >= (long) (1000.0 / targetCps);
        }
        return mc.player.getAttackStrengthScale(0.5F) >= 0.95F;
    }
}
