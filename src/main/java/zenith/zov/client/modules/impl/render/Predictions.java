package zenith.zov.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.lwjgl.opengl.GL11;
import zenith.zov.Zenith;
import zenith.zov.base.events.impl.render.EventRender2D;
import zenith.zov.base.events.impl.render.EventRender3D;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.player.RaytracingUtil;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationUtil;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.math.ProjectionUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.RenderLayerUtil;
import zenith.zov.utility.render.level.Render3DUtil;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

@ModuleAnnotation(name = "Predictions",category = Category.RENDER,description = "Показывает куда упадет предмет")
public final class Predictions extends Module {

    private final List<Point> points = new ArrayList<>();
    public static Predictions INSTANCE = new Predictions();
    private Predictions() {}

    @EventTarget
    public void onDraw(EventRender2D e) {

        for (Point point : points) {
            Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(point.pos);
            int ticks = point.ticks;

            if (!ProjectionUtil.canSee(point.pos)) continue;

            Font font = Fonts.MEDIUM.getFont(7);
            double time = ticks * 50 / 1000.0;
            String text = String.format("%.1f", time) + " сек";
            float textWidth = font.width(text);
            float iconSize = 16*0.7f;
            float padding = 2f;

            float centerX = (float) vec3d.getX();
            float centerY = (float) vec3d.getY();

            float totalWidth = iconSize + padding + textWidth;
            float totalHeight = iconSize;


            float rectX = centerX - totalWidth / 2f;
            float rectY = centerY - totalHeight / 2f;


          e.getContext().drawRoundedRect(
                    rectX - padding, rectY - padding,
                    totalWidth + padding * 2, totalHeight + padding * 2,
                    BorderRadius.all(2),
                    Zenith.getInstance().getThemeManager().getCurrentTheme().getForegroundColor());


            float itemX = rectX;
            float itemY = rectY + (totalHeight - iconSize) / 2f;
            e.getContext().pushMatrix();
            e.getContext().getMatrices().translate(itemX, itemY);
            e.getContext().getMatrices().scale(0.7f,0.7f);
            e.getContext().drawItem(point.stack, 0, 0);
            e.getContext().popMatrix();


            float textX = itemX + iconSize + padding;
            float textY = centerY - font.height() / 2f;
            e.getContext().drawText(
                    font,
                    text,
                    textX,
                    textY,
                    Zenith.getInstance().getThemeManager().getCurrentTheme().getWhite()
            );


        }
    }

