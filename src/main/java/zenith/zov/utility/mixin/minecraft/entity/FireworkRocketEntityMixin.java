package zenith.zov.utility.mixin.minecraft.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import zenith.zov.Zenith;
import zenith.zov.client.modules.impl.movement.ElytraBooster;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends ProjectileEntity {

    @Unique private Vec3d rotation;
    @Shadow private LivingEntity shooter;

    public FireworkRocketEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d tick(Vec3d original) {
        rotation = original;

        return rotation;
    }
    
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;", ordinal = 0))
    public Vec3d tick(Vec3d instance, double x, double y, double z) {
        ElytraBooster elytraBooster = ElytraBooster.INSTANCE;
        if (elytraBooster != null && elytraBooster.isEnabled() && MinecraftClient.getInstance().player.isGliding()) {
            if (elytraBooster.getMode().getValue().getName().equals("Auto")) {
                return instance.add(
                        rotation.x * 0.1 + (rotation.x * 2.0 - instance.x) * 0.5D,
                        rotation.y * 0.1 + (rotation.y * 2.0 - instance.y) * 0.5D,
                        rotation.z * 0.1 + (rotation.z * 2.0 - instance.z) * 0.5D
                );
            } else {
                double boostValue = elytraBooster.getBoost().getCurrent();
                return instance.add(
                        rotation.x * 0.1 + (rotation.x * boostValue - instance.x) * 0.5D,
                        rotation.y * 0.1 + (rotation.y * boostValue - instance.y) * 0.5D,
                        rotation.z * 0.1 + (rotation.z * boostValue - instance.z) * 0.5D
                );
            }
        } else return instance.add(
                rotation.x * 0.1 + (rotation.x * 1.5D - instance.x) * 0.5D,
                rotation.y * 0.1 + (rotation.y * 1.5D - instance.y) * 0.5D,
                rotation.z * 0.1 + (rotation.z * 1.5D - instance.z) * 0.5D
        );
    }

    @Unique
    private Vec3d calculateRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }
} 