    @EventTarget
    public void onWorldRender(EventRender3D e) {
        points.clear();
        drawPredictionInHand(e.getMatrix(), List.of(mc.player.getMainHandStack(), mc.player.getOffHandStack()));
        getProjectiles().forEach(entity -> {
            Vec3d motion = entity.getVelocity();
            Vec3d pos = entity.getPos();
            Vec3d prevPos;
            int ticks = 0;

            for (int i = 0; i < 300; i++) {
                prevPos = pos;
                pos = pos.add(motion);
                motion = calculateMotion(entity, prevPos, motion);

                HitResult result = RaytracingUtil.raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);
                if (!result.getType().equals(HitResult.Type.MISS)) {
                    pos = result.getPos();
                }


                Render3DUtil.drawLine(prevPos, pos, Zenith.getInstance().getThemeManager().getClientColor(i).mulAlpha(MathHelper.clamp(i / 25.0f, 0, 1)).getRGB(), 2, false);

                Vec3d finalPrevPos = prevPos, finalPos = pos;
                boolean inEntity = PlayerIntersectionUtil.streamEntities()
                        .filter(ent -> ent instanceof LivingEntity living && living != mc.player && living.isAlive())
                        .anyMatch(ent -> ent.getBoundingBox().expand(0.25).intersects(finalPrevPos, finalPos));
                if (result.getType().equals(HitResult.Type.BLOCK) || pos.y < -128 || inEntity || result.getType().equals(HitResult.Type.ENTITY)) {
                    BreakingBad(entity, pos, ticks);
                    break;
                }
                ticks++;
            }
        });
    }

    public void drawPredictionInHand(MatrixStack matrix, List<ItemStack> stacks) {
        Item activeItem = mc.player.getActiveItem().getItem();
        for (ItemStack stack : stacks) {
            List<HitResult> results = switch (stack.getItem()) {
                case ExperienceBottleItem item -> checkTrajectory(new ExperienceBottleEntity(mc.world, mc.player, stack), 0.8);
                case SplashPotionItem item -> checkTrajectory(new SplashPotionEntity(mc.world, mc.player, stack), 0.55);
                case TridentItem item when item.equals(activeItem) && mc.player.getItemUseTime() >= 10 -> checkTrajectory(new TridentEntity(mc.world, mc.player, stack), 2.5);
                case SnowballItem item -> checkTrajectory(new SnowballEntity(mc.world, mc.player, stack), 1.5);
                case EggItem item -> checkTrajectory(new EggEntity(mc.world, mc.player, stack), 1.5);
                case EnderPearlItem item -> checkTrajectory(new EnderPearlEntity(mc.world, mc.player, stack), 1.5);
                case BowItem item when item.equals(activeItem) && mc.player.isUsingItem() -> checkTrajectory(new ArrowEntity(mc.world, mc.player, stack, stack), 3 * MathHelper.clamp((mc.player.getItemUseTime() + mc.getRenderTickCounter().getTickProgress(false)) / 20F,0F, 1F));
                case CrossbowItem item when CrossbowItem.isCharged(stack) -> {
                    ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                    List<HitResult> list = new ArrayList<>();
                    if (component != null) {
                        float velocity = component.getProjectiles().getFirst().isOf(Items.FIREWORK_ROCKET) ? 100 : 3;
                        list.add(checkTrajectory(RotationUtil.getClientRotation().toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                        if (component.getProjectiles().size() > 2) {
                            float pitchAbs = mc.player.getPitch() / 90;
                            float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
                            float yaw = MathHelper.lerp(Math.abs(delta), 10, 90);
                            float pitch = MathHelper.lerp(delta, 0, 10);
                            list.add(checkTrajectory(new Rotation(mc.player.getYaw() - yaw, mc.player.getPitch() - pitch).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                            list.add(checkTrajectory(new Rotation(mc.player.getYaw() + yaw, mc.player.getPitch() - pitch).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                        }
                    }
                    yield list;
                }
                default -> null;
            };
            if (results != null) {
                results = results.stream().filter(Objects::nonNull).toList();
                if (!results.isEmpty()) renderProjectileResults(matrix, results);
            }
            return;
        }
    }

    public void renderProjectileResults(MatrixStack matrix, List<HitResult> results) {
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        RenderSystem.lineWidth(1);
        RenderLayer lineLayer = RenderLayerUtil.lines();
        BufferBuilder buffer = RenderLayerUtil.begin(lineLayer);
        for (HitResult result : results) {
            Direction direction = getDirection(result);
            Vec3d renderPos = result.getPos().subtract(mc.getEntityRenderDispatcher().camera.getPos());
            int color = result.getType().equals(HitResult.Type.ENTITY) ? ColorRGBA.RED.getRGB() : Zenith.getInstance().getThemeManager().getClientColor(90).getRGB();
            double width = 0.3;

            matrix.push();
            matrix.translate(renderPos.x, renderPos.y, renderPos.z);
            if (direction.equals(Direction.WEST) || direction.equals(Direction.EAST)) matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
            else if (direction.equals(Direction.SOUTH) || direction.equals(Direction.NORTH)) matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            for (int i = 0, size = 90; i <= size; i++) {
                Render3DUtil.vertexLine(matrix, buffer, MathUtil.cosSin(i, size, width), MathUtil.cosSin(i + 1, size, width), color);
            }
            
            Render3DUtil.vertexLine(matrix, buffer, new Vec3d(0, 0, -width), new Vec3d(0, 0, width), color);
            Render3DUtil.vertexLine(matrix, buffer, new Vec3d(-width, 0, 0), new Vec3d(width, 0, 0), color);
            matrix.pop();
        }
        RenderLayerUtil.draw(lineLayer, buffer);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    public List<Entity> getProjectiles() {
        return PlayerIntersectionUtil.streamEntities().filter(e -> (e instanceof PersistentProjectileEntity || e instanceof ThrownItemEntity || e instanceof ItemEntity) && !visible(e)).toList();
    }

    public List<HitResult> checkTrajectory(ProjectileEntity entity, double velocity) {
       return new ArrayList<>(Collections.singleton(checkTrajectory(RotationUtil.getClientRotation().toVector(), entity, velocity)));
    }

    public HitResult checkTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity) {
        double distance = Math.sqrt(lookVec.x * lookVec.x + lookVec.y * lookVec.y + lookVec.z * lookVec.z);
        Vec3d motion = mc.player.getPos().subtract(mc.player.lastX, mc.player.lastY, mc.player.lastZ);
        if (entity instanceof ArrowEntity arrow && arrow.getItemStack().getItem() instanceof CrossbowItem) {
            motion = Vec3d.ZERO;
        }
        return traceTrajectory(mc.player.getEyePos().add(MathUtil.interpolate(mc.player).subtract(mc.player.getPos())), lookVec.multiply(velocity / distance).add(motion), entity);
    }

    public HitResult calcTrajectory(ProjectileEntity e) {
        return traceTrajectory(e.getPos(), e.getVelocity(), e);
    }

    public HitResult traceTrajectory(Vec3d pos, Vec3d motion, ProjectileEntity entity) {
        Vec3d prevPos;
        for (int i = 0; i < 300; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = calculateMotion(entity, prevPos, motion);

            HitResult result = RaytracingUtil.raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);
            if (!result.getType().equals(HitResult.Type.MISS)) {
                return result;
            }

            Vec3d finalPrevPos = prevPos, finalPos = pos;
            if (PlayerIntersectionUtil.streamEntities().filter(ent -> ent != entity.getOwner() && ent instanceof LivingEntity living && living != mc.player && living.isAlive())
                    .anyMatch(ent -> ent.getBoundingBox().expand(0.3).intersects(finalPrevPos, finalPos))) {
                return new HitResult(pos) {
                    @Override
                    public Type getType() {
                        return Type.ENTITY;
                    }
                };
            }

            if (pos.y < -128) break;
        }
        return null;
    }

    public Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = Objects.requireNonNull(mc.world).getBlockState(BlockPos.ofFloored(prevPos)).getFluidState().isIn(FluidTags.WATER);

        float multiply = switch (entity) {
            case TridentEntity i -> 0.99F;
            case PersistentProjectileEntity i when isInWater -> 0.6F;
            default -> isInWater ? 0.8F : 0.99F;
        };

        return motion.multiply(multiply).add(0, -entity.getFinalGravity(),0);
    }

    private void BreakingBad(Entity entity, Vec3d pos, int ticks) {
        switch (entity) {
            case ItemEntity item -> points.add(new Point(item.getStack(), pos, ticks));
            case ThrownItemEntity thrown -> points.add(new Point(thrown.getStack(), pos, ticks));
            case PersistentProjectileEntity persistent -> points.add(new Point(persistent.getItemStack(), pos, ticks));
            default -> {}
        }
    }

    private Direction getDirection(HitResult result) {
        if (result instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getSide();
        }
        Vec3d diff = result.getPos().subtract(mc.player.getEyePos()).normalize();
        return Direction.getFacing(diff.x, diff.y, diff.z);
    }

    private boolean visible(Entity entity) {
        boolean posChange = entity.getX() == entity.lastX && entity.getY() == entity.lastY && entity.getZ() == entity.lastZ;
        boolean itemEntityCheck = entity instanceof ItemEntity && (entity.isOnGround() || PlayerIntersectionUtil.isBoxInBlock(entity.getBoundingBox().expand(2), Blocks.WATER));
        return posChange || itemEntityCheck;
    }

    private record Point(ItemStack stack, Vec3d pos, int ticks) {}
}

